package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.TopicCourse;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ProgressRecalculationServiceImpl - Tests")
class ProgressRecalculationServiceImplTest extends BaseUnitTest {

  @InjectMocks private ProgressRecalculationServiceImpl service;

  @Mock private LearningRoadmapRepository roadmapRepository;
  @Mock private RoadmapTopicRepository topicRepository;
  @Mock private TopicCourseRepository topicCourseRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private LessonProgressRepository lessonProgressRepository;
  @Mock private AssessmentRepository assessmentRepository;

  private UUID roadmapId;
  private UUID studentId;
  private LearningRoadmap roadmap;

  @BeforeEach
  void setUp() {
    roadmapId = UUID.randomUUID();
    studentId = UUID.randomUUID();
    roadmap = new LearningRoadmap();
    roadmap.setId(roadmapId);
    roadmap.setName("Algebra Progress Roadmap");
    roadmap.setStudentId(studentId);
    roadmap.setProgressPercentage(BigDecimal.ZERO);
    roadmap.setCompletedTopicsCount(0);
    roadmap.setTotalTopicsCount(0);
  }

  /** Normal case: Calculates roadmap progress and completed topics. */
  @Test
  void it_should_recalculate_progress_when_roadmap_has_topics_courses_and_enrollments() {
    // ===== ARRANGE =====
    RoadmapTopic topic = new RoadmapTopic();
    topic.setId(UUID.randomUUID());
    topic.setTitle("Linear Functions");
    TopicCourse topicCourse = new TopicCourse();
    topicCourse.setCourseId(UUID.randomUUID());
    Course course = new Course();
    course.setId(topicCourse.getCourseId());
    Enrollment enrollment = new Enrollment();
    enrollment.setId(UUID.randomUUID());

    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
    when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));
    when(topicCourseRepository.findByTopicId(topic.getId())).thenReturn(List.of(topicCourse));
    when(courseRepository.findByIdAndDeletedAtIsNull(topicCourse.getCourseId())).thenReturn(Optional.of(course));
    when(courseLessonRepository.countByCourseIdAndNotDeleted(course.getId())).thenReturn(4L);
    when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, course.getId()))
        .thenReturn(Optional.of(enrollment));
    when(lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId())).thenReturn(4L);

    // ===== ACT =====
    service.recalculateRoadmapProgress(roadmapId);

    // ===== ASSERT =====
    assertTrue(roadmap.getProgressPercentage().compareTo(new BigDecimal("100.00")) == 0);
    assertTrue(roadmap.getCompletedTopicsCount() == 1);
    assertTrue(roadmap.getTotalTopicsCount() == 1);

    // ===== VERIFY =====
    verify(roadmapRepository, times(1)).save(roadmap);
  }

  /** Abnormal case: Returns early when roadmap is null or deleted. */
  @Test
  void it_should_return_without_saving_when_roadmap_not_found() {
    // ===== ARRANGE =====
    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.empty());

    // ===== ACT =====
    service.recalculateRoadmapProgress(roadmapId);

    // ===== ASSERT & VERIFY =====
    verify(roadmapRepository, never()).save(any(LearningRoadmap.class));
    verify(topicRepository, never()).findByRoadmapIdOrderBySequenceOrder(any());
  }

  /** Abnormal case: Returns early when roadmap does not have student id. */
  @Test
  void it_should_return_without_saving_when_roadmap_has_null_student_id() {
    // ===== ARRANGE =====
    roadmap.setStudentId(null);
    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

    // ===== ACT =====
    service.recalculateRoadmapProgress(roadmapId);

    // ===== ASSERT & VERIFY =====
    verify(roadmapRepository, never()).save(any(LearningRoadmap.class));
    verify(topicRepository, never()).findByRoadmapIdOrderBySequenceOrder(any());
  }

  /** Normal case: Recalculates all roadmaps and continues when one roadmap fails. */
  @Test
  void it_should_continue_recalculating_all_roadmaps_when_one_recalculation_fails() {
    // ===== ARRANGE =====
    LearningRoadmap active1 = new LearningRoadmap();
    active1.setId(UUID.randomUUID());
    active1.setStudentId(UUID.randomUUID());
    LearningRoadmap active2 = new LearningRoadmap();
    active2.setId(UUID.randomUUID());
    active2.setStudentId(UUID.randomUUID());
    LearningRoadmap deleted = new LearningRoadmap();
    deleted.setId(UUID.randomUUID());
    deleted.setDeletedAt(Instant.now());
    when(roadmapRepository.findAll()).thenReturn(List.of(active1, active2, deleted));

    ProgressRecalculationServiceImpl spyService = spy(service);
    org.mockito.Mockito.doThrow(new RuntimeException("boom"))
        .when(spyService)
        .recalculateRoadmapProgress(active1.getId());

    // ===== ACT =====
    spyService.recalculateAllRoadmapProgress();

    // ===== ASSERT & VERIFY =====
    verify(spyService, times(1)).recalculateRoadmapProgress(active1.getId());
    verify(spyService, times(1)).recalculateRoadmapProgress(active2.getId());
    verify(spyService, never()).recalculateRoadmapProgress(deleted.getId());
  }

  /** Normal case: Clears invalid entry test reference and keeps valid one. */
  @Test
  void it_should_cleanup_only_invalid_entry_test_references() {
    // ===== ARRANGE =====
    UUID invalidAssessmentId = UUID.randomUUID();
    UUID validAssessmentId = UUID.randomUUID();
    LearningRoadmap roadmapWithInvalid = new LearningRoadmap();
    roadmapWithInvalid.setId(UUID.randomUUID());
    roadmapWithInvalid.setEntryTestId(invalidAssessmentId);
    LearningRoadmap roadmapWithValid = new LearningRoadmap();
    roadmapWithValid.setId(UUID.randomUUID());
    roadmapWithValid.setEntryTestId(validAssessmentId);

    when(roadmapRepository.findAll()).thenReturn(List.of(roadmapWithInvalid, roadmapWithValid));
    when(assessmentRepository.findByIdAndNotDeleted(invalidAssessmentId)).thenReturn(Optional.empty());
    when(assessmentRepository.findByIdAndNotDeleted(validAssessmentId))
        .thenReturn(Optional.of(new Assessment()));

    // ===== ACT =====
    service.cleanupInvalidEntryTests();

    // ===== ASSERT =====
    assertTrue(roadmapWithInvalid.getEntryTestId() == null);
    assertTrue(roadmapWithValid.getEntryTestId().equals(validAssessmentId));

    // ===== VERIFY =====
    verify(roadmapRepository, times(1)).save(roadmapWithInvalid);
    verify(roadmapRepository, never()).save(roadmapWithValid);
  }

  /** Normal case: Returns diagnosis with mismatch warning when calculated progress diverges. */
  @Test
  void it_should_return_warning_in_diagnosis_when_progress_mismatch_exceeds_threshold() {
    // ===== ARRANGE =====
    roadmap.setProgressPercentage(new BigDecimal("10.00"));
    RoadmapTopic topic = new RoadmapTopic();
    topic.setId(UUID.randomUUID());
    topic.setTitle("Quadratic Equations");
    TopicCourse tc = new TopicCourse();
    tc.setCourseId(UUID.randomUUID());
    Course course = new Course();
    course.setId(tc.getCourseId());
    Enrollment enrollment = new Enrollment();
    enrollment.setId(UUID.randomUUID());

    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
    when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));
    when(topicCourseRepository.findByTopicId(topic.getId())).thenReturn(List.of(tc));
    when(courseRepository.findByIdAndDeletedAtIsNull(tc.getCourseId())).thenReturn(Optional.of(course));
    when(courseLessonRepository.countByCourseIdAndNotDeleted(course.getId())).thenReturn(10L);
    when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, course.getId()))
        .thenReturn(Optional.of(enrollment));
    when(lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId())).thenReturn(10L);

    // ===== ACT =====
    String diagnosis = service.diagnoseProgressIssues(roadmapId);

    // ===== ASSERT =====
    assertTrue(diagnosis.contains("WARNING: Progress mismatch detected!"));
  }

  /** Abnormal case: Returns not-found message when diagnosis target roadmap does not exist. */
  @Test
  void it_should_return_not_found_message_when_diagnosing_missing_roadmap() {
    // ===== ARRANGE =====
    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.empty());

    // ===== ACT =====
    String diagnosis = service.diagnoseProgressIssues(roadmapId);

    // ===== ASSERT =====
    assertEquals("Roadmap not found", diagnosis);
  }

  /** Abnormal case: Returns template-roadmap message when student id is null in diagnosis. */
  @Test
  void it_should_return_template_message_when_diagnosing_roadmap_without_student() {
    // ===== ARRANGE =====
    roadmap.setStudentId(null);
    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

    // ===== ACT =====
    String diagnosis = service.diagnoseProgressIssues(roadmapId);

    // ===== ASSERT =====
    assertTrue(diagnosis.contains("ERROR: No student ID - this is a template roadmap"));
  }

  /** Normal case: Marks diagnosis as correct when calculated progress nearly matches stored value. */
  @Test
  void it_should_confirm_progress_when_diagnosis_difference_is_within_threshold() {
    // ===== ARRANGE =====
    roadmap.setProgressPercentage(new BigDecimal("50.00"));
    RoadmapTopic topic = new RoadmapTopic();
    topic.setId(UUID.randomUUID());
    topic.setTitle("Polynomials");
    TopicCourse tc = new TopicCourse();
    tc.setCourseId(UUID.randomUUID());
    Course course = new Course();
    course.setId(tc.getCourseId());
    Enrollment enrollment = new Enrollment();
    enrollment.setId(UUID.randomUUID());

    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
    when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));
    when(topicCourseRepository.findByTopicId(topic.getId())).thenReturn(List.of(tc));
    when(courseRepository.findByIdAndDeletedAtIsNull(tc.getCourseId())).thenReturn(Optional.of(course));
    when(courseLessonRepository.countByCourseIdAndNotDeleted(course.getId())).thenReturn(4L);
    when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, course.getId()))
        .thenReturn(Optional.of(enrollment));
    when(lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId())).thenReturn(2L);

    // ===== ACT =====
    String diagnosis = service.diagnoseProgressIssues(roadmapId);

    // ===== ASSERT =====
    assertTrue(diagnosis.contains("Progress calculation looks correct."));
  }

  /** Normal case: Recalculates deleted roadmap branch and exits without saving. */
  @Test
  void it_should_return_without_saving_when_roadmap_is_deleted() {
    // ===== ARRANGE =====
    roadmap.setDeletedAt(Instant.now());
    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));

    // ===== ACT =====
    service.recalculateRoadmapProgress(roadmapId);

    // ===== ASSERT & VERIFY =====
    verify(roadmapRepository, never()).save(any(LearningRoadmap.class));
  }

  /** Normal case: Handles missing course and missing enrollment branches during recalculation. */
  @Test
  void it_should_handle_missing_course_and_enrollment_when_recalculating_progress() {
    // ===== ARRANGE =====
    RoadmapTopic topic = new RoadmapTopic();
    topic.setId(UUID.randomUUID());
    topic.setTitle("Systems of Equations");

    TopicCourse missingCourseLink = new TopicCourse();
    missingCourseLink.setCourseId(UUID.randomUUID());
    TopicCourse validCourseLink = new TopicCourse();
    validCourseLink.setCourseId(UUID.randomUUID());
    Course validCourse = new Course();
    validCourse.setId(validCourseLink.getCourseId());

    when(roadmapRepository.findById(roadmapId)).thenReturn(Optional.of(roadmap));
    when(topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)).thenReturn(List.of(topic));
    when(topicCourseRepository.findByTopicId(topic.getId()))
        .thenReturn(List.of(missingCourseLink, validCourseLink));
    when(courseRepository.findByIdAndDeletedAtIsNull(missingCourseLink.getCourseId()))
        .thenReturn(Optional.empty());
    when(courseRepository.findByIdAndDeletedAtIsNull(validCourseLink.getCourseId()))
        .thenReturn(Optional.of(validCourse));
    when(courseLessonRepository.countByCourseIdAndNotDeleted(validCourse.getId())).thenReturn(5L);
    when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, validCourse.getId()))
        .thenReturn(Optional.empty());

    // ===== ACT =====
    service.recalculateRoadmapProgress(roadmapId);

    // ===== ASSERT =====
    assertEquals(new BigDecimal("0.00"), roadmap.getProgressPercentage());
    assertEquals(0, roadmap.getCompletedTopicsCount());

    // ===== VERIFY =====
    verify(roadmapRepository, times(1)).save(roadmap);
  }
}
