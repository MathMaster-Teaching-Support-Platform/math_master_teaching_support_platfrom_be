package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.OllamaChatResponse;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.OllamaService;
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

  OllamaService ollamaService;
  ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public AIEnhancedQuestionResponse enhanceQuestion(AIEnhancementRequest request) {
    log.info("Enhancing question using Ollama AI");

    try {
      // Build the AI prompt
      String prompt = buildEnhancementPrompt(request);

      // Call Ollama
      OllamaChatResponse ollamaResponse = ollamaService.sendMessage(prompt);

      // Parse the response
      AIEnhancedQuestionResponse enhancedResponse =
          parseAIResponse(ollamaResponse.getMessage().getContent(), request);

      // Validate the AI output
      boolean isValid = validateAIOutput(request, enhancedResponse);
      enhancedResponse.setValid(isValid);

      if (!isValid) {
        log.warn("AI output validation failed, using fallback");
        return createFallbackResponse(request, enhancedResponse.getValidationErrors());
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
    if (!isSameAnswer(
        request.getCorrectAnswer(),
        response.getCorrectAnswerKey(),
        response.getEnhancedOptions())) {
      errors.add("AI changed the correct answer - this is not allowed");
    }

    // 2. Validate MCQ has exactly 4 options (A, B, C, D)
    if (request.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
      if (response.getEnhancedOptions() == null || response.getEnhancedOptions().size() != 4) {
        errors.add("MCQ must have exactly 4 options (A, B, C, D)");
      } else {
        Set<String> expectedKeys = new HashSet<>(Arrays.asList("A", "B", "C", "D"));
        if (!response.getEnhancedOptions().keySet().equals(expectedKeys)) {
          errors.add("MCQ options must be labeled A, B, C, D");
        }
      }
    }

    // 3. Validate content is mathematics-related
    if (!isMathematicsContent(response.getEnhancedQuestionText())) {
      errors.add("Content contains non-mathematics material");
    }

    // 4. Validate question text is not empty
    if (response.getEnhancedQuestionText() == null
        || response.getEnhancedQuestionText().trim().isEmpty()) {
      errors.add("Enhanced question text is empty");
    }

    // 5. Validate no harmful/inappropriate content
    if (containsInappropriateContent(response.getEnhancedQuestionText())) {
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

    prompt.append(
        "You are a mathematics education expert. Your task is to enhance this math question.\n\n");
    prompt.append("IMPORTANT RULES:\n");
    prompt.append("1. DO NOT change the correct answer value\n");
    prompt.append("2. Keep all content strictly about mathematics\n");
    prompt.append(
        "3. For multiple choice questions, provide exactly 4 options labeled A, B, C, D\n");
    prompt.append("4. Make distractors (wrong answers) reflect common student mistakes\n\n");

    prompt.append("ORIGINAL QUESTION:\n");
    prompt.append(request.getRawQuestionText()).append("\n\n");

    prompt.append("CORRECT ANSWER: ").append(request.getCorrectAnswer()).append("\n\n");

    if (request.getQuestionType() == QuestionType.MULTIPLE_CHOICE
        && request.getRawOptions() != null) {
      prompt.append("ORIGINAL OPTIONS:\n");
      request
          .getRawOptions()
          .forEach((key, value) -> prompt.append(key).append(". ").append(value).append("\n"));
      prompt.append("\n");
    }

    prompt.append("DIFFICULTY: ").append(request.getDifficulty()).append("\n");

    if (request.getContext() != null) {
      prompt.append("CONTEXT: ").append(request.getContext()).append("\n");
    }

    prompt.append("\nYour task:\n");
    prompt.append("1. Rewrite the question with clearer, more natural wording\n");
    prompt.append("2. For MCQ: Create 4 options where wrong answers reflect common mistakes\n");
    prompt.append("3. Provide a detailed explanation with solution steps\n");
    prompt.append("4. (Optional) Suggest alternative solution methods\n");
    prompt.append("5. Explain what mistake each wrong option represents\n\n");

    prompt.append("Respond in JSON format:\n");
    prompt.append("{\n");
    prompt.append("  \"enhancedQuestion\": \"...\",\n");
    prompt.append(
        "  \"options\": {\"A\": \"...\", \"B\": \"...\", \"C\": \"...\", \"D\": \"...\"},\n");
    prompt.append("  \"correctAnswerKey\": \"A\",\n");
    prompt.append("  \"explanation\": \"...\",\n");
    prompt.append("  \"alternativeSolutions\": [\"...\"],\n");
    prompt.append(
        "  \"distractorExplanations\": {\"B\": \"Common mistake: ...\", \"C\": \"...\", \"D\": \"...\"}\n");
    prompt.append("}\n");

    return prompt.toString();
  }

  private AIEnhancedQuestionResponse parseAIResponse(
      String aiContent, AIEnhancementRequest request) {
    try {
      // Extract JSON from the response (AI might wrap it in markdown code blocks)
      String jsonContent = extractJSON(aiContent);

      JsonNode root = objectMapper.readTree(jsonContent);

      AIEnhancedQuestionResponse response =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText(root.path("enhancedQuestion").asText())
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
          options.put(key, optionsNode.get(key).asText());
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
    // Try to extract JSON from markdown code blocks
    Pattern pattern = Pattern.compile("```(?:json)?\\s*\\n?(.+?)```", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(content);

    if (matcher.find()) {
      return matcher.group(1).trim();
    }

    // If no code block, try to find JSON object
    int startIdx = content.indexOf('{');
    int endIdx = content.lastIndexOf('}');

    if (startIdx >= 0 && endIdx > startIdx) {
      return content.substring(startIdx, endIdx + 1);
    }

    return content;
  }

  private boolean isSameAnswer(
      String originalAnswer, String answerKey, Map<String, String> options) {
    if (answerKey == null || options == null) {
      return false;
    }

    String aiAnswer = options.get(answerKey);
    if (aiAnswer == null) {
      return false;
    }

    // Normalize and compare
    String normalized1 = normalizeAnswer(originalAnswer);
    String normalized2 = normalizeAnswer(aiAnswer);

    return normalized1.equals(normalized2);
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
}
