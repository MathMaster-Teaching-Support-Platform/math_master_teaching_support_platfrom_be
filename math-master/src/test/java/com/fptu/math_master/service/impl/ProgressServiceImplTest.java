package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.UpdateProgressRequest;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.LessonProgress;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@DisplayName("ProgressServiceImpl - Tests")
class ProgressServiceImplTest extends BaseUnitTest {

  @InjectMocks private ProgressServiceImpl progressService;

  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private LessonProgressRepository lessonProgressRepository;
  @Mock private CourseRepository courseRepository;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  private static final UUID STUDENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
  private static final UUID ENROLLMENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
  private static final UUID COURSE_ID = UUID.fromString("50000000-0000-0000-0000-000000000003");
  private static final UUID COURSE_LESSON_ID = UUID.fromString("50000000-0000-0000-0000-000000000004");

  @BeforeEach
  void setUp() {
    securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  private Enrollment buildEnrollment(UUID id, UUID studentId, UUID courseId, EnrollmentStatus status) {
    Enrollment enrollment = new Enrollment();
    enrollment.setId(id);
    enrollment.setStudentId(studentId);
    enrollment.setCourseId(courseId);
    enrollment.setStatus(status);
    return enrollment;
  }

  private LessonProgress buildProgress(
      UUID enrollmentId, UUID courseLessonId, boolean completed, int watchedSeconds) {
    LessonProgress lp = new LessonProgress();
    lp.setEnrollmentId(enrollmentId);
    lp.setCourseLessonId(courseLessonId);
    lp.setCompleted(completed);
    lp.setWatchedSeconds(watchedSeconds);
    lp.setCompletedAt(completed ? Instant.parse("2026-04-26T02:00:00Z") : null);
    return lp;
  }

  private CourseLesson buildCourseLesson(UUID id, UUID courseId, String title, Integer order) {
    CourseLesson cl = new CourseLesson();
    cl.setId(id);
    cl.setCourseId(courseId);
    cl.setVideoTitle(title);
    cl.setOrderIndex(order);
    return cl;
  }

  @Nested
  @DisplayName("markComplete()")
  class MarkCompleteTests {

    @Test
    void it_should_throw_enrollment_not_found_when_mark_complete_with_missing_enrollment() {
      // ===== ARRANGE =====
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> progressService.markComplete(ENROLLMENT_ID, COURSE_LESSON_ID));
      assertEquals(ErrorCode.ENROLLMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1)).findByIdAndDeletedAtIsNull(ENROLLMENT_ID);
      verifyNoMoreInteractions(enrollmentRepository, courseLessonRepository, lessonProgressRepository);
    }

