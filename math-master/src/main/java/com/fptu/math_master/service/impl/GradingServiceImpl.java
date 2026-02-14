package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.*;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.GradingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class GradingServiceImpl implements GradingService {

    SubmissionRepository submissionRepository;
    AnswerRepository answerRepository;
    QuestionRepository questionRepository;
    AssessmentRepository assessmentRepository;
    UserRepository userRepository;
    GradeAuditLogRepository gradeAuditLogRepository;
    RegradeRequestRepository regradeRequestRepository;
    QuizAttemptRepository quizAttemptRepository;

    // FR-GR-001: Auto-Grade Objective Questions
    @Override
    @Transactional
    public void autoGradeSubmission(UUID submissionId) {
        log.info("Auto-grading submission: {}", submissionId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
            log.warn("Submission {} is not in SUBMITTED status, skipping auto-grade", submissionId);
            return;
        }

        List<Answer> answers = answerRepository.findBySubmissionId(submissionId);
        boolean hasSubjectiveQuestions = false;
        BigDecimal totalScore = BigDecimal.ZERO;

        for (Answer answer : answers) {
            Question question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

            boolean autoGraded = autoGradeAnswer(answer, question);
            if (!autoGraded) {
                hasSubjectiveQuestions = true;
            }

            if (answer.getPointsEarned() != null) {
                totalScore = totalScore.add(answer.getPointsEarned());
            }

            answerRepository.save(answer);
        }

        // Update submission score
        submission.setScore(totalScore);

        // Calculate percentage
        if (submission.getMaxScore() != null && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = totalScore
                    .divide(submission.getMaxScore(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            submission.setPercentage(percentage);
        }

        // Calculate final score (score + manual adjustment)
        BigDecimal finalScore = totalScore;
        if (submission.getManualAdjustment() != null) {
            finalScore = finalScore.add(submission.getManualAdjustment());
        }
        submission.setFinalScore(finalScore);

        // Set status to GRADED only if no subjective questions
        if (!hasSubjectiveQuestions) {
            submission.setStatus(SubmissionStatus.GRADED);
            submission.setGradedAt(Instant.now());
        }

        submissionRepository.save(submission);

        log.info("Auto-grading completed for submission: {}. Status: {}, Score: {}",
                submissionId, submission.getStatus(), totalScore);
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
                // Cannot auto-grade, needs manual review
                return false;
            default:
                return false;
        }
    }

    private boolean gradeMultipleChoice(Answer answer, Question question) {
        if (answer.getAnswerData() == null || question.getCorrectAnswer() == null) {
            return false;
        }

        Object selected = answer.getAnswerData().get("selected");
        boolean isCorrect = selected != null && selected.toString().equals(question.getCorrectAnswer());

        answer.setIsCorrect(isCorrect);
        answer.setPointsEarned(isCorrect ? question.getPoints() : BigDecimal.ZERO);

        return true;
    }

    private boolean gradeTrueFalse(Answer answer, Question question) {
        if (answer.getAnswerData() == null || question.getCorrectAnswer() == null) {
            return false;
        }

        Object selected = answer.getAnswerData().get("value");
        boolean isCorrect = selected != null && selected.toString().equalsIgnoreCase(question.getCorrectAnswer());

        answer.setIsCorrect(isCorrect);
        answer.setPointsEarned(isCorrect ? question.getPoints() : BigDecimal.ZERO);

        return true;
    }

    private boolean gradeShortAnswer(Answer answer, Question question) {
        // Simple keyword matching - can be enhanced
        if (answer.getAnswerText() == null || question.getCorrectAnswer() == null) {
            answer.setIsCorrect(false);
            answer.setPointsEarned(BigDecimal.ZERO);
            return false;
        }

        String studentAnswer = answer.getAnswerText().trim().toLowerCase();
        String correctAnswer = question.getCorrectAnswer().trim().toLowerCase();

        boolean isCorrect = studentAnswer.equals(correctAnswer);

        answer.setIsCorrect(isCorrect);
        answer.setPointsEarned(isCorrect ? question.getPoints() : BigDecimal.ZERO);

        // Mark as auto-graded but may need manual review
        return false; // Return false to indicate it needs manual verification
    }

    // FR-GR-002: Manual Grade Subjective Questions
    @Override
    @Transactional(readOnly = true)
    public GradingSubmissionResponse getSubmissionForGrading(UUID submissionId) {
        log.info("Getting submission for grading: {}", submissionId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        return mapToGradingResponse(submission);
    }

    @Override
    @Transactional
    public GradingSubmissionResponse completeGrading(CompleteGradingRequest request) {
        log.info("Completing grading for submission: {}", request.getSubmissionId());

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        UUID teacherId = getCurrentUserId();
        BigDecimal totalScore = submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO;

        // Grade each answer
        for (ManualGradeRequest gradeRequest : request.getGrades()) {
            Answer answer = answerRepository.findById(gradeRequest.getAnswerId())
                    .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

            // Update answer
            BigDecimal oldPoints = answer.getPointsEarned();
            answer.setPointsEarned(gradeRequest.getPointsEarned());
            answer.setFeedback(gradeRequest.getFeedback());

            Question question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

            answer.setIsCorrect(gradeRequest.getPointsEarned().compareTo(question.getPoints()) == 0);

            answerRepository.save(answer);

            // Adjust total score
            if (oldPoints != null) {
                totalScore = totalScore.subtract(oldPoints);
            }
            totalScore = totalScore.add(gradeRequest.getPointsEarned());
        }

        // Update submission
        submission.setScore(totalScore);

        // Calculate percentage
        if (submission.getMaxScore() != null && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = totalScore
                    .divide(submission.getMaxScore(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            submission.setPercentage(percentage);
        }

        // Calculate final score
        BigDecimal finalScore = totalScore;
        if (submission.getManualAdjustment() != null) {
            finalScore = finalScore.add(submission.getManualAdjustment());
        }
        submission.setFinalScore(finalScore);

        submission.setStatus(SubmissionStatus.GRADED);
        submission.setGradedBy(teacherId);
        submission.setGradedAt(Instant.now());

        submissionRepository.save(submission);

        log.info("Grading completed for submission: {}. Final score: {}", request.getSubmissionId(), finalScore);

        return mapToGradingResponse(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GradingSubmissionResponse> getGradingQueue(Pageable pageable) {
        log.info("Getting grading queue");

        Page<Submission> submissions = submissionRepository.findByStatus(SubmissionStatus.SUBMITTED, pageable);

        return submissions.map(this::mapToGradingResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GradingSubmissionResponse> getGradingQueueByTeacher(UUID teacherId, Pageable pageable) {
        log.info("Getting grading queue for teacher: {}", teacherId);

        Page<Submission> submissions = submissionRepository.findByTeacherIdAndStatus(
                teacherId, SubmissionStatus.SUBMITTED, pageable);

        return submissions.map(this::mapToGradingResponse);
    }

    // FR-GR-003: Batch Grading
    @Override
    @Transactional
    public void gradeMultipleSubmissions(UUID teacherId, CompleteGradingRequest... requests) {
        log.info("Batch grading {} submissions by teacher: {}", requests.length, teacherId);

        for (CompleteGradingRequest request : requests) {
            try {
                completeGrading(request);
            } catch (Exception e) {
                log.error("Error grading submission: {}", request.getSubmissionId(), e);
                // Continue with next submission
            }
        }
    }

    // FR-GR-004: Grade Override
    @Override
    @Transactional
    public void overrideGrade(GradeOverrideRequest request) {
        log.info("Overriding grade for answer: {}", request.getAnswerId());

        Answer answer = answerRepository.findById(request.getAnswerId())
                .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

        Submission submission = submissionRepository.findById(answer.getSubmissionId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        UUID teacherId = getCurrentUserId();

        // Create audit log
        GradeAuditLog auditLog = GradeAuditLog.builder()
                .submissionId(submission.getId())
                .answerId(answer.getId())
                .teacherId(teacherId)
                .oldPoints(answer.getPointsEarned())
                .newPoints(request.getNewPoints())
                .reason(request.getReason())
                .build();

        gradeAuditLogRepository.save(auditLog);

        // Update answer
        BigDecimal oldPoints = answer.getPointsEarned() != null ? answer.getPointsEarned() : BigDecimal.ZERO;
        answer.setPointsEarned(request.getNewPoints());
        answerRepository.save(answer);

        // Recalculate submission score
        BigDecimal scoreDiff = request.getNewPoints().subtract(oldPoints);
        BigDecimal newScore = submission.getScore().add(scoreDiff);
        submission.setScore(newScore);

        // Recalculate percentage
        if (submission.getMaxScore() != null && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = newScore
                    .divide(submission.getMaxScore(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            submission.setPercentage(percentage);
        }

        // Recalculate final score
        BigDecimal finalScore = newScore;
        if (submission.getManualAdjustment() != null) {
            finalScore = finalScore.add(submission.getManualAdjustment());
        }
        submission.setFinalScore(finalScore);

        submissionRepository.save(submission);

        log.info("Grade overridden for answer: {}. Old: {}, New: {}",
                request.getAnswerId(), oldPoints, request.getNewPoints());
    }

    // FR-GR-005: Add Manual Grade to Submission
    @Override
    @Transactional
    public void addManualAdjustment(ManualAdjustmentRequest request) {
        log.info("Adding manual adjustment to submission: {}", request.getSubmissionId());

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        UUID teacherId = getCurrentUserId();

        // Create audit log
        GradeAuditLog auditLog = GradeAuditLog.builder()
                .submissionId(submission.getId())
                .teacherId(teacherId)
                .oldPoints(submission.getManualAdjustment())
                .newPoints(request.getAdjustmentAmount())
                .reason(request.getReason())
                .build();

        gradeAuditLogRepository.save(auditLog);

        // Update submission
        submission.setManualAdjustment(request.getAdjustmentAmount());
        submission.setManualAdjustmentReason(request.getReason());

        // Recalculate final score
        BigDecimal finalScore = submission.getScore() != null ? submission.getScore() : BigDecimal.ZERO;
        finalScore = finalScore.add(request.getAdjustmentAmount());
        submission.setFinalScore(finalScore);

        submissionRepository.save(submission);

        log.info("Manual adjustment added to submission: {}. Amount: {}",
                request.getSubmissionId(), request.getAdjustmentAmount());
    }

    // FR-GR-006: View Grading Analytics
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
                    .build();
        }

        Long totalSubmissions = (long) submissions.size();
        Long gradedSubmissions = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                .count();
        Long pendingSubmissions = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED)
                .count();

        List<BigDecimal> scores = submissions.stream()
                .filter(s -> s.getFinalScore() != null)
                .map(Submission::getFinalScore)
                .sorted()
                .collect(Collectors.toList());

        BigDecimal averageScore = scores.isEmpty() ? BigDecimal.ZERO :
                scores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);

        BigDecimal medianScore = scores.isEmpty() ? BigDecimal.ZERO :
                scores.size() % 2 == 0 ?
                        scores.get(scores.size() / 2 - 1).add(scores.get(scores.size() / 2))
                                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP) :
                        scores.get(scores.size() / 2);

        BigDecimal highestScore = scores.isEmpty() ? BigDecimal.ZERO : scores.get(scores.size() - 1);
        BigDecimal lowestScore = scores.isEmpty() ? BigDecimal.ZERO : scores.get(0);

        // Calculate pass rate
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

        double passRate = 0.0;
        if (assessment.getPassingScore() != null && !scores.isEmpty()) {
            long passedCount = scores.stream()
                    .filter(score -> score.compareTo(assessment.getPassingScore()) >= 0)
                    .count();
            passRate = (double) passedCount / scores.size() * 100;
        }

        // Score distribution
        Map<String, Long> scoreDistribution = new LinkedHashMap<>();
        scoreDistribution.put("0-20", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(20)) <= 0).count());
        scoreDistribution.put("21-40", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(20)) > 0 && s.compareTo(BigDecimal.valueOf(40)) <= 0).count());
        scoreDistribution.put("41-60", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(40)) > 0 && s.compareTo(BigDecimal.valueOf(60)) <= 0).count());
        scoreDistribution.put("61-80", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(60)) > 0 && s.compareTo(BigDecimal.valueOf(80)) <= 0).count());
        scoreDistribution.put("81-100", scores.stream().filter(s -> s.compareTo(BigDecimal.valueOf(80)) > 0).count());

        // Average time spent
        Long averageTimeSpent = (long) submissions.stream()
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

    // FR-GR-007: Export Grades
    @Override
    @Transactional(readOnly = true)
    public String exportGrades(UUID assessmentId) {
        log.info("Exporting grades for assessment: {}", assessmentId);

        List<Submission> submissions = submissionRepository.findAllByAssessmentId(assessmentId);

        StringBuilder csv = new StringBuilder();
        csv.append("Student Name,Student Email,Score,Percentage,Final Score,Time Taken (seconds),Submitted At,Status\n");

        for (Submission submission : submissions) {
            User student = userRepository.findById(submission.getStudentId()).orElse(null);
            if (student == null) continue;

            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
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

    // FR-GR-008: Release Grades
    @Override
    @Transactional
    public void releaseGrades(UUID assessmentId) {
        log.info("Releasing grades for assessment: {}", assessmentId);

        List<Submission> submissions = submissionRepository.findAllByAssessmentId(assessmentId);

        for (Submission submission : submissions) {
            if (submission.getStatus() == SubmissionStatus.GRADED) {
                submission.setGradesReleased(true);
            }
        }

        submissionRepository.saveAll(submissions);

        log.info("Released grades for {} submissions in assessment: {}", submissions.size(), assessmentId);
    }

    @Override
    @Transactional
    public void releaseGradesForSubmission(UUID submissionId) {
        log.info("Releasing grades for submission: {}", submissionId);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (submission.getStatus() == SubmissionStatus.GRADED) {
            submission.setGradesReleased(true);
            submissionRepository.save(submission);
        } else {
            throw new AppException(ErrorCode.SUBMISSION_NOT_GRADED);
        }
    }

    // FR-GR-009: Request Regrade
    @Override
    @Transactional
    public RegradeRequestResponse createRegradeRequest(RegradeRequestCreationRequest request) {
        log.info("Creating regrade request for submission: {}, question: {}",
                request.getSubmissionId(), request.getQuestionId());

        UUID studentId = getCurrentUserId();

        Submission submission = submissionRepository.findById(request.getSubmissionId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (!submission.getStudentId().equals(studentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!submission.getGradesReleased()) {
            throw new AppException(ErrorCode.GRADES_NOT_RELEASED);
        }

        RegradeRequest regradeRequest = RegradeRequest.builder()
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

        UUID teacherId = getCurrentUserId();

        RegradeRequest regradeRequest = regradeRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new AppException(ErrorCode.REGRADE_REQUEST_NOT_FOUND));

        regradeRequest.setStatus(request.getStatus());
        regradeRequest.setTeacherResponse(request.getTeacherResponse());
        regradeRequest.setReviewedBy(teacherId);
        regradeRequest.setReviewedAt(Instant.now());

        regradeRequest = regradeRequestRepository.save(regradeRequest);

        return mapToRegradeRequestResponse(regradeRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegradeRequestResponse> getRegradeRequests(Pageable pageable) {
        log.info("Getting all regrade requests");

        Page<RegradeRequest> requests = regradeRequestRepository.findByStatus(
                RegradeRequestStatus.PENDING, pageable);

        return requests.map(this::mapToRegradeRequestResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegradeRequestResponse> getStudentRegradeRequests(UUID studentId, Pageable pageable) {
        log.info("Getting regrade requests for student: {}", studentId);

        Page<RegradeRequest> requests = regradeRequestRepository.findByStudentId(studentId, pageable);

        return requests.map(this::mapToRegradeRequestResponse);
    }

    // Helper methods
    private UUID getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .map(User::getId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private GradingSubmissionResponse mapToGradingResponse(Submission submission) {
        User student = userRepository.findById(submission.getStudentId()).orElse(null);
        Assessment assessment = assessmentRepository.findById(submission.getAssessmentId()).orElse(null);

        List<Answer> answers = answerRepository.findBySubmissionId(submission.getId());

        long pendingCount = answers.stream()
                .filter(a -> a.getPointsEarned() == null)
                .count();

        long autoGradedCount = answers.stream()
                .filter(a -> a.getPointsEarned() != null)
                .count();

        List<AnswerGradeResponse> answerResponses = answers.stream()
                .map(this::mapToAnswerGradeResponse)
                .collect(Collectors.toList());

        // Get attempt number
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
                .score(submission.getScore())
                .maxScore(submission.getMaxScore())
                .percentage(submission.getPercentage())
                .manualAdjustment(submission.getManualAdjustment())
                .manualAdjustmentReason(submission.getManualAdjustmentReason())
                .finalScore(submission.getFinalScore())
                .timeSpentSeconds(submission.getTimeSpentSeconds())
                .attemptNumber(attemptNumber)
                .pendingQuestionsCount(pendingCount)
                .autoGradedQuestionsCount(autoGradedCount)
                .answers(answerResponses)
                .gradesReleased(submission.getGradesReleased())
                .gradedAt(submission.getGradedAt())
                .build();
    }

    private AnswerGradeResponse mapToAnswerGradeResponse(Answer answer) {
        Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);

        boolean isManuallyAdjusted = !gradeAuditLogRepository.findByAnswerId(answer.getId()).isEmpty();

        return AnswerGradeResponse.builder()
                .answerId(answer.getId())
                .questionId(answer.getQuestionId())
                .questionText(question != null ? question.getQuestionText() : null)
                .answerText(answer.getAnswerText())
                .isCorrect(answer.getIsCorrect())
                .pointsEarned(answer.getPointsEarned())
                .maxPoints(question != null ? question.getPoints() : null)
                .feedback(answer.getFeedback())
                .isManuallyAdjusted(isManuallyAdjusted)
                .gradedAt(answer.getUpdatedAt())
                .build();
    }

    private RegradeRequestResponse mapToRegradeRequestResponse(RegradeRequest request) {
        User student = userRepository.findById(request.getStudentId()).orElse(null);
        User reviewer = request.getReviewedBy() != null ?
                userRepository.findById(request.getReviewedBy()).orElse(null) : null;
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


