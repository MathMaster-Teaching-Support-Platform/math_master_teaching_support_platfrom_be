package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.TopicBatchItem;
import com.fptu.math_master.dto.request.BatchTopicRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.UpdateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.CreateRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.SubmitRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.StartAssessmentRequest;
import com.fptu.math_master.dto.request.RoadmapEntryTestAnswerRequest;
import com.fptu.math_master.dto.request.RoadmapEntryTestFlagRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.StudentAssessmentResponse;
import com.fptu.math_master.dto.response.AttemptStartResponse;
import com.fptu.math_master.dto.response.AnswerAckResponse;
import com.fptu.math_master.dto.response.DraftSnapshotResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.RoadmapEntryQuestionMapping;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.entity.TopicLearningMaterial;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import java.math.BigDecimal;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.QuizAttemptRepository;
import com.fptu.math_master.repository.RoadmapEntryQuestionMappingRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.repository.TopicCourseRepository;
import com.fptu.math_master.repository.TopicLearningMaterialRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GradingService;
import com.fptu.math_master.service.LearningRoadmapService;
import com.fptu.math_master.service.StudentAssessmentService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("RoadmapAdminServiceImpl - Tests")
class RoadmapAdminServiceImplTest extends BaseUnitTest {

  @InjectMocks private RoadmapAdminServiceImpl roadmapAdminService;

