package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
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
            .findByIdWithCreator(id)
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
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();

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

    Page<QuestionTemplate> templates = questionTemplateRepository.findAllWithCreator(pageable);
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

    Page<QuestionTemplate> templates = questionTemplateRepository.findAllWithCreator(pageable);

    return templates.map(this::mapToResponse);
  }

  // =====================================================================
  // Test / Validate — always via LLM
  // =====================================================================

  @Override
  public TemplateTestResponse testTemplate(UUID id, Integer sampleCount, Boolean useAI) {
    log.info("Testing template with id: {} (LLM-based)", id);
    QuestionTemplate template = fetchTemplateForTesting(id);
    return generateWithLLM(template, sampleCount != null ? sampleCount : 3);
  }


  @Override
  @Transactional
  public QuestionTemplateResponse togglePublicStatus(UUID id) {
    log.info("Toggling public status for template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    template.setIsPublic(!template.getIsPublic());
    template = questionTemplateRepository.save(template);

    log.info("Template public status toggled to: {}", template.getIsPublic());
    return mapToResponse(template);
  }

  @Override
  public AIEnhancedQuestionResponse generateAIEnhancedQuestion(UUID id) {
    log.info("Generating AI-enhanced question from template: {}", id);

    QuestionTemplate template = fetchTemplateForTesting(id);

    try {
      // Step 1: generate a base question (params + correct answer) via LLM
      GeneratedQuestionSample sample = aiEnhancementService.generateQuestion(template, 0);

      // Step 2: build a full AIEnhancementRequest so enhanceQuestion can enrich it
      AIEnhancementRequest enhancementRequest =
          AIEnhancementRequest.builder()
              .rawQuestionText(sample.getQuestionText())
              .questionType(template.getTemplateType())
              .correctAnswer(
                  sample.getOptions() != null && sample.getCorrectAnswer() != null
                      ? sample.getOptions().get(sample.getCorrectAnswer())
                      : null)
              .rawOptions(sample.getOptions())
              .parameters(sample.getUsedParameters())
              .answerFormula(template.getAnswerFormula())
              .difficulty(sample.getCalculatedDifficulty())
              .context(template.getDescription())
              .build();

      // Step 3: call enhanceQuestion which calls Gemini with the richer prompt
      AIEnhancedQuestionResponse enhanced = aiEnhancementService.enhanceQuestion(enhancementRequest);

      // Always preserve original question data
      enhanced.setOriginalQuestionText(sample.getQuestionText());
      enhanced.setOriginalOptions(sample.getOptions());

      return enhanced;

    } catch (Exception e) {
      log.error("Failed to generate AI-enhanced question: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  // =====================================================================
  // Private helpers
  // =====================================================================

  private TemplateTestResponse generateWithLLM(QuestionTemplate template, int sampleCount) {
    List<GeneratedQuestionSample> samples = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    for (int i = 0; i < sampleCount; i++) {
      try {
        GeneratedQuestionSample sample = aiEnhancementService.generateQuestion(template, i);
        if (sample.getQuestionText().startsWith("[LLM generation failed]")) {
          errors.add("Sample " + (i + 1) + ": " + sample.getQuestionText());
        } else {
          samples.add(sample);
        }
      } catch (Exception e) {
        log.error("Error generating LLM sample {}: {}", i + 1, e.getMessage());
        errors.add("Sample " + (i + 1) + " generation failed: " + e.getMessage());
      }
    }

    return TemplateTestResponse.builder()
        .templateId(template.getId())
        .templateName(template.getName())
        .samples(samples)
        .isValid(!samples.isEmpty())
        .validationErrors(errors)
        .build();
  }

  protected QuestionTemplate fetchTemplateForTesting(UUID id) {
    QuestionTemplate template =
        questionTemplateRepository
            .findById(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    return template;
  }

  private List<String> validateTemplateSyntax(QuestionTemplateRequest request) {
    List<String> errors = new ArrayList<>();

    // Validate template text has placeholders matching parameters
    String templateTextStr = request.getTemplateText() != null ? request.getTemplateText().toString() : "";
    Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    Matcher matcher = pattern.matcher(templateTextStr);

    Set<String> placeholders = new HashSet<>();
    while (matcher.find()) {
      placeholders.add(matcher.group(1));
    }

    Map<String, Object> parameters =
        request.getParameters() != null ? request.getParameters() : Collections.emptyMap();
    for (String placeholder : placeholders) {
      if (!parameters.containsKey(placeholder)) {
        errors.add("Placeholder {{" + placeholder + "}} not defined in parameters");
      }
    }

    if (request.getDifficultyRules() == null || request.getDifficultyRules().isEmpty()) {
      errors.add("Difficulty rules are required");
    }

    return errors;
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
}