    @Test
    void it_should_throw_access_denied_when_mark_complete_enrollment_belongs_to_other_student() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(
              ENROLLMENT_ID, UUID.fromString("50000000-0000-0000-0000-000000000099"), COURSE_ID, EnrollmentStatus.ACTIVE);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> progressService.markComplete(ENROLLMENT_ID, COURSE_LESSON_ID));
      assertEquals(ErrorCode.ENROLLMENT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_throw_not_found_when_mark_complete_with_non_active_enrollment() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.DROPPED);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> progressService.markComplete(ENROLLMENT_ID, COURSE_LESSON_ID));
      assertEquals(ErrorCode.ENROLLMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_throw_course_lesson_not_found_when_mark_complete_with_unlinked_lesson() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ACTIVE);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
      when(courseLessonRepository.existsByIdAndCourseIdAndDeletedAtIsNull(COURSE_LESSON_ID, COURSE_ID))
          .thenReturn(false);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> progressService.markComplete(ENROLLMENT_ID, COURSE_LESSON_ID));
      assertEquals(ErrorCode.COURSE_LESSON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_return_existing_progress_when_mark_complete_called_twice() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ACTIVE);
      LessonProgress existing = buildProgress(ENROLLMENT_ID, COURSE_LESSON_ID, true, 35);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID, "Ham so", 1);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
      when(courseLessonRepository.existsByIdAndCourseIdAndDeletedAtIsNull(COURSE_LESSON_ID, COURSE_ID))
          .thenReturn(true);
      when(lessonProgressRepository.findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
              ENROLLMENT_ID, COURSE_LESSON_ID))
          .thenReturn(Optional.of(existing));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(cl));

      // ===== ACT =====
      var item = progressService.markComplete(ENROLLMENT_ID, COURSE_LESSON_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(COURSE_LESSON_ID, item.getCourseLessonId()),
          () -> assertTrue(item.isCompleted()),
          () -> assertEquals("Ham so", item.getVideoTitle()));

      // ===== VERIFY =====
      verify(lessonProgressRepository, never()).save(any(LessonProgress.class));
    }

    @Test
    void it_should_create_new_progress_when_mark_complete_first_time() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ACTIVE);
      LessonProgress saved = buildProgress(ENROLLMENT_ID, COURSE_LESSON_ID, true, 0);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID, "Tich phan", 2);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
      when(courseLessonRepository.existsByIdAndCourseIdAndDeletedAtIsNull(COURSE_LESSON_ID, COURSE_ID))
          .thenReturn(true);
      when(lessonProgressRepository.findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
              ENROLLMENT_ID, COURSE_LESSON_ID))
          .thenReturn(Optional.empty());
      when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(saved);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(cl));

      // ===== ACT =====
      var item = progressService.markComplete(ENROLLMENT_ID, COURSE_LESSON_ID);

      // ===== ASSERT =====
      assertTrue(item.isCompleted());
      assertEquals("Tich phan", item.getVideoTitle());

      // ===== VERIFY =====
      verify(lessonProgressRepository, times(1)).save(any(LessonProgress.class));
    }
  }

  @Nested
  @DisplayName("updateProgress()")
  class UpdateProgressTests {

    @Test
    void it_should_create_new_progress_and_cap_watched_seconds_at_max_value_seen() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ACTIVE);
      UpdateProgressRequest request =
          UpdateProgressRequest.builder().watchedSeconds(90).isCompleted(false).build();
      LessonProgress saved = buildProgress(ENROLLMENT_ID, COURSE_LESSON_ID, false, 90);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID, "He phuong trinh", 3);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
      when(courseLessonRepository.existsByIdAndCourseIdAndDeletedAtIsNull(COURSE_LESSON_ID, COURSE_ID))
          .thenReturn(true);
      when(lessonProgressRepository.findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
              ENROLLMENT_ID, COURSE_LESSON_ID))
          .thenReturn(Optional.empty());
      when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(saved);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(cl));

      // ===== ACT =====
      var item = progressService.updateProgress(ENROLLMENT_ID, COURSE_LESSON_ID, request);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(90, item.getWatchedSeconds()), () -> assertEquals("He phuong trinh", item.getVideoTitle()));
    }

    @Test
    void it_should_keep_higher_watched_seconds_and_complete_when_request_marks_completed() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ACTIVE);
      LessonProgress existing = buildProgress(ENROLLMENT_ID, COURSE_LESSON_ID, false, 120);
      UpdateProgressRequest request =
          UpdateProgressRequest.builder().watchedSeconds(50).isCompleted(true).build();
      LessonProgress saved = buildProgress(ENROLLMENT_ID, COURSE_LESSON_ID, true, 120);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID, "Luong giac", 4);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
      when(courseLessonRepository.existsByIdAndCourseIdAndDeletedAtIsNull(COURSE_LESSON_ID, COURSE_ID))
          .thenReturn(true);
      when(lessonProgressRepository.findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
              ENROLLMENT_ID, COURSE_LESSON_ID))
          .thenReturn(Optional.of(existing));
      when(lessonProgressRepository.save(any(LessonProgress.class))).thenReturn(saved);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(cl));

      // ===== ACT =====
      var item = progressService.updateProgress(ENROLLMENT_ID, COURSE_LESSON_ID, request);

      // ===== ASSERT =====
      assertAll(() -> assertTrue(item.isCompleted()), () -> assertEquals(120, item.getWatchedSeconds()));
    }
  }

  @Nested
  @DisplayName("getProgress()")
  class GetProgressTests {

    @Test
    void it_should_throw_not_found_when_get_progress_enrollment_is_missing() {
      // ===== ARRANGE =====
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> progressService.getProgress(ENROLLMENT_ID));
      assertEquals(ErrorCode.ENROLLMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_throw_access_denied_when_get_progress_enrollment_owner_is_different() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(
              ENROLLMENT_ID, UUID.fromString("50000000-0000-0000-0000-000000000066"), COURSE_ID, EnrollmentStatus.ACTIVE);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> progressService.getProgress(ENROLLMENT_ID));
      assertEquals(ErrorCode.ENROLLMENT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_return_zero_completion_rate_when_course_has_no_lessons() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ACTIVE);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(0L);
      when(lessonProgressRepository.findByEnrollmentIdOrderByCourseLessonOrderIndex(ENROLLMENT_ID))
          .thenReturn(List.of());
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of());

      // ===== ACT =====
      var response = progressService.getProgress(ENROLLMENT_ID);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(0, response.getTotalLessons()), () -> assertEquals(0.0, response.getCompletionRate()));
    }

    @Test
    void it_should_return_progress_details_with_lesson_items_and_cap_completion_rate_to_100() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ACTIVE);
      LessonProgress lp1 = buildProgress(ENROLLMENT_ID, COURSE_LESSON_ID, true, 200);
      LessonProgress lp2 =
          buildProgress(
              ENROLLMENT_ID,
              UUID.fromString("50000000-0000-0000-0000-000000000077"),
              true,
              150);
      Course course = new Course();
      course.setId(COURSE_ID);
      course.setTitle("Khoa hoc toan nang cao");
      CourseLesson cl1 = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID, "Bai 1", 1);
      CourseLesson cl2 =
          buildCourseLesson(
              UUID.fromString("50000000-0000-0000-0000-000000000078"), COURSE_ID, "Bai 2", 2);

      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.of(enrollment));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(1L);
      when(lessonProgressRepository.findByEnrollmentIdOrderByCourseLessonOrderIndex(ENROLLMENT_ID))
          .thenReturn(List.of(lp1, lp2));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl1, cl2));
      when(lessonProgressRepository.findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
              ENROLLMENT_ID, COURSE_LESSON_ID))
          .thenReturn(Optional.of(lp1));
      when(lessonProgressRepository.findByEnrollmentIdAndCourseLessonIdAndDeletedAtIsNull(
              ENROLLMENT_ID, cl2.getId()))
          .thenReturn(Optional.empty());

      // ===== ACT =====
      var response = progressService.getProgress(ENROLLMENT_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Khoa hoc toan nang cao", response.getCourseTitle()),
          () -> assertEquals(1, response.getTotalLessons()),
          () -> assertEquals(2, response.getCompletedLessons()),
          () -> assertEquals(100.0, response.getCompletionRate()),
          () -> assertEquals(2, response.getLessons().size()),
          () -> assertTrue(response.getLessons().get(0).isCompleted()),
          () -> assertEquals(0, response.getLessons().get(1).getWatchedSeconds()));
    }
  }
}
