package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AttemptScoringPolicy;
import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StudentAssessmentServiceImpl implements StudentAssessmentService {

  AssessmentRepository assessmentRepository;
  SubmissionRepository submissionRepository;
  QuizAttemptRepository quizAttemptRepository;
  AssessmentQuestionRepository assessmentQuestionRepository;
  AnswerRepository answerRepository;
  AssessmentDraftService draftService;
  CentrifugoService centrifugoService;
  GradingService gradingService;
  EnrollmentRepository enrollmentRepository;
  CourseAssessmentRepository courseAssessmentRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<StudentAssessmentResponse> getMyAssessments(String statusFilter, Pageable pageable) {
    UUID studentId = getCurrentUserId();
    Instant now = Instant.now();

    log.info("Getting assessments for student: {}, filter: {}", studentId, statusFilter);

    Set<UUID> accessibleAssessmentIds = getAccessibleAssessmentIds(studentId, null);
    if (accessibleAssessmentIds.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<Assessment> allAssessments =
        assessmentRepository.findByIdInAndNotDeleted(accessibleAssessmentIds).stream()
            .filter(a -> a.getStatus() == AssessmentStatus.PUBLISHED)
            .filter(a -> isAssessmentAvailable(a, now))
            .collect(Collectors.toList());

    List<StudentAssessmentResponse> responses =
        allAssessments.stream()
          .map(assessment -> mapToStudentResponse(assessment, studentId, now, null, null))
            .filter(response -> matchesStatusFilter(response, statusFilter))
            .sorted(
                Comparator.comparing(
                    StudentAssessmentResponse::getDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), responses.size());

    if (start >= responses.size()) {
      return new PageImpl<>(List.of(), pageable, responses.size());
    }

    return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<StudentAssessmentResponse> getMyAssessmentsByCourse(
      UUID courseId, String statusFilter, Pageable pageable) {
    UUID studentId = getCurrentUserId();
    Instant now = Instant.now();

    Optional<Enrollment> enrollmentOpt =
        enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId);
    if (enrollmentOpt.isEmpty()
        || enrollmentOpt.get().getStatus() != com.fptu.math_master.enums.EnrollmentStatus.ACTIVE) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<CourseAssessment> courseAssessments =
        courseAssessmentRepository.findByCourseIdAndNotDeleted(courseId);
    if (courseAssessments.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    Map<UUID, CourseAssessment> courseAssessmentByAssessmentId =
        courseAssessments.stream()
            .collect(Collectors.toMap(CourseAssessment::getAssessmentId, ca -> ca, (left, right) -> left));

    List<Assessment> assessments =
        assessmentRepository.findByIdInAndNotDeleted(courseAssessmentByAssessmentId.keySet()).stream()
            .filter(a -> a.getStatus() == AssessmentStatus.PUBLISHED)
            .filter(a -> isAssessmentAvailable(a, now))
            .collect(Collectors.toList());

    List<StudentAssessmentResponse> responses =
        assessments.stream()
            .map(
                assessment -> {
                  CourseAssessment ca = courseAssessmentByAssessmentId.get(assessment.getId());
                  return mapToStudentResponse(
                      assessment,
                      studentId,
                      now,
                      ca != null ? ca.isRequired() : null,
                      ca != null ? ca.getOrderIndex() : null);
                })
            .filter(response -> matchesStatusFilter(response, statusFilter))
            .sorted(
                Comparator.comparing(
                        StudentAssessmentResponse::getCourseOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                        StudentAssessmentResponse::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), responses.size());
    if (start >= responses.size()) {
      return new PageImpl<>(List.of(), pageable, responses.size());
    }

    return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
  }

  @Override
  @Transactional(readOnly = true)
  public StudentAssessmentResponse getAssessmentDetails(UUID assessmentId) {
    UUID studentId = getCurrentUserId();
    Instant now = Instant.now();

    if (!getAccessibleAssessmentIds(studentId, null).contains(assessmentId)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }

    Assessment assessment =
        assessmentRepository
            .findById(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_PUBLISHED);
    }

    return mapToStudentResponse(assessment, studentId, now, null, null);
  }

  @Override
  @Transactional
  public AttemptStartResponse startAssessment(StartAssessmentRequest request) {
    UUID studentId = getCurrentUserId();
    UUID assessmentId = request.getAssessmentId();
    Instant now = Instant.now();

    log.info("Student {} starting assessment {}", studentId, assessmentId);

    if (!getAccessibleAssessmentIds(studentId, null).contains(assessmentId)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }

    Assessment assessment =
        assessmentRepository
            .findById(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateAssessmentAvailability(assessment, now);

    // Lock on student-assessment pair to make submission creation atomic.
    String attemptLockKey = ("attempt_" + assessmentId + "_" + studentId).intern();
    synchronized (attemptLockKey) {
      Submission submission = findOrCreateSubmission(assessment, studentId);

      // Double-check for IN_PROGRESS attempts inside synchronized block
      List<QuizAttempt> inProgressAttempts =
        quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
          assessmentId, studentId, SubmissionStatus.IN_PROGRESS);
      if (!inProgressAttempts.isEmpty()) {
        QuizAttempt activeAttempt = inProgressAttempts.get(0);
        log.info(
          "Found existing active attempt {} in synchronized block for student {} and assessment {}",
          activeAttempt.getId(),
          studentId,
          assessmentId);
        return buildAttemptStartResponse(assessment, activeAttempt, false);
      }

      // Check attempt limits only when starting a new attempt.
      validateAttemptLimit(assessment, submission.getId());

      Integer attemptNumber = quizAttemptRepository.countBySubmissionId(submission.getId()) + 1;

      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submission.getId())
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(attemptNumber)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now)
              .build();

      attempt = quizAttemptRepository.save(attempt);
      log.info("Created quiz attempt: {}", attempt.getId());

      draftService.initDraft(attempt.getId(), assessmentId, assessment.getTimeLimitMinutes());

      return buildAttemptStartResponse(assessment, attempt, Boolean.TRUE.equals(assessment.getRandomizeQuestions()));
    }
  }

  private AttemptStartResponse buildAttemptStartResponse(
      Assessment assessment, QuizAttempt attempt, boolean randomizeQuestions) {
    UUID studentId = attempt.getStudentId();

    String connectionToken = centrifugoService.generateConnectionToken(studentId, attempt.getId());
    String channelName = centrifugoService.getAttemptChannel(attempt.getId());

    List<AssessmentQuestion> assessmentQuestions =
        assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessment.getId());

    if (randomizeQuestions) {
      Collections.shuffle(assessmentQuestions);
    }

    List<AttemptQuestionResponse> questions =
        assessmentQuestions.stream().map(this::mapToAttemptQuestion).collect(Collectors.toList());

    Instant expiresAt =
        assessment.getTimeLimitMinutes() != null
        ? attempt.getStartedAt().plusSeconds(assessment.getTimeLimitMinutes() * 60L)
            : null;

    return AttemptStartResponse.builder()
        .attemptId(attempt.getId())
        .submissionId(attempt.getSubmissionId())
      .assessmentId(assessment.getId())
      .attemptNumber(attempt.getAttemptNumber())
      .startedAt(attempt.getStartedAt())
        .expiresAt(expiresAt)
        .timeLimitMinutes(assessment.getTimeLimitMinutes())
        .totalQuestions((long) questions.size())
        .instructions(assessment.getDescription())
        .connectionToken(connectionToken)
        .channelName(channelName)
        .questions(questions)
        .build();
  }

  @Override
  @Transactional
  public AnswerAckResponse updateAnswer(AnswerUpdateRequest request) {
    UUID studentId = getCurrentUserId();
    UUID attemptId = request.getAttemptId();

    log.debug("Updating answer for attempt {}, question {}", attemptId, request.getQuestionId());

    QuizAttempt attempt = validateAttemptAccess(attemptId, studentId);

    if (isAttemptExpired(attempt)) {
      throw new AppException(ErrorCode.TIME_LIMIT_EXCEEDED);
    }

    draftService.saveAnswer(attemptId, request.getQuestionId(), request.getAnswerValue());
    centrifugoService.publishAnswerAck(
        attemptId, request.getQuestionId(), request.getSequenceNumber());

    return AnswerAckResponse.builder()
        .type("ack")
        .questionId(request.getQuestionId())
        .serverTimestamp(Instant.now())
        .sequenceNumber(request.getSequenceNumber())
        .success(true)
        .message("Answer saved successfully")
        .build();
  }

  @Override
  @Transactional
  public AnswerAckResponse updateFlag(FlagUpdateRequest request) {
    UUID studentId = getCurrentUserId();
    UUID attemptId = request.getAttemptId();

    log.debug(
        "Updating flag for attempt {}, question {}: {}",
        attemptId,
        request.getQuestionId(),
        request.getFlagged());

    validateAttemptAccess(attemptId, studentId);

    draftService.saveFlag(attemptId, request.getQuestionId(), request.getFlagged());
    centrifugoService.publishFlagAck(attemptId, request.getQuestionId(), request.getFlagged());

    return AnswerAckResponse.builder()
        .type("flag_ack")
        .questionId(request.getQuestionId())
        .serverTimestamp(Instant.now())
        .success(true)
        .message("Flag updated successfully")
        .build();
  }

  @Override
  @Transactional
  public void submitAssessment(SubmitAssessmentRequest request) {
    UUID studentId = getCurrentUserId();
    UUID attemptId = request.getAttemptId();

    log.info("Student {} submitting attempt {}", studentId, attemptId);

    QuizAttempt attempt = validateAttemptAccess(attemptId, studentId);

    draftService.flushDraftToDatabase(attemptId);

    Instant now = Instant.now();
    attempt.setSubmittedAt(now);
    attempt.setStatus(SubmissionStatus.SUBMITTED);
    attempt.setTimeSpentSeconds((int) Duration.between(attempt.getStartedAt(), now).getSeconds());

    quizAttemptRepository.save(attempt);

    updateSubmissionStatus(attempt.getSubmissionId());

    draftService.deleteDraft(attemptId);
    centrifugoService.publishSubmitted(attemptId);

    log.info("Assessment submitted successfully: {}", attemptId);

    // Trigger auto-grading for objective questions
    try {
      log.info("Triggering auto-grading for submission: {}", attempt.getSubmissionId());
      gradingService.autoGradeSubmission(attempt.getSubmissionId());
      log.info("Auto-grading completed for submission: {}", attempt.getSubmissionId());
    } catch (Exception e) {
      log.error(
          "Auto-grading failed for submission: {}. Teacher can grade manually later.",
          attempt.getSubmissionId(),
          e);
      // Continue - don't fail the submission if auto-grading fails
      // Teacher can still grade manually
    }
  }

  @Override
  @Transactional(readOnly = true)
  public DraftSnapshotResponse getDraftSnapshot(UUID attemptId) {
    UUID studentId = getCurrentUserId();

    QuizAttempt attempt = validateAttemptAccess(attemptId, studentId);

    Map<String, Object> snapshot = draftService.getDraftSnapshot(attemptId);

    Map<UUID, Object> answers = parseAnswersMap(snapshot.get("answers"));
    Map<UUID, Boolean> flags = parseFlagsMap(snapshot.get("flags"));

    Integer answeredCount = draftService.getAnsweredCount(attemptId);
    Long totalQuestions =
        (long)
            assessmentQuestionRepository
                .findByAssessmentIdOrderByOrderIndex(attempt.getAssessmentId())
                .size();

    Integer timeRemainingSeconds = calculateTimeRemaining(attempt);

    return DraftSnapshotResponse.builder()
        .attemptId(attemptId)
        .answers(answers)
        .flags(flags)
        .startedAt(attempt.getStartedAt())
        .expiresAt(getExpiresAt(attempt))
        .timeRemainingSeconds(timeRemainingSeconds)
        .answeredCount(answeredCount)
        .totalQuestions(totalQuestions.intValue())
        .build();
  }

  @Override
  @Transactional
  public void saveAndExit(UUID attemptId) {
    UUID studentId = getCurrentUserId();

    log.info("Student {} saving and exiting attempt {}", studentId, attemptId);
    validateAttemptAccess(attemptId, studentId);
    log.info("Attempt saved and exited: {}", attemptId);
  }

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth
        instanceof
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
                jwtAuth) {
      String sub = jwtAuth.getToken().getSubject();
      return UUID.fromString(sub);
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private boolean isAssessmentAvailable(Assessment assessment, Instant now) {
    if (assessment.getStartDate() != null && now.isBefore(assessment.getStartDate())) {
      return false;
    }
    if (assessment.getEndDate() != null && now.isAfter(assessment.getEndDate())) {
      return false;
    }
    return true;
  }

  private Set<UUID> getAccessibleAssessmentIds(UUID studentId, UUID courseId) {
    List<Enrollment> activeEnrollments;
    if (courseId != null) {
      activeEnrollments =
          enrollmentRepository
              .findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId)
              .filter(e -> e.getStatus() == com.fptu.math_master.enums.EnrollmentStatus.ACTIVE)
              .map(List::of)
              .orElseGet(List::of);
    } else {
      activeEnrollments =
          enrollmentRepository.findByStudentIdAndStatusAndDeletedAtIsNullOrderByEnrolledAtDesc(
              studentId, com.fptu.math_master.enums.EnrollmentStatus.ACTIVE);
    }

    if (activeEnrollments.isEmpty()) {
      return Set.of();
    }

    Set<UUID> courseIds = activeEnrollments.stream().map(Enrollment::getCourseId).collect(Collectors.toSet());
    List<CourseAssessment> courseAssessments =
        courseAssessmentRepository.findByCourseIdInAndNotDeleted(courseIds);

    return courseAssessments.stream().map(CourseAssessment::getAssessmentId).collect(Collectors.toSet());
  }

  private StudentAssessmentResponse mapToStudentResponse(
      Assessment assessment,
      UUID studentId,
      Instant now,
      Boolean isRequired,
      Integer courseOrderIndex) {
    Long totalQuestions =
        (long)
            assessmentQuestionRepository
                .findByAssessmentIdOrderByOrderIndex(assessment.getId())
                .size();

    Double totalPointsDouble = assessmentRepository.calculateTotalPoints(assessment.getId());
    BigDecimal totalPoints =
        totalPointsDouble != null ? BigDecimal.valueOf(totalPointsDouble) : BigDecimal.ZERO;

    Optional<Submission> submissionOpt =
        submissionRepository.findByAssessmentIdAndStudentId(assessment.getId(), studentId);

    String studentStatus = determineStudentStatus(assessment, submissionOpt, now);
    UUID currentAttemptId = null;
    Integer attemptNumber = 0;

    if (submissionOpt.isPresent()) {
      List<QuizAttempt> inProgressAttempts =
          quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessment.getId(), studentId, SubmissionStatus.IN_PROGRESS);
      if (!inProgressAttempts.isEmpty()) {
        currentAttemptId = inProgressAttempts.get(0).getId();
      }

      List<QuizAttempt> attempts =
          quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(
              submissionOpt.get().getId());

      if (!attempts.isEmpty()) {
        attemptNumber = attempts.size();
      }
    }

    boolean canStart = canStartAssessment(assessment, submissionOpt, now);
    String cannotStartReason =
        canStart ? null : getCannotStartReason(assessment, submissionOpt, now);

    return StudentAssessmentResponse.builder()
        .id(assessment.getId())
        .title(assessment.getTitle())
        .description(assessment.getDescription())
        .assessmentType(assessment.getAssessmentType())
        .totalQuestions(totalQuestions)
        .totalPoints(totalPoints)
        .timeLimitMinutes(assessment.getTimeLimitMinutes())
        .passingScore(assessment.getPassingScore())
        .dueDate(assessment.getEndDate())
        .startDate(assessment.getStartDate())
        .endDate(assessment.getEndDate())
        .status(assessment.getStatus())
        .studentStatus(studentStatus)
        .currentAttemptId(currentAttemptId)
        .attemptNumber(attemptNumber)
        .maxAttempts(assessment.getMaxAttempts())
        .allowMultipleAttempts(assessment.getAllowMultipleAttempts())
        .canStart(canStart)
        .cannotStartReason(cannotStartReason)
          .isRequired(isRequired)
          .courseOrderIndex(courseOrderIndex)
        .build();
  }

  private String determineStudentStatus(
      Assessment assessment, Optional<Submission> submissionOpt, Instant now) {
    if (assessment.getStartDate() != null && now.isBefore(assessment.getStartDate())) {
      return "UPCOMING";
    }

    if (submissionOpt.isPresent()) {
      List<QuizAttempt> inProgressAttempts =
          quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessment.getId(), submissionOpt.get().getStudentId(), SubmissionStatus.IN_PROGRESS);

      if (!inProgressAttempts.isEmpty()) {
        return "IN_PROGRESS";
      }

      if (submissionOpt.get().getStatus() == SubmissionStatus.SUBMITTED
          || submissionOpt.get().getStatus() == SubmissionStatus.GRADED) {
        return "COMPLETED";
      }
    }

    if (assessment.getEndDate() != null && now.isAfter(assessment.getEndDate())) {
      return "COMPLETED";
    }

    return "UPCOMING";
  }

  private boolean matchesStatusFilter(StudentAssessmentResponse response, String statusFilter) {
    if (statusFilter == null || statusFilter.isEmpty()) {
      return true;
    }
    return response.getStudentStatus().equalsIgnoreCase(statusFilter);
  }

  private void validateAssessmentCanStart(Assessment assessment, UUID studentId, Instant now) {
    validateAssessmentAvailability(assessment, now);

    Optional<Submission> submissionOpt =
        submissionRepository.findByAssessmentIdAndStudentId(assessment.getId(), studentId);

    if (submissionOpt.isEmpty()) {
      return;
    }

    List<QuizAttempt> inProgressAttempts =
        quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
            assessment.getId(), studentId, SubmissionStatus.IN_PROGRESS);
    if (!inProgressAttempts.isEmpty()) {
      return;
    }

    validateAttemptLimit(assessment, submissionOpt.get().getId());
  }

  private void validateAssessmentAvailability(Assessment assessment, Instant now) {
    if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_PUBLISHED);
    }

    if (assessment.getStartDate() != null && now.isBefore(assessment.getStartDate())) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_AVAILABLE);
    }

    if (assessment.getEndDate() != null && now.isAfter(assessment.getEndDate())) {
      throw new AppException(ErrorCode.ASSESSMENT_EXPIRED);
    }
  }

  private void validateAttemptLimit(Assessment assessment, UUID submissionId) {
    Integer attemptCount = quizAttemptRepository.countBySubmissionId(submissionId);

    if (Boolean.TRUE.equals(assessment.getAllowMultipleAttempts())) {
      if (assessment.getMaxAttempts() != null && attemptCount >= assessment.getMaxAttempts()) {
        throw new AppException(ErrorCode.MAX_ATTEMPTS_REACHED);
      }
    } else {
      if (attemptCount > 0) {
        throw new AppException(ErrorCode.MAX_ATTEMPTS_REACHED);
      }
    }
  }

  private boolean canStartAssessment(
      Assessment assessment, Optional<Submission> submissionOpt, Instant now) {
    try {
      validateAssessmentCanStart(
          assessment, submissionOpt.map(Submission::getStudentId).orElse(getCurrentUserId()), now);
      return true;
    } catch (AppException e) {
      return false;
    }
  }

  private String getCannotStartReason(
      Assessment assessment, Optional<Submission> submissionOpt, Instant now) {
    if (assessment.getStartDate() != null && now.isBefore(assessment.getStartDate())) {
      return "Assessment has not started yet";
    }

    if (assessment.getEndDate() != null && now.isAfter(assessment.getEndDate())) {
      return "Assessment has expired";
    }

    if (submissionOpt.isPresent()) {
      List<QuizAttempt> inProgressAttempts =
          quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessment.getId(), submissionOpt.get().getStudentId(), SubmissionStatus.IN_PROGRESS);
      if (!inProgressAttempts.isEmpty()) {
        return null;
      }

      Integer attemptCount = quizAttemptRepository.countBySubmissionId(submissionOpt.get().getId());

      if (Boolean.TRUE.equals(assessment.getAllowMultipleAttempts())) {
        if (assessment.getMaxAttempts() != null && attemptCount >= assessment.getMaxAttempts()) {
          return "Maximum attempts reached";
        }
      } else if (attemptCount > 0) {
        return "Only one attempt allowed";
      }
    }

    return null;
  }

  private Submission createSubmission(Assessment assessment, UUID studentId) {
    Double rawMaxScore = assessmentRepository.calculateTotalPoints(assessment.getId());
    BigDecimal maxScore =
        rawMaxScore != null && rawMaxScore > 0
            ? new BigDecimal(rawMaxScore.toString())
            : BigDecimal.ZERO;

    Submission submission =
        Submission.builder()
            .assessmentId(assessment.getId())
            .studentId(studentId)
            .status(SubmissionStatus.IN_PROGRESS)
            .startedAt(Instant.now())
            .maxScore(maxScore)
            .gradesReleased(false)
            .build();

    return submissionRepository.save(submission);
  }

  private Submission findOrCreateSubmission(Assessment assessment, UUID studentId) {
    return submissionRepository
        .findByAssessmentIdAndStudentId(assessment.getId(), studentId)
        .orElseGet(
            () -> {
              try {
                return createSubmission(assessment, studentId);
              } catch (DataIntegrityViolationException e) {
                // Another concurrent request created the same submission first.
                return submissionRepository
                    .findByAssessmentIdAndStudentId(assessment.getId(), studentId)
                    .orElseThrow(() -> e);
              }
            });
  }

  private QuizAttempt validateAttemptAccess(UUID attemptId, UUID studentId) {
    QuizAttempt attempt =
        quizAttemptRepository
            .findById(attemptId)
            .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));

    if (!attempt.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ATTEMPT_ACCESS_DENIED);
    }

    if (attempt.getStatus() != SubmissionStatus.IN_PROGRESS) {
      throw new AppException(ErrorCode.ATTEMPT_NOT_IN_PROGRESS);
    }

    return attempt;
  }

  private boolean isAttemptExpired(QuizAttempt attempt) {
    Assessment assessment =
        assessmentRepository
            .findById(attempt.getAssessmentId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (assessment.getTimeLimitMinutes() == null) {
      return false;
    }

    Instant expiresAt = attempt.getStartedAt().plusSeconds(assessment.getTimeLimitMinutes() * 60L);
    return Instant.now().isAfter(expiresAt);
  }

  private void updateSubmissionStatus(UUID submissionId) {
    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    List<QuizAttempt> attempts =
        quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId);

    boolean allSubmitted =
        attempts.stream()
            .allMatch(
                a ->
                    a.getStatus() == SubmissionStatus.SUBMITTED
                        || a.getStatus() == SubmissionStatus.GRADED);

    if (allSubmitted && !attempts.isEmpty()) {
      submission.setStatus(SubmissionStatus.SUBMITTED);
      submission.setSubmittedAt(Instant.now());

      Assessment assessment =
          assessmentRepository
              .findById(submission.getAssessmentId())
              .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

      AttemptScoringPolicy policy =
          assessment.getAttemptScoringPolicy() != null
              ? assessment.getAttemptScoringPolicy()
              : AttemptScoringPolicy.BEST;

      List<BigDecimal> attemptScores =
          attempts.stream()
              .filter(a -> a.getScore() != null)
              .map(QuizAttempt::getScore)
              .collect(Collectors.toList());

      if (!attemptScores.isEmpty()) {
        BigDecimal policyScore;
        switch (policy) {
          case LATEST:
            policyScore =
                attempts.stream()
                    .filter(a -> a.getScore() != null)
                    .findFirst()
                    .map(QuizAttempt::getScore)
                    .orElse(BigDecimal.ZERO);
            break;
          case AVERAGE:
            policyScore =
                attemptScores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(attemptScores.size()), 2, RoundingMode.HALF_UP);
            break;
          case BEST:
          default:
            policyScore = attemptScores.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            break;
        }
        if (submission.getManualAdjustment() == null) {
          submission.setFinalScore(policyScore);
        } else {
          submission.setFinalScore(policyScore.add(submission.getManualAdjustment()));
        }
      }

      submissionRepository.save(submission);
    }
  }

  private AttemptQuestionResponse mapToAttemptQuestion(AssessmentQuestion aq) {
    Question question = aq.getQuestion();
    
    // Determine partNumber from question type
    Integer partNumber = null;
    if (question.getQuestionType() != null) {
      switch (question.getQuestionType()) {
        case MULTIPLE_CHOICE:
          partNumber = 1;
          break;
        case TRUE_FALSE:
          partNumber = 2;
          break;
        case SHORT_ANSWER:
          partNumber = 3;
          break;
        default:
          partNumber = null;
      }
    }

    return AttemptQuestionResponse.builder()
        .questionId(question.getId())
        .orderIndex(aq.getOrderIndex())
        .partNumber(partNumber)
        .questionType(question.getQuestionType())
        .questionText(question.getQuestionText())
      .diagramData(question.getDiagramData())
      .diagramUrl(question.getRenderedImageUrl())
        .options(question.getOptions())
        .points(aq.getPointsOverride() != null ? aq.getPointsOverride() : question.getPoints())
        .build();
  }

  @SuppressWarnings("unchecked")
  private Map<UUID, Object> parseAnswersMap(Object answersObj) {
    if (answersObj instanceof Map) {
      Map<String, Object> raw = (Map<String, Object>) answersObj;
      Map<UUID, Object> parsed = new HashMap<>();
      raw.forEach(
          (k, v) -> {
            try {
              parsed.put(UUID.fromString(k), v);
            } catch (Exception e) {
              log.warn("Failed to parse answer key: {}", k);
            }
          });
      return parsed;
    }
    return new HashMap<>();
  }

  @SuppressWarnings("unchecked")
  private Map<UUID, Boolean> parseFlagsMap(Object flagsObj) {
    if (flagsObj instanceof Map) {
      Map<String, Boolean> raw = (Map<String, Boolean>) flagsObj;
      Map<UUID, Boolean> parsed = new HashMap<>();
      raw.forEach(
          (k, v) -> {
            try {
              parsed.put(UUID.fromString(k), v);
            } catch (Exception e) {
              log.warn("Failed to parse flag key: {}", k);
            }
          });
      return parsed;
    }
    return new HashMap<>();
  }

  private Integer calculateTimeRemaining(QuizAttempt attempt) {
    Assessment assessment =
        assessmentRepository
            .findById(attempt.getAssessmentId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (assessment.getTimeLimitMinutes() == null) {
      return null;
    }

    long elapsed = Duration.between(attempt.getStartedAt(), Instant.now()).getSeconds();
    long total = assessment.getTimeLimitMinutes() * 60L;
    long remaining = total - elapsed;

    return remaining > 0 ? (int) remaining : 0;
  }

  private Instant getExpiresAt(QuizAttempt attempt) {
    Assessment assessment =
        assessmentRepository
            .findById(attempt.getAssessmentId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (assessment.getTimeLimitMinutes() == null) {
      return null;
    }

    return attempt.getStartedAt().plusSeconds(assessment.getTimeLimitMinutes() * 60L);
  }
}
