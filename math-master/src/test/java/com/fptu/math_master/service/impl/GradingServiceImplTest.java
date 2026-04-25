package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.GradingAnalyticsResponse;
import com.fptu.math_master.dto.response.GradingSubmissionResponse;
import com.fptu.math_master.dto.response.RegradeRequestResponse;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.*;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("GradingServiceImpl - Tests")
class GradingServiceImplTest extends BaseUnitTest {

  @InjectMocks private GradingServiceImpl gradingService;

  @Mock private SubmissionRepository submissionRepository;
  @Mock private AnswerRepository answerRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
  @Mock private AssessmentRepository assessmentRepository;
  @Mock private UserRepository userRepository;
  @Mock private GradeAuditLogRepository gradeAuditLogRepository;
  @Mock private RegradeRequestRepository regradeRequestRepository;
  @Mock private QuizAttemptRepository quizAttemptRepository;
  @Mock private AiReviewRepository aiReviewRepository;

  private UUID submissionId;
  private UUID assessmentId;
  private UUID teacherId;
  private UUID studentId;
  private UUID answerId;
  private UUID questionId;

  @BeforeEach
  void setUp() {
    submissionId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    assessmentId = UUID.fromString("00000000-0000-0000-0000-000000000102");
    teacherId = UUID.fromString("00000000-0000-0000-0000-000000000103");
    studentId = UUID.fromString("00000000-0000-0000-0000-000000000104");
    answerId = UUID.fromString("00000000-0000-0000-0000-000000000105");
    questionId = UUID.fromString("00000000-0000-0000-0000-000000000106");
  }

  private Submission buildSubmission(SubmissionStatus status) {
    Submission submission = new Submission();
    submission.setId(submissionId);
    submission.setAssessmentId(assessmentId);
    submission.setStudentId(studentId);
    submission.setStatus(status);
    submission.setMaxScore(new BigDecimal("10.00"));
    submission.setGradesReleased(Boolean.FALSE);
    submission.setSubmittedAt(Instant.now().minus(1, ChronoUnit.DAYS));
    return submission;
  }

  private Question buildQuestion(QuestionType type, String correctAnswer, BigDecimal points) {
    Question question = new Question();
    question.setId(questionId);
    question.setQuestionType(type);
    question.setCorrectAnswer(correctAnswer);
    question.setPoints(points);
    question.setQuestionText("Question text");
    return question;
  }

  private Answer buildAnswer(UUID id, UUID qId, String answerText, Map<String, Object> answerData) {
    Answer answer = new Answer();
    answer.setId(id);
    answer.setSubmissionId(submissionId);
    answer.setQuestionId(qId);
    answer.setAnswerText(answerText);
    answer.setAnswerData(answerData);
    return answer;
  }

  private Assessment buildAssessment(UUID ownerId) {
    Assessment assessment = new Assessment();
    assessment.setId(assessmentId);
    assessment.setTeacherId(ownerId);
    assessment.setTitle("Toan roi rac");
    assessment.setPassingScore(new BigDecimal("5.00"));
    assessment.setShowCorrectAnswers(Boolean.TRUE);
    assessment.setShowScoreImmediately(Boolean.FALSE);
    return assessment;
  }

