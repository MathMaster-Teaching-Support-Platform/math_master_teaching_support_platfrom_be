package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.QuestionTemplateService;
import com.fptu.math_master.util.SecurityUtils;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionTemplateServiceImpl implements QuestionTemplateService {

  QuestionTemplateRepository questionTemplateRepository;
  AIEnhancementService aiEnhancementService;
  QuestionRepository questionRepository;

  @Override
  @Transactional
  public QuestionTemplateResponse createQuestionTemplate(QuestionTemplateRequest request) {
    log.info("Creating question template: {}", request.getName());

    UUID currentUserId = SecurityUtils.getCurrentUserId();

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
            .status(TemplateStatus.DRAFT)
            .usageCount(0)
            .build();

    template = questionTemplateRepository.save(template);

    log.info("Question template created with id: {}", template.getId());
    return mapToResponse(template);
  }

  @Override
  @Transactional
  public QuestionTemplateResponse updateQuestionTemplate(UUID id, QuestionTemplateRequest request) {
    log.info("Updating question template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    if (template.getStatus() == TemplateStatus.PUBLISHED) {
      throw new AppException(ErrorCode.TEMPLATE_ALREADY_PUBLISHED);
    }

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

    log.info("Question template updated: {}", id);
    return mapToResponse(template);
  }

  @Override
  @Transactional
  public void deleteQuestionTemplate(UUID id) {
    log.info("Deleting question template: {}", id);
    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    if (template.getStatus() == TemplateStatus.PUBLISHED) {
      log.warn("Template {} is PUBLISHED — archiving instead of soft-deleting to preserve audit trail", id);
      template.setStatus(TemplateStatus.ARCHIVED);
      questionTemplateRepository.save(template);
      return;
    }

    template.setDeletedAt(Instant.now());
    questionTemplateRepository.save(template);
    log.info("Question template soft-deleted: {}", id);
  }

  @Override
  @Transactional
  public QuestionTemplateResponse publishTemplate(UUID id) {
    log.info("Publishing template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    if (template.getStatus() == TemplateStatus.PUBLISHED) {
      throw new AppException(ErrorCode.TEMPLATE_ALREADY_PUBLISHED);
    }
    if (template.getStatus() == TemplateStatus.ARCHIVED) {
      throw new AppException(ErrorCode.TEMPLATE_ALREADY_ARCHIVED);
    }

    template.setStatus(TemplateStatus.PUBLISHED);
    template = questionTemplateRepository.save(template);

    log.info("Template {} published", id);
    return mapToResponse(template);
  }

  @Override
  @Transactional
  public QuestionTemplateResponse archiveTemplate(UUID id) {
    log.info("Archiving template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    if (template.getStatus() == TemplateStatus.ARCHIVED) {
      throw new AppException(ErrorCode.TEMPLATE_ALREADY_ARCHIVED);
    }

    template.setStatus(TemplateStatus.ARCHIVED);
    template = questionTemplateRepository.save(template);

    log.info("Template {} archived", id);
    return mapToResponse(template);
  }

  @Override
  @Transactional(readOnly = true)
  public QuestionTemplateResponse getQuestionTemplateById(UUID id) {
    log.info("Getting question template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    if (!template.getIsPublic()
        && !template.getCreatedBy().equals(currentUserId)
        && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }

    return mapToResponse(template);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionTemplateResponse> getMyQuestionTemplates(Pageable pageable) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    log.info("Getting question templates for user: {}", currentUserId);

    return questionTemplateRepository
        .findByCreatedByAndNotDeleted(currentUserId, pageable)
        .map(this::mapToResponse);
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
    log.info(
        "Searching question templates – type: {}, cognitive: {}, isPublic: {}, term: {}",
        templateType, cognitiveLevel, isPublic, searchTerm);

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    return questionTemplateRepository
        .searchTemplates(currentUserId, isPublic, templateType, cognitiveLevel, searchTerm, pageable)
        .map(this::mapToResponse);
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

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    if (template.getStatus() == TemplateStatus.DRAFT && !template.getIsPublic()) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    template.setIsPublic(!template.getIsPublic());
    template = questionTemplateRepository.save(template);

    log.info("Template {} public status toggled to: {}", id, template.getIsPublic());
    return mapToResponse(template);
  }

  @Override
  @Transactional
  public TemplateTestResponse testTemplate(UUID id, Integer sampleCount, Boolean useAI) {
    log.info("Testing template: {} (LLM-based)", id);
    QuestionTemplate template = fetchTemplateForTesting(id);
    return generateWithLLMAndSave(template, sampleCount != null ? sampleCount : 3);
  }

  @Override
  @Transactional
  public AIEnhancedQuestionResponse generateAIEnhancedQuestion(UUID id) {
    log.info("Generating AI-enhanced question from template: {}", id);

    QuestionTemplate template = fetchTemplateForTesting(id);

    try {
      GeneratedQuestionSample sample = aiEnhancementService.generateQuestion(template, 0);

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

      AIEnhancedQuestionResponse enhanced = aiEnhancementService.enhanceQuestion(enhancementRequest);

      enhanced.setOriginalQuestionText(sample.getQuestionText());
      enhanced.setOriginalOptions(sample.getOptions());

      // Save Question entity with DRAFT status
      try {
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        // Build generation metadata with DRAFT status and enhancement info
        Map<String, Object> generationMetadata = new HashMap<>();
        generationMetadata.put("status", "DRAFT");
        generationMetadata.put("enhanced", enhanced.isEnhanced());
        generationMetadata.put("usedParameters", sample.getUsedParameters());
        generationMetadata.put("enhancementApplied", true);

        // Create Question entity
        Question question = Question.builder()
            .createdBy(currentUserId)
            .templateId(template.getId())
            .questionType(template.getTemplateType())
            .questionText(enhanced.getEnhancedQuestionText() != null
                ? enhanced.getEnhancedQuestionText()
                : enhanced.getOriginalQuestionText())
            .options(enhanced.getEnhancedOptions() != null
                ? new HashMap<>(enhanced.getEnhancedOptions())
                : (enhanced.getOriginalOptions() != null ? new HashMap<>(enhanced.getOriginalOptions()) : null))
            .correctAnswer(enhanced.getCorrectAnswerKey())
            .explanation(enhanced.getExplanation())
            .difficulty(sample.getCalculatedDifficulty())
            .cognitiveLevel(template.getCognitiveLevel())
            .generationMetadata(generationMetadata)
            .build();

        // Save to database
        Question savedQuestion = questionRepository.save(question);
        log.info(
            "Saved AI-enhanced question as DRAFT with ID: {} (template: {})",
            savedQuestion.getId(),
            template.getId());

        // Return response with generated question ID
        enhanced.setGeneratedQuestionId(savedQuestion.getId().toString());

      } catch (Exception e) {
        log.error("Failed to save generated question as DRAFT: {}", e.getMessage(), e);
        // Continue and return response even if save fails - don't block the response
        if (enhanced.getValidationErrors() == null) {
          enhanced.setValidationErrors(new ArrayList<>());
        }
        enhanced.getValidationErrors()
            .add("Warning: Question was not saved to database. Error: " + e.getMessage());
      }

      return enhanced;

    } catch (Exception e) {
      log.error("Failed to generate AI-enhanced question: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  private TemplateTestResponse generateWithLLMAndSave(QuestionTemplate template, int sampleCount) {
    List<GeneratedQuestionSample> samples = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    List<UUID> savedQuestionIds = new ArrayList<>();

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    for (int i = 0; i < sampleCount; i++) {
      try {
        GeneratedQuestionSample sample = aiEnhancementService.generateQuestion(template, i);
        if (sample.getQuestionText().startsWith("[LLM generation failed]")) {
          errors.add("Sample " + (i + 1) + ": " + sample.getQuestionText());
        } else {
          samples.add(sample);

          // Save question as DRAFT
          try {
            Map<String, Object> generationMetadata = new HashMap<>();
            generationMetadata.put("status", "DRAFT");
            generationMetadata.put("sampleIndex", i);
            generationMetadata.put("usedParameters", sample.getUsedParameters());
            generationMetadata.put("enhancementApplied", false);

            Question question = Question.builder()
                .createdBy(currentUserId)
                .templateId(template.getId())
                .questionType(template.getTemplateType())
                .questionText(sample.getQuestionText())
                .options(sample.getOptions() != null ? new HashMap<>(sample.getOptions()) : null)
                .correctAnswer(sample.getCorrectAnswer())
                .difficulty(sample.getCalculatedDifficulty())
                .cognitiveLevel(template.getCognitiveLevel())
                .generationMetadata(generationMetadata)
                .build();

            Question savedQuestion = questionRepository.save(question);
            savedQuestionIds.add(savedQuestion.getId());

            log.info(
                "Saved test question sample {} as DRAFT with ID: {} (template: {})",
                i + 1,
                savedQuestion.getId(),
                template.getId());

          } catch (Exception saveException) {
            log.error("Failed to save test question sample {}: {}", i + 1, saveException.getMessage(), saveException);
            errors.add("Sample " + (i + 1) + " saved but with warning: " + saveException.getMessage());
          }
        }
      } catch (Exception e) {
        log.error("Error generating LLM sample {}: {}", i + 1, e.getMessage());
        errors.add("Sample " + (i + 1) + " generation failed: " + e.getMessage());
      }
    }

    TemplateTestResponse response = TemplateTestResponse.builder()
        .templateId(template.getId())
        .templateName(template.getName())
        .samples(samples)
        .isValid(!samples.isEmpty())
        .validationErrors(errors)
        .build();

    // Add saved question IDs to response (if TemplateTestResponse supports it)
    if (!savedQuestionIds.isEmpty()) {
      log.info("Saved {} test question samples as DRAFT", savedQuestionIds.size());
    }

    return response;
  }


  /**
   * access extended to public templates (was owner-only).
   * only PUBLISHED templates may generate questions.
   */
  protected QuestionTemplate fetchTemplateForTesting(UUID id) {
    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    boolean isOwner = template.getCreatedBy().equals(currentUserId);
    boolean isAdmin = SecurityUtils.hasRole("ADMIN");
    boolean isPublicTemplate = Boolean.TRUE.equals(template.getIsPublic());

    if (!isOwner && !isAdmin && !isPublicTemplate) {
      throw new AppException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }

    if (template.getStatus() != TemplateStatus.PUBLISHED) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    return template;
  }

  private List<String> validateTemplateSyntax(QuestionTemplateRequest request) {
    List<String> errors = new ArrayList<>();

    String templateTextStr =
        request.getTemplateText() != null ? request.getTemplateText().toString() : "";
    Pattern pattern = Pattern.compile("\\{\\{(\\w+)}}");
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
            .status(template.getStatus())
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

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }
  }
}