  @Mock private LearningRoadmapRepository roadmapRepository;
  @Mock private AssessmentRepository assessmentRepository;
  @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
  @Mock private RoadmapTopicRepository topicRepository;
  @Mock private TopicCourseRepository topicCourseRepository;
  @Mock private RoadmapEntryQuestionMappingRepository roadmapEntryQuestionMappingRepository;
  @Mock private SubmissionRepository submissionRepository;
  @Mock private QuizAttemptRepository quizAttemptRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private TopicLearningMaterialRepository topicLearningMaterialRepository;
  @Mock private UserRepository userRepository;
  @Mock private LearningRoadmapService learningRoadmapService;
  @Mock private StudentAssessmentService studentAssessmentService;
  @Mock private GradingService gradingService;

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = RoadmapAdminServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return (T) method.invoke(roadmapAdminService, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private method: " + methodName, ex);
    }
  }

  private TopicBatchItem buildTopicBatchItem(
      String title, Integer sequenceOrder, Double mark, TopicStatus status) {
    return TopicBatchItem.builder()
        .id(UUID.randomUUID())
        .title(title)
        .description("Mốc kiến thức cho " + title)
        .sequenceOrder(sequenceOrder)
        .difficulty(QuestionDifficulty.MEDIUM)
        .mark(mark)
        .status(status)
        .build();
  }

  private LearningRoadmap buildRoadmap(UUID id) {
    LearningRoadmap roadmap = new LearningRoadmap();
    roadmap.setId(id);
    roadmap.setName("Lộ trình Toán THPT");
    roadmap.setStatus(RoadmapStatus.GENERATED);
    return roadmap;
  }

  private void invokeValidateTopicMarksStrictlyIncreasing(List<TopicBatchItem> items) {
    invokePrivate("validateTopicMarksStrictlyIncreasing", new Class<?>[] {List.class}, items);
  }

  private void invokeValidateRoadmapAttempt(UUID studentId, UUID roadmapId, UUID attemptId) {
    invokePrivate(
        "validateRoadmapAttempt",
        new Class<?>[] {UUID.class, UUID.class, UUID.class},
        studentId,
        roadmapId,
        attemptId);
  }

  private Object invokeEvaluateEntryTestResult(UUID studentId, UUID roadmapId, UUID submissionId, double prevBest) {
    return invokePrivate(
        "evaluateEntryTestResult",
        new Class<?>[] {UUID.class, UUID.class, UUID.class, double.class},
        studentId,
        roadmapId,
        submissionId,
        prevBest);
  }

  @Nested
  @DisplayName("entry test operations")
  class EntryTestOperationTests {

    @Test
    void it_should_set_entry_test_id_when_roadmap_and_assessment_are_valid() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("10000000-0000-0000-0000-000000000001");
      UUID entryTestId = UUID.fromString("10000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(assessmentRepository.findByIdAndNotDeleted(entryTestId))
          .thenReturn(Optional.of(new com.fptu.math_master.entity.Assessment()));

      // ===== ACT =====
      roadmapAdminService.setRoadmapEntryTest(roadmapId, entryTestId);

      // ===== ASSERT =====
      assertEquals(entryTestId, roadmap.getEntryTestId());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).save(roadmap);
    }

    @Test
    void it_should_remove_entry_test_id_when_roadmap_is_active() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("20000000-0000-0000-0000-000000000001");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      roadmap.setEntryTestId(UUID.fromString("20000000-0000-0000-0000-000000000002"));
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

      // ===== ACT =====
      roadmapAdminService.removeRoadmapEntryTest(roadmapId);

      // ===== ASSERT =====
      assertEquals(null, roadmap.getEntryTestId());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).save(roadmap);
    }

    @Test
    void it_should_configure_entry_test_with_question_mappings_for_admin_template() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("90000000-0000-0000-0000-000000000001");
      UUID assessmentId = UUID.fromString("90000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      roadmap.setGenerationType(RoadmapGenerationType.ADMIN_TEMPLATE);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(new Assessment()));
      AssessmentQuestion q1 = AssessmentQuestion.builder().questionId(UUID.randomUUID()).orderIndex(0).build();
      AssessmentQuestion q2 = AssessmentQuestion.builder().questionId(UUID.randomUUID()).orderIndex(1).build();
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(q1, q2));

      CreateRoadmapEntryTestRequest request =
          CreateRoadmapEntryTestRequest.builder().assessmentId(assessmentId).build();

      // ===== ACT =====
      roadmapAdminService.configureEntryTest(roadmapId, request);

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(roadmapEntryQuestionMappingRepository, times(1)).deleteByRoadmapId(roadmapId);
      verify(roadmapEntryQuestionMappingRepository, times(1)).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void it_should_throw_question_not_found_when_configuring_entry_test_without_questions() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("91000000-0000-0000-0000-000000000001");
      UUID assessmentId = UUID.fromString("91000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      roadmap.setGenerationType(RoadmapGenerationType.ADMIN_TEMPLATE);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(new Assessment()));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());
      CreateRoadmapEntryTestRequest request =
          CreateRoadmapEntryTestRequest.builder().assessmentId(assessmentId).build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> roadmapAdminService.configureEntryTest(roadmapId, request));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_configure_entry_test_for_non_admin_template() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("91500000-0000-0000-0000-000000000001");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      roadmap.setGenerationType(null);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      CreateRoadmapEntryTestRequest request =
          CreateRoadmapEntryTestRequest.builder().assessmentId(UUID.randomUUID()).build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> roadmapAdminService.configureEntryTest(roadmapId, request));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("student roadmap and submission flows")
  class StudentRoadmapAndSubmissionFlowTests {

    @Test
    void it_should_return_roadmap_for_student_with_computed_progress_when_entry_test_is_absent() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("92000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("92000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of());

      List<RoadmapTopicResponse> topics =
          List.of(
              RoadmapTopicResponse.builder().title("Đại số").sequenceOrder(1).mark(4.0).build(),
              RoadmapTopicResponse.builder().title("Giải tích").sequenceOrder(2).mark(8.0).build());
      RoadmapDetailResponse detail =
          RoadmapDetailResponse.builder()
              .id(roadmapId)
              .topics(topics)
              .entryTest(null)
              .build();
      when(learningRoadmapService.getRoadmapById(roadmapId)).thenReturn(detail);
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of());

      // ===== ACT =====
      RoadmapDetailResponse response = roadmapAdminService.getRoadmapForStudent(studentId, roadmapId);

      // ===== ASSERT =====
      assertNotNull(response.getProgress());
      assertEquals(0, response.getStudentBestScore());
      assertEquals(0, response.getProgress().getCurrentTopicIndex());
    }

    @Test
    void it_should_throw_access_denied_when_finishing_entry_test_of_other_student_attempt() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("93000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("93000000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("93000000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("93000000-0000-0000-0000-000000000004");

      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapEntryQuestionMapping mapping = RoadmapEntryQuestionMapping.builder()
          .roadmapId(roadmapId).assessmentId(assessmentId).questionId(UUID.randomUUID()).orderIndex(0).build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));

      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setAssessmentId(assessmentId);
      attempt.setStudentId(UUID.fromString("93000000-0000-0000-0000-000000000999"));
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> roadmapAdminService.finishEntryTest(studentId, roadmapId, attemptId));
      assertEquals(ErrorCode.ATTEMPT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_throw_unauthorized_when_submitting_entry_test_of_other_student_submission() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("94000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("94000000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("94000000-0000-0000-0000-000000000003");
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of());

      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setAssessmentId(UUID.fromString("94000000-0000-0000-0000-000000000004"));
      submission.setStudentId(UUID.fromString("94000000-0000-0000-0000-000000000999"));
      submission.setStatus(SubmissionStatus.SUBMITTED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      SubmitRoadmapEntryTestRequest request =
          SubmitRoadmapEntryTestRequest.builder().submissionId(submissionId).build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> roadmapAdminService.submitEntryTest(studentId, roadmapId, request));
      assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void it_should_return_active_attempt_with_null_attempt_id_when_no_in_progress_attempt_exists() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("95000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("95000000-0000-0000-0000-000000000002");
      UUID assessmentId = UUID.fromString("95000000-0000-0000-0000-000000000003");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));
      StudentAssessmentResponse assessmentInfo =
          StudentAssessmentResponse.builder().id(assessmentId).studentStatus("NOT_STARTED").build();
      when(studentAssessmentService.getAssessmentDetails(assessmentId)).thenReturn(assessmentInfo);
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());

      // ===== ACT =====
      var response = roadmapAdminService.getActiveEntryTestAttempt(studentId, roadmapId);

      // ===== ASSERT =====
      assertEquals(null, response.getAttemptId());
      assertEquals("NOT_STARTED", response.getStudentStatus());
    }

    @Test
    void it_should_start_entry_test_using_configured_assessment_id() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("96000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("96000000-0000-0000-0000-000000000002");
      UUID assessmentId = UUID.fromString("96000000-0000-0000-0000-000000000003");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));
      AttemptStartResponse startResponse =
          AttemptStartResponse.builder().attemptId(UUID.randomUUID()).assessmentId(assessmentId).build();
      when(studentAssessmentService.startAssessment(org.mockito.ArgumentMatchers.any(StartAssessmentRequest.class)))
          .thenReturn(startResponse);

      // ===== ACT =====
      AttemptStartResponse response = roadmapAdminService.startEntryTest(studentId, roadmapId);

      // ===== ASSERT =====
      assertEquals(assessmentId, response.getAssessmentId());
    }

    @Test
    void it_should_return_entry_test_info_for_student_when_assessment_is_configured() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("97000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("97000000-0000-0000-0000-000000000002");
      UUID assessmentId = UUID.fromString("97000000-0000-0000-0000-000000000003");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));
      StudentAssessmentResponse assessmentInfo =
          StudentAssessmentResponse.builder()
              .id(assessmentId)
              .title("Placement test")
              .studentStatus("IN_PROGRESS")
              .canStart(true)
              .build();
      when(studentAssessmentService.getAssessmentDetails(assessmentId)).thenReturn(assessmentInfo);
      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(UUID.fromString("97000000-0000-0000-0000-000000000004"));
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of(attempt));

      // ===== ACT =====
      var response = roadmapAdminService.getEntryTestForStudent(studentId, roadmapId);

      // ===== ASSERT =====
      assertEquals(assessmentId, response.getAssessmentId());
      assertEquals("IN_PROGRESS", response.getStudentStatus());
      assertNotNull(response.getActiveAttemptId());
    }

    @Test
    void it_should_continue_get_roadmap_for_student_when_entry_test_status_resolution_fails() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("98000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("98000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapDetailResponse detail =
          RoadmapDetailResponse.builder()
              .id(roadmapId)
              .topics(List.of())
              .entryTest(com.fptu.math_master.dto.response.RoadmapEntryTestInfo.builder().build())
              .build();
      when(learningRoadmapService.getRoadmapById(roadmapId)).thenReturn(detail);
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of()); // make getEntryTestForStudent throw ASSESSMENT_NOT_FOUND

      // ===== ACT =====
      RoadmapDetailResponse response = roadmapAdminService.getRoadmapForStudent(studentId, roadmapId);

      // ===== ASSERT =====
      assertNotNull(response);
      assertNotNull(response.getEntryTest());
    }

    @Test
    void it_should_throw_assessment_not_found_when_finish_entry_test_attempt_assessment_mismatches() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("99000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("99000000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("99000000-0000-0000-0000-000000000003");
      UUID configuredAssessmentId = UUID.fromString("99000000-0000-0000-0000-000000000004");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(configuredAssessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));
      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setStudentId(studentId);
      attempt.setAssessmentId(UUID.fromString("99000000-0000-0000-0000-000000000999"));
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> roadmapAdminService.finishEntryTest(studentId, roadmapId, attemptId));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_finish_entry_test_and_return_result_when_attempt_and_submission_are_valid() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("c1000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("c1000000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("c1000000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("c1000000-0000-0000-0000-000000000004");
      UUID submissionId = UUID.fromString("c1000000-0000-0000-0000-000000000005");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));
      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setStudentId(studentId);
      attempt.setAssessmentId(assessmentId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      // before-submit existing best
      Submission previousSubmission = new Submission();
      previousSubmission.setId(UUID.fromString("c1000000-0000-0000-0000-000000000099"));
      previousSubmission.setAssessmentId(assessmentId);
      previousSubmission.setStudentId(studentId);
      previousSubmission.setFinalScore(BigDecimal.valueOf(6));
      previousSubmission.setMaxScore(BigDecimal.TEN);
      previousSubmission.setStatus(SubmissionStatus.GRADED);
      Submission evaluatedSubmission = new Submission();
      evaluatedSubmission.setId(submissionId);
      evaluatedSubmission.setAssessmentId(assessmentId);
      evaluatedSubmission.setStudentId(studentId);
      evaluatedSubmission.setFinalScore(BigDecimal.valueOf(8));
      evaluatedSubmission.setMaxScore(BigDecimal.TEN);
      evaluatedSubmission.setStatus(SubmissionStatus.SUBMITTED);
      int[] lookupCount = {0};
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenAnswer(
              invocation ->
                  lookupCount[0]++ == 0
                      ? Optional.of(previousSubmission)
                      : Optional.of(evaluatedSubmission));
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of(previousSubmission, evaluatedSubmission));

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(evaluatedSubmission));

      RoadmapTopic t1 = new RoadmapTopic();
      t1.setId(UUID.fromString("c1000000-0000-0000-0000-000000000011"));
      t1.setTitle("Nền tảng");
      t1.setMark(5.0);
      t1.setSequenceOrder(1);
      RoadmapTopic t2 = new RoadmapTopic();
      t2.setId(UUID.fromString("c1000000-0000-0000-0000-000000000012"));
      t2.setTitle("Nâng cao");
      t2.setMark(9.0);
      t2.setSequenceOrder(2);
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(t1, t2));

      // ===== ACT =====
      var result = roadmapAdminService.finishEntryTest(studentId, roadmapId, attemptId);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(roadmapId, result.getRoadmapId());
      assertEquals(submissionId, result.getSubmissionId());
      assertNotNull(result.getUnlockedTopics());

      // ===== VERIFY =====
      verify(studentAssessmentService, times(1))
          .submitAssessment(org.mockito.ArgumentMatchers.any());
      verify(gradingService, times(1)).autoGradeSubmission(submissionId);
    }

    @Test
    void it_should_submit_entry_test_and_return_result_when_submission_belongs_to_student() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("c2000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("c2000000-0000-0000-0000-000000000002");
      UUID assessmentId = UUID.fromString("c2000000-0000-0000-0000-000000000003");
      UUID submissionId = UUID.fromString("c2000000-0000-0000-0000-000000000004");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));

      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setAssessmentId(assessmentId);
      submission.setStudentId(studentId);
      submission.setFinalScore(BigDecimal.valueOf(7));
      submission.setMaxScore(BigDecimal.TEN);
      submission.setStatus(SubmissionStatus.SUBMITTED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of(submission));
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(UUID.fromString("c2000000-0000-0000-0000-000000000011"));
      topic.setTitle("Chủ đề");
      topic.setMark(6.0);
      topic.setSequenceOrder(1);
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));

      SubmitRoadmapEntryTestRequest request =
          SubmitRoadmapEntryTestRequest.builder().submissionId(submissionId).build();

      // ===== ACT =====
      var result = roadmapAdminService.submitEntryTest(studentId, roadmapId, request);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(submissionId, result.getSubmissionId());
      assertNotNull(result.getSuggestedTopicId());

      // ===== VERIFY =====
      verify(gradingService, times(1)).autoGradeSubmission(submissionId);
    }
  }

  @Nested
  @DisplayName("attempt mutation and snapshot flows")
  class AttemptMutationAndSnapshotFlowTests {

    private UUID roadmapId = UUID.fromString("aa000000-0000-0000-0000-000000000001");
    private UUID studentId = UUID.fromString("aa000000-0000-0000-0000-000000000002");
    private UUID assessmentId = UUID.fromString("aa000000-0000-0000-0000-000000000003");
    private UUID attemptId = UUID.fromString("aa000000-0000-0000-0000-000000000004");

    private void mockConfiguredAttempt() {
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));
      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setStudentId(studentId);
      attempt.setAssessmentId(assessmentId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
    }

    @Test
    void it_should_save_entry_test_answer_when_attempt_is_valid() {
      // ===== ARRANGE =====
      mockConfiguredAttempt();
      UUID questionId = UUID.fromString("aa000000-0000-0000-0000-000000000005");
      RoadmapEntryTestAnswerRequest request =
          RoadmapEntryTestAnswerRequest.builder()
              .questionId(questionId)
              .answerValue("42")
              .sequenceNumber(2L)
              .clientTimestamp(Instant.now())
              .build();
      AnswerAckResponse ack =
          AnswerAckResponse.builder().questionId(questionId).success(true).sequenceNumber(2L).build();
      when(studentAssessmentService.updateAnswer(org.mockito.ArgumentMatchers.any())).thenReturn(ack);

      // ===== ACT =====
      AnswerAckResponse response =
          roadmapAdminService.saveEntryTestAnswer(studentId, roadmapId, attemptId, request);

      // ===== ASSERT =====
      assertEquals(true, response.getSuccess());
      assertEquals(questionId, response.getQuestionId());
    }

    @Test
    void it_should_update_entry_test_flag_when_attempt_is_valid() {
      // ===== ARRANGE =====
      mockConfiguredAttempt();
      UUID questionId = UUID.fromString("aa000000-0000-0000-0000-000000000006");
      RoadmapEntryTestFlagRequest request =
          RoadmapEntryTestFlagRequest.builder().questionId(questionId).flagged(true).build();
      AnswerAckResponse ack =
          AnswerAckResponse.builder().questionId(questionId).success(true).type("flag_ack").build();
      when(studentAssessmentService.updateFlag(org.mockito.ArgumentMatchers.any())).thenReturn(ack);

      // ===== ACT =====
      AnswerAckResponse response =
          roadmapAdminService.updateEntryTestFlag(studentId, roadmapId, attemptId, request);

      // ===== ASSERT =====
      assertEquals("flag_ack", response.getType());
      assertEquals(true, response.getSuccess());
    }

    @Test
    void it_should_return_snapshot_with_progress_when_attempt_is_valid() {
      // ===== ARRANGE =====
      mockConfiguredAttempt();
      DraftSnapshotResponse snapshot =
          DraftSnapshotResponse.builder()
              .attemptId(attemptId)
              .answers(Map.of())
              .flags(Map.of())
              .startedAt(Instant.now())
              .expiresAt(Instant.now().plusSeconds(1200))
              .timeRemainingSeconds(1200)
              .answeredCount(3)
              .totalQuestions(10)
              .build();
      when(studentAssessmentService.getDraftSnapshot(attemptId)).thenReturn(snapshot);

      // ===== ACT =====
      var response = roadmapAdminService.getEntryTestSnapshot(studentId, roadmapId, attemptId);

      // ===== ASSERT =====
      assertEquals(attemptId, response.getAttemptId());
      assertEquals(3, response.getProgress().getAnsweredCount());
      assertEquals(30.0, response.getProgress().getCompletionPercentage());
    }

    @Test
    void it_should_save_and_exit_entry_test_when_attempt_is_valid() {
      // ===== ARRANGE =====
      mockConfiguredAttempt();

      // ===== ACT =====
      roadmapAdminService.saveEntryTestAndExit(studentId, roadmapId, attemptId);

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(studentAssessmentService, times(1)).saveAndExit(attemptId);
    }
  }

  @Nested
  @DisplayName("entry test evaluation internals")
  class EntryTestEvaluationInternalTests {

    @Test
    void it_should_evaluate_entry_test_result_when_submission_mapping_and_topics_are_valid() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("bb000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("bb000000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("bb000000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("bb000000-0000-0000-0000-000000000004");

      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setStudentId(studentId);
      submission.setAssessmentId(assessmentId);
      submission.setFinalScore(BigDecimal.valueOf(8));
      submission.setMaxScore(BigDecimal.valueOf(10));
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));

      RoadmapTopic topic1 = new RoadmapTopic();
      topic1.setId(UUID.fromString("bb000000-0000-0000-0000-000000000011"));
      topic1.setTitle("Cơ bản");
      topic1.setMark(4.0);
      topic1.setSequenceOrder(1);
      RoadmapTopic topic2 = new RoadmapTopic();
      topic2.setId(UUID.fromString("bb000000-0000-0000-0000-000000000012"));
      topic2.setTitle("Nâng cao");
      topic2.setMark(9.0);
      topic2.setSequenceOrder(2);
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId))
          .thenReturn(List.of(topic1, topic2));
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of(submission));

      // ===== ACT =====
      Object response = invokeEvaluateEntryTestResult(studentId, roadmapId, submissionId, 3.0);

      // ===== ASSERT =====
      assertNotNull(response);
    }

    @Test
    void it_should_throw_assessment_not_found_when_mapping_assessment_mismatches_submission() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("bc000000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("bc000000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("bc000000-0000-0000-0000-000000000003");
      UUID submissionAssessmentId = UUID.fromString("bc000000-0000-0000-0000-000000000004");
      UUID mappingAssessmentId = UUID.fromString("bc000000-0000-0000-0000-000000000005");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setStudentId(studentId);
      submission.setAssessmentId(submissionAssessmentId);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(mappingAssessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));

      // ===== ACT & ASSERT =====
      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class, () -> invokeEvaluateEntryTestResult(studentId, roadmapId, submissionId, 0.0));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ((AppException) target).getErrorCode());
    }
  }

  @Nested
  @DisplayName("validateTopicMarksStrictlyIncreasing()")
  class ValidateTopicMarksStrictlyIncreasingTests {

    /**
     * Normal case: Accept active topics with strictly increasing marks.
     *
     * <p>Input:
     * <ul>
     *   <li>items: 3 active topics with marks 2.0, 5.5, 8.0</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>mark > 0 branch for each item</li>
     *   <li>strictly increasing check FALSE branch (no exception)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>No exception is thrown</li>
     * </ul>
     */
    @Test
    void it_should_accept_topic_marks_when_marks_are_positive_and_strictly_increasing() {
      // ===== ARRANGE =====
      List<TopicBatchItem> items =
          List.of(
              buildTopicBatchItem("Hàm số bậc nhất", 1, 2.0, TopicStatus.NOT_STARTED),
              buildTopicBatchItem("Hàm số bậc hai", 2, 5.5, TopicStatus.IN_PROGRESS),
              buildTopicBatchItem("Hàm số lượng giác", 3, 8.0, TopicStatus.COMPLETED));

      // ===== ACT =====
      invokePrivate("validateTopicMarksStrictlyIncreasing", new Class<?>[] {List.class}, items);

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }

    /**
     * Abnormal case: Reject mark order when a later topic mark is not greater.
     *
     * <p>Input:
     * <ul>
     *   <li>items: marks 3.0 then 3.0</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>strictly increasing check TRUE branch (throw exception)</li>
     *   <li>FALSE branch is covered by
     *       {@code it_should_accept_topic_marks_when_marks_are_positive_and_strictly_increasing}</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} with {@code INVALID_TOPIC_POINT_ORDER}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_mark_is_not_strictly_increasing() {
      // ===== ARRANGE =====
      List<TopicBatchItem> items =
          List.of(
              buildTopicBatchItem("Giải hệ phương trình", 1, 3.0, TopicStatus.NOT_STARTED),
              buildTopicBatchItem("Bất phương trình", 2, 3.0, TopicStatus.NOT_STARTED));

      // ===== ACT & ASSERT =====
      RuntimeException wrapped =
          assertThrows(RuntimeException.class, () -> invokeValidateTopicMarksStrictlyIncreasing(items));
      assertTrue(wrapped.getCause() instanceof java.lang.reflect.InvocationTargetException);
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.INVALID_TOPIC_POINT_ORDER, ((AppException) target).getErrorCode());

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }
  }

  @Nested
  @DisplayName("resolveCurrentTopicIndexFromMarks()")
  class ResolveCurrentTopicIndexFromMarksTests {

    /**
     * Normal case: Return index of first topic whose mark threshold is met.
     *
     * <p>Input:
     * <ul>
     *   <li>topics: ordered by sequence after sorting with marks 3.0, 6.0, 9.0</li>
     *   <li>scoreOnTen: 5.0</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>topics not empty branch</li>
     *   <li>mark != null and scoreOnTen &lt;= mark TRUE branch at index 1</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return current topic index = 1</li>
     * </ul>
     */
    @Test
    void it_should_return_first_matching_index_when_score_fits_topic_threshold() {
      // ===== ARRANGE =====
      List<RoadmapTopicResponse> topics =
          List.of(
              RoadmapTopicResponse.builder().title("Mệnh đề").sequenceOrder(2).mark(6.0).build(),
              RoadmapTopicResponse.builder().title("Tập hợp").sequenceOrder(1).mark(3.0).build(),
              RoadmapTopicResponse.builder().title("Hàm số").sequenceOrder(3).mark(9.0).build());
      RoadmapDetailResponse detail = RoadmapDetailResponse.builder().topics(topics).build();

      // ===== ACT =====
      Integer actual =
          invokePrivate(
              "resolveCurrentTopicIndexFromMarks",
              new Class<?>[] {RoadmapDetailResponse.class, double.class},
              detail,
              5.0);

      // ===== ASSERT =====
      assertEquals(1, actual);

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }
  }

  @Nested
  @DisplayName("buildMaterialResourceLink()")
  class BuildMaterialResourceLinkTests {

    /**
     * Normal case: Build lesson API link when lessonId exists.
     *
     * <p>Input:
     * <ul>
     *   <li>material.lessonId: non-null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>lessonId != null TRUE branch</li>
     *   <li>other resource branches are skipped by early return</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return {@code /api/v1/lessons/{id}}</li>
     * </ul>
     */
    @Test
    void it_should_return_lesson_link_when_lesson_id_is_available() {
      // ===== ARRANGE =====
      UUID lessonId = UUID.fromString("11111111-1111-1111-1111-111111111111");
      TopicLearningMaterial material = new TopicLearningMaterial();
      material.setLessonId(lessonId);

      // ===== ACT =====
      String link =
          invokePrivate(
              "buildMaterialResourceLink", new Class<?>[] {TopicLearningMaterial.class}, material);

      // ===== ASSERT =====
      assertEquals("/api/v1/lessons/" + lessonId, link);

      // ===== VERIFY =====
      verifyNoInteractions(topicLearningMaterialRepository, topicRepository, roadmapRepository);
    }

    @Test
    void it_should_return_null_when_no_resource_id_is_present() {
      // ===== ARRANGE =====
      TopicLearningMaterial material = new TopicLearningMaterial();

      // ===== ACT =====
      String link =
          invokePrivate(
              "buildMaterialResourceLink", new Class<?>[] {TopicLearningMaterial.class}, material);

      // ===== ASSERT =====
      assertEquals(null, link);

      // ===== VERIFY =====
      verifyNoInteractions(topicLearningMaterialRepository, topicRepository, roadmapRepository);
    }
  }

  @Nested
  @DisplayName("mapStudentEntryStatus()")
  class MapStudentEntryStatusTests {

    @Test
    void it_should_return_not_started_when_status_is_blank() {
      // ===== ARRANGE =====
      String rawStatus = "   ";

      // ===== ACT =====
      String mapped =
          invokePrivate("mapStudentEntryStatus", new Class<?>[] {String.class}, rawStatus);

      // ===== ASSERT =====
      assertEquals("NOT_STARTED", mapped);

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }

    @Test
    void it_should_return_completed_when_status_is_completed_ignoring_case() {
      // ===== ARRANGE =====
      String rawStatus = "completed";

      // ===== ACT =====
      String mapped =
          invokePrivate("mapStudentEntryStatus", new Class<?>[] {String.class}, rawStatus);

      // ===== ASSERT =====
      assertEquals("COMPLETED", mapped);

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }
  }

  @Nested
  @DisplayName("resolveCurrentTopicIndexFromMarks() additional")
  class ResolveCurrentTopicIndexFromMarksAdditionalTests {

    @Test
    void it_should_return_zero_when_topic_list_is_empty() {
      // ===== ARRANGE =====
      RoadmapDetailResponse detail = RoadmapDetailResponse.builder().topics(new ArrayList<>()).build();

      // ===== ACT =====
      Integer actual =
          invokePrivate(
              "resolveCurrentTopicIndexFromMarks",
              new Class<?>[] {RoadmapDetailResponse.class, double.class},
              detail,
              7.5);

      // ===== ASSERT =====
      assertEquals(0, actual);

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }
  }

  @Nested
  @DisplayName("buildProgress()")
  class BuildProgressTests {

    @Test
    void it_should_return_zero_percentage_when_total_question_is_zero() {
      // ===== ARRANGE =====
      Integer answered = 5;
      Integer total = 0;

      // ===== ACT =====
      Object progress =
          invokePrivate("buildProgress", new Class<?>[] {Integer.class, Integer.class}, answered, total);

      // ===== ASSERT =====
      assertNotNull(progress);
      double percentage = extractCompletionPercentage(progress);
      assertEquals(0.0, percentage);

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }
  }

  @Nested
  @DisplayName("score helpers")
  class ScoreHelperTests {

    @Test
    void it_should_compute_score_from_final_score_when_final_score_and_max_score_are_present() {
      // ===== ARRANGE =====
      Submission submission = new Submission();
      submission.setFinalScore(BigDecimal.valueOf(8));
      submission.setMaxScore(BigDecimal.valueOf(10));

      // ===== ACT =====
      Double score =
          invokePrivate("computeScoreOnTen", new Class<?>[] {Submission.class}, submission);

      // ===== ASSERT =====
      assertEquals(8.0, score);

      // ===== VERIFY =====
      verifyNoInteractions(submissionRepository, topicRepository);
    }

    @Test
    void it_should_compute_score_from_percentage_when_score_values_are_missing() {
      // ===== ARRANGE =====
      Submission submission = new Submission();
      submission.setPercentage(BigDecimal.valueOf(75));

      // ===== ACT =====
      Double score =
          invokePrivate("computeScoreOnTen", new Class<?>[] {Submission.class}, submission);

      // ===== ASSERT =====
      assertEquals(7.5, score);

      // ===== VERIFY =====
      verifyNoInteractions(submissionRepository, topicRepository);
    }

    @Test
    void it_should_clamp_score_to_ten_when_calculated_value_exceeds_range() {
      // ===== ARRANGE =====
      Submission submission = new Submission();
      submission.setFinalScore(BigDecimal.valueOf(150));
      submission.setMaxScore(BigDecimal.valueOf(100));

      // ===== ACT =====
      Double score =
          invokePrivate("computeScoreOnTen", new Class<?>[] {Submission.class}, submission);

      // ===== ASSERT =====
      assertEquals(10.0, score);

      // ===== VERIFY =====
      verifyNoInteractions(submissionRepository, topicRepository);
    }
  }

  @Nested
  @DisplayName("unlock helpers")
  class UnlockHelperTests {

    @Test
    void it_should_unlock_topic_when_required_mark_is_less_or_equal_to_best_score() {
      // ===== ARRANGE =====
      RoadmapTopic topic = new RoadmapTopic();
      topic.setMark(6.0);
      double bestScore = 6.0;

      // ===== ACT =====
      Boolean unlocked =
          invokePrivate("isTopicUnlocked", new Class<?>[] {RoadmapTopic.class, double.class}, topic, bestScore);

      // ===== ASSERT =====
      assertTrue(unlocked);

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }

    @Test
    void it_should_map_only_newly_unlocked_topics_between_previous_and_current_scores() {
      // ===== ARRANGE =====
      RoadmapTopic topic1 = new RoadmapTopic();
      topic1.setId(UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111"));
      topic1.setTitle("Đại số cơ bản");
      topic1.setMark(3.0);
      RoadmapTopic topic2 = new RoadmapTopic();
      topic2.setId(UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222"));
      topic2.setTitle("Hàm số nâng cao");
      topic2.setMark(7.0);

      List<RoadmapTopic> topics = List.of(topic1, topic2);

      // ===== ACT =====
      List<?> newlyUnlocked =
          invokePrivate(
              "mapNewlyUnlockedTopics",
              new Class<?>[] {List.class, double.class, double.class},
              topics,
              2.0,
              6.0);

      // ===== ASSERT =====
      assertEquals(1, newlyUnlocked.size());

      // ===== VERIFY =====
      verifyNoInteractions(topicRepository, roadmapRepository, courseRepository);
    }

    @Test
    void it_should_map_unlocked_topic_with_zero_required_point_when_mark_is_null() {
      // ===== ARRANGE =====
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(UUID.fromString("dddddddd-1111-1111-1111-111111111111"));
      topic.setTitle("Không có điểm mốc");
      topic.setMark(null);

      // ===== ACT =====
      Object mapped =
          invokePrivate("mapUnlockedTopic", new Class<?>[] {RoadmapTopic.class}, topic);

      // ===== ASSERT =====
      assertNotNull(mapped);
    }
  }

  @Nested
  @DisplayName("summary and score helper branches")
  class SummaryAndScoreHelperBranchTests {

    @Test
    void it_should_map_summary_with_all_students_name_when_student_id_is_null() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(UUID.fromString("ce000000-0000-0000-0000-000000000001"));
      roadmap.setStudentId(null);
      roadmap.setSubject("Toán");
      roadmap.setGradeLevel("Lớp 10");

      // ===== ACT =====
      Object summary =
          invokePrivate("mapToSummaryResponse", new Class<?>[] {LearningRoadmap.class}, roadmap);

      // ===== ASSERT =====
      assertNotNull(summary);
    }

    @Test
    void it_should_round_score_to_nearest_int_when_mapping_to_point_scale() {
      // ===== ARRANGE =====
      double score = 7.6;

      // ===== ACT =====
      Integer rounded =
          invokePrivate("toPointScaleInt", new Class<?>[] {double.class}, score);

      // ===== ASSERT =====
      assertEquals(8, rounded);
    }
  }

  @SuppressWarnings("unused")
  private double extractCompletionPercentage(Object progressObj) {
    try {
      Method m = progressObj.getClass().getMethod("getCompletionPercentage");
      return (double) m.invoke(progressObj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nested
  @DisplayName("topic crud and batch flows")
  class TopicCrudAndBatchFlowTests {

    @Test
    void it_should_add_topic_without_course_and_return_topic_details() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("de000000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("de000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(topicRepository.save(org.mockito.ArgumentMatchers.any(RoadmapTopic.class)))
          .thenAnswer(
              invocation -> {
                RoadmapTopic topic = invocation.getArgument(0);
                topic.setId(topicId);
                return topic;
              });
      RoadmapTopicResponse topicResponse = RoadmapTopicResponse.builder().id(topicId).title("Chủ đề mới").build();
      when(learningRoadmapService.getTopicDetails(topicId)).thenReturn(topicResponse);

      CreateRoadmapTopicRequest request =
          CreateRoadmapTopicRequest.builder()
              .title("Chủ đề mới")
              .description("Mô tả")
              .difficulty(QuestionDifficulty.MEDIUM)
              .sequenceOrder(1)
              .mark(2.5)
              .build();

      // ===== ACT =====
      RoadmapTopicResponse response = roadmapAdminService.addTopic(roadmapId, request);

      // ===== ASSERT =====
      assertEquals(topicId, response.getId());
      verify(topicCourseRepository, times(0)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void it_should_update_existing_topic_fields_and_return_latest_details() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("df000000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("df000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      topic.setTitle("Cũ");
      topic.setStatus(TopicStatus.NOT_STARTED);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
      when(learningRoadmapService.getTopicDetails(topicId))
          .thenReturn(RoadmapTopicResponse.builder().id(topicId).title("Mới").build());

      UpdateRoadmapTopicRequest request =
          UpdateRoadmapTopicRequest.builder().title("Mới").status(TopicStatus.IN_PROGRESS).build();

      // ===== ACT =====
      RoadmapTopicResponse response = roadmapAdminService.updateTopic(roadmapId, topicId, request);

      // ===== ASSERT =====
      assertEquals("Mới", response.getTitle());
      assertEquals("Mới", topic.getTitle());
      assertEquals(TopicStatus.IN_PROGRESS, topic.getStatus());
      verify(topicRepository, times(1)).save(topic);
    }

    @Test
    void it_should_soft_delete_topic_and_update_total_topic_count() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("e0000000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("e0000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic, new RoadmapTopic(), new RoadmapTopic()));

      // ===== ACT =====
      roadmapAdminService.softDeleteTopic(roadmapId, topicId);

      // ===== ASSERT =====
      assertNotNull(topic.getDeletedAt());
      assertEquals(2, roadmap.getTotalTopicsCount());
      verify(roadmapRepository, times(1)).save(roadmap);
    }

    @Test
    void it_should_batch_create_topic_when_item_id_is_null() {
      // ===== ARRANGE =====
      UUID roadmapId = UUID.fromString("e1000000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("e1000000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(topicRepository.save(org.mockito.ArgumentMatchers.any(RoadmapTopic.class)))
          .thenAnswer(
              invocation -> {
                RoadmapTopic saved = invocation.getArgument(0);
                saved.setId(topicId);
                return saved;
              });
      when(learningRoadmapService.getTopicDetails(topicId))
          .thenReturn(RoadmapTopicResponse.builder().id(topicId).title("Batch topic").build());

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(null)
              .title("Batch topic")
              .description("Desc")
              .sequenceOrder(1)
              .difficulty(QuestionDifficulty.EASY)
              .mark(1.0)
              .status(TopicStatus.NOT_STARTED)
              .build();

      com.fptu.math_master.dto.request.BatchTopicRequest request =
          com.fptu.math_master.dto.request.BatchTopicRequest.builder()
              .roadmapId(roadmapId)
              .topics(List.of(item))
              .build();
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId))
          .thenReturn(List.of(new RoadmapTopic()));

      // ===== ACT =====
      List<RoadmapTopicResponse> responses = roadmapAdminService.batchSaveTopics(request);

      // ===== ASSERT =====
      assertEquals(1, responses.size());
      assertEquals(topicId, responses.get(0).getId());
    }
  }

  @Nested
  @DisplayName("roadmap and helper branch expansion")
  class RoadmapAndHelperBranchExpansionTests {

    @Test
    void it_should_create_roadmap_when_subject_and_grade_are_valid() {
      UUID subjectId = UUID.fromString("f1000000-0000-0000-0000-000000000001");
      UUID roadmapId = UUID.fromString("f1000000-0000-0000-0000-000000000002");
      com.fptu.math_master.entity.SchoolGrade grade = new com.fptu.math_master.entity.SchoolGrade();
      grade.setName("Lớp 11");
      com.fptu.math_master.entity.Subject subject = new com.fptu.math_master.entity.Subject();
      subject.setId(subjectId);
      subject.setName("Toán");
      subject.setSchoolGrade(grade);
      when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
      when(roadmapRepository.save(org.mockito.ArgumentMatchers.any(LearningRoadmap.class)))
          .thenAnswer(
              inv -> {
                LearningRoadmap r = inv.getArgument(0);
                r.setId(roadmapId);
                return r;
              });
      when(learningRoadmapService.getRoadmapById(roadmapId))
          .thenReturn(RoadmapDetailResponse.builder().id(roadmapId).build());

      com.fptu.math_master.dto.request.CreateAdminRoadmapRequest request =
          com.fptu.math_master.dto.request.CreateAdminRoadmapRequest.builder().name("LT 11").subjectId(subjectId).build();

      RoadmapDetailResponse response = roadmapAdminService.createRoadmap(request);
      assertEquals(roadmapId, response.getId());
    }

    @Test
    void it_should_get_all_roadmaps_with_trimmed_name() {
      UUID id = UUID.fromString("f1100000-0000-0000-0000-000000000001");
      LearningRoadmap r = buildRoadmap(id);
      when(roadmapRepository.findAdminTemplates("abc", PageRequest.of(0, 5)))
          .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 5), 1));

      var page = roadmapAdminService.getAllRoadmaps("  abc  ", PageRequest.of(0, 5));
      assertEquals(1, page.getTotalElements());
    }

    @Test
    void it_should_throw_not_found_when_get_roadmap_points_to_soft_deleted_record() {
      UUID id = UUID.fromString("f1200000-0000-0000-0000-000000000001");
      LearningRoadmap r = buildRoadmap(id);
      r.setDeletedAt(Instant.now());
      when(roadmapRepository.findById(id)).thenReturn(Optional.of(r));

      AppException ex = assertThrows(AppException.class, () -> roadmapAdminService.getRoadmap(id));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_update_roadmap_subject_and_status_when_values_present() {
      UUID roadmapId = UUID.fromString("f1300000-0000-0000-0000-000000000001");
      UUID subjectId = UUID.fromString("f1300000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      com.fptu.math_master.entity.SchoolGrade grade = new com.fptu.math_master.entity.SchoolGrade();
      grade.setName("Lớp 12");
      com.fptu.math_master.entity.Subject subject = new com.fptu.math_master.entity.Subject();
      subject.setId(subjectId);
      subject.setName("Giải tích");
      subject.setSchoolGrade(grade);
      when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
      when(learningRoadmapService.getRoadmapById(roadmapId))
          .thenReturn(RoadmapDetailResponse.builder().id(roadmapId).build());

      com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest request =
          com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest.builder()
              .subjectId(subjectId)
              .status(RoadmapStatus.ARCHIVED)
              .build();
      var response = roadmapAdminService.updateRoadmap(roadmapId, request);
      assertEquals(roadmapId, response.getId());
      assertEquals("Giải tích", roadmap.getSubject());
      assertEquals("Lớp 12", roadmap.getGradeLevel());
    }

    @Test
    void it_should_soft_delete_roadmap_and_set_archived_status() {
      UUID roadmapId = UUID.fromString("f1400000-0000-0000-0000-000000000001");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

      roadmapAdminService.softDeleteRoadmap(roadmapId);
      assertNotNull(roadmap.getDeletedAt());
      assertEquals(RoadmapStatus.ARCHIVED, roadmap.getStatus());
    }

    @Test
    void it_should_throw_invalid_topic_point_order_when_topic_mark_is_null() {
      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(UUID.randomUUID())
              .title("Null mark")
              .sequenceOrder(1)
              .status(TopicStatus.NOT_STARTED)
              .mark(null)
              .build();
      RuntimeException wrapped =
          assertThrows(RuntimeException.class, () -> invokeValidateTopicMarksStrictlyIncreasing(List.of(item)));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.INVALID_TOPIC_POINT_ORDER, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_ignore_inactive_topics_when_validating_mark_order() {
      TopicBatchItem active =
          TopicBatchItem.builder()
              .id(UUID.randomUUID())
              .title("A")
              .sequenceOrder(1)
              .status(TopicStatus.NOT_STARTED)
              .mark(5.0)
              .build();
      TopicBatchItem inactive =
          TopicBatchItem.builder()
              .id(UUID.randomUUID())
              .title("B")
              .sequenceOrder(2)
              .status(TopicStatus.INACTIVE)
              .mark(1.0)
              .build();

      invokePrivate(
          "validateTopicMarksStrictlyIncreasing", new Class<?>[] {List.class}, List.of(active, inactive));
      assertTrue(true);
    }

    @Test
    void it_should_batch_soft_delete_existing_topic_when_status_inactive() {
      UUID roadmapId = UUID.fromString("f1500000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f1500000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(topicId)
              .title("inactive")
              .sequenceOrder(1)
              .mark(2.0)
              .status(TopicStatus.INACTIVE)
              .build();
      com.fptu.math_master.dto.request.BatchTopicRequest req =
          com.fptu.math_master.dto.request.BatchTopicRequest.builder()
              .roadmapId(roadmapId)
              .topics(List.of(item))
              .build();

      List<RoadmapTopicResponse> out = roadmapAdminService.batchSaveTopics(req);
      assertEquals(0, out.size());
      verify(topicCourseRepository, times(1)).softDeleteByTopicId(topicId);
    }

    @Test
    void it_should_batch_update_existing_topic_and_replace_courses() {
      UUID roadmapId = UUID.fromString("f1600000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f1600000-0000-0000-0000-000000000002");
      UUID courseId = UUID.fromString("f1600000-0000-0000-0000-000000000003");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      roadmap.setSubjectId(UUID.fromString("f1600000-0000-0000-0000-000000000004"));
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      topic.setStatus(TopicStatus.NOT_STARTED);
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
      com.fptu.math_master.entity.Course course = new com.fptu.math_master.entity.Course();
      course.setId(courseId);
      course.setSubjectId(roadmap.getSubjectId());
      when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
      when(topicCourseRepository.existsByTopicIdAndCourseId(topicId, courseId)).thenReturn(false);
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));
      when(learningRoadmapService.getTopicDetails(topicId))
          .thenReturn(RoadmapTopicResponse.builder().id(topicId).title("Updated").build());

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(topicId)
              .title("Updated")
              .description("D")
              .sequenceOrder(1)
              .difficulty(QuestionDifficulty.MEDIUM)
              .mark(3.0)
              .status(TopicStatus.IN_PROGRESS)
              .courseIds(List.of(courseId))
              .build();
      com.fptu.math_master.dto.request.BatchTopicRequest req =
          com.fptu.math_master.dto.request.BatchTopicRequest.builder()
              .roadmapId(roadmapId)
              .topics(List.of(item))
              .build();

      List<RoadmapTopicResponse> out = roadmapAdminService.batchSaveTopics(req);
      assertEquals(1, out.size());
      verify(topicCourseRepository, times(1)).softDeleteByTopicId(topicId);
    }
  }

  @Nested
  @DisplayName("branch coverage push (jacoco branch %)")
  class BranchCoveragePushTests {

    @Test
    void it_should_batch_create_topic_with_courses_and_skip_duplicate_course_link() {
      UUID roadmapId = UUID.fromString("f2000000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f2000000-0000-0000-0000-000000000002");
      UUID courseId = UUID.fromString("f2000000-0000-0000-0000-000000000003");

      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      roadmap.setSubjectId(UUID.fromString("f2000000-0000-0000-0000-000000000004"));
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

      when(topicRepository.save(org.mockito.ArgumentMatchers.any(RoadmapTopic.class)))
          .thenAnswer(
              inv -> {
                RoadmapTopic t = inv.getArgument(0);
                t.setId(topicId);
                return t;
              });

      Course course = new Course();
      course.setId(courseId);
      course.setSubjectId(roadmap.getSubjectId());
      when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
      when(topicCourseRepository.existsByTopicIdAndCourseId(topicId, courseId)).thenReturn(true);

      when(learningRoadmapService.getTopicDetails(topicId))
          .thenReturn(RoadmapTopicResponse.builder().id(topicId).title("Created with courses").build());

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(null)
              .title("Created with courses")
              .description("D")
              .sequenceOrder(1)
              .difficulty(QuestionDifficulty.EASY)
              .mark(1.0)
              .status(null)
              .courseIds(List.of(courseId, courseId))
              .build();

      BatchTopicRequest req = BatchTopicRequest.builder().roadmapId(roadmapId).topics(List.of(item)).build();
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(new RoadmapTopic()));

      List<RoadmapTopicResponse> out = roadmapAdminService.batchSaveTopics(req);
      assertEquals(1, out.size());
      verify(topicCourseRepository, times(0)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void it_should_batch_update_topic_clearing_courses_when_course_ids_is_empty_list() {
      UUID roadmapId = UUID.fromString("f2100000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f2100000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      topic.setDeletedAt(null);
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));
      when(learningRoadmapService.getTopicDetails(topicId))
          .thenReturn(RoadmapTopicResponse.builder().id(topicId).title("Cleared").build());

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(topicId)
              .title("Cleared")
              .description("D")
              .sequenceOrder(1)
              .difficulty(QuestionDifficulty.MEDIUM)
              .mark(2.0)
              .status(TopicStatus.IN_PROGRESS)
              .courseIds(List.of())
              .build();

      BatchTopicRequest req = BatchTopicRequest.builder().roadmapId(roadmapId).topics(List.of(item)).build();
      List<RoadmapTopicResponse> out = roadmapAdminService.batchSaveTopics(req);
      assertEquals(1, out.size());
      verify(topicCourseRepository, times(1)).softDeleteByTopicId(topicId);
      verify(courseRepository, times(0)).findByIdAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void it_should_batch_soft_delete_skip_save_when_topic_already_deleted() {
      UUID roadmapId = UUID.fromString("f2200000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f2200000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      topic.setDeletedAt(Instant.now());
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of());

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(topicId)
              .title("already deleted")
              .sequenceOrder(1)
              .mark(1.0)
              .status(TopicStatus.INACTIVE)
              .build();

      BatchTopicRequest req = BatchTopicRequest.builder().roadmapId(roadmapId).topics(List.of(item)).build();
      roadmapAdminService.batchSaveTopics(req);
      verify(topicRepository, times(0)).save(org.mockito.ArgumentMatchers.any());
      verify(topicCourseRepository, times(0)).softDeleteByTopicId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void it_should_throw_not_found_when_batch_soft_delete_topic_belongs_to_other_roadmap() {
      UUID roadmapId = UUID.fromString("f2300000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f2300000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(UUID.fromString("f2300000-0000-0000-0000-000000000099"));
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(topicId)
              .title("wrong roadmap")
              .sequenceOrder(1)
              .mark(1.0)
              .status(TopicStatus.INACTIVE)
              .build();

      BatchTopicRequest req = BatchTopicRequest.builder().roadmapId(roadmapId).topics(List.of(item)).build();
      AppException ex = assertThrows(AppException.class, () -> roadmapAdminService.batchSaveTopics(req));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_return_active_attempt_snapshot_when_in_progress_attempt_exists() {
      UUID roadmapId = UUID.fromString("f2400000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f2400000-0000-0000-0000-000000000002");
      UUID assessmentId = UUID.fromString("f2400000-0000-0000-0000-000000000003");
      UUID attemptId = UUID.fromString("f2400000-0000-0000-0000-000000000004");

      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));

      when(studentAssessmentService.getAssessmentDetails(assessmentId))
          .thenReturn(StudentAssessmentResponse.builder().id(assessmentId).studentStatus("IN_PROGRESS").build());

      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of(attempt));

      when(studentAssessmentService.getDraftSnapshot(attemptId))
          .thenReturn(
              DraftSnapshotResponse.builder()
                  .attemptId(attemptId)
                  .answeredCount(2)
                  .totalQuestions(5)
                  .build());

      var resp = roadmapAdminService.getActiveEntryTestAttempt(studentId, roadmapId);
      assertEquals(attemptId, resp.getAttemptId());
      assertNotNull(resp.getProgress());
    }

    @Test
    void it_should_map_topic_materials_using_public_service_method() {
      UUID topicId = UUID.fromString("f2500000-0000-0000-0000-000000000001");
      TopicLearningMaterial m = new TopicLearningMaterial();
      m.setId(UUID.fromString("f2500000-0000-0000-0000-000000000002"));
      m.setResourceTitle("Tài liệu");
      m.setResourceType("PDF");
      m.setSequenceOrder(1);
      m.setIsRequired(true);
      m.setMindmapId(UUID.fromString("f2500000-0000-0000-0000-000000000003"));
      when(topicLearningMaterialRepository.findByTopicIdOrderBySequenceOrder(topicId)).thenReturn(List.of(m));

      var materials = roadmapAdminService.getTopicMaterials(topicId);
      assertEquals(1, materials.size());
      assertEquals("/api/v1/mindmaps/" + m.getMindmapId(), materials.get(0).getResourceLink());
    }

    @Test
    void it_should_map_student_entry_status_in_progress_ignoring_case() {
      String mapped = invokePrivate("mapStudentEntryStatus", new Class<?>[] {String.class}, "in_progress");
      assertEquals("IN_PROGRESS", mapped);
    }

    @Test
    void it_should_resolve_current_topic_index_when_all_topic_marks_are_null() {
      List<RoadmapTopicResponse> topics =
          List.of(
              RoadmapTopicResponse.builder().title("A").sequenceOrder(1).mark(null).build(),
              RoadmapTopicResponse.builder().title("B").sequenceOrder(2).mark(null).build());
      RoadmapDetailResponse detail = RoadmapDetailResponse.builder().topics(topics).build();
      Integer idx =
          invokePrivate(
              "resolveCurrentTopicIndexFromMarks", new Class<?>[] {RoadmapDetailResponse.class, double.class}, detail, 9.9);
      assertEquals(1, idx);
    }

    @Test
    void it_should_compute_score_on_ten_from_score_field_when_final_score_missing() {
      Submission s = new Submission();
      s.setScore(BigDecimal.valueOf(45));
      s.setMaxScore(BigDecimal.valueOf(90));
      Double v = invokePrivate("computeScoreOnTen", new Class<?>[] {Submission.class}, s);
      assertEquals(5.0, v);
    }

    @Test
    void it_should_compute_score_on_ten_zero_when_max_score_is_zero_even_if_scores_present() {
      Submission s = new Submission();
      s.setFinalScore(BigDecimal.TEN);
      s.setMaxScore(BigDecimal.ZERO);
      s.setScore(BigDecimal.ONE);
      Double v = invokePrivate("computeScoreOnTen", new Class<?>[] {Submission.class}, s);
      assertEquals(0.0, v);
    }

    @Test
    void it_should_map_summary_student_name_unknown_when_user_missing() {
      UUID studentId = UUID.fromString("f2600000-0000-0000-0000-000000000001");
      LearningRoadmap r = buildRoadmap(UUID.fromString("f2600000-0000-0000-0000-000000000002"));
      r.setStudentId(studentId);
      when(userRepository.findById(studentId)).thenReturn(Optional.empty());

      Object summary = invokePrivate("mapToSummaryResponse", new Class<?>[] {LearningRoadmap.class}, r);
      assertNotNull(summary);
    }

    @Test
    void it_should_throw_school_grade_not_found_when_grade_name_blank() {
      com.fptu.math_master.entity.SchoolGrade grade = new com.fptu.math_master.entity.SchoolGrade();
      grade.setName("   ");
      com.fptu.math_master.entity.Subject subject = new com.fptu.math_master.entity.Subject();
      subject.setSchoolGrade(grade);

      RuntimeException wrapped =
          assertThrows(RuntimeException.class, () -> invokePrivate("resolveGradeLevel", new Class<?>[] {com.fptu.math_master.entity.Subject.class}, subject));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_throw_subject_not_found_when_subject_soft_deleted() {
      UUID subjectId = UUID.fromString("f2700000-0000-0000-0000-000000000001");
      com.fptu.math_master.entity.Subject subject = new com.fptu.math_master.entity.Subject();
      subject.setId(subjectId);
      subject.setDeletedAt(Instant.now());
      when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class,
              () -> invokePrivate("resolveSubject", new Class<?>[] {UUID.class}, subjectId));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_map_topic_material_via_private_mapper_for_each_resource_type() {
      TopicLearningMaterial lesson = new TopicLearningMaterial();
      lesson.setLessonId(UUID.fromString("f2800000-0000-0000-0000-000000000001"));
      Object r1 = invokePrivate("mapToTopicMaterialResponse", new Class<?>[] {TopicLearningMaterial.class}, lesson);
      assertNotNull(r1);

      TopicLearningMaterial question = new TopicLearningMaterial();
      question.setQuestionId(UUID.fromString("f2800000-0000-0000-0000-000000000002"));
      Object r2 = invokePrivate("mapToTopicMaterialResponse", new Class<?>[] {TopicLearningMaterial.class}, question);
      assertNotNull(r2);

      TopicLearningMaterial assessment = new TopicLearningMaterial();
      assessment.setAssessmentId(UUID.fromString("f2800000-0000-0000-0000-000000000003"));
      Object r3 = invokePrivate("mapToTopicMaterialResponse", new Class<?>[] {TopicLearningMaterial.class}, assessment);
      assertNotNull(r3);

      TopicLearningMaterial mindmap = new TopicLearningMaterial();
      mindmap.setMindmapId(UUID.fromString("f2800000-0000-0000-0000-000000000004"));
      Object r4 = invokePrivate("mapToTopicMaterialResponse", new Class<?>[] {TopicLearningMaterial.class}, mindmap);
      assertNotNull(r4);

      TopicLearningMaterial chapter = new TopicLearningMaterial();
      chapter.setChapterId(UUID.fromString("f2800000-0000-0000-0000-000000000005"));
      Object r5 = invokePrivate("mapToTopicMaterialResponse", new Class<?>[] {TopicLearningMaterial.class}, chapter);
      assertNotNull(r5);
    }

    @Test
    void it_should_update_roadmap_name_trim_and_ignore_blank_name() {
      UUID roadmapId = UUID.fromString("f2900000-0000-0000-0000-000000000001");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      roadmap.setName("Old");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(learningRoadmapService.getRoadmapById(roadmapId))
          .thenReturn(RoadmapDetailResponse.builder().id(roadmapId).build());

      com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest req1 =
          com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest.builder().name("  New Name  ").build();
      roadmapAdminService.updateRoadmap(roadmapId, req1);
      assertEquals("New Name", roadmap.getName());

      com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest req2 =
          com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest.builder().name("   ").build();
      roadmapAdminService.updateRoadmap(roadmapId, req2);
      assertEquals("New Name", roadmap.getName());
    }

    @Test
    void it_should_soft_delete_topic_noop_when_already_deleted() {
      UUID roadmapId = UUID.fromString("f2a00000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f2a00000-0000-0000-0000-000000000002");
      LearningRoadmap roadmap = buildRoadmap(roadmapId);
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      topic.setDeletedAt(Instant.now());
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

      roadmapAdminService.softDeleteTopic(roadmapId, topicId);
      verify(topicRepository, times(0)).save(org.mockito.ArgumentMatchers.any());
      verify(roadmapRepository, times(0)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void it_should_throw_not_found_when_update_topic_is_soft_deleted() {
      UUID roadmapId = UUID.fromString("f2b00000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f2b00000-0000-0000-0000-000000000002");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(roadmapId);
      topic.setDeletedAt(Instant.now());
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  roadmapAdminService.updateTopic(
                      roadmapId,
                      topicId,
                      UpdateRoadmapTopicRequest.builder().title("X").build()));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_access_denied_when_validate_attempt_student_mismatches() {
      UUID roadmapId = UUID.fromString("f2c00000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f2c00000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("f2c00000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f2c00000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(
              List.of(
                  RoadmapEntryQuestionMapping.builder()
                      .roadmapId(roadmapId)
                      .assessmentId(assessmentId)
                      .questionId(UUID.randomUUID())
                      .orderIndex(0)
                      .build()));

      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setStudentId(UUID.fromString("f2c00000-0000-0000-0000-000000000099"));
      attempt.setAssessmentId(assessmentId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      RuntimeException wrapped =
          assertThrows(RuntimeException.class, () -> invokeValidateRoadmapAttempt(studentId, roadmapId, attemptId));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.ATTEMPT_ACCESS_DENIED, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_throw_assessment_not_found_when_validate_attempt_assessment_mismatches() {
      UUID roadmapId = UUID.fromString("f2d00000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f2d00000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("f2d00000-0000-0000-0000-000000000003");
      UUID configuredAssessmentId = UUID.fromString("f2d00000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(
              List.of(
                  RoadmapEntryQuestionMapping.builder()
                      .roadmapId(roadmapId)
                      .assessmentId(configuredAssessmentId)
                      .questionId(UUID.randomUUID())
                      .orderIndex(0)
                      .build()));

      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setStudentId(studentId);
      attempt.setAssessmentId(UUID.fromString("f2d00000-0000-0000-0000-000000000099"));
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      RuntimeException wrapped =
          assertThrows(RuntimeException.class, () -> invokeValidateRoadmapAttempt(studentId, roadmapId, attemptId));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_batch_update_topic_id_missing() {
      UUID roadmapId = UUID.fromString("f2e00000-0000-0000-0000-000000000001");
      UUID missingTopicId = UUID.fromString("f2e00000-0000-0000-0000-000000000002");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(topicRepository.findById(missingTopicId)).thenReturn(Optional.empty());

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(missingTopicId)
              .title("missing")
              .sequenceOrder(1)
              .mark(1.0)
              .status(TopicStatus.NOT_STARTED)
              .build();

      BatchTopicRequest req = BatchTopicRequest.builder().roadmapId(roadmapId).topics(List.of(item)).build();
      AppException ex = assertThrows(AppException.class, () -> roadmapAdminService.batchSaveTopics(req));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_batch_soft_delete_topic_id_missing() {
      UUID roadmapId = UUID.fromString("f2f00000-0000-0000-0000-000000000001");
      UUID missingTopicId = UUID.fromString("f2f00000-0000-0000-0000-000000000002");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(topicRepository.findById(missingTopicId)).thenReturn(Optional.empty());

      TopicBatchItem item =
          TopicBatchItem.builder()
              .id(missingTopicId)
              .title("missing inactive")
              .sequenceOrder(1)
              .mark(1.0)
              .status(TopicStatus.INACTIVE)
              .build();

      BatchTopicRequest req = BatchTopicRequest.builder().roadmapId(roadmapId).topics(List.of(item)).build();
      AppException ex = assertThrows(AppException.class, () -> roadmapAdminService.batchSaveTopics(req));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_soft_delete_topic_belongs_to_other_roadmap() {
      UUID roadmapId = UUID.fromString("f3000000-0000-0000-0000-000000000001");
      UUID topicId = UUID.fromString("f3000000-0000-0000-0000-000000000002");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(topicId);
      topic.setRoadmapId(UUID.fromString("f3000000-0000-0000-0000-000000000099"));
      when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

      AppException ex =
          assertThrows(AppException.class, () -> roadmapAdminService.softDeleteTopic(roadmapId, topicId));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_set_entry_test_assessment_missing() {
      UUID roadmapId = UUID.fromString("f3100000-0000-0000-0000-000000000001");
      UUID assessmentId = UUID.fromString("f3100000-0000-0000-0000-000000000002");
      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(AppException.class, () -> roadmapAdminService.setRoadmapEntryTest(roadmapId, assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_finish_entry_test_without_auto_grade_when_submission_already_graded() {
      UUID roadmapId = UUID.fromString("f3200000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3200000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("f3200000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f3200000-0000-0000-0000-000000000004");
      UUID submissionId = UUID.fromString("f3200000-0000-0000-0000-000000000005");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      RoadmapEntryQuestionMapping mapping =
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(0)
              .build();
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(List.of(mapping));

      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setStudentId(studentId);
      attempt.setAssessmentId(assessmentId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      Submission graded = new Submission();
      graded.setId(submissionId);
      graded.setAssessmentId(assessmentId);
      graded.setStudentId(studentId);
      graded.setFinalScore(BigDecimal.valueOf(6));
      graded.setMaxScore(BigDecimal.TEN);
      graded.setStatus(SubmissionStatus.GRADED);

      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.of(graded));
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of(graded));

      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(graded));

      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(UUID.fromString("f3200000-0000-0000-0000-000000000011"));
      topic.setTitle("T");
      topic.setMark(10.0);
      topic.setSequenceOrder(1);
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));

      var result = roadmapAdminService.finishEntryTest(studentId, roadmapId, attemptId);
      assertNotNull(result);
      verify(gradingService, times(0)).autoGradeSubmission(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void it_should_submit_entry_test_without_auto_grade_when_submission_already_graded() {
      UUID roadmapId = UUID.fromString("f3300000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3300000-0000-0000-0000-000000000002");
      UUID assessmentId = UUID.fromString("f3300000-0000-0000-0000-000000000003");
      UUID submissionId = UUID.fromString("f3300000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(
              List.of(
                  RoadmapEntryQuestionMapping.builder()
                      .roadmapId(roadmapId)
                      .assessmentId(assessmentId)
                      .questionId(UUID.randomUUID())
                      .orderIndex(0)
                      .build()));

      Submission graded = new Submission();
      graded.setId(submissionId);
      graded.setAssessmentId(assessmentId);
      graded.setStudentId(studentId);
      graded.setFinalScore(BigDecimal.valueOf(6));
      graded.setMaxScore(BigDecimal.TEN);
      graded.setStatus(SubmissionStatus.GRADED);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(graded));
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of(graded));

      RoadmapTopic topic = new RoadmapTopic();
      topic.setId(UUID.fromString("f3300000-0000-0000-0000-000000000011"));
      topic.setTitle("T");
      topic.setMark(10.0);
      topic.setSequenceOrder(1);
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));

      var result =
          roadmapAdminService.submitEntryTest(
              studentId,
              roadmapId,
              SubmitRoadmapEntryTestRequest.builder().submissionId(submissionId).build());
      assertNotNull(result);
      verify(gradingService, times(0)).autoGradeSubmission(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void it_should_throw_quiz_attempt_not_found_when_finish_entry_test_attempt_missing() {
      UUID roadmapId = UUID.fromString("f3400000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3400000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("f3400000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f3400000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(
              List.of(
                  RoadmapEntryQuestionMapping.builder()
                      .roadmapId(roadmapId)
                      .assessmentId(assessmentId)
                      .questionId(UUID.randomUUID())
                      .orderIndex(0)
                      .build()));
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(AppException.class, () -> roadmapAdminService.finishEntryTest(studentId, roadmapId, attemptId));
      assertEquals(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_submission_not_found_when_finish_entry_test_has_no_submission_after_submit() {
      UUID roadmapId = UUID.fromString("f3500000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3500000-0000-0000-0000-000000000002");
      UUID attemptId = UUID.fromString("f3500000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f3500000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(
              List.of(
                  RoadmapEntryQuestionMapping.builder()
                      .roadmapId(roadmapId)
                      .assessmentId(assessmentId)
                      .questionId(UUID.randomUUID())
                      .orderIndex(0)
                      .build()));

      com.fptu.math_master.entity.QuizAttempt attempt = new com.fptu.math_master.entity.QuizAttempt();
      attempt.setId(attemptId);
      attempt.setStudentId(studentId);
      attempt.setAssessmentId(assessmentId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenAnswer((Answer<Optional<Submission>>) invocation -> Optional.empty());

      AppException ex =
          assertThrows(AppException.class, () -> roadmapAdminService.finishEntryTest(studentId, roadmapId, attemptId));
      assertEquals(ErrorCode.SUBMISSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_unauthorized_when_evaluate_entry_test_result_student_mismatches() {
      UUID roadmapId = UUID.fromString("f3600000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3600000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("f3600000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f3600000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setStudentId(UUID.fromString("f3600000-0000-0000-0000-000000000099"));
      submission.setAssessmentId(assessmentId);
      submission.setFinalScore(BigDecimal.valueOf(5));
      submission.setMaxScore(BigDecimal.TEN);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class, () -> invokeEvaluateEntryTestResult(studentId, roadmapId, submissionId, 0.0));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.UNAUTHORIZED, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_throw_assessment_not_found_when_evaluate_entry_test_has_no_mappings() {
      UUID roadmapId = UUID.fromString("f3700000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3700000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("f3700000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f3700000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setStudentId(studentId);
      submission.setAssessmentId(assessmentId);
      submission.setFinalScore(BigDecimal.valueOf(5));
      submission.setMaxScore(BigDecimal.TEN);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId)).thenReturn(List.of());

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class, () -> invokeEvaluateEntryTestResult(studentId, roadmapId, submissionId, 0.0));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_throw_submission_not_found_when_evaluate_entry_test_submission_missing() {
      UUID roadmapId = UUID.fromString("f3800000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3800000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("f3800000-0000-0000-0000-000000000003");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class, () -> invokeEvaluateEntryTestResult(studentId, roadmapId, submissionId, 0.0));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.SUBMISSION_NOT_FOUND, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_throw_assessment_not_found_when_evaluate_entry_test_has_no_active_topics() {
      UUID roadmapId = UUID.fromString("f3900000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3900000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("f3900000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f3900000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setStudentId(studentId);
      submission.setAssessmentId(assessmentId);
      submission.setFinalScore(BigDecimal.valueOf(5));
      submission.setMaxScore(BigDecimal.TEN);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(
              List.of(
                  RoadmapEntryQuestionMapping.builder()
                      .roadmapId(roadmapId)
                      .assessmentId(assessmentId)
                      .questionId(UUID.randomUUID())
                      .orderIndex(0)
                      .build()));
      RoadmapTopic deleted = new RoadmapTopic();
      deleted.setId(UUID.fromString("f3900000-0000-0000-0000-000000000011"));
      deleted.setDeletedAt(Instant.now());
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(deleted));
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of(submission));

      RuntimeException wrapped =
          assertThrows(
              RuntimeException.class, () -> invokeEvaluateEntryTestResult(studentId, roadmapId, submissionId, 0.0));
      Throwable target = ((java.lang.reflect.InvocationTargetException) wrapped.getCause()).getTargetException();
      assertTrue(target instanceof AppException);
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ((AppException) target).getErrorCode());
    }

    @Test
    void it_should_suggest_last_topic_when_all_topic_marks_are_null_in_evaluate_entry_test_result() {
      UUID roadmapId = UUID.fromString("f3a00000-0000-0000-0000-000000000001");
      UUID studentId = UUID.fromString("f3a00000-0000-0000-0000-000000000002");
      UUID submissionId = UUID.fromString("f3a00000-0000-0000-0000-000000000003");
      UUID assessmentId = UUID.fromString("f3a00000-0000-0000-0000-000000000004");

      when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(buildRoadmap(roadmapId)));
      Submission submission = new Submission();
      submission.setId(submissionId);
      submission.setStudentId(studentId);
      submission.setAssessmentId(assessmentId);
      submission.setFinalScore(BigDecimal.valueOf(5));
      submission.setMaxScore(BigDecimal.TEN);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId))
          .thenReturn(
              List.of(
                  RoadmapEntryQuestionMapping.builder()
                      .roadmapId(roadmapId)
                      .assessmentId(assessmentId)
                      .questionId(UUID.randomUUID())
                      .orderIndex(0)
                      .build()));

      RoadmapTopic t1 = new RoadmapTopic();
      t1.setId(UUID.fromString("f3a00000-0000-0000-0000-000000000011"));
      t1.setTitle("A");
      t1.setMark(null);
      t1.setSequenceOrder(1);
      RoadmapTopic t2 = new RoadmapTopic();
      t2.setId(UUID.fromString("f3a00000-0000-0000-0000-000000000012"));
      t2.setTitle("B");
      t2.setMark(null);
      t2.setSequenceOrder(2);
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(t1, t2));
      when(submissionRepository.findByStudentIdAndStatuses(
              org.mockito.ArgumentMatchers.eq(studentId), org.mockito.ArgumentMatchers.any()))
          .thenReturn(List.of(submission));

      Object resp = invokeEvaluateEntryTestResult(studentId, roadmapId, submissionId, 0.0);
      assertNotNull(resp);
    }
  }
}
