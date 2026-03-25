package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.response.TemplateImportResponse;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.TemplateImportService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TemplateImportServiceImpl implements TemplateImportService {

  GeminiService geminiService;
  QuestionTemplateRepository questionTemplateRepository;
  QuestionBankRepository questionBankRepository;
  UserRepository userRepository;
  ObjectMapper objectMapper = new ObjectMapper();

  static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of(
          "application/pdf",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/msword",
          "text/plain");

  @Override
  public TemplateImportResponse importTemplateFromFile(
      MultipartFile file, String subjectHint, String contextHint, UUID questionBankId) {

    log.info("Importing template from file: {}", file.getOriginalFilename());

    // Validate file
    if (!validateFile(file)) {
      return createErrorResponse("Invalid file format or size");
    }

    try {
      // Step 1: Extract text from file
      String extractedText = extractTextFromFile(file);

      UUID currentUserId = getCurrentUserId();
      if (questionBankId != null) {
        validateCanUseQuestionBank(questionBankId, currentUserId);
      }

      if (extractedText == null || extractedText.trim().isEmpty()) {
        return createErrorResponse("No text content found in file");
      }

      log.info("Extracted {} characters from file", extractedText.length());

      // Step 2: Analyze with AI
      TemplateImportResponse response = analyzeWithAI(extractedText, subjectHint, contextHint);
      response.setExtractedText(extractedText);

      // Step 3: Save as DRAFT if analysis was successful
      if (response.getAnalysisSuccessful() && response.getSuggestedTemplate() != null) {
        try {
          QuestionTemplate savedTemplate =
              saveDraftTemplate(response, currentUserId, questionBankId);
          log.info("Saved template as DRAFT with ID: {}", savedTemplate.getId());
        } catch (Exception e) {
          log.error("Failed to save template as DRAFT: {}", e.getMessage(), e);
          // Add warning but continue - template can still be reviewed and saved manually
          if (response.getWarnings() == null) {
            response.setWarnings(new ArrayList<>());
          }
          response
              .getWarnings()
              .add(
                  "Warning: Failed to auto-save template as DRAFT. You can save it manually after review.");
        }
      }

      return response;

    } catch (Exception e) {
      log.error("Failed to import template from file: {}", e.getMessage(), e);
      return createErrorResponse("Failed to process file: " + e.getMessage());
    }
  }

  @Override
  public String extractTextFromFile(MultipartFile file) {
    try {
      String contentType = file.getContentType();
      String filename = file.getOriginalFilename();

      if (contentType == null && filename != null) {
        contentType = guessContentType(filename);
      }

      if (contentType == null) {
        throw new IllegalArgumentException("Cannot determine file type");
      }

      log.info("Extracting text from file type: {}", contentType);

      // PDF
      return switch (contentType) {
        case "application/pdf" -> extractFromPDF(file.getInputStream());

          // Word (DOCX)
        case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
            extractFromDOCX(file.getInputStream());

          // Plain text
        case "text/plain" -> new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        default -> throw new IllegalArgumentException("Unsupported file type: " + contentType);
      };

    } catch (IOException e) {
      log.error("Failed to extract text from file: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to extract text: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      log.warn("File is null or empty");
      return false;
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      log.warn("File size {} exceeds maximum {}", file.getSize(), MAX_FILE_SIZE);
      return false;
    }

    String contentType = file.getContentType();
    String filename = file.getOriginalFilename();

    if (contentType == null && filename != null) {
      contentType = guessContentType(filename);
    }

    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      log.warn("Invalid content type: {}", contentType);
      return false;
    }

    return true;
  }

  // Helper methods

  private String extractFromPDF(InputStream inputStream) throws IOException {
    try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
      PDFTextStripper stripper = new PDFTextStripper();
      return stripper.getText(document);
    }
  }

  private String extractFromDOCX(InputStream inputStream) throws IOException {
    try (XWPFDocument document = new XWPFDocument(inputStream)) {
      StringBuilder text = new StringBuilder();
      for (XWPFParagraph paragraph : document.getParagraphs()) {
        text.append(paragraph.getText()).append("\n");
      }
      return text.toString();
    }
  }

  private String guessContentType(String filename) {
    if (filename == null) {
      return null;
    }

    String lower = filename.toLowerCase();
    if (lower.endsWith(".pdf")) {
      return "application/pdf";
    }
    if (lower.endsWith(".docx")) {
      return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
    if (lower.endsWith(".doc")) {
      return "application/msword";
    }
    if (lower.endsWith(".txt")) {
      return "text/plain";
    }

    return null;
  }

  private TemplateImportResponse analyzeWithAI(
      String text, String subjectHint, String contextHint) {
    try {
      // Build AI analysis prompt
      String prompt = buildAnalysisPrompt(text, subjectHint, contextHint);
      log.info("Built analysis prompt with {} characters", prompt.length());

      // Call Gemini
      log.info("Sending analysis request to Gemini AI...");
      long startTime = System.currentTimeMillis();

      String aiResponse = geminiService.sendMessage(prompt);

      long duration = System.currentTimeMillis() - startTime;
      log.info("Received AI response in {} ms ({} seconds)", duration, duration / 1000.0);

      // Parse AI response
      return parseAIAnalysis(aiResponse, text);

    } catch (Exception e) {
      log.error("AI analysis failed: {}", e.getMessage(), e);

      // Fallback to rule-based analysis
      log.info("Falling back to rule-based analysis");
      return performRuleBasedAnalysis(text);
    }
  }

  private String buildAnalysisPrompt(String text, String subjectHint, String contextHint) {
    StringBuilder prompt = new StringBuilder();

    // SYSTEM PROMPT - Deterministic Math Question Template Analyzer
    prompt.append("# ROLE\n");
    prompt.append(
        "You are a Mathematics Question Template Analyzer for an educational platform.\n");
    prompt.append(
        "Your purpose is to analyze extracted text from uploaded files and propose a reusable Question Template DRAFT.\n");
    prompt.append("This is for MATHEMATICS ONLY. Accuracy and correctness are critical.\n\n");

    prompt.append("# CRITICAL CONSTRAINTS\n");
    prompt.append("- NEVER hallucinate mathematical formulas or facts\n");
    prompt.append("- NEVER auto-publish or assume your analysis is final\n");
    prompt.append("- If unsure, report LOW confidence and include warnings\n");
    prompt.append("- Prefer mathematical correctness over fluent language\n");
    prompt.append("- Output ONLY valid JSON, no markdown, no explanations outside JSON\n\n");

    prompt.append("# ANALYSIS PROCESS (Internal reasoning - do NOT expose in output)\n");
    prompt.append("Step 1: Identify question structure (MCQ, True/False, Short Answer, Essay)\n");
    prompt.append("Step 2: Extract all mathematical expressions exactly as written\n");
    prompt.append("Step 3: Detect repeated patterns across multiple questions\n");
    prompt.append(
        "Step 4: Identify variables that could be parameterized (numbers, coefficients)\n");
    prompt.append(
        "Step 5: Infer the mathematical structure (e.g., ax + b = c, quadratic, geometry)\n");
    prompt.append("Step 6: Derive the answer formula with proper operator precedence\n");
    prompt.append("Step 7: Validate formula logic (e.g., division by zero, negative roots)\n");
    prompt.append("Step 8: Detect common student error patterns in distractors\n");
    prompt.append("Step 9: Assess confidence based on pattern clarity\n");
    prompt.append("Step 10: Generate warnings for ambiguities or risks\n\n");

    prompt.append("# MATHEMATICAL VALIDATION RULES\n");
    prompt.append(
        "- Preserve exact operator precedence: use parentheses (a + b) * c NOT a + b * c if ambiguous\n");
    prompt.append("- Handle negative parameters: if b can be negative, use ({{b}}) in formulas\n");
    prompt.append("- Validate domain constraints: sqrt(x) requires x >= 0\n");
    prompt.append("- Check for division by zero: denominators must have constraints\n");
    prompt.append("- Verify answer formula produces values matching examples\n\n");

    prompt.append("# INPUT CONTEXT\n");
    if (subjectHint != null && !subjectHint.isEmpty()) {
      prompt.append("Subject Area: ").append(subjectHint).append("\n");
    }
    if (contextHint != null && !contextHint.isEmpty()) {
      prompt.append("Additional Context: ").append(contextHint).append("\n");
    }
    prompt.append("\n# EXTRACTED TEXT TO ANALYZE\n");
    prompt.append(text);
    prompt.append("\n\n");

    prompt.append("# OUTPUT REQUIREMENTS\n");
    prompt.append("Return ONLY a valid JSON object with this EXACT structure:\n\n");
    prompt.append("{\n");
    prompt.append("  \"analysisSuccessful\": true,\n");
    prompt.append("  \"confidenceScore\": 0.85,\n");
    prompt.append("  \"extractedText\": \"<original text>\",\n");
    prompt.append("  \"analysis\": {\n");
    prompt.append("    \"detectedType\": \"MULTIPLE_CHOICE\",\n");
    prompt.append("    \"detectedPatterns\": [\"Linear equation pattern: ax + b = c\"],\n");
    prompt.append("    \"detectedFormulas\": [\"3x + 7 = 22\", \"5x - 10 = 15\"],\n");
    prompt.append(
        "    \"parameterizableElements\": [\"coefficient a\", \"constant b\", \"result c\"],\n");
    prompt.append("    \"mathematicalStructure\": \"Linear equation: ax + b = c, solve for x\",\n");
    prompt.append("    \"commonErrors\": [\"Forgot to divide\", \"Sign error with negative b\"]\n");
    prompt.append("  },\n");
    prompt.append("  \"suggestedTemplate\": {\n");
    prompt.append("    \"name\": \"Linear Equation Solver\",\n");
    prompt.append("    \"description\": \"Solve linear equations of form ax + b = c\",\n");
    prompt.append("    \"templateType\": \"MULTIPLE_CHOICE\",\n");
    prompt.append("    \"templateText\": \"Giải phương trình: {{a}}x + {{b}} = {{c}}\",\n");
    prompt.append("    \"parameters\": [\n");
    prompt.append(
        "      {\"name\": \"a\", \"type\": \"integer\", \"min\": 1, \"max\": 10, \"description\": \"Coefficient of x\"},\n");
    prompt.append(
        "      {\"name\": \"b\", \"type\": \"integer\", \"min\": -20, \"max\": 20, \"description\": \"Constant term (can be negative)\"},\n");
    prompt.append(
        "      {\"name\": \"c\", \"type\": \"integer\", \"min\": 1, \"max\": 50, \"description\": \"Result value\"}\n");
    prompt.append("    ],\n");
    prompt.append("    \"answerFormula\": \"(c - b) / a\",\n");
    prompt.append("    \"formulaExplanation\": \"Solve ax + b = c => x = (c - b) / a\",\n");
    prompt.append("    \"cognitiveLevel\": \"APPLY\",\n");
    prompt.append("    \"tags\": [\"linear-equation\", \"algebra\"],\n");
    prompt.append("    \"difficultyRules\": {\n");
    prompt.append("      \"easy\": \"a <= 3 AND b >= 0 AND c <= 20\",\n");
    prompt.append("      \"medium\": \"a <= 7 OR b < 0\",\n");
    prompt.append("      \"hard\": \"a > 7 OR ABS(b) > 15\"\n");
    prompt.append("    },\n");
    prompt.append("    \"constraints\": [\"a != 0\"]\n");
    prompt.append("  },\n");
    prompt.append(
        "  \"warnings\": [\"Parameter b can be negative - ensure display handles minus signs\", \"Division by zero if a=0\"]\n");
    prompt.append("}\n\n");

    prompt.append("# FIELD DEFINITIONS\n");
    prompt.append(
        "- analysisSuccessful: boolean, true if analysis completed, false if text is unprocessable\n");
    prompt.append(
        "- confidenceScore: 0.0 to 1.0, based on pattern clarity and mathematical validity\n");
    prompt.append("- detectedType: MULTIPLE_CHOICE | TRUE_FALSE | SHORT_ANSWER | ESSAY\n");
    prompt.append(
        "- answerFormula: Mathematical expression using {{parameter}} syntax. MUST be algebraically correct.\n");
    prompt.append(
        "- cognitiveLevel: REMEMBER | UNDERSTAND | APPLY | ANALYZE | EVALUATE | CREATE\n");
    prompt.append(
        "- warnings: Array of strings. Include ANY mathematical risks, ambiguities, or edge cases.\n\n");

    prompt.append("# CONFIDENCE SCORING GUIDE\n");
    prompt.append("- 0.9-1.0: Clear pattern, validated formula, multiple examples, no ambiguity\n");
    prompt.append("- 0.7-0.9: Clear pattern, formula seems correct, minor uncertainties\n");
    prompt.append("- 0.5-0.7: Pattern detected but formula uncertain or only one example\n");
    prompt.append("- 0.3-0.5: Weak pattern, significant ambiguity\n");
    prompt.append("- 0.0-0.3: Cannot reliably analyze, set analysisSuccessful=false\n\n");

    prompt.append("# WHAT NOT TO DO\n");
    prompt.append("- Do NOT include markdown code blocks (no ```json)\n");
    prompt.append("- Do NOT add explanatory text outside the JSON\n");
    prompt.append("- Do NOT invent formulas not present in the text\n");
    prompt.append("- Do NOT assume the template is ready for production use\n");
    prompt.append("- Do NOT omit warnings even if they seem minor\n\n");

    prompt.append("Begin analysis. Output ONLY the JSON object:\n");

    return prompt.toString();
  }

  private TemplateImportResponse parseAIAnalysis(String aiResponse, String originalText) {
    try {
      log.info(
          "Parsing AI response, length: {} characters",
          aiResponse != null ? aiResponse.length() : 0);

      // Extract JSON from response
      String jsonContent = extractJSON(aiResponse);
      log.info(
          "Extracted JSON content, length: {} characters",
          jsonContent != null ? jsonContent.length() : 0);

      if (jsonContent == null || jsonContent.trim().isEmpty()) {
        log.error("No JSON content extracted from AI response");
        return performRuleBasedAnalysis(originalText);
      }

      JsonNode root = objectMapper.readTree(jsonContent);
      log.info("Successfully parsed JSON root node");

      // Check if analysis was successful
      boolean analysisSuccessful = root.path("analysisSuccessful").asBoolean(false);
      if (!analysisSuccessful) {
        log.warn("AI reported analysis unsuccessful");
        return performRuleBasedAnalysis(originalText);
      }

      // Parse nested 'analysis' object
      JsonNode analysisNode = root.path("analysis");
      if (analysisNode.isMissingNode()) {
        log.error("Missing 'analysis' node in AI response");
        return performRuleBasedAnalysis(originalText);
      }

      TemplateImportResponse.QuestionStructureAnalysis analysis =
          TemplateImportResponse.QuestionStructureAnalysis.builder()
              .detectedType(parseQuestionType(analysisNode.path("detectedType").asText()))
              .detectedPatterns(parseStringArray(analysisNode.path("detectedPatterns")))
              .detectedFormulas(parseStringArray(analysisNode.path("detectedFormulas")))
              .mathematicalStructure(analysisNode.path("mathematicalStructure").asText())
              .detectedLanguage(analysisNode.path("detectedLanguage").asText())
              .placeholderSuggestions(new ArrayList<>()) // Will be filled from parameters
              .sampleQuestions(extractSampleQuestions(originalText))
              .build();

      // Parse nested 'suggestedTemplate' object
      JsonNode templateNode = root.path("suggestedTemplate");

      // Parse parameters and convert to placeholder suggestions
      List<TemplateImportResponse.PlaceholderSuggestion> placeholders = new ArrayList<>();
      if (templateNode.has("parameters") && templateNode.get("parameters").isArray()) {
        for (JsonNode param : templateNode.get("parameters")) {
          placeholders.add(
              TemplateImportResponse.PlaceholderSuggestion.builder()
                  .variableName(param.path("name").asText())
                  .type(param.path("type").asText())
                  .minValue(param.path("min").asInt())
                  .maxValue(param.path("max").asInt())
                  .description(param.path("description").asText())
                  .exampleValues(new ArrayList<>())
                  .build());
        }
      }
      analysis.setPlaceholderSuggestions(placeholders);

      // Parse template text (could be string or object)
      Map<String, String> templateTextMap;
      JsonNode templateTextNode = templateNode.path("templateText");
      if (templateTextNode.isTextual()) {
        templateTextMap = new HashMap<>();
        templateTextMap.put("en", templateTextNode.asText());
      } else if (templateTextNode.isObject()) {
        templateTextMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = templateTextNode.fields();
        while (fields.hasNext()) {
          Map.Entry<String, JsonNode> entry = fields.next();
          templateTextMap.put(entry.getKey(), entry.getValue().asText());
        }
      } else {
        templateTextMap = new HashMap<>();
        templateTextMap.put("en", "");
      }

      TemplateImportResponse.TemplateDraft draft =
          TemplateImportResponse.TemplateDraft.builder()
              .name(templateNode.path("name").asText())
              .description(templateNode.path("description").asText())
              .templateType(parseQuestionType(templateNode.path("templateType").asText()))
              .templateText(templateTextMap)
              .parameters(buildParametersFromNode(templateNode.path("parameters")))
              .answerFormula(templateNode.path("answerFormula").asText())
              .difficultyRules(parseDifficultyRules(templateNode.path("difficultyRules")))
              .cognitiveLevel(parseCognitiveLevel(templateNode.path("cognitiveLevel").asText()))
              .tags(parseStringArray(templateNode.path("tags")).toArray(new String[0]))
              .build();

      // Build response with initial warnings
      List<String> warnings = new ArrayList<>(parseStringArray(root.path("warnings")));

      TemplateImportResponse response =
          TemplateImportResponse.builder()
              .extractedText(root.path("extractedText").asText(originalText))
              .analysis(analysis)
              .suggestedTemplate(draft)
              .confidenceScore(root.path("confidenceScore").asDouble(0.5))
              .warnings(warnings)
              .analysisSuccessful(true)
              .build();

      // Post-process and validate the response
      return postProcessAIResponse(response, originalText);

    } catch (Exception e) {
      log.error("Failed to parse AI analysis: {}", e.getMessage(), e);
      log.error(
          "AI response that failed to parse (first 1000 chars): {}",
          aiResponse != null
              ? aiResponse.substring(0, Math.min(1000, aiResponse.length()))
              : "null");
      return performRuleBasedAnalysis(originalText);
    }
  }

  private TemplateImportResponse postProcessAIResponse(
      TemplateImportResponse response, String originalText) {
    try {
      List<String> additionalWarnings = new ArrayList<>(response.getWarnings());

      // 1. Fix language detection if missing
      if (response.getAnalysis() != null
          && (response.getAnalysis().getDetectedLanguage() == null
              || response.getAnalysis().getDetectedLanguage().isEmpty())) {
        String detectedLang = detectLanguage(originalText);
        response.getAnalysis().setDetectedLanguage(detectedLang);
        log.info("Auto-detected language: {}", detectedLang);
      }

      // 2. Fix templateText language key if incorrect
      if (response.getSuggestedTemplate() != null) {
        Map<String, String> templateText = response.getSuggestedTemplate().getTemplateText();
        if (templateText != null && !templateText.isEmpty() && response.getAnalysis() != null) {
          String correctLang = response.getAnalysis().getDetectedLanguage();
          if (templateText.containsKey("en") && "vi".equals(correctLang)) {
            String text = templateText.get("en");
            // Create a new mutable map instead of clearing immutable map
            Map<String, String> newTemplateText = new HashMap<>();
            newTemplateText.put("vi", text);
            response.getSuggestedTemplate().setTemplateText(newTemplateText);
            additionalWarnings.add("Fixed: Template text language key changed from 'en' to 'vi'");
            log.warn("Fixed incorrect language key: en -> vi");
          }
        }

        // 3. Add missing optionsGenerator for MULTIPLE_CHOICE
        if (response.getSuggestedTemplate().getTemplateType() == QuestionType.MULTIPLE_CHOICE
            && (response.getSuggestedTemplate().getOptionsGenerator() == null
                || response.getSuggestedTemplate().getOptionsGenerator().isEmpty())) {

          Map<String, Object> defaultGenerator = new HashMap<>();
          defaultGenerator.put("correctAnswer", "Use answerFormula");
          defaultGenerator.put("count", 4);
          defaultGenerator.put(
              "distractors",
              List.of(
                  "Common mistake: forgot to subtract b",
                  "Common mistake: forgot to divide by a",
                  "Common mistake: sign error"));
          response.getSuggestedTemplate().setOptionsGenerator(defaultGenerator);
          additionalWarnings.add("Added default optionsGenerator for MULTIPLE_CHOICE question");
          log.warn("Added missing optionsGenerator for MULTIPLE_CHOICE");
        }

        // 4. Validate and add missing constraints
        String formula = response.getSuggestedTemplate().getAnswerFormula();
        if (formula != null && formula.contains("/ a")) {
          // Check if constraints exist
          Map<String, String> templateText2 = response.getSuggestedTemplate().getTemplateText();
          if (templateText2 != null) {
            String templateStr = templateText2.values().stream().findFirst().orElse("");
            if (templateStr.contains("{{a}}")
                && additionalWarnings.stream()
                    .noneMatch(w -> w.contains("a=0") || w.contains("a != 0"))) {
              additionalWarnings.add(
                  "CRITICAL: Add constraint 'a != 0' to prevent division by zero");
            }
          }
        }

        // 5. Validate difficulty rules are mutually exclusive
        Map<String, String> diffRules = response.getSuggestedTemplate().getDifficultyRules();
        if (diffRules != null && diffRules.size() >= 2) {
          // This is a simplified check - in production you'd parse the conditions
          if (diffRules.containsKey("easy")
              && diffRules.containsKey("medium")
              && diffRules.containsKey("hard")) {
            String easy = diffRules.get("easy");
            String medium = diffRules.get("medium");
            // Check for overlapping conditions (simplified)
            if ((easy.contains("<=") && medium.contains("<="))
                || (easy.contains("AND") && !medium.contains("AND"))) {
              additionalWarnings.add(
                  "WARNING: Difficulty rules may overlap - verify mutual exclusivity");
            }
          }
        }
      }

      // Update warnings
      response.setWarnings(additionalWarnings);

      return response;
    } catch (Exception e) {
      log.error("Error in postProcessAIResponse: {}", e.getMessage(), e);
      // Return response as-is if post-processing fails
      return response;
    }
  }

  private String detectLanguage(String text) {
    // Simple language detection based on character patterns
    if (text == null || text.isEmpty()) {
      return "en";
    }

    // Check for Vietnamese characters
    if (text.matches(
        ".*[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴĐ].*")) {
      return "vi";
    }

    // Check for common Vietnamese words
    String lowerText = text.toLowerCase();
    if (lowerText.contains("giải")
        || lowerText.contains("phương trình")
        || lowerText.contains("câu")
        || lowerText.contains("đề")) {
      return "vi";
    }

    return "en";
  }

  private TemplateImportResponse performRuleBasedAnalysis(String text) {
    log.info("Performing rule-based analysis");

    // Detect numbers in text
    List<String> numbers = extractNumbers(text);

    // Detect question type
    QuestionType detectedType = detectQuestionType(text);

    // Create basic placeholder suggestions
    List<TemplateImportResponse.PlaceholderSuggestion> placeholders = new ArrayList<>();
    if (!numbers.isEmpty()) {
      placeholders.add(
          TemplateImportResponse.PlaceholderSuggestion.builder()
              .variableName("x")
              .type("integer")
              .minValue(1)
              .maxValue(100)
              .exampleValues(numbers.subList(0, Math.min(3, numbers.size())))
              .description("First variable detected in text")
              .build());
    }

    // Build basic analysis
    TemplateImportResponse.QuestionStructureAnalysis analysis =
        TemplateImportResponse.QuestionStructureAnalysis.builder()
            .detectedType(detectedType)
            .detectedPatterns(List.of("Numbers found: " + numbers.size()))
            .placeholderSuggestions(placeholders)
            .detectedFormulas(new ArrayList<>())
            .sampleQuestions(List.of(text.substring(0, Math.min(200, text.length()))))
            .build();

    // Build basic template draft
    TemplateImportResponse.TemplateDraft draft =
        TemplateImportResponse.TemplateDraft.builder()
            .name("Imported Template")
            .description("Template imported from file - please review and edit")
            .templateType(detectedType)
            .templateText(Map.of("en", text))
            .parameters(new HashMap<>())
            .answerFormula("")
            .difficultyRules(
                Map.of(
                    "easy", "true",
                    "medium", "true",
                    "hard", "true"))
            .cognitiveLevel(CognitiveLevel.APPLY)
            .tags(new String[] {"imported"})
            .build();

    return TemplateImportResponse.builder()
        .extractedText(text)
        .analysis(analysis)
        .suggestedTemplate(draft)
        .confidenceScore(0.3)
        .warnings(
            List.of(
                "AI analysis failed - using rule-based fallback",
                "Please review and edit the template carefully"))
        .analysisSuccessful(false)
        .build();
  }

  private String extractJSON(String content) {
    if (content == null || content.trim().isEmpty()) {
      log.error("extractJSON received null or empty content");
      return null;
    }

    log.debug(
        "Extracting JSON from content (first 200 chars): {}",
        content.substring(0, Math.min(200, content.length())) + "...");

    // Find first { or [ and last } or ]
    int startIdx = content.indexOf('{');
    int startArrayIdx = content.indexOf('[');

    // Pick the earliest valid start bracket
    int finalStart = -1;
    if (startIdx >= 0 && startArrayIdx >= 0) finalStart = Math.min(startIdx, startArrayIdx);
    else if (startIdx >= 0) finalStart = startIdx;
    else if (startArrayIdx >= 0) finalStart = startArrayIdx;

    int endIdx = content.lastIndexOf('}');
    int endArrayIdx = content.lastIndexOf(']');

    // Pick the latest valid end bracket
    int finalEnd = -1;
    if (endIdx >= 0 && endArrayIdx >= 0) finalEnd = Math.max(endIdx, endArrayIdx);
    else if (endIdx >= 0) finalEnd = endIdx;
    else if (endArrayIdx >= 0) finalEnd = endArrayIdx;

    if (finalStart >= 0 && finalEnd > finalStart) {
      String extracted = content.substring(finalStart, finalEnd + 1);
      log.info("Extracted JSON by bracket detection, length: {}", extracted.length());
      return extracted;
    }

    log.warn("Could not extract JSON structure, returning trimmed original content as fallback.");
    return content.trim();
  }

  private QuestionType parseQuestionType(String type) {
    try {
      return QuestionType.valueOf(type.toUpperCase());
    } catch (Exception e) {
      return QuestionType.MULTIPLE_CHOICE;
    }
  }

  private CognitiveLevel parseCognitiveLevel(String level) {
    try {
      return CognitiveLevel.valueOf(level.toUpperCase());
    } catch (Exception e) {
      return CognitiveLevel.APPLY;
    }
  }

  private List<String> parseStringArray(JsonNode node) {
    List<String> result = new ArrayList<>();
    if (node.isArray()) {
      node.forEach(item -> result.add(item.asText()));
    }
    return result;
  }

  private List<TemplateImportResponse.PlaceholderSuggestion> parsePlaceholders(JsonNode node) {
    List<TemplateImportResponse.PlaceholderSuggestion> result = new ArrayList<>();

    if (node.isArray()) {
      node.forEach(
          item -> {
            TemplateImportResponse.PlaceholderSuggestion placeholder =
                TemplateImportResponse.PlaceholderSuggestion.builder()
                    .variableName(item.path("name").asText())
                    .type(item.path("type").asText())
                    .minValue(item.path("min").asInt())
                    .maxValue(item.path("max").asInt())
                    .exampleValues(parseStringArray(item.path("examples")))
                    .description(item.path("description").asText())
                    .build();
            result.add(placeholder);
          });
    }

    return result;
  }

  private Map<String, Object> buildParameters(JsonNode placeholdersNode) {
    Map<String, Object> params = new HashMap<>();

    if (placeholdersNode.isArray()) {
      placeholdersNode.forEach(
          item -> {
            String name = item.path("name").asText();
            Map<String, Object> paramDef = new HashMap<>();
            paramDef.put("type", item.path("type").asText());
            paramDef.put("min", item.path("min").asInt());
            paramDef.put("max", item.path("max").asInt());
            params.put(name, paramDef);
          });
    }

    return params;
  }

  private Map<String, Object> buildParametersFromNode(JsonNode parametersNode) {
    return buildParameters(parametersNode);
  }

  private Map<String, String> parseDifficultyRules(JsonNode node) {
    Map<String, String> rules = new HashMap<>();

    if (node.isObject()) {
      Iterator<String> fieldNames = node.fieldNames();
      while (fieldNames.hasNext()) {
        String key = fieldNames.next();
        rules.put(key, node.get(key).asText());
      }
    }

    if (rules.isEmpty()) {
      rules.put("easy", "true");
      rules.put("medium", "true");
      rules.put("hard", "true");
    }

    return rules;
  }

  private List<String> extractSampleQuestions(String text) {
    List<String> samples = new ArrayList<>();

    // Split by common question markers
    String[] lines = text.split("\\n");
    for (String line : lines) {
      line = line.trim();
      if (line.matches(".*\\?$") || line.matches("^\\d+[.)].+")) {
        samples.add(line);
        if (samples.size() >= 5) break;
      }
    }

    return samples;
  }

  private List<String> extractNumbers(String text) {
    List<String> numbers = new ArrayList<>();
    Pattern pattern = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b");
    Matcher matcher = pattern.matcher(text);

    while (matcher.find()) {
      numbers.add(matcher.group());
      if (numbers.size() >= 10) break;
    }

    return numbers;
  }

  private QuestionType detectQuestionType(String text) {
    String lower = text.toLowerCase();

    if (lower.contains("true or false") || lower.contains("đúng hay sai")) {
      return QuestionType.TRUE_FALSE;
    }

    if (lower.matches(".*\\b[a-d][.)].+")) {
      return QuestionType.MULTIPLE_CHOICE;
    }

    return QuestionType.SHORT_ANSWER;
  }

  private TemplateImportResponse createErrorResponse(String errorMessage) {
    return TemplateImportResponse.builder()
        .extractedText("")
        .analysis(null)
        .suggestedTemplate(null)
        .confidenceScore(0.0)
        .warnings(List.of(errorMessage))
        .analysisSuccessful(false)
        .build();
  }

  /**
   * Save the analyzed template as DRAFT status
   *
   * @param response The analyzed template import response
   * @return Saved QuestionTemplate entity
   */
  private QuestionTemplate saveDraftTemplate(
      TemplateImportResponse response, UUID currentUserId, UUID questionBankId) {
    TemplateImportResponse.TemplateDraft draft = response.getSuggestedTemplate();

    // Convert templateText Map<String, String> to Map<String, Object> for JSONB
    Map<String, Object> templateTextObj = new HashMap<>();
    if (draft.getTemplateText() != null) {
      templateTextObj.putAll(draft.getTemplateText());
    }

    // Build QuestionTemplate entity
    QuestionTemplate template =
        QuestionTemplate.builder()
            .questionBankId(questionBankId)
            .name(draft.getName() != null ? draft.getName() : "Imported Template")
            .description(
                draft.getDescription() != null
                    ? draft.getDescription()
                    : "Template imported from file - please review and edit")
            .templateType(
                draft.getTemplateType() != null
                    ? draft.getTemplateType()
                    : QuestionType.SHORT_ANSWER)
            .templateText(templateTextObj)
            .parameters(draft.getParameters() != null ? draft.getParameters() : new HashMap<>())
            .answerFormula(draft.getAnswerFormula() != null ? draft.getAnswerFormula() : "")
            .optionsGenerator(draft.getOptionsGenerator())
            .difficultyRules(convertDifficultyRules(draft.getDifficultyRules()))
            .constraints(new String[0])
            .cognitiveLevel(
                draft.getCognitiveLevel() != null
                    ? draft.getCognitiveLevel()
                    : CognitiveLevel.APPLY)
            .tags(draft.getTags() != null ? draft.getTags() : new String[] {"imported"})
            .status(TemplateStatus.DRAFT)
            .isPublic(false)
            .usageCount(0)
            .build();
    template.setCreatedBy(currentUserId);

    // Save to database
    QuestionTemplate savedTemplate = questionTemplateRepository.save(template);
    log.info(
        "Template saved successfully with ID: {} and status: {}",
        savedTemplate.getId(),
        savedTemplate.getStatus());

    return savedTemplate;
  }

  private void validateCanUseQuestionBank(UUID bankId, UUID currentUserId) {
    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    if (!bank.getTeacherId().equals(currentUserId)
        && !Boolean.TRUE.equals(bank.getIsPublic())
        && !hasRoleAdmin()) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }
  }

  private boolean hasRoleAdmin() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Object scopeClaim = jwtAuth.getToken().getClaims().get("scope");
      if (scopeClaim instanceof String scopes) {
        return Arrays.asList(scopes.split(" ")).contains("ROLE_ADMIN");
      }
    }
    return false;
  }

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return UUID.fromString(jwtAuth.getToken().getSubject());
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  /** Convert difficulty rules Map<String, String> to Map<String, Object> for JSONB */
  private Map<String, Object> convertDifficultyRules(Map<String, String> rules) {
    if (rules == null) {
      return new HashMap<>();
    }
    Map<String, Object> result = new HashMap<>(rules);
    return result;
  }
}
