package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.UpdateTopicProgressRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapStatsResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.TopicCourse;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.TopicCourseRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("LearningRoadmapServiceImpl - Tests")
class LearningRoadmapServiceImplTest extends BaseUnitTest {

  private static final UUID ROADMAP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TEACHER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUBJECT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID TOPIC_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID COURSE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID ENTRY_TEST_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID ENROLLMENT_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

  @InjectMocks private LearningRoadmapServiceImpl learningRoadmapService;

  @Mock private LearningRoadmapRepository roadmapRepository;
  @Mock private RoadmapTopicRepository topicRepository;
  @Mock private TopicCourseRepository topicCourseRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private LessonProgressRepository lessonProgressRepository;
  @Mock private AssessmentRepository assessmentRepository;

  @Nested
  @DisplayName("getRoadmapById()")
  class GetRoadmapByIdTests {

    /**
     * Abnormal case: Ném exception khi roadmap không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmapId: ROADMAP_ID (không có trong repository)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>roadmapRepository.findById() -> empty (throw ASSESSMENT_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_roadmap_is_not_found() {
      // ===== ARRANGE =====
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> learningRoadmapService.getRoadmapById(ROADMAP_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
      verifyNoMoreInteractions(roadmapRepository);
    }

    /**
     * Normal case: Trả về roadmap detail và map đầy đủ thông tin topic/courses.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap: có 1 topic, entryTestId = null</li>
     *   <li>topic: có 1 course đang tồn tại, student đã enroll</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>entryTestId == null (skip entryTest lookup)</li>
     *   <li>course != null (normal course mapping branch)</li>
     *   <li>studentId != null và enrollment != null</li>
     *   <li>totalLessons > 0 (tính progress từ lesson ratio)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về response có topic/courses và progress chính xác</li>
     * </ul>
     */
    @Test
    void it_should_return_detail_response_when_roadmap_exists() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, BigDecimal.ZERO);
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.EASY, 1);
      TopicCourse topicCourse = buildTopicCourse(TOPIC_ID, COURSE_ID);
      Course course = buildCourse(COURSE_ID, "Giải Tích 1 - Nền Tảng");
      Enrollment enrollment = buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID);

      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(List.of(topic));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(List.of(topicCourse));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(10L);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(enrollment));
      when(lessonProgressRepository.countCompletedByEnrollmentId(ENROLLMENT_ID)).thenReturn(4L);

      // ===== ACT =====
      RoadmapDetailResponse result = learningRoadmapService.getRoadmapById(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(ROADMAP_ID, result.getId()),
          () -> assertEquals(1, result.getTopics().size()),
          () -> assertNotNull(result.getStats()),
          () -> assertEquals(new BigDecimal("40.00"), result.getProgressPercentage()),
          () -> assertEquals(1, result.getTotalTopicsCount()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(2)).findById(ROADMAP_ID);
      verify(topicRepository, times(2)).findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID);
      verify(topicCourseRepository, times(1)).findByTopicId(TOPIC_ID);
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(courseLessonRepository, times(1)).countByCourseIdAndNotDeleted(COURSE_ID);
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID);
      verify(lessonProgressRepository, times(1)).countCompletedByEnrollmentId(ENROLLMENT_ID);
    }

    /**
     * Normal case: Có entry test hợp lệ thì trả về entryTest info.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap.entryTestId: ENTRY_TEST_ID</li>
     *   <li>assessment tồn tại và countQuestions trả null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>entryTestId != null</li>
     *   <li>assessment != null</li>
     *   <li>totalQuestions == null -> fallback 0</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>entryTest được map với totalQuestions = 0</li>
     * </ul>
     */
    @Test
    void it_should_include_entry_test_info_when_entry_test_exists() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, BigDecimal.ZERO);
      roadmap.setEntryTestId(ENTRY_TEST_ID);
      Assessment assessment = buildAssessment(ENTRY_TEST_ID);

      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(Collections.emptyList());
      when(assessmentRepository.findByIdAndNotDeleted(ENTRY_TEST_ID)).thenReturn(Optional.of(assessment));
      when(assessmentRepository.countQuestionsByAssessmentId(ENTRY_TEST_ID)).thenReturn(null);

      // ===== ACT =====
      RoadmapDetailResponse result = learningRoadmapService.getRoadmapById(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertNotNull(result.getEntryTest()),
          () -> assertEquals(ENTRY_TEST_ID, result.getEntryTest().getAssessmentId()),
          () -> assertEquals(0, result.getEntryTest().getTotalQuestions()));

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(ENTRY_TEST_ID);
      verify(assessmentRepository, times(1)).countQuestionsByAssessmentId(ENTRY_TEST_ID);
    }

    /**
     * Abnormal case: Entry test bị xóa thì service clear reference.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap.entryTestId: ENTRY_TEST_ID</li>
     *   <li>assessmentRepository trả empty</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>entryTestId != null</li>
     *   <li>assessment == null -> clear entryTestId và save roadmap</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>roadmap.entryTestId bị set null</li>
     * </ul>
     */
    @Test
    void it_should_clear_entry_test_reference_when_assessment_is_missing() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, BigDecimal.ZERO);
      roadmap.setEntryTestId(ENTRY_TEST_ID);

      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(Collections.emptyList());
      when(assessmentRepository.findByIdAndNotDeleted(ENTRY_TEST_ID)).thenReturn(Optional.empty());

      // ===== ACT =====
      RoadmapDetailResponse result = learningRoadmapService.getRoadmapById(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertNull(result.getEntryTest()), () -> assertNull(roadmap.getEntryTestId()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(2)).save(roadmap);
    }

    /**
     * Abnormal case: Lỗi khi load entry test thì service vẫn clear reference.
     *
     * <p>Input:
     * <ul>
     *   <li>assessmentRepository.findByIdAndNotDeleted ném RuntimeException</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>catch Exception branch trong load entry test</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Không throw ra ngoài, roadmap.entryTestId được set null</li>
     * </ul>
     */
    @Test
    void it_should_clear_entry_test_reference_when_loading_entry_test_throws_exception() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, BigDecimal.ZERO);
      roadmap.setEntryTestId(ENTRY_TEST_ID);

      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(Collections.emptyList());
      when(assessmentRepository.findByIdAndNotDeleted(ENTRY_TEST_ID))
          .thenThrow(new RuntimeException("Assessment DB timeout"));

      // ===== ACT =====
      RoadmapDetailResponse result = learningRoadmapService.getRoadmapById(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertNull(roadmap.getEntryTestId()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(2)).save(roadmap);
    }
  }

  @Nested
  @DisplayName("updateTopicProgress()")
  class UpdateTopicProgressTests {

    /**
     * Abnormal case: Ném exception khi topic không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>topicId: TOPIC_ID (không có)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>topicRepository.findById() -> empty</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_updating_progress_for_missing_topic() {
      // ===== ARRANGE =====
      UpdateTopicProgressRequest request =
          UpdateTopicProgressRequest.builder()
              .topicId(TOPIC_ID)
              .status(TopicStatus.IN_PROGRESS)
              .progressPercentage(new BigDecimal("30"))
              .build();
      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> learningRoadmapService.updateTopicProgress(request));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findById(TOPIC_ID);
      verify(topicRepository, never()).save(any(RoadmapTopic.class));
    }

    /**
     * Normal case: Chuyển trạng thái sang IN_PROGRESS và set startedAt.
     *
     * <p>Input:
     * <ul>
     *   <li>previousStatus: NOT_STARTED</li>
     *   <li>newStatus: IN_PROGRESS</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>previousStatus != IN_PROGRESS và newStatus == IN_PROGRESS</li>
     *   <li>completion branch FALSE</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Topic được set startedAt và progress giữ theo request</li>
     * </ul>
     */
    @Test
    void it_should_set_started_at_when_topic_transitions_to_in_progress() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.GENERATED, BigDecimal.ZERO);
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.NOT_STARTED, QuestionDifficulty.MEDIUM, 1);
      UpdateTopicProgressRequest request =
          UpdateTopicProgressRequest.builder()
              .topicId(TOPIC_ID)
              .status(TopicStatus.IN_PROGRESS)
              .progressPercentage(new BigDecimal("25"))
              .build();

      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
      when(topicRepository.save(any(RoadmapTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(roadmapRepository.findById(ROADMAP_ID))
          .thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(List.of(topic));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(Collections.emptyList());

      // ===== ACT =====
      RoadmapTopicResponse result = learningRoadmapService.updateTopicProgress(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(TOPIC_ID, result.getId()),
          () -> assertNotNull(topic.getStartedAt()),
          () -> assertNull(topic.getCompletedAt()));

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findById(TOPIC_ID);
      verify(topicRepository, times(1)).save(topic);
      verify(roadmapRepository, times(2)).findById(ROADMAP_ID);
      verify(roadmapRepository, times(1)).save(roadmap);
    }

    /**
     * Normal case: Không set lại startedAt khi topic đã IN_PROGRESS.
     *
     * <p>Input:
     * <ul>
     *   <li>previousStatus: IN_PROGRESS</li>
     *   <li>newStatus: IN_PROGRESS</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>previousStatus != IN_PROGRESS -> FALSE branch</li>
     *   <li>completed transition condition -> FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>startedAt giữ nguyên giá trị cũ, completedAt vẫn null</li>
     * </ul>
     */
    @Test
    void it_should_keep_started_at_unchanged_when_topic_remains_in_progress() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("35.00"));
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.MEDIUM, 1);
      Instant existingStartedAt = Instant.parse("2026-01-10T08:00:00Z");
      topic.setStartedAt(existingStartedAt);

      UpdateTopicProgressRequest request =
          UpdateTopicProgressRequest.builder()
              .topicId(TOPIC_ID)
              .status(TopicStatus.IN_PROGRESS)
              .progressPercentage(new BigDecimal("40"))
              .build();

      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
      when(topicRepository.save(any(RoadmapTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(List.of(topic));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(Collections.emptyList());

      // ===== ACT =====
      RoadmapTopicResponse result = learningRoadmapService.updateTopicProgress(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(existingStartedAt, topic.getStartedAt()),
          () -> assertNull(topic.getCompletedAt()));
    }

    /**
     * Normal case: Chuyển trạng thái sang COMPLETED thì progress bị ép về 100%.
     *
     * <p>Input:
     * <ul>
     *   <li>previousStatus: IN_PROGRESS</li>
     *   <li>newStatus: COMPLETED</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>completed branch TRUE -> set completedAt + progress=100</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Topic response có progress = 100.0 và completedAt khác null</li>
     * </ul>
     */
    @Test
    void it_should_force_progress_to_hundred_when_topic_is_completed() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap =
          buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("50.00"));
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.HARD, 1);
      UpdateTopicProgressRequest request =
          UpdateTopicProgressRequest.builder()
              .topicId(TOPIC_ID)
              .status(TopicStatus.COMPLETED)
              .progressPercentage(new BigDecimal("80"))
              .build();

      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
      when(topicRepository.save(any(RoadmapTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(roadmapRepository.findById(ROADMAP_ID))
          .thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(List.of(topic));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(Collections.emptyList());

      // ===== ACT =====
      RoadmapTopicResponse result = learningRoadmapService.updateTopicProgress(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(TOPIC_ID, result.getId()),
          () -> assertNotNull(topic.getCompletedAt()),
          () -> assertEquals(BigDecimal.valueOf(100), topic.getProgressPercentage()));

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findById(TOPIC_ID);
      verify(topicRepository, times(1)).save(topic);
      verify(roadmapRepository, times(1)).save(roadmap);
    }

    /**
     * Abnormal case: updateRoadmapProgress không tìm thấy roadmap và luồng public throw ở lần load sau.
     *
     * <p>Input:
     * <ul>
     *   <li>topic tồn tại nhưng roadmapRepository.findById luôn empty</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>updateRoadmapProgress(): roadmap == null -> return sớm</li>
     *   <li>updateTopicProgress(): load roadmap lần 2 -> throw exception</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_roadmap_missing_after_topic_is_saved() {
      // ===== ARRANGE =====
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.NOT_STARTED, QuestionDifficulty.MEDIUM, 1);
      UpdateTopicProgressRequest request =
          UpdateTopicProgressRequest.builder()
              .topicId(TOPIC_ID)
              .status(TopicStatus.IN_PROGRESS)
              .progressPercentage(new BigDecimal("10"))
              .build();

      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
      when(topicRepository.save(any(RoadmapTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> learningRoadmapService.updateTopicProgress(request));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(topicRepository, times(1)).save(topic);
      verify(roadmapRepository, times(2)).findById(ROADMAP_ID);
      verify(roadmapRepository, never()).save(any(LearningRoadmap.class));
    }

    /**
     * Normal case: Khi tất cả topic hoàn thành thì roadmap chuyển COMPLETED.
     *
     * <p>Input:
     * <ul>
     *   <li>topics gồm 1 completed topic đang update và 1 topic deleted</li>
     *   <li>course có 5 lessons và enrollment completed đủ 5</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>updateRoadmapProgress: totalLessons > 0</li>
     *   <li>topic.deletedAt != null -> continue branch</li>
     *   <li>completedCount >= topics.size() && !topics.isEmpty() -> TRUE</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>roadmap.status = COMPLETED và progress = 100</li>
     * </ul>
     */
    @Test
    void it_should_mark_roadmap_completed_when_all_topics_are_completed() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, BigDecimal.ZERO);
      RoadmapTopic updatedTopic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.EASY, 1);
      RoadmapTopic deletedTopic =
          buildTopic(UUID.randomUUID(), ROADMAP_ID, TopicStatus.COMPLETED, QuestionDifficulty.MEDIUM, 2);
      deletedTopic.setDeletedAt(Instant.now());
      TopicCourse topicCourse = buildTopicCourse(TOPIC_ID, COURSE_ID);
      Course course = buildCourse(COURSE_ID, "Đại số tuyến tính cơ bản");
      Enrollment enrollment = buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID);

      UpdateTopicProgressRequest request =
          UpdateTopicProgressRequest.builder()
              .topicId(TOPIC_ID)
              .status(TopicStatus.COMPLETED)
              .progressPercentage(new BigDecimal("90"))
              .build();

      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(updatedTopic));
      when(topicRepository.save(any(RoadmapTopic.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap), Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID))
          .thenReturn(List.of(updatedTopic, deletedTopic));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(List.of(topicCourse), List.of(topicCourse));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course), Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(5L, 5L);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(enrollment), Optional.of(enrollment));
      when(lessonProgressRepository.countCompletedByEnrollmentId(ENROLLMENT_ID)).thenReturn(5L, 5L);

      // ===== ACT =====
      RoadmapTopicResponse result = learningRoadmapService.updateTopicProgress(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(TOPIC_ID, result.getId()),
          () -> assertEquals(RoadmapStatus.COMPLETED, roadmap.getStatus()),
          () -> assertEquals(new BigDecimal("100"), roadmap.getProgressPercentage()),
          () -> assertNotNull(roadmap.getCompletedAt()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).save(roadmap);
    }
  }

  @Nested
  @DisplayName("getNextTopic()")
  class GetNextTopicTests {

    /**
     * Abnormal case: Ném exception khi không có next topic.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmapId: ROADMAP_ID (findNextTopic trả empty)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>topicRepository.findNextTopic() -> empty</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_next_topic_does_not_exist() {
      // ===== ARRANGE =====
      when(topicRepository.findNextTopic(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> learningRoadmapService.getNextTopic(ROADMAP_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findNextTopic(ROADMAP_ID);
      verify(roadmapRepository, never()).findById(any());
    }

    /**
     * Normal case: Trả về next topic khi topic và roadmap đều tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap.studentId: null</li>
     *   <li>topic có course link nhưng course đã deleted</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findNextTopic -> present</li>
     *   <li>studentId == null (skip enrollment check)</li>
     *   <li>course == null -> placeholder "Unavailable"</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Response có topic id đúng và course title = "Unavailable"</li>
     * </ul>
     */
    @Test
    void it_should_return_next_topic_with_unavailable_course_when_course_is_deleted() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.GENERATED, BigDecimal.ZERO);
      roadmap.setStudentId(null);
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.NOT_STARTED, QuestionDifficulty.EASY, 1);
      TopicCourse topicCourse = buildTopicCourse(TOPIC_ID, COURSE_ID);

      when(topicRepository.findNextTopic(ROADMAP_ID)).thenReturn(Optional.of(topic));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(List.of(topicCourse));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());

      // ===== ACT =====
      RoadmapTopicResponse result = learningRoadmapService.getNextTopic(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(TOPIC_ID, result.getId()),
          () -> assertEquals(1, result.getCourses().size()),
          () -> assertEquals("Unavailable", result.getCourses().get(0).getTitle()));

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findNextTopic(ROADMAP_ID);
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
    }

    /**
     * Abnormal case: Có next topic nhưng roadmap không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>findNextTopic trả về topic, roadmapRepository.findById trả empty</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getNextTopic(): Optional roadmap empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_next_topic_exists_but_roadmap_is_missing() {
      // ===== ARRANGE =====
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.NOT_STARTED, QuestionDifficulty.EASY, 1);
      when(topicRepository.findNextTopic(ROADMAP_ID)).thenReturn(Optional.of(topic));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> learningRoadmapService.getNextTopic(ROADMAP_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findNextTopic(ROADMAP_ID);
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
    }
  }

  @Nested
  @DisplayName("subject and topic retrieval")
  class SubjectAndTopicRetrievalTests {

    /**
     * Normal case: Lấy active roadmap theo subject thành công.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID, subject: Toán 10</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getActiveRoadmapBySubject() -> present branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Response trả về roadmap id đúng</li>
     * </ul>
     */
    @Test
    void it_should_return_active_roadmap_when_subject_match_exists() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, BigDecimal.ZERO);
      when(roadmapRepository.findTopByStudentIdAndSubjectAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, "Toán 10"))
          .thenReturn(Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(Collections.emptyList());
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

      // ===== ACT =====
      RoadmapDetailResponse result = learningRoadmapService.getActiveRoadmapBySubject(STUDENT_ID, "Toán 10");

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertEquals(ROADMAP_ID, result.getId()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(1))
          .findTopByStudentIdAndSubjectAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, "Toán 10");
    }

    /**
     * Abnormal case: Ném exception khi không có roadmap active theo subject.
     *
     * <p>Input:
     * <ul>
     *   <li>subject: Toán 11 (không có roadmap)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getActiveRoadmapBySubject() -> empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_active_roadmap_by_subject_is_not_found() {
      // ===== ARRANGE =====
      when(roadmapRepository.findTopByStudentIdAndSubjectAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, "Toán 11"))
          .thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> learningRoadmapService.getActiveRoadmapBySubject(STUDENT_ID, "Toán 11"));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1))
          .findTopByStudentIdAndSubjectAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, "Toán 11");
    }

    /**
     * Abnormal case: Lấy topic details fail khi roadmap parent không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>topic tồn tại nhưng roadmap bị missing</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>topicRepository.findById -> present</li>
     *   <li>roadmapRepository.findById -> empty (throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_topic_exists_but_roadmap_is_missing() {
      // ===== ARRANGE =====
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.NOT_STARTED, QuestionDifficulty.MEDIUM, 1);
      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> learningRoadmapService.getTopicDetails(TOPIC_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findById(TOPIC_ID);
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
    }

    /**
     * Abnormal case: getTopicDetails ném exception khi topic không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>topicId: TOPIC_ID (không có trong repository)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>topicRepository.findById() -> empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_topic_details_topic_is_not_found() {
      // ===== ARRANGE =====
      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> learningRoadmapService.getTopicDetails(TOPIC_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(topicRepository, times(1)).findById(TOPIC_ID);
      verify(roadmapRepository, never()).findById(any());
    }

    /**
     * Normal case: Topic details vẫn map được khi student chưa enroll course.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID</li>
     *   <li>course tồn tại, totalLessons = 6, enrollment = empty</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>studentId != null nhưng enrollment == null</li>
     *   <li>isEnrolled giữ false, progress null</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>course response có isEnrolled=false và completedLessons=0</li>
     * </ul>
     */
    @Test
    void it_should_return_topic_details_with_not_enrolled_course_when_enrollment_is_missing() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("10.00"));
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.EASY, 1);
      TopicCourse topicCourse = buildTopicCourse(TOPIC_ID, COURSE_ID);
      Course course = buildCourse(COURSE_ID, "Toán rời rạc nhập môn");

      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(List.of(topicCourse));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(6L);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.empty());

      // ===== ACT =====
      RoadmapTopicResponse result = learningRoadmapService.getTopicDetails(TOPIC_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getCourses().size()),
          () -> assertEquals(false, result.getCourses().get(0).getIsEnrolled()),
          () -> assertEquals(0, result.getCourses().get(0).getCompletedLessons()),
          () -> assertNull(result.getCourses().get(0).getProgress()));
    }

    /**
     * Normal case: Topic details với enrollment có lessons = 0 thì progress = 0.0.
     *
     * <p>Input:
     * <ul>
     *   <li>student enrolled nhưng course totalLessons = 0</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>enrollment != null</li>
     *   <li>totalLessons == 0 -> progress = 0.0 branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>course progress trả về 0.0</li>
     * </ul>
     */
    @Test
    void it_should_set_zero_progress_when_enrolled_course_has_no_lessons() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("10.00"));
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.EASY, 1);
      TopicCourse topicCourse = buildTopicCourse(TOPIC_ID, COURSE_ID);
      Course course = buildCourse(COURSE_ID, "Hình học giải tích");
      Enrollment enrollment = buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID);

      when(topicRepository.findById(TOPIC_ID)).thenReturn(Optional.of(topic));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(topicCourseRepository.findByTopicId(TOPIC_ID)).thenReturn(List.of(topicCourse));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(0L);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(enrollment));
      when(lessonProgressRepository.countCompletedByEnrollmentId(ENROLLMENT_ID)).thenReturn(0L);

      // ===== ACT =====
      RoadmapTopicResponse result = learningRoadmapService.getTopicDetails(TOPIC_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(0.0, result.getCourses().get(0).getProgress()),
          () -> assertEquals(0.0, result.getProgress()));
    }
  }

  @Nested
  @DisplayName("getRoadmapStats()")
  class GetRoadmapStatsTests {

    /**
     * Normal case: Tính thống kê đủ độ khó, giờ ước tính và daysRemaining.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap progress: 50%</li>
     *   <li>topics: EASY/MEDIUM/HARD</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>estimatedHours null -> fallback 1 giờ</li>
     *   <li>calculateDaysRemaining với progress < 100</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về đúng số lượng topic theo độ khó và averageProgress</li>
     * </ul>
     */
    @Test
    void it_should_calculate_stats_when_roadmap_has_topics() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap =
          buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("50.00"));
      roadmap.setEstimatedCompletionDays(20);

      RoadmapTopic easy = buildTopic(UUID.randomUUID(), ROADMAP_ID, TopicStatus.NOT_STARTED, QuestionDifficulty.EASY, 1);
      easy.setEstimatedHours(2);
      easy.setProgressPercentage(new BigDecimal("10"));
      RoadmapTopic medium = buildTopic(UUID.randomUUID(), ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.MEDIUM, 2);
      medium.setEstimatedHours(null);
      medium.setProgressPercentage(new BigDecimal("50"));
      RoadmapTopic hard = buildTopic(UUID.randomUUID(), ROADMAP_ID, TopicStatus.COMPLETED, QuestionDifficulty.HARD, 3);
      hard.setEstimatedHours(3);
      hard.setProgressPercentage(new BigDecimal("100"));

      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID))
          .thenReturn(List.of(easy, medium, hard));

      // ===== ACT =====
      RoadmapStatsResponse result = learningRoadmapService.getRoadmapStats(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(6, result.getTotalEstimatedHours()),
          () -> assertEquals(1L, result.getEasyTopicsCount()),
          () -> assertEquals(1L, result.getMediumTopicsCount()),
          () -> assertEquals(1L, result.getHardTopicsCount()),
          () -> assertEquals(new BigDecimal("53.33"), result.getAverageProgress()),
          () -> assertEquals(10, result.getDaysRemaining()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
      verify(topicRepository, times(1)).findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID);
    }

    /**
     * Normal case: Khi progress >= 100 thì daysRemaining bằng 0.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap progress: 100%</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>calculateDaysRemaining(): progress >= 100 -> return 0</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>daysRemaining = 0</li>
     * </ul>
     */
    @Test
    void it_should_return_zero_days_remaining_when_progress_is_full() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap =
          buildRoadmap(ROADMAP_ID, RoadmapStatus.COMPLETED, new BigDecimal("100.00"));
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.COMPLETED, QuestionDifficulty.EASY, 1);
      topic.setProgressPercentage(new BigDecimal("100"));

      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(List.of(topic));

      // ===== ACT =====
      RoadmapStatsResponse result = learningRoadmapService.getRoadmapStats(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertEquals(0, result.getDaysRemaining()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
      verify(topicRepository, times(1)).findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID);
    }

    /**
     * Normal case: Khi estimatedCompletionDays null thì dùng fallback 30 ngày.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap progress = 20%, estimatedCompletionDays = null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>calculateDaysRemaining: estimatedCompletionDays == null -> fallback 30</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>daysRemaining = 24 (80% * 30)</li>
     * </ul>
     */
    @Test
    void it_should_use_default_days_when_estimated_completion_days_is_null() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("20.00"));
      roadmap.setEstimatedCompletionDays(null);
      RoadmapTopic topic =
          buildTopic(TOPIC_ID, ROADMAP_ID, TopicStatus.IN_PROGRESS, QuestionDifficulty.MEDIUM, 1);
      topic.setProgressPercentage(new BigDecimal("20"));

      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));
      when(topicRepository.findByRoadmapIdOrderBySequenceOrder(ROADMAP_ID)).thenReturn(List.of(topic));

      // ===== ACT =====
      RoadmapStatsResponse result = learningRoadmapService.getRoadmapStats(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertEquals(24, result.getDaysRemaining()));
    }

    /**
     * Abnormal case: getRoadmapStats ném exception khi roadmap không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmapId: ROADMAP_ID (không có trong repository)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getRoadmapStats(): roadmap Optional empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_getting_stats_for_missing_roadmap() {
      // ===== ARRANGE =====
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> learningRoadmapService.getRoadmapStats(ROADMAP_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
      verify(topicRepository, never()).findByRoadmapIdOrderBySequenceOrder(any());
    }
  }

  @Nested
  @DisplayName("utility methods")
  class UtilityMethodTests {

    /**
     * Normal case: Trả về true/false theo repository cho existsActiveRoadmap.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID, subject: Toán 10</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>delegation branch của existsActiveRoadmap()</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả trả về trùng repository</li>
     * </ul>
     */
    @Test
    void it_should_delegate_exists_active_roadmap_when_repository_returns_value() {
      // ===== ARRANGE =====
      when(roadmapRepository.existsActiveRoadmapForStudentAndSubject(STUDENT_ID, "Toán 10"))
          .thenReturn(true);

      // ===== ACT =====
      boolean result = learningRoadmapService.existsActiveRoadmap(STUDENT_ID, "Toán 10");

      // ===== ASSERT =====
      assertTrue(result);

      // ===== VERIFY =====
      verify(roadmapRepository, times(1))
          .existsActiveRoadmapForStudentAndSubject(STUDENT_ID, "Toán 10");
      verifyNoMoreInteractions(roadmapRepository);
    }

    /**
     * Normal case: Archive roadmap sẽ set deletedAt và ARCHIVED.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmapId: ROADMAP_ID (tồn tại)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findById -> present (update archive fields)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>roadmap.status = ARCHIVED và repository.save được gọi</li>
     * </ul>
     */
    @Test
    void it_should_archive_roadmap_when_roadmap_exists() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap =
          buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("20.00"));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

      // ===== ACT =====
      learningRoadmapService.archiveRoadmap(ROADMAP_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(RoadmapStatus.ARCHIVED, roadmap.getStatus()),
          () -> assertNotNull(roadmap.getDeletedAt()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
      verify(roadmapRepository, times(1)).save(roadmap);
    }

    /**
     * Abnormal case: estimateCompletionDays ném exception khi roadmap không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmapId: ROADMAP_ID (không có)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findById -> empty (throw ASSESSMENT_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_estimating_completion_days_for_missing_roadmap() {
      // ===== ARRANGE =====
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> learningRoadmapService.estimateCompletionDays(ROADMAP_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
    }

    /**
     * Normal case: estimateCompletionDays trả về giá trị estimatedCompletionDays.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap.estimatedCompletionDays = 18</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>estimateCompletionDays() present branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Giá trị trả về bằng 18</li>
     * </ul>
     */
    @Test
    void it_should_return_estimated_days_when_roadmap_exists() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("25.00"));
      roadmap.setEstimatedCompletionDays(18);
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

      // ===== ACT =====
      Integer result = learningRoadmapService.estimateCompletionDays(ROADMAP_ID);

      // ===== ASSERT =====
      assertEquals(18, result);

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
    }

    /**
     * Normal case: calculateRoadmapProgress trả về cached progress của roadmap.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmap.progressPercentage = 72.50</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>calculateRoadmapProgress() present branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Giá trị trả về bằng 72.50</li>
     * </ul>
     */
    @Test
    void it_should_return_progress_percentage_when_calculating_roadmap_progress() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap = buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("72.50"));
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.of(roadmap));

      // ===== ACT =====
      BigDecimal result = learningRoadmapService.calculateRoadmapProgress(ROADMAP_ID);

      // ===== ASSERT =====
      assertEquals(new BigDecimal("72.50"), result);

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
    }

    /**
     * Abnormal case: calculateRoadmapProgress ném exception khi roadmap không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmapId: ROADMAP_ID (không có trong repository)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>calculateRoadmapProgress() -> Optional empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_calculating_progress_for_missing_roadmap() {
      // ===== ARRANGE =====
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> learningRoadmapService.calculateRoadmapProgress(ROADMAP_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
    }

    /**
     * Abnormal case: archiveRoadmap ném exception khi roadmap không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>roadmapId: ROADMAP_ID (không có)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>archiveRoadmap -> findById empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_archiving_missing_roadmap() {
      // ===== ARRANGE =====
      when(roadmapRepository.findById(ROADMAP_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> learningRoadmapService.archiveRoadmap(ROADMAP_ID));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findById(ROADMAP_ID);
      verify(roadmapRepository, never()).save(any(LearningRoadmap.class));
    }
  }

  @Nested
  @DisplayName("list and page retrieval")
  class ListRetrievalTests {

    /**
     * Normal case: Trả về page roadmap summary theo student.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID</li>
     *   <li>pageable: trang đầu, size 10</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>mapToSummaryResponse branch cho dữ liệu page</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Page trả về có đúng số phần tử và roadmap id</li>
     * </ul>
     */
    @Test
    void it_should_return_page_summary_when_student_has_roadmaps() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap =
          buildRoadmap(ROADMAP_ID, RoadmapStatus.IN_PROGRESS, new BigDecimal("66.66"));
      Pageable pageable = PageRequest.of(0, 10);
      Page<LearningRoadmap> page = new PageImpl<>(List.of(roadmap));

      when(roadmapRepository.findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, pageable))
          .thenReturn(page);

      // ===== ACT =====
      Page<RoadmapSummaryResponse> result = learningRoadmapService.getStudentRoadmaps(STUDENT_ID, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getTotalElements()),
          () -> assertEquals(ROADMAP_ID, result.getContent().get(0).getId()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(1))
          .findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(STUDENT_ID, pageable);
    }

    /**
     * Normal case: Trả về list roadmap summary cho student.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>mapToSummaryResponse branch cho list dữ liệu</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>List trả về đúng kích thước và subject</li>
     * </ul>
     */
    @Test
    void it_should_return_summary_list_when_student_has_roadmaps() {
      // ===== ARRANGE =====
      LearningRoadmap roadmap =
          buildRoadmap(ROADMAP_ID, RoadmapStatus.GENERATED, new BigDecimal("0.00"));
      when(roadmapRepository.findByStudentIdAndDeletedAtIsNull(STUDENT_ID)).thenReturn(List.of(roadmap));

      // ===== ACT =====
      List<RoadmapSummaryResponse> result = learningRoadmapService.getStudentRoadmapsList(STUDENT_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.size()),
          () -> assertEquals("Toán 10", result.get(0).getSubject()));

      // ===== VERIFY =====
      verify(roadmapRepository, times(1)).findByStudentIdAndDeletedAtIsNull(STUDENT_ID);
    }
  }

  private LearningRoadmap buildRoadmap(UUID roadmapId, RoadmapStatus status, BigDecimal progress) {
    LearningRoadmap roadmap = new LearningRoadmap();
    roadmap.setId(roadmapId);
    roadmap.setName("Lộ trình Toán học học kỳ 1");
    roadmap.setStudentId(STUDENT_ID);
    roadmap.setTeacherId(TEACHER_ID);
    roadmap.setSubjectId(SUBJECT_ID);
    roadmap.setSubject("Toán 10");
    roadmap.setGradeLevel("Lớp 10");
    roadmap.setGenerationType(RoadmapGenerationType.ADMIN_TEMPLATE);
    roadmap.setStatus(status);
    roadmap.setProgressPercentage(progress);
    roadmap.setCompletedTopicsCount(0);
    roadmap.setTotalTopicsCount(0);
    roadmap.setEstimatedCompletionDays(20);
    roadmap.setDescription("Lộ trình tập trung vào hàm số bậc nhất và phương trình.");
    roadmap.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
    roadmap.setUpdatedAt(Instant.parse("2026-01-02T00:00:00Z"));
    return roadmap;
  }

  private RoadmapTopic buildTopic(
      UUID topicId,
      UUID roadmapId,
      TopicStatus status,
      QuestionDifficulty difficulty,
      int sequenceOrder) {
    RoadmapTopic topic = new RoadmapTopic();
    topic.setId(topicId);
    topic.setRoadmapId(roadmapId);
    topic.setTitle("Hàm số và đồ thị");
    topic.setDescription("Ôn lại lý thuyết và bài tập vận dụng.");
    topic.setStatus(status);
    topic.setDifficulty(difficulty);
    topic.setSequenceOrder(sequenceOrder);
    topic.setProgressPercentage(new BigDecimal("0"));
    topic.setEstimatedHours(2);
    topic.setMark(7.5);
    return topic;
  }

  private TopicCourse buildTopicCourse(UUID topicId, UUID courseId) {
    TopicCourse topicCourse = new TopicCourse();
    topicCourse.setId(UUID.randomUUID());
    topicCourse.setTopicId(topicId);
    topicCourse.setCourseId(courseId);
    return topicCourse;
  }

  private Course buildCourse(UUID courseId, String title) {
    Course course = new Course();
    course.setId(courseId);
    course.setTitle(title);
    course.setThumbnailUrl("https://cdn.math-master.edu.vn/course/toan10.png");
    return course;
  }

  private Enrollment buildEnrollment(UUID enrollmentId, UUID studentId, UUID courseId) {
    Enrollment enrollment = new Enrollment();
    enrollment.setId(enrollmentId);
    enrollment.setStudentId(studentId);
    enrollment.setCourseId(courseId);
    return enrollment;
  }

  @SuppressWarnings("unused")
  private Assessment buildAssessment(UUID assessmentId) {
    Assessment assessment = new Assessment();
    assessment.setId(assessmentId);
    assessment.setTitle("Kiểm tra đầu vào Toán 10");
    assessment.setDescription("Đánh giá nền tảng đại số.");
    return assessment;
  }
}
