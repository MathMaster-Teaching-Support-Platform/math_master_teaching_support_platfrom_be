package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.ExamMatrixService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

  @Override
  @Transactional
  public ExamMatrixResponse createExamMatrix(UUID assessmentId, ExamMatrixRequest request) {
    log.info("Creating exam matrix for assessment: {}", assessmentId);

    // Check if matrix already exists
    if (examMatrixRepository.existsByAssessmentId(assessmentId)) {
      throw new AppException(ErrorCode.EXAM_MATRIX_ALREADY_EXISTS);
    }

    // Get and validate assessment
    Assessment assessment = assessmentRepository.findByIdAndNotDeleted(assessmentId)
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
    ExamMatrix matrix = ExamMatrix.builder()
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
  public ExamMatrixResponse configureMatrixDimensions(UUID matrixId, MatrixDimensionRequest request) {
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
    config.put("cognitiveLevels", request.getCognitiveLevels().stream()
      .map(Enum::name).collect(Collectors.toList()));
    config.put("gridSize", request.getChapterIds().size() * request.getCognitiveLevels().size());

    matrix.setMatrixConfig(config);
    matrix = examMatrixRepository.save(matrix);

    log.info("Matrix dimensions configured: {} chapters × {} cognitive levels = {} cells",
      request.getChapterIds().size(), request.getCognitiveLevels().size(),
      request.getChapterIds().size() * request.getCognitiveLevels().size());

    return mapToResponse(matrix);
  }

  @Override
  @Transactional
  public MatrixCellResponse createOrUpdateMatrixCell(UUID matrixId, MatrixCellRequest request) {
    log.info("Creating/updating matrix cell for matrix: {}", matrixId);

    ExamMatrix matrix = getMatrixAndValidateAccess(matrixId);
    validateNotLocked(matrix);

    MatrixCell cell = MatrixCell.builder()
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

    return cells.stream()
      .map(this::mapToCellResponse)
      .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public SuggestedQuestionsResponse suggestQuestionsForCell(UUID matrixCellId, Integer limit) {
    log.info("Suggesting questions for matrix cell: {}, limit: {}", matrixCellId, limit);

    MatrixCell cell = matrixCellRepository.findById(matrixCellId)
      .orElseThrow(() -> new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND));

    ExamMatrix matrix = getMatrixAndValidateAccess(cell.getMatrixId());

    // Get already selected questions for this matrix
    List<UUID> excludedIds = matrixQuestionMappingRepository.findSelectedQuestionIdsByMatrixId(matrix.getId());
    if (excludedIds.isEmpty()) {
      excludedIds = List.of(UUID.randomUUID()); // Dummy UUID to avoid SQL error
    }

    // Find matching questions
    List<Question> questions = questionRepository.findSuggestedQuestions(
      cell.getChapterId(),
      cell.getDifficulty().name(),
      cell.getCognitiveLevel().name(),
      cell.getQuestionType() != null ? cell.getQuestionType().name() : null,
      excludedIds
    );

    int requested = limit != null ? limit : cell.getNumQuestions();
    List<Question> limitedQuestions = questions.stream()
      .limit(requested)
      .collect(Collectors.toList());

    List<SuggestedQuestionsResponse.QuestionResponse> questionResponses = limitedQuestions.stream()
      .map(q -> SuggestedQuestionsResponse.QuestionResponse.builder()
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
    log.info("Manually selecting {} questions for cell: {}",
      request.getQuestionIds().size(), request.getMatrixCellId());

    MatrixCell cell = matrixCellRepository.findById(request.getMatrixCellId())
      .orElseThrow(() -> new AppException(ErrorCode.MATRIX_CELL_NOT_FOUND));

    ExamMatrix matrix = getMatrixAndValidateAccess(cell.getMatrixId());
    validateNotLocked(matrix);

    // Clear existing mappings
    matrixQuestionMappingRepository.deleteByMatrixCellId(cell.getId());

    // Create new mappings
    int priority = 1;
    for (UUID questionId : request.getQuestionIds()) {
      MatrixQuestionMapping mapping = MatrixQuestionMapping.builder()
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
    BigDecimal actualPoints = actualPointsDouble != null
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
        errors.add(String.format("Total questions (%d) is less than target (%d)",
          actualQuestions, matrix.getTotalQuestions()));
      } else {
        warnings.add(String.format("Total questions (%d) exceeds target (%d)",
          actualQuestions, matrix.getTotalQuestions()));
      }
    }

    if (!pointsMatchTarget) {
      if (actualPoints.compareTo(matrix.getTotalPoints()) < 0) {
        warnings.add(String.format("Total points (%.2f) is less than target (%.2f)",
          actualPoints, matrix.getTotalPoints()));
      } else {
        warnings.add(String.format("Total points (%.2f) exceeds target (%.2f)",
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
      warnings.add(String.format("Only %d cognitive levels covered (recommended: at least 3)", distinctLevels));
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

    ExamMatrix matrix = examMatrixRepository.findByIdAndNotDeleted(matrixId)
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

    ExamMatrix matrix = examMatrixRepository.findByAssessmentIdAndNotDeleted(assessmentId)
      .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    Assessment assessment = assessmentRepository.findByIdAndNotDeleted(assessmentId)
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
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUserName(username)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    return user.getId();
  }

  private boolean isAdmin(UUID userId) {
    User user = userRepository.findByIdWithRoles(userId)
      .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    return user.getRoles().stream()
      .anyMatch(role -> role.getName().equals("ADMIN"));
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !isAdmin(currentUserId)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }
  }

  private ExamMatrix getMatrixAndValidateAccess(UUID matrixId) {
    ExamMatrix matrix = examMatrixRepository.findByIdAndNotDeleted(matrixId)
      .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    validateOwnerOrAdmin(matrix.getTeacherId(), getCurrentUserId());
    return matrix;
  }

  private void validateNotLocked(ExamMatrix matrix) {
    if (matrix.getStatus() == MatrixStatus.LOCKED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_LOCKED);
    }
  }

  private Map<String, Double> calculateDifficultyDistribution(UUID matrixId, Integer totalQuestions) {
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
        warnings.add(String.format("Chapter '%s' only %.1f%% coverage (recommended: ≥15%%)",
          entry.getKey(), entry.getValue()));
      }
    }
  }

  private void populateAssessmentQuestionsFromMatrix(ExamMatrix matrix) {
    log.info("Populating assessment questions from matrix: {}", matrix.getId());

    List<UUID> selectedQuestions = matrixQuestionMappingRepository
      .findSelectedQuestionIdsByMatrixId(matrix.getId());

    int orderIndex = 1;
    for (UUID questionId : selectedQuestions) {
      AssessmentQuestion aq = AssessmentQuestion.builder()
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

    String teacherName = userRepository.findById(matrix.getTeacherId())
      .map(User::getFullName)
      .orElse("Unknown");

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
}

