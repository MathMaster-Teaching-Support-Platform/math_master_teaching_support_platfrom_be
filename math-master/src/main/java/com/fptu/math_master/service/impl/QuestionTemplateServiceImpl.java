package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.QuestionTemplateService;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionTemplateServiceImpl implements QuestionTemplateService {

  QuestionTemplateRepository questionTemplateRepository;
  UserRepository userRepository;
  AIEnhancementService aiEnhancementService;
  ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

  @Override
  @Transactional
  public QuestionTemplateResponse createQuestionTemplate(QuestionTemplateRequest request) {
    log.info("Creating question template: {}", request.getName());

    UUID currentUserId = getCurrentUserId();

    // Validate template syntax
    List<String> validationErrors = validateTemplateSyntax(request);
    if (!validationErrors.isEmpty()) {
      log.error("Template validation failed: {}", validationErrors);
      throw new AppException(ErrorCode.INVALID_TEMPLATE_SYNTAX);
    }

    QuestionTemplate template =
        QuestionTemplate.builder()
            .createdBy(currentUserId)
            .name(request.getName())
            .description(request.getDescription())
            .templateType(request.getTemplateType())
            .templateText(request.getTemplateText())
            .parameters(request.getParameters())
            .answerFormula(request.getAnswerFormula())
            .optionsGenerator(request.getOptionsGenerator())
            .difficultyRules(request.getDifficultyRules())
            .constraints(request.getConstraints())
            .cognitiveLevel(request.getCognitiveLevel())
            .tags(request.getTags())
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
            .usageCount(0)
            .build();

    template = questionTemplateRepository.save(template);

    log.info("Question template created successfully with id: {}", template.getId());
    return mapToResponse(template);
  }

  @Override
  @Transactional
  public QuestionTemplateResponse updateQuestionTemplate(UUID id, QuestionTemplateRequest request) {
    log.info("Updating question template with id: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    // Validate template syntax
    List<String> validationErrors = validateTemplateSyntax(request);
    if (!validationErrors.isEmpty()) {
      log.error("Template validation failed: {}", validationErrors);
      throw new AppException(ErrorCode.INVALID_TEMPLATE_SYNTAX);
    }

    template.setName(request.getName());
    template.setDescription(request.getDescription());
    template.setTemplateType(request.getTemplateType());
    template.setTemplateText(request.getTemplateText());
    template.setParameters(request.getParameters());
    template.setAnswerFormula(request.getAnswerFormula());
    template.setOptionsGenerator(request.getOptionsGenerator());
    template.setDifficultyRules(request.getDifficultyRules());
    template.setConstraints(request.getConstraints());
    template.setCognitiveLevel(request.getCognitiveLevel());
    template.setTags(request.getTags());
    if (request.getIsPublic() != null) {
      template.setIsPublic(request.getIsPublic());
    }

    template = questionTemplateRepository.save(template);

    log.info("Question template updated successfully: {}", id);
    return mapToResponse(template);
  }

  @Override
  @Transactional
  public void deleteQuestionTemplate(UUID id) {
    log.info("Deleting question template with id: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    // Soft delete
    template.setDeletedAt(Instant.now());
    questionTemplateRepository.save(template);

    log.info("Question template soft deleted successfully: {}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public QuestionTemplateResponse getQuestionTemplateById(UUID id) {
    log.info("Getting question template with id: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();

    // Check access: owner, admin, or public template
    if (!template.getIsPublic() && !template.getCreatedBy().equals(currentUserId) && !isAdmin()) {
      throw new AccessDeniedException("You don't have permission to access this template");
    }

    return mapToResponse(template);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionTemplateResponse> getMyQuestionTemplates(Pageable pageable) {
    UUID currentUserId = getCurrentUserId();
    log.info("Getting question templates for user: {}", currentUserId);

    Page<QuestionTemplate> templates = questionTemplateRepository.findAll(pageable);
    return templates.map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionTemplateResponse> searchQuestionTemplates(
      QuestionType templateType,
      CognitiveLevel cognitiveLevel,
      Boolean isPublic,
      String searchTerm,
      String[] tags,
      Pageable pageable) {
    log.info("Searching question templates with filters");

    UUID currentUserId = getCurrentUserId();
    Page<QuestionTemplate> templates = questionTemplateRepository.findAll(pageable);

    return templates.map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public TemplateTestResponse testTemplate(UUID id, Integer sampleCount) {
    log.info("Testing template with id: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    return generateTemplateTest(template, sampleCount != null ? sampleCount : 5);
  }

  @Override
  @Transactional(readOnly = true)
  public TemplateTestResponse validateAndTestTemplate(
      QuestionTemplateRequest request, Integer sampleCount) {
    log.info("Validating and testing template: {}", request.getName());

    List<String> validationErrors = validateTemplateSyntax(request);

    QuestionTemplate tempTemplate =
        QuestionTemplate.builder()
            .name(request.getName())
            .templateType(request.getTemplateType())
            .templateText(request.getTemplateText())
            .parameters(request.getParameters())
            .answerFormula(request.getAnswerFormula())
            .optionsGenerator(request.getOptionsGenerator())
            .difficultyRules(request.getDifficultyRules())
            .constraints(request.getConstraints())
            .cognitiveLevel(request.getCognitiveLevel())
            .tags(request.getTags())
            .build();

    if (!validationErrors.isEmpty()) {
      return TemplateTestResponse.builder()
          .templateName(request.getName())
          .samples(new ArrayList<>())
          .isValid(false)
          .validationErrors(validationErrors)
          .build();
    }

    return generateTemplateTest(tempTemplate, sampleCount != null ? sampleCount : 5);
  }

  @Override
  @Transactional
  public QuestionTemplateResponse togglePublicStatus(UUID id) {
    log.info("Toggling public status for template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    template.setIsPublic(!template.getIsPublic());
    template = questionTemplateRepository.save(template);

    log.info("Template public status toggled to: {}", template.getIsPublic());
    return mapToResponse(template);
  }

  // Helper methods

  private List<String> validateTemplateSyntax(QuestionTemplateRequest request) {
    List<String> errors = new ArrayList<>();

    // Validate template text has placeholders matching parameters
    String templateTextStr = request.getTemplateText().toString();
    Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    Matcher matcher = pattern.matcher(templateTextStr);

    Set<String> placeholders = new HashSet<>();
    while (matcher.find()) {
      placeholders.add(matcher.group(1));
    }

    Map<String, Object> parameters = request.getParameters();
    for (String placeholder : placeholders) {
      if (!parameters.containsKey(placeholder)) {
        errors.add("Placeholder {{" + placeholder + "}} not defined in parameters");
      }
    }

    // Validate answer formula
    try {
      validateFormula(request.getAnswerFormula(), parameters.keySet());
    } catch (Exception e) {
      errors.add("Invalid answer formula: " + e.getMessage());
    }

    // Validate difficulty rules
    if (request.getDifficultyRules() == null || request.getDifficultyRules().isEmpty()) {
      errors.add("Difficulty rules are required");
    }

    return errors;
  }

  private void validateFormula(String formula, Set<String> paramNames) throws ScriptException {
    ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
    if (engine == null) {
      throw new RuntimeException("JavaScript engine not available");
    }

    // Set dummy values for parameters
    for (String param : paramNames) {
      engine.put(param, 1);
    }

    // Try to evaluate the formula
    engine.eval(formula);
  }

  private TemplateTestResponse generateTemplateTest(QuestionTemplate template, int sampleCount) {
    List<GeneratedQuestionSample> samples = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    for (int i = 0; i < sampleCount; i++) {
      try {
        GeneratedQuestionSample sample = generateQuestionSample(template);
        samples.add(sample);
      } catch (Exception e) {
        log.error("Error generating sample {}: {}", i, e.getMessage());
        errors.add("Sample " + (i + 1) + " generation failed: " + e.getMessage());
      }
    }

    return TemplateTestResponse.builder()
        .templateId(template.getId())
        .templateName(template.getName())
        .samples(samples)
        .isValid(errors.isEmpty())
        .validationErrors(errors)
        .build();
  }

  @SuppressWarnings("unchecked")
  private GeneratedQuestionSample generateQuestionSample(QuestionTemplate template)
      throws ScriptException {
    Map<String, Object> usedParameters = new HashMap<>();
    ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");

    // Generate random values for each parameter
    Map<String, Object> parameters = template.getParameters();
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      String paramName = entry.getKey();
      Map<String, Object> paramDef = (Map<String, Object>) entry.getValue();

      Object value = generateParameterValue(paramDef);
      usedParameters.put(paramName, value);
      engine.put(paramName, value);
    }

    // Calculate answer
    Object answerObj = engine.eval(template.getAnswerFormula());
    double answer = ((Number) answerObj).doubleValue();

    // Check constraints
    if (template.getConstraints() != null) {
      engine.put("answer", answer);
      for (String constraint : template.getConstraints()) {
        Object result = engine.eval(constraint);
        if (!(result instanceof Boolean) || !((Boolean) result)) {
          throw new RuntimeException("Constraint failed: " + constraint);
        }
      }
    }

    // Generate question text
    String questionText = generateQuestionText(template.getTemplateText(), usedParameters);

    // Generate options
    Map<String, String> options = generateOptions(template, answer, engine);

    // Determine difficulty
    QuestionDifficulty difficulty =
        calculateDifficulty(template.getDifficultyRules(), usedParameters, engine);

    return GeneratedQuestionSample.builder()
        .questionText(questionText)
        .options(options)
        .correctAnswer(formatAnswer(answer))
        .explanation("Based on the formula: " + template.getAnswerFormula())
        .calculatedDifficulty(difficulty)
        .usedParameters(usedParameters)
        .answerCalculation(template.getAnswerFormula() + " = " + answer)
        .build();
  }

  @SuppressWarnings("unchecked")
  private Object generateParameterValue(Map<String, Object> paramDef) {
    String type = (String) paramDef.getOrDefault("type", "integer");
    Random random = new Random();

    switch (type.toLowerCase()) {
      case "integer":
        int min = ((Number) paramDef.getOrDefault("min", 1)).intValue();
        int max = ((Number) paramDef.getOrDefault("max", 10)).intValue();
        List<Integer> exclude =
            paramDef.containsKey("exclude")
                ? ((List<?>) paramDef.get("exclude"))
                    .stream().map(o -> ((Number) o).intValue()).collect(Collectors.toList())
                : new ArrayList<>();

        int value;
        do {
          value = random.nextInt(max - min + 1) + min;
        } while (exclude.contains(value));
        return value;

      case "decimal":
        double minD = ((Number) paramDef.getOrDefault("min", 1.0)).doubleValue();
        double maxD = ((Number) paramDef.getOrDefault("max", 10.0)).doubleValue();
        return minD + (maxD - minD) * random.nextDouble();

      default:
        return 1;
    }
  }

  private String generateQuestionText(
      Map<String, Object> templateText, Map<String, Object> parameters) {
    String text = templateText.getOrDefault("vi", templateText.get("en")).toString();

    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      String placeholder = "{{" + entry.getKey() + "}}";
      text = text.replace(placeholder, entry.getValue().toString());
    }

    return text;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> generateOptions(
      QuestionTemplate template, double answer, ScriptEngine engine) throws ScriptException {
    Map<String, String> options = new LinkedHashMap<>();

    if (template.getTemplateType() == QuestionType.TRUE_FALSE) {
      options.put("A", "True");
      options.put("B", "False");
      return options;
    }

    if (template.getOptionsGenerator() == null) {
      options.put("A", formatAnswer(answer));
      return options;
    }

    Map<String, Object> generator = template.getOptionsGenerator();
    List<String> distractors = (List<String>) generator.get("distractors");

    List<String> allOptions = new ArrayList<>();
    allOptions.add(formatAnswer(answer));

    if (distractors != null) {
      engine.put("answer", answer);
      for (String distractor : distractors) {
        try {
          Object result = engine.eval(distractor);
          double distractorValue = ((Number) result).doubleValue();
          if (distractorValue != answer) {
            allOptions.add(formatAnswer(distractorValue));
          }
        } catch (Exception e) {
          log.warn("Failed to generate distractor: {}", distractor);
        }
      }
    }

    // Shuffle options
    Collections.shuffle(allOptions);

    char optionLabel = 'A';
    for (String option : allOptions) {
      options.put(String.valueOf(optionLabel++), option);
      if (optionLabel > 'D') break;
    }

    return options;
  }

  @SuppressWarnings("unchecked")
  private QuestionDifficulty calculateDifficulty(
      Map<String, Object> rules, Map<String, Object> parameters, ScriptEngine engine)
      throws ScriptException {
    // Set parameters in engine
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      engine.put(entry.getKey(), entry.getValue());
    }

    // Check difficulty rules
    String easyRule = (String) rules.get("easy");
    String mediumRule = (String) rules.get("medium");
    String hardRule = (String) rules.get("hard");

    if (easyRule != null && evaluateRule(engine, easyRule)) {
      return QuestionDifficulty.EASY;
    }
    if (mediumRule != null && evaluateRule(engine, mediumRule)) {
      return QuestionDifficulty.MEDIUM;
    }
    if (hardRule != null && evaluateRule(engine, hardRule)) {
      return QuestionDifficulty.HARD;
    }

    return QuestionDifficulty.MEDIUM;
  }

  private boolean evaluateRule(ScriptEngine engine, String rule) {
    try {
      // Convert SQL-like operators to JavaScript
      String jsRule =
          rule.replaceAll("\\bAND\\b", "&&")
              .replaceAll("\\bOR\\b", "||")
              .replaceAll("\\babs\\(", "Math.abs(");

      Object result = engine.eval(jsRule);
      return result instanceof Boolean && (Boolean) result;
    } catch (Exception e) {
      log.warn("Failed to evaluate rule: {}", rule, e);
      return false;
    }
  }

  private String formatAnswer(double value) {
    if (value == (long) value) {
      return String.format("%d", (long) value);
    } else {
      return String.format("%.2f", value);
    }
  }

  private QuestionTemplateResponse mapToResponse(QuestionTemplate template) {
    QuestionTemplateResponse response =
        QuestionTemplateResponse.builder()
            .id(template.getId())
            .createdBy(template.getCreatedBy())
            .name(template.getName())
            .description(template.getDescription())
            .templateType(template.getTemplateType())
            .templateText(template.getTemplateText())
            .parameters(template.getParameters())
            .answerFormula(template.getAnswerFormula())
            .optionsGenerator(template.getOptionsGenerator())
            .difficultyRules(template.getDifficultyRules())
            .constraints(template.getConstraints())
            .cognitiveLevel(template.getCognitiveLevel())
            .tags(template.getTags())
            .isPublic(template.getIsPublic())
            .usageCount(template.getUsageCount())
            .avgSuccessRate(template.getAvgSuccessRate())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();

    if (template.getCreator() != null) {
      response.setCreatorName(template.getCreator().getFullName());
    }

    return response;
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    return UUID.fromString(authentication.getName());
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !isAdmin()) {
      throw new AccessDeniedException("You don't have permission to perform this action");
    }
  }

  private boolean isAdmin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }

  // AI Enhancement Methods

  @Override
  @Transactional(readOnly = true)
  public TemplateTestResponse testTemplateWithAI(UUID id, Integer sampleCount, Boolean useAI) {
    log.info("Testing template with AI={}: {}", useAI, id);

    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    if (Boolean.TRUE.equals(useAI)) {
      return generateTemplateTestWithAI(template, sampleCount != null ? sampleCount : 3);
    } else {
      return generateTemplateTest(template, sampleCount != null ? sampleCount : 5);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public AIEnhancedQuestionResponse generateAIEnhancedQuestion(UUID id) {
    log.info("Generating AI-enhanced question from template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    try {
      // Generate base question
      GeneratedQuestionSample baseSample = generateQuestionSample(template);

      // Build AI enhancement request
      AIEnhancementRequest aiRequest =
          AIEnhancementRequest.builder()
              .rawQuestionText(baseSample.getQuestionText())
              .questionType(template.getTemplateType())
              .correctAnswer(baseSample.getCorrectAnswer())
              .rawOptions(baseSample.getOptions())
              .parameters(baseSample.getUsedParameters())
              .answerFormula(template.getAnswerFormula())
              .difficulty(baseSample.getCalculatedDifficulty())
              .subject("Mathematics")
              .context(template.getDescription())
              .build();

      // Enhance with AI
      return aiEnhancementService.enhanceQuestion(aiRequest);

    } catch (Exception e) {
      log.error("Failed to generate AI-enhanced question: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  private TemplateTestResponse generateTemplateTestWithAI(
      QuestionTemplate template, int sampleCount) {
    List<GeneratedQuestionSample> samples = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    for (int i = 0; i < sampleCount; i++) {
      try {
        // Generate base sample
        GeneratedQuestionSample baseSample = generateQuestionSample(template);

        // Build AI enhancement request
        AIEnhancementRequest aiRequest =
            AIEnhancementRequest.builder()
                .rawQuestionText(baseSample.getQuestionText())
                .questionType(template.getTemplateType())
                .correctAnswer(baseSample.getCorrectAnswer())
                .rawOptions(baseSample.getOptions())
                .parameters(baseSample.getUsedParameters())
                .answerFormula(template.getAnswerFormula())
                .difficulty(baseSample.getCalculatedDifficulty())
                .subject("Mathematics")
                .context(template.getDescription())
                .build();

        // Enhance with AI
        AIEnhancedQuestionResponse aiResponse = aiEnhancementService.enhanceQuestion(aiRequest);

        // Convert to GeneratedQuestionSample for display
        GeneratedQuestionSample enhancedSample =
            GeneratedQuestionSample.builder()
                .questionText(
                    aiResponse.isEnhanced()
                        ? aiResponse.getEnhancedQuestionText()
                        : baseSample.getQuestionText())
                .options(
                    aiResponse.isEnhanced()
                        ? aiResponse.getEnhancedOptions()
                        : baseSample.getOptions())
                .correctAnswer(
                    aiResponse.isEnhanced()
                        ? aiResponse.getCorrectAnswerKey()
                        : baseSample.getCorrectAnswer())
                .explanation(
                    aiResponse.isEnhanced()
                        ? aiResponse.getExplanation()
                        : baseSample.getExplanation())
                .calculatedDifficulty(baseSample.getCalculatedDifficulty())
                .usedParameters(baseSample.getUsedParameters())
                .answerCalculation(baseSample.getAnswerCalculation())
                .build();

        samples.add(enhancedSample);

        if (!aiResponse.isEnhanced()) {
          errors.add(
              "Sample "
                  + (i + 1)
                  + " AI enhancement failed (fallback used): "
                  + String.join(", ", aiResponse.getValidationErrors()));
        }

      } catch (Exception e) {
        log.error("Error generating AI-enhanced sample {}: {}", i, e.getMessage());
        errors.add("Sample " + (i + 1) + " generation failed: " + e.getMessage());
      }
    }

    return TemplateTestResponse.builder()
        .templateId(template.getId())
        .templateName(template.getName())
        .samples(samples)
        .isValid(samples.size() > 0)
        .validationErrors(errors)
        .build();
  }
}
