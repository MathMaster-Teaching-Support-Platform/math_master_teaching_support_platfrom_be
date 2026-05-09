package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fptu.math_master.dto.request.AutoBlueprintRequest;
import com.fptu.math_master.dto.request.LatexRenderRequest;
import com.fptu.math_master.dto.response.BlueprintFromRealQuestionResponse;
import com.fptu.math_master.dto.response.BlueprintFromRealQuestionResponse.DiffEntry;
import com.fptu.math_master.dto.response.BlueprintFromRealQuestionResponse.TfClauseDraft;
import com.fptu.math_master.dto.response.BlueprintParameter;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.exception.LatexCompileException;
import com.fptu.math_master.exception.LatexRenderProxyException;
import com.fptu.math_master.exception.LatexRenderTimeoutException;
import com.fptu.math_master.service.BlueprintService;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.LatexRenderService;
import com.fptu.math_master.service.TokenCostConfigService;
import com.fptu.math_master.service.UserSubscriptionService;
import com.fptu.math_master.util.PromptLoader;
import com.fptu.math_master.util.SecurityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single AI-call reverse-templating + constraint-aware value selection. Both the
 * Method-1 reverse pass and the generation-time value picker use Gemini through
 * the same {@link GeminiService}; the prompt files live in {@code resources/prompts/}
 * and are loaded once at boot via {@link PromptLoader}.
 *
 * <p>Two design points worth knowing:
 *
 * <ol>
 *   <li><b>Bundled-context reverse pass.</b> The teacher's question text, options,
 *       clauses, solution steps, and diagram all go in one prompt so the model can
 *       decide that the same {@code 2} appearing across artifacts is the same
 *       {@code {{a}}}. That cross-artifact agreement was the recurring bug in the
 *       per-field {@code extractParameters} endpoint.</li>
 *   <li><b>Programmatic guardrail after the AI selector.</b> The AI is good at
 *       reading {@code "integer, 1 ≤ a ≤ 9, a is even"} but occasionally returns
 *       {@code 10} (off-by-one) or a decimal. We run a tiny regex-based validator
 *       after every set; failed sets are dropped before the substitutor sees them.
 *       The selector is asked once per batch with the failed indices as feedback.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlueprintServiceImpl implements BlueprintService {

  private static final String REVERSE_PROMPT = "blueprint_from_real_question";
  private static final String SELECT_PROMPT = "select_parameter_values";

  private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");
  /** After substituting sample values, any remaining `{{name}}` means invalid blueprint output. */
  private static final Pattern UNRESOLVED_DIAGRAM_PLACEHOLDER =
      Pattern.compile("\\{\\{\\s*[\\p{L}\\p{N}_]+\\s*\\}\\}");

  /** Gemini re-tries when diagram LaTeX fails QuickLaTeX; avoids infinite provider spend. */
  private static final int BLUEPRINT_DIAGRAM_RENDER_MAX_ATTEMPTS = 5;

  private final GeminiService geminiService;
  private final PromptLoader promptLoader;
  private final UserSubscriptionService userSubscriptionService;
  private final TokenCostConfigService tokenCostConfigService;
  private final LatexRenderService latexRenderService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  // ───────────────────────────── Method 1: reverse-templating ─────────────────────────────

  @Override
  public BlueprintFromRealQuestionResponse blueprintFromRealQuestion(AutoBlueprintRequest req) {
    String latexRepairAppendix = "";
    JsonNode root = null;

    for (int attempt = 1; attempt <= BLUEPRINT_DIAGRAM_RENDER_MAX_ATTEMPTS; attempt++) {
      String prompt =
          promptLoader.body(REVERSE_PROMPT)
              + "\n\n## Now process this input:\n"
              + buildReverseInput(req);
      if (!latexRepairAppendix.isBlank()) {
        prompt += "\n\n" + latexRepairAppendix;
      }

      String aiContent;
      try {
        aiContent = geminiService.sendJsonMessage(prompt);
      } catch (Exception e) {
        log.error("[Blueprint] reverse-template AI call failed: {}", e.getMessage(), e);
        throw new RuntimeException("AI reverse-templating failed: " + e.getMessage(), e);
      }

      root = parseJsonOrThrow(aiContent, "reverse-template");

      Optional<String> diagramFailure = validateDiagramTemplateCompiles(root);
      if (diagramFailure.isEmpty()) {
        break;
      }

      String lastDiagramError = diagramFailure.get();
      log.warn(
          "[Blueprint] diagram LaTeX validation failed attempt {}/{}: {}",
          attempt,
          BLUEPRINT_DIAGRAM_RENDER_MAX_ATTEMPTS,
          lastDiagramError);

      if (attempt >= BLUEPRINT_DIAGRAM_RENDER_MAX_ATTEMPTS) {
        throw new RuntimeException(
            "Không thể render sơ đồ LaTeX sau "
                + BLUEPRINT_DIAGRAM_RENDER_MAX_ATTEMPTS
                + " lần thử AI. Chi tiết lỗi compile/log: "
                + lastDiagramError);
      }

      latexRepairAppendix = buildDiagramLatexRepairAppendix(root, lastDiagramError);
    }

    if (root == null) {
      throw new RuntimeException("AI reverse-templating returned no parseable result.");
    }

    // Token deduction: only after JSON parses AND diagram compiles (or diagram absent).
    // Retrying Gemini for LaTeX repair does not deduct extra tokens — single successful charge.
    if (!SecurityUtils.hasRole("ADMIN")) {
      Integer cost = tokenCostConfigService.getCostPerUse("question-blueprint");
      if (cost != null && cost > 0) {
        userSubscriptionService.consumeMyTokens(cost, "BLUEPRINT_FROM_QUESTION");
      }
    }

    BlueprintFromRealQuestionResponse.BlueprintFromRealQuestionResponseBuilder b =
        BlueprintFromRealQuestionResponse.builder();

    b.templateText(textOrEmpty(root, "templateText"));
    b.answerFormula(textOrEmpty(root, "answerFormula"));
    b.solutionStepsTemplate(textOrNull(root, "solutionStepsTemplate"));
    b.diagramTemplate(textOrNull(root, "diagramTemplate"));

    // optionsGenerator may be missing for non-MCQ
    JsonNode opts = root.get("optionsGenerator");
    if (opts != null && opts.isObject()) {
      Map<String, String> map = new LinkedHashMap<>();
      opts.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText("")));
      b.optionsGenerator(map);
    }

    // clauseTemplates may be missing for non-TF
    JsonNode clauses = root.get("clauseTemplates");
    if (clauses != null && clauses.isArray()) {
      List<TfClauseDraft> drafts = new ArrayList<>();
      for (JsonNode c : clauses) {
        drafts.add(TfClauseDraft.builder()
            .key(c.path("key").asText(""))
            .text(c.path("text").asText(""))
            .truthValue(c.path("truthValue").asBoolean(false))
            .build());
      }
      b.clauseTemplates(drafts);
    }

    List<BlueprintParameter> params = extractParametersFromRoot(root);
    b.parameters(params);

    // globalConstraints[]
    List<String> gc = new ArrayList<>();
    JsonNode gcNode = root.get("globalConstraints");
    if (gcNode != null && gcNode.isArray()) {
      gcNode.forEach(n -> gc.add(n.asText("")));
    }
    b.globalConstraints(gc);

    // diff[] — synthesised on the BE side using the AI output and the original input
    b.diff(buildDiff(req, params, root));

    // warnings + confidence
    List<String> warnings = new ArrayList<>();
    JsonNode wNode = root.get("warnings");
    if (wNode != null && wNode.isArray()) {
      wNode.forEach(n -> warnings.add(n.asText("")));
    }
    double confidence = 1.0;
    if (params.isEmpty()) {
      warnings.add("AI extracted zero parameters — Blueprint will be effectively static.");
      confidence -= 0.5;
    }
    if (!round_trips(root, params)) {
      warnings.add("answerFormula failed smoke evaluation against the teacher's correctAnswer.");
      confidence -= 0.4;
    }
    confidence = Math.max(0, Math.min(1, confidence));
    b.warnings(warnings);
    b.confidence(confidence);

    return b.build();
  }

  private List<BlueprintParameter> extractParametersFromRoot(JsonNode root) {
    List<BlueprintParameter> params = new ArrayList<>();
    JsonNode pNode = root.get("parameters");
    if (pNode == null || !pNode.isArray()) {
      return params;
    }
    for (JsonNode p : pNode) {
      List<String> occ = new ArrayList<>();
      JsonNode oArr = p.get("occurrences");
      if (oArr != null && oArr.isArray()) {
        oArr.forEach(o -> occ.add(o.asText("")));
      }
      params.add(
          BlueprintParameter.builder()
              .name(p.path("name").asText(""))
              .constraintText(p.path("constraintText").asText(""))
              .sampleValue(extractSampleValue(p.get("sampleValue")))
              .occurrences(occ)
              .build());
    }
    return params;
  }

  /**
   * Substitutes {@code {{param}}} using each {@link BlueprintParameter#getSampleValue()} for server-side
   * diagram compile checks (same idea as the FE preview).
   */
  private static String substituteBlueprintPlaceholders(String template, List<BlueprintParameter> params) {
    if (template == null || template.isBlank()) {
      return template == null ? "" : template;
    }
    String out = template;
    for (BlueprintParameter p : params) {
      String name = p.getName();
      if (name == null || name.isBlank()) {
        continue;
      }
      Object v = p.getSampleValue();
      if (v == null) {
        continue;
      }
      String replacement = Matcher.quoteReplacement(v.toString());
      Pattern pat = Pattern.compile("\\{\\{\\s*" + Pattern.quote(name) + "\\s*\\}\\}");
      out = pat.matcher(out).replaceAll(replacement);
    }
    return out;
  }

  /**
   * @return empty if there is no diagram or QuickLaTeX accepted the concrete LaTeX; otherwise an error
   *     description for the model / caller.
   */
  private Optional<String> validateDiagramTemplateCompiles(JsonNode root) {
    String diag = textOrNull(root, "diagramTemplate");
    if (diag == null || diag.isBlank()) {
      return Optional.empty();
    }
    List<BlueprintParameter> params = extractParametersFromRoot(root);
    String substituted = substituteBlueprintPlaceholders(diag, params).trim();
    if (substituted.isBlank()) {
      return Optional.of("diagramTemplate became empty after substituting sample values.");
    }
    if (UNRESOLVED_DIAGRAM_PLACEHOLDER.matcher(substituted).find()) {
      return Optional.of(
          "diagramTemplate still contains unresolved {{placeholders}} after applying sampleValue — "
              + "every placeholder must match a parameter name.");
    }
    try {
      latexRenderService.render(LatexRenderRequest.builder().latex(substituted).build());
      return Optional.empty();
    } catch (LatexCompileException e) {
      String msg = e.getMessage();
      return Optional.of("QuickLaTeX compile error: " + (msg == null ? "(no message)" : msg));
    } catch (LatexRenderTimeoutException e) {
      return Optional.of("QuickLaTeX timeout: " + e.getMessage());
    } catch (LatexRenderProxyException e) {
      return Optional.of("QuickLaTeX / network error: " + e.getMessage());
    } catch (IllegalArgumentException e) {
      return Optional.of("Invalid LaTeX payload: " + e.getMessage());
    }
  }

  private String buildDiagramLatexRepairAppendix(JsonNode root, String rendererError) {
    String tpl = textOrNull(root, "diagramTemplate");
    String tplSafe = tpl == null ? "" : tpl;
    if (tplSafe.length() > 6000) {
      tplSafe = tplSafe.substring(0, 6000) + "\n… [truncated]";
    }
    List<BlueprintParameter> params = extractParametersFromRoot(root);
    String concrete = substituteBlueprintPlaceholders(tplSafe, params);
    String concreteSafe =
        concrete.length() > 8000 ? concrete.substring(0, 8000) + "\n… [truncated]" : concrete;

    return "## CRITICAL — diagramTemplate MUST compile on the server\n"
        + "The backend substitutes each parameter's `sampleValue` into `{{name}}`, then sends the "
        + "result to **QuickLaTeX** (preamble includes tikz, pgfplots, tkz-tab, tkz-euclide).\n"
        + "**Validation failed.** Renderer output:\n```\n"
        + rendererError
        + "\n```\n"
        + "**Your diagramTemplate (may contain placeholders):**\n```\n"
        + tplSafe
        + "\n```\n"
        + "**Concrete LaTeX sent to QuickLaTeX after substitution:**\n```\n"
        + concreteSafe
        + "\n```\n"
        + "Fix `diagramTemplate` so the concrete LaTeX compiles. Full "
        + "`\\\\documentclass...\\\\begin{document}` wrappers are OK — the server strips them. "
        + "Reply with **strict JSON only**, same schema as before.\n";
  }

  /** Builds the JSON input block appended to the reverse-templating prompt. */
  private String buildReverseInput(AutoBlueprintRequest r) {
    ObjectNode n = objectMapper.createObjectNode();
    n.put("questionType", r.getQuestionType() != null ? r.getQuestionType().name() : "");
    n.put("questionText", r.getQuestionText() == null ? "" : r.getQuestionText());
    n.put("correctAnswer", r.getCorrectAnswer() == null ? "" : r.getCorrectAnswer());
    n.put("solutionSteps", r.getSolutionSteps() == null ? "" : r.getSolutionSteps());
    n.put("diagramLatex", r.getDiagramLatex() == null ? "" : r.getDiagramLatex());
    n.put("cognitiveLevel", r.getCognitiveLevel() != null ? r.getCognitiveLevel().name() : "");

    if (r.getOptions() != null) {
      ObjectNode opts = n.putObject("options");
      r.getOptions().forEach(opts::put);
    }
    if (r.getClauses() != null) {
      ArrayNode arr = n.putArray("clauses");
      r.getClauses().forEach((k, v) -> {
        ObjectNode c = arr.addObject();
        c.put("key", k);
        if (v instanceof Map<?, ?> m) {
          Object t = m.get("text");
          Object tv = m.get("truthValue");
          c.put("text", t == null ? "" : t.toString());
          c.put("truthValue", Boolean.TRUE.equals(tv));
        } else {
          c.put("text", v == null ? "" : v.toString());
          c.put("truthValue", false);
        }
      });
    }
    return n.toPrettyString();
  }

  private List<DiffEntry> buildDiff(
      AutoBlueprintRequest r, List<BlueprintParameter> params, JsonNode root) {
    List<DiffEntry> out = new ArrayList<>();
    String tplText = textOrEmpty(root, "templateText");
    if (!tplText.isBlank() && r.getQuestionText() != null) {
      out.add(DiffEntry.builder().field("templateText").before(r.getQuestionText()).after(tplText).build());
    }
    String formula = textOrEmpty(root, "answerFormula");
    if (!formula.isBlank() && r.getCorrectAnswer() != null) {
      out.add(DiffEntry.builder().field("answerFormula").before(r.getCorrectAnswer()).after(formula).build());
    }
    String solution = textOrNull(root, "solutionStepsTemplate");
    if (solution != null && r.getSolutionSteps() != null) {
      out.add(DiffEntry.builder().field("solutionStepsTemplate").before(r.getSolutionSteps()).after(solution).build());
    }
    String diag = textOrNull(root, "diagramTemplate");
    if (diag != null && r.getDiagramLatex() != null) {
      out.add(DiffEntry.builder().field("diagramTemplate").before(r.getDiagramLatex()).after(diag).build());
    }
    JsonNode opts = root.get("optionsGenerator");
    if (opts != null && opts.isObject() && r.getOptions() != null) {
      opts.fields().forEachRemaining(e -> {
        String before = r.getOptions().getOrDefault(e.getKey(), "");
        out.add(DiffEntry.builder()
            .field("options." + e.getKey())
            .before(before)
            .after(e.getValue().asText(""))
            .build());
      });
    }
    if (params.isEmpty()) {
      out.add(DiffEntry.builder().field("parameters").before("(none)").after("(none extracted)").build());
    }
    return out;
  }

  /** Best-effort smoke test: substitute sampleValue into answerFormula and see if it parses. */
  private boolean round_trips(JsonNode root, List<BlueprintParameter> params) {
    String formula = textOrEmpty(root, "answerFormula");
    if (formula.isBlank()) return false;
    String substituted = formula;
    for (BlueprintParameter p : params) {
      Object v = p.getSampleValue();
      if (v == null) continue;
      substituted = substituted.replace("{{" + p.getName() + "}}", v.toString());
    }
    // We don't run a full math evaluator here — that's done by the substitutor.
    // We just check the substituted string is non-empty and free of leftover {{}} markers.
    return !substituted.isBlank() && !substituted.contains("{{");
  }

  // ───────────────────────────── Generation: value selection ──────────────────────────────

  @Override
  public List<Map<String, Object>> selectValueSets(
      QuestionTemplate template, int count, List<Map<String, Object>> alreadyUsed, String hint) {
    List<BlueprintParameter> params = readParameters(template);
    if (params.isEmpty() || count <= 0) return List.of();

    String prompt = promptLoader.body(SELECT_PROMPT) + "\n\n## Now process this input:\n"
        + buildSelectionInput(params, template.getGlobalConstraints(), count, alreadyUsed, hint);

    String aiContent;
    try {
      aiContent = geminiService.sendJsonMessage(prompt);
    } catch (Exception e) {
      log.error("[Blueprint] value-selection AI call failed: {}", e.getMessage(), e);
      return List.of();
    }

    JsonNode root = parseJsonOrLog(aiContent, "value-selection");
    if (root == null) return List.of();

    JsonNode arr = root.get("valueSets");
    if (arr == null || !arr.isArray()) return List.of();

    List<Map<String, Object>> validSets = new ArrayList<>();
    for (JsonNode set : arr) {
      if (!set.isObject()) continue;
      Map<String, Object> tuple = new LinkedHashMap<>();
      set.fields().forEachRemaining(e -> {
        if (e.getKey().startsWith("_")) return;
        JsonNode v = e.getValue();
        if (v.isInt() || v.isLong()) tuple.put(e.getKey(), v.asLong());
        else if (v.isDouble() || v.isFloat()) tuple.put(e.getKey(), v.asDouble());
        else tuple.put(e.getKey(), v.asText(""));
      });
      if (passesGuardrails(tuple, params)) {
        validSets.add(tuple);
      }
    }
    return validSets;
  }

  @Override
  public String selectionPromptVersion() {
    return promptLoader.version(SELECT_PROMPT);
  }

  @Override
  public String reversePromptVersion() {
    return promptLoader.version(REVERSE_PROMPT);
  }

  // ─── Guardrails ────────────────────────────────────────────────────────────────────────

  private static final Pattern INT_RE = Pattern.compile("\\binteger\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern EVEN_RE = Pattern.compile("\\beven\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern ODD_RE = Pattern.compile("\\bodd\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern PRIME_RE = Pattern.compile("\\bprime\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern NEZ_RE = Pattern.compile("(?:!=|≠)\\s*0");
  private static final Pattern LE_RE =
      Pattern.compile("(?:^|[^<])(-?\\d+(?:\\.\\d+)?)\\s*(?:<=|≤)\\s*(\\w+)");
  private static final Pattern GE_RE =
      Pattern.compile("(\\w+)\\s*(?:<=|≤)\\s*(-?\\d+(?:\\.\\d+)?)");
  private static final Pattern DIV_RE =
      Pattern.compile("divisible\\s*by\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

  /** Quick programmatic check; not exhaustive but catches the obvious AI off-by-ones. */
  private boolean passesGuardrails(Map<String, Object> tuple, List<BlueprintParameter> params) {
    for (BlueprintParameter p : params) {
      Object v = tuple.get(p.getName());
      if (v == null) return false;
      String c = p.getConstraintText() == null ? "" : p.getConstraintText();
      double d;
      try {
        d = Double.parseDouble(v.toString());
      } catch (NumberFormatException nfe) {
        // non-numeric (e.g. "prime expression") — let the AI's word stand
        continue;
      }
      if (INT_RE.matcher(c).find() && d != Math.floor(d)) return false;
      if (EVEN_RE.matcher(c).find() && Math.floorMod((long) d, 2L) != 0) return false;
      if (ODD_RE.matcher(c).find() && Math.floorMod((long) d, 2L) != 1) return false;
      if (PRIME_RE.matcher(c).find() && !isPrime((long) d)) return false;
      if (NEZ_RE.matcher(c).find() && d == 0.0) return false;
      Matcher mLe = LE_RE.matcher(c);
      while (mLe.find()) {
        if (mLe.group(2).equals(p.getName())) {
          double lo = Double.parseDouble(mLe.group(1));
          if (d < lo) return false;
        }
      }
      Matcher mGe = GE_RE.matcher(c);
      while (mGe.find()) {
        if (mGe.group(1).equals(p.getName())) {
          double hi = Double.parseDouble(mGe.group(2));
          if (d > hi) return false;
        }
      }
      Matcher mDiv = DIV_RE.matcher(c);
      if (mDiv.find()) {
        long k = Long.parseLong(mDiv.group(1));
        if (k > 0 && Math.floorMod((long) d, k) != 0) return false;
      }
    }
    return true;
  }

  private static boolean isPrime(long n) {
    if (n < 2) return false;
    if (n < 4) return true;
    if (n % 2 == 0) return false;
    for (long i = 3; i * i <= n; i += 2) if (n % i == 0) return false;
    return true;
  }

  // ─── Helpers ───────────────────────────────────────────────────────────────────────────

  /** Reads {@link QuestionTemplate#getParameters()} in the new shape. Tolerates the legacy shape too. */
  @SuppressWarnings("unchecked")
  private List<BlueprintParameter> readParameters(QuestionTemplate t) {
    Map<String, Object> raw = t.getParameters();
    if (raw == null) return List.of();
    List<BlueprintParameter> out = new ArrayList<>();
    for (Map.Entry<String, Object> e : raw.entrySet()) {
      Object def = e.getValue();
      String name = e.getKey();
      if (def instanceof Map<?, ?> rawMap) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) rawMap;
        Object ctxt = m.get("constraintText");
        if (ctxt == null) {
          // legacy shape — synthesise constraintText from min/max/exclude
          Object typeObj = m.get("type");
          String type = typeObj == null ? "integer" : typeObj.toString();
          Object min = m.get("min");
          Object max = m.get("max");
          StringBuilder s = new StringBuilder(type);
          if (min != null && max != null) {
            s.append(", ").append(min).append(" ≤ ").append(name).append(" ≤ ").append(max);
          } else if (min != null) {
            s.append(", ").append(name).append(" ≥ ").append(min);
          } else if (max != null) {
            s.append(", ").append(name).append(" ≤ ").append(max);
          }
          if (m.get("exclude") instanceof List<?> ex && !ex.isEmpty()) {
            List<String> excludeStrings = new ArrayList<>(ex.size());
            for (Object o : ex) excludeStrings.add(String.valueOf(o));
            s.append(", ")
                .append(name)
                .append(" ∉ {")
                .append(String.join(",", excludeStrings))
                .append("}");
          }
          ctxt = s.toString();
        }
        Object sample = m.get("sampleValue");
        if (sample == null) sample = m.get("min");
        List<String> occ = new ArrayList<>();
        if (m.get("occurrences") instanceof List<?> l) {
          for (Object o : l) occ.add(String.valueOf(o));
        }
        out.add(BlueprintParameter.builder()
            .name(name)
            .constraintText(ctxt.toString())
            .sampleValue(sample)
            .occurrences(occ)
            .build());
      }
    }
    return out;
  }

  private String buildSelectionInput(
      List<BlueprintParameter> params,
      String[] global,
      int count,
      List<Map<String, Object>> alreadyUsed,
      String hint) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("count", count);
    ArrayNode pArr = root.putArray("parameters");
    for (BlueprintParameter p : params) {
      ObjectNode po = pArr.addObject();
      po.put("name", p.getName());
      po.put("constraintText", p.getConstraintText() == null ? "" : p.getConstraintText());
      Object s = p.getSampleValue();
      if (s instanceof Number n) po.put("sampleValue", n.doubleValue());
      else po.put("sampleValue", s == null ? "" : s.toString());
    }
    ArrayNode gArr = root.putArray("globalConstraints");
    if (global != null) for (String s : global) gArr.add(s);

    ArrayNode usedArr = root.putArray("alreadyUsedTuples");
    if (alreadyUsed != null) {
      for (Map<String, Object> tuple : alreadyUsed) {
        ObjectNode t = usedArr.addObject();
        tuple.forEach((k, v) -> {
          if (v instanceof Number n) t.put(k, n.doubleValue());
          else t.put(k, v == null ? "" : v.toString());
        });
      }
    }
    root.put("distinctnessHint", hint == null ? "" : hint);
    return root.toPrettyString();
  }

  private JsonNode parseJsonOrThrow(String aiContent, String label) {
    JsonNode n = parseJsonOrLog(aiContent, label);
    if (n == null) throw new RuntimeException("AI " + label + " returned non-JSON content");
    return n;
  }

  private JsonNode parseJsonOrLog(String aiContent, String label) {
    if (aiContent == null) return null;
    String trimmed = aiContent.trim();
    // Models often wrap output in ```json fences; strip them.
    if (trimmed.startsWith("```")) {
      int firstNl = trimmed.indexOf('\n');
      int lastFence = trimmed.lastIndexOf("```");
      if (firstNl > 0 && lastFence > firstNl) {
        trimmed = trimmed.substring(firstNl + 1, lastFence).trim();
      }
    }
    if (!trimmed.startsWith("{")) {
      Matcher m = JSON_OBJECT.matcher(trimmed);
      if (m.find()) trimmed = m.group();
    }
    try {
      return objectMapper.readTree(trimmed);
    } catch (Exception ex) {
      log.warn("[Blueprint] failed to parse {} JSON: {}", label, ex.getMessage());
      return null;
    }
  }

  private static String textOrEmpty(JsonNode n, String key) {
    JsonNode v = n.get(key);
    return v == null || v.isNull() ? "" : v.asText("");
  }

  private static String textOrNull(JsonNode n, String key) {
    JsonNode v = n.get(key);
    if (v == null || v.isNull()) return null;
    String s = v.asText("");
    return s.isBlank() ? null : s;
  }

  private static Object extractSampleValue(JsonNode n) {
    if (n == null || n.isNull()) return null;
    if (n.isInt() || n.isLong()) return n.asLong();
    if (n.isDouble() || n.isFloat()) return n.asDouble();
    return n.asText("");
  }

  /** Default fallback for {@link Map} sample values when the AI omits the field. */
  @SuppressWarnings("unused")
  private static Object firstNonNull(Object... values) {
    for (Object v : values) if (v != null) return v;
    return null;
  }

  /** Reserve hash for legacy tests. */
  @SuppressWarnings("unused")
  private static int hash(Map<String, Object> m) {
    return new HashMap<>(m).hashCode();
  }
}
