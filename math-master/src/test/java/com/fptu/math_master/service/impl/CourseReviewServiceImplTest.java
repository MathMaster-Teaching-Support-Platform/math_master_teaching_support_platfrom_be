package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CourseReviewRequest;
import com.fptu.math_master.dto.request.InstructorReplyRequest;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseReview;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.CourseReviewRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("CourseReviewServiceImpl - Tests")
class CourseReviewServiceImplTest extends BaseUnitTest {

  @InjectMocks private CourseReviewServiceImpl courseReviewService;

  @Mock private CourseReviewRepository reviewRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private UserRepository userRepository;

  private static final UUID COURSE_ID = UUID.fromString("90000000-0000-0000-0000-000000000001");
  private static final UUID STUDENT_ID = UUID.fromString("90000000-0000-0000-0000-000000000002");
  private static final UUID TEACHER_ID = UUID.fromString("90000000-0000-0000-0000-000000000003");
  private static final UUID REVIEW_ID = UUID.fromString("90000000-0000-0000-0000-000000000004");

  private Course buildCourse(UUID id, UUID teacherId) {
    Course course = new Course();
    course.setId(id);
    course.setTeacherId(teacherId);
    course.setTitle("Course title");
    course.setRating(BigDecimal.ZERO);
    return course;
  }

  private CourseReview buildReview(UUID id, UUID courseId, UUID studentId, int rating, String comment) {
    CourseReview review = new CourseReview();
    review.setId(id);
    review.setCourseId(courseId);
    review.setStudentId(studentId);
    review.setRating(rating);
    review.setComment(comment);
    review.setCreatedAt(Instant.parse("2026-04-26T05:10:00Z"));
    review.setUpdatedAt(Instant.parse("2026-04-26T05:10:00Z"));
    return review;
  }

  @Nested
  @DisplayName("submitReview()")
  class SubmitReviewTests {

    @Test
    void it_should_submit_review_and_update_course_rating_when_input_valid() {
      // ===== ARRANGE =====
      CourseReviewRequest request = CourseReviewRequest.builder().rating(5).comment("Great").build();
      Course course = buildCourse(COURSE_ID, TEACHER_ID);
      CourseReview saved = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 5, "Great");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(new Enrollment()));
      when(reviewRepository.findByCourseIdAndStudentIdAndDeletedAtIsNull(COURSE_ID, STUDENT_ID))
          .thenReturn(Optional.empty());
      when(reviewRepository.save(any(CourseReview.class))).thenReturn(saved);
      when(reviewRepository.calculateAverageRating(COURSE_ID)).thenReturn(4.6666D);
      when(courseRepository.save(course)).thenReturn(course);

