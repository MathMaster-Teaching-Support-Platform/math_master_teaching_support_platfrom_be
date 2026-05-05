package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.request.AutoBlueprintRequest;
import com.fptu.math_master.dto.request.ExtractParametersRequest;
import com.fptu.math_master.dto.request.GenerateParametersRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.request.SetClausePointsRequest;
import com.fptu.math_master.dto.request.UpdateParametersRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.AutoBlueprintResponse;
import com.fptu.math_master.dto.response.BlueprintFromRealQuestionResponse;
import com.fptu.math_master.dto.response.BlueprintParameter;
import com.fptu.math_master.dto.response.ExtractParametersResponse;
import com.fptu.math_master.dto.response.GenerateParametersResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.entity.CanonicalQuestion;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateVariant;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.BlueprintService;
import com.fptu.math_master.service.GeminiService;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AIEnhancementServiceImpl implements AIEnhancementService {

  private static final Pattern PLACEHOLDER_PATTERN =
      Pattern.compile("\\{\\{\\s*(.+?)\\s*}}|\\{\\s*(.+?)\\s*}");
  private static final Pattern DOUBLE_BRACE_PLACEHOLDER_PATTERN =
      Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

  GeminiService geminiService;
  ObjectMapper objectMapper = new ObjectMapper();
  QuestionTemplateRepository questionTemplateRepository;
  QuestionRepository questionRepository;
  BlueprintService blueprintService;

  @Override
  public AIEnhancedQuestionResponse enhanceQuestion(AIEnhancementRequest request) {
    log.info("Enhancing question using Gemini AI");

    try {
      // Build the AI prompt
      String prompt = buildEnhancementPrompt(request);

      // Call Gemini
      String aiContent = geminiService.sendMessage(prompt);

      // Parse the response
      AIEnhancedQuestionResponse enhancedResponse = parseAIResponse(aiContent, request);

      // Validate the AI output
      boolean isValid = validateAIOutput(request, enhancedResponse);
      enhancedResponse.setValid(isValid);

      if (!isValid) {
        log.warn(
            "AI output validation failed with errors: {}", enhancedResponse.getValidationErrors());
        // For now, we'll still return the response instead of using fallback
        // because fallback creates incomplete questions
        enhancedResponse.setEnhanced(false);
        return enhancedResponse; // Return the AI response even if validation failed
      }

      enhancedResponse.setEnhanced(true);
      log.info("Question enhanced successfully");
      return enhancedResponse;

    } catch (Exception e) {
      log.error("Failed to enhance question with AI: {}", e.getMessage(), e);
      return createFallbackResponse(
          request, Collections.singletonList("AI enhancement failed: " + e.getMessage()));
    }
  }

  @Override
  public boolean validateAIOutput(
      AIEnhancementRequest request, AIEnhancedQuestionResponse response) {
    List<String> errors = new ArrayList<>();

    // 1. Validate correct answer hasn't changed
    // Try to calculate correct answer numerically using formula + parameters first
    String calculatedCorrectAnswer = null;
    if (request.getAnswerFormula() != null && request.getParameters() != null) {
      try {
        calculatedCorrectAnswer =
            evaluateFormula(request.getAnswerFormula(), request.getParameters());
      } catch (Exception e) {
        log.debug("Could not evaluate formula for validation: {}", e.getMessage());
      }
    }

    // Use calculated answer for validation if available, otherwise use the provided answer
    String answerToValidate =
        calculatedCorrectAnswer != null ? calculatedCorrectAnswer : request.getCorrectAnswer();

    if (!isSameAnswer(
        answerToValidate,
        request.getRawOptions(),
        response.getCorrectAnswerKey(),
        response.getEnhancedOptions())) {
      log.warn(
          "Validation: AI changed the correct answer from '{}' to '{}'",
          answerToValidate,
          response.getCorrectAnswerKey());
      errors.add("AI changed the correct answer - this is not allowed");
    }

    // 2. Validate MCQ has exactly 4 options (A, B, C, D)
    if (request.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
      if (response.getEnhancedOptions() == null || response.getEnhancedOptions().size() != 4) {
        log.warn(
            "Validation: MCQ has {} options instead of 4",
            response.getEnhancedOptions() == null ? 0 : response.getEnhancedOptions().size());
        errors.add("MCQ must have exactly 4 options (A, B, C, D)");
      } else {
        Set<String> expectedKeys = new HashSet<>(Arrays.asList("A", "B", "C", "D"));
        if (!response.getEnhancedOptions().keySet().equals(expectedKeys)) {
          log.warn(
              "Validation: MCQ options keys don't match A,B,C,D: {}",
              response.getEnhancedOptions().keySet());
          errors.add("MCQ options must be labeled A, B, C, D");
        }
      }
    }

    // 3. Validate content is mathematics-related
    if (!isMathematicsContent(response.getEnhancedQuestionText())) {
      log.warn("Validation: Content doesn't seem to be mathematics-related");
      errors.add("Content contains non-mathematics material");
    }

    // 4. Validate question text is not empty
    if (response.getEnhancedQuestionText() == null
        || response.getEnhancedQuestionText().trim().isEmpty()) {
      log.warn("Validation: Enhanced question text is empty");
      errors.add("Enhanced question text is empty");
    }

    // 5. Validate no harmful/inappropriate content
    if (containsInappropriateContent(response.getEnhancedQuestionText())) {
      log.warn("Validation: Content contains inappropriate material");
      errors.add("Content contains inappropriate material");
    }

    response.setValidationErrors(errors);
    return errors.isEmpty();
  }

  @Override
  public AIEnhancedQuestionResponse testEnhancement(AIEnhancementRequest request) {
    return enhanceQuestion(request);
  }

  // Helper methods

  private String buildEnhancementPrompt(AIEnhancementRequest request) {
    StringBuilder prompt = new StringBuilder();

    // Strong “JSON only” instruction up front
    prompt.append("OUTPUT RULE: Return ONLY valid JSON. No markdown, no extra text.\n");
    prompt.append(
        "You are a mathematics education expert. Your task is to enhance this math question.\n\n");

    prompt.append("CRITICAL RULES (MUST FOLLOW):\n");
    prompt
        .append("1. DO NOT change the correct answer value - it must remain: ")
        .append(request.getCorrectAnswer())
        .append("\n");
    prompt.append("2. Keep all content strictly about mathematics\n");
    prompt.append(
        "3. For multiple choice questions, provide exactly 4 options labeled A, B, C, D\n");

    prompt.append("4. IMPORTANT: Option values MUST contain ONLY the numeric answer\n");
    prompt.append("   - ❌ WRONG: \"5 (assuming 8 = 10)\" or \"6 (ignoring the constant term)\"\n");
    prompt.append("   - ✅ CORRECT: \"5\" or \"6\" or \"3.14\"\n");
    prompt.append(
        "   - NO explanatory text, NO parentheses, NO units, NO words in option values\n");

    prompt.append(
        "5. Put all explanations in the 'distractorExplanations' field, NOT in option values\n");
    prompt.append(
        "6. Make distractors reflect common student mistakes but explain mistakes only in distractorExplanations\n");

    // New rule to prevent “Wait / self-correction / wrong steps”
    prompt.append("7. IMPORTANT: In 'explanation', include ONLY correct solution steps.\n");
    prompt.append(
        "   - DO NOT include self-talk or self-correction (e.g., \"Wait\", \"That's not correct\", \"Let's try again\")\n");
    prompt.append(
        "   - DO NOT include any incorrect intermediate work, even if you later correct it\n");
    prompt.append("   - Keep explanation concise: 2–5 sentences, only correct math steps\n");

    // Ensure correctAnswerKey matches the preserved correct answer value
    prompt.append(
        "8. correctAnswerKey MUST be the key whose option value equals the preserved correct answer value.\n\n");

    prompt.append("ORIGINAL QUESTION:\n");
    prompt.append(request.getRawQuestionText()).append("\n\n");

    prompt
        .append("CORRECT ANSWER VALUE (must be preserved exactly): ")
        .append(request.getCorrectAnswer())
        .append("\n\n");

    if (request.getQuestionType() == QuestionType.MULTIPLE_CHOICE
        && request.getRawOptions() != null) {
      prompt.append("ORIGINAL OPTIONS (notice they contain only numeric values):\n");
      request
          .getRawOptions()
          .forEach((key, value) -> prompt.append(key).append(". ").append(value).append("\n"));
      prompt.append("\n");
    }

    prompt.append("DIFFICULTY: ").append(request.getDifficulty()).append("\n");

    if (request.getContext() != null && !request.getContext().isBlank()) {
      prompt.append("CONTEXT: ").append(request.getContext()).append("\n");
    }

    prompt.append("\nYour task:\n");
    prompt.append("1. Rewrite the question with clearer, more natural Vietnamese wording\n");
    prompt.append("2. For MCQ: Create 4 options with ONLY numeric values (no text descriptions)\n");
    prompt.append(
        "3. Provide an explanation (2–5 sentences) with ONLY correct steps (no self-correction)\n");
    prompt.append(
        "4. (Optional) Suggest alternative solution methods (short bullet-style strings)\n");
    prompt.append(
        "5. Explain what mistake each wrong option represents in distractorExplanations\n\n");

    prompt.append("OUTPUT FORMAT (strict JSON only):\n");
    prompt.append("{\n");
    prompt.append("  \"enhancedQuestion\": \"Improved question text in Vietnamese\",\n");
    prompt.append("  \"options\": {\n");
    prompt.append("    \"A\": \"3.14\",\n");
    prompt.append("    \"B\": \"6.28\",\n");
    prompt.append("    \"C\": \"9.42\",\n");
    prompt.append("    \"D\": \"12.56\"\n");
    prompt.append("  },\n");
    prompt.append("  \"correctAnswerKey\": \"A\",\n");
    prompt.append(
        "  \"explanation\": \"Step-by-step solution in Vietnamese (2–5 sentences, only correct steps)\",\n");
    prompt.append(
        "  \"alternativeSolutions\": [\"Alternative method 1\", \"Alternative method 2\"],\n");
    prompt.append("  \"distractorExplanations\": {\n");
    prompt.append("    \"B\": \"Common mistake explanation (in Vietnamese)\",\n");
    prompt.append("    \"C\": \"Common mistake explanation (in Vietnamese)\",\n");
    prompt.append("    \"D\": \"Common mistake explanation (in Vietnamese)\"\n");
    prompt.append("  }\n");
    prompt.append("}\n\n");

    prompt.append("VALIDATION CHECKLIST (MUST SATISFY ALL):\n");
    prompt.append("✓ Output is ONLY JSON (no extra text)\n");
    prompt.append("✓ Exactly 4 options: A, B, C, D\n");
    prompt.append(
        "✓ Option values contain ONLY numbers (e.g., \"5\", \"3.14\", \"0.5\", \"-2\")\n");
    prompt.append("✓ Each option value must match regex: ^-?\\d+(\\.\\d+)?$\n");
    prompt.append(
        "✓ NO text in option values like \"(assuming...)\" or \"(ignoring...)\" or any words\n");
    prompt
        .append("✓ correctAnswerKey points to the option whose value equals: ")
        .append(request.getCorrectAnswer())
        .append("\n");
    prompt.append(
        "✓ Explanation is 2–5 sentences, ONLY correct steps, NO self-correction words (Wait/try again/not correct)\n");
    prompt.append("✓ All mistake descriptions are only in 'distractorExplanations'\n");

    return prompt.toString();
  }

  private AIEnhancedQuestionResponse parseAIResponse(
      String aiContent, AIEnhancementRequest request) {
    try {
      // Extract JSON from the response (AI might wrap it in markdown code blocks)
      String jsonContent = extractJSON(aiContent);
      log.debug("Extracted JSON:\n{}", jsonContent);

      JsonNode root = objectMapper.readTree(jsonContent);

      String enhancedQuestion = root.path("enhancedQuestion").asText();
      if (enhancedQuestion == null || enhancedQuestion.isBlank()) {
        log.warn("enhancedQuestion field is empty, using original question text");
        enhancedQuestion = request.getRawQuestionText();
      }

      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText(enhancedQuestion)
              .correctAnswerKey(root.path("correctAnswerKey").asText())
              .explanation(root.path("explanation").asText())
              .originalQuestionText(request.getRawQuestionText())
              .originalOptions(request.getRawOptions())
              .build();

      // Parse options
      if (root.has("options")) {
        Map<String, String> options = new LinkedHashMap<>();
        JsonNode optionsNode = root.get("options");
        Iterator<String> fieldNames = optionsNode.fieldNames();
        while (fieldNames.hasNext()) {
          String key = fieldNames.next();
          String rawValue = optionsNode.get(key).asText();

          String renderedValue = renderOptionValue(rawValue, request.getParameters());
          options.put(key, renderedValue);
        }
        response.setEnhancedOptions(options);
      }

      // Parse alternative solutions
      if (root.has("alternativeSolutions")) {
        List<String> alternatives = new ArrayList<>();
        root.get("alternativeSolutions").forEach(node -> alternatives.add(node.asText()));
        response.setAlternativeSolutions(alternatives);
      }

      // Parse distractor explanations
      if (root.has("distractorExplanations")) {
        Map<String, String> distractorExplanations = new HashMap<>();
        JsonNode distractorsNode = root.get("distractorExplanations");
        Iterator<String> fieldNames = distractorsNode.fieldNames();
        while (fieldNames.hasNext()) {
          String key = fieldNames.next();
          distractorExplanations.put(key, distractorsNode.get(key).asText());
        }
        response.setDistractorExplanations(distractorExplanations);
      }

      return response;

    } catch (Exception e) {
      log.error("Failed to parse AI response: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
    }
  }

  private String extractJSON(String content) {
    String jsonContent = content;

    // Try to extract JSON from markdown code blocks first
    Pattern pattern = Pattern.compile("```(?:json)?\\s*\\n?(.+?)```", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(content);

    if (matcher.find()) {
      jsonContent = matcher.group(1).trim();
      log.debug("Extracted JSON from code block");
    } else {
      // If no code block, try to find JSON object
      int startIdx = content.indexOf('{');
      int endIdx = content.lastIndexOf('}');

      if (startIdx >= 0 && endIdx > startIdx) {
        jsonContent = content.substring(startIdx, endIdx + 1);
        log.debug("Extracted JSON as substring from position {} to {}", startIdx, endIdx);
      } else {
        log.warn(
            "Could not find valid JSON structure in content. Raw content:\n{}",
            content.length() > 500 ? content.substring(0, 500) + "..." : content);
      }
    }

    // Sanitize JSON: escape unescaped control characters within string values
    // This handles cases where AI generates multi-line strings without proper escaping
    jsonContent = sanitizeJSON(jsonContent);

    return jsonContent;
  }

  private String sanitizeJSON(String json) {
    if (json == null) {
      return null;
    }

    // Replace unescaped newlines, tabs, and carriage returns in string values
    // We need to be careful not to replace them in already-escaped sequences
    StringBuilder result = new StringBuilder();
    boolean inString = false;
    boolean escaped = false;

    for (int i = 0; i < json.length(); i++) {
      char c = json.charAt(i);

      if (escaped) {
        result.append(c);
        escaped = false;
        continue;
      }

      if (c == '\\') {
        result.append(c);
        escaped = true;
        continue;
      }

      if (c == '"') {
        inString = !inString;
        result.append(c);
        continue;
      }

      if (inString) {
        // Inside a string, escape control characters
        switch (c) {
          case '\n':
            result.append("\\n");
            break;
          case '\r':
            result.append("\\r");
            break;
          case '\t':
            result.append("\\t");
            break;
          case '\b':
            result.append("\\b");
            break;
          case '\f':
            result.append("\\f");
            break;
          default:
            // Check for other control characters
            if (Character.isISOControl(c)) {
              result.append(String.format("\\u%04x", (int) c));
            } else {
              result.append(c);
            }
        }
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

  /**
   * Clean option value by removing explanatory text in parentheses. Examples: "5 (assuming 8 = 10)"
   * -> "5" "6 (ignoring the constant term)" -> "6" "3.14" -> "3.14"
   */
  private String cleanOptionValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      return value;
    }

    String normalizedValue = normalizeNumericLocale(value);

    // Remove anything in parentheses
    String cleaned = normalizedValue.replaceAll("\\s*\\([^)]*\\)", "").trim();

    // Remove any trailing non-numeric text (but keep decimal points and negative signs)
    cleaned = cleaned.replaceAll("^([+-]?\\d+\\.?\\d*).*", "$1").trim();

    // If result is empty or invalid, return original
    if (cleaned.isEmpty()) {
      log.warn(
          "cleanOptionValue resulted in empty string for input '{}', returning original", value);
      return value;
    }

    // Validate it's numeric
    try {
      Double.parseDouble(cleaned);
      return cleaned;
    } catch (NumberFormatException e) {
      log.warn(
          "cleanOptionValue could not parse '{}' (from '{}') as number, returning original",
          cleaned,
          value);
      return value;
    }
  }

  private boolean isSameAnswer(
      String originalAnswer,
      Map<String, String> originalOptions,
      String answerKey,
      Map<String, String> options) {
    if (answerKey == null || options == null) {
      return false;
    }

    String aiAnswer = options.get(answerKey);
    if (aiAnswer == null) {
      return false;
    }

    // If we have the original options, find the original answer key for better comparison
    String originalAnswerKey = null;
    if (originalOptions != null && originalAnswer != null) {
      // Find which key has the original answer value
      for (Map.Entry<String, String> entry : originalOptions.entrySet()) {
        if (entry.getValue() != null && entry.getValue().equals(originalAnswer)) {
          originalAnswerKey = entry.getKey();
          break;
        }
      }
    }

    // Try numeric comparison first
    try {
      double original = parseNumericAnswer(originalAnswer);
      double ai = parseNumericAnswer(aiAnswer);

      // Use epsilon comparison for floating point values
      // Consider answers equal if they're within 0.01 (for 2 decimal place precision)
      return Math.abs(original - ai) < 0.01;
    } catch (NumberFormatException e) {
      // Fall back to string comparison
      String normalized1 = normalizeAnswer(originalAnswer);
      String normalized2 = normalizeAnswer(aiAnswer);
      return normalized1.equals(normalized2);
    }
  }

  private double parseNumericAnswer(String answer) {
    if (answer == null) {
      throw new NumberFormatException("Answer is null");
    }
    // Extract just the numeric part
    String cleaned = normalizeNumericLocale(answer).replaceAll("[^0-9.-]", "");
    return Double.parseDouble(cleaned);
  }

  private String normalizeAnswer(String answer) {
    if (answer == null) {
      return "";
    }

    // Remove whitespace and convert to lowercase
    String normalized = answer.trim().toLowerCase();

    // Try to parse as number for numeric comparison
    try {
      double num =
          Double.parseDouble(normalizeNumericLocale(normalized).replaceAll("[^0-9.-]", ""));
      return formatDecimal(num, 2);
    } catch (NumberFormatException e) {
      return normalized;
    }
  }

  private boolean isMathematicsContent(String content) {
    if (content == null) {
      return false;
    }

    // Check for mathematics-related keywords and symbols
    String lowerContent = content.toLowerCase();

    // Math keywords
    boolean hasMathKeywords =
        lowerContent.matches(
            ".*\\b(calculate|solve|find|equals?|sum|difference|product|quotient|formula|equation|number|value|result|answer)\\b.*");

    // Math symbols
    boolean hasMathSymbols =
        content.matches(".*[+\\-*/=<>≤≥≠√∑∏∫].*") || content.matches(".*\\d+.*");

    // Not containing non-math subjects
    boolean noOtherSubjects =
        !lowerContent.matches(
            ".*(history|literature|biology|chemistry|physics|geography|art|music).*");

    return (hasMathKeywords || hasMathSymbols) && noOtherSubjects;
  }

  private boolean containsInappropriateContent(String content) {
    if (content == null) {
      return false;
    }

    String lowerContent = content.toLowerCase();

    // Basic inappropriate content detection
    String[] inappropriateKeywords = {
      "violence",
      "weapon",
      "drug",
      "alcohol",
      "sexual",
      "hate",
      "racist",
      "discrimination",
      "offensive"
    };

    for (String keyword : inappropriateKeywords) {
      if (lowerContent.contains(keyword)) {
        return true;
      }
    }

    return false;
  }

  private AIEnhancedQuestionResponse createFallbackResponse(
      AIEnhancementRequest request, List<String> errors) {

    return AIEnhancedQuestionResponse.builder()
        .enhancedQuestionText(request.getRawQuestionText())
        .enhancedOptions(request.getRawOptions())
        .correctAnswerKey(findCorrectAnswerKey(request.getRawOptions(), request.getCorrectAnswer()))
        .explanation("Based on formula: " + request.getAnswerFormula())
        .enhanced(false)
        .isValid(false)
        .validationErrors(errors)
        .originalQuestionText(request.getRawQuestionText())
        .originalOptions(request.getRawOptions())
        .build();
  }

  private String findCorrectAnswerKey(Map<String, String> options, String correctAnswer) {
    if (options == null || correctAnswer == null) {
      return "A";
    }

    String normalized = normalizeAnswer(correctAnswer);

    for (Map.Entry<String, String> entry : options.entrySet()) {
      if (normalizeAnswer(entry.getValue()).equals(normalized)) {
        return entry.getKey();
      }
    }

    return "A"; // Default fallback
  }

  // =====================================================================
  // LLM-based full question generation from template
  // =====================================================================

  @Override
  public GeneratedQuestionSample generateQuestion(QuestionTemplate template, int sampleIndex) {
    return generateQuestion(template, sampleIndex, null);
  }

  /**
   * Constraint-aware overload. When {@code presetParams} is supplied (non-null,
   * non-empty), the blueprint methods use those values verbatim — they were
   * picked by {@link com.fptu.math_master.service.BlueprintService#selectValueSets}
   * to satisfy the template's per-parameter {@code constraintText} and
   * {@code globalConstraints}. The legacy {@code pickParameters} sampler — which
   * still reads {@code min}/{@code max} keys that the V112-era parameter shape
   * no longer contains — is bypassed entirely.
   *
   * <p>When {@code presetParams} is null/empty we fall back to the legacy sampler
   * so callers without an AI tuple (test-template preview, lesson-content
   * generator) keep working.
   */
  @Override
  public GeneratedQuestionSample generateQuestion(
      QuestionTemplate template, int sampleIndex, Map<String, Object> presetParams) {
    log.info(
        "Generating question from template '{}' using LLM (sample #{}, preset={})",
        template.getName(),
        sampleIndex + 1,
        presetParams != null && !presetParams.isEmpty());

    switch (template.getTemplateType()) {
      case SHORT_ANSWER:
        return generateShortAnswerQuestion(template, sampleIndex, presetParams);
      case TRUE_FALSE:
        return generateTrueFalseQuestion(template, sampleIndex, presetParams);
      case MULTIPLE_CHOICE:
      default:
        return generateMultipleChoiceQuestion(template, sampleIndex, presetParams);
    }
  }

  /**
   * Returns {@code presetParams} when it carries values (constraint-aware path)
   * and otherwise calls the legacy random sampler. Centralised so the three
   * blueprint methods can't accidentally bypass the AI tuple.
   */
  private Map<String, Object> resolveParameters(
      QuestionTemplate template, int sampleIndex, Map<String, Object> presetParams) {
    if (presetParams != null && !presetParams.isEmpty()) {
      return new LinkedHashMap<>(presetParams);
    }
    return pickParameters(template, sampleIndex);
  }

  /**
   * Generate a MULTIPLE_CHOICE question from template (existing MCQ flow)
   */
  private GeneratedQuestionSample generateMultipleChoiceQuestion(
      QuestionTemplate template, int sampleIndex, Map<String, Object> presetParams) {
    log.info("Generating MCQ from template '{}' (sample #{})", template.getName(), sampleIndex + 1);

    // Step 1: resolve parameter values. When the BlueprintService AI selector
    // already gave us a constraint-respecting tuple, use it verbatim; otherwise
    // fall back to the legacy random sampler.
    Map<String, Object> params = resolveParameters(template, sampleIndex, presetParams);
    String correctAnswerStr = evaluateFormula(template.getAnswerFormula(), params);

    // DEBUG: Log what we computed
    log.info(
        "Template '{}' computed answer: '{}' from formula: '{}'",
        template.getName(),
        correctAnswerStr,
        template.getAnswerFormula());
    log.info("  Parameters: {}", params);

    // If answer is still '?', something is wrong with formula evaluation
    if ("?".equals(correctAnswerStr)) {
      log.error(
          "CRITICAL: Formula evaluation returned '?'. Formula: '{}', Template: '{}'",
          template.getAnswerFormula(),
          template.getName());
      // Create minimal fallback
      return GeneratedQuestionSample.builder()
          .questionText("Template error: Cannot evaluate formula")
          .options(Map.of("A", "Unable to generate"))
          .correctAnswer("A")
          .explanation("Error: Formula evaluation failed")
          .solutionSteps("Error: Formula evaluation failed")
          .diagramData(template.getDiagramTemplate())
          .calculatedDifficulty(QuestionDifficulty.MEDIUM)
          .usedParameters(params)
          .answerCalculation("Error: " + template.getAnswerFormula())
          .build();
    }

    QuestionDifficulty difficulty = determineDifficulty(null, params);
    String questionTextBase = fillTemplateText(template.getTemplateText(), params);

    // Step 2a: deterministic short-circuit. If optionsGenerator holds 4 fully
    // evaluable formulas keyed A-D, compute them ourselves — no LLM call,
    // no LaTeX rendering surprises, distractors are exactly the pedagogical
    // mistakes the template author encoded.
    GeneratedQuestionSample deterministic =
        tryGenerateMcqFromOptionsGenerator(
            template, params, correctAnswerStr, questionTextBase, difficulty);
    if (deterministic != null) {
      return deterministic;
    }

    try {
      // Step 2b: fall back to LLM for question wording + 3 distractors
      String prompt =
          buildGenerationPrompt(template, params, correctAnswerStr, questionTextBase, sampleIndex);
      String aiContent = geminiService.sendMessage(prompt);
      return parseGeneratedQuestion(
          aiContent, template, params, correctAnswerStr, difficulty, questionTextBase);
    } catch (Exception e) {
      log.error(
          "LLM call failed for template '{}', returning Java-computed fallback: {}",
          template.getName(),
          e.getMessage());
      // Return a valid question using only Java-computed values (no LLM dependency)
      Map<String, String> fallbackOptions = buildFallbackOptions(correctAnswerStr, params);
      String correctKey = findKeyByValue(fallbackOptions, correctAnswerStr);
      if (correctKey == null) correctKey = "A";
      return GeneratedQuestionSample.builder()
          .questionText(questionTextBase)
          .options(fallbackOptions)
          .correctAnswer(correctKey) // KEY (A/B/C/D)
          .explanation(
              "Áp dụng công thức: " + template.getAnswerFormula() + " = " + correctAnswerStr)
          .solutionSteps(
              "Áp dụng công thức: " + template.getAnswerFormula() + " = " + correctAnswerStr)
          .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), params))
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(template.getAnswerFormula() + " = " + correctAnswerStr)
          .build();
    }
  }

  /**
   * If {@code optionsGenerator} contains 4 evaluable formulas keyed A/B/C/D,
   * compute each one and return a fully-formed {@link GeneratedQuestionSample}
   * with consistent numeric options — no LLM call needed.
   *
   * <p>Returns {@code null} when the template either has no
   * {@code optionsGenerator}, doesn't have all 4 keys, has any non-string /
   * blank value, or any formula fails to evaluate. The caller falls back to
   * the existing LLM path in those cases.
   *
   * <p>This is the deterministic guarantee that pedagogical-distractor
   * formulas in the template (forgot-base, wrong-radius, h-as-slant, …)
   * actually appear in the rendered question. The previous flow handed
   * those formulas to Gemini as a "style hint" — the AI improvised LaTeX
   * that often broke KaTeX rendering downstream.
   */
  private GeneratedQuestionSample tryGenerateMcqFromOptionsGenerator(
      QuestionTemplate template,
      Map<String, Object> params,
      String correctAnswerStr,
      String questionTextBase,
      QuestionDifficulty difficulty) {
    Map<String, Object> generator = template.getOptionsGenerator();
    if (generator == null || generator.isEmpty()) return null;

    String[] keys = {"A", "B", "C", "D"};
    Map<String, String> options = new LinkedHashMap<>();
    for (String key : keys) {
      Object raw = generator.get(key);
      if (!(raw instanceof String formula) || formula.isBlank()) {
        return null;
      }
      String evaluated = evaluateFormula(formula, params);
      if (evaluated == null || "?".equals(evaluated)) {
        log.debug(
            "Deterministic MCQ skipped for template '{}': option {} formula '{}' did not evaluate",
            template.getName(),
            key,
            formula);
        return null;
      }
      options.put(key, evaluated);
    }

    String correctKey = findKeyByValue(options, correctAnswerStr);
    if (correctKey == null) {
      log.warn(
          "Deterministic MCQ for template '{}': no option matched correctAnswer '{}' from"
              + " optionsGenerator values {}. Falling back to LLM.",
          template.getName(),
          correctAnswerStr,
          options);
      return null;
    }

    // Render solution / explanation: prefer template's solutionStepsTemplate
    // with strict {{param}} substitution (LaTeX braces preserved). The
    // deterministic substitution is the "ground truth" — guaranteed correct
    // numbers, no AI hallucination. We then OPTIONALLY ask Gemini to polish
    // the prose: expand each step with the computed value and add reasoning,
    // strictly preserving the answer.
    String rawSteps = renderSolutionSteps(template, params);
    String headline =
        "Áp dụng công thức "
            + (template.getAnswerFormula() == null ? "" : template.getAnswerFormula() + " ")
            + "với các tham số đã cho, đáp án bằng "
            + correctAnswerStr
            + ".";
    String diagram = renderDiagramTemplate(template.getDiagramTemplate(), params);

    PolishedSolution polished =
        polishExplanationWithAI(
            template, params, correctAnswerStr, questionTextBase, rawSteps, options);

    String explanation;
    String solutionSteps;
    if (polished != null) {
      explanation = polished.explanation;
      solutionSteps = polished.solutionSteps;
    } else {
      solutionSteps = rawSteps != null ? rawSteps : headline;
      explanation =
          rawSteps != null && !rawSteps.isBlank() ? headline + "\n\n" + rawSteps : headline;
    }

    log.info(
        "Deterministic MCQ generated for template '{}': correctKey={}, options={}, polished={}",
        template.getName(),
        correctKey,
        options,
        polished != null);

    return GeneratedQuestionSample.builder()
        .questionText(questionTextBase)
        .options(new LinkedHashMap<>(options))
        .correctAnswer(correctKey)
        .explanation(explanation)
        .solutionSteps(solutionSteps)
        .diagramData(diagram)
        .calculatedDifficulty(difficulty)
        .usedParameters(params)
        .answerCalculation(template.getAnswerFormula() + " = " + correctAnswerStr)
        .build();
  }

  /** Holder for the AI-polished prose; both fields non-null when this is returned. */
  private record PolishedSolution(String explanation, String solutionSteps) {}

  /**
   * Send Gemini the deterministic stem + answer + raw substituted steps and
   * ask it to enrich the prose. The AI is forbidden from changing the answer,
   * the options, or any computed numbers — its only job is to expand the
   * reasoning and tighten the LaTeX.
   *
   * <p>Returns {@code null} if the polish fails for any reason (no AI service
   * configured, network error, invalid JSON, validation failure). Caller
   * falls back to the deterministic prose.
   */
  private PolishedSolution polishExplanationWithAI(
      QuestionTemplate template,
      Map<String, Object> params,
      String correctAnswerStr,
      String stem,
      String rawSteps,
      Map<String, String> options) {
    if (rawSteps == null || rawSteps.isBlank()) {
      // Nothing to polish; deterministic headline is fine.
      return null;
    }
    if (geminiService == null) {
      return null;
    }
    try {
      String prompt = buildPolishPrompt(template, params, correctAnswerStr, stem, rawSteps, options);
      String aiContent = geminiService.sendMessage(prompt);
      String json = repairTruncatedJson(extractJSON(aiContent));
      JsonNode root = objectMapper.readTree(json);

      String aiExplanation = root.path("explanation").asText();
      String aiSteps = root.path("solutionSteps").asText();
      if (aiExplanation == null || aiExplanation.isBlank()) return null;
      if (aiSteps == null || aiSteps.isBlank()) return null;

      // Substitute any leftover {{param}} (the AI was told to keep computed
      // values, but if it left a placeholder we render it with strict double
      // brace — never destroying LaTeX braces).
      aiExplanation = fillStrictDoubleBrace(aiExplanation, params);
      aiSteps = fillStrictDoubleBrace(aiSteps, params);

      if (!isPolishOutputValid(aiExplanation, aiSteps, correctAnswerStr)) {
        log.warn(
            "AI polish output failed validation for template '{}': "
                + "answer not present, braces unbalanced, or contains 'null'. "
                + "Falling back to deterministic prose.",
            template.getName());
        return null;
      }
      return new PolishedSolution(aiExplanation, aiSteps);
    } catch (Exception e) {
      log.warn(
          "AI polish failed for template '{}': {}. Falling back to deterministic prose.",
          template.getName(),
          e.getMessage());
      return null;
    }
  }

  private String buildPolishPrompt(
      QuestionTemplate template,
      Map<String, Object> params,
      String correctAnswerStr,
      String stem,
      String rawSteps,
      Map<String, String> options) {
    StringBuilder p = new StringBuilder();
    p.append("Return ONLY a JSON object. No markdown, no prose outside JSON.\n\n");
    p.append(
        "Task: Polish a math solution. The numbers are ALREADY correct — your job is to enrich"
            + " the prose, expand each step with the computed value, and tighten the LaTeX.\n\n");
    p.append("Question stem: ").append(stem).append("\n");
    p.append("Parameters used: ").append(serializeParamsForPrompt(params)).append("\n");
    if (template.getAnswerFormula() != null && !template.getAnswerFormula().isBlank()) {
      p.append("Source formula: ").append(template.getAnswerFormula()).append("\n");
    }
    p.append("Correct answer (LOCKED): ").append(correctAnswerStr).append("\n");
    p.append("Options (LOCKED, do NOT change values):\n");
    for (Map.Entry<String, String> e : options.entrySet()) {
      p.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
    }
    p.append("\nRaw substituted solution steps (already correct, but mechanical):\n");
    p.append(rawSteps).append("\n\n");

    p.append("Polish rules — depth and rigour matter MORE than brevity:\n");
    p.append(
        "P1. Keep all numeric values EXACTLY as they appear. Do not recompute. The final answer must remain ")
        .append(correctAnswerStr)
        .append(".\n");
    p.append(
        "P2. For EACH Bước you must include all four pieces in this order:\n"
            + "    (a) Name the concept / formula being applied (e.g. \"Diện tích tam giác đều cạnh $a$ là\");\n"
            + "    (b) Write the formula in symbolic form ($S = \\dfrac{\\sqrt{3}}{4} a^2$);\n"
            + "    (c) Show the substitution with the actual numbers ($S = \\dfrac{\\sqrt{3}}{4} \\cdot 2^2$);\n"
            + "    (d) Give the simplified intermediate result ($= \\sqrt{3}$ cm²).\n");
    p.append(
        "P3. Add 1-2 short reasoning sentences per step explaining WHY the step is valid"
            + " (vì đáy là tam giác đều, vì $SO$ vuông góc đáy nên áp dụng Pythagoras...). Cite the"
            + " geometric / algebraic property by name when relevant.\n");
    p.append(
        "P4. Vietnamese with proper accents (UTF-8). Be SUBSTANTIVE, not telegraphic — at least"
            + " 2 sentences per Bước. AVOID vague openings like \"Để tính X chúng ta cần ...\""
            + " when the student already knows what X is.\n");
    p.append(
        "P5. The `explanation` field must be a 4-7 sentence WALK-THROUGH that names every"
            + " formula used, shows the SUBSTITUTED numerical chain leading to the answer, and"
            + " ends with the final value. It is NOT a high-level summary — it is a complete"
            + " worked solution in paragraph form. Include at least 2 inline LaTeX expressions"
            + " ($...$) that show actual numbers, not symbols.\n");
    p.append(
        "P6. The `solutionSteps` field is the same content broken into numbered Bước, formatted"
            + " as a single string with literal \\n separators.\n");
    p.append(
        "P7. NEVER write \"trong đó X được xác định bằng Y\" without then computing Y. Compute"
            + " every intermediate quantity to a number.\n\n");

    p.append("STYLE EXAMPLE — explanation field:\n");
    p.append(
        "  ❌ SHALLOW (do NOT do this): \"Để tính diện tích toàn phần, ta cần diện tích đáy và"
            + " diện tích xung quanh. Diện tích đáy là diện tích tam giác đều. Diện tích xung"
            + " quanh tính bằng nửa chu vi nhân trung đoạn.\"\n");
    p.append(
        "  ✅ DETAILED (target this): \"Khối chóp tam giác đều $S.ABC$ có diện tích toàn phần"
            + " bằng diện tích đáy cộng diện tích xung quanh. Diện tích đáy là tam giác đều cạnh"
            + " $a = 2$ nên $S_{\\text{đáy}} = \\dfrac{\\sqrt{3}}{4} \\cdot 2^2 = \\sqrt{3}$"
            + " cm². Bán kính nội tiếp đáy là $r = \\dfrac{a}{2\\sqrt{3}} = \\dfrac{1}{\\sqrt{3}}$"
            + " cm; áp dụng Pythagoras trong tam giác $SOM$ vuông tại $O$, trung đoạn"
            + " $SM = \\sqrt{SO^2 + r^2} = \\sqrt{3^2 + \\dfrac{1}{3}} = \\sqrt{\\dfrac{28}{3}}$"
            + " cm. Diện tích xung quanh $S_{xq} = \\dfrac{1}{2} \\cdot 3a \\cdot SM = 3 \\cdot"
            + " \\sqrt{\\dfrac{28}{3}}$ cm². Cộng lại $S_{tp} = \\sqrt{3} + 3\\sqrt{\\dfrac{28}{3}}"
            + " \\approx ").append(correctAnswerStr).append(" cm².\"\n\n");

    appendLatexStrictnessRules(p, correctAnswerStr);

    p.append("JSON format:\n");
    p.append(
        "{\"explanation\":\"<4-7 sentence detailed worked walk-through with substituted numbers"
            + " and explicit LaTeX>\",\"solutionSteps\":\"Bước 1: <name>. <formula>. <substitution>."
            + " <result>. <reasoning>.\\nBước 2: ...\\nBước 3: ...\"}\n");
    return p.toString();
  }

  private boolean isPolishOutputValid(String explanation, String steps, String expectedAnswer) {
    if (explanation == null || steps == null) return false;
    String combined = explanation + "\n" + steps;
    // Forbid the literal "null" (catches answerFormula leakage from any
    // future regression).
    if (combined.toLowerCase().contains(" null ")
        || combined.toLowerCase().contains("=null")
        || combined.toLowerCase().contains("= null")) {
      return false;
    }
    // Brace balance — a quick sanity check; KaTeX requires balanced { }.
    int braceDepth = 0;
    for (int i = 0; i < combined.length(); i++) {
      char c = combined.charAt(i);
      if (c == '{') braceDepth++;
      else if (c == '}') braceDepth--;
      if (braceDepth < 0) return false;
    }
    if (braceDepth != 0) return false;
    // $...$ pairing — odd count of unescaped $ means a math block was left open.
    int dollarCount = 0;
    for (int i = 0; i < combined.length(); i++) {
      if (combined.charAt(i) == '$' && (i == 0 || combined.charAt(i - 1) != '\\')) {
        dollarCount++;
      }
    }
    if (dollarCount % 2 != 0) return false;
    // Reject shallow output: explanation must contain at least 2 inline
    // math expressions ($...$) to count as a "worked walk-through". A polish
    // that loses all the substituted numbers is no better than the
    // deterministic raw steps.
    int explanationDollars = 0;
    for (int i = 0; i < explanation.length(); i++) {
      if (explanation.charAt(i) == '$' && (i == 0 || explanation.charAt(i - 1) != '\\')) {
        explanationDollars++;
      }
    }
    if (explanationDollars < 4) {
      // need at least 2 paired $...$ blocks → 4 unescaped $
      return false;
    }
    // Steps must have at least 2 line breaks (3 numbered items minimum).
    if (steps.split("\\n").length < 3) {
      return false;
    }
    // Answer presence — accept partial match (numeric tolerance is hard at
    // string level, so we only require the integer portion to appear).
    if (expectedAnswer != null && !expectedAnswer.isBlank()) {
      String firstNum = expectedAnswer.split("[^0-9.\\-]", 2)[0];
      if (!firstNum.isBlank() && !combined.contains(firstNum)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Render {@code solutionStepsTemplate} using strict double-brace
   * substitution. Unlike {@link #fillText}, this does NOT touch single
   * {@code {...}} groupings — critical for LaTeX content where
   * {@code \dfrac{\sqrt{3}}{4}} contains brace pairs that the legacy
   * {@code PLACEHOLDER_PATTERN} would mistakenly consume.
   *
   * <p>After parameter substitution, evaluate inline arithmetic blocks like
   * {@code $3+1$} → {@code $4$} so steps display computed values, not
   * unsimplified expressions.
   */
  private String renderSolutionSteps(QuestionTemplate template, Map<String, Object> params) {
    String tpl = template.getSolutionStepsTemplate();
    if (tpl == null || tpl.isBlank()) return null;
    String filled = fillStrictDoubleBrace(tpl, params);
    return evaluateInlineArithmetic(filled, params);
  }

  /**
   * Replace ONLY {@code {{name}}} placeholders. Single {@code {...}}
   * groupings are left untouched so LaTeX expressions like
   * {@code \dfrac{\sqrt{3}}{4}} survive intact.
   */
  private String fillStrictDoubleBrace(String raw, Map<String, Object> params) {
    if (raw == null || params == null || params.isEmpty()) return raw;
    String rendered = raw;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String key = entry.getKey();
      if (key == null || key.isBlank()) continue;
      String token = "{{" + key + "}}";
      String value = formatParameterValue(entry.getValue());
      rendered = rendered.replace(token, value);
    }
    return rendered;
  }

  /**
   * Generate a SHORT_ANSWER question from template
   * Flow: Resolve parameters → Substitute into template → Evaluate formula → Return answer
   */
  private GeneratedQuestionSample generateShortAnswerQuestion(
      QuestionTemplate template, int sampleIndex, Map<String, Object> presetParams) {
    log.info(
        "Generating SHORT_ANSWER from template '{}' (sample #{})",
        template.getName(),
        sampleIndex + 1);

    try {
      // Step 1: Resolve parameters (preset from AI selector, or fall back to sampler)
      Map<String, Object> params = resolveParameters(template, sampleIndex, presetParams);
      log.info("Resolved parameters: {}", params);

      // Step 2: Substitute into template text
      String questionText = fillTemplateText(template.getTemplateText(), params);
      log.info("Question text: {}", questionText);

      // Step 3: Evaluate answer formula
      String correctAnswer = evaluateFormula(template.getAnswerFormula(), params);
      log.info("Correct answer: {}", correctAnswer);

      if ("?".equals(correctAnswer)) {
        log.error("Formula evaluation failed for template: {}", template.getName());
        correctAnswer = "Unable to calculate";
      }

      // Step 4: Determine difficulty
      QuestionDifficulty difficulty = determineDifficulty(null, params);

      // Step 5: Get validation mode from metadata (default to EXACT)
      Map<String, Object> gradingMetadata = new HashMap<>();
      gradingMetadata.put("answerValidationMode", "EXACT");
      gradingMetadata.put("answerTolerance", 0.001);

      // Step 6: Call AI to generate explanation and solution steps
      try {
        String prompt =
            buildGenerationPrompt(template, params, correctAnswer, questionText, sampleIndex);
        String aiContent = geminiService.sendMessage(prompt);
        return parseGeneratedQuestion(
            aiContent, template, params, correctAnswer, difficulty, questionText);
      } catch (Exception aiError) {
        log.error(
            "AI generation failed for SHORT_ANSWER, using fallback: {}", aiError.getMessage());
        // Fallback to basic explanation if AI fails
        return GeneratedQuestionSample.builder()
            .questionText(questionText)
            .options(null) // No options for SHORT_ANSWER
            .correctAnswer(correctAnswer)
            .explanation(
                "Áp dụng công thức: " + template.getAnswerFormula() + " = " + correctAnswer)
            .solutionSteps(
                "Áp dụng công thức: " + template.getAnswerFormula() + " = " + correctAnswer)
            .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), params))
            .calculatedDifficulty(difficulty)
            .usedParameters(params)
            .answerCalculation(template.getAnswerFormula() + " = " + correctAnswer)
            .generationMetadata(gradingMetadata)
            .build();
      }

    } catch (Exception e) {
      log.error("Failed to generate SHORT_ANSWER question: {}", e.getMessage(), e);
      return GeneratedQuestionSample.builder()
          .questionText("Error generating question: " + e.getMessage())
          .options(null)
          .correctAnswer("Error")
          .explanation("Failed to generate question")
          .solutionSteps("Error: " + e.getMessage())
          .calculatedDifficulty(QuestionDifficulty.MEDIUM)
          .usedParameters(new HashMap<>())
          .build();
    }
  }

  /**
   * Generate a TRUE_FALSE question from template
   * Flow: Resolve parameters → Generate 4 clauses → Determine true/false for each → Return question
   */
  private GeneratedQuestionSample generateTrueFalseQuestion(
      QuestionTemplate template, int sampleIndex, Map<String, Object> presetParams) {
    log.info(
        "Generating TRUE_FALSE from template '{}' (sample #{})",
        template.getName(),
        sampleIndex + 1);

    try {
      // Step 1: Resolve parameters (preset from AI selector, or fall back to sampler)
      Map<String, Object> params = resolveParameters(template, sampleIndex, presetParams);
      log.info("Resolved parameters: {}", params);

      // Step 2: Substitute into template text (stem)
      String questionStem = fillTemplateText(template.getTemplateText(), params);
      log.info("Question stem: {}", questionStem);

      // Step 3: Get clause templates from statementMutations
      Map<String, Object> statementMutations =
          template.getStatementMutations() != null
              ? template.getStatementMutations()
              : new HashMap<>();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> clauseTemplates =
          (List<Map<String, Object>>)
              statementMutations.getOrDefault("clauseTemplates", new ArrayList<>());

      if (clauseTemplates.isEmpty()) {
        log.warn("No clause templates found for TRUE_FALSE question");
        clauseTemplates = generateDefaultClauseTemplates();
      }

      // Step 4: Generate clauses and determine true/false
      Map<String, String> clauses = new LinkedHashMap<>();
      Map<String, Object> tfClauses = new LinkedHashMap<>();
      Set<String> trueKeys = new HashSet<>();

      for (int i = 0; i < Math.min(4, clauseTemplates.size()); i++) {
        @SuppressWarnings("unchecked")
        Map<String, Object> clauseTemplate = clauseTemplates.get(i);
        String key = String.valueOf((char) ('A' + i));

        // Substitute parameters in clause text, then evaluate any inline
        // arithmetic blocks ($...$) so a clause like
        // "f(0) = $2*0 + 3$" displays as "f(0) = $3$" instead of leaving
        // the un-simplified expression visible to students.
        // Use strict double-brace substitution so LaTeX braces inside the
        // clause (e.g. $\dfrac{a}{b}$) are preserved.
        String clauseText = (String) clauseTemplate.get("text");
        if (clauseText != null) {
          clauseText = fillStrictDoubleBrace(clauseText, params);
          clauseText = evaluateInlineArithmetic(clauseText, params);
        } else {
          clauseText = "Clause " + key;
        }

        clauses.put(key, clauseText);

        // Store clause metadata
        @SuppressWarnings("unchecked")
        Map<String, Object> clauseMeta = new LinkedHashMap<>();
        clauseMeta.put("chapterId", clauseTemplate.get("chapterId"));
        clauseMeta.put("cognitiveLevel", clauseTemplate.get("cognitiveLevel"));
        tfClauses.put(key, clauseMeta);

        // Track true clauses
        Boolean truthValue = (Boolean) clauseTemplate.get("truthValue");
        if (truthValue != null && truthValue) {
          trueKeys.add(key);
        }
      }

      // Step 5: Determine difficulty
      QuestionDifficulty difficulty = determineDifficulty(null, params);

      // Step 6: Build metadata for grading
      Map<String, Object> gradingMetadata = new HashMap<>();
      gradingMetadata.put("tfClauses", tfClauses);

      // Step 7: Build correct answer (comma-separated true keys)
      String correctAnswer = String.join(",", trueKeys);
      if (correctAnswer.isEmpty()) {
        correctAnswer = "A"; // Default if no true clauses
      }

      // Step 8: Call AI to generate explanation and solution steps
      try {
        String prompt =
            buildGenerationPrompt(template, params, correctAnswer, questionStem, sampleIndex);
        String aiContent = geminiService.sendMessage(prompt);
        GeneratedQuestionSample aiGenerated =
            parseGeneratedQuestion(
                aiContent, template, params, correctAnswer, difficulty, questionStem);

        // Use AI-generated explanation and solution steps, but keep our clauses and correct answer
        return GeneratedQuestionSample.builder()
            .questionText(questionStem)
            .options(clauses) // Use our generated clauses, not AI's
            .correctAnswer(correctAnswer) // Use our computed correct answer
            .explanation(aiGenerated.getExplanation()) // Use AI explanation
            .solutionSteps(aiGenerated.getSolutionSteps()) // Use AI solution steps
            .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), params))
            .calculatedDifficulty(difficulty)
            .usedParameters(params)
            .answerCalculation(
                "Các mệnh đề đúng: "
                    + (trueKeys.isEmpty() ? "Không có" : String.join(", ", trueKeys)))
            .generationMetadata(gradingMetadata)
            .build();
      } catch (Exception aiError) {
        log.error("AI generation failed for TRUE_FALSE, using fallback: {}", aiError.getMessage());
        // Fallback to basic explanation if AI fails
        return GeneratedQuestionSample.builder()
            .questionText(questionStem)
            .options(clauses)
            .correctAnswer(correctAnswer)
            .explanation(
                "Xét các mệnh đề: "
                    + String.join(
                        ", ", trueKeys.isEmpty() ? List.of("Không có mệnh đề nào đúng") : trueKeys))
            .solutionSteps("Phân tích từng mệnh đề dựa trên các tính chất toán học")
            .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), params))
            .calculatedDifficulty(difficulty)
            .usedParameters(params)
            .answerCalculation(
                "Các mệnh đề đúng: "
                    + (trueKeys.isEmpty() ? "Không có" : String.join(", ", trueKeys)))
            .generationMetadata(gradingMetadata)
            .build();
      }

    } catch (Exception e) {
      log.error("Failed to generate TRUE_FALSE question: {}", e.getMessage(), e);
      return GeneratedQuestionSample.builder()
          .questionText("Error generating question: " + e.getMessage())
          .options(Map.of("A", "Error", "B", "Error", "C", "Error", "D", "Error"))
          .correctAnswer("A")
          .explanation("Failed to generate question")
          .solutionSteps("Error: " + e.getMessage())
          .calculatedDifficulty(QuestionDifficulty.MEDIUM)
          .usedParameters(new HashMap<>())
          .build();
    }
  }

  /**
   * Generate default clause templates if none are provided
   */
  private List<Map<String, Object>> generateDefaultClauseTemplates() {
    List<Map<String, Object>> clauses = new ArrayList<>();
    clauses.add(
        Map.of(
            "text", "Mệnh đề A",
            "truthValue", true,
            "chapterId", "default",
            "cognitiveLevel", "THONG_HIEU"));
    clauses.add(
        Map.of(
            "text", "Mệnh đề B",
            "truthValue", false,
            "chapterId", "default",
            "cognitiveLevel", "THONG_HIEU"));
    clauses.add(
        Map.of(
            "text", "Mệnh đề C",
            "truthValue", true,
            "chapterId", "default",
            "cognitiveLevel", "VAN_DUNG"));
    clauses.add(
        Map.of(
            "text", "Mệnh đề D",
            "truthValue", false,
            "chapterId", "default",
            "cognitiveLevel", "VAN_DUNG"));
    return clauses;
  }

  @Override
  public GeneratedQuestionSample generateQuestionFromCanonical(
      CanonicalQuestion canonicalQuestion, QuestionTemplate template, int sampleIndex) {
    if (canonicalQuestion == null) {
      return generateQuestion(template, sampleIndex);
    }

    Map<String, Object> params = pickParameters(template, sampleIndex);
    QuestionDifficulty difficulty = determineDifficulty(null, params);

    String canonicalProblem =
        canonicalQuestion.getProblemText() != null
            ? fillStrictDoubleBrace(canonicalQuestion.getProblemText(), params)
            : null;
    String canonicalSolution =
        canonicalQuestion.getSolutionSteps() != null
            ? fillStrictDoubleBrace(canonicalQuestion.getSolutionSteps(), params)
            : null;
    String diagramData =
        canonicalQuestion.getDiagramDefinition() != null
            ? renderDiagramTemplate(canonicalQuestion.getDiagramDefinition(), params)
            : renderDiagramTemplate(template.getDiagramTemplate(), params);

    String fallbackQuestionText =
        canonicalProblem != null && !canonicalProblem.isBlank()
            ? canonicalProblem
            : fillTemplateText(template.getTemplateText(), params);
    String fallbackExplanation =
        canonicalSolution != null && !canonicalSolution.isBlank()
            ? canonicalSolution
            : "AI-generated explanation from canonical source";

    try {
      String prompt =
          buildCanonicalGenerationPrompt(
              canonicalQuestion,
              template,
              params,
              fallbackQuestionText,
              fallbackExplanation,
              sampleIndex);
      String aiContent = geminiService.sendMessage(prompt);
      return parseCanonicalGeneratedQuestion(
          aiContent,
          template,
          params,
          difficulty,
          fallbackQuestionText,
          fallbackExplanation,
          diagramData);
    } catch (Exception e) {
      log.error(
          "Canonical generation failed for canonical '{}' with template '{}': {}",
          canonicalQuestion.getId(),
          template.getId(),
          e.getMessage());

      Map<String, String> fallbackOptions = buildCanonicalFallbackOptions(template, params);
      String fallbackCorrectAnswer =
          template.getTemplateType() == QuestionType.MULTIPLE_CHOICE
              ? "A"
              : (template.getAnswerFormula() != null ? template.getAnswerFormula() : "N/A");

      return GeneratedQuestionSample.builder()
          .questionText(fallbackQuestionText)
          .options(
              template.getTemplateType() == QuestionType.MULTIPLE_CHOICE ? fallbackOptions : null)
          .correctAnswer(fallbackCorrectAnswer)
          .explanation(fallbackExplanation)
          .solutionSteps(fallbackExplanation)
          .diagramData(diagramData)
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(template.getAnswerFormula())
          .build();
    }
  }

  private String buildCanonicalGenerationPrompt(
      CanonicalQuestion canonicalQuestion,
      QuestionTemplate template,
      Map<String, Object> params,
      String canonicalProblem,
      String canonicalSolution,
      int sampleIndex) {
    StringBuilder p = new StringBuilder();
    p.append("Return ONLY valid JSON. No markdown, no extra text.\n\n");
    p.append("You are a Vietnamese math teacher assistant.\n");
    p.append("Generate ONE new question inspired by canonical problem and solution.\n");
    p.append(
        "Keep mathematical consistency with canonical source, but wording can vary naturally.\n\n");

    p.append("CANONICAL PROBLEM:\n").append(canonicalProblem).append("\n\n");
    p.append("CANONICAL SOLUTION:\n").append(canonicalSolution).append("\n\n");

    if (canonicalQuestion.getDiagramDefinition() != null
        && !canonicalQuestion.getDiagramDefinition().isBlank()) {
      p.append("CANONICAL DIAGRAM (optional):\n")
          .append(canonicalQuestion.getDiagramDefinition())
          .append("\n\n");
    }

    p.append("QUESTION TYPE: ").append(template.getTemplateType()).append("\n");
    p.append("SAMPLE INDEX: ").append(sampleIndex + 1).append("\n");
    p.append("PARAMETERS (backend picked): ")
        .append(serializeParamsForPrompt(params))
        .append("\n\n");

    p.append("Rules:\n");
    p.append("1. Output strictly in Vietnamese with natural wording.\n");
    p.append("2. Keep question mathematically aligned with canonical problem/solution.\n");
    if (template.getTemplateType() == QuestionType.MULTIPLE_CHOICE) {
      p.append("3. Provide exactly 4 options: A, B, C, D.\n");
      p.append("4. correctAnswer must be one key among A/B/C/D.\n");
      p.append("5. Ensure one and only one correct option.\n");
    } else {
      p.append("3. For non-MCQ, options can be empty object {}.\n");
      p.append("4. correctAnswer should be the final answer text.\n");
    }
    p.append("6. explanation and solutionSteps should be concise, correct, no self-correction.\n");
    p.append("7. You may provide usedParameters to override placeholders if needed.\n\n");

    p.append("JSON format:\n");
    p.append("{");
    p.append("\"questionText\":\"...\",");
    p.append("\"options\":{\"A\":\"...\",\"B\":\"...\",\"C\":\"...\",\"D\":\"...\"},");
    p.append("\"correctAnswer\":\"A\",");
    p.append("\"explanation\":\"...\",");
    p.append("\"solutionSteps\":\"...\",");
    p.append("\"difficulty\":\"EASY|MEDIUM|HARD\",");
    p.append("\"usedParameters\":{},");
    p.append("\"answerCalculation\":\"...\"");
    p.append("}\n");

    return p.toString();
  }

  private GeneratedQuestionSample parseCanonicalGeneratedQuestion(
      String aiContent,
      QuestionTemplate template,
      Map<String, Object> params,
      QuestionDifficulty fallbackDifficulty,
      String fallbackQuestionText,
      String fallbackExplanation,
      String fallbackDiagramData) {
    try {
      String json = repairTruncatedJson(extractJSON(aiContent));
      JsonNode root = objectMapper.readTree(json);

      Map<String, Object> effectiveParams = resolveEffectiveParameters(root, template, params);

      String questionText = root.path("questionText").asText();
      if (questionText == null || questionText.isBlank()) {
        questionText = fallbackQuestionText;
      }
      questionText = fillStrictDoubleBrace(questionText, effectiveParams);

      String explanation = root.path("explanation").asText();
      if (explanation == null || explanation.isBlank()) {
        explanation = fallbackExplanation;
      }
      explanation = fillStrictDoubleBrace(explanation, effectiveParams);

      String solutionSteps = root.path("solutionSteps").asText();
      if (solutionSteps == null || solutionSteps.isBlank()) {
        solutionSteps = explanation;
      }
      solutionSteps = fillStrictDoubleBrace(solutionSteps, effectiveParams);

      QuestionDifficulty difficulty = fallbackDifficulty;
      String diffStr = root.path("difficulty").asText("").trim().toUpperCase();
      if (!diffStr.isBlank()) {
        try {
          difficulty = QuestionDifficulty.valueOf(diffStr);
        } catch (Exception ignored) {
        }
      }

      Map<String, String> options = null;
      String correctAnswer;

      if (template.getTemplateType() == QuestionType.MULTIPLE_CHOICE) {
        options = parseCanonicalOptions(root.path("options"), effectiveParams);
        Map<String, String> templateOptions = buildTemplateOptions(template, effectiveParams);
        if (options.isEmpty() && !templateOptions.isEmpty()) {
          options = new LinkedHashMap<>(templateOptions);
        }
        ensureCanonicalFourOptionKeys(options);

        String correctKey = root.path("correctAnswer").asText();
        if (correctKey == null || correctKey.isBlank() || !options.containsKey(correctKey.trim())) {
          correctKey = "A";
        }
        correctAnswer = correctKey.trim();
      } else {
        correctAnswer = root.path("correctAnswer").asText();
        if (correctAnswer == null || correctAnswer.isBlank()) {
          correctAnswer = template.getAnswerFormula() != null ? template.getAnswerFormula() : "N/A";
        }
      }

      String answerCalculation = root.path("answerCalculation").asText();
      if (answerCalculation == null || answerCalculation.isBlank()) {
        answerCalculation = template.getAnswerFormula();
      }

      return GeneratedQuestionSample.builder()
          .questionText(questionText)
          .options(options)
          .correctAnswer(correctAnswer)
          .explanation(explanation)
          .solutionSteps(solutionSteps)
          .diagramData(fallbackDiagramData)
          .calculatedDifficulty(difficulty)
          .usedParameters(effectiveParams)
          .answerCalculation(answerCalculation)
          .build();
    } catch (Exception e) {
      log.error("Failed parsing canonical generated question: {}", e.getMessage());
      Map<String, String> fallbackOptions = buildCanonicalFallbackOptions(template, params);
      return GeneratedQuestionSample.builder()
          .questionText(fallbackQuestionText)
          .options(
              template.getTemplateType() == QuestionType.MULTIPLE_CHOICE ? fallbackOptions : null)
          .correctAnswer(template.getTemplateType() == QuestionType.MULTIPLE_CHOICE ? "A" : "N/A")
          .explanation(fallbackExplanation)
          .solutionSteps(fallbackExplanation)
          .diagramData(fallbackDiagramData)
          .calculatedDifficulty(fallbackDifficulty)
          .usedParameters(params)
          .answerCalculation(template.getAnswerFormula())
          .build();
    }
  }

  private Map<String, String> parseCanonicalOptions(
      JsonNode optionsNode, Map<String, Object> params) {
    Map<String, String> options = new LinkedHashMap<>();
    if (optionsNode == null || !optionsNode.isObject()) {
      return options;
    }

    Iterator<String> fields = optionsNode.fieldNames();
    while (fields.hasNext()) {
      String key = fields.next();
      String value = optionsNode.path(key).asText();
      if (value == null || value.isBlank()) {
        continue;
      }
      options.put(key, renderOptionValue(value, params));
    }
    return options;
  }

  private void ensureCanonicalFourOptionKeys(Map<String, String> options) {
    if (options == null) {
      return;
    }
    String[] keys = {"A", "B", "C", "D"};
    for (String key : keys) {
      options.putIfAbsent(key, "Lua chon " + key);
    }
    options.keySet().retainAll(Arrays.asList(keys));
  }

  private Map<String, String> buildCanonicalFallbackOptions(
      QuestionTemplate template, Map<String, Object> params) {
    Map<String, String> templateOptions = buildTemplateOptions(template, params);
    if (!templateOptions.isEmpty()) {
      ensureCanonicalFourOptionKeys(templateOptions);
      return templateOptions;
    }

    Map<String, String> fallback = new LinkedHashMap<>();
    fallback.put("A", "Lua chon A");
    fallback.put("B", "Lua chon B");
    fallback.put("C", "Lua chon C");
    fallback.put("D", "Lua chon D");
    return fallback;
  }

  /** Pick random parameter values within defined ranges, seeded by sampleIndex for variety */
  @SuppressWarnings("unchecked")
  private Map<String, Object> pickParameters(QuestionTemplate template, int sampleIndex) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (template.getParameters() == null) return result;

    Random rnd = new Random(System.currentTimeMillis() + sampleIndex * 1337L);
    for (Map.Entry<String, Object> entry : template.getParameters().entrySet()) {
      String name = entry.getKey();
      Object defObj = entry.getValue();
      if (!(defObj instanceof Map)) {
        result.put(name, 1);
        continue;
      }
      Map<String, Object> def = (Map<String, Object>) defObj;

      String type = def.getOrDefault("type", "integer").toString();
      int minVal = ((Number) def.getOrDefault("min", 1)).intValue();
      int maxVal = ((Number) def.getOrDefault("max", 10)).intValue();
      if (maxVal < minVal) maxVal = minVal + 9;

      // Exclude zero for denominators / coefficients named 'a'
      List<Integer> exclude = new ArrayList<>();
      if (def.containsKey("exclude")) {
        Object excObj = def.get("exclude");
        if (excObj instanceof List) {
          for (Object o : (List<?>) excObj) exclude.add(((Number) o).intValue());
        }
      }
      // Never allow 'a' == 0 (would cause division by zero in most formulas)
      if ("a".equals(name)) exclude.add(0);

      if (isIntegerParameterType(type)) {
        int v;
        int tries = 0;
        do {
          v = rnd.nextInt(maxVal - minVal + 1) + minVal;
          tries++;
        } while (exclude.contains(v) && tries < 50);
        result.put(name, v);
      } else {
        double minD = ((Number) def.getOrDefault("min", 1.0)).doubleValue();
        double maxD = ((Number) def.getOrDefault("max", 10.0)).doubleValue();
        result.put(name, minD + (maxD - minD) * rnd.nextDouble());
      }
    }
    return result;
  }

  /** Evaluate the answer formula using picked parameters via javax.script */
  private String evaluateFormula(String formula, Map<String, Object> params) {
    if (formula == null || formula.isBlank()) return "?";

    String normalizedFormula = normalizeFormulaForEvaluation(formula);
    Map<String, Object> paramsForFormula = buildFormulaParameterAliases(params);

    // Prefer internal evaluator first to avoid JS-engine differences for '^' and math functions.
    try {
      String directResult = evaluateFormulaSimple(normalizedFormula, paramsForFormula);
      if (!"?".equals(directResult)) {
        return directResult;
      }
    } catch (Exception e) {
      log.debug("Internal evaluator failed for '{}': {}", normalizedFormula, e.getMessage());
    }

    javax.script.ScriptEngine engine = null;
    try {
      javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
      engine = mgr.getEngineByName("JavaScript");
      if (engine == null) engine = mgr.getEngineByName("graal.js");
    } catch (Throwable e) {
      // ScriptEngineManager initialization failed (Graal.js not available)
      log.debug(
          "ScriptEngineManager initialization failed, using simple evaluator: {}", e.getMessage());
      engine = null;
    }

    // If no ScriptEngine available, internal evaluator is already attempted above.
    if (engine == null) {
      String symbolicAnswer = buildSymbolicFormulaAnswer(normalizedFormula, paramsForFormula);
      if (!"?".equals(symbolicAnswer)) {
        log.info("Using symbolic formula fallback answer: '{}'", symbolicAnswer);
        return symbolicAnswer;
      }

      String literalAnswer = resolveLiteralFormulaAnswer(formula, paramsForFormula);
      if (!"?".equals(literalAnswer)) {
        log.info("Using literal formula fallback answer: '{}'", literalAnswer);
        return literalAnswer;
      }

      return "?";
    }

    try {
      for (Map.Entry<String, Object> e : paramsForFormula.entrySet()) {
        engine.put(e.getKey(), e.getValue());
      }
      String jsFormula =
          normalizedFormula
              .replaceAll("\\bsqrt\\s*\\(", "Math.sqrt(")
              .replaceAll("\\babs\\s*\\(", "Math.abs(")
              .replace("^", "**");
      Object result = engine.eval(jsFormula);
      double val = ((Number) result).doubleValue();
      // Format: integer if whole, else strip trailing zeros (e.g. 9.50 → 9.5, 3.25 → 3.25)
      if (val == Math.floor(val) && !Double.isInfinite(val)) {
        return String.valueOf((long) val);
      }
      String formatted = formatDecimal(val, 4); // up to 4 decimals to avoid rounding loss
      formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
      return formatted;
    } catch (Exception e) {
      log.warn(
          "ScriptEngine evaluation failed for '{}': {}, trying simple evaluator",
          normalizedFormula,
          e.getMessage());
      // Try simple fallback, then symbolic fallback.
      try {
        String fallbackResult = evaluateFormulaSimple(normalizedFormula, paramsForFormula);
        if (!"?".equals(fallbackResult)) {
          return fallbackResult;
        }
      } catch (Exception fallbackError) {
        log.warn("Simple fallback also failed: {}", fallbackError.getMessage());
      }

      String symbolicAnswer = buildSymbolicFormulaAnswer(normalizedFormula, paramsForFormula);
      if (!"?".equals(symbolicAnswer)) {
        log.info("Using symbolic formula fallback answer: '{}'", symbolicAnswer);
        return symbolicAnswer;
      }

      String literalAnswer = resolveLiteralFormulaAnswer(formula, paramsForFormula);
      if (!"?".equals(literalAnswer)) {
        log.info("Using literal formula fallback answer: '{}'", literalAnswer);
        return literalAnswer;
      }

      return "?";
    }
  }

  /**
   * Build a symbolic answer by substituting known parameters while preserving unknown math
   * symbols (e.g. x). This prevents hard failures for valid symbolic formulas.
   */
  private String buildSymbolicFormulaAnswer(String formula, Map<String, Object> params) {
    if (formula == null || formula.isBlank()) {
      return "?";
    }

    String expression = formula;
    boolean replacedAnyParameter = false;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String paramName = entry.getKey();
      Object paramValue = entry.getValue();
      String valueStr = paramValue != null ? paramValue.toString() : "0";
      String replaced = expression.replaceAll("\\b" + Pattern.quote(paramName) + "\\b", valueStr);
      if (!replaced.equals(expression)) {
        replacedAnyParameter = true;
        expression = replaced;
      }
    }

    expression = expression.replaceAll("\\s+", " ").trim();
    if (expression.isEmpty()) {
      return "?";
    }

    // Avoid returning raw formula when nothing was substituted.
    return replacedAnyParameter ? expression : "?";
  }

  /**
   * Resolve textual answer formulas (e.g. option statements) when numeric evaluation is not
   * applicable.
   */
  private String resolveLiteralFormulaAnswer(String formula, Map<String, Object> params) {
    if (formula == null || formula.isBlank()) {
      return "?";
    }

    String rendered = fillText(formula, params);
    if (rendered == null || rendered.isBlank()) {
      return "?";
    }

    String cleaned = rendered.trim();
    if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
        || (cleaned.startsWith("'") && cleaned.endsWith("'"))) {
      cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
    }

    if (cleaned.isBlank() || PLACEHOLDER_PATTERN.matcher(cleaned).find()) {
      return "?";
    }

    return looksLikeArithmeticExpression(cleaned) ? "?" : cleaned;
  }

  private boolean looksLikeArithmeticExpression(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }

    String compact = value.replaceAll("\\s+", "");
    if (compact.matches("^[0-9().,\\-+*/^]+$")) {
      return true;
    }

    if (compact.matches("(?i).*(sqrt|sin|cos|tan|log|ln|abs)\\(.*")) {
      return true;
    }

    return compact.matches(".*[+\\-*/^=].*") && compact.matches(".*\\d.*");
  }

  /**
   * Normalize common LaTeX constructs to arithmetic expressions before evaluation.
   * Examples:
   * - \frac{-b}{2*a} -> ((-b)/(2*a))
   * - \times, \cdot -> *
   * - \left( ... \right) -> ( ... )
   */
  private String normalizeFormulaForEvaluation(String formula) {
    String normalized = formula.trim();

    // Remove math mode delimiters if present
    normalized = normalized.replace("$", "").trim();

    if (normalized.startsWith("\\(") && normalized.endsWith("\\)")) {
      normalized = normalized.substring(2, normalized.length() - 2).trim();
    }
    if (normalized.startsWith("\\[") && normalized.endsWith("\\]")) {
      normalized = normalized.substring(2, normalized.length() - 2).trim();
    }

    // Convert placeholders in formulas: {{a}} -> a
    normalized = normalized.replaceAll("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}", "$1");

    // Normalize common unicode operators to ASCII
    normalized = normalized.replace('−', '-').replace('–', '-').replace('—', '-');
    normalized = normalized.replace('×', '*').replace('·', '*').replace('∙', '*');
    normalized = normalized.replace('÷', '/').replace('∕', '/');

    // Decimal comma in numeric literals (e.g. 3,14 -> 3.14)
    normalized = normalized.replaceAll("(?<=\\d),(?=\\d)", ".");

    // Normalize common LaTeX operators
    normalized = normalized.replace("\\times", "*");
    normalized = normalized.replace("\\cdot", "*");
    normalized = normalized.replace("\\left", "");
    normalized = normalized.replace("\\right", "");
    normalized = normalized.replace("\\\\times", "*");
    normalized = normalized.replace("\\\\cdot", "*");
    normalized = normalized.replace("\\\\left", "");
    normalized = normalized.replace("\\\\right", "");

    // Repeatedly convert \frac{A}{B} -> ((A)/(B))
    Pattern fracPattern = Pattern.compile("\\\\+frac\\{([^{}]+)\\}\\{([^{}]+)\\}");
    String previous;
    do {
      previous = normalized;
      Matcher m = fracPattern.matcher(normalized);
      normalized = m.replaceAll("(($1)/($2))");
    } while (!normalized.equals(previous));

    return normalized;
  }

  private Map<String, Object> buildFormulaParameterAliases(Map<String, Object> params) {
    Map<String, Object> aliases = new LinkedHashMap<>();
    if (params == null) {
      return aliases;
    }

    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key == null) {
        continue;
      }

      aliases.put(key, value);

      String trimmed = key.trim();
      if (trimmed.startsWith("{{") && trimmed.endsWith("}}") && trimmed.length() > 4) {
        String plain = trimmed.substring(2, trimmed.length() - 2).trim();
        if (!plain.isEmpty()) {
          aliases.put(plain, value);
        }
      }
    }

    return aliases;
  }

  /**
   * Simple fallback formula evaluator without ScriptEngine.
   * Substitutes parameters and evaluates basic arithmetic expressions.
   */
  private String evaluateFormulaSimple(String formula, Map<String, Object> params) {
    try {
      // Check if formula contains set notation or complex math that we can't handle
      if (formula.contains("{") && formula.contains("}")) {
        log.warn(
            "Formula '{}' contains set notation {{}} - cannot evaluate as simple number", formula);
        return "?";
      }

      String expression = formula;

      // Substitute each parameter with its value
      for (Map.Entry<String, Object> entry : params.entrySet()) {
        String paramName = entry.getKey();
        Object paramValue = entry.getValue();
        String valueStr = paramValue != null ? paramValue.toString() : "0";
        // Replace parameter name (as whole word) with its value
        expression = expression.replaceAll("\\b" + Pattern.quote(paramName) + "\\b", valueStr);
      }

      log.debug("Evaluating formula: '{}' with substituted expression: '{}'", formula, expression);
      double result = parseArithmetic(expression);
      log.debug("Formula evaluation result: {}", result);

      // Format result
      if (result == Math.floor(result) && !Double.isInfinite(result)) {
        return String.valueOf((long) result);
      }
      String formatted = formatDecimal(result, 4);
      formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
      return formatted;
    } catch (Exception e) {
      log.warn("Simple formula evaluation failed for '{}': {}", formula, e.getMessage());
      return "?";
    }
  }

  /** Parse and evaluate arithmetic expression like "(1) - (1) / (1)" */
  private double parseArithmetic(String expr) {
    expr = expr.replaceAll("\\s+", "");
    int[] pos = new int[] {0};
    double result = evaluateAddSub(expr, pos);
    if (pos[0] != expr.length()) {
      throw new RuntimeException("Unexpected token at position " + pos[0]);
    }
    return result;
  }

  private double evaluateAddSub(String expr, int[] pos) {
    double result = evaluateMulDiv(expr, pos);
    while (pos[0] < expr.length()) {
      if (expr.charAt(pos[0]) == '+' || expr.charAt(pos[0]) == '-') {
        char op = expr.charAt(pos[0]++);
        double right = evaluateMulDiv(expr, pos);
        result = op == '+' ? result + right : result - right;
      } else {
        break;
      }
    }
    return result;
  }

  private double evaluateMulDiv(String expr, int[] pos) {
    double result = evaluatePower(expr, pos);
    while (pos[0] < expr.length()) {
      if (expr.charAt(pos[0]) == '*' || expr.charAt(pos[0]) == '/') {
        char op = expr.charAt(pos[0]++);
        double right = evaluatePower(expr, pos);
        result = op == '*' ? result * right : result / right;
      } else {
        break;
      }
    }
    return result;
  }

  private double evaluatePower(String expr, int[] pos) {
    double result = evaluatePrimary(expr, pos);
    while (pos[0] < expr.length() && expr.charAt(pos[0]) == '^') {
      pos[0]++;
      double exponent = evaluatePower(expr, pos); // right-associative exponent
      result = Math.pow(result, exponent);
    }
    return result;
  }

  private double evaluatePrimary(String expr, int[] pos) {
    if (pos[0] >= expr.length()) throw new RuntimeException("Unexpected end");

    // Support unary plus/minus before any primary, e.g. -(3), -x, +4
    if (expr.charAt(pos[0]) == '+' || expr.charAt(pos[0]) == '-') {
      char sign = expr.charAt(pos[0]++);
      double value = evaluatePrimary(expr, pos);
      return sign == '-' ? -value : value;
    }

    if (expr.charAt(pos[0]) == '(') {
      pos[0]++;
      double result = evaluateAddSub(expr, pos);
      if (pos[0] >= expr.length() || expr.charAt(pos[0]) != ')') {
        throw new RuntimeException("Missing )");
      }
      pos[0]++;
      return result;
    }

    // Function names and identifiers (e.g. sqrt(...), abs(...), pi)
    if (Character.isLetter(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '_') {
      StringBuilder id = new StringBuilder();
      while (pos[0] < expr.length()) {
        char c = expr.charAt(pos[0]);
        if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
          id.append(c);
          pos[0]++;
        } else {
          break;
        }
      }

      String identifier = id.toString();
      if (pos[0] < expr.length() && expr.charAt(pos[0]) == '(') {
        pos[0]++; // skip '('
        List<Double> args = new ArrayList<>();
        if (pos[0] < expr.length() && expr.charAt(pos[0]) != ')') {
          while (true) {
            args.add(evaluateAddSub(expr, pos));
            if (pos[0] < expr.length() && expr.charAt(pos[0]) == ',') {
              pos[0]++;
              continue;
            }
            break;
          }
        }
        if (pos[0] >= expr.length() || expr.charAt(pos[0]) != ')') {
          throw new RuntimeException("Missing ) after function " + identifier);
        }
        pos[0]++; // skip ')'
        return evaluateKnownFunction(identifier, args);
      }

      String normalizedId = identifier.toLowerCase(Locale.ROOT);
      if ("pi".equals(normalizedId) || "math.pi".equals(normalizedId)) {
        return Math.PI;
      }
      if ("e".equals(normalizedId) || "math.e".equals(normalizedId)) {
        return Math.E;
      }
      throw new RuntimeException("Unknown identifier: " + identifier);
    }

    StringBuilder num = new StringBuilder();

    while (pos[0] < expr.length()
        && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
      num.append(expr.charAt(pos[0]++));
    }

    if (num.length() == 0) throw new RuntimeException("Expected number");
    return Double.parseDouble(num.toString());
  }

  private double evaluateKnownFunction(String functionName, List<Double> args) {
    String fn = functionName.toLowerCase(Locale.ROOT);
    switch (fn) {
      case "sqrt":
      case "math.sqrt":
        if (args.size() != 1) throw new RuntimeException("sqrt expects 1 argument");
        return Math.sqrt(args.get(0));
      case "abs":
      case "math.abs":
        if (args.size() != 1) throw new RuntimeException("abs expects 1 argument");
        return Math.abs(args.get(0));
      case "pow":
      case "math.pow":
        if (args.size() != 2) throw new RuntimeException("pow expects 2 arguments");
        return Math.pow(args.get(0), args.get(1));
      case "max":
      case "math.max":
        if (args.size() != 2) throw new RuntimeException("max expects 2 arguments");
        return Math.max(args.get(0), args.get(1));
      case "min":
      case "math.min":
        if (args.size() != 2) throw new RuntimeException("min expects 2 arguments");
        return Math.min(args.get(0), args.get(1));
      default:
        throw new RuntimeException("Unknown function: " + functionName);
    }
  }

  /** Fill template text placeholders with actual parameter values */
  private String fillTemplateText(Map<String, Object> templateText, Map<String, Object> params) {
    if (templateText == null || templateText.isEmpty()) return "";
    String text =
        templateText
            .getOrDefault(
                "vi", templateText.getOrDefault("en", templateText.values().iterator().next()))
            .toString();
    for (Map.Entry<String, Object> e : params.entrySet()) {
      text = text.replace("{{" + e.getKey() + "}}", formatParameterValue(e.getValue()));
    }
    // Normalize sign combinations after substitution:
    // "N + -M"  → "N - M"
    // "N - -M"  → "N + M"
    text = text.replaceAll("\\+\\s*-(\\d)", "- $1");
    text = text.replaceAll("-\\s*-(\\d)", "+ $1");
    return text;
  }

  /** Determine difficulty from rules by simple keyword evaluation */
  private QuestionDifficulty determineDifficulty(
      Map<String, Object> rules, Map<String, Object> params) {
    if (rules == null || rules.isEmpty()) return QuestionDifficulty.MEDIUM;

    javax.script.ScriptEngine engine = null;
    try {
      javax.script.ScriptEngineManager mgr = new javax.script.ScriptEngineManager();
      engine = mgr.getEngineByName("JavaScript");
      if (engine == null) engine = mgr.getEngineByName("graal.js");
    } catch (Throwable e) {
      // ScriptEngineManager initialization failed (Graal.js not available)
      log.debug(
          "ScriptEngineManager initialization failed in determineDifficulty: {}", e.getMessage());
      return QuestionDifficulty.MEDIUM;
    }

    if (engine == null) {
      log.debug("No ScriptEngine available for difficulty determination");
      return QuestionDifficulty.MEDIUM;
    }

    try {
      for (Map.Entry<String, Object> e : params.entrySet()) engine.put(e.getKey(), e.getValue());

      for (String level : new String[] {"easy", "EASY", "medium", "MEDIUM", "hard", "HARD"}) {
        Object rule = rules.get(level);
        if (rule == null) continue;
        String jsRule =
            rule.toString()
                .replaceAll("\\bAND\\b", "&&")
                .replaceAll("\\bOR\\b", "||")
                .replaceAll("\\bABS\\(", "Math.abs(");
        try {
          Object res = engine.eval(jsRule);
          if (res instanceof Boolean && (Boolean) res) {
            return QuestionDifficulty.valueOf(level.toUpperCase());
          }
        } catch (Exception ignored) {
        }
      }
    } catch (Exception e) {
      log.debug("Error evaluating difficulty rules: {}", e.getMessage());
    }
    return QuestionDifficulty.MEDIUM;
  }

  private String buildGenerationPrompt(
      QuestionTemplate template,
      Map<String, Object> params,
      String correctAnswer,
      String baseQuestionText,
      int sampleIndex) {

    // Delegate to type-specific prompt builders
    QuestionType questionType = template.getTemplateType();

    if (questionType == QuestionType.TRUE_FALSE) {
      return buildTFGenerationPrompt(template, params, baseQuestionText, sampleIndex);
    } else if (questionType == QuestionType.SHORT_ANSWER) {
      return buildSAGenerationPrompt(
          template, params, correctAnswer, baseQuestionText, sampleIndex);
    }

    // Default: MCQ prompt (existing logic)
    boolean textualOptions = templateUsesTextualOptions(template);
    boolean optionPlaceholderMode = templateUsesDynamicOptionPlaceholders(template);
    Set<String> optionPlaceholderNames = collectOptionPlaceholderNames(template);
    StringBuilder p = new StringBuilder();
    p.append("Return ONLY a JSON object. No markdown, no explanation outside JSON.\n\n");
    p.append("Task: Generate a math question in Vietnamese with 4 options (A,B,C,D).\n\n");

    p.append("Question (already formed): ").append(baseQuestionText).append("\n");
    p.append("Correct answer (pre-computed, DO NOT change): ").append(correctAnswer).append("\n");
    p.append("Parameters used: ").append(serializeParamsForPrompt(params)).append("\n");
    if (template.getAnswerFormula() != null && !template.getAnswerFormula().isBlank()) {
      p.append("Formula: ").append(template.getAnswerFormula()).append("\n");
    }
    p.append("Sample #: ").append(sampleIndex + 1).append("\n\n");

    p.append("Rules:\n");
    p.append("1. Place ")
        .append(correctAnswer)
        .append(" as one of A/B/C/D (vary position each sample, sample #")
        .append(sampleIndex + 1)
        .append(" → prefer key ")
        .append(new String[] {"B", "C", "D", "A", "B"}[sampleIndex % 5])
        .append(").\n");
    if (optionPlaceholderMode) {
      p.append(
          "2. DO NOT generate option values yourself. Keep options empty {} (or omit options).\n");
      p.append("3. Provide usedParameters for option placeholders: ")
          .append(optionPlaceholderNames)
          .append(".\n");
      p.append(
          "4. If placeholder {{para}} exists, generate one concise Vietnamese paragraph for key para.\n");
      p.append(
          "5. Keep placeholder values plain text (no wrapping with {{}} in usedParameters).\n");
    } else if (textualOptions) {
      p.append(
          "2. Create 3 wrong options as plausible statement-level distractors (text), keep style consistent with the template.\n");
      p.append("3. All 4 option values must be distinct statements.\n");
    } else {
      p.append(
          "2. Create 3 wrong options representing common student mistakes. Numeric values only (e.g. \"5\", \"-3.14\"). NO text.\n");
      p.append("3. All 4 option values must be distinct numbers.\n");
    }
    p.append(
        "4. correctAnswer field MUST be the LETTER KEY (A, B, C, or D) — NOT the numeric value.\n");
    p.append("5. The letter key in correctAnswer must map to value ")
        .append(correctAnswer)
        .append(" in options.\n");
    p.append(
        "6. explanation + solutionSteps follow the Depth rules below — worked walk-through, not summary, no self-correction.\n");
    p.append("7. answerCalculation = simple expression like \"(c - b) / a = ")
        .append(correctAnswer)
        .append("\".\n\n");
    p.append(
        "8. questionText and explanation MUST be natural Vietnamese with proper accents (UTF-8), no transliteration.\n");
    p.append(
        "9. Numeric values must use plain format: no thousands separators, use dot for decimal if needed (e.g. 3.5).\n\n");
    p.append(
        "10. IMPORTANT: keep parameter placeholders in double braces (e.g. {{a}}, {{x1}}, {{yMax}}). Do NOT replace placeholders with concrete numbers; backend will substitute values.\n\n");
    p.append(
        "11. You MAY propose usedParameters as numeric values. Backend will validate and use them to render final diagram and substitute placeholders.\n\n");

    appendLatexStrictnessRules(p, correctAnswer);

    p.append("JSON format:\n");
    if (optionPlaceholderMode) {
      p.append("{\"questionText\":\"...\",\"options\":{},");
    } else {
      p.append(
          "{\"questionText\":\"...\",\"options\":{\"A\":\"n\",\"B\":\"n\",\"C\":\"n\",\"D\":\"n\"},");
    }
    p.append(
        "\"correctAnswer\":\"X\",\"explanation\":\"...\",\"solutionSteps\":\"Bước 1: ...\\nBước 2: ...\",\"difficulty\":\"EASY|MEDIUM|HARD\",");
    p.append("\"usedParameters\":{");
    // inline params so LLM just copies them back
    StringBuilder paramStr = new StringBuilder();
    params.forEach(
        (k, v) -> {
          if (paramStr.length() > 0) paramStr.append(",");
          Object value = v;
          if (value instanceof Number) {
            paramStr.append("\"").append(k).append("\":").append(formatParameterValue(value));
          } else {
            paramStr
                .append("\"")
                .append(k)
                .append("\":\"")
                .append(String.valueOf(value).replace("\"", "\\\\\""))
                .append("\"");
          }
        });
    p.append(paramStr);
    p.append("},\"answerCalculation\":\"")
        .append(template.getAnswerFormula() != null ? template.getAnswerFormula() : "")
        .append(" = ")
        .append(correctAnswer)
        .append("\"}\n");

    return p.toString();
  }

  /**
   * Append strict LaTeX / formatting rules that every question-generation
   * prompt should enforce. Centralised so MCQ, TRUE_FALSE, and SHORT_ANSWER
   * prompts all get the same guarantees.
   *
   * @param expectedAnswer if non-null, prompt forbids the AI from recomputing
   *                       and demands this value appear verbatim in the
   *                       solution (used by MCQ + SA; pass {@code null} for TF
   *                       where the "answer" is a comma-separated key list)
   */
  private void appendLatexStrictnessRules(StringBuilder p, String expectedAnswer) {
    p.append("LaTeX strictness rules (MUST follow exactly):\n");
    p.append(
        "L1. Every \\dfrac and \\frac MUST have TWO braced arguments: \\dfrac{a}{b}, never \\dfrac a b or \\dfrac\\sqrt{3}4.\n");
    p.append(
        "L2. \\sqrt with multi-token argument MUST brace it: \\sqrt{a^2+b^2}. Single tokens may use \\sqrt 3 but prefer \\sqrt{3}.\n");
    p.append(
        "L3. Subscripts and superscripts longer than ONE character MUST be braced: S_{xq} not S_xq, x^{12} not x^12, S_{\\text{đáy}} not S_\\text{đáy}.\n");
    p.append(
        "L4. \\text{...} arguments MUST be braced. Always close every brace you open — the entire expression must have balanced { }.\n");
    p.append(
        "L5. Inline math uses single $...$, display math uses $$...$$. Each $ must have a matching $.\n");
    p.append(
        "L6. solutionSteps MUST be a single string with literal \\n separating each Bước. Format: \"Bước 1: ...\\nBước 2: ...\\nBước 3: ...\".\n");
    p.append(
        "L7. Each Bước SHOULD show the substituted numeric value, not just the symbolic formula. Example: write $S = \\dfrac{\\sqrt{3}}{4} \\cdot 2^2 = \\sqrt{3}$, not $S = \\dfrac{\\sqrt{3}}{4}a^2$.\n");
    if (expectedAnswer != null && !expectedAnswer.isBlank()) {
      p.append(
          "L8. DO NOT recompute the answer. The correct numeric answer is exactly \"")
          .append(expectedAnswer)
          .append("\" — copy this value into the solution; do not reach a different number.\n");
    }
    p.append(
        "L9. NEVER output the literal word \"null\" anywhere in explanation or solutionSteps.\n\n");

    // Detail / depth rules — apply to every generation prompt so every output
    // is a worked walk-through, not a high-level description of the approach.
    p.append("Depth rules (explanation + solutionSteps must be SUBSTANTIVE):\n");
    p.append(
        "D1. `explanation` is a 4-7 sentence WORKED walk-through, NOT a summary. It must:\n"
            + "    - name every formula used,\n"
            + "    - show the substituted numerical chain leading to the answer,\n"
            + "    - contain at least 2 inline LaTeX expressions ($...$) that show actual numbers,\n"
            + "    - end with the final value.\n");
    p.append(
        "D2. `solutionSteps` has 3+ numbered Bước. Each Bước contains ALL of:\n"
            + "    (a) name of the concept / formula being applied,\n"
            + "    (b) symbolic form of the formula,\n"
            + "    (c) substituted form with the actual numbers,\n"
            + "    (d) simplified intermediate result,\n"
            + "    (e) 1 short reasoning sentence (\"vì ..., áp dụng ..., do đó ...\").\n");
    p.append(
        "D3. NEVER write \"trong đó X được xác định bằng Y\" without then computing Y to a"
            + " concrete number. Compute every intermediate quantity.\n");
    p.append(
        "D4. AVOID vague openings (\"Để tính ... chúng ta cần ...\", \"Bài này yêu cầu ...\")"
            + " followed by an abstract description. Jump straight into the worked computation.\n\n");
  }

  /**
   * Build AI prompt for TRUE/FALSE question generation
   */
  private String buildTFGenerationPrompt(
      QuestionTemplate template,
      Map<String, Object> params,
      String baseQuestionText,
      int sampleIndex) {

    StringBuilder p = new StringBuilder();
    p.append("Return ONLY a JSON object. No markdown, no explanation outside JSON.\n\n");
    p.append(
        "Task: Generate explanation and solution steps for a TRUE/FALSE question in Vietnamese.\n\n");

    p.append("Question statement: ").append(baseQuestionText).append("\n");
    p.append("Parameters used: ").append(serializeParamsForPrompt(params)).append("\n");

    // Include the actual clauses from the template
    Map<String, Object> statementMutations =
        template.getStatementMutations() != null
            ? template.getStatementMutations()
            : new HashMap<>();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> clauseTemplates =
        (List<Map<String, Object>>)
            statementMutations.getOrDefault("clauseTemplates", new ArrayList<>());

    if (!clauseTemplates.isEmpty()) {
      p.append("\nClauses:\n");
      for (int i = 0; i < Math.min(4, clauseTemplates.size()); i++) {
        @SuppressWarnings("unchecked")
        Map<String, Object> clauseTemplate = clauseTemplates.get(i);
        String key = String.valueOf((char) ('A' + i));
        String clauseText = (String) clauseTemplate.get("text");
        Boolean truthValue = (Boolean) clauseTemplate.get("truthValue");

        if (clauseText != null) {
          // Same evaluation pass as the runtime clause builder so the prompt
          // we send to Gemini shows the *evaluated* clause, not the un-simplified
          // formula. Otherwise the AI explanation would describe e.g. "2*0+3"
          // when the student actually sees "3".
          clauseText = fillStrictDoubleBrace(clauseText, params);
          clauseText = evaluateInlineArithmetic(clauseText, params);
          p.append(key)
              .append(") ")
              .append(clauseText)
              .append(" [")
              .append(truthValue != null && truthValue ? "TRUE" : "FALSE")
              .append("]\n");
        }
      }
    }

    p.append("\nSample #: ").append(sampleIndex + 1).append("\n\n");

    p.append("Rules:\n");
    p.append("1. DO NOT generate new clauses - use the clauses provided above.\n");
    p.append(
        "2. `explanation` is a 4-7 sentence WORKED walk-through — it must show the substituted"
            + " numerical reasoning that decides each clause's truth, not just describe the"
            + " concepts.\n");
    p.append(
        "3. `solutionSteps` has one Mệnh đề-X block per clause (A, B, C, D in order), each"
            + " with the symbolic check, the substituted check with actual numbers, the verdict"
            + " (đúng / sai), and 1 reasoning sentence citing the property used.\n");
    p.append("4. Focus on mathematical reasoning. Cite specific properties / theorems by name.\n");
    p.append("5. All text MUST be natural Vietnamese with proper accents (UTF-8).\n\n");

    appendLatexStrictnessRules(p, null);

    p.append("JSON format:\n");
    p.append("{\n");
    p.append("  \"questionText\": \"")
        .append(baseQuestionText.replace("\"", "\\\""))
        .append("\",\n");
    p.append("  \"options\": {},\n"); // Empty - we'll use template clauses
    p.append("  \"correctAnswer\": \"A\",\n"); // Placeholder - we'll use template answer
    p.append("  \"explanation\": \"Overall explanation connecting all clauses...\",\n");
    p.append(
        "  \"solutionSteps\": \"Mệnh đề A: [reasoning why true/false]\\nMệnh đề B: [reasoning]\\nMệnh đề C: [reasoning]\\nMệnh đề D: [reasoning]\",\n");
    p.append("  \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
    p.append("  \"usedParameters\": {");

    // Add parameters
    StringBuilder paramStr = new StringBuilder();
    params.forEach(
        (k, v) -> {
          if (paramStr.length() > 0) paramStr.append(",");
          Object value = v;
          if (value instanceof Number) {
            paramStr.append("\"").append(k).append("\":").append(formatParameterValue(value));
          } else {
            paramStr
                .append("\"")
                .append(k)
                .append("\":\"")
                .append(String.valueOf(value).replace("\"", "\\\""))
                .append("\"");
          }
        });
    p.append(paramStr);
    p.append("}\n}\n");

    return p.toString();
  }

  /**
   * Build AI prompt for SHORT ANSWER question generation
   */
  private String buildSAGenerationPrompt(
      QuestionTemplate template,
      Map<String, Object> params,
      String correctAnswer,
      String baseQuestionText,
      int sampleIndex) {

    StringBuilder p = new StringBuilder();
    p.append("Return ONLY a JSON object. No markdown, no explanation outside JSON.\n\n");
    p.append("Task: Generate a SHORT ANSWER question in Vietnamese.\n\n");

    p.append("Question (already formed): ").append(baseQuestionText).append("\n");
    p.append("Correct answer (pre-computed, DO NOT change): ").append(correctAnswer).append("\n");
    p.append("Parameters used: ").append(serializeParamsForPrompt(params)).append("\n");

    if (template.getAnswerFormula() != null && !template.getAnswerFormula().isBlank()) {
      p.append("Formula: ").append(template.getAnswerFormula()).append("\n");
    }

    p.append("Sample #: ").append(sampleIndex + 1).append("\n\n");

    p.append("Rules:\n");
    p.append("1. correctAnswer = \"")
        .append(correctAnswer)
        .append("\" (DO NOT change this value).\n");
    p.append(
        "2. `explanation` is a 4-7 sentence WORKED walk-through — name every formula used,"
            + " show the substituted numerical chain, end with the final value. NOT a 2-3 sentence"
            + " summary.\n");
    p.append(
        "3. `solutionSteps` has 3+ numbered Bước, each containing: concept name, symbolic"
            + " formula, substitution with numbers, simplified result, 1 reasoning sentence.\n");
    p.append("4. Include reasoning and key formulas used. Compute every intermediate quantity.\n");
    p.append("5. All text MUST be natural Vietnamese with proper accents (UTF-8).\n");
    p.append("6. Keep parameter placeholders in double braces (e.g. {{a}}, {{x1}}).\n\n");

    appendLatexStrictnessRules(p, correctAnswer);

    p.append("JSON format:\n");
    p.append("{\n");
    p.append("  \"questionText\": \"")
        .append(baseQuestionText.replace("\"", "\\\""))
        .append("\",\n");
    p.append("  \"options\": {},\n");
    p.append("  \"correctAnswer\": \"").append(correctAnswer).append("\",\n");
    p.append("  \"explanation\": \"Concept explanation in Vietnamese...\",\n");
    p.append("  \"solutionSteps\": \"Bước 1: ...\\nBước 2: ...\\nBước 3: ...\",\n");
    p.append("  \"difficulty\": \"EASY|MEDIUM|HARD\",\n");
    p.append("  \"answerCalculation\": \"");

    if (template.getAnswerFormula() != null) {
      p.append(template.getAnswerFormula()).append(" = ").append(correctAnswer);
    }

    p.append("\",\n");
    p.append("  \"usedParameters\": {");

    // Add parameters
    StringBuilder paramStr = new StringBuilder();
    params.forEach(
        (k, v) -> {
          if (paramStr.length() > 0) paramStr.append(",");
          Object value = v;
          if (value instanceof Number) {
            paramStr.append("\"").append(k).append("\":").append(formatParameterValue(value));
          } else {
            paramStr
                .append("\"")
                .append(k)
                .append("\":\"")
                .append(String.valueOf(value).replace("\"", "\\\""))
                .append("\"");
          }
        });
    p.append(paramStr);
    p.append("}\n}\n");

    return p.toString();
  }

  @SuppressWarnings("unchecked")
  private GeneratedQuestionSample parseGeneratedQuestion(
      String aiContent,
      QuestionTemplate template,
      Map<String, Object> params,
      String correctAnswer,
      QuestionDifficulty difficulty,
      String baseQuestionText) {
    try {
      String json = extractJSON(aiContent);
      // Attempt to repair truncated JSON
      json = repairTruncatedJson(json);
      log.debug("Extracted and repaired JSON for question generation:\n{}", json);

      JsonNode root = objectMapper.readTree(json);

      Map<String, Object> effectiveParams = resolveEffectiveParameters(root, template, params);
      String effectiveCorrectAnswer = evaluateFormula(template.getAnswerFormula(), effectiveParams);
      if ("?".equals(effectiveCorrectAnswer)) {
        effectiveCorrectAnswer = correctAnswer;
      }
      String fallbackQuestionText = fillTemplateText(template.getTemplateText(), effectiveParams);

      String questionText = root.path("questionText").asText();

      // Validate and fallback
      if (questionText == null || questionText.isBlank()) {
        log.warn("questionText is empty, using baseQuestionText");
        questionText = fallbackQuestionText;
      } else if (questionText.matches("^\\d+$")) {
        // If it's only numbers, it's likely a parsing error
        log.warn(
            "questionText contains only numbers ({}), using baseQuestionText instead",
            questionText);
        questionText = fallbackQuestionText;
      }
      questionText = fillStrictDoubleBrace(questionText, effectiveParams);

      String explanation = root.path("explanation").asText();
      if (explanation == null || explanation.isBlank()) {
        explanation = "Solution provided by AI";
      }
      explanation = fillStrictDoubleBrace(explanation, effectiveParams);

      // Parse solutionSteps from AI response (fallback to explanation if not provided)
      String solutionSteps = root.path("solutionSteps").asText();
      if (solutionSteps == null || solutionSteps.isBlank()) {
        solutionSteps = buildSolutionSteps(explanation);
      } else {
        solutionSteps = fillStrictDoubleBrace(solutionSteps, effectiveParams);
      }

      // Difficulty from LLM or pre-computed
      String diffStr = root.path("difficulty").asText("").trim().toUpperCase();
      if (!diffStr.isBlank()) {
        try {
          difficulty = QuestionDifficulty.valueOf(diffStr);
        } catch (Exception ignored) {
        }
      }

      // Parse options
      Map<String, String> options = new LinkedHashMap<>();
      boolean optionPlaceholderMode = templateUsesDynamicOptionPlaceholders(template);
      JsonNode optionsNode = null;
      if (!optionPlaceholderMode && root.has("options") && !root.get("options").isNull()) {
        optionsNode = root.get("options");
        Iterator<String> fields = optionsNode.fieldNames();
        while (fields.hasNext()) {
          String k = fields.next();
          options.put(k, renderOptionValue(optionsNode.get(k).asText(), effectiveParams));
        }
      }

      Map<String, String> templateOptions = buildTemplateOptions(template, effectiveParams);
      if (optionPlaceholderMode && !templateOptions.isEmpty()) {
        options = new LinkedHashMap<>(templateOptions);
      } else if (!templateOptions.isEmpty()
          && !aiOptionsUseTemplateParameters(optionsNode, effectiveParams)
          && !containsValueApprox(templateOptions, effectiveCorrectAnswer)) {
        log.warn(
            "AI options do not contain template parameters; fallback to template options and only update correct answer.");
        options = new LinkedHashMap<>(templateOptions);
      }

      // Ensure all 4 keys exist
      if (!optionPlaceholderMode) {
        ensureFourOptions(options, effectiveCorrectAnswer, effectiveParams);
      }

      // Always find the correct key by matching the pre-computed answer
      String correctKey = findKeyByValue(options, effectiveCorrectAnswer);
      if (correctKey == null) {
        // LLM didn't put the correct answer in options — inject it at key A and shift others
        options.put("A", effectiveCorrectAnswer);
        correctKey = "A";
        log.warn(
            "LLM did not include correct answer '{}' in options — injected at A",
            effectiveCorrectAnswer);
      }

      String answerCalc =
          template.getAnswerFormula() != null
              ? template.getAnswerFormula() + " = " + effectiveCorrectAnswer
              : "= " + effectiveCorrectAnswer;

      return GeneratedQuestionSample.builder()
          .questionText(questionText)
          .options(options)
          .correctAnswer(correctKey) // KEY (A/B/C/D), not numeric value
          .explanation(explanation)
          .solutionSteps(solutionSteps)
          .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), effectiveParams))
          .calculatedDifficulty(difficulty)
          .usedParameters(effectiveParams)
          .answerCalculation(answerCalc)
          .build();

    } catch (Exception e) {
      log.error("Failed to parse LLM-generated question, using base fallback: {}", e.getMessage());
      // Build a type-aware minimal sample. The legacy MCQ-shaped fallback
      // produced "Áp dụng công thức: null = A,C" for TRUE_FALSE templates
      // (which have no answerFormula). Branch by type to keep the message
      // sensible regardless of which generator path triggered the parse.
      String renderedSteps = renderSolutionSteps(template, params);
      String fallbackExplanation =
          buildTypeAwareFallbackExplanation(template, correctAnswer, renderedSteps);
      QuestionType type = template.getTemplateType();
      Map<String, String> fallbackOptions =
          type == QuestionType.MULTIPLE_CHOICE
              ? buildFallbackOptions(correctAnswer, params)
              : null;
      String correctKey = correctAnswer;
      if (type == QuestionType.MULTIPLE_CHOICE && fallbackOptions != null) {
        String matched = findKeyByValue(fallbackOptions, correctAnswer);
        correctKey = matched != null ? matched : "A";
      }
      return GeneratedQuestionSample.builder()
          .questionText(baseQuestionText)
          .options(fallbackOptions)
          .correctAnswer(correctKey)
          .explanation(fallbackExplanation)
          .solutionSteps(renderedSteps != null ? renderedSteps : fallbackExplanation)
          .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), params))
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(
              template.getAnswerFormula() != null
                  ? template.getAnswerFormula() + " = " + correctAnswer
                  : "= " + correctAnswer)
          .build();
    }
  }

  private String buildTypeAwareFallbackExplanation(
      QuestionTemplate template, String correctAnswer, String renderedSteps) {
    QuestionType type = template.getTemplateType();
    String headline;
    if (type == QuestionType.TRUE_FALSE) {
      headline =
          correctAnswer == null || correctAnswer.isBlank()
              ? "Phân tích từng mệnh đề và xét tính đúng/sai dựa vào nội dung đề bài."
              : "Các mệnh đề đúng: "
                  + correctAnswer
                  + ". Phân tích từng mệnh đề dựa vào nội dung đề bài.";
    } else if (template.getAnswerFormula() != null
        && !template.getAnswerFormula().isBlank()) {
      headline =
          "Áp dụng công thức "
              + template.getAnswerFormula()
              + " với các tham số đã cho, đáp án bằng "
              + correctAnswer
              + ".";
    } else {
      headline = "Đáp án: " + correctAnswer + ".";
    }
    if (renderedSteps != null && !renderedSteps.isBlank()) {
      return headline + "\n\n" + renderedSteps;
    }
    return headline;
  }

  private String buildSolutionSteps(String aiExplanation) {
    return aiExplanation;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> resolveEffectiveParameters(
      JsonNode root, QuestionTemplate template, Map<String, Object> fallbackParams) {
    Map<String, Object> effective = new LinkedHashMap<>();
    if (fallbackParams != null) {
      effective.putAll(fallbackParams);
    }

    if (root == null || template == null) {
      return effective;
    }

    Map<String, Object> definedParameters =
        template.getParameters() != null ? template.getParameters() : Collections.emptyMap();
    Set<String> dynamicPlaceholderNames = collectTemplatePlaceholders(template);

    JsonNode usedParametersNode = root.path("usedParameters");
    if (!usedParametersNode.isObject()) {
      return effective;
    }

    boolean updated = false;
    Iterator<String> aiParamFields = usedParametersNode.fieldNames();
    while (aiParamFields.hasNext()) {
      String paramName = aiParamFields.next();
      JsonNode valueNode = usedParametersNode.get(paramName);
      if (valueNode == null || valueNode.isNull()) {
        continue;
      }

      Object definedParam = definedParameters.get(paramName);
      if (!(definedParam instanceof Map<?, ?> defMapAny)) {
        if (!dynamicPlaceholderNames.contains(paramName)) {
          continue;
        }

        Object dynamicValue = parseDynamicPlaceholderValue(valueNode);
        if (dynamicValue == null) {
          continue;
        }

        effective.put(paramName, dynamicValue);
        updated = true;
        continue;
      }

      Map<String, Object> def = (Map<String, Object>) defMapAny;
      String type = String.valueOf(def.getOrDefault("type", "integer"));
      Object parsed = parseAIParameterValue(valueNode, type);
      if (parsed == null) {
        continue;
      }

      if (!isAIParameterValueAllowed(paramName, parsed, type, def)) {
        continue;
      }

      effective.put(paramName, parsed);
      updated = true;
    }

    if (updated) {
      log.info("Using AI-proposed parameters for final rendering: {}", effective);
    }

    return effective;
  }

  private Object parseAIParameterValue(JsonNode valueNode, String type) {
    if (isIntegerParameterType(type)) {
      Long longValue = extractLongValue(valueNode);
      if (longValue == null) {
        return null;
      }
      if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
        return null;
      }
      return longValue.intValue();
    }

    Double doubleValue = extractDoubleValue(valueNode);
    return doubleValue;
  }

  private Object parseDynamicPlaceholderValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }

    if (node.isIntegralNumber()) {
      long value = node.longValue();
      if (value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE) {
        return (int) value;
      }
      return value;
    }

    if (node.isFloatingPointNumber()) {
      return node.doubleValue();
    }

    String text = node.asText("").trim();
    return text.isBlank() ? null : text;
  }

  private Long extractLongValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isIntegralNumber()) {
      return node.longValue();
    }
    if (node.isNumber()) {
      double d = node.doubleValue();
      if (d == Math.rint(d)) {
        return (long) d;
      }
      return null;
    }

    String text = normalizeNumericLocale(node.asText(""));
    if (text == null || text.isBlank()) {
      return null;
    }
    text = text.trim();
    if (text.matches("^-?\\d+$")) {
      try {
        return Long.parseLong(text);
      } catch (Exception e) {
        return null;
      }
    }
    if (text.matches("^-?\\d*\\.\\d+$")) {
      try {
        double d = Double.parseDouble(text);
        if (d == Math.rint(d)) {
          return (long) d;
        }
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  private Double extractDoubleValue(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    if (node.isNumber()) {
      return node.doubleValue();
    }

    String text = normalizeNumericLocale(node.asText(""));
    if (text == null || text.isBlank()) {
      return null;
    }
    text = text.trim();
    try {
      return Double.parseDouble(text);
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private boolean isAIParameterValueAllowed(
      String paramName, Object value, String type, Map<String, Object> def) {
    if (value == null) {
      return false;
    }

    if (isIntegerParameterType(type)) {
      int intValue = ((Number) value).intValue();
      int minVal = ((Number) def.getOrDefault("min", 1)).intValue();
      int maxVal = ((Number) def.getOrDefault("max", 10)).intValue();
      if (maxVal < minVal) {
        maxVal = minVal;
      }
      if (intValue < minVal || intValue > maxVal) {
        return false;
      }

      List<Integer> exclude = new ArrayList<>();
      Object excObj = def.get("exclude");
      if (excObj instanceof List<?>) {
        for (Object o : (List<?>) excObj) {
          if (o instanceof Number n) {
            exclude.add(n.intValue());
          }
        }
      }
      if ("a".equals(paramName)) {
        exclude.add(0);
      }
      return !exclude.contains(intValue);
    }

    double doubleValue = ((Number) value).doubleValue();
    double minVal = ((Number) def.getOrDefault("min", 1.0)).doubleValue();
    double maxVal = ((Number) def.getOrDefault("max", 10.0)).doubleValue();
    if (maxVal < minVal) {
      maxVal = minVal;
    }
    return doubleValue >= minVal && doubleValue <= maxVal;
  }

  private boolean templateUsesTextualOptions(QuestionTemplate template) {
    if (template == null || template.getOptionsGenerator() == null) {
      return false;
    }

    String[] keys = {"A", "B", "C", "D"};
    for (String key : keys) {
      Object value = template.getOptionsGenerator().get(key);
      String raw = extractTemplateOptionRawValue(value);
      if (raw == null || raw.isBlank()) {
        continue;
      }

      String normalized = raw.replaceAll("\\{\\{[^}]+}}", "").trim();
      if (normalized.matches(".*[A-Za-zÀ-ỹ].*")) {
        return true;
      }
    }
    return false;
  }

  private boolean templateUsesDynamicOptionPlaceholders(QuestionTemplate template) {
    return !collectOptionPlaceholderNames(template).isEmpty();
  }

  private Set<String> collectOptionPlaceholderNames(QuestionTemplate template) {
    Set<String> names = new LinkedHashSet<>();
    if (template == null || template.getOptionsGenerator() == null) {
      return names;
    }

    String[] keys = {"A", "B", "C", "D"};
    for (String key : keys) {
      Object value = template.getOptionsGenerator().get(key);
      String raw = extractTemplateOptionRawValue(value);
      collectDoubleBracePlaceholders(raw, names);
    }

    return names;
  }

  private Set<String> collectTemplatePlaceholders(QuestionTemplate template) {
    Set<String> names = new LinkedHashSet<>();
    if (template == null) {
      return names;
    }

    if (template.getParameters() != null) {
      names.addAll(template.getParameters().keySet());
    }

    if (template.getTemplateText() != null) {
      for (Object value : template.getTemplateText().values()) {
        collectPlaceholdersFromObject(value, names);
      }
    }

    names.addAll(collectOptionPlaceholderNames(template));
    collectDoubleBracePlaceholders(template.getDiagramTemplate(), names);
    collectDoubleBracePlaceholders(template.getAnswerFormula(), names);
    return names;
  }

  @SuppressWarnings("unchecked")
  private void collectPlaceholdersFromObject(Object value, Set<String> names) {
    if (value == null) {
      return;
    }
    if (value instanceof String s) {
      collectDoubleBracePlaceholders(s, names);
      return;
    }
    if (value instanceof Map<?, ?> map) {
      for (Object nested : ((Map<?, Object>) map).values()) {
        collectPlaceholdersFromObject(nested, names);
      }
      return;
    }
    if (value instanceof Iterable<?> iterable) {
      for (Object nested : iterable) {
        collectPlaceholdersFromObject(nested, names);
      }
    }
  }

  private void collectDoubleBracePlaceholders(String text, Set<String> names) {
    if (text == null || text.isBlank()) {
      return;
    }
    Matcher matcher = DOUBLE_BRACE_PLACEHOLDER_PATTERN.matcher(text);
    while (matcher.find()) {
      names.add(matcher.group(1));
    }
  }

  private String fillText(String raw, Map<String, Object> params) {
    if (raw == null || params == null || params.isEmpty()) {
      return raw;
    }

    Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String token = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      Object resolved = resolvePlaceholderValue(token, params);
      String replacement = resolved == null ? matcher.group() : String.valueOf(resolved);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * For options: if there is no parameter placeholder, keep original value unchanged.
   * Only render placeholders when present.
   *
   * <p>After {{param}} substitution the value can still contain a raw expression
   * like {@code "$(-3)/2$"}. Teachers expect the option to display the computed
   * value ({@code "$-1.5$"}), not the formula that produced it. We pass the
   * substituted string through {@link #evaluateInlineArithmetic} which:
   *
   * <ul>
   *   <li>finds {@code $...$} math blocks and replaces each block's content
   *       with the evaluated numeric result if the content is pure arithmetic;</li>
   *   <li>if the entire option (after stripping a single outer {@code $...$})
   *       is itself pure arithmetic, evaluates the whole option;</li>
   *   <li>leaves natural-language text alone so clause/option prose is preserved.</li>
   * </ul>
   */
  private String renderOptionValue(String rawValue, Map<String, Object> params) {
    if (rawValue == null) {
      return null;
    }

    boolean hasPlaceholder = PLACEHOLDER_PATTERN.matcher(rawValue).find();
    String rendered = hasPlaceholder ? fillText(rawValue, params) : rawValue;
    if (rendered == null) {
      rendered = rawValue;
    }
    return evaluateInlineArithmetic(rendered, params);
  }

  /**
   * Pure-arithmetic detector: a string consisting of digits, parens, the
   * usual operators, and whitespace — i.e. something that {@code evaluateFormula}
   * can crunch without falling through to the symbolic fallback. Variables and
   * letters are excluded so we don't accidentally evaluate prose.
   */
  private static final Pattern PURE_ARITHMETIC = Pattern.compile("^[\\s0-9+\\-*/().,^%]+$");

  /**
   * Evaluates arithmetic inside {@code $...$} math blocks and (if the whole
   * input minus a single outer {@code $...$} is itself pure arithmetic) the
   * whole input. Used for options and clause text post-substitution.
   *
   * <p>A clause like {@code "f(0) = 2*0 + 3"} after substitution becomes
   * {@code "f(0) = 2*0 + 3"} — the equation is mixed prose+math, so the
   * outer text is left untouched. If the same clause is written as
   * {@code "f(0) = $2*0 + 3$"} the LaTeX block is collapsed to {@code "$3$"}.
   */
  private String evaluateInlineArithmetic(String input, Map<String, Object> params) {
    if (input == null || input.isBlank()) {
      return input;
    }

    // 1. Replace each $...$ math block with its evaluated value when the
    //    inner content is pure arithmetic. We use a non-greedy match so
    //    multiple inline blocks inside one string each evaluate independently.
    Matcher m = INLINE_MATH_BLOCK.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String inner = m.group(1);
      String evaluated = tryEvaluate(inner, params);
      String replacement = evaluated != null ? "$" + evaluated + "$" : m.group();
      m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(sb);
    String afterInline = sb.toString();

    // 2. If the whole result (or the result with a single outer $...$ stripped)
    //    is itself pure arithmetic, collapse it. Covers options written without
    //    LaTeX wrappers, e.g. raw "{{a}}+{{b}}" → "2+3" → "5".
    String stripped = stripOuterMathDelims(afterInline);
    String wholeEvaluated = tryEvaluate(stripped, params);
    if (wholeEvaluated != null) {
      return afterInline.equals(stripped) ? wholeEvaluated : "$" + wholeEvaluated + "$";
    }
    return afterInline;
  }

  private static final Pattern INLINE_MATH_BLOCK = Pattern.compile("\\$([^$]+)\\$");

  /** Returns evaluated arithmetic value or null when content is not pure arithmetic. */
  private String tryEvaluate(String expr, Map<String, Object> params) {
    if (expr == null) return null;
    String trimmed = expr.trim();
    if (trimmed.isEmpty() || !PURE_ARITHMETIC.matcher(trimmed).matches()) {
      return null;
    }
    String result = evaluateFormula(trimmed, params);
    if (result == null || "?".equals(result) || result.equals(trimmed)) {
      return null;
    }
    return result;
  }

  private String stripOuterMathDelims(String s) {
    if (s == null) return null;
    String t = s.trim();
    if (t.length() >= 2
        && t.startsWith("$")
        && t.endsWith("$")
        && t.indexOf('$', 1) == t.length() - 1) {
      return t.substring(1, t.length() - 1);
    }
    return t;
  }

  private Object resolvePlaceholderValue(String token, Map<String, Object> params) {
    if (token == null) {
      return null;
    }

    String expr = token.trim();
    if (expr.isEmpty()) {
      return null;
    }

    if (params.containsKey(expr)) {
      return params.get(expr);
    }

    String evaluated = evaluateFormula(expr, params);
    if ("?".equals(evaluated)) {
      return null;
    }

    try {
      if (evaluated.matches("^-?\\d+$")) {
        return Long.parseLong(evaluated);
      }
      if (evaluated.matches("^-?\\d*\\.\\d+$")) {
        return Double.parseDouble(evaluated);
      }
    } catch (Exception ignored) {
      // Keep evaluated string if numeric coercion fails.
    }

    return evaluated;
  }

  private String renderDiagramTemplate(String diagramTemplate, Map<String, Object> params) {
    if (diagramTemplate == null || diagramTemplate.isBlank()) {
      return null;
    }

    // Strict mode: only replace exact double-brace placeholders (e.g. {{a}} -> 6).
    // Do not repair or transform any other diagram content.
    if (params == null || params.isEmpty()) {
      return diagramTemplate;
    }

    String rendered = diagramTemplate;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String key = entry.getKey();
      if (key == null || key.isBlank()) {
        continue;
      }
      String token = "{{" + key + "}}";
      String value = formatParameterValue(entry.getValue());
      rendered = rendered.replace(token, value);
    }

    return rendered;
  }

  private String repairDanglingRenderedDoubleBraces(String input) {
    if (input == null || input.isBlank()) {
      return input;
    }

    // Only collapse duplicated braces tokens. Keep normal single braces untouched.
    String fixed = input;
    while (fixed.contains("{{") || fixed.contains("}}")) {
      fixed = fixed.replace("{{", "{").replace("}}", "}");
    }
    return fixed;
  }

  private String repairTikzCoordinatesSyntax(String input) {
    if (input == null || input.isBlank()) {
      return input;
    }

    String fixed = input;
    // Fix: \addplot[...] coordinates (...)  ->  \addplot[...] coordinates { (...)
    fixed = fixed.replaceAll("(\\\\addplot\\s*\\[[^\\]]*]\\s*coordinates)\\s*\\(", "$1 {\\n    (");

    // If opening '{' exists but block ends with ');', convert to proper '};'.
    fixed =
        fixed.replaceAll(
            "(\\\\addplot\\s*\\[[^\\]]*]\\s*coordinates\\s*\\{[\\s\\S]*?)\\)\\s*;", "$1)\\n};");
    return fixed;
  }

  private Map<String, String> buildTemplateOptions(
      QuestionTemplate template, Map<String, Object> params) {
    Map<String, String> built = new LinkedHashMap<>();
    if (template == null || template.getOptionsGenerator() == null) {
      return built;
    }

    String[] keys = {"A", "B", "C", "D"};
    for (String key : keys) {
      Object value = template.getOptionsGenerator().get(key);
      if (value == null) {
        continue;
      }
      String raw = extractTemplateOptionRawValue(value);
      if (raw == null || raw.isBlank()) {
        continue;
      }
      built.put(key, renderOptionValue(raw, params));
    }
    return built;
  }

  @SuppressWarnings("unchecked")
  private String extractTemplateOptionRawValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String s) {
      return s;
    }
    if (value instanceof Map<?, ?> map) {
      Object nested = map.get("value");
      if (nested instanceof String nestedStr) {
        return nestedStr;
      }
      Object text = map.get("text");
      if (text instanceof String textStr) {
        return textStr;
      }
    }
    return String.valueOf(value);
  }

  private boolean aiOptionsUseTemplateParameters(JsonNode optionsNode, Map<String, Object> params) {
    if (optionsNode == null || !optionsNode.isObject() || params == null || params.isEmpty()) {
      return false;
    }

    Iterator<String> fields = optionsNode.fieldNames();
    while (fields.hasNext()) {
      String key = fields.next();
      JsonNode valueNode = optionsNode.get(key);
      if (valueNode == null || valueNode.isNull()) {
        continue;
      }
      String raw = valueNode.asText("");
      if (PLACEHOLDER_PATTERN.matcher(raw).find()) {
        return true;
      }

      for (String paramName : params.keySet()) {
        if (raw.matches(".*\\b" + Pattern.quote(paramName) + "\\b.*")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean containsValueApprox(Map<String, String> options, String targetValue) {
    return findKeyByValue(options, targetValue) != null;
  }

  /** Attempt to repair common truncation: add missing closing braces/brackets */
  private String repairTruncatedJson(String json) {
    if (json == null) return "{}";
    String trimmed = json.trim();
    if (trimmed.isEmpty()) return "{}";
    // Count unclosed braces/brackets
    int braces = 0, brackets = 0;
    boolean inString = false;
    boolean escaped = false;
    for (char c : trimmed.toCharArray()) {
      if (escaped) {
        escaped = false;
        continue;
      }
      if (c == '\\') {
        escaped = true;
        continue;
      }
      if (c == '"') {
        inString = !inString;
        continue;
      }
      if (!inString) {
        if (c == '{') braces++;
        else if (c == '}') braces--;
        else if (c == '[') brackets++;
        else if (c == ']') brackets--;
      }
    }
    StringBuilder sb = new StringBuilder(trimmed);
    // If last char is inside an incomplete string or ends with comma, clean up
    if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
    for (int i = 0; i < brackets; i++) sb.append(']');
    for (int i = 0; i < braces; i++) sb.append('}');
    return sb.toString();
  }

  /** Ensure options map has exactly A,B,C,D. Fill missing keys with distractor values. */
  private void ensureFourOptions(
      Map<String, String> options, String correctAnswer, Map<String, Object> params) {
    String[] keys = {"A", "B", "C", "D"};
    double base;
    try {
      base = Double.parseDouble(correctAnswer.replaceAll("[^0-9.\\-]", ""));
    } catch (Exception e) {
      base = 1.0;
    }

    int offset = 1;
    for (String k : keys) {
      if (!options.containsKey(k)) {
        // generate a unique distractor
        String candidate;
        do {
          candidate = formatNum(base + offset);
          offset++;
        } while (options.containsValue(candidate));
        options.put(k, candidate);
      }
    }
    // Remove extra keys beyond A-D
    options.keySet().retainAll(Arrays.asList(keys));
  }

  /** Build simple fallback options with Java-computed correct answer + 3 numeric distractors */
  private Map<String, String> buildFallbackOptions(
      String correctAnswer, Map<String, Object> params) {
    Map<String, String> opts = new LinkedHashMap<>();
    double base;
    try {
      base = Double.parseDouble(correctAnswer.replaceAll("[^0-9.\\-]", ""));
    } catch (Exception e) {
      base = 1.0;
    }

    List<String> vals = new ArrayList<>();
    vals.add(correctAnswer);
    int[] offsets = {1, -1, 2};
    for (int off : offsets) {
      String v = formatNum(base + off);
      if (!vals.contains(v)) vals.add(v);
    }
    while (vals.size() < 4) vals.add(formatNum(base + vals.size()));

    // Shuffle deterministically
    Collections.shuffle(vals, new Random(correctAnswer.hashCode()));
    String[] keys = {"A", "B", "C", "D"};
    for (int i = 0; i < 4; i++) opts.put(keys[i], vals.get(i));
    return opts;
  }

  private String formatNum(double val) {
    if (val == Math.floor(val) && !Double.isInfinite(val)) return String.valueOf((long) val);
    return formatDecimal(val, 2);
  }

  /** Find the map key whose value matches targetValue (numeric-tolerant). */
  private String findKeyByValue(Map<String, String> options, String targetValue) {
    if (options == null || targetValue == null) return null;
    for (Map.Entry<String, String> entry : options.entrySet()) {
      try {
        double optVal = parseNumericAnswer(entry.getValue());
        double target = parseNumericAnswer(targetValue);
        if (Math.abs(optVal - target) < 0.01) return entry.getKey();
      } catch (NumberFormatException e) {
        if (entry.getValue().trim().equalsIgnoreCase(targetValue.trim())) return entry.getKey();
      }
    }
    return null;
  }

  private String normalizeNumericLocale(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace('−', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replaceAll("(?<=\\d),(?=\\d)", ".");
  }

  private boolean isIntegerParameterType(String type) {
    if (type == null) {
      return true;
    }
    String normalized = type.trim().toLowerCase(Locale.ROOT);
    return normalized.equals("integer")
        || normalized.equals("int")
        || normalized.equals("long")
        || normalized.equals("short")
        || normalized.equals("byte")
        || normalized.equals("whole");
  }

  private String formatParameterValue(Object value) {
    if (!(value instanceof Number)) {
      return String.valueOf(value);
    }

    Number n = (Number) value;
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) {
      return String.valueOf(n.longValue());
    }

    double d = n.doubleValue();
    if (d == Math.floor(d) && !Double.isInfinite(d)) {
      return String.valueOf((long) d);
    }

    String formatted = formatDecimal(d, 4);
    return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
  }

  private String formatDecimal(double value, int scale) {
    return String.format(Locale.ROOT, "%." + scale + "f", value);
  }

  private String serializeParamsForPrompt(Map<String, Object> params) {
    if (params == null || params.isEmpty()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(entry.getKey()).append("=").append(formatParameterValue(entry.getValue()));
    }
    sb.append("}");
    return sb.toString();
  }

  // =========================================================================
  // FEATURE 1: AI Auto-Extract Parameters From Question Text
  // =========================================================================

  @Override
  public ExtractParametersResponse extractParameters(
      java.util.UUID templateId, ExtractParametersRequest request) {
    log.info("[Feature1] Extracting parameters from template text for templateId={}", templateId);

    String prompt = buildExtractParametersPrompt(request);
    try {
      String aiContent = geminiService.sendMessage(prompt);
      return parseExtractParametersResponse(aiContent);
    } catch (Exception e) {
      log.error("[Feature1] extractParameters AI call failed: {}", e.getMessage(), e);
      throw new RuntimeException("AI parameter extraction failed: " + e.getMessage(), e);
    }
  }

  private String buildExtractParametersPrompt(ExtractParametersRequest req) {
    StringBuilder p = new StringBuilder();
    p.append("Return ONLY valid JSON. No markdown, no extra text.\n\n");
    p.append("Task: extract_parameters\n");
    p.append(
        "You are a Vietnamese math teacher assistant. Read the question text and all related fields.\n");
    p.append(
        "Identify which numbers are CHANGEABLE parameters (should become {{param}} placeholders)\n");
    p.append("and which are FIXED structural values that must NOT change.\n\n");

    p.append("RULES FOR CHANGEABLE (make into {{param}}):\n");
    p.append("- Independent input values (coefficients, constants)\n");
    p.append("- Numbers that directly affect the answer\n");
    p.append(
        "- Numbers that appear as INPUTS in answer_formula or solution_steps (not results)\n\n");

    p.append("RULES FOR FIXED (do NOT change):\n");
    p.append("- Structural math: exponents (x², x³), indices, powers\n");
    p.append("- Mathematical constants: π, e, 0, 1 when structural\n");
    p.append("- Derived values: computed FROM other parameters\n");
    p.append("  e.g. 'tổng nghiệm = -b/a = 3' → 3 is derived, not param\n");
    p.append("- LaTeX command syntax (NEVER touch \\frac, \\sqrt, \\left, etc.)\n");
    p.append("- Step numbers in solution_steps\n\n");

    p.append("SAME NUMBER IN MULTIPLE ROLES:\n");
    p.append("- Distinguish by math context and position\n");
    p.append("- e.g. '2' as coefficient (changeable) vs '2' in x² (fixed)\n\n");

    p.append("INPUT FIELDS:\n");
    p.append("template_text: ").append(req.getTemplateText()).append("\n");
    if (req.getAnswerFormula() != null) {
      p.append("answer_formula: ").append(req.getAnswerFormula()).append("\n");
    }
    if (req.getSolutionSteps() != null) {
      p.append("solution_steps: ").append(req.getSolutionSteps()).append("\n");
    }
    if (req.getDiagramLatex() != null) {
      p.append("diagram_latex: ").append(req.getDiagramLatex()).append("\n");
    }
    if (req.getOptions() != null && !req.getOptions().isEmpty()) {
      p.append("options: ").append(req.getOptions()).append("\n");
    }
    if (req.getClauses() != null && !req.getClauses().isEmpty()) {
      p.append("clauses: ").append(req.getClauses()).append("\n");
    }

    p.append("\nOUTPUT FORMAT (strict JSON):\n");
    p.append("{\n");
    p.append("  \"suggested_params\": [\n");
    p.append("    {\n");
    p.append("      \"original_value\": \"2\",\n");
    p.append("      \"location\": \"2x² (leading coefficient)\",\n");
    p.append("      \"suggested_name\": \"a\",\n");
    p.append("      \"reason\": \"Independent coefficient, affects roots\",\n");
    p.append("      \"changeable\": true\n");
    p.append("    }\n");
    p.append("  ],\n");
    p.append("  \"fixed_values\": [\n");
    p.append("    {\n");
    p.append("      \"original_value\": \"2\",\n");
    p.append("      \"location\": \"x² (exponent)\",\n");
    p.append("      \"reason\": \"Structural, defines equation degree\"\n");
    p.append("    }\n");
    p.append("  ],\n");
    p.append("  \"template_result\": \"Cho phương trình {{a}}x² + {{b}}x + {{c}} = 0...\"\n");
    p.append("}\n");
    return p.toString();
  }

  private ExtractParametersResponse parseExtractParametersResponse(String aiContent) {
    try {
      String json = repairTruncatedJson(extractJSON(aiContent));
      JsonNode root = objectMapper.readTree(json);

      List<ExtractParametersResponse.SuggestedParam> suggested = new ArrayList<>();
      if (root.has("suggested_params")) {
        for (JsonNode node : root.get("suggested_params")) {
          suggested.add(
              ExtractParametersResponse.SuggestedParam.builder()
                  .originalValue(node.path("original_value").asText())
                  .location(node.path("location").asText())
                  .suggestedName(node.path("suggested_name").asText())
                  .reason(node.path("reason").asText())
                  .changeable(true)
                  .build());
        }
      }

      List<ExtractParametersResponse.FixedValue> fixed = new ArrayList<>();
      if (root.has("fixed_values")) {
        for (JsonNode node : root.get("fixed_values")) {
          fixed.add(
              ExtractParametersResponse.FixedValue.builder()
                  .originalValue(node.path("original_value").asText())
                  .location(node.path("location").asText())
                  .reason(node.path("reason").asText())
                  .build());
        }
      }

      String templateResult = root.path("template_result").asText("");

      return ExtractParametersResponse.builder()
          .suggestedParams(suggested)
          .fixedValues(fixed)
          .templateResult(templateResult)
          .build();
    } catch (Exception e) {
      log.error("[Feature1] Failed to parse extract-parameters AI response: {}", e.getMessage());
      throw new RuntimeException("Failed to parse AI extract-parameters response", e);
    }
  }

  // =========================================================================
  // FEATURE 2: AI Generates Parameter Values
  // =========================================================================

  @Override
  public GenerateParametersResponse generateParameters(
      java.util.UUID templateId, GenerateParametersRequest request) {
    log.info("[Feature2] Generating parameter values for templateId={}", templateId);
    String prompt = buildGenerateParametersPrompt(request, null);
    try {
      String aiContent = geminiService.sendMessage(prompt);
      GenerateParametersResponse response = parseGenerateParametersResponse(aiContent);
      // Build filled text preview
      if (request.getTemplateText() != null && response.getParameters() != null) {
        String preview =
            fillTextWithNegativeParens(request.getTemplateText(), response.getParameters());
        response.setFilledTextPreview(preview);
      }
      return response;
    } catch (Exception e) {
      log.error("[Feature2] generateParameters AI call failed: {}", e.getMessage(), e);
      throw new RuntimeException("AI parameter generation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public GenerateParametersResponse updateParameters(
      java.util.UUID templateId, UpdateParametersRequest request) {
    log.info(
        "[Feature2] Updating parameter values for templateId={}, command='{}'",
        templateId,
        request.getTeacherCommand());
    String prompt = buildUpdateParametersPrompt(request);
    try {
      String aiContent = geminiService.sendMessage(prompt);
      GenerateParametersResponse response = parseGenerateParametersResponse(aiContent);
      if (request.getTemplateText() != null && response.getParameters() != null) {
        String preview =
            fillTextWithNegativeParens(request.getTemplateText(), response.getParameters());
        response.setFilledTextPreview(preview);
      }
      return response;
    } catch (Exception e) {
      log.error("[Feature2] updateParameters AI call failed: {}", e.getMessage(), e);
      throw new RuntimeException("AI parameter update failed: " + e.getMessage(), e);
    }
  }

  private String buildGenerateParametersPrompt(
      GenerateParametersRequest req, String additionalInstruction) {
    StringBuilder p = new StringBuilder();
    p.append("Return ONLY valid JSON. No markdown, no extra text.\n\n");
    p.append("Task: generate_parameters\n");
    p.append("You are a Vietnamese math teacher assistant.\n");
    p.append("Read ALL content fields below. Detect math constraints from the formula.\n");
    p.append(
        "Generate a valid, non-duplicate parameter combination that satisfies ALL constraints.\n\n");

    p.append("CONSTRAINT DETECTION RULES:\n");
    p.append("- 'sqrt(b²-4ac)' in formula → b²-4ac ≥ 0 required\n");
    p.append("- '/ 2a' in formula          → a ≠ 0 required\n");
    p.append("- Integer options            → parameters should yield integer answers\n");
    p.append("- 'has 2 distinct roots' in clauses → delta > 0 required\n\n");

    p.append("CONTENT FIELDS:\n");
    p.append("template_text: ").append(req.getTemplateText()).append("\n");
    if (req.getAnswerFormula() != null) {
      p.append("answer_formula: ").append(req.getAnswerFormula()).append("\n");
    }
    if (req.getSolutionSteps() != null) {
      p.append("solution_steps: ").append(req.getSolutionSteps()).append("\n");
    }
    if (req.getDiagramLatex() != null) {
      p.append("diagram_latex: ").append(req.getDiagramLatex()).append("\n");
    }
    if (req.getOptions() != null && !req.getOptions().isEmpty()) {
      p.append("options: ").append(req.getOptions()).append("\n");
    }
    if (req.getClauses() != null && !req.getClauses().isEmpty()) {
      p.append("clauses: ").append(req.getClauses()).append("\n");
    }
    if (req.getParameters() != null && !req.getParameters().isEmpty()) {
      p.append("parameter_names: ").append(req.getParameters()).append("\n");
    }
    if (req.getSampleQuestions() != null && !req.getSampleQuestions().isEmpty()) {
      p.append("existing_samples (AVOID DUPLICATES): ")
          .append(req.getSampleQuestions())
          .append("\n");
    }
    if (additionalInstruction != null) {
      p.append("\nADDITIONAL TEACHER REQUIREMENT (highest priority):\n");
      p.append(additionalInstruction).append("\n");
    }

    p.append("\nOUTPUT FORMAT (strict JSON):\n");
    p.append("{\n");
    p.append("  \"parameters\": {\"a\": 2, \"b\": -3, \"c\": 1},\n");
    p.append("  \"constraint_text\": {\n");
    p.append("    \"a\": \"a = 2, số nguyên dương, khác 0 để giữ bậc 2\",\n");
    p.append("    \"b\": \"b = -3, đảm bảo delta = 9 - 8 = 1 ≥ 0\",\n");
    p.append("    \"c\": \"c = 1, cho nghiệm thực x = 1 và x = 0.5\"\n");
    p.append("  },\n");
    p.append("  \"combined_constraints\": [\n");
    p.append("    \"b² - 4ac = 1 ≥ 0: phương trình có nghiệm thực\",\n");
    p.append("    \"Bộ {a:2, b:-3, c:1} chưa tồn tại trong hệ thống\"\n");
    p.append("  ]\n");
    p.append("}\n");
    return p.toString();
  }

  private String buildUpdateParametersPrompt(UpdateParametersRequest req) {
    StringBuilder p = new StringBuilder();
    p.append("Return ONLY valid JSON. No markdown, no extra text.\n\n");
    p.append("Task: update_parameters\n");
    p.append("You are a Vietnamese math teacher assistant.\n");
    p.append("Current parameter values: ").append(req.getCurrentParameters()).append("\n");
    p.append("Current constraint explanations: ")
        .append(req.getCurrentConstraintText())
        .append("\n");
    p.append("Teacher command (NEW REQUIREMENT - highest priority): " + req.getTeacherCommand())
        .append("\n");
    if (req.getTemplateText() != null) {
      p.append("Template text: ").append(req.getTemplateText()).append("\n");
    }
    if (req.getAnswerFormula() != null) {
      p.append("Formula: ").append(req.getAnswerFormula()).append("\n");
    }
    p.append(
        "\nRe-generate parameter values satisfying ALL existing constraints PLUS the teacher command.\n");
    p.append("Update constraint_text to reflect the new values and teacher requirement.\n");

    p.append("\nOUTPUT FORMAT (same as generate_parameters):\n");
    p.append(
        "{\"parameters\": {...}, \"constraint_text\": {...}, \"combined_constraints\": [...]}\n");
    return p.toString();
  }

  private GenerateParametersResponse parseGenerateParametersResponse(String aiContent) {
    try {
      String json = repairTruncatedJson(extractJSON(aiContent));
      JsonNode root = objectMapper.readTree(json);

      // Parse parameters
      Map<String, Object> parameters = new LinkedHashMap<>();
      JsonNode paramsNode = root.path("parameters");
      if (paramsNode.isObject()) {
        Iterator<String> fields = paramsNode.fieldNames();
        while (fields.hasNext()) {
          String key = fields.next();
          JsonNode val = paramsNode.get(key);
          if (val.isIntegralNumber()) {
            parameters.put(key, val.intValue());
          } else if (val.isFloatingPointNumber()) {
            parameters.put(key, val.doubleValue());
          } else {
            parameters.put(key, val.asText());
          }
        }
      }

      // Parse constraint_text
      Map<String, String> constraintText = new LinkedHashMap<>();
      JsonNode ctNode = root.path("constraint_text");
      if (ctNode.isObject()) {
        Iterator<String> fields = ctNode.fieldNames();
        while (fields.hasNext()) {
          String key = fields.next();
          constraintText.put(key, ctNode.get(key).asText());
        }
      }

      // Parse combined_constraints
      List<String> combinedConstraints = new ArrayList<>();
      JsonNode ccNode = root.path("combined_constraints");
      if (ccNode.isArray()) {
        for (JsonNode item : ccNode) {
          combinedConstraints.add(item.asText());
        }
      }

      return GenerateParametersResponse.builder()
          .parameters(parameters)
          .constraintText(constraintText)
          .combinedConstraints(combinedConstraints)
          .build();
    } catch (Exception e) {
      log.error("[Feature2] Failed to parse generate-parameters AI response: {}", e.getMessage());
      throw new RuntimeException("Failed to parse AI generate-parameters response", e);
    }
  }

  /**
   * Fill template text with parameter values, wrapping negative values in parentheses.
   * BUG FIX 1: This is the CORRECT way to fill a template before sending it to AI.
   * e.g. {{a}}x² + {{b}}x + {{c}} with {a=2, b=-3, c=1}
   *   → "2x² + (-3)x + 1"
   */
  private String fillTextWithNegativeParens(String text, Map<String, Object> params) {
    if (text == null || params == null) return text;
    String filled = text;
    for (Map.Entry<String, Object> entry : params.entrySet()) {
      String token = "{{" + entry.getKey() + "}}";
      Object val = entry.getValue();
      String valStr;
      if (val instanceof Number) {
        double d = ((Number) val).doubleValue();
        if (d < 0) {
          valStr = "(" + formatParameterValue(val) + ")";
        } else {
          valStr = formatParameterValue(val);
        }
      } else {
        valStr = String.valueOf(val);
      }
      filled = filled.replace(token, valStr);
    }
    // Normalize sign combinations: "N + -M" → "N - M", "N - -M" → "N + M"
    filled = filled.replaceAll("\\+\\s*-\\(", "- (");
    filled = filled.replaceAll("-\\s*-\\(", "+ (");
    return filled;
  }

  // =========================================================================
  // FEATURE 4: Set Overdrive Points Per Clause (TF Only)
  // =========================================================================

  @Override
  public void setClausePoints(java.util.UUID questionId, SetClausePointsRequest request) {
    log.info("[Feature4] Setting clause points for questionId={}", questionId);

    Question question =
        questionRepository
            .findById(questionId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

    if (question.getQuestionType() != QuestionType.TRUE_FALSE) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "Clause points can only be set for TRUE_FALSE questions");
    }

    // Validate: sum(clause_points) must equal total_point
    BigDecimal totalPoint = request.getTotalPoint();
    Map<String, BigDecimal> clausePoints = request.getClausePoints();
    BigDecimal sum = clausePoints.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    if (sum.compareTo(totalPoint) != 0) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          String.format(
              "Clause points must sum to total question point (%.2f). Current sum: %.2f",
              totalPoint.doubleValue(), sum.doubleValue()));
    }

    // Merge overdrive_point into existing options JSON per clause
    @SuppressWarnings("unchecked")
    Map<String, Object> options =
        question.getOptions() != null
            ? new LinkedHashMap<>(question.getOptions())
            : new LinkedHashMap<>();

    for (Map.Entry<String, BigDecimal> entry : clausePoints.entrySet()) {
      String clauseKey = entry.getKey();
      BigDecimal clausePoint = entry.getValue();

      Object existingClause = options.get(clauseKey);
      if (existingClause instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> clauseMap = new LinkedHashMap<>((Map<String, Object>) existingClause);
        clauseMap.put("overdrive_point", clausePoint.doubleValue());
        options.put(clauseKey, clauseMap);
      } else {
        // Create a new clause map if it doesn't exist
        Map<String, Object> newClause = new LinkedHashMap<>();
        newClause.put("text", existingClause != null ? String.valueOf(existingClause) : "");
        newClause.put("overdrive_point", clausePoint.doubleValue());
        options.put(clauseKey, newClause);
      }
    }

    question.setOptions(options);
    question.setPoints(totalPoint);
    questionRepository.save(question);
  }

  // =========================================================================
  // FEATURE 5: Validate TF Clause Content For Matrix Selection
  // =========================================================================

  @Override
  public boolean validateClauseForMatrix(
      String clauseText, String chapterName, String cognitiveLevel) {
    log.info(
        "[Feature5] Validating clause for chapter='{}' level='{}'", chapterName, cognitiveLevel);
    if (clauseText == null || clauseText.isBlank()) return false;

    String prompt = buildClauseValidationPrompt(clauseText, chapterName, cognitiveLevel);
    try {
      String aiResponse = geminiService.sendMessage(prompt);
      // Expect a simple YES/NO answer
      String normalized = aiResponse.trim().toUpperCase();
      boolean valid = normalized.startsWith("YES");
      log.info(
          "[Feature5] Clause validation result: {} for chapter='{}' level='{}'",
          valid ? "VALID" : "INVALID",
          chapterName,
          cognitiveLevel);
      return valid;
    } catch (Exception e) {
      log.error("[Feature5] Clause validation AI call failed: {}", e.getMessage());
      // Fail open to avoid blocking question generation
      return true;
    }
  }

  private String buildClauseValidationPrompt(
      String clauseText, String chapterName, String cognitiveLevel) {
    StringBuilder p = new StringBuilder();
    p.append("You are a Vietnamese math teacher. Answer with ONLY 'YES' or 'NO'.\n\n");
    p.append("Given:\n");
    p.append("  Chapter: ").append(chapterName).append("\n");
    p.append("  Cognitive level: ").append(cognitiveLevel).append("\n");
    p.append("  Clause: ").append(clauseText).append("\n\n");
    p.append("Does this clause MATCH both the chapter content and the cognitive level?\n");
    p.append("Answer YES if it matches both. Answer NO if it does not match one or both.\n");
    p.append("Format: YES [reason] or NO [reason]\n");
    return p.toString();
  }

  /**
   * Constraint-aware batch parameter generation. Delegates to the unified
   * {@link com.fptu.math_master.service.BlueprintService} which runs one Gemini
   * call for the whole batch and validates each set against simple programmatic
   * guardrails. Returns at most {@code count} sets — caller must tolerate a
   * shorter list when constraints are over-tight.
   */
  @Override
  public List<Map<String, Object>> generateParameterBatch(QuestionTemplate template, int count) {
    if (count <= 0) return Collections.emptyList();
    return blueprintService.selectValueSets(template, count, Collections.emptyList(), null);
  }

  // ─────────────────────────────────────────────────────────────────────────────────────
  //  Method 1 reverse-templating — delegates to BlueprintService and re-shapes the
  //  output as the legacy AutoBlueprintResponse so existing callers keep compiling.
  //  New callers use POST /question-templates/blueprint-from-real-question, which
  //  returns the richer BlueprintFromRealQuestionResponse directly.
  // ─────────────────────────────────────────────────────────────────────────────────────
  @Override
  public AutoBlueprintResponse autoBlueprint(AutoBlueprintRequest request) {
    BlueprintFromRealQuestionResponse rich = blueprintService.blueprintFromRealQuestion(request);

    // Convert the rich response into the legacy QuestionTemplateRequest wrapper so this
    // method's contract stays stable. The new shape is preserved inside `parameters`.
    QuestionTemplateRequest blueprint = new QuestionTemplateRequest();
    blueprint.setName(buildDraftTemplateName(request));
    blueprint.setDescription("Auto-generated from a real question via Method 1");
    blueprint.setTemplateType(request.getQuestionType());
    blueprint.setTemplateText(
        Map.of("vi", rich.getTemplateText() == null ? "" : rich.getTemplateText()));
    blueprint.setAnswerFormula(rich.getAnswerFormula());
    blueprint.setSolutionStepsTemplate(rich.getSolutionStepsTemplate());
    blueprint.setDiagramTemplate(rich.getDiagramTemplate());
    if (rich.getOptionsGenerator() != null) {
      blueprint.setOptionsGenerator(new HashMap<>(rich.getOptionsGenerator()));
    }
    if (rich.getClauseTemplates() != null && !rich.getClauseTemplates().isEmpty()) {
      List<Map<String, Object>> clauseList =
          rich.getClauseTemplates().stream()
              .map(
                  c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("text", c.getText());
                    m.put("truthValue", c.isTruthValue());
                    return m;
                  })
              .toList();
      blueprint.setStatementMutations(Map.of("clauseTemplates", clauseList));
    }
    Map<String, Object> paramMap = new LinkedHashMap<>();
    if (rich.getParameters() != null) {
      for (BlueprintParameter p : rich.getParameters()) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("constraintText", p.getConstraintText());
        def.put("sampleValue", p.getSampleValue());
        def.put("occurrences", p.getOccurrences() == null ? List.of() : p.getOccurrences());
        paramMap.put(p.getName(), def);
      }
    }
    blueprint.setParameters(paramMap);
    if (rich.getGlobalConstraints() != null) {
      blueprint.setConstraints(rich.getGlobalConstraints().toArray(new String[0]));
    }
    blueprint.setCognitiveLevel(request.getCognitiveLevel());
    // Stamp variant for downstream telemetry
    // (the entity field, set on persistence — we hint via name only here).
    return AutoBlueprintResponse.builder()
        .blueprint(blueprint)
        .extractionNotes(rich.getWarnings() == null ? List.of() : rich.getWarnings())
        .confidence(rich.getConfidence())
        .build();
  }

  private String buildDraftTemplateName(AutoBlueprintRequest req) {
    String stem = req.getQuestionText() == null ? "Untitled" : req.getQuestionText().trim();
    int max = 60;
    if (stem.length() > max) stem = stem.substring(0, max - 1) + "…";
    String type = req.getQuestionType() == null ? "TPL" : req.getQuestionType().name();
    return "[" + type + "] " + stem;
  }

  /** Sanity reference so the variant enum import is used; emits a debug log only. */
  @SuppressWarnings("unused")
  private void touchVariantEnum() {
    log.trace("variant in scope: {}", TemplateVariant.AI_REVERSE_TEMPLATED);
  }
}