  private void mockMinimalResponseMapping(Submission submission, List<Answer> answers) {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().fullName("Nguyen Van A").email("a@fpt.edu.vn").build()));
    when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));
    when(answerRepository.findBySubmissionId(submission.getId())).thenReturn(answers);
    when(questionRepository.findAllById(any())).thenReturn(List.of());
    when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(submission.getAssessmentId()))
        .thenReturn(List.of());
    when(gradeAuditLogRepository.findBySubmissionId(submission.getId())).thenReturn(List.of());
    when(quizAttemptRepository.countBySubmissionId(submission.getId())).thenReturn(1);
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) {
    try {
      var method = GradingServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return (T) method.invoke(gradingService, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private method: " + methodName, ex);
    }
  }

  @Nested
  @DisplayName("autoGradeSubmission()")
  class AutoGradeSubmissionTests {

    @Test
    void it_should_throw_exception_when_submission_not_found() {
      // ===== ARRANGE =====
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> gradingService.autoGradeSubmission(submissionId));
      assertEquals(ErrorCode.SUBMISSION_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(submissionRepository, times(1)).findById(submissionId);
      verifyNoMoreInteractions(submissionRepository, answerRepository);
    }

    @Test
    void it_should_return_when_submission_is_already_graded() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT =====
      gradingService.autoGradeSubmission(submissionId);

      // ===== ASSERT =====
      assertEquals(SubmissionStatus.GRADED, submission.getStatus());

      // ===== VERIFY =====
      verify(submissionRepository, times(1)).findById(submissionId);
      verify(answerRepository, never()).findBySubmissionId(any());
      verifyNoMoreInteractions(answerRepository, questionRepository);
    }

    @Test
    void it_should_return_when_submission_status_is_not_submitted() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.IN_PROGRESS);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT =====
      gradingService.autoGradeSubmission(submissionId);

      // ===== ASSERT =====
      assertEquals(SubmissionStatus.IN_PROGRESS, submission.getStatus());

      // ===== VERIFY =====
      verify(submissionRepository, times(1)).findById(submissionId);
      verify(answerRepository, never()).findBySubmissionId(any());
    }

    @Test
    void it_should_auto_grade_objective_answers_and_mark_submission_graded() {
      // ===== ARRANGE =====
      UUID q1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
      UUID q2 = UUID.fromString("00000000-0000-0000-0000-000000000202");
      UUID q3 = UUID.fromString("00000000-0000-0000-0000-000000000203");
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      submission.setManualAdjustment(new BigDecimal("1.00"));

      Answer mcqAnswer = buildAnswer(UUID.randomUUID(), q1, null, Map.of("selected", "A"));
      Answer tfAnswer = buildAnswer(UUID.randomUUID(), q2, null, Map.of("value", "đúng"));
      Answer shortAnswer = buildAnswer(UUID.randomUUID(), q3, "  Integral   ", Map.of());

      Question mcqQ = buildQuestion(QuestionType.MULTIPLE_CHOICE, "A", new BigDecimal("3.00"));
      mcqQ.setId(q1);
      Question tfQ = buildQuestion(QuestionType.TRUE_FALSE, "true", new BigDecimal("2.00"));
      tfQ.setId(q2);
      Question saQ = buildQuestion(QuestionType.SHORT_ANSWER, "integral", new BigDecimal("5.00"));
      saQ.setId(q3);

      QuizAttempt attempt = new QuizAttempt();
      attempt.setStatus(SubmissionStatus.SUBMITTED);

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(answerRepository.findBySubmissionId(submissionId)).thenReturn(List.of(mcqAnswer, tfAnswer, shortAnswer));
      when(questionRepository.findAllById(any())).thenReturn(List.of(mcqQ, tfQ, saQ));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId)).thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId)).thenReturn(List.of(attempt));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

        // ===== ACT =====
        gradingService.autoGradeSubmission(submissionId);
      }

      // ===== ASSERT =====
      assertEquals(new BigDecimal("10.00"), submission.getScore());
      assertEquals(new BigDecimal("100.00"), submission.getPercentage());
      assertEquals(new BigDecimal("11.00"), submission.getFinalScore());
      assertEquals(SubmissionStatus.GRADED, submission.getStatus());
      assertEquals(teacherId, submission.getGradedBy());
      assertEquals(SubmissionStatus.GRADED, attempt.getStatus());
      assertEquals(new BigDecimal("10.00"), attempt.getScore());

      // ===== VERIFY =====
      verify(answerRepository, times(3)).save(any(Answer.class));
      verify(submissionRepository, times(1)).save(submission);
      verify(quizAttemptRepository, times(1)).save(attempt);
    }

    @Test
    void it_should_keep_submission_submitted_when_has_subjective_or_missing_question() {
      // ===== ARRANGE =====
      UUID essayQId = UUID.fromString("00000000-0000-0000-0000-000000000301");
      UUID missingQId = UUID.fromString("00000000-0000-0000-0000-000000000302");
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      submission.setMaxScore(null);

      Answer essayAnswer =
          buildAnswer(UUID.randomUUID(), essayQId, "Long essay", Map.of("answer", "Long essay"));
      Answer missingAnswer =
          buildAnswer(UUID.randomUUID(), missingQId, "Unknown", Map.of("answer", "Unknown"));

      Question essay = buildQuestion(QuestionType.ESSAY, null, new BigDecimal("5.00"));
      essay.setId(essayQId);
      QuizAttempt draftAttempt = new QuizAttempt();
      draftAttempt.setStatus(SubmissionStatus.IN_PROGRESS);

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(answerRepository.findBySubmissionId(submissionId))
          .thenReturn(List.of(essayAnswer, missingAnswer));
      when(questionRepository.findAllById(any())).thenReturn(List.of(essay));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(List.of(draftAttempt));

      // ===== ACT =====
      gradingService.autoGradeSubmission(submissionId);

      // ===== ASSERT =====
      assertEquals(SubmissionStatus.SUBMITTED, submission.getStatus());
      assertNull(submission.getPercentage());
      assertEquals(BigDecimal.ZERO, submission.getScore());
      assertNull(submission.getGradedBy());
      verify(quizAttemptRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("completeGrading()")
  class CompleteGradingTests {

    @Test
    void it_should_throw_exception_when_submission_status_is_not_submitted() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      CompleteGradingRequest request =
          CompleteGradingRequest.builder().submissionId(submissionId).grades(List.of()).build();
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT & ASSERT =====
      AppException ex = assertThrows(AppException.class, () -> gradingService.completeGrading(request));
      assertEquals(ErrorCode.SUBMISSION_ALREADY_GRADED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(submissionRepository, times(1)).findById(submissionId);
      verify(answerRepository, never()).findById(any());
    }

    @Test
    void it_should_throw_exception_when_answer_submission_mismatch() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      Answer answer = new Answer();
      answer.setId(answerId);
      answer.setSubmissionId(UUID.randomUUID());

      ManualGradeRequest grade = ManualGradeRequest.builder().answerId(answerId).pointsEarned(new BigDecimal("2")).build();
      CompleteGradingRequest request =
          CompleteGradingRequest.builder().submissionId(submissionId).grades(List.of(grade)).build();

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId)).thenReturn(List.of());

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> gradingService.completeGrading(request));
        assertEquals(ErrorCode.ANSWER_SUBMISSION_MISMATCH, ex.getErrorCode());
      }

      // ===== VERIFY =====
      verify(answerRepository, times(1)).findById(answerId);
      verify(answerRepository, never()).save(any());
    }

    @Test
    void it_should_complete_grading_and_update_attempt() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      submission.setScore(new BigDecimal("1.00"));
      submission.setManualAdjustment(new BigDecimal("0.50"));

      Answer answer = new Answer();
      answer.setId(answerId);
      answer.setSubmissionId(submissionId);
      answer.setQuestionId(questionId);
      answer.setPointsEarned(new BigDecimal("1.00"));

      Question question = buildQuestion(QuestionType.SHORT_ANSWER, "42", new BigDecimal("4.00"));
      QuizAttempt attempt = new QuizAttempt();
      attempt.setStatus(SubmissionStatus.SUBMITTED);

      ManualGradeRequest grade =
          ManualGradeRequest.builder()
              .answerId(answerId)
              .pointsEarned(new BigDecimal("4.00"))
              .feedback("Good reasoning")
              .build();
      CompleteGradingRequest request =
          CompleteGradingRequest.builder().submissionId(submissionId).grades(List.of(grade)).build();

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
      when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId)).thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId)).thenReturn(List.of(attempt));
      mockMinimalResponseMapping(submission, List.of(answer));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT =====
        GradingSubmissionResponse response = gradingService.completeGrading(request);

        // ===== ASSERT =====
        assertNotNull(response);
      }

      assertEquals(new BigDecimal("4.00"), submission.getScore());
      assertEquals(new BigDecimal("40.00"), submission.getPercentage());
      assertEquals(new BigDecimal("4.50"), submission.getFinalScore());
      assertEquals(SubmissionStatus.GRADED, submission.getStatus());
      assertEquals(SubmissionStatus.GRADED, attempt.getStatus());

      // ===== VERIFY =====
      verify(answerRepository, times(1)).save(answer);
      verify(submissionRepository, times(1)).save(submission);
      verify(quizAttemptRepository, times(1)).save(attempt);
    }

    @Test
    void it_should_throw_exception_when_question_not_found_during_complete_grading() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      Answer answer = new Answer();
      answer.setId(answerId);
      answer.setSubmissionId(submissionId);
      answer.setQuestionId(questionId);

      ManualGradeRequest grade =
          ManualGradeRequest.builder().answerId(answerId).pointsEarned(new BigDecimal("2.00")).build();
      CompleteGradingRequest request =
          CompleteGradingRequest.builder().submissionId(submissionId).grades(List.of(grade)).build();

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
      when(questionRepository.findById(questionId)).thenReturn(Optional.empty());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId)).thenReturn(List.of());

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> gradingService.completeGrading(request));
        assertEquals(ErrorCode.QUESTION_NOT_FOUND, ex.getErrorCode());
      }
    }

    @Test
    void it_should_complete_grading_when_old_points_and_max_score_are_null() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      submission.setScore(null);
      submission.setMaxScore(null);
      submission.setManualAdjustment(null);

      Answer answer = new Answer();
      answer.setId(answerId);
      answer.setSubmissionId(submissionId);
      answer.setQuestionId(questionId);
      answer.setPointsEarned(null);

      Question question = buildQuestion(QuestionType.SHORT_ANSWER, "42", new BigDecimal("4.00"));
      ManualGradeRequest grade =
          ManualGradeRequest.builder().answerId(answerId).pointsEarned(new BigDecimal("1.50")).build();
      CompleteGradingRequest request =
          CompleteGradingRequest.builder().submissionId(submissionId).grades(List.of(grade)).build();

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
      when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId)).thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId)).thenReturn(List.of());
      mockMinimalResponseMapping(submission, List.of(answer));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT =====
        gradingService.completeGrading(request);
      }

      // ===== ASSERT =====
      assertEquals(new BigDecimal("1.50"), submission.getScore());
      assertNull(submission.getPercentage());
      assertEquals(new BigDecimal("1.50"), submission.getFinalScore());
    }
  }

  @Nested
  @DisplayName("release and result flow")
  class ReleaseAndResultTests {

    @Test
    void it_should_release_only_graded_submissions() {
      // ===== ARRANGE =====
      Submission graded = buildSubmission(SubmissionStatus.GRADED);
      Submission pending = buildSubmission(SubmissionStatus.SUBMITTED);
      when(submissionRepository.findAllByAssessmentId(assessmentId)).thenReturn(List.of(graded, pending));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT =====
        gradingService.releaseGrades(assessmentId);
      }

      // ===== ASSERT =====
      assertTrue(graded.getGradesReleased());
      assertNotEquals(Boolean.TRUE, pending.getGradesReleased());

      // ===== VERIFY =====
      verify(submissionRepository, times(1)).saveAll(List.of(graded, pending));
    }

    @Test
    void it_should_throw_exception_when_releasing_non_graded_submission() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class, () -> gradingService.releaseGradesForSubmission(submissionId));
      assertEquals(ErrorCode.SUBMISSION_NOT_GRADED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(submissionRepository, times(1)).findById(submissionId);
      verify(submissionRepository, never()).save(any());
    }

    @Test
    void it_should_hide_correct_answers_when_assessment_disables_them() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(true);
      Assessment assessment = buildAssessment(teacherId);
      assessment.setShowCorrectAnswers(false);

      Answer answer = buildAnswer(answerId, questionId, "student", Map.of());
      Question question = buildQuestion(QuestionType.SHORT_ANSWER, "teacher-key", new BigDecimal("1"));

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
      when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().fullName("Tran Thi B").email("b@fpt.edu.vn").build()));
      when(answerRepository.findBySubmissionId(submissionId)).thenReturn(List.of(answer));
      when(questionRepository.findAllById(any())).thenReturn(List.of(question));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId)).thenReturn(List.of());
      when(gradeAuditLogRepository.findBySubmissionId(submissionId)).thenReturn(List.of());
      when(quizAttemptRepository.countBySubmissionId(submissionId)).thenReturn(2);

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT =====
        GradingSubmissionResponse response = gradingService.getMyResult(submissionId);

        // ===== ASSERT =====
        assertNotNull(response);
        assertNull(response.getAnswers().get(0).getCorrectAnswer());
      }

      // ===== VERIFY =====
      verify(submissionRepository, times(1)).findById(submissionId);
    }

    @Test
    void it_should_throw_exception_when_student_tries_to_access_others_submission() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setStudentId(UUID.randomUUID());
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.getMyResult(submissionId));
        assertEquals(ErrorCode.ATTEMPT_ACCESS_DENIED, ex.getErrorCode());
      }
    }

    @Test
    void it_should_throw_exception_when_result_not_available_and_not_immediate_mode() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(false);
      Assessment assessment = buildAssessment(teacherId);
      assessment.setShowScoreImmediately(false);

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.getMyResult(submissionId));
        assertEquals(ErrorCode.SUBMISSION_RESULT_NOT_AVAILABLE, ex.getErrorCode());
      }
    }

    @Test
    void it_should_release_single_submission_when_status_is_graded() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT =====
      gradingService.releaseGradesForSubmission(submissionId);

      // ===== ASSERT =====
      assertTrue(submission.getGradesReleased());
      verify(submissionRepository, times(1)).save(submission);
    }
  }

  @Nested
  @DisplayName("regrade flow")
  class RegradeFlowTests {

    @Test
    void it_should_create_regrade_request_when_valid_and_inside_window() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(true);
      submission.setSubmittedAt(Instant.now().minus(2, ChronoUnit.DAYS));

      RegradeRequestCreationRequest request =
          RegradeRequestCreationRequest.builder()
              .submissionId(submissionId)
              .questionId(questionId)
              .reason("Please review partial credit")
              .build();

      RegradeRequest saved = new RegradeRequest();
      saved.setId(UUID.fromString("00000000-0000-0000-0000-000000000500"));
      saved.setSubmissionId(submissionId);
      saved.setQuestionId(questionId);
      saved.setStudentId(studentId);
      saved.setReason("Please review partial credit");
      saved.setStatus(RegradeRequestStatus.PENDING);

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(regradeRequestRepository.existsPendingRequest(submissionId, questionId, studentId)).thenReturn(false);
      when(regradeRequestRepository.save(any(RegradeRequest.class))).thenReturn(saved);
      when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().fullName("Le C").build()));
      when(questionRepository.findById(questionId)).thenReturn(Optional.of(buildQuestion(QuestionType.SHORT_ANSWER, "x", new BigDecimal("2"))));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT =====
        RegradeRequestResponse response = gradingService.createRegradeRequest(request);

        // ===== ASSERT =====
        assertNotNull(response);
        assertEquals(RegradeRequestStatus.PENDING, response.getStatus());
      }

      // ===== VERIFY =====
      verify(regradeRequestRepository, times(1)).save(any(RegradeRequest.class));
    }

    @Test
    void it_should_create_regrade_request_when_submitted_at_is_null() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(true);
      submission.setSubmittedAt(null);

      RegradeRequestCreationRequest request =
          RegradeRequestCreationRequest.builder()
              .submissionId(submissionId)
              .questionId(questionId)
              .reason("No submittedAt should bypass deadline check")
              .build();

      RegradeRequest saved = new RegradeRequest();
      saved.setId(UUID.randomUUID());
      saved.setSubmissionId(submissionId);
      saved.setQuestionId(questionId);
      saved.setStudentId(studentId);
      saved.setReason(request.getReason());
      saved.setStatus(RegradeRequestStatus.PENDING);

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(regradeRequestRepository.existsPendingRequest(submissionId, questionId, studentId))
          .thenReturn(false);
      when(regradeRequestRepository.save(any(RegradeRequest.class))).thenReturn(saved);
      when(userRepository.findById(studentId))
          .thenReturn(Optional.of(User.builder().fullName("Le C").build()));
      when(questionRepository.findById(questionId))
          .thenReturn(Optional.of(buildQuestion(QuestionType.SHORT_ANSWER, "x", BigDecimal.ONE)));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT =====
        RegradeRequestResponse response = gradingService.createRegradeRequest(request);

        // ===== ASSERT =====
        assertNotNull(response);
        assertEquals(RegradeRequestStatus.PENDING, response.getStatus());
      }
    }

    @Test
    void it_should_throw_exception_when_regrade_deadline_passed() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(true);
      submission.setSubmittedAt(Instant.now().minus(8, ChronoUnit.DAYS));

      RegradeRequestCreationRequest request =
          RegradeRequestCreationRequest.builder()
              .submissionId(submissionId)
              .questionId(questionId)
              .reason("Late appeal")
              .build();
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.createRegradeRequest(request));
        assertEquals(ErrorCode.REGRADE_DEADLINE_PASSED, ex.getErrorCode());
      }

      // ===== VERIFY =====
      verify(regradeRequestRepository, never()).save(any());
    }

    @Test
    void it_should_throw_exception_when_submission_belongs_to_another_student() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(true);
      submission.setStudentId(UUID.randomUUID());
      RegradeRequestCreationRequest request =
          RegradeRequestCreationRequest.builder()
              .submissionId(submissionId)
              .questionId(questionId)
              .reason("Not owner")
              .build();
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.createRegradeRequest(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
      }
    }

    @Test
    void it_should_throw_exception_when_grades_not_released_for_regrade() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(false);
      RegradeRequestCreationRequest request =
          RegradeRequestCreationRequest.builder()
              .submissionId(submissionId)
              .questionId(questionId)
              .reason("Scores hidden")
              .build();
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.createRegradeRequest(request));
        assertEquals(ErrorCode.GRADES_NOT_RELEASED, ex.getErrorCode());
      }
    }

    @Test
    void it_should_throw_exception_when_pending_regrade_already_exists() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setGradesReleased(true);
      RegradeRequestCreationRequest request =
          RegradeRequestCreationRequest.builder()
              .submissionId(submissionId)
              .questionId(questionId)
              .reason("Duplicate")
              .build();
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(regradeRequestRepository.existsPendingRequest(submissionId, questionId, studentId))
          .thenReturn(true);

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.createRegradeRequest(request));
        assertEquals(ErrorCode.REGRADE_REQUEST_ALREADY_PENDING, ex.getErrorCode());
      }
    }

    @Test
    void it_should_approve_regrade_and_update_submission_score() {
      // ===== ARRANGE =====
      UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000510");
      RegradeRequest regradeRequest = new RegradeRequest();
      regradeRequest.setId(requestId);
      regradeRequest.setSubmissionId(submissionId);
      regradeRequest.setQuestionId(questionId);
      regradeRequest.setStudentId(studentId);
      regradeRequest.setStatus(RegradeRequestStatus.PENDING);

      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setScore(new BigDecimal("6.00"));
      submission.setMaxScore(new BigDecimal("10.00"));
      submission.setManualAdjustment(new BigDecimal("1.00"));

      Answer answer = buildAnswer(answerId, questionId, "old", Map.of());
      answer.setPointsEarned(new BigDecimal("2.00"));

      RegradeResponseRequest request =
          RegradeResponseRequest.builder()
              .requestId(requestId)
              .status(RegradeRequestStatus.APPROVED)
              .teacherResponse("Accepted")
              .newPoints(new BigDecimal("4.00"))
              .build();

      when(regradeRequestRepository.findById(requestId)).thenReturn(Optional.of(regradeRequest));
      when(regradeRequestRepository.save(any(RegradeRequest.class))).thenReturn(regradeRequest);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(answerRepository.findBySubmissionIdAndQuestionId(submissionId, questionId)).thenReturn(Optional.of(answer));
      when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().fullName("Le C").build()));
      when(questionRepository.findById(questionId)).thenReturn(Optional.of(buildQuestion(QuestionType.SHORT_ANSWER, "x", new BigDecimal("2"))));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

        // ===== ACT =====
        RegradeRequestResponse response = gradingService.respondToRegradeRequest(request);

        // ===== ASSERT =====
        assertNotNull(response);
        assertEquals(RegradeRequestStatus.APPROVED, response.getStatus());
      }

      assertEquals(new BigDecimal("8.00"), submission.getScore());
      assertEquals(new BigDecimal("80.00"), submission.getPercentage());
      assertEquals(new BigDecimal("9.00"), submission.getFinalScore());
      verify(gradeAuditLogRepository, times(1)).save(any(GradeAuditLog.class));
      verify(answerRepository, times(1)).save(answer);
      verify(submissionRepository, times(1)).save(submission);
    }

    @Test
    void it_should_throw_exception_when_regrade_request_is_not_pending() {
      // ===== ARRANGE =====
      UUID requestId = UUID.randomUUID();
      RegradeRequest regradeRequest = new RegradeRequest();
      regradeRequest.setId(requestId);
      regradeRequest.setStatus(RegradeRequestStatus.APPROVED);
      when(regradeRequestRepository.findById(requestId)).thenReturn(Optional.of(regradeRequest));

      RegradeResponseRequest request =
          RegradeResponseRequest.builder()
              .requestId(requestId)
              .status(RegradeRequestStatus.REJECTED)
              .teacherResponse("Already processed")
              .build();

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.respondToRegradeRequest(request));
        assertEquals(ErrorCode.REGRADE_REQUEST_NOT_PENDING, ex.getErrorCode());
      }
    }

    @Test
    void it_should_update_request_only_when_regrade_is_rejected() {
      // ===== ARRANGE =====
      UUID requestId = UUID.randomUUID();
      RegradeRequest existing = new RegradeRequest();
      existing.setId(requestId);
      existing.setSubmissionId(submissionId);
      existing.setQuestionId(questionId);
      existing.setStudentId(studentId);
      existing.setStatus(RegradeRequestStatus.PENDING);

      RegradeResponseRequest request =
          RegradeResponseRequest.builder()
              .requestId(requestId)
              .status(RegradeRequestStatus.REJECTED)
              .teacherResponse("No grading error found")
              .newPoints(null)
              .build();

      when(regradeRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
      when(regradeRequestRepository.save(any(RegradeRequest.class))).thenReturn(existing);
      when(userRepository.findById(studentId))
          .thenReturn(Optional.of(User.builder().fullName("Le C").build()));
      when(questionRepository.findById(questionId))
          .thenReturn(Optional.of(buildQuestion(QuestionType.SHORT_ANSWER, "x", BigDecimal.ONE)));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

        // ===== ACT =====
        RegradeRequestResponse response = gradingService.respondToRegradeRequest(request);

        // ===== ASSERT =====
        assertNotNull(response);
        assertEquals(RegradeRequestStatus.REJECTED, response.getStatus());
        assertNotNull(response.getReviewedAt());
      }

      // ===== VERIFY =====
      verify(submissionRepository, never()).findById(any());
      verify(answerRepository, never()).findBySubmissionIdAndQuestionId(any(), any());
      verify(gradeAuditLogRepository, never()).save(any());
    }

    @Test
    void it_should_only_mark_reviewed_when_approved_without_new_points() {
      // ===== ARRANGE =====
      UUID requestId = UUID.randomUUID();
      RegradeRequest existing = new RegradeRequest();
      existing.setId(requestId);
      existing.setSubmissionId(submissionId);
      existing.setQuestionId(questionId);
      existing.setStudentId(studentId);
      existing.setStatus(RegradeRequestStatus.PENDING);

      RegradeResponseRequest request =
          RegradeResponseRequest.builder()
              .requestId(requestId)
              .status(RegradeRequestStatus.APPROVED)
              .teacherResponse("Approved but no score change")
              .newPoints(null)
              .build();

      when(regradeRequestRepository.findById(requestId)).thenReturn(Optional.of(existing));
      when(regradeRequestRepository.save(any(RegradeRequest.class))).thenReturn(existing);
      when(userRepository.findById(studentId))
          .thenReturn(Optional.of(User.builder().fullName("Le C").build()));
      when(questionRepository.findById(questionId))
          .thenReturn(Optional.of(buildQuestion(QuestionType.SHORT_ANSWER, "x", BigDecimal.ONE)));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

        // ===== ACT =====
        RegradeRequestResponse response = gradingService.respondToRegradeRequest(request);

        // ===== ASSERT =====
        assertEquals(RegradeRequestStatus.APPROVED, response.getStatus());
      }

      // ===== VERIFY =====
      verify(submissionRepository, never()).findById(any());
      verify(answerRepository, never()).findBySubmissionIdAndQuestionId(any(), any());
      verify(gradeAuditLogRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("analytics and export")
  class AnalyticsAndExportTests {

    @Test
    void it_should_return_zero_analytics_when_no_submission_exists() {
      // ===== ARRANGE =====
      when(submissionRepository.findAllByAssessmentId(assessmentId)).thenReturn(List.of());

      // ===== ACT =====
      GradingAnalyticsResponse response = gradingService.getGradingAnalytics(assessmentId);

      // ===== ASSERT =====
      assertNotNull(response);
      assertEquals(0L, response.getTotalSubmissions());
      assertEquals(BigDecimal.ZERO, response.getAverageScore());

      // ===== VERIFY =====
      verify(assessmentRepository, never()).findById(any());
    }

    @Test
    void it_should_compute_analytics_with_median_and_distribution() {
      // ===== ARRANGE =====
      Submission s1 = buildSubmission(SubmissionStatus.GRADED);
      s1.setFinalScore(new BigDecimal("4.00"));
      s1.setTimeSpentSeconds(200);
      Submission s2 = buildSubmission(SubmissionStatus.GRADED);
      s2.setFinalScore(new BigDecimal("6.00"));
      s2.setTimeSpentSeconds(400);
      Submission s3 = buildSubmission(SubmissionStatus.SUBMITTED);
      s3.setFinalScore(new BigDecimal("8.00"));
      s3.setTimeSpentSeconds(600);
      when(submissionRepository.findAllByAssessmentId(assessmentId)).thenReturn(List.of(s1, s2, s3));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));

      // ===== ACT =====
      GradingAnalyticsResponse response = gradingService.getGradingAnalytics(assessmentId);

      // ===== ASSERT =====
      assertEquals(3L, response.getTotalSubmissions());
      assertEquals(2L, response.getGradedSubmissions());
      assertEquals(1L, response.getPendingSubmissions());
      assertEquals(new BigDecimal("6.00"), response.getMedianScore());
      assertEquals(66.66666666666666, response.getPassRate());
      assertEquals(400L, response.getAverageTimeSpentSeconds());
      assertEquals(1L, response.getScoreDistribution().get("2-4"));
      assertEquals(1L, response.getScoreDistribution().get("4-6"));
      assertEquals(1L, response.getScoreDistribution().get("6-8"));
    }

    @Test
    void it_should_compute_even_median_and_zero_pass_rate_when_no_passing_score() {
      // ===== ARRANGE =====
      Submission s1 = buildSubmission(SubmissionStatus.GRADED);
      s1.setFinalScore(new BigDecimal("2.00"));
      Submission s2 = buildSubmission(SubmissionStatus.GRADED);
      s2.setFinalScore(new BigDecimal("8.00"));
      Assessment assessment = buildAssessment(teacherId);
      assessment.setPassingScore(null);

      when(submissionRepository.findAllByAssessmentId(assessmentId)).thenReturn(List.of(s1, s2));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

      // ===== ACT =====
      GradingAnalyticsResponse response = gradingService.getGradingAnalytics(assessmentId);

      // ===== ASSERT =====
      assertEquals(new BigDecimal("5.00"), response.getMedianScore());
      assertEquals(0.0, response.getPassRate());
      assertEquals(new BigDecimal("2.00"), response.getLowestScore());
      assertEquals(new BigDecimal("8.00"), response.getHighestScore());
    }

    @Test
    void it_should_export_grades_as_csv_and_skip_missing_user() {
      // ===== ARRANGE =====
      Submission keep = buildSubmission(SubmissionStatus.GRADED);
      keep.setStudentId(studentId);
      keep.setScore(new BigDecimal("8.00"));
      keep.setPercentage(new BigDecimal("80.00"));
      keep.setFinalScore(new BigDecimal("8.50"));
      keep.setTimeSpentSeconds(1250);
      keep.setSubmittedAt(Instant.parse("2026-04-20T10:15:30Z"));
      Submission skipped = buildSubmission(SubmissionStatus.GRADED);
      skipped.setStudentId(UUID.randomUUID());

      User student = User.builder().fullName("Le \"Thi\" D").email("student@fpt.edu.vn").build();

      when(submissionRepository.findAllByAssessmentId(assessmentId)).thenReturn(List.of(keep, skipped));
      when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
      when(userRepository.findById(skipped.getStudentId())).thenReturn(Optional.empty());

      // ===== ACT =====
      String csv = gradingService.exportGrades(assessmentId);

      // ===== ASSERT =====
      assertTrue(csv.startsWith("Student Name,Student Email"));
      assertTrue(csv.contains("\"Le \"\"Thi\"\" D\""));
      assertTrue(csv.contains("\"student@fpt.edu.vn\""));
      assertFalse(csv.contains(skipped.getStudentId().toString()));
    }

    @Test
    void it_should_export_csv_with_empty_name_when_student_full_name_is_null() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setStudentId(studentId);
      User student = User.builder().fullName(null).email("nullname@fpt.edu.vn").build();

      when(submissionRepository.findAllByAssessmentId(assessmentId)).thenReturn(List.of(submission));
      when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

      // ===== ACT =====
      String csv = gradingService.exportGrades(assessmentId);

      // ===== ASSERT =====
      assertTrue(csv.contains(",\"nullname@fpt.edu.vn\""));
    }
  }

  @Nested
  @DisplayName("misc grading operations")
  class MiscTests {

    @Test
    void it_should_override_grade_and_recompute_submission_fields() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setScore(new BigDecimal("7.00"));
      submission.setMaxScore(new BigDecimal("10.00"));
      submission.setManualAdjustment(new BigDecimal("1.00"));

      Answer answer = buildAnswer(answerId, questionId, "old", Map.of());
      answer.setPointsEarned(new BigDecimal("2.00"));

      GradeOverrideRequest request =
          GradeOverrideRequest.builder()
              .answerId(answerId)
              .newPoints(new BigDecimal("4.00"))
              .reason("Rubric correction")
              .build();

      when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT =====
        gradingService.overrideGrade(request);
      }

      // ===== ASSERT =====
      assertEquals(new BigDecimal("9.00"), submission.getScore());
      assertEquals(new BigDecimal("90.00"), submission.getPercentage());
      assertEquals(new BigDecimal("10.00"), submission.getFinalScore());
      assertEquals(teacherId, submission.getGradedBy());

      // ===== VERIFY =====
      verify(gradeAuditLogRepository, times(1)).save(any(GradeAuditLog.class));
      verify(answerRepository, times(1)).save(answer);
      verify(submissionRepository, times(1)).save(submission);
    }

    @Test
    void it_should_throw_exception_when_override_grade_access_denied_for_non_owner_teacher() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      Answer answer = buildAnswer(answerId, questionId, "old", Map.of());
      GradeOverrideRequest request =
          GradeOverrideRequest.builder()
              .answerId(answerId)
              .newPoints(new BigDecimal("4.00"))
              .reason("No permission")
              .build();
      Assessment assessment = buildAssessment(UUID.randomUUID());

      when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> gradingService.overrideGrade(request));
        assertEquals(ErrorCode.GRADING_ACCESS_DENIED, ex.getErrorCode());
      }
    }

    @Test
    void it_should_override_grade_when_old_points_score_and_max_score_are_null() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setScore(null);
      submission.setMaxScore(null);
      submission.setManualAdjustment(null);

      Answer answer = buildAnswer(answerId, questionId, "old", Map.of());
      answer.setPointsEarned(null);

      GradeOverrideRequest request =
          GradeOverrideRequest.builder()
              .answerId(answerId)
              .newPoints(new BigDecimal("2.50"))
              .reason("Manual correction")
              .build();

      when(answerRepository.findById(answerId)).thenReturn(Optional.of(answer));
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT =====
        gradingService.overrideGrade(request);
      }

      // ===== ASSERT =====
      assertEquals(new BigDecimal("2.50"), submission.getScore());
      assertNull(submission.getPercentage());
      assertEquals(new BigDecimal("2.50"), submission.getFinalScore());
    }

    @Test
    void it_should_cap_manual_adjustment_when_proposed_final_exceeds_max_score() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setScore(new BigDecimal("8.00"));
      submission.setMaxScore(new BigDecimal("10.00"));

      ManualAdjustmentRequest request =
          ManualAdjustmentRequest.builder()
              .submissionId(submissionId)
              .adjustmentAmount(new BigDecimal("5.00"))
              .reason("Bonus policy")
              .build();

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT =====
        gradingService.addManualAdjustment(request);
      }

      // ===== ASSERT =====
      assertEquals(new BigDecimal("10.00"), submission.getFinalScore());
      assertEquals(new BigDecimal("5.00"), submission.getManualAdjustment());
      assertEquals("Bonus policy", submission.getManualAdjustmentReason());
      verify(gradeAuditLogRepository, times(1)).save(any(GradeAuditLog.class));
    }

    @Test
    void it_should_throw_exception_when_invalidating_in_progress_submission() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.IN_PROGRESS);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> gradingService.invalidateSubmission(submissionId, "suspected cheating"));
      assertEquals(ErrorCode.SUBMISSION_INVALIDATION_BLOCKED, ex.getErrorCode());
    }

    @Test
    void it_should_invalidate_submission_and_write_audit_log() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      submission.setFinalScore(new BigDecimal("7.00"));
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));
      mockMinimalResponseMapping(submission, List.of());

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        securityUtils.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

        // ===== ACT =====
        GradingSubmissionResponse response =
            gradingService.invalidateSubmission(submissionId, "duplicate account");

        // ===== ASSERT =====
        assertNotNull(response);
      }

      assertEquals(SubmissionStatus.INVALIDATED, submission.getStatus());
      assertTrue(submission.getManualAdjustmentReason().contains("INVALIDATED"));
      verify(gradeAuditLogRepository, times(1)).save(any(GradeAuditLog.class));
      verify(submissionRepository, times(1)).save(submission);
    }

    @Test
    void it_should_create_ai_review_stubs_when_submission_graded() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      Answer a1 = buildAnswer(UUID.randomUUID(), questionId, "ans1", Map.of());
      Answer a2 = buildAnswer(UUID.randomUUID(), UUID.randomUUID(), "ans2", Map.of());

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(aiReviewRepository.existsBySubmissionId(submissionId)).thenReturn(false);
      when(answerRepository.findBySubmissionId(submissionId)).thenReturn(List.of(a1, a2));

      // ===== ACT =====
      gradingService.triggerAiReview(submissionId);

      // ===== ASSERT =====
      verify(aiReviewRepository, times(3)).save(any(AiReview.class));
    }

    @Test
    void it_should_skip_ai_review_creation_when_already_exists() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.GRADED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(aiReviewRepository.existsBySubmissionId(submissionId)).thenReturn(true);

      // ===== ACT =====
      gradingService.triggerAiReview(submissionId);

      // ===== ASSERT + VERIFY =====
      verify(answerRepository, never()).findBySubmissionId(any());
      verify(aiReviewRepository, never()).save(any());
    }

    @Test
    void it_should_return_regrade_pending_page() {
      // ===== ARRANGE =====
      RegradeRequest request = new RegradeRequest();
      request.setId(UUID.fromString("00000000-0000-0000-0000-000000000777"));
      request.setSubmissionId(submissionId);
      request.setQuestionId(questionId);
      request.setStudentId(studentId);
      request.setStatus(RegradeRequestStatus.PENDING);
      Page<RegradeRequest> page = new PageImpl<>(List.of(request));
      when(regradeRequestRepository.findByStatus(eq(RegradeRequestStatus.PENDING), any(PageRequest.class))).thenReturn(page);
      when(userRepository.findById(studentId)).thenReturn(Optional.of(User.builder().fullName("Nguyen Van A").build()));
      when(questionRepository.findById(questionId)).thenReturn(Optional.of(buildQuestion(QuestionType.SHORT_ANSWER, "x", BigDecimal.ONE)));

      // ===== ACT =====
      Page<RegradeRequestResponse> result = gradingService.getRegradeRequests(PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertEquals(1, result.getTotalElements());
      assertEquals(RegradeRequestStatus.PENDING, result.getContent().get(0).getStatus());
    }

    @Test
    void it_should_return_count_for_pending_subjective_submissions() {
      // ===== ARRANGE =====
      when(submissionRepository.countByTeacherIdAndStatus(teacherId, SubmissionStatus.SUBMITTED))
          .thenReturn(12L);

      // ===== ACT =====
      Long count = gradingService.countPendingSubjectiveSubmissions(teacherId);

      // ===== ASSERT =====
      assertEquals(12L, count);
    }

    @Test
    void it_should_throw_exception_when_trigger_ai_review_on_non_graded_submission() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> gradingService.triggerAiReview(submissionId));
      assertEquals(ErrorCode.SUBMISSION_NOT_GRADED, ex.getErrorCode());
    }

    @Test
    void it_should_process_batch_grading_even_when_one_request_fails() {
      // ===== ARRANGE =====
      GradingServiceImpl spyService = Mockito.spy(gradingService);
      CompleteGradingRequest r1 =
          CompleteGradingRequest.builder().submissionId(UUID.randomUUID()).grades(List.of()).build();
      CompleteGradingRequest r2 =
          CompleteGradingRequest.builder().submissionId(UUID.randomUUID()).grades(List.of()).build();

      doThrow(new RuntimeException("boom")).when(spyService).completeGrading(r1);
      doReturn(new GradingSubmissionResponse()).when(spyService).completeGrading(r2);

      // ===== ACT =====
      spyService.gradeMultipleSubmissions(teacherId, r1, r2);

      // ===== ASSERT + VERIFY =====
      verify(spyService, times(1)).completeGrading(r1);
      verify(spyService, times(1)).completeGrading(r2);
    }

    @Test
    void it_should_throw_exception_when_manual_adjustment_called_for_in_progress_submission() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.IN_PROGRESS);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      ManualAdjustmentRequest request =
          ManualAdjustmentRequest.builder()
              .submissionId(submissionId)
              .adjustmentAmount(new BigDecimal("1.00"))
              .reason("Need bonus")
              .build();

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> gradingService.addManualAdjustment(request));
      assertEquals(ErrorCode.SUBMISSION_NOT_GRADED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_invalidating_already_invalidated_submission() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.INVALIDATED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> gradingService.invalidateSubmission(submissionId, "again"));
      assertEquals(ErrorCode.SUBMISSION_ALREADY_INVALIDATED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("result guard branches")
  class ResultGuardTests {

    @Test
    void it_should_throw_exception_when_my_result_requested_for_in_progress_submission() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.IN_PROGRESS);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(studentId);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> gradingService.getMyResult(submissionId));
        assertEquals(ErrorCode.SUBMISSION_NOT_GRADED, ex.getErrorCode());
      }
    }
  }

  @Nested
  @DisplayName("private helper branch coverage")
  class PrivateHelperTests {

    @Test
    void it_should_cover_object_to_single_value_and_normalization_variants() {
      // ===== ARRANGE =====
      Answer arrayAnswer =
          buildAnswer(
              UUID.randomUUID(),
              questionId,
              null,
              Map.of("selectedOption", new String[] {"  \"A\"  ", "B"}));
      Question mcq = buildQuestion(QuestionType.MULTIPLE_CHOICE, "A", new BigDecimal("1"));

      // ===== ACT =====
      Boolean autoGraded =
          invokePrivate(
              "autoGradeAnswer",
              new Class[] {Answer.class, Question.class, BigDecimal.class},
              arrayAnswer,
              mcq,
              new BigDecimal("1"));
      String normalizedUnknown =
          invokePrivate("normalizeTrueFalseToken", new Class[] {String.class}, "maybe");
      String normalizedNull =
          invokePrivate("normalizeToken", new Class[] {String.class}, "   ");
      String freeText =
          invokePrivate("normalizeFreeText", new Class[] {String.class}, "  Multi   Word   Text ");

      // ===== ASSERT =====
      assertTrue(autoGraded);
      assertEquals("maybe", normalizedUnknown);
      assertNull(normalizedNull);
      assertEquals("multi word text", freeText);
    }

    @Test
    void it_should_return_false_for_unsupported_or_invalid_auto_grade_paths() {
      // ===== ARRANGE =====
      Answer essayAnswer = buildAnswer(UUID.randomUUID(), questionId, "essay", Map.of());
      Question essay = buildQuestion(QuestionType.ESSAY, "anything", new BigDecimal("2"));
      Question shortQ = buildQuestion(QuestionType.SHORT_ANSWER, "expected", new BigDecimal("2"));
      Answer nullShort = buildAnswer(UUID.randomUUID(), questionId, null, Map.of());

      // ===== ACT =====
      Boolean essayResult =
          invokePrivate(
              "autoGradeAnswer",
              new Class[] {Answer.class, Question.class, BigDecimal.class},
              essayAnswer,
              essay,
              new BigDecimal("2"));
      Boolean shortResult =
          invokePrivate(
              "autoGradeAnswer",
              new Class[] {Answer.class, Question.class, BigDecimal.class},
              nullShort,
              shortQ,
              new BigDecimal("2"));

      // ===== ASSERT =====
      assertFalse(essayResult);
      assertFalse(shortResult);
    }

    @Test
    void it_should_cover_true_false_and_token_branch_variants() {
      // ===== ARRANGE + ACT =====
      String tfTrue = invokePrivate("normalizeTrueFalseToken", new Class[] {String.class}, "T");
      String tfFalse = invokePrivate("normalizeTrueFalseToken", new Class[] {String.class}, "0");
      String tfViTrue =
          invokePrivate("normalizeTrueFalseToken", new Class[] {String.class}, "đúng");
      String tfViFalse = invokePrivate("normalizeTrueFalseToken", new Class[] {String.class}, "sai");
      String tokenQuoted = invokePrivate("normalizeToken", new Class[] {String.class}, "'  value  '");
      String tokenPlain = invokePrivate("normalizeToken", new Class[] {String.class}, "plain");

      Object fromEmptyCollection =
          invokePrivate("objectToSingleValue", new Class[] {Object.class}, List.of());
      Object fromCollection =
          invokePrivate("objectToSingleValue", new Class[] {Object.class}, List.of(" first "));
      Object fromEmptyArray =
          invokePrivate("objectToSingleValue", new Class[] {Object.class}, (Object) new String[] {});

      // ===== ASSERT =====
      assertEquals("true", tfTrue);
      assertEquals("false", tfFalse);
      assertEquals("true", tfViTrue);
      assertEquals("false", tfViFalse);
      assertEquals("value", tokenQuoted);
      assertEquals("plain", tokenPlain);
      assertNull(fromEmptyCollection);
      assertEquals("first", fromCollection);
      assertNull(fromEmptyArray);
    }

    @Test
    void it_should_cover_incorrect_and_null_paths_for_objective_grading_helpers() {
      // ===== ARRANGE =====
      Answer mcqWrong = buildAnswer(UUID.randomUUID(), questionId, null, Map.of("selected", "B"));
      Question mcqQuestion = buildQuestion(QuestionType.MULTIPLE_CHOICE, "A", new BigDecimal("2"));
      Answer mcqNull = buildAnswer(UUID.randomUUID(), questionId, null, Map.of());
      Question mcqNullCorrect = buildQuestion(QuestionType.MULTIPLE_CHOICE, null, new BigDecimal("2"));

      Answer tfWrong = buildAnswer(UUID.randomUUID(), questionId, null, Map.of("value", "false"));
      Question tfQuestion = buildQuestion(QuestionType.TRUE_FALSE, "true", new BigDecimal("1"));
      Answer tfNull = buildAnswer(UUID.randomUUID(), questionId, null, Map.of());
      Question tfNullCorrect = buildQuestion(QuestionType.TRUE_FALSE, null, new BigDecimal("1"));

      // ===== ACT =====
      Boolean mcqWrongResult =
          invokePrivate(
              "autoGradeAnswer",
              new Class[] {Answer.class, Question.class, BigDecimal.class},
              mcqWrong,
              mcqQuestion,
              new BigDecimal("2"));
      Boolean mcqNullResult =
          invokePrivate(
              "autoGradeAnswer",
              new Class[] {Answer.class, Question.class, BigDecimal.class},
              mcqNull,
              mcqNullCorrect,
              new BigDecimal("2"));
      Boolean tfWrongResult =
          invokePrivate(
              "autoGradeAnswer",
              new Class[] {Answer.class, Question.class, BigDecimal.class},
              tfWrong,
              tfQuestion,
              new BigDecimal("1"));
      Boolean tfNullResult =
          invokePrivate(
              "autoGradeAnswer",
              new Class[] {Answer.class, Question.class, BigDecimal.class},
              tfNull,
              tfNullCorrect,
              new BigDecimal("1"));

      // ===== ASSERT =====
      assertTrue(mcqWrongResult);
      assertEquals(BigDecimal.ZERO, mcqWrong.getPointsEarned());
      assertFalse(mcqWrong.getIsCorrect());
      assertFalse(mcqNullResult);
      assertTrue(tfWrongResult);
      assertEquals(BigDecimal.ZERO, tfWrong.getPointsEarned());
      assertFalse(tfWrong.getIsCorrect());
      assertFalse(tfNullResult);
    }
  }

  @Nested
  @DisplayName("submission mapping and fetching")
  class MappingAndFetchingTests {

    @Test
    void it_should_get_submission_for_grading_with_assessment_point_overrides() {
      // ===== ARRANGE =====
      Submission submission = buildSubmission(SubmissionStatus.SUBMITTED);
      Answer answer = buildAnswer(answerId, questionId, "42", Map.of("value", "42"));
      answer.setPointsEarned(new BigDecimal("3.00"));
      answer.setFeedback("Good");

      Question question = buildQuestion(QuestionType.SHORT_ANSWER, "42", new BigDecimal("2.00"));
      AssessmentQuestion aq = new AssessmentQuestion();
      aq.setQuestionId(questionId);
      aq.setPointsOverride(new BigDecimal("4.00"));

      GradeAuditLog audit = new GradeAuditLog();
      audit.setAnswerId(answerId);

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(userRepository.findById(studentId))
          .thenReturn(Optional.of(User.builder().fullName("SV A").email("sva@fpt.edu.vn").build()));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(buildAssessment(teacherId)));
      when(answerRepository.findBySubmissionId(submissionId)).thenReturn(List.of(answer));
      when(questionRepository.findAllById(any())).thenReturn(List.of(question));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq));
      when(gradeAuditLogRepository.findBySubmissionId(submissionId)).thenReturn(List.of(audit));
      when(quizAttemptRepository.countBySubmissionId(submissionId)).thenReturn(3);

      // ===== ACT =====
      GradingSubmissionResponse response = gradingService.getSubmissionForGrading(submissionId);

      // ===== ASSERT =====
      assertNotNull(response);
      assertEquals(1, response.getAnswers().size());
      assertEquals(new BigDecimal("4.00"), response.getAnswers().get(0).getMaxPoints());
      assertTrue(response.getAnswers().get(0).getIsManuallyAdjusted());
    }
  }
}
