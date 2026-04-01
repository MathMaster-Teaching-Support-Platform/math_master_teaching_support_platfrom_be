package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.*;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.GradingService;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
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
public class GradingServiceImpl implements GradingService {

  SubmissionRepository submissionRepository;
  AnswerRepository answerRepository;
  QuestionRepository questionRepository;
  AssessmentRepository assessmentRepository;
  UserRepository userRepository;
  GradeAuditLogRepository gradeAuditLogRepository;
  RegradeRequestRepository regradeRequestRepository;
  QuizAttemptRepository quizAttemptRepository;
  AiReviewRepository aiReviewRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void autoGradeSubmission(UUID submissionId) {
    log.info("Auto-grading submission: {}", submissionId);

    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (submission.getStatus() == SubmissionStatus.GRADED) {
      log.info("Skipping auto-grade for already graded submission: {}", submissionId);
      return;
    }

    if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
      throw new AppException(ErrorCode.SUBMISSION_ALREADY_GRADED);
    }

    List<Answer> answers = answerRepository.findBySubmissionId(submissionId);

    Set<UUID> questionIds = answers.stream().map(Answer::getQuestionId).collect(Collectors.toSet());
    Map<UUID, Question> questionMap =
        questionRepository.findAllById(questionIds).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));

    boolean hasSubjectiveQuestions = false;
    BigDecimal totalScore = BigDecimal.ZERO;

    for (Answer answer : answers) {
      Question question = questionMap.get(answer.getQuestionId());
      if (question == null) {
        log.warn(
            "Question {} not found for answer {}, skipping",
            answer.getQuestionId(),
            answer.getId());
        hasSubjectiveQuestions = true;
        continue;
      }

      boolean autoGraded = autoGradeAnswer(answer, question);
      if (!autoGraded) {
        hasSubjectiveQuestions = true;
      } else {
        if (answer.getPointsEarned() != null) {
          totalScore = totalScore.add(answer.getPointsEarned());
        }
      }

      answerRepository.save(answer);
    }

    submission.setScore(totalScore);

    BigDecimal computedPercentage = null;
    if (submission.getMaxScore() != null
        && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
      computedPercentage =
          totalScore
              .divide(submission.getMaxScore(), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
      submission.setPercentage(computedPercentage);
    }

    BigDecimal finalScore = totalScore;
    if (submission.getManualAdjustment() != null) {
      finalScore = finalScore.add(submission.getManualAdjustment());
    }
    submission.setFinalScore(finalScore);

    if (!hasSubjectiveQuestions) {
      submission.setStatus(SubmissionStatus.GRADED);
      submission.setGradedBy(SecurityUtils.getCurrentUserId());
      submission.setGradedAt(Instant.now());
    }

    submissionRepository.save(submission);

    final BigDecimal finalTotalScore = totalScore;
    final BigDecimal finalComputedPercentage = computedPercentage;
    final boolean finalHasSubjectiveQuestions = hasSubjectiveQuestions;
    quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId).stream()
        .filter(
            a ->
                a.getStatus() == SubmissionStatus.SUBMITTED
                    || a.getStatus() == SubmissionStatus.GRADED)
        .findFirst()
        .ifPresent(
            attempt -> {
              attempt.setScore(finalTotalScore);
              attempt.setMaxScore(submission.getMaxScore());
              attempt.setPercentage(finalComputedPercentage);
              if (!finalHasSubjectiveQuestions) {
                attempt.setStatus(SubmissionStatus.GRADED);
              }
              quizAttemptRepository.save(attempt);
            });

    log.info(
        "Auto-grading completed for submission: {}. Status: {}, Score: {}",
        submissionId,
        submission.getStatus(),
        finalTotalScore);
  }

  private boolean autoGradeAnswer(Answer answer, Question question) {
    QuestionType type = question.getQuestionType();

    switch (type) {
      case MULTIPLE_CHOICE:
        return gradeMultipleChoice(answer, question);
      case TRUE_FALSE:
        return gradeTrueFalse(answer, question);
      case SHORT_ANSWER:
        return gradeShortAnswer(answer, question);
      case ESSAY:
      case CODING:
        return false;
      default:
        return false;
    }
  }

  private boolean gradeMultipleChoice(Answer answer, Question question) {
    if (question.getCorrectAnswer() == null) {
      return false;
    }
    
    // Try to get answer from answerData first (structured format)
    String studentAnswer = null;
    if (answer.getAnswerData() != null) {
      Object selected = answer.getAnswerData().get("selected");
      if (selected != null) {
        studentAnswer = selected.toString();
      }
    }
    
    // Fallback to answerText (simple string format)
    if (studentAnswer == null && answer.getAnswerText() != null) {
      studentAnswer = answer.getAnswerText().trim();
    }
    
    if (studentAnswer == null) {
      return false;
    }
    
    boolean isCorrect = studentAnswer.equals(question.getCorrectAnswer());
    answer.setIsCorrect(isCorrect);
    answer.setPointsEarned(isCorrect ? question.getPoints() : BigDecimal.ZERO);
    return true;
  }

  private boolean gradeTrueFalse(Answer answer, Question question) {
    if (question.getCorrectAnswer() == null) {
      return false;
    }
    
    // Try to get answer from answerData first (structured format)
    String studentAnswer = null;
    if (answer.getAnswerData() != null) {
      Object value = answer.getAnswerData().get("value");
      if (value != null) {
        studentAnswer = value.toString();
      }
    }
    
    // Fallback to answerText (simple string format)
    if (studentAnswer == null && answer.getAnswerText() != null) {
      studentAnswer = answer.getAnswerText().trim();
    }
    
    if (studentAnswer == null) {
      return false;
    }
    
    boolean isCorrect = studentAnswer.equalsIgnoreCase(question.getCorrectAnswer());
    answer.setIsCorrect(isCorrect);
    answer.setPointsEarned(isCorrect ? question.getPoints() : BigDecimal.ZERO);
    return true;
  }

  private boolean gradeShortAnswer(Answer answer, Question question) {
    if (answer.getAnswerText() == null || question.getCorrectAnswer() == null) {
      return false;
    }
    String studentAnswer = answer.getAnswerText().trim().toLowerCase();
    String correctAnswer = question.getCorrectAnswer().trim().toLowerCase();
    boolean isCorrect = studentAnswer.equals(correctAnswer);
    answer.setIsCorrect(isCorrect);
    answer.setPointsEarned(isCorrect ? question.getPoints() : BigDecimal.ZERO);
    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public GradingSubmissionResponse getSubmissionForGrading(UUID submissionId) {
    log.info("Getting submission for grading: {}", submissionId);

    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    return mapToGradingResponse(submission);
  }

  @Override
  @Transactional
  public GradingSubmissionResponse completeGrading(CompleteGradingRequest request) {
    log.info("Completing grading for submission: {}", request.getSubmissionId());

    Submission submission =
        submissionRepository
            .findById(request.getSubmissionId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
      throw new AppException(ErrorCode.SUBMISSION_ALREADY_GRADED);
    }

    UUID teacherId = SecurityUtils.getCurrentUserId();

    validateGradingAccess(submission.getAssessmentId(), teacherId);

    BigDecimal totalScore = submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO;

    for (ManualGradeRequest gradeRequest : request.getGrades()) {
      Answer answer =
          answerRepository
              .findById(gradeRequest.getAnswerId())
              .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

      if (!answer.getSubmissionId().equals(request.getSubmissionId())) {
        throw new AppException(ErrorCode.ANSWER_SUBMISSION_MISMATCH);
      }

      BigDecimal oldPoints = answer.getPointsEarned();
      answer.setPointsEarned(gradeRequest.getPointsEarned());
      answer.setFeedback(gradeRequest.getFeedback());

      Question question =
          questionRepository
              .findById(answer.getQuestionId())
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

      answer.setIsCorrect(gradeRequest.getPointsEarned().compareTo(question.getPoints()) == 0);
      answerRepository.save(answer);

      if (oldPoints != null) {
        totalScore = totalScore.subtract(oldPoints);
      }
      totalScore = totalScore.add(gradeRequest.getPointsEarned());
    }

    submission.setScore(totalScore);

    if (submission.getMaxScore() != null
        && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal percentage =
          totalScore
              .divide(submission.getMaxScore(), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
      submission.setPercentage(percentage);
    }

    BigDecimal finalScore = totalScore;
    if (submission.getManualAdjustment() != null) {
      finalScore = finalScore.add(submission.getManualAdjustment());
    }
    submission.setFinalScore(finalScore);
    submission.setStatus(SubmissionStatus.GRADED);
    // Fix #15: always stamp the grader who actually completed grading (not the auto-grader)
    submission.setGradedBy(teacherId);
    submission.setGradedAt(Instant.now());

    submissionRepository.save(submission);

    // Fix #17: back-fill latest QuizAttempt with score so scoring-policy has per-attempt data
    final BigDecimal attemptScore = totalScore;
    final BigDecimal attemptMax = submission.getMaxScore();
    quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submission.getId()).stream()
        .findFirst()
        .ifPresent(
            attempt -> {
              attempt.setScore(attemptScore);
              attempt.setMaxScore(attemptMax);
              if (attemptMax != null && attemptMax.compareTo(BigDecimal.ZERO) > 0) {
                attempt.setPercentage(
                    attemptScore
                        .divide(attemptMax, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP));
              }
              attempt.setStatus(SubmissionStatus.GRADED);
              quizAttemptRepository.save(attempt);
            });

    log.info(
        "Grading completed for submission: {}. Final score: {}",
        request.getSubmissionId(),
        finalScore);

    return mapToGradingResponse(submission);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<GradingSubmissionResponse> getGradingQueue(Pageable pageable) {
    log.info("Getting grading queue");
    return submissionRepository
        .findByStatus(SubmissionStatus.SUBMITTED, pageable)
        .map(this::mapToGradingResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<GradingSubmissionResponse> getGradingQueueByTeacher(
      UUID teacherId, Pageable pageable) {
    log.info("Getting grading queue for teacher: {}", teacherId);
    return submissionRepository
        .findByTeacherIdAndStatus(teacherId, SubmissionStatus.SUBMITTED, pageable)
        .map(this::mapToGradingResponse);
  }

  @Override
  @Transactional
  public void gradeMultipleSubmissions(UUID teacherId, CompleteGradingRequest... requests) {
    log.info("Batch grading {} submissions by teacher: {}", requests.length, teacherId);
    for (CompleteGradingRequest request : requests) {
      try {
        completeGrading(request);
      } catch (Exception e) {
        log.error("Error grading submission: {}", request.getSubmissionId(), e);
      }
    }
  }

  @Override
  @Transactional
  public void overrideGrade(GradeOverrideRequest request) {
    log.info("Overriding grade for answer: {}", request.getAnswerId());

    Answer answer =
        answerRepository
            .findById(request.getAnswerId())
            .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

    Submission submission =
        submissionRepository
            .findById(answer.getSubmissionId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    UUID teacherId = SecurityUtils.getCurrentUserId();

    validateGradingAccess(submission.getAssessmentId(), teacherId);

    GradeAuditLog auditLog =
        GradeAuditLog.builder()
            .submissionId(submission.getId())
            .answerId(answer.getId())
            .teacherId(teacherId)
            .oldPoints(answer.getPointsEarned())
            .newPoints(request.getNewPoints())
            .reason(request.getReason())
            .build();
    gradeAuditLogRepository.save(auditLog);

    BigDecimal oldPoints =
        answer.getPointsEarned() != null ? answer.getPointsEarned() : BigDecimal.ZERO;
    answer.setPointsEarned(request.getNewPoints());
    answerRepository.save(answer);

    BigDecimal scoreDiff = request.getNewPoints().subtract(oldPoints);
    BigDecimal newScore =
        (submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO).add(scoreDiff);
    submission.setScore(newScore);

    if (submission.getMaxScore() != null
        && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal percentage =
          newScore
              .divide(submission.getMaxScore(), 4, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100))
              .setScale(2, RoundingMode.HALF_UP);
      submission.setPercentage(percentage);
    }

    BigDecimal finalScore = newScore;
    if (submission.getManualAdjustment() != null) {
      finalScore = finalScore.add(submission.getManualAdjustment());
    }
    submission.setFinalScore(finalScore);
    // Fix #15: update grader identity so audit trail is not stale
    submission.setGradedBy(teacherId);
    submission.setGradedAt(Instant.now());
    submissionRepository.save(submission);

    log.info(
        "Grade overridden for answer: {}. Old: {}, New: {}",
        request.getAnswerId(),
        oldPoints,
        request.getNewPoints());
  }

  @Override
  @Transactional
  public void addManualAdjustment(ManualAdjustmentRequest request) {
    log.info("Adding manual adjustment to submission: {}", request.getSubmissionId());

    Submission submission =
        submissionRepository
            .findById(request.getSubmissionId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (submission.getStatus() == SubmissionStatus.IN_PROGRESS) {
      throw new AppException(ErrorCode.SUBMISSION_NOT_GRADED);
    }

    UUID teacherId = SecurityUtils.getCurrentUserId();
    validateGradingAccess(submission.getAssessmentId(), teacherId);

    BigDecimal baseScore = submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO;
    BigDecimal proposedFinal = baseScore.add(request.getAdjustmentAmount());
    if (submission.getMaxScore() != null && proposedFinal.compareTo(submission.getMaxScore()) > 0) {
      proposedFinal = submission.getMaxScore();
      log.warn(
          "Manual adjustment for submission {} capped at maxScore {}",
          request.getSubmissionId(),
          submission.getMaxScore());
    }

    GradeAuditLog auditLog =
        GradeAuditLog.builder()
            .submissionId(submission.getId())
            .teacherId(teacherId)
            .oldPoints(submission.getManualAdjustment())
            .newPoints(request.getAdjustmentAmount())
            .reason(request.getReason())
            .build();
    gradeAuditLogRepository.save(auditLog);

    submission.setManualAdjustment(request.getAdjustmentAmount());
    submission.setManualAdjustmentReason(request.getReason());
    submission.setFinalScore(proposedFinal);
    // Fix #15: update grader identity so audit trail reflects who made the adjustment
    submission.setGradedBy(teacherId);
    submission.setGradedAt(Instant.now());
    submissionRepository.save(submission);

    log.info(
        "Manual adjustment added to submission: {}. Amount: {}, Final: {}",
        request.getSubmissionId(),
        request.getAdjustmentAmount(),
        proposedFinal);
  }

  @Override
  @Transactional(readOnly = true)
  public GradingAnalyticsResponse getGradingAnalytics(UUID assessmentId) {
    log.info("Getting grading analytics for assessment: {}", assessmentId);

    List<Submission> submissions = submissionRepository.findAllByAssessmentId(assessmentId);

    if (submissions.isEmpty()) {
      return GradingAnalyticsResponse.builder()
          .totalSubmissions(0L)
          .gradedSubmissions(0L)
          .pendingSubmissions(0L)
          .averageScore(BigDecimal.ZERO)
          .medianScore(BigDecimal.ZERO)
          .highestScore(BigDecimal.ZERO)
          .lowestScore(BigDecimal.ZERO)
          .passRate(0.0)
          .scoreDistribution(Map.of())
          .averageTimeSpentSeconds(0L)
          .build();
    }

    Long totalSubmissions = (long) submissions.size();
    Long gradedSubmissions =
        submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.GRADED).count();
    Long pendingSubmissions =
        submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED).count();

    List<BigDecimal> scores =
        submissions.stream()
            .filter(s -> s.getFinalScore() != null)
            .map(Submission::getFinalScore)
            .sorted()
            .collect(Collectors.toList());

    BigDecimal averageScore =
        scores.isEmpty()
            ? BigDecimal.ZERO
            : scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

    BigDecimal medianScore =
        scores.isEmpty()
            ? BigDecimal.ZERO
            : scores.size() % 2 == 0
                ? scores
                    .get(scores.size() / 2 - 1)
                    .add(scores.get(scores.size() / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                : scores.get(scores.size() / 2);

    BigDecimal highestScore = scores.isEmpty() ? BigDecimal.ZERO : scores.get(scores.size() - 1);
    BigDecimal lowestScore = scores.isEmpty() ? BigDecimal.ZERO : scores.get(0);

    Assessment assessment =
        assessmentRepository
            .findById(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    double passRate = 0.0;
    if (assessment.getPassingScore() != null && !scores.isEmpty()) {
      long passedCount =
          scores.stream()
              .filter(score -> score.compareTo(assessment.getPassingScore()) >= 0)
              .count();
      passRate = (double) passedCount / scores.size() * 100;
    }

    // Score distribution theo thang điểm Việt Nam (0-10)
    Map<String, Long> scoreDistribution = new LinkedHashMap<>();
    scoreDistribution.put(
        "0-2", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(2)) <= 0).count());
    scoreDistribution.put(
        "2-4",
        scores.stream()
            .filter(
                s ->
                    s.compareTo(BigDecimal.valueOf(2)) > 0
                        && s.compareTo(BigDecimal.valueOf(4)) <= 0)
            .count());
    scoreDistribution.put(
        "4-6",
        scores.stream()
            .filter(
                s ->
                    s.compareTo(BigDecimal.valueOf(4)) > 0
                        && s.compareTo(BigDecimal.valueOf(6)) <= 0)
            .count());
    scoreDistribution.put(
        "6-8",
        scores.stream()
            .filter(
                s ->
                    s.compareTo(BigDecimal.valueOf(6)) > 0
                        && s.compareTo(BigDecimal.valueOf(8)) <= 0)
            .count());
    scoreDistribution.put(
        "8-10", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(8)) > 0).count());

    Long averageTimeSpent =
        (long)
            submissions.stream()
                .filter(s -> s.getTimeSpentSeconds() != null)
                .mapToLong(Submission::getTimeSpentSeconds)
                .average()
                .orElse(0.0);

    return GradingAnalyticsResponse.builder()
        .totalSubmissions(totalSubmissions)
        .gradedSubmissions(gradedSubmissions)
        .pendingSubmissions(pendingSubmissions)
        .averageScore(averageScore)
        .medianScore(medianScore)
        .highestScore(highestScore)
        .lowestScore(lowestScore)
        .passRate(passRate)
        .scoreDistribution(scoreDistribution)
        .averageTimeSpentSeconds(averageTimeSpent)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public String exportGrades(UUID assessmentId) {
    log.info("Exporting grades for assessment: {}", assessmentId);

    List<Submission> submissions = submissionRepository.findAllByAssessmentId(assessmentId);

    StringBuilder csv = new StringBuilder();
    csv.append(
        "Student Name,Student Email,Score,Percentage,Final Score,Time Taken (seconds),Submitted At,Status\n");

    for (Submission submission : submissions) {
      User student = userRepository.findById(submission.getStudentId()).orElse(null);
      if (student == null) continue;

      csv.append(
          String.format(
              "%s,%s,%s,%s,%s,%s,%s,%s\n",
              escapeCsv(student.getFullName()),
              escapeCsv(student.getEmail()),
              submission.getScore() != null ? submission.getScore() : "0",
              submission.getPercentage() != null ? submission.getPercentage() : "0",
              submission.getFinalScore() != null ? submission.getFinalScore() : "0",
              submission.getTimeSpentSeconds() != null ? submission.getTimeSpentSeconds() : "0",
              submission.getSubmittedAt() != null ? submission.getSubmittedAt() : "",
              submission.getStatus()));
    }

    return csv.toString();
  }

  private String escapeCsv(String value) {
    if (value == null) return "";
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  @Override
  @Transactional
  public void releaseGrades(UUID assessmentId) {
    log.info("Releasing grades for assessment: {}", assessmentId);

    UUID teacherId = SecurityUtils.getCurrentUserId();

    validateGradingAccess(assessmentId, teacherId);

    List<Submission> submissions = submissionRepository.findAllByAssessmentId(assessmentId);

    long released = 0;
    long skipped = 0;
    for (Submission submission : submissions) {
      if (submission.getStatus() == SubmissionStatus.GRADED) {
        submission.setGradesReleased(true);
        released++;
      } else {
        skipped++;
      }
    }

    submissionRepository.saveAll(submissions);

    log.info(
        "Released {} submission(s), skipped {} ungraded submission(s) in assessment: {}",
        released,
        skipped,
        assessmentId);
  }

  @Override
  @Transactional
  public void releaseGradesForSubmission(UUID submissionId) {
    log.info("Releasing grades for submission: {}", submissionId);

    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (submission.getStatus() != SubmissionStatus.GRADED) {
      throw new AppException(ErrorCode.SUBMISSION_NOT_GRADED);
    }

    submission.setGradesReleased(true);
    submissionRepository.save(submission);
  }

  @Override
  @Transactional
  public RegradeRequestResponse createRegradeRequest(RegradeRequestCreationRequest request) {
    log.info(
        "Creating regrade request for submission: {}, question: {}",
        request.getSubmissionId(),
        request.getQuestionId());

    UUID studentId = SecurityUtils.getCurrentUserId();

    Submission submission =
        submissionRepository
            .findById(request.getSubmissionId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (!submission.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    if (!submission.getGradesReleased()) {
      throw new AppException(ErrorCode.GRADES_NOT_RELEASED);
    }

    // Fix #12: enforce a 7-day regrade appeal window measured from submission time
    if (submission.getSubmittedAt() != null) {
      Instant deadline = submission.getSubmittedAt().plus(java.time.Duration.ofDays(7));
      if (Instant.now().isAfter(deadline)) {
        throw new AppException(ErrorCode.REGRADE_DEADLINE_PASSED);
      }
    }

    if (regradeRequestRepository.existsPendingRequest(
        request.getSubmissionId(), request.getQuestionId(), studentId)) {
      throw new AppException(ErrorCode.REGRADE_REQUEST_ALREADY_PENDING);
    }

    RegradeRequest regradeRequest =
        RegradeRequest.builder()
            .submissionId(request.getSubmissionId())
            .questionId(request.getQuestionId())
            .studentId(studentId)
            .reason(request.getReason())
            .build();

    regradeRequest = regradeRequestRepository.save(regradeRequest);

    return mapToRegradeRequestResponse(regradeRequest);
  }

  @Override
  @Transactional
  public RegradeRequestResponse respondToRegradeRequest(RegradeResponseRequest request) {
    log.info("Responding to regrade request: {}", request.getRequestId());

    UUID teacherId = SecurityUtils.getCurrentUserId(); // Fix #12

    RegradeRequest regradeRequest =
        regradeRequestRepository
            .findById(request.getRequestId())
            .orElseThrow(() -> new AppException(ErrorCode.REGRADE_REQUEST_NOT_FOUND));

    if (regradeRequest.getStatus() != RegradeRequestStatus.PENDING) {
      throw new AppException(ErrorCode.REGRADE_REQUEST_NOT_PENDING);
    }

    regradeRequest.setStatus(request.getStatus());
    regradeRequest.setTeacherResponse(request.getTeacherResponse());
    regradeRequest.setReviewedBy(teacherId);
    regradeRequest.setReviewedAt(Instant.now());
    regradeRequest = regradeRequestRepository.save(regradeRequest);

    if (request.getStatus() == RegradeRequestStatus.APPROVED && request.getNewPoints() != null) {
      Submission submission =
          submissionRepository
              .findById(regradeRequest.getSubmissionId())
              .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

      Answer answer =
          answerRepository
              .findBySubmissionIdAndQuestionId(
                  regradeRequest.getSubmissionId(), regradeRequest.getQuestionId())
              .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

      GradeAuditLog auditLog =
          GradeAuditLog.builder()
              .submissionId(submission.getId())
              .answerId(answer.getId())
              .teacherId(teacherId)
              .oldPoints(answer.getPointsEarned())
              .newPoints(request.getNewPoints())
              .reason("Regrade approved: " + request.getTeacherResponse())
              .build();
      gradeAuditLogRepository.save(auditLog);

      BigDecimal oldPoints =
          answer.getPointsEarned() != null ? answer.getPointsEarned() : BigDecimal.ZERO;
      answer.setPointsEarned(request.getNewPoints());
      answerRepository.save(answer);

      BigDecimal scoreDiff = request.getNewPoints().subtract(oldPoints);
      BigDecimal newScore =
          (submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO).add(scoreDiff);
      submission.setScore(newScore);

      if (submission.getMaxScore() != null
          && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
        submission.setPercentage(
            newScore
                .divide(submission.getMaxScore(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP));
      }

      BigDecimal finalScore = newScore;
      if (submission.getManualAdjustment() != null) {
        finalScore = finalScore.add(submission.getManualAdjustment());
      }
      submission.setFinalScore(finalScore);
      submissionRepository.save(submission);

      log.info(
          "Regrade approved — answer {} updated from {} to {}",
          answer.getId(),
          oldPoints,
          request.getNewPoints());
    }

    return mapToRegradeRequestResponse(regradeRequest);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RegradeRequestResponse> getRegradeRequests(Pageable pageable) {
    log.info("Getting all regrade requests");
    return regradeRequestRepository
        .findByStatus(RegradeRequestStatus.PENDING, pageable)
        .map(this::mapToRegradeRequestResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RegradeRequestResponse> getStudentRegradeRequests(UUID studentId, Pageable pageable) {
    log.info("Getting regrade requests for student: {}", studentId);
    return regradeRequestRepository
        .findByStudentId(studentId, pageable)
        .map(this::mapToRegradeRequestResponse);
  }

  // ── Fix #4: Invalidate Submission ────────────────────────────────────────────────
  @Override
  @Transactional
  public GradingSubmissionResponse invalidateSubmission(UUID submissionId, String reason) {
    log.info("Invalidating submission: {}", submissionId);

    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (submission.getStatus() == SubmissionStatus.INVALIDATED) {
      throw new AppException(ErrorCode.SUBMISSION_ALREADY_INVALIDATED);
    }
    if (submission.getStatus() == SubmissionStatus.IN_PROGRESS) {
      throw new AppException(ErrorCode.SUBMISSION_INVALIDATION_BLOCKED);
    }

    UUID teacherId = SecurityUtils.getCurrentUserId();
    validateGradingAccess(submission.getAssessmentId(), teacherId);

    // Write an audit log so there is a permanent record of the invalidation
    GradeAuditLog auditLog =
        GradeAuditLog.builder()
            .submissionId(submission.getId())
            .teacherId(teacherId)
            .oldPoints(submission.getFinalScore())
            .newPoints(BigDecimal.ZERO)
            .reason("INVALIDATED: " + (reason != null ? reason : "no reason provided"))
            .build();
    gradeAuditLogRepository.save(auditLog);

    submission.setStatus(SubmissionStatus.INVALIDATED);
    submission.setManualAdjustmentReason("INVALIDATED: " + (reason != null ? reason : ""));
    submission.setGradedBy(teacherId);
    submission.setGradedAt(Instant.now());
    submissionRepository.save(submission);

    log.info("Submission {} invalidated by teacher {}", submissionId, teacherId);
    return mapToGradingResponse(submission);
  }

  // ── Fix #5 / #6 / #7: Student result gated by gradesReleased ─────────────────────
  @Override
  @Transactional(readOnly = true)
  public GradingSubmissionResponse getMyResult(UUID submissionId) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    // Ownership check
    if (!submission.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ATTEMPT_ACCESS_DENIED);
    }

    // Submission must be at least SUBMITTED
    if (submission.getStatus() == SubmissionStatus.IN_PROGRESS) {
      throw new AppException(ErrorCode.SUBMISSION_NOT_GRADED);
    }

    Assessment assessment =
        assessmentRepository
            .findById(submission.getAssessmentId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    // Fix #5 / #7: enforce gradesReleased gate unless showScoreImmediately bypasses it
    boolean gradesReleased = Boolean.TRUE.equals(submission.getGradesReleased());
    boolean immediateMode = Boolean.TRUE.equals(assessment.getShowScoreImmediately());

    if (!gradesReleased && !immediateMode) {
      throw new AppException(ErrorCode.SUBMISSION_RESULT_NOT_AVAILABLE);
    }

    GradingSubmissionResponse response = mapToGradingResponse(submission);

    // Fix #7: hide correctAnswer when showCorrectAnswers = false
    if (!Boolean.TRUE.equals(assessment.getShowCorrectAnswers()) && response.getAnswers() != null) {
      response.getAnswers().forEach(a -> a.setCorrectAnswer(null));
    }

    return response;
  }

  // ── Fix #13: Trigger AI review ────────────────────────────────────────────────────
  @Override
  @Transactional
  public void triggerAiReview(UUID submissionId) {
    log.info("Triggering AI review for submission: {}", submissionId);

    Submission submission =
        submissionRepository
            .findById(submissionId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (submission.getStatus() != SubmissionStatus.GRADED) {
      throw new AppException(ErrorCode.SUBMISSION_NOT_GRADED);
    }

    // Idempotency: skip if an overall review already exists
    if (aiReviewRepository.existsBySubmissionId(submissionId)) {
      log.info("AI review already exists for submission {}, skipping", submissionId);
      return;
    }

    List<Answer> answers = answerRepository.findBySubmissionId(submissionId);

    // Per-question review stubs — actual AI content to be populated async
    for (Answer answer : answers) {
      AiReview perQuestion =
          AiReview.builder()
              .submissionId(submissionId)
              .answerId(answer.getId())
              .reviewType(AiReviewType.QUESTION_SPECIFIC)
              .aiModel("gemini")
              .reviewContent("Review pending — AI generation in progress.")
              .build();
      aiReviewRepository.save(perQuestion);
    }

    // Overall review stub
    AiReview overall =
        AiReview.builder()
            .submissionId(submissionId)
            .reviewType(AiReviewType.OVERALL)
            .aiModel("gemini")
            .reviewContent("Overall review pending — AI generation in progress.")
            .build();
    aiReviewRepository.save(overall);

    log.info(
        "AI review stubs created for submission {} ({} per-question + 1 overall)",
        submissionId,
        answers.size());
  }

  // ── Fix #16: Count pending subjective submissions for dashboard notification ──────
  @Override
  @Transactional(readOnly = true)
  public Long countPendingSubjectiveSubmissions(UUID teacherId) {
    return submissionRepository.countByTeacherIdAndStatus(teacherId, SubmissionStatus.SUBMITTED);
  }

  private void validateGradingAccess(UUID assessmentId, UUID callerId) {
    if (SecurityUtils.hasRole("ADMIN")) return;

    Assessment assessment =
        assessmentRepository
            .findById(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (!assessment.getTeacherId().equals(callerId)) {
      throw new AppException(ErrorCode.GRADING_ACCESS_DENIED);
    }
  }

  /**
   * Fix #10: build the grading response with batch-loaded questions and audit-log flags
   * to eliminate N+1 queries (was: 2 DB round-trips per answer).
   */
  private GradingSubmissionResponse mapToGradingResponse(Submission submission) {
    User student = userRepository.findById(submission.getStudentId()).orElse(null);
    Assessment assessment =
        assessmentRepository.findById(submission.getAssessmentId()).orElse(null);

    List<Answer> answers = answerRepository.findBySubmissionId(submission.getId());

    Set<UUID> questionIds = answers.stream().map(Answer::getQuestionId).collect(Collectors.toSet());
    Map<UUID, Question> questionMap =
        questionRepository.findAllById(questionIds).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));

    Set<UUID> manuallyAdjustedAnswerIds =
        gradeAuditLogRepository.findBySubmissionId(submission.getId()).stream()
            .filter(log -> log.getAnswerId() != null)
            .map(GradeAuditLog::getAnswerId)
            .collect(Collectors.toSet());

    long pendingCount = answers.stream().filter(a -> a.getPointsEarned() == null).count();
    long autoGradedCount = answers.stream().filter(a -> a.getPointsEarned() != null).count();

    List<AnswerGradeResponse> answerResponses =
        answers.stream()
            .map(
                answer -> {
                  Question question = questionMap.get(answer.getQuestionId());
                  return AnswerGradeResponse.builder()
                      .answerId(answer.getId())
                      .questionId(answer.getQuestionId())
                      .questionText(question != null ? question.getQuestionText() : null)
                      .answerText(answer.getAnswerText())
                      // Fix #7: always populate correctAnswer here; getMyResult will null it if
                      // needed
                      .correctAnswer(question != null ? question.getCorrectAnswer() : null)
                      .isCorrect(answer.getIsCorrect())
                        .pointsEarned(
                          answer.getPointsEarned() != null ? answer.getPointsEarned() : BigDecimal.ZERO)
                        .maxPoints(
                          question != null && question.getPoints() != null
                            ? question.getPoints()
                            : BigDecimal.ZERO)
                      .feedback(answer.getFeedback())
                      .isManuallyAdjusted(manuallyAdjustedAnswerIds.contains(answer.getId()))
                      .gradedAt(answer.getUpdatedAt())
                      .build();
                })
            .collect(Collectors.toList());

    Integer attemptNumber = quizAttemptRepository.countBySubmissionId(submission.getId());

    return GradingSubmissionResponse.builder()
        .submissionId(submission.getId())
        .assessmentId(submission.getAssessmentId())
        .assessmentTitle(assessment != null ? assessment.getTitle() : null)
        .studentId(submission.getStudentId())
        .studentName(student != null ? student.getFullName() : null)
        .studentEmail(student != null ? student.getEmail() : null)
        .status(submission.getStatus())
        .submittedAt(submission.getSubmittedAt())
        .score(submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO)
        .maxScore(submission.getMaxScore() != null ? submission.getMaxScore() : BigDecimal.ZERO)
        .percentage(submission.getPercentage() != null ? submission.getPercentage() : BigDecimal.ZERO)
        .manualAdjustment(
          submission.getManualAdjustment() != null ? submission.getManualAdjustment() : BigDecimal.ZERO)
        .manualAdjustmentReason(submission.getManualAdjustmentReason())
        .finalScore(submission.getFinalScore() != null ? submission.getFinalScore() : BigDecimal.ZERO)
        .timeSpentSeconds(submission.getTimeSpentSeconds() != null ? submission.getTimeSpentSeconds() : 0)
        .attemptNumber(attemptNumber)
        .pendingQuestionsCount(pendingCount)
        .autoGradedQuestionsCount(autoGradedCount)
        .answers(answerResponses)
        .gradesReleased(submission.getGradesReleased())
        .gradedAt(submission.getGradedAt())
        .build();
  }

  private RegradeRequestResponse mapToRegradeRequestResponse(RegradeRequest request) {
    User student = userRepository.findById(request.getStudentId()).orElse(null);
    User reviewer =
        request.getReviewedBy() != null
            ? userRepository.findById(request.getReviewedBy()).orElse(null)
            : null;
    Question question = questionRepository.findById(request.getQuestionId()).orElse(null);

    return RegradeRequestResponse.builder()
        .id(request.getId())
        .submissionId(request.getSubmissionId())
        .questionId(request.getQuestionId())
        .questionText(question != null ? question.getQuestionText() : null)
        .studentId(request.getStudentId())
        .studentName(student != null ? student.getFullName() : null)
        .reason(request.getReason())
        .status(request.getStatus())
        .teacherResponse(request.getTeacherResponse())
        .reviewedBy(request.getReviewedBy())
        .reviewerName(reviewer != null ? reviewer.getFullName() : null)
        .reviewedAt(request.getReviewedAt())
        .createdAt(request.getCreatedAt())
        .build();
  }
}
