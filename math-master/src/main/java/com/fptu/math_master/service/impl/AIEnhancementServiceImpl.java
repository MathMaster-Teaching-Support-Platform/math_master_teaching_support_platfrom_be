package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.GeminiService;
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

  GeminiService geminiService;
  ObjectMapper objectMapper = new ObjectMapper();

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

          // Clean option value: remove any explanatory text in parentheses
          String cleanedValue = cleanOptionValue(rawValue);
          options.put(key, cleanedValue);
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

    // Remove anything in parentheses
    String cleaned = value.replaceAll("\\s*\\([^)]*\\)", "").trim();

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
    String cleaned = answer.trim().replaceAll("[^0-9.-]", "");
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
      double num = Double.parseDouble(normalized.replaceAll("[^0-9.-]", ""));
      return String.format("%.2f", num);
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
    log.info(
        "Generating question from template '{}' using LLM (sample #{})",
        template.getName(),
        sampleIndex + 1);

    // Step 1: always compute params + answer in Java first (guaranteed correct)
    Map<String, Object> params = pickParameters(template, sampleIndex);
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
          .calculatedDifficulty(QuestionDifficulty.MEDIUM)
          .usedParameters(params)
          .answerCalculation("Error: " + template.getAnswerFormula())
          .build();
    }

    QuestionDifficulty difficulty = determineDifficulty(template.getDifficultyRules(), params);
    String questionTextBase = fillTemplateText(template.getTemplateText(), params);

    try {
      // Step 2: ask LLM only for question wording + 3 distractors
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
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(template.getAnswerFormula() + " = " + correctAnswerStr)
          .build();
    }
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

      if ("integer".equalsIgnoreCase(type)) {
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

    // If no ScriptEngine available, use simple arithmetic evaluator as fallback
    if (engine == null) {
      try {
        return evaluateFormulaSimple(normalizedFormula, paramsForFormula);
      } catch (Exception fallbackError) {
        log.warn(
            "Simple formula evaluation also failed for '{}': {}",
            normalizedFormula,
            fallbackError.getMessage());
        return "?";
      }
    }

    try {
      for (Map.Entry<String, Object> e : paramsForFormula.entrySet()) {
        engine.put(e.getKey(), e.getValue());
      }
      Object result = engine.eval(normalizedFormula);
      double val = ((Number) result).doubleValue();
      // Format: integer if whole, else strip trailing zeros (e.g. 9.50 → 9.5, 3.25 → 3.25)
      if (val == Math.floor(val) && !Double.isInfinite(val)) {
        return String.valueOf((long) val);
      }
      String formatted = String.format("%.4f", val); // up to 4 decimals to avoid rounding loss
      formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
      return formatted;
    } catch (Exception e) {
      log.warn(
          "ScriptEngine evaluation failed for '{}': {}, trying simple evaluator",
          normalizedFormula,
          e.getMessage());
      // Try simple fallback
      try {
        return evaluateFormulaSimple(normalizedFormula, paramsForFormula);
      } catch (Exception fallbackError) {
        log.warn("Simple fallback also failed: {}", fallbackError.getMessage());
        return "?";
      }
    }
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
      String formatted = String.format("%.4f", result);
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
    return evaluateAddSub(expr, new int[] {0});
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
    double result = evaluatePrimary(expr, pos);
    while (pos[0] < expr.length()) {
      if (expr.charAt(pos[0]) == '*' || expr.charAt(pos[0]) == '/') {
        char op = expr.charAt(pos[0]++);
        double right = evaluatePrimary(expr, pos);
        result = op == '*' ? result * right : result / right;
      } else {
        break;
      }
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

    StringBuilder num = new StringBuilder();

    while (pos[0] < expr.length()
        && (Character.isDigit(expr.charAt(pos[0])) || expr.charAt(pos[0]) == '.')) {
      num.append(expr.charAt(pos[0]++));
    }

    if (num.length() == 0) throw new RuntimeException("Expected number");
    return Double.parseDouble(num.toString());
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
      text = text.replace("{{" + e.getKey() + "}}", e.getValue().toString());
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
    StringBuilder p = new StringBuilder();
    p.append("Return ONLY a JSON object. No markdown, no explanation outside JSON.\n\n");
    p.append("Task: Generate a math question in Vietnamese with 4 options (A,B,C,D).\n\n");

    p.append("Question (already formed): ").append(baseQuestionText).append("\n");
    p.append("Correct answer (pre-computed, DO NOT change): ").append(correctAnswer).append("\n");
    p.append("Parameters used: ").append(params).append("\n");
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
    p.append(
        "2. Create 3 wrong options representing common student mistakes. Numeric values only (e.g. \"5\", \"-3.14\"). NO text.\n");
    p.append("3. All 4 option values must be distinct numbers.\n");
    p.append(
        "4. correctAnswer field MUST be the LETTER KEY (A, B, C, or D) — NOT the numeric value.\n");
    p.append("5. The letter key in correctAnswer must map to value ")
        .append(correctAnswer)
        .append(" in options.\n");
    p.append(
        "6. explanation = correct solution steps in Vietnamese, 2-3 sentences, no self-correction.\n");
    p.append("7. answerCalculation = simple expression like \"(c - b) / a = ")
        .append(correctAnswer)
        .append("\".\n\n");

    p.append("JSON format:\n");
    p.append(
        "{\"questionText\":\"...\",\"options\":{\"A\":\"n\",\"B\":\"n\",\"C\":\"n\",\"D\":\"n\"},");
    p.append(
        "\"correctAnswer\":\"X\",\"explanation\":\"...\",\"difficulty\":\"EASY|MEDIUM|HARD\",");
    p.append("\"usedParameters\":{");
    // inline params so LLM just copies them back
    StringBuilder paramStr = new StringBuilder();
    params.forEach(
        (k, v) -> {
          if (paramStr.length() > 0) paramStr.append(",");
          paramStr.append("\"").append(k).append("\":").append(v);
        });
    p.append(paramStr);
    p.append("},\"answerCalculation\":\"")
        .append(template.getAnswerFormula() != null ? template.getAnswerFormula() : "")
        .append(" = ")
        .append(correctAnswer)
        .append("\"}\n");

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

      String questionText = root.path("questionText").asText();

      // Validate and fallback
      if (questionText == null || questionText.isBlank()) {
        log.warn("questionText is empty, using baseQuestionText");
        questionText = baseQuestionText;
      } else if (questionText.matches("^\\d+$")) {
        // If it's only numbers, it's likely a parsing error
        log.warn(
            "questionText contains only numbers ({}), using baseQuestionText instead",
            questionText);
        questionText = baseQuestionText;
      }

      String explanation = root.path("explanation").asText();
      if (explanation == null || explanation.isBlank()) {
        explanation = "Solution provided by AI";
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
      if (root.has("options") && !root.get("options").isNull()) {
        JsonNode optionsNode = root.get("options");
        Iterator<String> fields = optionsNode.fieldNames();
        while (fields.hasNext()) {
          String k = fields.next();
          options.put(k, cleanOptionValue(optionsNode.get(k).asText()));
        }
      }

      // Ensure all 4 keys exist
      ensureFourOptions(options, correctAnswer, params);

      // Always find the correct key by matching the pre-computed answer
      String correctKey = findKeyByValue(options, correctAnswer);
      if (correctKey == null) {
        // LLM didn't put the correct answer in options — inject it at key A and shift others
        options.put("A", correctAnswer);
        correctKey = "A";
        log.warn(
            "LLM did not include correct answer '{}' in options — injected at A", correctAnswer);
      }

      String answerCalc =
          template.getAnswerFormula() != null
              ? template.getAnswerFormula() + " = " + correctAnswer
              : "= " + correctAnswer;

      return GeneratedQuestionSample.builder()
          .questionText(questionText)
          .options(options)
          .correctAnswer(correctKey) // KEY (A/B/C/D), not numeric value
          .explanation(explanation)
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(answerCalc)
          .build();

    } catch (Exception e) {
      log.error("Failed to parse LLM-generated question, using base fallback: {}", e.getMessage());
      // Build a minimal valid sample using Java-computed values
      Map<String, String> fallbackOptions = buildFallbackOptions(correctAnswer, params);
      String correctKey = findKeyByValue(fallbackOptions, correctAnswer);
      if (correctKey == null) correctKey = "A";
      return GeneratedQuestionSample.builder()
          .questionText(baseQuestionText)
          .options(fallbackOptions)
          .correctAnswer(correctKey) // KEY (A/B/C/D)
          .explanation("Áp dụng công thức: " + template.getAnswerFormula() + " = " + correctAnswer)
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(template.getAnswerFormula() + " = " + correctAnswer)
          .build();
    }
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
    return String.format("%.2f", val);
  }

  /** Find the map key whose value matches targetValue (numeric-tolerant). */
  private String findKeyByValue(Map<String, String> options, String targetValue) {
    if (options == null || targetValue == null) return null;
    for (Map.Entry<String, String> entry : options.entrySet()) {
      try {
        double optVal = Double.parseDouble(entry.getValue().replaceAll("[^0-9.\\-]", ""));
        double target = Double.parseDouble(targetValue.replaceAll("[^0-9.\\-]", ""));
        if (Math.abs(optVal - target) < 0.01) return entry.getKey();
      } catch (NumberFormatException e) {
        if (entry.getValue().trim().equalsIgnoreCase(targetValue.trim())) return entry.getKey();
      }
    }
    return null;
  }
}
