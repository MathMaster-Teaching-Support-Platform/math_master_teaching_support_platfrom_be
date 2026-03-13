package com.fptu.math_master.service.impl;

import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AddQuestionToAssessmentRequest;
import com.fptu.math_master.dto.request.AssessmentRequest;
import com.fptu.math_master.dto.request.CloneAssessmentRequest;
import com.fptu.math_master.dto.request.GenerateAssessmentQuestionsRequest;
import com.fptu.math_master.dto.request.PointsOverrideRequest;
import com.fptu.math_master.dto.response.AssessmentGenerationResponse;
import com.fptu.math_master.dto.response.AssessmentResponse;
import com.fptu.math_master.dto.response.AssessmentSummary;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentLesson;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AttemptScoringPolicy;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.AssessmentService;
import com.fptu.math_master.service.ExamMatrixService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssessmentServiceImpl implements AssessmentService {

  AssessmentRepository assessmentRepository;
  AssessmentLessonRepository assessmentLessonRepository;
  AssessmentQuestionRepository assessmentQuestionRepository;
  UserRepository userRepository;
  ExamMatrixRepository examMatrixRepository;
  ExamMatrixTemplateMappingRepository examMatrixTemplateMappingRepository;
  LessonRepository lessonRepository;
  ExamMatrixService examMatrixService;
  QuestionRepository questionRepository;

  @Override
  @Transactional
  public AssessmentResponse createAssessment(AssessmentRequest request) {
    log.info("Creating assessment: {}", request.getTitle());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);
    validateDates(request.getStartDate(), request.getEndDate());
    ExamMatrix matrix = validateAndGetAccessibleMatrix(request.getExamMatrixId(), currentUserId);
    List<UUID> lessonIds = normalizeLessonIds(request.getLessonIds());
    validateLessonSelectionForMatrix(matrix.getId(), lessonIds);

    Assessment assessment =
        Assessment.builder()
            .teacherId(currentUserId)
            .title(request.getTitle())
            .description(request.getDescription())
            .assessmentType(request.getAssessmentType())
            .timeLimitMinutes(request.getTimeLimitMinutes())
            .passingScore(request.getPassingScore())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .randomizeQuestions(
                request.getRandomizeQuestions() != null ? request.getRandomizeQuestions() : false)
            .showCorrectAnswers(
                request.getShowCorrectAnswers() != null ? request.getShowCorrectAnswers() : false)
            .assessmentMode(request.getAssessmentMode())
            .examMatrixId(matrix.getId())
            .allowMultipleAttempts(
                request.getAllowMultipleAttempts() != null
                    ? request.getAllowMultipleAttempts()
                    : false)
            .maxAttempts(request.getMaxAttempts())
            .attemptScoringPolicy(
                request.getAttemptScoringPolicy() != null
                    ? request.getAttemptScoringPolicy()
                    : AttemptScoringPolicy.BEST)
            .showScoreImmediately(
                request.getShowScoreImmediately() != null
                    ? request.getShowScoreImmediately()
                    : true)
            .status(AssessmentStatus.DRAFT)
            .build();

    assessment = assessmentRepository.save(assessment);
    syncAssessmentLessons(assessment.getId(), lessonIds);
    log.info("Assessment created successfully with id: {}", assessment.getId());
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse updateAssessment(UUID id, AssessmentRequest request) {
    log.info("Updating assessment with id: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(assessment.getTeacherId(), currentUserId);

    if (assessment.getStatus().isTerminal()) {
      throw new AppException(ErrorCode.ASSESSMENT_IS_CLOSED);
    }
    if (assessment.getStatus() == AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    validateDates(request.getStartDate(), request.getEndDate());
    ExamMatrix matrix = validateAndGetAccessibleMatrix(request.getExamMatrixId(), currentUserId);
    List<UUID> lessonIds = normalizeLessonIds(request.getLessonIds());
    validateLessonSelectionForMatrix(matrix.getId(), lessonIds);

    assessment.setTitle(request.getTitle());
    assessment.setDescription(request.getDescription());
    assessment.setAssessmentType(request.getAssessmentType());
    assessment.setTimeLimitMinutes(request.getTimeLimitMinutes());
    assessment.setPassingScore(request.getPassingScore());
    assessment.setStartDate(request.getStartDate());
    assessment.setEndDate(request.getEndDate());

    if (request.getRandomizeQuestions() != null) {
      assessment.setRandomizeQuestions(request.getRandomizeQuestions());
    }
    if (request.getShowCorrectAnswers() != null) {
      assessment.setShowCorrectAnswers(request.getShowCorrectAnswers());
    }
    if (request.getAssessmentMode() != null) {
      assessment.setAssessmentMode(request.getAssessmentMode());
    }
    assessment.setExamMatrixId(matrix.getId());
    if (request.getAllowMultipleAttempts() != null) {
      assessment.setAllowMultipleAttempts(request.getAllowMultipleAttempts());
    }
    if (request.getMaxAttempts() != null) {
      assessment.setMaxAttempts(request.getMaxAttempts());
    }
    if (request.getAttemptScoringPolicy() != null) {
      assessment.setAttemptScoringPolicy(request.getAttemptScoringPolicy());
    }
    if (request.getShowScoreImmediately() != null) {
      assessment.setShowScoreImmediately(request.getShowScoreImmediately());
    }

    assessment = assessmentRepository.save(assessment);
    syncAssessmentLessons(assessment.getId(), lessonIds);
    log.info("Assessment updated successfully: {}", id);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse setPointsOverride(UUID assessmentId, PointsOverrideRequest request) {
    log.info(
        "Setting points override for assessment: {}, question: {}",
        assessmentId,
        request.getQuestionId());

    Assessment assessment = loadAssessmentOrThrow(assessmentId);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() == AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    AssessmentQuestion aq =
        assessmentQuestionRepository
            .findByAssessmentIdAndQuestionId(assessmentId, request.getQuestionId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_QUESTION_NOT_FOUND));

    aq.setPointsOverride(request.getPointsOverride());
    assessmentQuestionRepository.save(aq);

    // Only process matrix if Assessment has an exam matrix linked
    if (assessment.getExamMatrixId() != null) {
      examMatrixRepository
          .findByIdAndNotDeleted(assessment.getExamMatrixId())
          .ifPresent(
              matrix -> {
                if (matrix.getStatus() == MatrixStatus.APPROVED) {
                  matrix.setStatus(MatrixStatus.DRAFT);
                  examMatrixRepository.save(matrix);
                  log.warn(
                      "Matrix {} demoted to DRAFT because points override invalidates its cached distribution",
                      matrix.getId());
                }
              });
    }

    log.info("Points override set successfully");
    return mapToResponse(assessment);
  }

  @Override
  @Transactional(readOnly = true)
  public AssessmentResponse getAssessmentPreview(UUID id) {
    log.info("Getting assessment preview: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());
    return mapToResponse(assessment);
  }

  @Override
  @Transactional(readOnly = true)
  public AssessmentSummary getPublishSummary(UUID id) {
    log.info("Getting publish summary for assessment: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(id);
    BigDecimal totalPoints = safeTotalPoints(assessmentRepository.calculateTotalPoints(id));

    boolean canPublish = true;
    String validationMessage = "";

    if (totalQuestions == 0) {
      canPublish = false;
      validationMessage = "Assessment must have at least one question";
    } else if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
      canPublish = false;
      validationMessage = "Total points must be greater than 0";
    } else if (assessment.getStartDate() != null
        && assessment.getStartDate().isBefore(Instant.now())) {
      canPublish = false;
      validationMessage = "Start date cannot be in the past";
    }

    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    return AssessmentSummary.builder()
        .totalQuestions(totalQuestions)
        .totalPoints(totalPoints)
        .timeLimitMinutes(assessment.getTimeLimitMinutes())
        .startDate(
            assessment.getStartDate() != null ? formatter.format(assessment.getStartDate()) : null)
        .endDate(assessment.getEndDate() != null ? formatter.format(assessment.getEndDate()) : null)
        .hasSchedule(assessment.getStartDate() != null || assessment.getEndDate() != null)
        .canPublish(canPublish)
        .validationMessage(validationMessage)
        .build();
  }

  @Override
  @Transactional
  public AssessmentResponse publishAssessment(UUID id) {
    log.info("Publishing assessment: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() == AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }
    if (assessment.getStatus().isTerminal()) {
      throw new AppException(ErrorCode.ASSESSMENT_IS_CLOSED);
    }

    Instant now = Instant.now();

    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(id);
    if (totalQuestions == 0) {
      throw new AppException(ErrorCode.ASSESSMENT_NO_QUESTIONS);
    }

    BigDecimal totalPoints = safeTotalPoints(assessmentRepository.calculateTotalPoints(id));
    if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
      throw new AppException(ErrorCode.ASSESSMENT_ZERO_TOTAL_POINTS);
    }

    if (assessment.getStartDate() != null && assessment.getStartDate().isBefore(now)) {
      throw new AppException(ErrorCode.ASSESSMENT_START_DATE_PAST);
    }

    if (assessment.getExamMatrixId() != null) {
      ExamMatrix matrix =
          examMatrixRepository
              .findByIdAndNotDeleted(assessment.getExamMatrixId())
              .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

      if (matrix.getStatus() != MatrixStatus.APPROVED) {
        throw new AppException(ErrorCode.MATRIX_NOT_APPROVED);
      }

      // TODO: Implement proper question count validation for ExamMatrixTemplateMapping architecture
      // For now, matrix locking logic is deferred
      examMatrixService.lockMatrix(matrix.getId());
    }

    assessment.setStatus(AssessmentStatus.PUBLISHED);
    assessment = assessmentRepository.save(assessment);
    log.info("Assessment published successfully: {}", id);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse unpublishAssessment(UUID id) {
    log.info("Unpublishing assessment: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus().isTerminal()) {
      throw new AppException(ErrorCode.ASSESSMENT_IS_CLOSED);
    }
    if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_PUBLISHED);
    }

    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(id);
    if (submissionCount > 0) {
      throw new AppException(ErrorCode.ASSESSMENT_HAS_SUBMISSIONS);
    }

    assessment.setStatus(AssessmentStatus.DRAFT);
    assessmentRepository.save(assessment);

    if (assessment.getExamMatrixId() != null) {
      examMatrixRepository
          .findByIdAndNotDeleted(assessment.getExamMatrixId())
          .ifPresent(
              matrix -> {
                if (matrix.getStatus() == MatrixStatus.LOCKED) {
                  matrix.setStatus(MatrixStatus.APPROVED);
                  examMatrixRepository.save(matrix);
                  log.info(
                      "Matrix {} reset from LOCKED to APPROVED after assessment unpublish",
                      matrix.getId());
                }
              });
    }

    log.info("Assessment unpublished successfully: {}", id);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public void deleteAssessment(UUID id) {
    log.info("Deleting assessment: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus().isTerminal()) {
      throw new AppException(ErrorCode.ASSESSMENT_IS_CLOSED);
    }
    if (assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(id);
    if (submissionCount > 0) {
      throw new AppException(ErrorCode.ASSESSMENT_HAS_SUBMISSIONS);
    }

    Instant now = Instant.now();
    assessment.setDeletedAt(now);
    assessmentRepository.save(assessment);

    examMatrixRepository
        .findByAssessmentIdAndNotDeleted(id)
        .ifPresent(
            matrix -> {
              matrix.setDeletedAt(now);
              examMatrixRepository.save(matrix);
              log.info(
                  "Soft-deleted orphaned ExamMatrix {} along with Assessment {}",
                  matrix.getId(),
                  id);
            });

    log.info("Assessment soft deleted successfully: {}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public AssessmentResponse getAssessmentById(UUID id) {
    log.info("Getting assessment: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    UUID currentUserId = getCurrentUserId();

    boolean isOwnerOrAdmin =
        assessment.getTeacherId().equals(currentUserId) || hasRole(PredefinedRole.ADMIN_ROLE);
    boolean isPublishedAndAnyUser =
        assessment.getStatus() == AssessmentStatus.PUBLISHED && !isOwnerOrAdmin;

    if (!isOwnerOrAdmin && !isPublishedAndAnyUser) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }

    return mapToResponse(assessment);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AssessmentResponse> getMyAssessments(AssessmentStatus status, Pageable pageable) {

    UUID currentUserId = getCurrentUserId();
    Page<Assessment> assessmentsPage =
        assessmentRepository.findWithFilters(currentUserId, status, pageable);

    // FIX: eliminate N+1 — fetch bulk summary for all IDs on this page in one query
    List<UUID> ids =
        assessmentsPage.getContent().stream().map(Assessment::getId).collect(Collectors.toList());

    Map<UUID, long[]> summaryMap = buildSummaryMap(ids);

    // Pre-fetch teacher name once (all assessments on this page share the same teacher)
    String teacherName =
        userRepository.findById(currentUserId).map(User::getFullName).orElse("Unknown");

    return assessmentsPage.map(
        a ->
            mapToResponseWithSummary(
                a, teacherName, summaryMap.getOrDefault(a.getId(), new long[] {0L, 0L, 0L})));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canEditAssessment(UUID id) {
    Assessment assessment = loadAssessmentOrThrow(id);
    UUID currentUserId = getCurrentUserId();
    return (assessment.getTeacherId().equals(currentUserId) || hasRole(PredefinedRole.ADMIN_ROLE))
        && assessment.getStatus() == AssessmentStatus.DRAFT;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canDeleteAssessment(UUID id) {
    Assessment assessment = loadAssessmentOrThrow(id);
    UUID currentUserId = getCurrentUserId();
    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(id);
    return (assessment.getTeacherId().equals(currentUserId) || hasRole(PredefinedRole.ADMIN_ROLE))
        && assessment.getStatus() == AssessmentStatus.DRAFT
        && submissionCount == 0;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canPublishAssessment(UUID id) {
    Assessment assessment = loadAssessmentOrThrow(id);
    UUID currentUserId = getCurrentUserId();
    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(id);
    return (assessment.getTeacherId().equals(currentUserId) || hasRole(PredefinedRole.ADMIN_ROLE))
        && assessment.getStatus() == AssessmentStatus.DRAFT
        && totalQuestions > 0;
  }

  @Override
  @Transactional
  public AssessmentResponse closeAssessment(UUID id) {
    log.info("Closing assessment: {}", id);

    Assessment assessment = loadAssessmentOrThrow(id);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() == AssessmentStatus.CLOSED) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_CLOSED);
    }
    if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_PUBLISHED);
    }

    assessment.setStatus(AssessmentStatus.CLOSED);
    assessment = assessmentRepository.save(assessment);
    log.info("Assessment {} closed", id);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse cloneAssessment(UUID sourceId, CloneAssessmentRequest request) {
    log.info("Cloning assessment: {}", sourceId);

    Assessment source = loadAssessmentOrThrow(sourceId);
    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

    String newTitle =
        (request.getNewTitle() != null && !request.getNewTitle().isBlank())
            ? request.getNewTitle()
            : "Copy of " + source.getTitle();

    Assessment clone =
        Assessment.builder()
            .teacherId(currentUserId)
            .title(newTitle)
            .description(source.getDescription())
            .assessmentType(source.getAssessmentType())
            .timeLimitMinutes(source.getTimeLimitMinutes())
            .passingScore(source.getPassingScore())
            .randomizeQuestions(source.getRandomizeQuestions())
            .showCorrectAnswers(source.getShowCorrectAnswers())
            // Matrix is NOT cloned — teacher must rebuild if needed
            // examMatrixId is now the FK field
            .allowMultipleAttempts(source.getAllowMultipleAttempts())
            .maxAttempts(source.getMaxAttempts())
            .attemptScoringPolicy(source.getAttemptScoringPolicy())
            .showScoreImmediately(source.getShowScoreImmediately())
            .status(AssessmentStatus.DRAFT)
            .build();

    clone = assessmentRepository.save(clone);

    // Clone questions only for non-matrix path and when explicitly requested
    boolean shouldCloneQuestions =
        source.getExamMatrixId() == null && Boolean.TRUE.equals(request.getCloneQuestions());

    if (shouldCloneQuestions) {
      java.util.List<AssessmentQuestion> sourceQuestions =
          assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(source.getId());
      for (AssessmentQuestion aq : sourceQuestions) {
        AssessmentQuestion cloned =
            AssessmentQuestion.builder()
                .assessmentId(clone.getId())
                .questionId(aq.getQuestionId())
                .orderIndex(aq.getOrderIndex())
                .pointsOverride(aq.getPointsOverride())
                .build();
        assessmentQuestionRepository.save(cloned);
      }
      log.info("Cloned {} question(s) into assessment {}", sourceQuestions.size(), clone.getId());
    }

    log.info("Assessment {} cloned to {}", sourceId, clone.getId());
    return mapToResponse(clone);
  }

  @Override
  @Transactional
  public AssessmentResponse addQuestion(UUID assessmentId, AddQuestionToAssessmentRequest request) {
    log.info("Adding question {} to assessment {}", request.getQuestionId(), assessmentId);

    Assessment assessment = loadAssessmentOrThrow(assessmentId);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new AppException(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED);
    }
    if (assessment.getExamMatrixId() != null) {
      // Matrix-path: questions are managed through the matrix flow
      throw new AppException(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED);
    }

    // Verify the question exists and is not deleted
    questionRepository
        .findByIdAndNotDeleted(request.getQuestionId())
        .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

    if (assessmentQuestionRepository
        .findByAssessmentIdAndQuestionId(assessmentId, request.getQuestionId())
        .isPresent()) {
      throw new AppException(ErrorCode.QUESTION_ALREADY_IN_ASSESSMENT);
    }

    // Determine order index
    int nextOrder;
    if (request.getOrderIndex() != null) {
      nextOrder = request.getOrderIndex();
    } else {
      Integer maxOrder = assessmentQuestionRepository.findMaxOrderIndex(assessmentId);
      nextOrder = (maxOrder != null ? maxOrder : 0) + 1;
    }

    AssessmentQuestion aq =
        AssessmentQuestion.builder()
            .assessmentId(assessmentId)
            .questionId(request.getQuestionId())
            .orderIndex(nextOrder)
            .pointsOverride(request.getPointsOverride())
            .build();
    assessmentQuestionRepository.save(aq);

    log.info(
        "Question {} added to assessment {} at index {}",
        request.getQuestionId(),
        assessmentId,
        nextOrder);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse removeQuestion(UUID assessmentId, UUID questionId) {
    log.info("Removing question {} from assessment {}", questionId, assessmentId);

    Assessment assessment = loadAssessmentOrThrow(assessmentId);
    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new AppException(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED);
    }
    if (assessment.getExamMatrixId() != null) {
      throw new AppException(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED);
    }

    assessmentQuestionRepository
        .findByAssessmentIdAndQuestionId(assessmentId, questionId)
        .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_IN_ASSESSMENT));

    assessmentQuestionRepository.deleteByAssessmentIdAndQuestionId(assessmentId, questionId);

    log.info("Question {} removed from assessment {}", questionId, assessmentId);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentGenerationResponse generateQuestionsFromMatrix(
      UUID assessmentId, GenerateAssessmentQuestionsRequest request) {
    log.info(
        "Generating questions from matrix {} for assessment {}",
        request.getExamMatrixId(),
        assessmentId);

    Assessment assessment = loadAssessmentOrThrow(assessmentId);
    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(assessment.getTeacherId(), currentUserId);

    if (assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    // Load the exam matrix
    ExamMatrix matrix =
        examMatrixRepository
            .findByIdAndNotDeleted(request.getExamMatrixId())
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    // Verify matrix is approved
    if (matrix.getStatus() != MatrixStatus.APPROVED) {
      throw new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND);
    }

    // Load template mappings
    List<com.fptu.math_master.entity.ExamMatrixTemplateMapping> mappings =
        examMatrixTemplateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrix.getId());

    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND);
    }

    int totalQuestionsGenerated = 0;
    int totalPoints = 0;

    // For each template mapping, generate required number of questions
    for (com.fptu.math_master.entity.ExamMatrixTemplateMapping mapping : mappings) {
      log.info(
          "Processing mapping: {}, template: {}, count: {}",
          mapping.getId(),
          mapping.getTemplateId(),
          mapping.getQuestionCount());

      List<com.fptu.math_master.entity.Question> generatedQuestions =
          generateQuestionsFromTemplate(
              mapping.getTemplateId(),
              mapping.getQuestionCount(),
              mapping.getCognitiveLevel(),
              request.getReuseApprovedQuestions() != null
                  ? request.getReuseApprovedQuestions()
                  : false);

      // Create AssessmentQuestion records
      for (com.fptu.math_master.entity.Question question : generatedQuestions) {
        AssessmentQuestion assessmentQuestion =
            AssessmentQuestion.builder()
                .assessmentId(assessmentId)
                .questionId(question.getId())
                .matrixTemplateMappingId(mapping.getId())
                .pointsOverride(mapping.getPointsPerQuestion())
                .build();
        assessmentQuestionRepository.save(assessmentQuestion);
        totalQuestionsGenerated++;
        totalPoints += mapping.getPointsPerQuestion().intValue();
      }
    }

    log.info(
        "Generated {} questions for assessment {} with {} total points",
        totalQuestionsGenerated,
        assessmentId,
        totalPoints);

    return AssessmentGenerationResponse.builder()
        .totalQuestionsGenerated(totalQuestionsGenerated)
        .totalPoints(totalPoints)
        .message(
            String.format(
                "%d questions generated successfully from exam matrix. Total points: %d",
                totalQuestionsGenerated, totalPoints))
        .build();
  }

  private List<com.fptu.math_master.entity.Question> generateQuestionsFromTemplate(
      UUID templateId,
      Integer count,
      @SuppressWarnings("unused") com.fptu.math_master.enums.CognitiveLevel cognitiveLevel,
      boolean reuseApprovedQuestions) {
    log.info("Generating {} questions from template {}", count, templateId);

    ArrayList<com.fptu.math_master.entity.Question> result = new ArrayList<>();

    // First, try to reuse existing questions from template if requested
    if (reuseApprovedQuestions) {
      // Try to find existing questions from template
      List<com.fptu.math_master.entity.Question> existingList =
          questionRepository
              .findByTemplateIdAndNotDeleted(templateId)
              .stream()
              .limit(count)
              .collect(Collectors.toList());
      result.addAll(existingList);

      if (result.size() >= count) {
        if (!result.isEmpty()) {
        log.info("Reused {} approved questions from template {}", result.size(), templateId);
      }
        return result;
      }

      log.info(
          "Only {} approved questions available, need {} more",
          result.size(),
          count - result.size());
    }

    // Generate remaining questions using AI
    int remainingCount = count - result.size();
    for (int i = 0; i < remainingCount; i++) {
      try {
        // Call AI to generate a question from the template
        // This is a simplified implementation - in production, integrate with QuestionService
        log.debug("Generating question {} of {} from template {}", i + 1, remainingCount, templateId);
        // TODO: Integrate with AI question generation service
      } catch (Exception e) {
        log.error("Failed to generate question from template {}: {}", templateId, e.getMessage());
      }
    }

    return result;
  }

  private Assessment loadAssessmentOrThrow(UUID id) {
    return assessmentRepository
        .findByIdAndNotDeleted(id)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
  }

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

  private void validateDates(Instant startDate, Instant endDate) {
    if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
      throw new AppException(ErrorCode.ASSESSMENT_INVALID_SCHEDULE);
    }
  }

  private BigDecimal safeTotalPoints(Double raw) {
    if (raw == null) return BigDecimal.ZERO;
    return new BigDecimal(raw.toString());
  }

  /**
   * FIX: build a UUID → [questionCount, totalPoints*100, submissionCount] map from a single bulk
   * query so mapToResponse doesn't fire 3 extra queries per assessment (N+1 fix).
   * totalPoints is stored ×100 as long to avoid Double; divided back when building the response.
   */
  private Map<UUID, long[]> buildSummaryMap(Collection<UUID> ids) {
    Map<UUID, long[]> map = new HashMap<>();
    if (ids.isEmpty()) return map;
    List<Object[]> rows = assessmentRepository.findBulkSummaryByIds(ids);
    for (Object[] row : rows) {
      UUID aid = (UUID) row[0];
      long qCount = row[1] == null ? 0L : ((Number) row[1]).longValue();
      // row[2] is a Double from COALESCE(SUM(...))
      long pointsCents =
          row[2] == null
              ? 0L
              : new BigDecimal(row[2].toString()).multiply(BigDecimal.valueOf(100)).longValue();
      long subCount = row[3] == null ? 0L : ((Number) row[3]).longValue();
      map.put(aid, new long[] {qCount, pointsCents, subCount});
    }
    return map;
  }

  /** Single-assessment response — fires individual queries (used by write methods). */
  private AssessmentResponse mapToResponse(Assessment assessment) {
    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(assessment.getId());
    BigDecimal totalPoints =
        safeTotalPoints(assessmentRepository.calculateTotalPoints(assessment.getId()));
    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(assessment.getId());

    String teacherName =
        userRepository.findById(assessment.getTeacherId()).map(User::getFullName).orElse("Unknown");

    return buildResponse(assessment, teacherName, totalQuestions, totalPoints, submissionCount);
  }

  /** Bulk-list response — uses pre-fetched summary row to avoid N+1. */
  private AssessmentResponse mapToResponseWithSummary(
      Assessment assessment, String teacherName, long[] summary) {
    long qCount = summary[0];
    BigDecimal totalPoints =
        BigDecimal.valueOf(summary[1]).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    long subCount = summary[2];

    return buildResponse(assessment, teacherName, qCount, totalPoints, subCount);
  }

  private AssessmentResponse buildResponse(
      Assessment assessment,
      String teacherName,
      long totalQuestions,
      BigDecimal totalPoints,
      long submissionCount) {

    List<UUID> lessonIds = assessmentLessonRepository.findLessonIdsByAssessmentId(assessment.getId());
    Map<UUID, String> lessonTitleById =
        lessonRepository.findByIdInAndNotDeleted(lessonIds).stream()
            .collect(Collectors.toMap(Lesson::getId, Lesson::getTitle));
    List<String> lessonTitles =
        lessonIds.stream().map(id -> lessonTitleById.getOrDefault(id, "Unknown")).toList();

    return AssessmentResponse.builder()
        .id(assessment.getId())
        .teacherId(assessment.getTeacherId())
        .teacherName(teacherName)
        .lessonIds(lessonIds)
        .lessonTitles(lessonTitles)
        .title(assessment.getTitle())
        .description(assessment.getDescription())
        .assessmentType(assessment.getAssessmentType())
        .timeLimitMinutes(assessment.getTimeLimitMinutes())
        .passingScore(assessment.getPassingScore())
        .startDate(assessment.getStartDate())
        .endDate(assessment.getEndDate())
        .randomizeQuestions(assessment.getRandomizeQuestions())
        .showCorrectAnswers(assessment.getShowCorrectAnswers())
        .assessmentMode(assessment.getAssessmentMode())
        .examMatrixId(assessment.getExamMatrixId())
        .allowMultipleAttempts(assessment.getAllowMultipleAttempts())
        .maxAttempts(assessment.getMaxAttempts())
        .attemptScoringPolicy(assessment.getAttemptScoringPolicy())
        .showScoreImmediately(assessment.getShowScoreImmediately())
        .status(assessment.getStatus())
        .totalQuestions(totalQuestions)
        .totalPoints(totalPoints)
        .submissionCount(submissionCount)
        .createdAt(assessment.getCreatedAt())
        .updatedAt(assessment.getUpdatedAt())
        .build();
  }

  private ExamMatrix validateAndGetAccessibleMatrix(UUID matrixId, UUID currentUserId) {
    ExamMatrix matrix =
        examMatrixRepository
            .findByIdAndNotDeleted(matrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));
    if (!matrix.getTeacherId().equals(currentUserId) && !hasRole(PredefinedRole.ADMIN_ROLE)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }
    return matrix;
  }

  private List<UUID> normalizeLessonIds(List<UUID> lessonIds) {
    return new java.util.ArrayList<>(new LinkedHashSet<>(lessonIds));
  }

  private void validateLessonSelectionForMatrix(UUID matrixId, List<UUID> lessonIds) {
    Set<UUID> existingLessonIds = new java.util.HashSet<>(lessonRepository.findExistingIdsByIds(lessonIds));
    if (existingLessonIds.size() != lessonIds.size()) {
      throw new AppException(ErrorCode.LESSON_NOT_FOUND);
    }

    Set<UUID> matrixLessonIds =
        new java.util.HashSet<>(
            examMatrixTemplateMappingRepository.findDistinctLessonIdsByExamMatrixId(matrixId));
    if (!matrixLessonIds.containsAll(lessonIds)) {
      throw new AppException(ErrorCode.ASSESSMENT_LESSON_NOT_IN_MATRIX);
    }
  }

  private void syncAssessmentLessons(UUID assessmentId, List<UUID> lessonIds) {
    assessmentLessonRepository.deleteByAssessmentId(assessmentId);
    List<AssessmentLesson> links =
        lessonIds.stream()
            .map(
                lessonId ->
                    AssessmentLesson.builder().assessmentId(assessmentId).lessonId(lessonId).build())
            .toList();
    assessmentLessonRepository.saveAll(links);
  }
}
