package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.AIEnhancementRequest;
import com.fptu.math_master.dto.request.AIGenerateTemplatesRequest;
import com.fptu.math_master.dto.request.GenerateCanonicalQuestionsRequest;
import com.fptu.math_master.dto.request.GenerateTemplateQuestionsRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.AIGeneratedTemplatesResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.GeneratedQuestionsBatchResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionSourceType;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.QuestionStatus;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CanonicalQuestionRepository;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.BlueprintService;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.QuestionTemplateService;
import com.fptu.math_master.service.TokenCostConfigService;
import com.fptu.math_master.service.UserSubscriptionService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionTemplateServiceImpl implements QuestionTemplateService {

  QuestionTemplateRepository questionTemplateRepository;
  ChapterRepository chapterRepository;
  LessonRepository lessonRepository;
  AIEnhancementService aiEnhancementService;
  GeminiService geminiService;
  QuestionRepository questionRepository;
  QuestionBankRepository questionBankRepository;
  CanonicalQuestionRepository canonicalQuestionRepository;
  BlueprintService blueprintService;
  UserSubscriptionService userSubscriptionService;
  TokenCostConfigService tokenCostConfigService;

  @Override
  @Transactional
  public QuestionTemplateResponse createQuestionTemplate(QuestionTemplateRequest request) {
    log.info("Creating question template: {}", request.getName());

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    UUID bankId = request.getQuestionBankId();
    if (bankId != null) {
      validateCanUseQuestionBank(bankId, currentUserId);
    }

    List<String> validationErrors = validateTemplateSyntax(request);
    if (!validationErrors.isEmpty()) {
      log.error("Template validation failed: {}", validationErrors);
      throw new AppException(ErrorCode.INVALID_TEMPLATE_SYNTAX);
    }

    // Validate tags
    validateTags(request.getTags());

    // Auto-resolve chapterId if not provided
    UUID chapterId = request.getChapterId();
    UUID lessonId = request.getLessonId();

    if (chapterId == null && lessonId != null) {
      // Resolve chapter from lesson
      chapterId = lessonRepository.findById(lessonId)
          .map(Lesson::getChapterId)
          .orElse(null);
    }

    QuestionTemplate template =
        QuestionTemplate.builder()
            .questionBankId(bankId)
            .chapterId(chapterId)
            .lessonId(lessonId)
            .canonicalQuestionId(request.getCanonicalQuestionId())
            .name(request.getName())
            .description(request.getDescription())
            .templateType(request.getTemplateType())
            .templateText(request.getTemplateText())
            .parameters(request.getParameters())
            .answerFormula(request.getAnswerFormula())
            .diagramTemplate(request.getDiagramTemplate())
            .solutionStepsTemplate(request.getSolutionStepsTemplate())
            .optionsGenerator(request.getOptionsGenerator())
            // Cross-parameter constraints — must go to `globalConstraints` (the
            // writable column). The legacy `constraints` setter exists but is
            // mapped to a column with insertable=false/updatable=false, so JPA
            // silently dropped writes through it; that was the V112-era data
            // loss bug.
            .globalConstraints(request.getConstraints())
            .statementMutations(request.getStatementMutations())
            .cognitiveLevel(request.getCognitiveLevel())
            .tags(request.getTags())
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
            .status(TemplateStatus.DRAFT)
            .usageCount(0)
            .build();
    template.setCreatedBy(currentUserId);

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

    UUID bankId = request.getQuestionBankId();
    if (bankId != null) {
      validateCanUseQuestionBank(bankId, currentUserId);
      template.setQuestionBankId(bankId);
    } else {
      // Allow un-assigning from a bank explicitly
      template.setQuestionBankId(null);
    }

    template.setCanonicalQuestionId(request.getCanonicalQuestionId());

    if (template.getStatus() == TemplateStatus.PUBLISHED) {
      throw new AppException(ErrorCode.TEMPLATE_ALREADY_PUBLISHED);
    }

    List<String> validationErrors = validateTemplateSyntax(request);
    if (!validationErrors.isEmpty()) {
      log.error("Template validation failed: {}", validationErrors);
      throw new AppException(ErrorCode.INVALID_TEMPLATE_SYNTAX);
    }

    // Validate tags
    validateTags(request.getTags());

    template.setName(request.getName());
    template.setDescription(request.getDescription());
    template.setTemplateType(request.getTemplateType());
    template.setTemplateText(request.getTemplateText());
    template.setParameters(request.getParameters());
    template.setAnswerFormula(request.getAnswerFormula());
    template.setDiagramTemplate(request.getDiagramTemplate());
    template.setSolutionStepsTemplate(request.getSolutionStepsTemplate());
    template.setOptionsGenerator(request.getOptionsGenerator());
    // See note in createQuestionTemplate above — the legacy setter writes to a
    // read-only column and gets silently dropped.
    template.setGlobalConstraints(request.getConstraints());
    template.setStatementMutations(request.getStatementMutations());
    template.setCognitiveLevel(request.getCognitiveLevel());
    template.setTags(request.getTags());
    if (request.getIsPublic() != null) {
      template.setIsPublic(request.getIsPublic());
    }

    // Persist chapter/lesson on update — without this branch the
    // existing chapterId/lessonId would silently survive every edit, so
    // teachers couldn't fix templates imported from Excel that came in
    // without an academic anchor. Mirrors the create-time logic so an
    // update with only lessonId still resolves chapterId from the lesson.
    UUID requestedChapterId = request.getChapterId();
    UUID requestedLessonId = request.getLessonId();
    if (requestedLessonId != null) {
      template.setLessonId(requestedLessonId);
      if (requestedChapterId == null) {
        requestedChapterId =
            lessonRepository
                .findById(requestedLessonId)
                .map(Lesson::getChapterId)
                .orElse(null);
      }
      template.setChapterId(requestedChapterId);
    } else {
      // No lesson supplied → write chapter as-is (incl. explicit null to
      // clear the academic anchor) and detach any previous lesson link.
      template.setLessonId(null);
      template.setChapterId(requestedChapterId);
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
      log.warn(
          "Template {} is PUBLISHED — archiving instead of soft-deleting to preserve audit trail",
          id);
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
  public QuestionTemplateResponse unpublishTemplate(UUID id) {
    log.info("Unpublishing template: {}", id);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(id)
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(template.getCreatedBy(), currentUserId);

    if (template.getStatus() != TemplateStatus.PUBLISHED) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_PUBLISHED);
    }

    template.setStatus(TemplateStatus.DRAFT);
    template = questionTemplateRepository.save(template);

    log.info("Template {} unpublished (reverted to DRAFT)", id);
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
  public Page<QuestionTemplateResponse> getMyQuestionTemplatesFiltered(
      String search, TemplateStatus status, UUID gradeId, UUID chapterId, Pageable pageable) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
    return questionTemplateRepository
        .findByCreatedByWithSearch(currentUserId, status, searchTerm, gradeId, chapterId, pageable)
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
        templateType,
        cognitiveLevel,
        isPublic,
        searchTerm);

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    return questionTemplateRepository
        .searchTemplates(
            currentUserId, isPublic, templateType, cognitiveLevel, searchTerm, pageable)
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
      // Single-sample preview: ask the constraint-aware selector for one tuple
      // and feed it into the substitutor. If the selector returns nothing
      // (e.g. no parameters defined, or AI hiccup) we fall through to the
      // legacy random sampler — generateQuestion handles a null preset.
      Map<String, Object> presetTuple = pickPreviewTuple(template);
      GeneratedQuestionSample sample =
          aiEnhancementService.generateQuestion(template, 0, presetTuple);

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

      AIEnhancedQuestionResponse enhanced =
          aiEnhancementService.enhanceQuestion(enhancementRequest);

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
        Question question =
            Question.builder()
            .questionBankId(template.getQuestionBankId())
                .templateId(template.getId())
                .chapterId(template.getChapterId())
            .canonicalQuestionId(template.getCanonicalQuestionId())
                .questionType(template.getTemplateType())
                .questionText(
                    enhanced.getEnhancedQuestionText() != null
                        ? enhanced.getEnhancedQuestionText()
                        : enhanced.getOriginalQuestionText())
                .options(
                    enhanced.getEnhancedOptions() != null
                        ? new HashMap<>(enhanced.getEnhancedOptions())
                        : (enhanced.getOriginalOptions() != null
                            ? new HashMap<>(enhanced.getOriginalOptions())
                            : null))
                .correctAnswer(enhanced.getCorrectAnswerKey())
                .explanation(enhanced.getExplanation())
                .solutionSteps(sample.getSolutionSteps())
                .diagramData(sample.getDiagramData())
                .cognitiveLevel(template.getCognitiveLevel())
                .generationMetadata(generationMetadata)
                .build();
        question.setCreatedBy(currentUserId);

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
        enhanced
            .getValidationErrors()
            .add("Warning: Question was not saved to database. Error: " + e.getMessage());
      }

      return enhanced;

    } catch (Exception e) {
      log.error("Failed to generate AI-enhanced question: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }
  }

  @Override
  @Transactional
  public GeneratedQuestionsBatchResponse generateQuestionsFromTemplate(
      UUID id, GenerateTemplateQuestionsRequest request) {
    QuestionTemplate template = fetchTemplateForTesting(id);
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    int requested = request.getCount() != null ? request.getCount() : 0;
    if (requested <= 0) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }

    List<UUID> generatedIds = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    int skippedCount = 0;

    // 1. Collect already-used parameter tuples for distinctness.
    List<Map<String, Object>> alreadyUsed = new ArrayList<>();
    if (request.isAvoidDuplicates()) {
      List<Question> existingQuestions =
          questionRepository.findByTemplateIdAndNotDeleted(template.getId());
      for (Question q : existingQuestions) {
        // Rejected / withdrawn drafts must not block picking the same parameter tuple again —
        // teachers often "sinh lại" after archiving the previous attempt.
        if (q.getQuestionStatus() == QuestionStatus.ARCHIVED) {
          continue;
        }
        if (q.getGenerationMetadata() != null
            && q.getGenerationMetadata().get("usedParameters") instanceof Map<?, ?> m) {
          @SuppressWarnings("unchecked")
          Map<String, Object> tuple = (Map<String, Object>) m;
          alreadyUsed.add(tuple);
        }
      }
    }

    // 2. Ask the AI value selector for `requested + buffer` value sets.
    int buffer = Math.max(2, requested / 2);
    List<Map<String, Object>> proposed =
        blueprintService.selectValueSets(
            template, requested + buffer, alreadyUsed, request.getDistinctnessHint());

    if (proposed.isEmpty()) {
      // Distinguish the three failure modes so the FE toast points the teacher
      // at the right thing to fix instead of showing a generic "no valid sets".
      boolean noParams =
          template.getParameters() == null || template.getParameters().isEmpty();
      boolean noConstraints =
          template.getGlobalConstraints() == null
              || template.getGlobalConstraints().length == 0;
      String warning;
      if (noParams) {
        warning =
            "Mẫu này chưa khai báo tham số nào — generator không có gì để chọn. "
                + "Hãy thêm ít nhất một tham số ở phần biến số.";
        log.warn("[gen] template {} has no parameters; selector returned empty", template.getId());
      } else if (noConstraints) {
        warning =
            "Không có bộ giá trị nào được chấp nhận: Gemini có thể đã trả tuple nhưng tất cả bị lọc "
                + "(constraintText trên biến, thứ tự trục, hoặc kiểm tra answerFormula). "
                + "Xem log backend WARN [Blueprint] để biết lý do đầu tiên; hoặc chỉnh constraint/công thức.";
        log.warn(
            "[gen] template {} selector returned empty without globalConstraints — "
                + "often all tuples rejected by validation (not necessarily Gemini); see [Blueprint] WARN",
            template.getId());
      } else {
        warning =
            "AI selector không tìm được bộ giá trị nào thỏa mãn ràng buộc. "
                + "Hãy nới lỏng constraintText hoặc kiểm tra ràng buộc giữa các biến.";
        log.warn(
            "[gen] template {} constraints appear over-tight: globalConstraints={}",
            template.getId(),
            java.util.Arrays.toString(template.getGlobalConstraints()));
      }
      warnings.add(warning);
      return GeneratedQuestionsBatchResponse.builder()
          .totalRequested(requested)
          .totalGenerated(0)
          .generatedQuestionIds(generatedIds)
          .warnings(warnings)
          .build();
    }

    // Token deduction: cost-per-question is admin-configurable via TokenCostConfig
    // (featureKey="question-generate", default 1). Total cost = perQuestion ×
    // requested. Charged here, after the AI selector has produced usable value
    // sets but before substitution, so a substitution failure rolls back the
    // deduction with the parent @Transactional. ADMIN exempt; costPerUse=0
    // means the feature is free.
    if (!SecurityUtils.hasRole("ADMIN")) {
      Integer perQuestion = tokenCostConfigService.getCostPerUse("question-generate");
      int totalCost = (perQuestion == null ? 0 : perQuestion) * requested;
      if (totalCost > 0) {
        userSubscriptionService.consumeMyTokens(totalCost, "GENERATE_QUESTIONS_FROM_TEMPLATE");
      }
    }

    String selectionPromptVersion = blueprintService.selectionPromptVersion();

    // 3. Substitute + evaluate per set, persist as UNDER_REVIEW.
    for (Map<String, Object> tuple : proposed) {
      if (generatedIds.size() >= requested) break;

      // Skip duplicates against existing usage.
      if (request.isAvoidDuplicates() && alreadyUsed.contains(tuple)) {
        skippedCount++;
        continue;
      }

      try {
        // Render the question against the AI-selected tuple. The substitutor used
        // to call its own random sampler and we'd overwrite the metadata after the
        // fact — that left the rendered text and the recorded `usedParameters` out
        // of sync, and worse, ignored the constraint-aware tuple entirely. Now the
        // tuple drives substitution from the start.
        GeneratedQuestionSample sample =
            aiEnhancementService.generateQuestion(template, generatedIds.size(), tuple);
        if (sample == null
            || sample.getQuestionText() == null
            || sample.getQuestionText().isBlank()) {
          warnings.add("Substitutor produced empty question for tuple " + tuple);
          continue;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("status", QuestionStatus.UNDER_REVIEW.name());
        metadata.put("usedParameters", sample.getUsedParameters());
        metadata.put("promptVersion", selectionPromptVersion);
        metadata.put("valueSelectionStrategy", "AI_CONSTRAINT_AWARE");
        if (request.getDistinctnessHint() != null && !request.getDistinctnessHint().isBlank()) {
          metadata.put("distinctnessHint", request.getDistinctnessHint());
        }
        if (sample.getGenerationMetadata() != null) {
          metadata.putAll(sample.getGenerationMetadata());
        }

        Question question =
            Question.builder()
                .questionBankId(template.getQuestionBankId())
                .templateId(template.getId())
                .chapterId(template.getChapterId())
                .canonicalQuestionId(template.getCanonicalQuestionId())
                .questionType(template.getTemplateType())
                .questionText(sample.getQuestionText())
                .options(
                    sample.getOptions() != null
                        ? new HashMap<String, Object>(sample.getOptions())
                        : null)
                .correctAnswer(sample.getCorrectAnswer())
                .explanation(sample.getExplanation())
                .solutionSteps(sample.getSolutionSteps())
                .diagramData(sample.getDiagramData())
                .cognitiveLevel(template.getCognitiveLevel())
                .questionStatus(QuestionStatus.UNDER_REVIEW)
                .questionSourceType(QuestionSourceType.AI_GENERATED)
                .generationMetadata(metadata)
                .build();
        question.setCreatedBy(currentUserId);

        Question saved = questionRepository.save(question);
        generatedIds.add(saved.getId());
        alreadyUsed.add(sample.getUsedParameters());
      } catch (Exception ex) {
        log.warn(
            "Substitutor failed for tuple {} on template {}: {}", tuple, id, ex.getMessage());
        warnings.add("Substitutor failed for one set: " + ex.getMessage());
      }
    }

    if (skippedCount > 0) {
      warnings.add("Skipped " + skippedCount + " duplicate parameter sets.");
    }
    if (generatedIds.size() < requested) {
      warnings.add(
          "Wanted "
              + requested
              + " questions, the constraint-aware selector + substitutor produced "
              + generatedIds.size()
              + ". Try relaxing the constraints or lowering the count.");
    }

    return GeneratedQuestionsBatchResponse.builder()
        .totalRequested(requested)
        .totalGenerated(generatedIds.size())
        .generatedQuestionIds(generatedIds)
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  /**
   * @deprecated The "AI from canonical" mode has been removed. The unified generator
   *     reads the template's Blueprint and selects values via {@code BlueprintService}.
   *     This method is retained only so existing canonical-question UI does not 404;
   *     it now delegates to the regular template generation path.
   */
  @Deprecated
  @Override
  @Transactional
  public GeneratedQuestionsBatchResponse generateQuestionsFromCanonical(
      UUID canonicalQuestionId, GenerateCanonicalQuestionsRequest request) {
    GenerateTemplateQuestionsRequest delegatedRequest =
        GenerateTemplateQuestionsRequest.builder()
            .count(request.getCount())
            .avoidDuplicates(true)
            .build();
    return generateQuestionsFromTemplate(request.getTemplateId(), delegatedRequest);
  }

  @Override
  @Transactional
  public AIGeneratedTemplatesResponse aiGenerateTemplates(AIGenerateTemplatesRequest request) {
    log.info("AI generating templates from lesson: {}", request.getLessonId());

    // Load lesson to ensure it exists and get its content
    Lesson lesson =
        lessonRepository
            .findById(request.getLessonId())
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    int templateCount = request.getTemplateCount() != null ? request.getTemplateCount() : 1;

    log.info(
        "Generating {} templates from lesson {} (title: {})",
        templateCount,
        lesson.getId(),
        lesson.getTitle());

    List<QuestionTemplateResponse> generatedTemplates = new ArrayList<>();

    // Build a prompt that tells Gemini to analyze lesson content and generate templates
    String lessonContent = buildLessonContent(lesson);
    String prompt =
        String.format(
            "Analyze the following lesson content and generate %d practical and diverse question templates:\n\n"
                + "Lesson: %s\n"
                + "Content:\n%s\n\n"
                + "For each template, provide the following in JSON format:\n"
                + "{\n"
                + "  \"templates\": [\n"
                + "    {\n"
                + "      \"name\": \"Template Name\",\n"
                + "      \"description\": \"What this template tests\",\n"
                + "      \"templateType\": \"MULTIPLE_CHOICE\",\n"
                + "      \"cognitiveLevel\": \"UNDERSTAND\",\n"
                + "      \"templateText\": \"Question text with {parameter} placeholders\",\n"
                + "      \"parameters\": {\n"
                + "        \"parameter\": {\"type\": \"integer\", \"min\": 1, \"max\": 100}\n"
                + "      },\n"
                + "      \"answerFormula\": \"formula to calculate answer\",\n"
                + "      \"tags\": [\"tag1\", \"tag2\"]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "Generate templates that are practical, diverse in cognitive levels, and reusable.",
            templateCount, lesson.getTitle(), lessonContent);

    try {
      // Call Gemini to generate templates
      String aiResponse = geminiService.sendMessage(prompt);
      log.debug("Gemini response for template generation: {}", aiResponse);

      // Parse the JSON response (simplified - in production, use proper JSON parsing and
      // validation)
      List<QuestionTemplate> savedTemplates =
          parseAndSaveTemplates(aiResponse, lesson, currentUserId);

      generatedTemplates =
          savedTemplates.stream().map(this::mapToResponse).collect(Collectors.toList());

      log.info(
          "Successfully generated and saved {} templates from lesson {}",
          generatedTemplates.size(),
          lesson.getId());

    } catch (Exception e) {
      log.error("Failed to generate templates from lesson: {}", e.getMessage(), e);
      throw new AppException(ErrorCode.TEMPLATE_GENERATION_FAILED);
    }

    return AIGeneratedTemplatesResponse.builder()
        .totalTemplatesGenerated(generatedTemplates.size())
        .generatedTemplates(generatedTemplates)
        .lessonName(lesson.getTitle())
        .message(
            String.format(
                "%d templates generated successfully from lesson '%s'. Templates are in DRAFT status and ready to be configured.",
                generatedTemplates.size(), lesson.getTitle()))
        .build();
  }

  private String buildLessonContent(Lesson lesson) {
    // Combine lesson metadata and content for AI analysis
    StringBuilder content = new StringBuilder();
    if (lesson.getSummary() != null) {
      content.append("Summary: ").append(lesson.getSummary()).append("\n\n");
    }
    if (lesson.getLessonContent() != null) {
      content.append("Content: ").append(lesson.getLessonContent()).append("\n\n");
    }
    if (lesson.getLearningObjectives() != null) {
      content.append("Learning Objectives: ").append(lesson.getLearningObjectives()).append("\n\n");
    }
    return !content.isEmpty()
        ? content.toString()
        : "No detailed content available. Generate templates based on the lesson title: "
            + lesson.getTitle();
  }

  private List<QuestionTemplate> parseAndSaveTemplates(
      String aiResponse, Lesson lesson, UUID currentUserId) {
    List<QuestionTemplate> savedTemplates = new ArrayList<>();

    try {
      // Extract JSON from response (Gemini might include markdown code blocks)
      String jsonContent = extractJsonFromResponse(aiResponse);

      // Parse templates and create QuestionTemplate entities
      // Note: In a real implementation, use ObjectMapper for proper JSON parsing
      List<Map<String, Object>> templates = extractTemplatesList(jsonContent);

      for (Map<String, Object> templateData : templates) {
        QuestionTemplate template =
            QuestionTemplate.builder()
                .lessonId(lesson.getId())
                .name((String) templateData.getOrDefault("name", "Generated Template"))
                .description((String) templateData.getOrDefault("description", ""))
                .templateType(
                    QuestionType.valueOf(
                        (String) templateData.getOrDefault("templateType", "MULTIPLE_CHOICE")))
                .templateText(Map.of("en", (String) templateData.getOrDefault("templateText", "")))
                .parameters((Map<String, Object>) templateData.get("parameters"))
                .answerFormula((String) templateData.get("answerFormula"))
                .cognitiveLevel(
                    CognitiveLevel.valueOf(
                        (String) templateData.getOrDefault("cognitiveLevel", "UNDERSTAND")))
                .tags(
                    ((List<?>) templateData.getOrDefault("tags", new ArrayList<>()))
                        .stream()
                        .map(tag -> {
                          try {
                            return com.fptu.math_master.enums.QuestionTag.valueOf(tag.toString().trim().toUpperCase());
                          } catch (IllegalArgumentException e) {
                            return com.fptu.math_master.enums.QuestionTag.fromVietnameseName(tag.toString());
                          }
                        })
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()))
                .isPublic(false)
                .status(TemplateStatus.DRAFT)
                .usageCount(0)
                .build();
        template.setCreatedBy(currentUserId);

        savedTemplates.add(questionTemplateRepository.save(template));
        log.debug("Saved AI-generated template: {}", template.getName());
      }

    } catch (Exception e) {
      log.error("Failed to parse AI response and save templates: {}", e.getMessage(), e);
      // Return whatever templates were successfully saved
    }

    return savedTemplates;
  }

  private String extractJsonFromResponse(String response) {
    // Remove markdown code blocks if present
    if (response.contains("```json")) {
      int start = response.indexOf("```json") + 7;
      int end = response.lastIndexOf("```");
      if (end > start) {
        return response.substring(start, end).trim();
      }
    } else if (response.contains("```")) {
      int start = response.indexOf("```") + 3;
      int end = response.lastIndexOf("```");
      if (end > start) {
        return response.substring(start, end).trim();
      }
    }
    return response;
  }

  private List<Map<String, Object>> extractTemplatesList(String jsonContent) {
    // Simplified parsing - in production, use JsonNode with ObjectMapper
    List<Map<String, Object>> templates = new ArrayList<>();

    // This is a placeholder implementation
    // In production, use proper JSON parsing with ObjectMapper or Jackson
    log.debug("Extracting templates from JSON content");

    // For now, return empty list - the actual implementation would parse the JSON properly
    return templates;
  }

  private TemplateTestResponse generateWithLLMAndSave(QuestionTemplate template, int sampleCount) {
    List<GeneratedQuestionSample> samples = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    List<UUID> savedQuestionIds = new ArrayList<>();

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // Pre-fetch constraint-respecting tuples for the whole preview batch in one
    // BlueprintService call (one Gemini round trip), then substitute per sample.
    // Falls back to legacy random sampling if the selector produces nothing.
    List<Map<String, Object>> presetTuples =
        blueprintService.selectValueSets(template, sampleCount, java.util.List.of(), null);

    for (int i = 0; i < sampleCount; i++) {
      try {
        Map<String, Object> presetTuple =
            i < presetTuples.size() ? presetTuples.get(i) : null;
        GeneratedQuestionSample sample =
            aiEnhancementService.generateQuestion(template, i, presetTuple);
        if (sample.getQuestionText().startsWith("[LLM generation failed]")) {
          errors.add("Sample " + (i + 1) + ": " + sample.getQuestionText());
        } else {
          samples.add(sample);

          // Save question as DRAFT in its own transaction to prevent connection timeouts
          try {
            UUID savedQuestionId =
                saveQuestionWithOwnTransaction(currentUserId, template, sample, i);
            savedQuestionIds.add(savedQuestionId);
          } catch (Exception saveException) {
            log.error(
                "Failed to save test question sample {}: {}",
                i + 1,
                saveException.getMessage(),
                saveException);
            errors.add(
                "Sample " + (i + 1) + " saved but with warning: " + saveException.getMessage());
          }
        }
      } catch (Exception e) {
        log.error("Error generating LLM sample {}: {}", i + 1, e.getMessage());
        errors.add("Sample " + (i + 1) + " generation failed: " + e.getMessage());
      }
    }

    TemplateTestResponse response =
        TemplateTestResponse.builder()
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
   * Saves a question in its own transaction to prevent connection timeouts during AI generation.
   * This is necessary because AI generation (external API calls) can be slow, and keeping a
   * database connection idle for too long can cause it to be reset.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected UUID saveQuestionWithOwnTransaction(
      UUID currentUserId,
      QuestionTemplate template,
      GeneratedQuestionSample sample,
      int sampleIndex) {
    Map<String, Object> generationMetadata = new HashMap<>();
    generationMetadata.put("status", "DRAFT");
    generationMetadata.put("sampleIndex", sampleIndex);
    generationMetadata.put("usedParameters", sample.getUsedParameters());
    generationMetadata.put("enhancementApplied", false);

    Question question =
        Question.builder()
            .templateId(template.getId())
        .questionBankId(template.getQuestionBankId())
            .chapterId(template.getChapterId())
        .canonicalQuestionId(template.getCanonicalQuestionId())
            .questionType(template.getTemplateType())
            .questionText(sample.getQuestionText())
            .options(sample.getOptions() != null ? new HashMap<>(sample.getOptions()) : null)
            .correctAnswer(sample.getCorrectAnswer())
        .explanation(sample.getExplanation())
        .solutionSteps(sample.getSolutionSteps())
        .diagramData(sample.getDiagramData())
            .cognitiveLevel(template.getCognitiveLevel())
            .generationMetadata(generationMetadata)
            .build();
    question.setCreatedBy(currentUserId);

    Question savedQuestion = questionRepository.save(question);
    log.info(
        "Saved test question sample {} as DRAFT with ID: {} (template: {})",
        sampleIndex + 1,
        savedQuestion.getId(),
        template.getId());

    return savedQuestion.getId();
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

    // Validate answerFormula based on question type
    if (request.getTemplateType() != QuestionType.TRUE_FALSE) {
      // MCQ and SHORT_ANSWER require answerFormula
      if (request.getAnswerFormula() == null || request.getAnswerFormula().trim().isEmpty()) {
        errors.add("Answer formula is required for " + request.getTemplateType() + " questions");
      }
    }

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

    return errors;
  }

  private QuestionTemplateResponse mapToResponse(QuestionTemplate template) {
    QuestionTemplateResponse.QuestionTemplateResponseBuilder builder =
        QuestionTemplateResponse.builder()
            .id(template.getId())
            .createdBy(template.getCreatedBy())
            .name(template.getName())
            .description(template.getDescription())
            .chapterId(template.getChapterId())
            .lessonId(template.getLessonId())
            .templateType(template.getTemplateType())
            .templateText(template.getTemplateText())
            .parameters(template.getParameters())
            .answerFormula(template.getAnswerFormula())
            .diagramTemplate(template.getDiagramTemplate())
            .solutionStepsTemplate(template.getSolutionStepsTemplate())
            .optionsGenerator(template.getOptionsGenerator())
            // Prefer the writable column. The legacy `constraints` column is
            // still read for templates that pre-date V112's backfill window so
            // historical data does not appear empty in the FE.
            .constraints(
                template.getGlobalConstraints() != null
                    ? template.getGlobalConstraints()
                    : template.getConstraints())
            .statementMutations(template.getStatementMutations())
            .cognitiveLevel(template.getCognitiveLevel())
            .tags(template.getTags())
            .isPublic(template.getIsPublic())
            .status(template.getStatus())
            .usageCount(template.getUsageCount())
            .avgSuccessRate(template.getAvgSuccessRate())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .questionBankId(template.getQuestionBankId())
            .canonicalQuestionId(template.getCanonicalQuestionId());

    // Populate chapter, subject, and grade info if chapterId exists. The FE
    // edit-mode cascade needs subjectId (not just subjectName) to drive the
    // grade/subject/chapter dropdowns when reopening an existing template.
    if (template.getChapterId() != null) {
      chapterRepository.findById(template.getChapterId()).ifPresent(chapter -> {
        builder.chapterName(chapter.getTitle());
        if (chapter.getSubject() != null) {
          builder.subjectId(chapter.getSubject().getId());
          builder.subjectName(chapter.getSubject().getName());
          if (chapter.getSubject().getSchoolGrade() != null) {
            builder.gradeLevel(String.valueOf(chapter.getSubject().getSchoolGrade().getGradeLevel()));
          }
        }
      });
    }

    QuestionTemplateResponse response = builder.build();

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

  private void validateCanUseQuestionBank(UUID bankId, UUID currentUserId) {
    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    // owner, admin, or public bank
    if (!bank.getTeacherId().equals(currentUserId)
        && !Boolean.TRUE.equals(bank.getIsPublic())
        && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }
  }

  /**
   * Validates tags for question template (tags are optional; only the size ceiling is enforced).
   */
  private void validateTags(List<com.fptu.math_master.enums.QuestionTag> tags) {
    if (tags != null && tags.size() > 5) {
      throw new AppException(ErrorCode.TOO_MANY_TAGS);
    }
  }

  /**
   * Single-tuple wrapper around {@link BlueprintService#selectValueSets}. Returns
   * {@code null} when the selector finds no valid tuple — in that case the caller
   * falls through to {@code AIEnhancementService}'s legacy sampler. We keep the
   * fallback because preview/enhancement paths must still produce something for
   * templates with no parameters or no constraints defined yet.
   */
  private Map<String, Object> pickPreviewTuple(QuestionTemplate template) {
    java.util.List<Map<String, Object>> tuples =
        blueprintService.selectValueSets(template, 1, java.util.List.of(), null);
    return tuples.isEmpty() ? null : tuples.get(0);
  }
}
