package com.fptu.math_master.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AddTemplateMappingRequest;
import com.fptu.math_master.dto.request.BatchAddTemplateMappingsRequest;
import com.fptu.math_master.dto.request.BuildExamMatrixRequest;
import com.fptu.math_master.dto.request.ExamMatrixRequest;
import com.fptu.math_master.dto.request.FinalizePreviewRequest;
import com.fptu.math_master.dto.request.GeneratePreviewRequest;
import com.fptu.math_master.dto.request.MatrixCellRequest;
import com.fptu.math_master.dto.request.MatrixRowRequest;
import com.fptu.math_master.dto.response.BatchTemplateMappingsResponse;
import com.fptu.math_master.dto.response.ExamMatrixResponse;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.FinalizePreviewResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.MatchingTemplatesResponse;
import com.fptu.math_master.dto.response.MatrixCellResponse;
import com.fptu.math_master.dto.response.MatrixChapterGroupResponse;
import com.fptu.math_master.dto.response.MatrixRowResponse;
import com.fptu.math_master.dto.response.MatrixValidationReport;
import com.fptu.math_master.dto.response.PreviewCandidatesResponse;
import com.fptu.math_master.dto.response.TemplateMappingResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.ExamMatrixRow;
import com.fptu.math_master.entity.ExamMatrixTemplateMapping;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.ExamMatrixTemplateMappingRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.ExamMatrixService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamMatrixServiceImpl implements ExamMatrixService {

  ExamMatrixRepository examMatrixRepository;
  ExamMatrixTemplateMappingRepository templateMappingRepository;
  ExamMatrixRowRepository examMatrixRowRepository;
  QuestionTemplateRepository questionTemplateRepository;
  QuestionRepository questionRepository;
  AssessmentRepository assessmentRepository;
  AssessmentQuestionRepository assessmentQuestionRepository;
  UserRepository userRepository;
  CurriculumRepository curriculumRepository;
  ChapterRepository chapterRepository;
  SubjectRepository subjectRepository;
  AIEnhancementService aiEnhancementService;

  // ── Matrix CRUD ─────────────────────────────────────────────────────────

  @Override
  @Transactional
  public ExamMatrixResponse createExamMatrix(ExamMatrixRequest request) {
    log.info("Creating exam matrix: {}", request.getName());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

    ExamMatrix matrix =
        ExamMatrix.builder()
            .teacherId(currentUserId)
            .name(request.getName())
            .description(request.getDescription())
            .isReusable(request.getIsReusable() != null ? request.getIsReusable() : false)
            .totalQuestionsTarget(request.getTotalQuestionsTarget())
            .totalPointsTarget(request.getTotalPointsTarget())
            .status(MatrixStatus.DRAFT)
            .build();

    matrix = examMatrixRepository.save(matrix);
    log.info("Exam matrix created with id: {}", matrix.getId());
    return buildMatrixResponse(matrix, Collections.emptyList());
  }

  @Override
  @Transactional
  public ExamMatrixResponse updateExamMatrix(UUID matrixId, ExamMatrixRequest request) {
    log.info("Updating exam matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    matrix.setName(request.getName());
    matrix.setDescription(request.getDescription());
    if (request.getIsReusable() != null) {
      matrix.setIsReusable(request.getIsReusable());
    }
    if (request.getTotalQuestionsTarget() != null) {
      matrix.setTotalQuestionsTarget(request.getTotalQuestionsTarget());
    }
    if (request.getTotalPointsTarget() != null) {
      matrix.setTotalPointsTarget(request.getTotalPointsTarget());
    }

    matrix = examMatrixRepository.save(matrix);
    log.info("Exam matrix updated: {}", matrixId);
    return buildMatrixResponse(
        matrix, templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId));
  }

  @Override
  @Transactional(readOnly = true)
  public ExamMatrixResponse getExamMatrixById(UUID matrixId) {
    log.info("Getting exam matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
    return buildMatrixResponse(matrix, mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public ExamMatrixResponse getExamMatrixByAssessmentId(UUID assessmentId) {
    log.info("Getting exam matrix for assessment: {}", assessmentId);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    ExamMatrix matrix =
        examMatrixRepository
            .findByAssessmentIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrix.getId());
    return buildMatrixResponse(matrix, mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ExamMatrixResponse> getMyExamMatrices() {
    log.info("Getting my exam matrices");

    UUID currentUserId = getCurrentUserId();
    List<ExamMatrix> matrices = examMatrixRepository.findByTeacherIdAndNotDeleted(currentUserId);

    return matrices.stream()
        .map(
            m -> {
              List<ExamMatrixTemplateMapping> mappings =
                  templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(m.getId());
              return buildMatrixResponse(m, mappings);
            })
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void deleteExamMatrix(UUID matrixId) {
    log.info("Deleting exam matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    matrix.setDeletedAt(Instant.now());
    examMatrixRepository.save(matrix);
    log.info("Exam matrix soft-deleted: {}", matrixId);
  }

  // ── Template Mappings ───────────────────────────────────────────────────

  @Override
  @Transactional
  public TemplateMappingResponse addTemplateMapping(
      UUID matrixId, AddTemplateMappingRequest request) {
    log.info(
        "Adding template mapping to matrix {}: templateId={}", matrixId, request.getTemplateId());

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    ExamMatrixTemplateMapping mapping =
        ExamMatrixTemplateMapping.builder()
            .examMatrixId(matrixId)
            .templateId(request.getTemplateId())
            .cognitiveLevel(request.getCognitiveLevel())
            .questionCount(request.getQuestionCount())
            .pointsPerQuestion(request.getPointsPerQuestion())
            .build();

    mapping = templateMappingRepository.save(mapping);
    log.info("Template mapping added with id: {}", mapping.getId());
    return buildMappingResponse(mapping, template.getName());
  }

  @Override
  @Transactional
  public BatchTemplateMappingsResponse addTemplateMappings(
      UUID matrixId, BatchAddTemplateMappingsRequest request) {
    log.info(
        "Adding batch template mappings to matrix {}, count={}",
        matrixId,
        request.getMappings().size());

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    List<TemplateMappingResponse> addedMappings = new ArrayList<>();
    
    for (AddTemplateMappingRequest mappingRequest : request.getMappings()) {
      QuestionTemplate template =
          questionTemplateRepository
              .findByIdWithCreator(mappingRequest.getTemplateId())
              .filter(t -> t.getDeletedAt() == null)
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

      ExamMatrixTemplateMapping mapping =
          ExamMatrixTemplateMapping.builder()
              .examMatrixId(matrixId)
              .templateId(mappingRequest.getTemplateId())
              .cognitiveLevel(mappingRequest.getCognitiveLevel())
              .questionCount(mappingRequest.getQuestionCount())
              .pointsPerQuestion(mappingRequest.getPointsPerQuestion())
              .build();

      mapping = templateMappingRepository.save(mapping);
      addedMappings.add(buildMappingResponse(mapping, template.getName()));
      log.debug("Template mapping added with id: {}", mapping.getId());
    }

    log.info("Batch: {} template mappings added to matrix {}", addedMappings.size(), matrixId);
    
    return BatchTemplateMappingsResponse.builder()
        .totalMappingsAdded(addedMappings.size())
        .addedMappings(addedMappings)
        .message(
            String.format(
                "%d template mappings added successfully to matrix.",
                addedMappings.size()))
        .build();
  }

  @Override
  @Transactional
  public void removeTemplateMapping(UUID matrixId, UUID mappingId) {
    log.info("Removing template mapping {} from matrix {}", mappingId, matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    ExamMatrixTemplateMapping mapping =
        templateMappingRepository
            .findByIdAndExamMatrixId(mappingId, matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    templateMappingRepository.delete(mapping);
    log.info("Template mapping removed: {}", mappingId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TemplateMappingResponse> getTemplateMappings(UUID matrixId) {
    log.info("Getting template mappings for matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);

    return mappings.stream()
        .map(
            m -> {
              String templateName =
                  questionTemplateRepository
                      .findById(m.getTemplateId())
                      .map(QuestionTemplate::getName)
                      .orElse(null);
              return buildMappingResponse(m, templateName);
            })
        .collect(Collectors.toList());
  }

  // ── Validation & Lifecycle ──────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MatrixValidationReport validateMatrix(UUID matrixId) {
    log.info("Validating matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (mappings.isEmpty()) {
      errors.add("Matrix has no template mappings. Add at least one template mapping.");
    }

    // Aggregate totals
    int totalQuestions =
        mappings.stream().mapToInt(ExamMatrixTemplateMapping::getQuestionCount).sum();
    BigDecimal totalPoints =
        mappings.stream()
            .map(
                m ->
                    m.getTotalPoints() != null
                        ? m.getTotalPoints()
                        : m.getPointsPerQuestion()
                            .multiply(BigDecimal.valueOf(m.getQuestionCount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalQuestions == 0) {
      errors.add("Total question count across all mappings must be greater than 0.");
    }
    if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Total points across all mappings must be greater than 0.");
    }

    Integer totalQuestionsTarget = matrix.getTotalQuestionsTarget();
    BigDecimal totalPointsTarget = matrix.getTotalPointsTarget();

    boolean questionsMatchTarget = true;
    if (totalQuestionsTarget != null) {
      if (totalQuestionsTarget <= 0) {
        errors.add("Total questions target must be greater than 0.");
        questionsMatchTarget = false;
      } else {
        questionsMatchTarget = totalQuestions == totalQuestionsTarget;
        if (!questionsMatchTarget) {
          errors.add(
              String.format(
                  "Total questions mismatch: target=%d, actual=%d.",
                  totalQuestionsTarget, totalQuestions));
        }
      }
    } else {
      warnings.add(
          "Matrix does not define totalQuestionsTarget; question-count target matching is skipped.");
    }

    boolean pointsMatchTarget = true;
    if (totalPointsTarget != null) {
      if (totalPointsTarget.compareTo(BigDecimal.ZERO) <= 0) {
        errors.add("Total points target must be greater than 0.");
        pointsMatchTarget = false;
      } else {
        pointsMatchTarget = totalPoints.compareTo(totalPointsTarget) == 0;
        if (!pointsMatchTarget) {
          errors.add(
              String.format(
                  "Total points mismatch: target=%s, actual=%s.",
                  totalPointsTarget.toPlainString(), totalPoints.toPlainString()));
        }
      }
    } else {
      warnings.add(
          "Matrix does not define totalPointsTarget; points target matching is skipped.");
    }

    // Cognitive level coverage
    Map<String, Integer> cognitiveLevelCoverage =
        mappings.stream()
            .collect(
                Collectors.groupingBy(
                    m -> m.getCognitiveLevel().name(),
                    Collectors.summingInt(ExamMatrixTemplateMapping::getQuestionCount)));

    long distinctLevels = cognitiveLevelCoverage.size();
    boolean allCognitiveLevelsCovered = distinctLevels >= 3;
    if (!allCognitiveLevelsCovered) {
      warnings.add(
          String.format(
              "Only %d cognitive level(s) covered (recommended: at least 3).", distinctLevels));
    }

    boolean canApprove = errors.isEmpty();

    return MatrixValidationReport.builder()
        .canApprove(canApprove)
        .errors(errors)
        .warnings(warnings)
        .totalTemplateMappings(mappings.size())
        .totalQuestions(totalQuestions)
        .totalPoints(totalPoints)
        .totalQuestionsTarget(totalQuestionsTarget)
        .totalPointsTarget(totalPointsTarget)
        .cognitiveLevelCoverage(cognitiveLevelCoverage)
        .questionsMatchTarget(questionsMatchTarget)
        .pointsMatchTarget(pointsMatchTarget)
        .allCognitiveLevelsCovered(allCognitiveLevelsCovered)
        .build();
  }

  @Override
  @Transactional
  public ExamMatrixResponse approveMatrix(UUID matrixId) {
    log.info("Approving matrix: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    MatrixValidationReport report = validateMatrix(matrixId);
    if (!report.isCanApprove()) {
      log.error("Matrix validation failed: {}", report.getErrors());
      throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
    }

    matrix.setStatus(MatrixStatus.APPROVED);
    matrix = examMatrixRepository.save(matrix);

    log.info("Matrix approved: {}", matrixId);
    return buildMatrixResponse(
        matrix, templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId));
  }

  @Override
  @Transactional
  public void lockMatrix(UUID matrixId) {
    log.info("Locking matrix: {}", matrixId);

    ExamMatrix matrix =
        examMatrixRepository
            .findByIdAndNotDeleted(matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    if (matrix.getStatus() == MatrixStatus.LOCKED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_LOCKED);
    }
    if (matrix.getStatus() != MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.MATRIX_NOT_APPROVED);
    }

    matrix.setStatus(MatrixStatus.LOCKED);
    examMatrixRepository.save(matrix);
    log.info("Matrix locked: {}", matrixId);
  }

  @Override
  @Transactional
  public ExamMatrixResponse resetMatrix(UUID matrixId) {
    log.info("Resetting matrix to DRAFT: {}", matrixId);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    if (matrix.getStatus() != MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.MATRIX_NOT_APPROVED_FOR_RESET);
    }

    matrix.setStatus(MatrixStatus.DRAFT);
    matrix = examMatrixRepository.save(matrix);

    log.info("Matrix {} reset to DRAFT", matrixId);
    return buildMatrixResponse(
        matrix, templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId));
  }

  // ── Question Generation (template-mapping based) ────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MatchingTemplatesResponse listMatchingTemplates(
      UUID matrixId, String q, int page, int size, boolean onlyMine, boolean publicOnly) {

    log.info(
        "Listing matching templates for matrix {}, q={}, page={}, size={}, onlyMine={}, publicOnly={}",
        matrixId,
        q,
        page,
        size,
        onlyMine,
        publicOnly);

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    UUID currentUserId = getCurrentUserId();
    String searchTerm = (q != null && !q.isBlank()) ? q.trim() : null;

    // Derive aggregate requirements from all mappings: most common cognitiveLevel
    List<ExamMatrixTemplateMapping> mappings =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
    CognitiveLevel dominantLevel =
        mappings.stream()
            .collect(
                Collectors.groupingBy(
                    ExamMatrixTemplateMapping::getCognitiveLevel, Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

    int totalQuestionCount =
        mappings.stream().mapToInt(ExamMatrixTemplateMapping::getQuestionCount).sum();

    // Query templates
    var pageRequest = PageRequest.of(Math.max(page, 0), size > 0 ? size : 20);
    var templatePage =
        questionTemplateRepository.findMatchingTemplatesForCell(
            currentUserId, onlyMine, publicOnly, null, dominantLevel, searchTerm, pageRequest);

    // Score and sort
    List<QuestionTemplate> candidates = new ArrayList<>(templatePage.getContent());
    candidates.sort(
        Comparator.comparingInt(
                (QuestionTemplate t) -> computeRelevanceScore(t, null, dominantLevel))
            .reversed());

    MatchingTemplatesResponse.MappingRequirementsInfo requirementsInfo =
        MatchingTemplatesResponse.MappingRequirementsInfo.builder()
            .matrixId(matrixId)
            .cognitiveLevel(dominantLevel)
            .questionCount(totalQuestionCount)
            .build();

    List<MatchingTemplatesResponse.TemplateItem> items =
        candidates.stream()
            .map(t -> mapToTemplateItem(t, currentUserId, null, dominantLevel))
            .collect(Collectors.toList());

    String hint =
        items.isEmpty()
            ? "No matching templates found. Create a new template or adjust filters."
            : null;

    return MatchingTemplatesResponse.builder()
        .mappingRequirements(requirementsInfo)
        .totalTemplatesFound((int) templatePage.getTotalElements())
        .templates(items)
        .hint(hint)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public PreviewCandidatesResponse generatePreview(
      UUID matrixId, UUID mappingId, GeneratePreviewRequest request) {

    log.info(
        "Generating preview for matrixId={}, mappingId={}, templateId={}, count={}",
        matrixId,
        mappingId,
        request.getTemplateId(),
        request.getCount());

    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());

    ExamMatrixTemplateMapping mapping =
        templateMappingRepository
            .findByIdAndExamMatrixId(mappingId, matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    if (!mapping.getTemplateId().equals(request.getTemplateId())) {
      throw new AppException(ErrorCode.TEMPLATE_MAPPING_TEMPLATE_MISMATCH);
    }

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (template.getStatus() != null && "DRAFT".equalsIgnoreCase(template.getStatus().name())) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    List<String> warnings = new ArrayList<>();

    if (matrix.getStatus() == MatrixStatus.APPROVED || matrix.getStatus() == MatrixStatus.LOCKED) {
      warnings.add(
          String.format(
              "Matrix is currently %s. This preview is for exploration only. "
                  + "Reset the matrix to DRAFT before finalizing.",
              matrix.getStatus()));
    }

    // Difficulty mismatch warning
    QuestionDifficulty requestedDifficulty = request.getDifficulty();
    if (requestedDifficulty != null) {
      warnings.add(
          String.format(
              "Using requested difficulty override '%s' for preview only.", requestedDifficulty));
    }

    int targetCount = request.getCount();
    int maxAttempts = targetCount * 10;
    List<PreviewCandidatesResponse.CandidateQuestion> candidates = new ArrayList<>();
    Set<String> seenTexts = new HashSet<>();

    long baseSeed = request.getSeed() != null ? request.getSeed() : System.currentTimeMillis();
    int attemptsMade = 0;
    int sampleIndex = 0;

    while (candidates.size() < targetCount && attemptsMade < maxAttempts) {
      attemptsMade++;
      try {
        int effectiveIndex = (int) ((baseSeed + sampleIndex) % Integer.MAX_VALUE);
        GeneratedQuestionSample sample =
            aiEnhancementService.generateQuestion(template, effectiveIndex);
        sampleIndex++;

        if (sample == null || sample.getQuestionText() == null) continue;
        if (sample.getQuestionText().startsWith("[LLM generation failed]")) {
          log.warn("LLM generation failed on attempt {}, skipping", attemptsMade);
          continue;
        }

        String textKey = sample.getQuestionText().trim().toLowerCase();
        if (seenTexts.contains(textKey)) continue;
        seenTexts.add(textKey);

        // Difficulty filter — relax after half of max attempts
        if (requestedDifficulty != null
            && sample.getCalculatedDifficulty() != null
            && !requestedDifficulty.equals(sample.getCalculatedDifficulty())) {
          if (attemptsMade < maxAttempts / 2) continue;
          warnings.add(
              String.format(
                  "Could not generate enough '%s' questions; included '%s' candidate(s) to meet count.",
                  requestedDifficulty, sample.getCalculatedDifficulty()));
        }

        candidates.add(
            PreviewCandidatesResponse.CandidateQuestion.builder()
                .index(candidates.size() + 1)
                .questionText(sample.getQuestionText())
                .options(sample.getOptions())
                .correctAnswerKey(sample.getCorrectAnswer())
                .usedParameters(sample.getUsedParameters())
                .answerCalculation(sample.getAnswerCalculation())
                .calculatedDifficulty(sample.getCalculatedDifficulty())
                .explanation(sample.getExplanation())
                .build());

      } catch (Exception e) {
        log.warn("Error generating candidate on attempt {}: {}", attemptsMade, e.getMessage());
      }
    }

    if (candidates.size() < targetCount) {
      warnings.add(
          String.format(
              "Only %d of %d requested questions could be generated. "
                  + "Template parameter ranges may be too narrow.",
              candidates.size(), targetCount));
    }

    String templateName =
        questionTemplateRepository
            .findById(request.getTemplateId())
            .map(QuestionTemplate::getName)
            .orElse(null);

    PreviewCandidatesResponse.MappingInfo mappingInfo =
        PreviewCandidatesResponse.MappingInfo.builder()
            .templateMappingId(mapping.getId())
            .templateId(mapping.getTemplateId())
            .templateName(templateName)
            .cognitiveLevel(mapping.getCognitiveLevel())
            .questionCount(mapping.getQuestionCount())
            .build();

    log.info(
        "Preview generated: {}/{} candidates for mappingId={}",
        candidates.size(),
        targetCount,
        mappingId);

    return PreviewCandidatesResponse.builder()
        .templateId(template.getId())
        .templateName(template.getName())
        .templateMappingId(mappingId)
        .matrixId(matrixId)
        .requestedCount(targetCount)
        .generatedCount(candidates.size())
        .mappingRequirements(mappingInfo)
        .candidates(candidates)
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  @Override
  @Transactional
  public FinalizePreviewResponse finalizePreview(
      UUID matrixId, UUID mappingId, FinalizePreviewRequest request) {

    log.info(
        "Finalizing preview for matrixId={}, mappingId={}, templateId={}, count={}, replaceExisting={}",
        matrixId,
        mappingId,
        request.getTemplateId(),
        request.getQuestions().size(),
        request.getReplaceExisting());

    // ── 1. Validate matrix + ownership ──────────────────────────────────
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    // ── 2. Validate mapping belongs to matrix ────────────────────────────
    ExamMatrixTemplateMapping mapping =
        templateMappingRepository
            .findByIdAndExamMatrixId(mappingId, matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    if (!mapping.getTemplateId().equals(request.getTemplateId())) {
      throw new AppException(ErrorCode.TEMPLATE_MAPPING_TEMPLATE_MISMATCH);
    }

    // ── 3. Validate template ─────────────────────────────────────────────
    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (template.getStatus() != null && "DRAFT".equalsIgnoreCase(template.getStatus().name())) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    UUID currentUserId = getCurrentUserId();
    List<String> warnings = new ArrayList<>();

    // ── 4. Replace existing assessment questions for this mapping if requested ──
    if (Boolean.TRUE.equals(request.getReplaceExisting())) {
      log.info(
          "replaceExisting=true: removing existing assessment questions for mappingId={}",
          mappingId);
      // Find all AssessmentQuestion rows linked to this template mapping and remove them
      // across any assessment that references this matrix
      assessmentRepository
          .findByExamMatrixIdAndNotDeleted(matrixId)
          .forEach(
              a -> {
                List<AssessmentQuestion> existing =
                    assessmentQuestionRepository
                        .findByAssessmentIdOrderByOrderIndex(a.getId())
                        .stream()
                        .filter(aq -> mappingId.equals(aq.getMatrixTemplateMappingId()))
                        .collect(Collectors.toList());
                if (!existing.isEmpty()) {
                  List<UUID> qIds =
                      existing.stream()
                          .map(AssessmentQuestion::getQuestionId)
                          .collect(Collectors.toList());
                  assessmentQuestionRepository.deleteByAssessmentIdAndQuestionIdIn(a.getId(), qIds);
                }
              });
    }

    // ── 5. Per-question validation ───────────────────────────────────────
    Set<String> batchTexts = new HashSet<>();
    List<FinalizePreviewRequest.QuestionItem> validItems = new ArrayList<>();

    for (int i = 0; i < request.getQuestions().size(); i++) {
      FinalizePreviewRequest.QuestionItem item = request.getQuestions().get(i);
      String label = "Question[" + (i + 1) + "]: ";

      if (item.getQuestionText() == null || item.getQuestionText().isBlank()) {
        warnings.add(label + "skipped – questionText is blank.");
        continue;
      }

      String textKey = item.getQuestionText().trim().toLowerCase();
      if (batchTexts.contains(textKey)) {
        warnings.add(label + "skipped – duplicate questionText within this request.");
        continue;
      }

      // MCQ validation
      if (item.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
        Map<String, String> opts = item.getOptions();
        if (opts == null
            || !opts.keySet().containsAll(Set.of("A", "B", "C", "D"))
            || opts.size() != 4) {
          throw new AppException(ErrorCode.MCQ_INVALID_OPTIONS);
        }
        long distinct = opts.values().stream().map(String::trim).distinct().count();
        if (distinct < 4) throw new AppException(ErrorCode.MCQ_INVALID_OPTIONS);
        String ca = item.getCorrectAnswer();
        if (ca == null || !Set.of("A", "B", "C", "D").contains(ca.toUpperCase())) {
          throw new AppException(ErrorCode.MCQ_INVALID_CORRECT_OPTION);
        }
      }

      // Cognitive level mismatch warning
      if (mapping.getCognitiveLevel() != null
          && item.getCognitiveLevel() != null
          && !mapping.getCognitiveLevel().equals(item.getCognitiveLevel())) {
        warnings.add(
            label
                + "cognitiveLevel '"
                + item.getCognitiveLevel()
                + "' differs from mapping cognitiveLevel '"
                + mapping.getCognitiveLevel()
                + "'.");
      }

      batchTexts.add(textKey);
      validItems.add(item);
    }

    if (validItems.isEmpty()) {
      throw new AppException(ErrorCode.FINALIZE_EMPTY_QUESTIONS);
    }

    // ── 6. Overflow check (append mode) ─────────────────────────────────
    if (!Boolean.TRUE.equals(request.getReplaceExisting())) {
      long currentCount =
          assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId).stream()
              .mapToLong(
                  a ->
                      assessmentQuestionRepository.countByAssessmentIdAndMatrixTemplateMappingId(
                          a.getId(), mappingId))
              .max()
              .orElse(0L);
      long afterAdd = currentCount + validItems.size();
      if (afterAdd > mapping.getQuestionCount()) {
        warnings.add(
            String.format(
                "Appending %d question(s) to existing %d will exceed mapping target of %d.",
                validItems.size(), currentCount, mapping.getQuestionCount()));
      }
    }

    // ── 7. Persist questions ─────────────────────────────────────────────
    List<UUID> savedQuestionIds = new ArrayList<>();

    for (int i = 0; i < validItems.size(); i++) {
      FinalizePreviewRequest.QuestionItem item = validItems.get(i);

      Map<String, Object> metadata =
          item.getGenerationMetadata() != null
              ? new HashMap<>(item.getGenerationMetadata())
              : new HashMap<>();
      metadata.put("generatedAt", Instant.now().toString());
      metadata.put("templateId", template.getId().toString());
      metadata.put("templateName", template.getName());
      metadata.put("finalizedBy", currentUserId.toString());

      Map<String, Object> optionsJsonb =
          item.getOptions() != null ? new HashMap<>(item.getOptions()) : null;

      Question question =
          Question.builder()
              .questionBankId(request.getQuestionBankId())
              .createdBy(currentUserId)
              .questionType(item.getQuestionType())
              .questionText(item.getQuestionText())
              .options(optionsJsonb)
              .correctAnswer(item.getCorrectAnswer())
              .explanation(item.getExplanation())
              .points(request.getPointsPerQuestion())
              .difficulty(item.getDifficulty())
              .cognitiveLevel(item.getCognitiveLevel())
              .tags(item.getTags())
              .templateId(template.getId())
              .generationMetadata(metadata)
              .build();

      question = questionRepository.save(question);
      savedQuestionIds.add(question.getId());

      template.incrementUsageCount();
    }

    questionTemplateRepository.save(template);

    // ── 8. Write assessment_questions for all assessments using this matrix ──
    List<Assessment> linkedAssessments =
        assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId);

    for (Assessment assessment : linkedAssessments) {
      Integer maxOrder = assessmentQuestionRepository.findMaxOrderIndex(assessment.getId());
      int orderIndex = (maxOrder != null ? maxOrder : 0) + 1;

      for (UUID questionId : savedQuestionIds) {
        AssessmentQuestion aq =
            AssessmentQuestion.builder()
                .assessmentId(assessment.getId())
                .questionId(questionId)
                .orderIndex(orderIndex++)
                .pointsOverride(request.getPointsPerQuestion())
                .matrixTemplateMappingId(mappingId)
                .build();
        assessmentQuestionRepository.save(aq);
      }
    }

    // ── 9. Count current questions for this mapping ──────────────────────
    long currentMappingQuestionCount =
        linkedAssessments.isEmpty()
            ? savedQuestionIds.size()
            : assessmentQuestionRepository
                .findByAssessmentIdOrderByOrderIndex(linkedAssessments.get(0).getId())
                .stream()
                .filter(aq -> mappingId.equals(aq.getMatrixTemplateMappingId()))
                .count();

    log.info(
        "Finalize complete: {} questions saved for mappingId={}",
        savedQuestionIds.size(),
        mappingId);

    return FinalizePreviewResponse.builder()
        .templateMappingId(mappingId)
        .matrixId(matrixId)
        .templateId(template.getId())
        .requestedCount(request.getQuestions().size())
        .savedCount(savedQuestionIds.size())
        .questionIds(savedQuestionIds)
        .currentMappingQuestionCount((int) currentMappingQuestionCount)
        .mappingTargetCount(mapping.getQuestionCount())
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  // ── Structured Matrix Builder ───────────────────────────────────────────

  @Override
  @Transactional
  public ExamMatrixTableResponse buildMatrix(BuildExamMatrixRequest request) {
    log.info("Building structured exam matrix: name={}", request.getName());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

    // Resolve grade level from curriculum when not provided explicitly
    Integer gradeLevel = request.getGradeLevel();
    if (gradeLevel == null && request.getCurriculumId() != null) {
      gradeLevel =
          curriculumRepository
              .findByIdAndNotDeleted(request.getCurriculumId())
              .map(Curriculum::getGrade)
              .orElse(null);
    }

    ExamMatrix matrix =
        ExamMatrix.builder()
            .teacherId(currentUserId)
            .curriculumId(request.getCurriculumId())
            .gradeLevel(gradeLevel)
            .name(request.getName())
            .description(request.getDescription())
            .isReusable(request.getIsReusable() != null ? request.getIsReusable() : false)
            .totalQuestionsTarget(request.getTotalQuestionsTarget())
            .totalPointsTarget(request.getTotalPointsTarget())
            .status(MatrixStatus.DRAFT)
            .build();

    matrix = examMatrixRepository.save(matrix);
    final UUID matrixId = matrix.getId();

    // Persist rows and their cells
    int globalOrder = 1;
    for (MatrixRowRequest rowSpec : request.getRows()) {
      persistRow(matrixId, rowSpec, globalOrder++);
    }

    log.info("Structured matrix built: id={}", matrixId);
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional(readOnly = true)
  public ExamMatrixTableResponse getMatrixTable(UUID matrixId) {
    log.info("Fetching matrix table: {}", matrixId);
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixTableResponse addMatrixRow(UUID matrixId, MatrixRowRequest rowRequest) {
    log.info("Adding row to matrix: {}", matrixId);
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    List<ExamMatrixRow> existing = examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId);
    int nextOrder = existing.stream()
        .mapToInt(r -> r.getOrderIndex() != null ? r.getOrderIndex() : 0)
        .max().orElse(0) + 1;

    persistRow(matrixId, rowRequest, nextOrder);
    return buildTableResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixTableResponse removeMatrixRow(UUID matrixId, UUID rowId) {
    log.info("Removing row {} from matrix {}", rowId, matrixId);
    ExamMatrix matrix = loadMatrixOrThrow(matrixId);
    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    validateNotApprovedOrLocked(matrix);

    ExamMatrixRow row =
        examMatrixRowRepository
            .findById(rowId)
            .filter(r -> r.getExamMatrixId().equals(matrixId))
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_ROW_NOT_FOUND));

    examMatrixRowRepository.delete(row);
    return buildTableResponse(matrix);
  }

  // ── Private helpers ─────────────────────────────────────────────────────

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return UUID.fromString(jwtAuth.getToken().getSubject());
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private boolean hasRole(String roleName) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    String prefixed = "ROLE_" + roleName;
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(a -> a.equals(roleName) || a.equals(prefixed));
  }

  private void validateTeacherRole(UUID userId) {
    if (!hasRole(PredefinedRole.TEACHER_ROLE) && !hasRole(PredefinedRole.ADMIN_ROLE)) {
      User user =
          userRepository
              .findByIdWithRoles(userId)
              .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
      boolean isTeacher =
          user.getRoles().stream()
              .anyMatch(
                  role ->
                      PredefinedRole.TEACHER_ROLE.equals(role.getName())
                          || PredefinedRole.ADMIN_ROLE.equals(role.getName()));
      if (!isTeacher) {
        throw new AppException(ErrorCode.NOT_A_TEACHER);
      }
    }
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !hasRole(PredefinedRole.ADMIN_ROLE)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }
  }

  private ExamMatrix loadMatrixOrThrow(UUID matrixId) {
    return examMatrixRepository
        .findByIdAndNotDeleted(matrixId)
        .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));
  }

  private void validateNotApprovedOrLocked(ExamMatrix matrix) {
    if (matrix.getStatus() == MatrixStatus.LOCKED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_LOCKED);
    }
    if (matrix.getStatus() == MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_APPROVED);
    }
  }

  private ExamMatrixResponse buildMatrixResponse(
      ExamMatrix matrix, List<ExamMatrixTemplateMapping> mappings) {

    String teacherName =
        userRepository.findById(matrix.getTeacherId()).map(User::getFullName).orElse("Unknown");

    List<TemplateMappingResponse> mappingResponses =
        mappings.stream()
            .map(
                m -> {
                  String templateName =
                      questionTemplateRepository
                          .findById(m.getTemplateId())
                          .map(QuestionTemplate::getName)
                          .orElse(null);
                  return buildMappingResponse(m, templateName);
                })
            .collect(Collectors.toList());

    return ExamMatrixResponse.builder()
        .id(matrix.getId())
        .teacherId(matrix.getTeacherId())
        .teacherName(teacherName)
        .name(matrix.getName())
        .description(matrix.getDescription())
        .isReusable(matrix.getIsReusable())
        .totalQuestionsTarget(matrix.getTotalQuestionsTarget())
        .totalPointsTarget(matrix.getTotalPointsTarget())
        .status(matrix.getStatus())
        .templateMappingCount(mappingResponses.size())
        .templateMappings(mappingResponses)
        .createdAt(matrix.getCreatedAt())
        .updatedAt(matrix.getUpdatedAt())
        .build();
  }

  private TemplateMappingResponse buildMappingResponse(
      ExamMatrixTemplateMapping mapping, String templateName) {

    BigDecimal total =
        mapping.getTotalPoints() != null
            ? mapping.getTotalPoints()
            : mapping
                .getPointsPerQuestion()
                .multiply(BigDecimal.valueOf(mapping.getQuestionCount()));

    return TemplateMappingResponse.builder()
        .id(mapping.getId())
        .examMatrixId(mapping.getExamMatrixId())
        .templateId(mapping.getTemplateId())
        .templateName(templateName)
        .cognitiveLevel(mapping.getCognitiveLevel())
        .questionCount(mapping.getQuestionCount())
        .pointsPerQuestion(mapping.getPointsPerQuestion())
        .totalPoints(total)
        .createdAt(mapping.getCreatedAt())
        .updatedAt(mapping.getUpdatedAt())
        .build();
  }

  private int computeRelevanceScore(
      QuestionTemplate t, QuestionType requiredType, CognitiveLevel requiredLevel) {
    int score = 0;
    if (requiredType != null && requiredType.equals(t.getTemplateType())) score += 40;
    if (requiredLevel != null && requiredLevel.equals(t.getCognitiveLevel())) score += 30;
    if (t.getUsageCount() != null && t.getUsageCount() > 0) {
      score += Math.min(t.getUsageCount() / 10, 10);
    }
    return score;
  }

  private MatchingTemplatesResponse.TemplateItem mapToTemplateItem(
      QuestionTemplate t,
      UUID currentUserId,
      QuestionType requiredType,
      CognitiveLevel requiredLevel) {

    String creatorName = t.getCreator() != null ? t.getCreator().getFullName() : null;

    return MatchingTemplatesResponse.TemplateItem.builder()
        .templateId(t.getId())
        .name(t.getName())
        .description(t.getDescription())
        .templateType(t.getTemplateType())
        .cognitiveLevel(t.getCognitiveLevel())
        .tags(t.getTags())
        .mine(t.getCreatedBy().equals(currentUserId))
        .isPublic(t.getIsPublic())
        .createdBy(t.getCreatedBy())
        .createdByName(creatorName)
        .usageCount(t.getUsageCount())
        .avgSuccessRate(t.getAvgSuccessRate())
        .createdAt(t.getCreatedAt())
        .updatedAt(t.getUpdatedAt())
        .relevanceScore(computeRelevanceScore(t, requiredType, requiredLevel))
        .build();
  }

  /**
   * Persist one {@link ExamMatrixRow} plus all its {@link ExamMatrixTemplateMapping} cells.
   * If {@code rowSpec.templateId} is given the template must exist; the cell templateId
   * will always be that template.  When no templateId is supplied a sentinel (same as row
   * templateId, which may be null) is used — the mapping is still valid for counting purposes.
   */
  private void persistRow(UUID matrixId, MatrixRowRequest rowSpec, int orderIndex) {
    String qTypeName = rowSpec.getQuestionTypeName();

    // If templateId is supplied and questionTypeName is blank, default to template name
    if ((qTypeName == null || qTypeName.isBlank()) && rowSpec.getTemplateId() != null) {
      qTypeName = questionTemplateRepository
          .findById(rowSpec.getTemplateId())
          .map(QuestionTemplate::getName)
          .orElse(null);
    }

    if ((qTypeName == null || qTypeName.isBlank()) && rowSpec.getTemplateId() == null) {
      throw new AppException(ErrorCode.MATRIX_ROW_QUESTION_TYPE_REQUIRED);
    }

    ExamMatrixRow row =
        ExamMatrixRow.builder()
            .examMatrixId(matrixId)
            .chapterId(rowSpec.getChapterId())
            .lessonId(rowSpec.getLessonId())
            .templateId(rowSpec.getTemplateId())
            .questionTypeName(qTypeName)
            .referenceQuestions(rowSpec.getReferenceQuestions())
            .orderIndex(rowSpec.getOrderIndex() != null ? rowSpec.getOrderIndex() : orderIndex)
            .build();

    row = examMatrixRowRepository.save(row);
    final UUID rowId = row.getId();

    // Resolve which templateId to use for the mapping cell — must point to a real template
    // when provided; otherwise use a placeholder UUID approach is avoided: we require
    // templateId for mapping cells.  If row has no templateId the teacher must add templates
    // separately via the legacy addTemplateMapping endpoint.
    if (rowSpec.getTemplateId() != null) {
      // Validate template exists
      questionTemplateRepository
          .findById(rowSpec.getTemplateId())
          .filter(t -> t.getDeletedAt() == null)
          .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

      for (MatrixCellRequest cell : rowSpec.getCells()) {
        ExamMatrixTemplateMapping mapping =
            ExamMatrixTemplateMapping.builder()
                .examMatrixId(matrixId)
                .templateId(rowSpec.getTemplateId())
                .matrixRowId(rowId)
                .cognitiveLevel(cell.getCognitiveLevel())
                .questionCount(cell.getQuestionCount())
                .pointsPerQuestion(cell.getPointsPerQuestion())
                .build();
        templateMappingRepository.save(mapping);
      }
    }
  }

  /**
   * Build the full hierarchical {@link ExamMatrixTableResponse} for a matrix.
   * Groups rows by chapter in order_index order.
   */
  private ExamMatrixTableResponse buildTableResponse(ExamMatrix matrix) {
    UUID matrixId = matrix.getId();

    // Curriculum + subject info
    String curriculumName = null;
    UUID subjectId = null;
    String subjectName = null;

    if (matrix.getCurriculumId() != null) {
      curriculumRepository
          .findByIdAndNotDeleted(matrix.getCurriculumId())
          .ifPresent(
              c -> {
                // intentionally captured via final ref
              });

      var currOpt = curriculumRepository.findByIdAndNotDeleted(matrix.getCurriculumId());
      if (currOpt.isPresent()) {
        Curriculum curr = currOpt.get();
        // set into response fields later via builder
      }
    }

    // Load rows ordered by orderIndex
    List<ExamMatrixRow> rows = examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId);

    // Load all cells for this matrix keyed by rowId
    List<ExamMatrixTemplateMapping> allCells =
        templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId);
    Map<UUID, List<ExamMatrixTemplateMapping>> cellsByRow =
        allCells.stream()
            .filter(c -> c.getMatrixRowId() != null)
            .collect(Collectors.groupingBy(ExamMatrixTemplateMapping::getMatrixRowId));

    // Group rows by chapter
    Map<UUID, List<ExamMatrixRow>> rowsByChapter =
        rows.stream()
            .filter(r -> r.getChapterId() != null)
            .collect(Collectors.groupingBy(ExamMatrixRow::getChapterId, java.util.LinkedHashMap::new, Collectors.toList()));

    // Handle rows without a chapter
    List<ExamMatrixRow> uncategorised =
        rows.stream().filter(r -> r.getChapterId() == null).collect(Collectors.toList());
    if (!uncategorised.isEmpty()) {
      rowsByChapter.put(null, uncategorised);
    }

    // Grand-total accumulators
    Map<String, Integer> grandTotalByCognitive = new LinkedHashMap<>();
    int grandTotalQuestions = 0;
    BigDecimal grandTotalPoints = BigDecimal.ZERO;

    List<MatrixChapterGroupResponse> chapterGroups = new ArrayList<>();

    for (Map.Entry<UUID, List<ExamMatrixRow>> entry : rowsByChapter.entrySet()) {
      UUID chapterId = entry.getKey();
      List<ExamMatrixRow> chapterRows = entry.getValue();

      String chapterTitle = null;
      Integer chapterOrder = null;
      if (chapterId != null) {
        var chapOpt = chapterRepository.findById(chapterId);
        if (chapOpt.isPresent()) {
          chapterTitle = chapOpt.get().getTitle();
          chapterOrder = chapOpt.get().getOrderIndex();
        }
      }

      Map<String, Integer> chapterTotalByCognitive = new LinkedHashMap<>();
      int chapterTotalQ = 0;
      BigDecimal chapterTotalPts = BigDecimal.ZERO;
      List<MatrixRowResponse> rowResponses = new ArrayList<>();

      for (ExamMatrixRow row : chapterRows) {
        List<ExamMatrixTemplateMapping> cells =
            cellsByRow.getOrDefault(row.getId(), Collections.emptyList());

        Map<String, Integer> countByCognitive = new LinkedHashMap<>();
        List<MatrixCellResponse> cellResponses = new ArrayList<>();
        int rowTotalQ = 0;
        BigDecimal rowTotalPts = BigDecimal.ZERO;

        for (ExamMatrixTemplateMapping cell : cells) {
          String label = cognitiveLevelLabel(cell.getCognitiveLevel());
          int count = cell.getQuestionCount();
          BigDecimal pts = cell.getPointsPerQuestion().multiply(BigDecimal.valueOf(count));

          countByCognitive.merge(label, count, Integer::sum);
          rowTotalQ += count;
          rowTotalPts = rowTotalPts.add(pts);

          cellResponses.add(
              MatrixCellResponse.builder()
                  .mappingId(cell.getId())
                  .cognitiveLevel(cell.getCognitiveLevel())
                  .cognitiveLevelLabel(label)
                  .questionCount(count)
                  .pointsPerQuestion(cell.getPointsPerQuestion())
                  .totalPoints(pts)
                  .build());
        }

        rowResponses.add(
            MatrixRowResponse.builder()
                .rowId(row.getId())
                .chapterId(row.getChapterId())
                .lessonId(row.getLessonId())
                .templateId(row.getTemplateId())
                .questionTypeName(row.getQuestionTypeName())
                .referenceQuestions(row.getReferenceQuestions())
                .orderIndex(row.getOrderIndex())
                .cells(cellResponses)
                .countByCognitive(countByCognitive)
                .rowTotalQuestions(rowTotalQ)
                .rowTotalPoints(rowTotalPts)
                .build());

        // Accumulate chapter totals
        countByCognitive.forEach((k, v) -> chapterTotalByCognitive.merge(k, v, Integer::sum));
        chapterTotalQ += rowTotalQ;
        chapterTotalPts = chapterTotalPts.add(rowTotalPts);
      }

      chapterGroups.add(
          MatrixChapterGroupResponse.builder()
              .chapterId(chapterId)
              .chapterTitle(chapterTitle)
              .chapterOrderIndex(chapterOrder)
              .rows(rowResponses)
              .totalByCognitive(chapterTotalByCognitive)
              .chapterTotalQuestions(chapterTotalQ)
              .chapterTotalPoints(chapterTotalPts)
              .build());

      // Accumulate grand totals
      chapterTotalByCognitive.forEach((k, v) -> grandTotalByCognitive.merge(k, v, Integer::sum));
      grandTotalQuestions += chapterTotalQ;
      grandTotalPoints = grandTotalPoints.add(chapterTotalPts);
    }

    // Resolve curriculum info for the response
    String finalCurriculumName = null;
    UUID finalSubjectId = null;
    String finalSubjectName = null;
    if (matrix.getCurriculumId() != null) {
      var currOpt = curriculumRepository.findByIdAndNotDeleted(matrix.getCurriculumId());
      if (currOpt.isPresent()) {
        Curriculum curr = currOpt.get();
        finalCurriculumName = curr.getName();
        if (curr.getSubjectId() != null) {
          finalSubjectId = curr.getSubjectId();
          finalSubjectName = subjectRepository.findById(curr.getSubjectId())
              .map(Subject::getName).orElse(null);
        }
      }
    }

    String teacherName = userRepository.findById(matrix.getTeacherId())
        .map(User::getFullName).orElse("Unknown");

    return ExamMatrixTableResponse.builder()
        .id(matrix.getId())
        .name(matrix.getName())
        .description(matrix.getDescription())
        .teacherId(matrix.getTeacherId())
        .teacherName(teacherName)
        .gradeLevel(matrix.getGradeLevel())
        .curriculumId(matrix.getCurriculumId())
        .curriculumName(finalCurriculumName)
        .subjectId(finalSubjectId)
        .subjectName(finalSubjectName)
        .isReusable(matrix.getIsReusable())
        .status(matrix.getStatus())
        .chapters(chapterGroups)
        .grandTotalByCognitive(grandTotalByCognitive)
        .grandTotalQuestions(grandTotalQuestions)
        .grandTotalPoints(grandTotalPoints)
        .totalQuestionsTarget(matrix.getTotalQuestionsTarget())
        .totalPointsTarget(matrix.getTotalPointsTarget())
        .createdAt(matrix.getCreatedAt())
        .updatedAt(matrix.getUpdatedAt())
        .build();
  }

  /** Maps a {@link CognitiveLevel} to its short Vietnamese label (NB/TH/VD/VDC). */
  private static String cognitiveLevelLabel(CognitiveLevel level) {
    return switch (level) {
      case NHAN_BIET, REMEMBER -> "NB";
      case THONG_HIEU, UNDERSTAND -> "TH";
      case VAN_DUNG, APPLY -> "VD";
      case VAN_DUNG_CAO, ANALYZE, EVALUATE, CREATE -> "VDC";
    };
  }
}
