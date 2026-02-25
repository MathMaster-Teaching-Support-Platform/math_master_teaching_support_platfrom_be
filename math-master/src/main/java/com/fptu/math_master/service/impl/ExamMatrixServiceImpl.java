package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.ExamMatrixService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamMatrixServiceImpl implements ExamMatrixService {

  ExamMatrixRepository examMatrixRepository;
  MatrixCellRepository matrixCellRepository;
  MatrixQuestionMappingRepository matrixQuestionMappingRepository;
  AssessmentRepository assessmentRepository;
  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;
  QuestionRepository questionRepository;
  AssessmentQuestionRepository assessmentQuestionRepository;
  UserRepository userRepository;
  QuestionTemplateRepository questionTemplateRepository;
  AIEnhancementService aiEnhancementService;

  @Override
  @Transactional
  public ExamMatrixResponse createExamMatrix(UUID assessmentId, ExamMatrixRequest request) {
    log.info("Creating exam matrix for assessment: {}", assessmentId);

    // Check if matrix already exists
    if (examMatrixRepository.existsByAssessmentId(assessmentId)) {
      throw new AppException(ErrorCode.EXAM_MATRIX_ALREADY_EXISTS);
    }

    // Get and validate assessment
    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    // Check assessment has lesson
    if (assessment.getLessonId() == null) {
      throw new AppException(ErrorCode.ASSESSMENT_MUST_HAVE_LESSON);
    }

    // Check lesson has chapters
    Long chapterCount = chapterRepository.countByLessonIdAndNotDeleted(assessment.getLessonId());
    if (chapterCount == 0) {
      throw new AppException(ErrorCode.LESSON_HAS_NO_CHAPTERS);
    }

    // Create matrix
    ExamMatrix matrix =
        ExamMatrix.builder()
            .assessmentId(assessmentId)
            .lessonId(assessment.getLessonId())
            .teacherId(assessment.getTeacherId())
            .name(request.getName())
            .description(request.getDescription())
            .totalQuestions(request.getTotalQuestions())
            .totalPoints(request.getTotalPoints())
            .timeLimitMinutes(request.getTimeLimitMinutes())
            .matrixConfig(new HashMap<>()) // Initialize empty config
            .status(MatrixStatus.DRAFT)
            .build();

    matrix = examMatrixRepository.save(matrix);

    log.info("Exam matrix created successfully with id: {}", matrix.getId());
    return mapToResponse(matrix);
  }

  @Override
  @Transactional
  public ExamMatrixResponse configureMatrixDimensions(
      UUID matrixId, MatrixDimensionRequest request) {
    log.info("Configuring matrix dimensions for matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    validateNotLocked(matrix);

    // Validate chapters belong to lesson
    List<Chapter> chapters = chapterRepository.findByLessonIdAndNotDeleted(matrix.getLessonId());
    Set<UUID> validChapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toSet());

    for (UUID chapterId : request.getChapterIds()) {
      if (!validChapterIds.contains(chapterId)) {
        throw new AppException(ErrorCode.LESSON_HAS_NO_CHAPTERS);
      }
    }

    // Update matrix config
    Map<String, Object> config = matrix.getMatrixConfig();
    if (config == null) {
      config = new HashMap<>();
    }
    config.put("selectedChapters", request.getChapterIds());
    config.put(
        "cognitiveLevels",
        request.getCognitiveLevels().stream().map(Enum::name).collect(Collectors.toList()));
    config.put("gridSize", request.getChapterIds().size() * request.getCognitiveLevels().size());

    matrix.setMatrixConfig(config);
    matrix = examMatrixRepository.save(matrix);

    log.info(
        "Matrix dimensions configured: {} chapters × {} cognitive levels = {} cells",
        request.getChapterIds().size(),
        request.getCognitiveLevels().size(),
        request.getChapterIds().size() * request.getCognitiveLevels().size());

    return mapToResponse(matrix);
  }

  @Override
  @Transactional
  public MatrixCellResponse createOrUpdateMatrixCell(UUID matrixId, MatrixCellRequest request) {
    log.info("Creating/updating matrix cell for matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    validateNotLocked(matrix);

    MatrixCell cell =
        MatrixCell.builder()
            .matrixId(matrixId)
            .chapterId(request.getChapterId())
            .topic(request.getTopic())
            .cognitiveLevel(request.getCognitiveLevel())
            .difficulty(request.getDifficulty())
            .questionType(request.getQuestionType())
            .numQuestions(request.getNumQuestions())
            .pointsPerQuestion(request.getPointsPerQuestion())
            .notes(request.getNotes())
            .build();

    cell = matrixCellRepository.save(cell);

    log.info("Matrix cell created/updated with id: {}", cell.getId());
    return mapToCellResponse(cell);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MatrixCellResponse> getMatrixCells(UUID matrixId) {
    log.info("Getting matrix cells for matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    List<MatrixCell> cells = matrixCellRepository.findByMatrixIdOrderByCreatedAt(matrixId);

    return cells.stream().map(this::mapToCellResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public SuggestedQuestionsResponse suggestQuestionsForCell(UUID matrixCellId, Integer limit) {
    log.info("Suggesting questions for matrix cell: {}, limit: {}", matrixCellId, limit);

    MatrixCell cell =
        matrixCellRepository
            .findById(matrixCellId)
            .orElseThrow(() -> new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND));

    ExamMatrix matrix = getMatrixAndValidateAccess(cell.getMatrixId());

    // Get already selected questions for this matrix
    List<UUID> excludedIds =
        matrixQuestionMappingRepository.findSelectedQuestionIdsByMatrixId(matrix.getId());
    if (excludedIds.isEmpty()) {
      excludedIds = List.of(UUID.randomUUID()); // Dummy UUID to avoid SQL error
    }

    // Find matching questions
    List<Question> questions =
        questionRepository.findSuggestedQuestions(
            cell.getChapterId(),
            cell.getDifficulty().name(),
            cell.getCognitiveLevel().name(),
            cell.getQuestionType() != null ? cell.getQuestionType().name() : null,
            excludedIds);

    int requested = limit != null ? limit : cell.getNumQuestions();
    List<Question> limitedQuestions =
        questions.stream().limit(requested).collect(Collectors.toList());

    List<SuggestedQuestionsResponse.QuestionResponse> questionResponses =
        limitedQuestions.stream()
            .map(
                q ->
                    SuggestedQuestionsResponse.QuestionResponse.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType())
                        .difficulty(q.getDifficulty())
                        .bloomTaxonomyTags(q.getBloomTaxonomyTags())
                        .points(q.getPoints())
                        .build())
            .collect(Collectors.toList());

    return SuggestedQuestionsResponse.builder()
        .suggestedQuestions(questionResponses)
        .totalAvailable(questions.size())
        .requested(requested)
        .returned(limitedQuestions.size())
        .build();
  }

  @Override
  @Transactional
  public MatrixCellResponse selectQuestionsManually(ManualQuestionSelectionRequest request) {
    log.info(
        "Manually selecting {} questions for cell: {}",
        request.getQuestionIds().size(),
        request.getMatrixCellId());

    MatrixCell cell =
        matrixCellRepository
            .findById(request.getMatrixCellId())
            .orElseThrow(() -> new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND));

    ExamMatrix matrix = getMatrixAndValidateAccess(cell.getMatrixId());
    validateNotLocked(matrix);

    // Clear existing mappings
    matrixQuestionMappingRepository.deleteByMatrixCellId(cell.getId());

    // Create new mappings
    int priority = 1;
    for (UUID questionId : request.getQuestionIds()) {
      MatrixQuestionMapping mapping =
          MatrixQuestionMapping.builder()
              .matrixCellId(cell.getId())
              .questionId(questionId)
              .isSelected(true)
              .selectionPriority(priority++)
              .build();
      matrixQuestionMappingRepository.save(mapping);
    }

    log.info("Successfully mapped {} questions to cell", request.getQuestionIds().size());
    return mapToCellResponse(cell);
  }

  @Override
  @Transactional(readOnly = true)
  public MatrixValidationReport validateMatrix(UUID matrixId) {
    log.info("Validating matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // Get all cells
    List<MatrixCell> cells = matrixCellRepository.findByMatrixIdOrderByCreatedAt(matrixId);

    // Calculate totals
    Integer actualQuestions = matrixCellRepository.sumQuestionsByMatrixId(matrixId);
    if (actualQuestions == null) actualQuestions = 0;

    Double actualPointsDouble = matrixCellRepository.sumPointsByMatrixId(matrixId);
    BigDecimal actualPoints =
        actualPointsDouble != null
            ? BigDecimal.valueOf(actualPointsDouble).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    // Check if all cells are filled
    long filledCells = cells.stream().filter(c -> c.getNumQuestions() > 0).count();
    boolean allCellsFilled = !cells.isEmpty() && filledCells == cells.size();

    // Validate totals
    boolean questionsMatchTarget = actualQuestions.equals(matrix.getTotalQuestions());
    boolean pointsMatchTarget = actualPoints.compareTo(matrix.getTotalPoints()) == 0;

    if (!questionsMatchTarget) {
      if (actualQuestions < matrix.getTotalQuestions()) {
        errors.add(
            String.format(
                "Total questions (%d) is less than target (%d)",
                actualQuestions, matrix.getTotalQuestions()));
      } else {
        warnings.add(
            String.format(
                "Total questions (%d) exceeds target (%d)",
                actualQuestions, matrix.getTotalQuestions()));
      }
    }

    if (!pointsMatchTarget) {
      if (actualPoints.compareTo(matrix.getTotalPoints()) < 0) {
        warnings.add(
            String.format(
                "Total points (%.2f) is less than target (%.2f)",
                actualPoints, matrix.getTotalPoints()));
      } else {
        warnings.add(
            String.format(
                "Total points (%.2f) exceeds target (%.2f)",
                actualPoints, matrix.getTotalPoints()));
      }
    }

    if (!allCellsFilled) {
      warnings.add(String.format("Only %d out of %d cells are filled", filledCells, cells.size()));
    }

    // Difficulty distribution
    Map<String, Double> difficultyDist = calculateDifficultyDistribution(matrixId, actualQuestions);
    boolean difficultyBalanced = validateDifficultyDistribution(difficultyDist, warnings);

    // Cognitive level coverage
    Map<String, Integer> cognitiveDist = calculateCognitiveLevelDistribution(matrixId);
    Long distinctLevels = matrixCellRepository.countDistinctCognitiveLevels(matrixId);
    boolean allCognitiveLevelsCovered = distinctLevels >= 3; // At least 3 levels

    if (!allCognitiveLevelsCovered) {
      warnings.add(
          String.format(
              "Only %d cognitive levels covered (recommended: at least 3)", distinctLevels));
    }

    // Chapter distribution
    Map<String, Double> chapterDist = calculateChapterDistribution(matrixId, actualQuestions);
    validateChapterDistribution(chapterDist, warnings);

    // Can approve?
    boolean canApprove = errors.isEmpty();

    return MatrixValidationReport.builder()
        .canApprove(canApprove)
        .errors(errors)
        .warnings(warnings)
        .actualQuestions(actualQuestions)
        .targetQuestions(matrix.getTotalQuestions())
        .actualPoints(actualPoints)
        .targetPoints(matrix.getTotalPoints())
        .totalCells(cells.size())
        .filledCells((int) filledCells)
        .chapterDistribution(chapterDist)
        .difficultyDistribution(difficultyDist)
        .cognitiveLevelCoverage(cognitiveDist)
        .allCellsFilled(allCellsFilled)
        .questionsMatchTarget(questionsMatchTarget)
        .pointsMatchTarget(pointsMatchTarget)
        .difficultyBalanced(difficultyBalanced)
        .allCognitiveLevelsCovered(allCognitiveLevelsCovered)
        .build();
  }

  @Override
  @Transactional
  public ExamMatrixResponse approveMatrix(UUID matrixId) {
    log.info("Approving matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    validateNotLocked(matrix);

    // Validate first
    MatrixValidationReport report = validateMatrix(matrixId);

    if (!report.isCanApprove()) {
      log.error("Matrix validation failed with errors: {}", report.getErrors());
      throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
    }

    // Update status
    matrix.setStatus(MatrixStatus.APPROVED);
    matrix = examMatrixRepository.save(matrix);

    // Auto-populate assessment_questions from matrix
    populateAssessmentQuestionsFromMatrix(matrix);

    log.info("Matrix approved successfully");
    return mapToResponse(matrix);
  }

  @Override
  @Transactional
  public void lockMatrix(UUID matrixId) {
    log.info("Locking matrix: {}", matrixId);

    ExamMatrix matrix =
        examMatrixRepository
            .findByIdAndNotDeleted(matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    if (matrix.getStatus() != MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.MATRIX_NOT_APPROVED);
    }

    matrix.setStatus(MatrixStatus.LOCKED);
    examMatrixRepository.save(matrix);

    log.info("Matrix locked successfully");
  }

  @Override
  @Transactional(readOnly = true)
  public ExamMatrixResponse getExamMatrixById(UUID matrixId) {
    log.info("Getting exam matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    return mapToResponse(matrix);
  }

  @Override
  @Transactional(readOnly = true)
  public ExamMatrixResponse getExamMatrixByAssessmentId(UUID assessmentId) {
    log.info("Getting exam matrix by assessment: {}", assessmentId);

    ExamMatrix matrix =
        examMatrixRepository
            .findByAssessmentIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    return mapToResponse(matrix);
  }

  @Override
  @Transactional
  public void deleteExamMatrix(UUID matrixId) {
    log.info("Deleting exam matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    validateNotLocked(matrix);

    matrix.setDeletedAt(Instant.now());
    examMatrixRepository.save(matrix);

    log.info("Exam matrix soft deleted successfully");
  }

  // Helper methods

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      String sub = jwtAuth.getToken().getSubject();
      return UUID.fromString(sub);
    }

    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private boolean isAdmin(UUID userId) {
    User user =
        userRepository
            .findByIdWithRoles(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    return user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !isAdmin(currentUserId)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }
  }

  private ExamMatrix getMatrixAndValidateAccess(UUID matrixId) {
    ExamMatrix matrix =
        examMatrixRepository
            .findByIdAndNotDeleted(matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    return matrix;
  }

  private void validateNotLocked(ExamMatrix matrix) {
    if (matrix.getStatus() == MatrixStatus.LOCKED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_LOCKED);
    }
  }

  private Map<String, Double> calculateDifficultyDistribution(
      UUID matrixId, Integer totalQuestions) {
    List<Object[]> results = matrixCellRepository.getDifficultyDistribution(matrixId);
    Map<String, Double> distribution = new HashMap<>();

    for (Object[] result : results) {
      QuestionDifficulty difficulty = (QuestionDifficulty) result[0];
      Long count = (Long) result[1];
      double percentage = totalQuestions > 0 ? (count.doubleValue() / totalQuestions) * 100 : 0;
      distribution.put(difficulty.name(), percentage);
    }

    return distribution;
  }

  private boolean validateDifficultyDistribution(Map<String, Double> dist, List<String> warnings) {
    double easy = dist.getOrDefault("EASY", 0.0);
    double medium = dist.getOrDefault("MEDIUM", 0.0);
    double hard = dist.getOrDefault("HARD", 0.0);

    boolean balanced = true;

    if (easy < 30 || easy > 50) {
      warnings.add(String.format("Easy questions: %.1f%% (recommended: 30-50%%)", easy));
      balanced = false;
    }
    if (medium < 30 || medium > 50) {
      warnings.add(String.format("Medium questions: %.1f%% (recommended: 30-50%%)", medium));
      balanced = false;
    }
    if (hard < 10 || hard > 30) {
      warnings.add(String.format("Hard questions: %.1f%% (recommended: 10-30%%)", hard));
      balanced = false;
    }

    return balanced;
  }

  private Map<String, Integer> calculateCognitiveLevelDistribution(UUID matrixId) {
    List<Object[]> results = matrixCellRepository.getCognitiveLevelDistribution(matrixId);
    Map<String, Integer> distribution = new HashMap<>();

    for (Object[] result : results) {
      CognitiveLevel level = (CognitiveLevel) result[0];
      Long count = (Long) result[1];
      distribution.put(level.name(), count.intValue());
    }

    return distribution;
  }

  private Map<String, Double> calculateChapterDistribution(UUID matrixId, Integer totalQuestions) {
    List<Object[]> results = matrixCellRepository.getChapterDistribution(matrixId);
    Map<String, Double> distribution = new HashMap<>();

    for (Object[] result : results) {
      String chapterTitle = (String) result[0];
      Long count = (Long) result[1];
      double percentage = totalQuestions > 0 ? (count.doubleValue() / totalQuestions) * 100 : 0;
      distribution.put(chapterTitle, percentage);
    }

    return distribution;
  }

  private void validateChapterDistribution(Map<String, Double> dist, List<String> warnings) {
    for (Map.Entry<String, Double> entry : dist.entrySet()) {
      if (entry.getValue() < 15) {
        warnings.add(
            String.format(
                "Chapter '%s' only %.1f%% coverage (recommended: ≥15%%)",
                entry.getKey(), entry.getValue()));
      }
    }
  }

  private void populateAssessmentQuestionsFromMatrix(ExamMatrix matrix) {
    log.info("Populating assessment questions from matrix: {}", matrix.getId());

    List<UUID> selectedQuestions =
        matrixQuestionMappingRepository.findSelectedQuestionIdsByMatrixId(matrix.getId());

    int orderIndex = 1;
    for (UUID questionId : selectedQuestions) {
      AssessmentQuestion aq =
          AssessmentQuestion.builder()
              .assessmentId(matrix.getAssessmentId())
              .questionId(questionId)
              .orderIndex(orderIndex++)
              .build();
      assessmentQuestionRepository.save(aq);
    }

    log.info("Populated {} questions to assessment", selectedQuestions.size());
  }

  private ExamMatrixResponse mapToResponse(ExamMatrix matrix) {
    Long cellCount = examMatrixRepository.countCellsByMatrixId(matrix.getId());
    Long filledCells = examMatrixRepository.countFilledCellsByMatrixId(matrix.getId());
    Long selectedQuestions = examMatrixRepository.countSelectedQuestionsByMatrixId(matrix.getId());

    Assessment assessment = assessmentRepository.findById(matrix.getAssessmentId()).orElse(null);
    String assessmentTitle = assessment != null ? assessment.getTitle() : null;

    Lesson lesson = lessonRepository.findById(matrix.getLessonId()).orElse(null);
    String lessonTitle = lesson != null ? lesson.getTitle() : null;

    String teacherName =
        userRepository.findById(matrix.getTeacherId()).map(User::getFullName).orElse("Unknown");

    return ExamMatrixResponse.builder()
        .id(matrix.getId())
        .assessmentId(matrix.getAssessmentId())
        .assessmentTitle(assessmentTitle)
        .lessonId(matrix.getLessonId())
        .lessonTitle(lessonTitle)
        .teacherId(matrix.getTeacherId())
        .teacherName(teacherName)
        .name(matrix.getName())
        .description(matrix.getDescription())
        .totalQuestions(matrix.getTotalQuestions())
        .totalPoints(matrix.getTotalPoints())
        .timeLimitMinutes(matrix.getTimeLimitMinutes())
        .status(matrix.getStatus())
        .cellCount(cellCount.intValue())
        .filledCells(filledCells.intValue())
        .selectedQuestions(selectedQuestions.intValue())
        .createdAt(matrix.getCreatedAt())
        .updatedAt(matrix.getUpdatedAt())
        .build();
  }

  private MatrixCellResponse mapToCellResponse(MatrixCell cell) {
    Long selectedCount = matrixQuestionMappingRepository.countSelectedByMatrixCellId(cell.getId());

    Chapter chapter = chapterRepository.findById(cell.getChapterId()).orElse(null);
    String chapterTitle = chapter != null ? chapter.getTitle() : null;

    return MatrixCellResponse.builder()
        .id(cell.getId())
        .matrixId(cell.getMatrixId())
        .chapterId(cell.getChapterId())
        .chapterTitle(chapterTitle)
        .topic(cell.getTopic())
        .cognitiveLevel(cell.getCognitiveLevel())
        .difficulty(cell.getDifficulty())
        .questionType(cell.getQuestionType())
        .numQuestions(cell.getNumQuestions())
        .pointsPerQuestion(cell.getPointsPerQuestion())
        .totalPoints(cell.getTotalPoints())
        .selectedQuestionCount(selectedCount.intValue())
        .notes(cell.getNotes())
        .createdAt(cell.getCreatedAt())
        .updatedAt(cell.getUpdatedAt())
        .build();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-EM-NEW: Finalize / Approve Generated Questions for a Matrix Cell
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional   // full write transaction – rollback on any failure
  public FinalizePreviewResponse finalizePreview(
      UUID matrixId, UUID cellId, FinalizePreviewRequest request) {

    log.info("Finalizing preview for matrixId={}, cellId={}, templateId={}, count={}, replaceExisting={}",
        matrixId, cellId, request.getTemplateId(), request.getQuestions().size(), request.getReplaceExisting());

    // ── 1. Validate matrix + ownership ──────────────────────────────────
    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    validateNotLocked(matrix);

    // ── 2. Validate cell belongs to matrix ──────────────────────────────
    MatrixCell cell =
        matrixCellRepository
            .findById(cellId)
            .orElseThrow(() -> new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND));

    if (!cell.getMatrixId().equals(matrixId)) {
      throw new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND);
    }

    // ── 3. Validate template usable ─────────────────────────────────────
    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (template.getStatus() != null && "DRAFT".equalsIgnoreCase(template.getStatus().name())) {
      throw new AppException(ErrorCode.TEMPLATE_NOT_USABLE);
    }

    // ── 4. Resolve assessment ────────────────────────────────────────────
    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(matrix.getAssessmentId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    UUID assessmentId = assessment.getId();
    UUID currentUserId = getCurrentUserId();

    List<String> warnings = new ArrayList<>();

    // ── 5. Per-question validation ───────────────────────────────────────
    // Collect existing question texts in this assessment for duplicate check
    List<String> existingTextsInAssessment =
        assessmentQuestionRepository.findExistingQuestionTextsByAssessmentId(assessmentId);
    Set<String> existingTextSet = existingTextsInAssessment.stream()
        .map(String::trim)
        .map(String::toLowerCase)
        .collect(Collectors.toSet());

    // Track texts seen within this request batch
    Set<String> batchTexts = new HashSet<>();

    List<FinalizePreviewRequest.QuestionItem> validItems = new ArrayList<>();

    for (int i = 0; i < request.getQuestions().size(); i++) {
      FinalizePreviewRequest.QuestionItem item = request.getQuestions().get(i);
      String labelPrefix = "Question[" + (i + 1) + "]: ";

      // 5a. questionText blank (should be caught by @NotBlank, but double-check)
      if (item.getQuestionText() == null || item.getQuestionText().isBlank()) {
        warnings.add(labelPrefix + "skipped – questionText is blank.");
        continue;
      }

      // 5b. Duplicate within batch
      String textKey = item.getQuestionText().trim().toLowerCase();
      if (batchTexts.contains(textKey)) {
        warnings.add(labelPrefix + "skipped – duplicate questionText within this request.");
        continue;
      }

      // 5c. Duplicate in existing assessment (only relevant when replaceExisting=false,
      //     but if replaceExisting=true we cleared old ones so set will be empty for that cell)
      if (existingTextSet.contains(textKey)) {
        warnings.add(labelPrefix + "skipped – question with same text already exists in assessment.");
        continue;
      }

      // 5d. MCQ-specific validation
      if (item.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
        Map<String, String> opts = item.getOptions();
        if (opts == null || !opts.keySet().containsAll(Set.of("A", "B", "C", "D")) || opts.size() != 4) {
          throw new AppException(ErrorCode.MCQ_INVALID_OPTIONS);
        }
        // No duplicate option values
        long distinctValues = opts.values().stream().map(String::trim).distinct().count();
        if (distinctValues < 4) {
          throw new AppException(ErrorCode.MCQ_INVALID_OPTIONS);
        }
        // correctAnswer must be A/B/C/D
        String ca = item.getCorrectAnswer();
        if (ca == null || !Set.of("A", "B", "C", "D").contains(ca.toUpperCase())) {
          throw new AppException(ErrorCode.MCQ_INVALID_CORRECT_OPTION);
        }
      }

      // 5e. Difficulty mismatch warning
      if (cell.getDifficulty() != null && !cell.getDifficulty().equals(item.getDifficulty())) {
        warnings.add(labelPrefix + "difficulty '" + item.getDifficulty()
            + "' differs from cell difficulty '" + cell.getDifficulty() + "'.");
      }

      // 5f. CognitiveLevel mismatch warning
      if (cell.getCognitiveLevel() != null && !cell.getCognitiveLevel().equals(item.getCognitiveLevel())) {
        warnings.add(labelPrefix + "cognitiveLevel '" + item.getCognitiveLevel()
            + "' differs from cell cognitiveLevel '" + cell.getCognitiveLevel() + "'.");
      }

      batchTexts.add(textKey);
      validItems.add(item);
    }

    if (validItems.isEmpty()) {
      throw new AppException(ErrorCode.FINALIZE_EMPTY_QUESTIONS);
    }

    // ── 6. Replace existing if requested ────────────────────────────────
    if (Boolean.TRUE.equals(request.getReplaceExisting())) {
      log.info("replaceExisting=true: removing old mappings for cellId={}", cellId);

      // Find questions currently mapped to this cell
      List<UUID> oldQuestionIds =
          matrixQuestionMappingRepository.findQuestionIdsByMatrixCellId(cellId);

      if (!oldQuestionIds.isEmpty()) {
        // Remove from assessment_questions
        assessmentQuestionRepository.deleteByAssessmentIdAndQuestionIdIn(assessmentId, oldQuestionIds);
        // Detach from matrix cell (delete all mappings for this cell)
        matrixQuestionMappingRepository.deleteByMatrixCellId(cellId);
        // Soft-delete the question records themselves
        for (UUID qId : oldQuestionIds) {
          questionRepository.findById(qId).ifPresent(q -> {
            q.setDeletedAt(Instant.now());
            questionRepository.save(q);
          });
        }
      }

      // Reset existing text set so replaced texts are re-checkable
      existingTextSet.clear();
    } else {
      // append mode: check if adding would exceed cell target
      if (cell.getNumQuestions() != null && cell.getNumQuestions() > 0) {
        long currentCount = matrixQuestionMappingRepository.countByMatrixCellId(cellId);
        long afterAdd = currentCount + validItems.size();
        if (afterAdd > cell.getNumQuestions()) {
          warnings.add(String.format(
              "Appending %d question(s) to existing %d will exceed cell target of %d.",
              validItems.size(), currentCount, cell.getNumQuestions()));
        }
      }
    }

    // ── 7. Determine starting order_index for assessment_questions ───────
    Integer maxOrder = assessmentQuestionRepository.findMaxOrderIndex(assessmentId);
    int nextOrder = (maxOrder == null ? 0 : maxOrder) + 1;

    // ── 8. Persist atomically: questions → assessment_questions → matrix_question_mapping
    List<UUID> savedQuestionIds = new ArrayList<>();
    List<UUID> savedMappingIds = new ArrayList<>();

    for (int i = 0; i < validItems.size(); i++) {
      FinalizePreviewRequest.QuestionItem item = validItems.get(i);

      // Build generation_metadata
      Map<String, Object> metadata = item.getGenerationMetadata() != null
          ? new HashMap<>(item.getGenerationMetadata())
          : new HashMap<>();
      metadata.put("generatedAt", Instant.now().toString());
      metadata.put("templateId", template.getId().toString());
      metadata.put("templateName", template.getName());
      metadata.put("finalizedBy", currentUserId.toString());

      // Build options map (jsonb) — convert Map<String,String> → Map<String,Object>
      Map<String, Object> optionsJsonb = null;
      if (item.getOptions() != null) {
        optionsJsonb = new HashMap<>(item.getOptions());
      }

      // (1) Insert into questions
      Question question = Question.builder()
          .questionBankId(request.getQuestionBankId())   // may be null
          .chapterId(cell.getChapterId())
          .createdBy(currentUserId)
          .questionType(item.getQuestionType())
          .questionText(item.getQuestionText())
          .options(optionsJsonb)
          .correctAnswer(item.getCorrectAnswer())
          .explanation(item.getExplanation())
          .points(request.getPointsPerQuestion())
          .difficulty(item.getDifficulty())
          .cognitiveLevel(item.getCognitiveLevel() != null ? item.getCognitiveLevel().name() : null)
          .bloomTaxonomyTags(item.getTags())
          .tags(item.getTags())
          .templateId(template.getId())
          .generationMetadata(metadata)
          .build();

      question = questionRepository.save(question);
      savedQuestionIds.add(question.getId());

      // (2) Insert into assessment_questions
      AssessmentQuestion aq = AssessmentQuestion.builder()
          .assessmentId(assessmentId)
          .questionId(question.getId())
          .orderIndex(nextOrder + i)
          .pointsOverride(request.getPointsPerQuestion())
          .build();

      assessmentQuestionRepository.save(aq);

      // (3) Insert into matrix_question_mapping
      MatrixQuestionMapping mapping = MatrixQuestionMapping.builder()
          .matrixCellId(cellId)
          .questionId(question.getId())
          .isSelected(true)
          .selectionPriority(i + 1)
          .build();

      mapping = matrixQuestionMappingRepository.save(mapping);
      savedMappingIds.add(mapping.getId());
    }

    // ── 9. Final count check against cell target ─────────────────────────
    long currentCellMappingCount = matrixQuestionMappingRepository.countByMatrixCellId(cellId);
    int cellTarget = cell.getNumQuestions() != null ? cell.getNumQuestions() : 0;

    if (cellTarget > 0 && currentCellMappingCount > cellTarget) {
      warnings.add(String.format(
          "Cell now has %d question(s), which exceeds the target of %d.",
          currentCellMappingCount, cellTarget));
    }

    log.info("Finalize complete: saved {} questions, {} mappings for cellId={}",
        savedQuestionIds.size(), savedMappingIds.size(), cellId);

    return FinalizePreviewResponse.builder()
        .cellId(cellId)
        .matrixId(matrixId)
        .assessmentId(assessmentId)
        .templateId(template.getId())
        .requestedCount(request.getQuestions().size())
        .savedCount(savedQuestionIds.size())
        .questionIds(savedQuestionIds)
        .mappingIds(savedMappingIds)
        .currentCellMappingCount((int) currentCellMappingCount)
        .cellTargetCount(cellTarget)
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-EM-NEW: Generate Preview Questions for a Matrix Cell (NO DB WRITE)
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)   // readOnly = true guarantees no flush/writes
  public PreviewCandidatesResponse generatePreview(
      UUID matrixId, UUID cellId, GeneratePreviewRequest request) {

    log.info("Generating preview for matrixId={}, cellId={}, templateId={}, count={}",
        matrixId, cellId, request.getTemplateId(), request.getCount());

    // ── 1. Validate matrix + access ─────────────────────────────────────
    getMatrixAndValidateAccess(matrixId);

    // ── 2. Validate cell belongs to matrix ──────────────────────────────
    MatrixCell cell =
        matrixCellRepository
            .findById(cellId)
            .orElseThrow(() -> new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND));

    if (!cell.getMatrixId().equals(matrixId)) {
      throw new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND);
    }

    // ── 3. Validate template usable ─────────────────────────────────────
    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreator(request.getTemplateId())
            .filter(t -> t.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    // Template must not be soft-deleted (already guaranteed above).
    // DRAFT templates may not be used for generation.
    if (template.getStatus() != null) {
      // Only PUBLISHED / ACTIVE allowed (treat DRAFT as unusable)
      String statusName = template.getStatus().name();
      if ("DRAFT".equalsIgnoreCase(statusName)) {
        throw new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND);
      }
    }

    List<String> warnings = new ArrayList<>();

    // ── 4. Cross-validate template against cell requirements ─────────────
    // 4a. Question type check
    if (cell.getQuestionType() != null
        && !cell.getQuestionType().equals(template.getTemplateType())) {
      warnings.add(String.format(
          "Template type '%s' does not match cell question type '%s'. "
              + "Generated questions may not satisfy the cell constraint.",
          template.getTemplateType(), cell.getQuestionType()));
    }

    // 4b. Difficulty override vs. cell fixed difficulty
    QuestionDifficulty requestedDifficulty = request.getDifficulty();
    if (requestedDifficulty != null
        && cell.getDifficulty() != null
        && !requestedDifficulty.equals(cell.getDifficulty())) {
      warnings.add(String.format(
          "Requested difficulty '%s' differs from cell's fixed difficulty '%s'. "
              + "Using requested difficulty for preview only.",
          requestedDifficulty, cell.getDifficulty()));
    }

    // 4c. Warn if count differs significantly from cell.numQuestions
    int targetCount = request.getCount();
    if (cell.getNumQuestions() != null && cell.getNumQuestions() > 0) {
      int diff = Math.abs(targetCount - cell.getNumQuestions());
      if (diff > cell.getNumQuestions()) {
        warnings.add(String.format(
            "Requested count (%d) differs significantly from cell target (%d). "
                + "Preview is for exploration only.",
            targetCount, cell.getNumQuestions()));
      }
    }

    // ── 5. Build cell info for UI ────────────────────────────────────────
    Chapter chapter = cell.getChapterId() != null
        ? chapterRepository.findById(cell.getChapterId()).orElse(null)
        : null;

    PreviewCandidatesResponse.CellInfo cellInfo =
        PreviewCandidatesResponse.CellInfo.builder()
            .cellId(cell.getId())
            .chapterId(cell.getChapterId())
            .chapterTitle(chapter != null ? chapter.getTitle() : null)
            .topic(cell.getTopic())
            .cognitiveLevel(cell.getCognitiveLevel())
            .difficulty(cell.getDifficulty())
            .questionType(cell.getQuestionType())
            .numQuestions(cell.getNumQuestions())
            .build();

    // ── 6. Generate candidates in-memory ────────────────────────────────
    int maxAttempts = targetCount * 10;
    List<PreviewCandidatesResponse.CandidateQuestion> candidates = new ArrayList<>();
    Set<String> seenQuestionTexts = new HashSet<>(); // uniqueness guard within batch

    long baseSeed = request.getSeed() != null ? request.getSeed() : System.currentTimeMillis();
    int attemptsMade = 0;
    int sampleIndex = 0;

    while (candidates.size() < targetCount && attemptsMade < maxAttempts) {
      attemptsMade++;
      try {
        // Use AIEnhancementService which does Java-computed params + optional LLM wording.
        // We pass a modified sampleIndex that incorporates seed for reproducibility.
        int effectiveIndex = (int) ((baseSeed + sampleIndex) % Integer.MAX_VALUE);
        GeneratedQuestionSample sample =
            aiEnhancementService.generateQuestion(template, effectiveIndex);
        sampleIndex++;

        if (sample == null || sample.getQuestionText() == null) continue;
        if (sample.getQuestionText().startsWith("[LLM generation failed]")) {
          log.warn("LLM generation failed for attempt {}, skipping", attemptsMade);
          continue;
        }

        // Uniqueness check within this preview batch
        String textKey = sample.getQuestionText().trim().toLowerCase();
        if (seenQuestionTexts.contains(textKey)) continue;
        seenQuestionTexts.add(textKey);

        // Difficulty filter: if an override difficulty is requested, skip mismatches
        if (requestedDifficulty != null
            && sample.getCalculatedDifficulty() != null
            && !requestedDifficulty.equals(sample.getCalculatedDifficulty())) {
          // Skip candidates that don't match the requested difficulty.
          // After many failed attempts we'll relax this constraint.
          if (attemptsMade < maxAttempts / 2) continue;
          warnings.add(String.format(
              "Could not generate enough questions with difficulty '%s'; "
                  + "included '%s' question(s) to meet count.",
              requestedDifficulty, sample.getCalculatedDifficulty()));
        }

        candidates.add(PreviewCandidatesResponse.CandidateQuestion.builder()
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
        log.warn("Error generating preview candidate (attempt {}): {}", attemptsMade, e.getMessage());
      }
    }

    // ── 7. Partial result warnings ───────────────────────────────────────
    if (candidates.size() < targetCount) {
      warnings.add(String.format(
          "Only %d of %d requested questions could be generated. "
              + "Template constraints may be too strict or parameter ranges too narrow.",
          candidates.size(), targetCount));
    }

    log.info("Preview generated: {}/{} candidates for cellId={}", candidates.size(), targetCount, cellId);

    return PreviewCandidatesResponse.builder()
        .templateId(template.getId())
        .templateName(template.getName())
        .cellId(cellId)
        .matrixId(matrixId)
        .requestedCount(targetCount)
        .generatedCount(candidates.size())
        .cellRequirements(cellInfo)
        .candidates(candidates)
        .warnings(warnings.isEmpty() ? null : warnings)
        .build();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // FR-EM-NEW: List Matching Question Templates for a Matrix Cell
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public MatchingTemplatesResponse listMatchingTemplatesForCell(
      UUID matrixId,
      UUID cellId,
      String q,
      int page,
      int size,
      boolean onlyMine,
      boolean publicOnly) {

    log.info(
        "Listing matching templates for matrixId={}, cellId={}, q={}, page={}, size={}, onlyMine={}, publicOnly={}",
        matrixId, cellId, q, page, size, onlyMine, publicOnly);

    // 1. Validate & authorise matrix (throws 404/403 if invalid)
    getMatrixAndValidateAccess(matrixId);

    // 2. Validate cell exists and belongs to this matrix
    MatrixCell cell =
        matrixCellRepository
            .findById(cellId)
            .orElseThrow(() -> new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND));

    if (!cell.getMatrixId().equals(matrixId)) {
      throw new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND);
    }

    UUID currentUserId = getCurrentUserId();

    // 3. Derive cell requirements
    QuestionType requiredType = cell.getQuestionType();   // may be null → no filter
    CognitiveLevel requiredLevel = cell.getCognitiveLevel();

    // 4. Build chapter tags for tag-matching
    Set<String> chapterTags = buildChapterTags(cell);

    // 5. Query candidates with primary filters (type, cognitiveLevel, soft-delete, visibility)
    List<QuestionTemplate> candidates =
        questionTemplateRepository.findCandidateTemplates(
            currentUserId, requiredType, requiredLevel);

    // 6. Apply search term filter (name or tags)
    String searchTerm = (q != null && !q.isBlank()) ? q.trim().toLowerCase() : null;
    if (searchTerm != null) {
      candidates = candidates.stream()
          .filter(t -> matchesSearchTerm(t, searchTerm))
          .collect(Collectors.toList());
    }

    // 7. Apply onlyMine / publicOnly filter
    if (onlyMine) {
      candidates = candidates.stream()
          .filter(t -> t.getCreatedBy().equals(currentUserId))
          .collect(Collectors.toList());
    }
    if (publicOnly) {
      candidates = candidates.stream()
          .filter(t -> Boolean.TRUE.equals(t.getIsPublic()))
          .collect(Collectors.toList());
    }

    // 8. Score & rank
    candidates.sort(
        Comparator.comparingInt(
                (QuestionTemplate t) -> computeRelevanceScore(t, requiredType, requiredLevel, chapterTags))
            .reversed());

    int total = candidates.size();

    // 9. Paginate in memory (candidates are already filtered/ranked)
    int pageSize = size > 0 ? size : 20;
    int pageNum = page >= 0 ? page : 0;
    int fromIdx = pageNum * pageSize;
    int toIdx = Math.min(fromIdx + pageSize, total);
    List<QuestionTemplate> pageSlice =
        fromIdx >= total ? Collections.emptyList() : candidates.subList(fromIdx, toIdx);

    // 10. Build cell requirements summary
    Chapter chapter =
        cell.getChapterId() != null
            ? chapterRepository.findById(cell.getChapterId()).orElse(null)
            : null;
    String chapterTitle = chapter != null ? chapter.getTitle() : null;

    MatchingTemplatesResponse.CellRequirementsInfo cellReq =
        MatchingTemplatesResponse.CellRequirementsInfo.builder()
            .cellId(cell.getId())
            .matrixId(matrixId)
            .chapterId(cell.getChapterId())
            .chapterTitle(chapterTitle)
            .topic(cell.getTopic())
            .cognitiveLevel(cell.getCognitiveLevel())
            .difficulty(cell.getDifficulty())
            .questionType(cell.getQuestionType())
            .numQuestions(cell.getNumQuestions())
            .build();

    // 11. Map to response items
    List<MatchingTemplatesResponse.TemplateItem> items =
        pageSlice.stream()
            .map(
                t ->
                    mapToTemplateItem(
                        t,
                        currentUserId,
                        requiredType,
                        requiredLevel,
                        chapterTags))
            .collect(Collectors.toList());

    // 12. Build hint
    String hint =
        total == 0
            ? "No matching templates found. You can create a new template or loosen filters."
            : null;

    return MatchingTemplatesResponse.builder()
        .cellRequirements(cellReq)
        .totalTemplatesFound(total)
        .templates(items)
        .hint(hint)
        .build();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Private helpers for template matching
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Builds a set of lowercase tag strings derived from the cell's chapter title and topic,
   * used for overlapping against template tags.
   */
  private Set<String> buildChapterTags(MatrixCell cell) {
    Set<String> tags = new HashSet<>();

    if (cell.getChapterId() != null) {
      chapterRepository
          .findById(cell.getChapterId())
          .ifPresent(
              ch -> {
                if (ch.getTitle() != null) {
                  // Add each word of the chapter title as a tag candidate
                  Arrays.stream(ch.getTitle().toLowerCase().split("\\s+"))
                      .filter(w -> w.length() > 2)
                      .forEach(tags::add);
                  tags.add(ch.getTitle().toLowerCase());
                }
              });
    }

    if (cell.getTopic() != null && !cell.getTopic().isBlank()) {
      Arrays.stream(cell.getTopic().toLowerCase().split("\\s+"))
          .filter(w -> w.length() > 2)
          .forEach(tags::add);
      tags.add(cell.getTopic().toLowerCase());
    }

    return tags;
  }

  /**
   * Computes a relevance score for a template against cell requirements.
   * Scoring breakdown:
   * <ul>
   *   <li>+40  – templateType exact match (when cell constrains question type)</li>
   *   <li>+30  – cognitiveLevel exact match</li>
   *   <li>+20  – at least one tag overlaps with chapter/topic tags</li>
   *   <li>+10 extra per additional overlapping tag (capped at +30)</li>
   *   <li>+5   – template is owned by current user (mine-first preference)</li>
   *   <li>+usage_count / 10 (capped at +10) – popularity boost</li>
   * </ul>
   */
  private int computeRelevanceScore(
      QuestionTemplate t,
      QuestionType requiredType,
      CognitiveLevel requiredLevel,
      Set<String> chapterTags) {

    int score = 0;

    // Type match
    if (requiredType != null && requiredType.equals(t.getTemplateType())) {
      score += 40;
    }

    // Cognitive level match
    if (requiredLevel != null && requiredLevel.equals(t.getCognitiveLevel())) {
      score += 30;
    }

    // Tag overlap
    if (t.getTags() != null && t.getTags().length > 0 && !chapterTags.isEmpty()) {
      long overlaps =
          Arrays.stream(t.getTags())
              .filter(Objects::nonNull)
              .map(String::toLowerCase)
              .filter(chapterTags::contains)
              .count();
      if (overlaps > 0) {
        score += 20;
        score += (int) Math.min(overlaps - 1, 3) * 10; // +10 each extra tag, cap at +30
      }
    }

    // Popularity boost (capped)
    if (t.getUsageCount() != null && t.getUsageCount() > 0) {
      score += Math.min(t.getUsageCount() / 10, 10);
    }

    return score;
  }

  /** Returns true when the template name or any of its tags contains the search term. */
  private boolean matchesSearchTerm(QuestionTemplate t, String lowerTerm) {
    if (t.getName() != null && t.getName().toLowerCase().contains(lowerTerm)) {
      return true;
    }
    if (t.getTags() != null) {
      return Arrays.stream(t.getTags())
          .filter(Objects::nonNull)
          .anyMatch(tag -> tag.toLowerCase().contains(lowerTerm));
    }
    return false;
  }

  /** Maps a QuestionTemplate entity to a lightweight TemplateItem DTO. */
  private MatchingTemplatesResponse.TemplateItem mapToTemplateItem(
      QuestionTemplate t,
      UUID currentUserId,
      QuestionType requiredType,
      CognitiveLevel requiredLevel,
      Set<String> chapterTags) {

    String creatorName =
        t.getCreator() != null ? t.getCreator().getFullName() : null;

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
        .relevanceScore(computeRelevanceScore(t, requiredType, requiredLevel, chapterTags))
        .build();
  }
}