      // ===== ACT =====
      var response = courseReviewService.submitReview(COURSE_ID, request, STUDENT_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(REVIEW_ID, response.getId()),
          () -> assertEquals("Great", response.getComment()),
          () -> assertEquals("Anonymous", response.getStudentName()),
          () -> assertNull(response.getStudentAvatar()),
          () -> assertEquals(new BigDecimal("4.67"), course.getRating()));
    }

    @Test
    void it_should_throw_course_not_found_when_submit_with_invalid_course_id() {
      // ===== ARRANGE =====
      CourseReviewRequest request = CourseReviewRequest.builder().rating(4).comment("Good").build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> courseReviewService.submitReview(COURSE_ID, request, STUDENT_ID));
      assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_throw_not_enrolled_when_submit_review_without_enrollment() {
      // ===== ARRANGE =====
      CourseReviewRequest request = CourseReviewRequest.builder().rating(4).comment("Good").build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(COURSE_ID, TEACHER_ID)));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> courseReviewService.submitReview(COURSE_ID, request, STUDENT_ID));
      assertEquals(ErrorCode.COURSE_NOT_ENROLLED, exception.getErrorCode());
    }

    @Test
    void it_should_throw_already_reviewed_when_student_has_existing_review() {
      // ===== ARRANGE =====
      CourseReviewRequest request = CourseReviewRequest.builder().rating(4).comment("Good").build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(COURSE_ID, TEACHER_ID)));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(new Enrollment()));
      when(reviewRepository.findByCourseIdAndStudentIdAndDeletedAtIsNull(COURSE_ID, STUDENT_ID))
          .thenReturn(Optional.of(buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 4, "Old review")));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> courseReviewService.submitReview(COURSE_ID, request, STUDENT_ID));
      assertEquals(ErrorCode.ALREADY_REVIEWED, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("updateReview()")
  class UpdateReviewTests {

    @Test
    void it_should_update_review_and_course_rating_for_review_owner() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 3, "ok");
      Course course = buildCourse(COURSE_ID, TEACHER_ID);
      when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
      when(reviewRepository.save(review)).thenReturn(review);
      when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
      when(reviewRepository.calculateAverageRating(COURSE_ID)).thenReturn(4.2D);
      when(courseRepository.save(course)).thenReturn(course);

      // ===== ACT =====
      var response =
          courseReviewService.updateReview(
              REVIEW_ID, CourseReviewRequest.builder().rating(5).comment("Updated").build(), STUDENT_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(5, response.getRating()),
          () -> assertEquals("Updated", response.getComment()),
          () -> assertEquals(new BigDecimal("4.20"), course.getRating()));
    }

    @Test
    void it_should_throw_review_not_found_when_update_unknown_review() {
      // ===== ARRANGE =====
      CourseReviewRequest request = CourseReviewRequest.builder().rating(2).comment("bad").build();
      when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> courseReviewService.updateReview(REVIEW_ID, request, STUDENT_ID));
      assertEquals(ErrorCode.REVIEW_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_throw_unauthorized_when_update_review_by_other_student() {
      // ===== ARRANGE =====
      CourseReviewRequest request = CourseReviewRequest.builder().rating(2).comment("bad").build();
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, UUID.fromString("90000000-0000-0000-0000-000000000099"), 3, "ok");
      when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> courseReviewService.updateReview(REVIEW_ID, request, STUDENT_ID));
      assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("getCourseReviews() and getReviewSummary()")
  class QueryTests {

    @Test
    void it_should_get_course_reviews_filtered_by_rating_when_rating_not_null() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 5, "great");
      User student = new User();
      student.setFullName("Nguyen Van A");
      student.setAvatar("avatar-url");
      review.setStudent(student);
      when(reviewRepository.findByCourseIdAndRatingAndDeletedAtIsNull(COURSE_ID, 5, PageRequest.of(0, 10)))
          .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));

      // ===== ACT =====
      var page = courseReviewService.getCourseReviews(COURSE_ID, 5, PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, page.getTotalElements()),
          () -> assertEquals("Nguyen Van A", page.getContent().get(0).getStudentName()),
          () -> assertEquals("avatar-url", page.getContent().get(0).getStudentAvatar()));
    }

    @Test
    void it_should_get_course_reviews_without_rating_filter_when_rating_is_null() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 4, "good");
      when(reviewRepository.findByCourseIdAndDeletedAtIsNull(COURSE_ID, PageRequest.of(0, 10)))
          .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1));

      // ===== ACT =====
      var page = courseReviewService.getCourseReviews(COURSE_ID, null, PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertEquals(1, page.getTotalElements());
      verify(reviewRepository, times(1)).findByCourseIdAndDeletedAtIsNull(COURSE_ID, PageRequest.of(0, 10));
      verify(reviewRepository, never())
          .findByCourseIdAndRatingAndDeletedAtIsNull(any(), any(), any());
    }

    @Test
    void it_should_return_review_summary_with_default_zero_distribution_and_overrides() {
      // ===== ARRANGE =====
      when(reviewRepository.countByCourseIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(3L);
      when(reviewRepository.calculateAverageRating(COURSE_ID)).thenReturn(3.335D);
      when(reviewRepository.getRatingDistribution(COURSE_ID))
          .thenReturn(List.of(new Object[] {5, 2L}, new Object[] {1, 1L}));

      // ===== ACT =====
      var summary = courseReviewService.getReviewSummary(COURSE_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(3L, summary.getTotalReviews()),
          () -> assertEquals(new BigDecimal("3.34"), summary.getAverageRating()),
          () -> assertEquals(5, summary.getRatingDistribution().size()),
          () -> assertEquals(2L, summary.getRatingDistribution().get(5)),
          () -> assertEquals(1L, summary.getRatingDistribution().get(1)),
          () -> assertEquals(0L, summary.getRatingDistribution().get(3)));
    }

    @Test
    void it_should_return_zero_average_when_summary_average_is_null() {
      // ===== ARRANGE =====
      when(reviewRepository.countByCourseIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(0L);
      when(reviewRepository.calculateAverageRating(COURSE_ID)).thenReturn(null);
      when(reviewRepository.getRatingDistribution(COURSE_ID)).thenReturn(List.of());

      // ===== ACT =====
      var summary = courseReviewService.getReviewSummary(COURSE_ID);

      // ===== ASSERT =====
      assertEquals(BigDecimal.ZERO, summary.getAverageRating());
    }
  }

  @Nested
  @DisplayName("replyToReview(), deleteReview(), getMyReview()")
  class ReplyDeleteAndMyReviewTests {

    @Test
    void it_should_reply_to_review_when_teacher_owns_course() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 4, "nice");
      Course course = buildCourse(COURSE_ID, TEACHER_ID);
      InstructorReplyRequest request = new InstructorReplyRequest();
      request.setReply("Cam on em");
      when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
      when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
      when(reviewRepository.save(review)).thenReturn(review);

      // ===== ACT =====
      var response = courseReviewService.replyToReview(REVIEW_ID, request, TEACHER_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Cam on em", response.getInstructorReply()),
          () -> assertNotNull(response.getRepliedAt()));
    }

    @Test
    void it_should_throw_unauthorized_when_other_teacher_replies_review() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 4, "nice");
      Course course = buildCourse(COURSE_ID, UUID.fromString("90000000-0000-0000-0000-000000000055"));
      InstructorReplyRequest request = new InstructorReplyRequest();
      request.setReply("Cam on");
      when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
      when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> courseReviewService.replyToReview(REVIEW_ID, request, TEACHER_ID));
      assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void it_should_delete_review_and_update_course_rating_when_course_still_exists() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 4, "nice");
      Course course = buildCourse(COURSE_ID, TEACHER_ID);
      when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
      when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
      when(reviewRepository.calculateAverageRating(COURSE_ID)).thenReturn(2.0D);
      when(courseRepository.save(course)).thenReturn(course);

      // ===== ACT =====
      courseReviewService.deleteReview(REVIEW_ID, STUDENT_ID);

      // ===== ASSERT =====
      verify(reviewRepository, times(1)).delete(review);
      assertEquals(new BigDecimal("2.00"), course.getRating());
    }

    @Test
    void it_should_delete_review_without_updating_rating_when_course_not_found_after_delete() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 4, "nice");
      when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
      when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

      // ===== ACT =====
      courseReviewService.deleteReview(REVIEW_ID, STUDENT_ID);

      // ===== ASSERT =====
      verify(reviewRepository, times(1)).delete(review);
      verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void it_should_return_my_review_when_present_and_null_when_absent() {
      // ===== ARRANGE =====
      CourseReview review = buildReview(REVIEW_ID, COURSE_ID, STUDENT_ID, 5, "excellent");
      when(reviewRepository.findByCourseIdAndStudentIdAndDeletedAtIsNull(COURSE_ID, STUDENT_ID))
          .thenReturn(Optional.of(review))
          .thenReturn(Optional.empty());

      // ===== ACT =====
      var found = courseReviewService.getMyReview(COURSE_ID, STUDENT_ID);
      var missing = courseReviewService.getMyReview(COURSE_ID, STUDENT_ID);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(found), () -> assertNull(missing));
    }
  }
}
