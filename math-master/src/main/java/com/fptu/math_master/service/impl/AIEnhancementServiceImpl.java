package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.entity.CanonicalQuestion;
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

  private static final Pattern PLACEHOLDER_PATTERN =
      Pattern.compile("\\{\\{\\s*(.+?)\\s*}}|\\{\\s*(.+?)\\s*}");
  private static final Pattern DOUBLE_BRACE_PLACEHOLDER_PATTERN =
      Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}}");

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
      double num = Double.parseDouble(normalizeNumericLocale(normalized).replaceAll("[^0-9.-]", ""));
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
          .solutionSteps("Error: Formula evaluation failed")
          .diagramData(template.getDiagramTemplate())
          .calculatedDifficulty(QuestionDifficulty.MEDIUM)
          .usedParameters(params)
          .answerCalculation("Error: " + template.getAnswerFormula())
          .build();
    }

    QuestionDifficulty difficulty = determineDifficulty(null, params);
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
          .solutionSteps("Áp dụng công thức: " + template.getAnswerFormula() + " = " + correctAnswerStr)
          .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), params))
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(template.getAnswerFormula() + " = " + correctAnswerStr)
          .build();
    }
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
          ? fillText(canonicalQuestion.getProblemText(), params)
          : null;
      String canonicalSolution =
        canonicalQuestion.getSolutionSteps() != null
          ? fillText(canonicalQuestion.getSolutionSteps(), params)
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
                canonicalQuestion, template, params, fallbackQuestionText, fallbackExplanation, sampleIndex);
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
            .options(template.getTemplateType() == QuestionType.MULTIPLE_CHOICE ? fallbackOptions : null)
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
    p.append("Keep mathematical consistency with canonical source, but wording can vary naturally.\n\n");

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
    p.append("PARAMETERS (backend picked): ").append(serializeParamsForPrompt(params)).append("\n\n");

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
      questionText = fillText(questionText, effectiveParams);

      String explanation = root.path("explanation").asText();
      if (explanation == null || explanation.isBlank()) {
        explanation = fallbackExplanation;
      }
      explanation = fillText(explanation, effectiveParams);

      String solutionSteps = root.path("solutionSteps").asText();
      if (solutionSteps == null || solutionSteps.isBlank()) {
        solutionSteps = explanation;
      }
      solutionSteps = fillText(solutionSteps, effectiveParams);

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
          .options(template.getTemplateType() == QuestionType.MULTIPLE_CHOICE ? fallbackOptions : null)
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

  private Map<String, String> parseCanonicalOptions(JsonNode optionsNode, Map<String, Object> params) {
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
        "6. explanation = correct solution steps in Vietnamese, 2-3 sentences, no self-correction.\n");
    p.append("7. answerCalculation = simple expression like \"(c - b) / a = ")
        .append(correctAnswer)
        .append("\".\n\n");
    p.append("8. questionText and explanation MUST be natural Vietnamese with proper accents (UTF-8), no transliteration.\n");
    p.append("9. Numeric values must use plain format: no thousands separators, use dot for decimal if needed (e.g. 3.5).\n\n");
    p.append(
      "10. IMPORTANT: keep parameter placeholders in double braces (e.g. {{a}}, {{x1}}, {{yMax}}). Do NOT replace placeholders with concrete numbers; backend will substitute values.\n\n");
    p.append(
      "11. You MAY propose usedParameters as numeric values. Backend will validate and use them to render final diagram and substitute placeholders.\n\n");

    p.append("JSON format:\n");
    if (optionPlaceholderMode) {
      p.append(
        "{\"questionText\":\"...\",\"options\":{},");
    } else {
      p.append(
        "{\"questionText\":\"...\",\"options\":{\"A\":\"n\",\"B\":\"n\",\"C\":\"n\",\"D\":\"n\"},");
    }
    p.append(
        "\"correctAnswer\":\"X\",\"explanation\":\"...\",\"difficulty\":\"EASY|MEDIUM|HARD\",");
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
      questionText = fillText(questionText, effectiveParams);

      String explanation = root.path("explanation").asText();
      if (explanation == null || explanation.isBlank()) {
        explanation = "Solution provided by AI";
      }
      explanation = fillText(explanation, effectiveParams);

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
            "LLM did not include correct answer '{}' in options — injected at A", effectiveCorrectAnswer);
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
          .solutionSteps(buildSolutionSteps(explanation))
          .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), effectiveParams))
          .calculatedDifficulty(difficulty)
          .usedParameters(effectiveParams)
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
          .solutionSteps(buildSolutionSteps("Áp dụng công thức: " + template.getAnswerFormula() + " = " + correctAnswer))
          .diagramData(renderDiagramTemplate(template.getDiagramTemplate(), params))
          .calculatedDifficulty(difficulty)
          .usedParameters(params)
          .answerCalculation(template.getAnswerFormula() + " = " + correctAnswer)
          .build();
    }
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
   */
  private String renderOptionValue(String rawValue, Map<String, Object> params) {
    if (rawValue == null) {
      return null;
    }

    boolean hasPlaceholder = PLACEHOLDER_PATTERN.matcher(rawValue).find();
    if (!hasPlaceholder) {
      return rawValue;
    }

    String rendered = fillText(rawValue, params);
    return rendered == null ? rawValue : rendered;
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
    fixed = fixed.replaceAll(
        "(\\\\addplot\\s*\\[[^\\]]*]\\s*coordinates)\\s*\\(", "$1 {\\n    (");

    // If opening '{' exists but block ends with ');', convert to proper '};'.
    fixed = fixed.replaceAll(
        "(\\\\addplot\\s*\\[[^\\]]*]\\s*coordinates\\s*\\{[\\s\\S]*?)\\)\\s*;",
        "$1)\\n};");
    return fixed;
  }

  private Map<String, String> buildTemplateOptions(QuestionTemplate template, Map<String, Object> params) {
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
}
