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
import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.response.EnrollmentResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.enums.CourseStatus;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.repository.WalletRepository;
import com.fptu.math_master.service.WalletService;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
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

@DisplayName("EnrollmentServiceImpl - Tests")
class EnrollmentServiceImplTest extends BaseUnitTest {

  @InjectMocks private EnrollmentServiceImpl enrollmentService;

  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private UserRepository userRepository;
  @Mock private WalletService walletService;
  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private LessonProgressRepository lessonProgressRepository;
  @Mock private StreamPublisher streamPublisher;

  private static final UUID STUDENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID TEACHER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID COURSE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID ENROLLMENT_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
  private static final UUID STUDENT_WALLET_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
  private static final UUID TEACHER_WALLET_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

  private MockedStatic<SecurityUtils> securityUtilsMock;
  private Course publishedCourse;
  private Enrollment baseEnrollment;

  @BeforeEach
  void setUp() {
    securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);

    publishedCourse = buildCourse(COURSE_ID, TEACHER_ID, true, CourseStatus.PUBLISHED);
    publishedCourse.setOriginalPrice(new BigDecimal("100000.00"));
    publishedCourse.setDiscountedPrice(new BigDecimal("50000.00"));
    publishedCourse.setDiscountExpiryDate(Instant.now().plusSeconds(3600));

    baseEnrollment = buildEnrollment(ENROLLMENT_ID, COURSE_ID, STUDENT_ID, EnrollmentStatus.ACTIVE);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  private Course buildCourse(UUID id, UUID teacherId, boolean isPublished, CourseStatus status) {
    Course course = new Course();
    course.setId(id);
    course.setTeacherId(teacherId);
    course.setTitle("Giai tich 1 - Dai hoc");
    course.setPublished(isPublished);
    course.setStatus(status);
    course.setOriginalPrice(new BigDecimal("200000.00"));
    course.setThumbnailUrl("https://cdn.mathmaster.edu.vn/course-thumb.png");
    return course;
  }

  private Enrollment buildEnrollment(UUID id, UUID courseId, UUID studentId, EnrollmentStatus status) {
    Enrollment enrollment = new Enrollment();
    enrollment.setId(id);
    enrollment.setCourseId(courseId);
    enrollment.setStudentId(studentId);
    enrollment.setStatus(status);
    enrollment.setEnrolledAt(Instant.parse("2026-04-26T01:00:00Z"));
    return enrollment;
  }

  private Wallet buildWallet(UUID id, UUID userId) {
    User user = new User();
    user.setId(userId);

    Wallet wallet = new Wallet();
    wallet.setId(id);
    wallet.setUser(user);
    wallet.setBalance(new BigDecimal("500000.00"));
    return wallet;
  }

  private User buildStudent(UUID id, String fullName) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    return user;
  }

  @Nested
  @DisplayName("enroll()")
  class EnrollTests {

    @Test
    void it_should_throw_exception_when_course_not_found() {
      // ===== ARRANGE =====
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> enrollmentService.enroll(COURSE_ID));
      assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(enrollmentRepository, never()).saveAndFlush(any(Enrollment.class));
      verifyNoMoreInteractions(courseRepository, enrollmentRepository);
    }

    @Test
    void it_should_throw_exception_when_course_is_not_published() {
      // ===== ARRANGE =====
      Course draftCourse = buildCourse(COURSE_ID, TEACHER_ID, false, CourseStatus.DRAFT);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(draftCourse));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> enrollmentService.enroll(COURSE_ID));
      assertEquals(ErrorCode.COURSE_NOT_PUBLISHED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(enrollmentRepository, never())
          .findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID);
      verifyNoMoreInteractions(courseRepository, enrollmentRepository);
    }

    @Test
    void it_should_return_existing_enrollment_when_already_active() {
      // ===== ARRANGE =====
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(baseEnrollment));
      when(userRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(buildStudent(STUDENT_ID, "Nguyen Minh Khoa")));

      // ===== ACT =====
      EnrollmentResponse response = enrollmentService.enroll(COURSE_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(ENROLLMENT_ID, response.getId()),
          () -> assertEquals(EnrollmentStatus.ACTIVE, response.getStatus()),
          () -> assertEquals("Giai tich 1 - Dai hoc", response.getCourseTitle()));

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID);
      verify(userRepository, times(1)).findById(STUDENT_ID);
      verify(enrollmentRepository, never()).saveAndFlush(any(Enrollment.class));
      verifyNoMoreInteractions(courseRepository, enrollmentRepository, userRepository);
    }

    @Test
    void it_should_throw_exception_when_enrollment_is_pending() {
      // ===== ARRANGE =====
      Enrollment pendingEnrollment =
          buildEnrollment(ENROLLMENT_ID, COURSE_ID, STUDENT_ID, EnrollmentStatus.PENDING);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(pendingEnrollment));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> enrollmentService.enroll(COURSE_ID));
      assertEquals(ErrorCode.ENROLLMENT_IN_PROGRESS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID);
      verify(enrollmentRepository, never()).saveAndFlush(any(Enrollment.class));
      verifyNoMoreInteractions(courseRepository, enrollmentRepository);
    }

    @Test
    void it_should_finalize_enrollment_and_process_payment_when_course_is_paid() {
      // ===== ARRANGE =====
      Enrollment droppedEnrollment =
          buildEnrollment(ENROLLMENT_ID, COURSE_ID, STUDENT_ID, EnrollmentStatus.DROPPED);
      Wallet studentWallet = buildWallet(STUDENT_WALLET_ID, STUDENT_ID);
      Wallet teacherWallet = buildWallet(TEACHER_WALLET_ID, TEACHER_ID);

      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(droppedEnrollment));
      when(enrollmentRepository.saveAndFlush(any(Enrollment.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(walletRepository.findByUserIdWithLock(STUDENT_ID)).thenReturn(Optional.of(studentWallet));
      when(walletRepository.findByUserIdWithLock(TEACHER_ID)).thenReturn(Optional.of(teacherWallet));
      when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(buildStudent(STUDENT_ID, "Tran Bao Chau")));

      // ===== ACT =====
      EnrollmentResponse response = enrollmentService.enroll(COURSE_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(EnrollmentStatus.ACTIVE, response.getStatus()),
          () -> assertEquals(COURSE_ID, response.getCourseId()));

      // ===== VERIFY =====
      verify(walletService, times(1)).deductBalance(STUDENT_WALLET_ID, new BigDecimal("50000.00"));
      verify(walletService, times(1)).addBalance(TEACHER_WALLET_ID, new BigDecimal("45000.00"));
      verify(transactionRepository, times(2)).save(any(Transaction.class));
      verify(enrollmentRepository, times(1)).saveAndFlush(any(Enrollment.class));
      verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
      verify(streamPublisher, times(2)).publish(any());
    }

    @Test
    void it_should_mark_enrollment_dropped_and_rethrow_when_wallet_creation_fails() {
      // ===== ARRANGE =====
      Enrollment newEnrollment =
          buildEnrollment(ENROLLMENT_ID, COURSE_ID, STUDENT_ID, EnrollmentStatus.PENDING);
      newEnrollment.setId(ENROLLMENT_ID);

      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.empty());
      when(enrollmentRepository.saveAndFlush(any(Enrollment.class))).thenReturn(newEnrollment);
      when(walletRepository.findByUserIdWithLock(STUDENT_ID))
          .thenReturn(Optional.empty())
          .thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> enrollmentService.enroll(COURSE_ID));
      assertEquals(ErrorCode.WALLET_CREATION_FAILED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(walletService, times(1)).createWallet(STUDENT_ID);
      verify(enrollmentRepository, times(1)).saveAndFlush(any(Enrollment.class));
      verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
      verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void it_should_skip_payment_and_activate_enrollment_when_course_is_free() {
      // ===== ARRANGE =====
      Course freeCourse = buildCourse(COURSE_ID, TEACHER_ID, true, CourseStatus.PUBLISHED);
      freeCourse.setOriginalPrice(BigDecimal.ZERO);
      freeCourse.setDiscountedPrice(null);
      Enrollment freshEnrollment =
          buildEnrollment(ENROLLMENT_ID, COURSE_ID, STUDENT_ID, EnrollmentStatus.PENDING);

      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(freeCourse));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNullWithLock(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.empty());
      when(enrollmentRepository.saveAndFlush(any(Enrollment.class))).thenReturn(freshEnrollment);
      when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(buildStudent(STUDENT_ID, "Pham Tuan Anh")));

      // ===== ACT =====
      EnrollmentResponse response = enrollmentService.enroll(COURSE_ID);

      // ===== ASSERT =====
      assertEquals(EnrollmentStatus.ACTIVE, response.getStatus());

      // ===== VERIFY =====
      verify(walletService, never()).deductBalance(any(UUID.class), any(BigDecimal.class));
      verify(walletService, never()).addBalance(any(UUID.class), any(BigDecimal.class));
      verify(transactionRepository, never()).save(any(Transaction.class));
      verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }
  }

  @Nested
  @DisplayName("drop()")
  class DropTests {

    @Test
    void it_should_throw_exception_when_enrollment_not_found() {
      // ===== ARRANGE =====
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> enrollmentService.drop(ENROLLMENT_ID));
      assertEquals(ErrorCode.ENROLLMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1)).findByIdAndDeletedAtIsNull(ENROLLMENT_ID);
      verify(enrollmentRepository, never()).save(any(Enrollment.class));
      verifyNoMoreInteractions(enrollmentRepository);
    }

    @Test
    void it_should_throw_exception_when_student_drops_other_user_enrollment() {
      // ===== ARRANGE =====
      Enrollment enrollment =
          buildEnrollment(ENROLLMENT_ID, COURSE_ID, UUID.fromString("99999999-9999-9999-9999-999999999999"), EnrollmentStatus.ACTIVE);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID))
          .thenReturn(Optional.of(enrollment));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> enrollmentService.drop(ENROLLMENT_ID));
      assertEquals(ErrorCode.ENROLLMENT_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1)).findByIdAndDeletedAtIsNull(ENROLLMENT_ID);
      verify(enrollmentRepository, never()).save(any(Enrollment.class));
      verifyNoMoreInteractions(enrollmentRepository);
    }

    @Test
    void it_should_drop_enrollment_and_return_response_when_student_is_owner() {
      // ===== ARRANGE =====
      Enrollment enrollment = buildEnrollment(ENROLLMENT_ID, COURSE_ID, STUDENT_ID, EnrollmentStatus.ACTIVE);
      when(enrollmentRepository.findByIdAndDeletedAtIsNull(ENROLLMENT_ID))
          .thenReturn(Optional.of(enrollment));
      when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(publishedCourse));
      when(userRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(buildStudent(STUDENT_ID, "Le Quang Minh")));

      // ===== ACT =====
      EnrollmentResponse response = enrollmentService.drop(ENROLLMENT_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(EnrollmentStatus.DROPPED, response.getStatus()),
          () -> assertEquals("Giai tich 1 - Dai hoc", response.getCourseTitle()));

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(userRepository, times(1)).findById(STUDENT_ID);
    }
  }

  @Nested
  @DisplayName("getMyEnrollments()")
  class GetMyEnrollmentsTests {

    @Test
    void it_should_return_empty_list_when_student_has_no_enrollments() {
      // ===== ARRANGE =====
      when(enrollmentRepository.findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(STUDENT_ID))
          .thenReturn(List.of());

      // ===== ACT =====
      List<EnrollmentResponse> result = enrollmentService.getMyEnrollments();

      // ===== ASSERT =====
      assertTrue(result.isEmpty());

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(STUDENT_ID);
      verify(courseRepository, never()).findAllById(any());
      verifyNoMoreInteractions(enrollmentRepository, courseRepository);
    }

    @Test
    void it_should_map_course_and_progress_with_capped_completion_rate_when_enrollments_exist() {
      // ===== ARRANGE =====
      Enrollment first = buildEnrollment(ENROLLMENT_ID, COURSE_ID, STUDENT_ID, EnrollmentStatus.ACTIVE);
      Enrollment second =
          buildEnrollment(
              UUID.fromString("12121212-1212-1212-1212-121212121212"),
              UUID.fromString("34343434-3434-3434-3434-343434343434"),
              STUDENT_ID,
              EnrollmentStatus.ACTIVE);

      Course firstCourse = buildCourse(COURSE_ID, TEACHER_ID, true, CourseStatus.PUBLISHED);
      firstCourse.setThumbnailUrl("https://cdn.mathmaster.edu.vn/thumb-1.png");

      when(enrollmentRepository.findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(STUDENT_ID))
          .thenReturn(List.of(first, second));
      when(courseRepository.findAllById(any())).thenReturn(List.of(firstCourse));
      when(userRepository.findById(STUDENT_ID))
          .thenReturn(Optional.of(buildStudent(STUDENT_ID, "Pham Ngoc Linh")));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(2L);
      when(lessonProgressRepository.countCompletedByEnrollmentId(ENROLLMENT_ID)).thenReturn(5L);
      when(courseLessonRepository.countByCourseIdAndNotDeleted(second.getCourseId())).thenReturn(0L);
      when(lessonProgressRepository.countCompletedByEnrollmentId(second.getId())).thenReturn(0L);

      // ===== ACT =====
      List<EnrollmentResponse> result = enrollmentService.getMyEnrollments();

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(2, result.size()),
          () -> assertEquals(100.0, result.get(0).getCompletionRate()),
          () -> assertEquals(0.0, result.get(1).getCompletionRate()),
          () -> assertEquals("https://cdn.mathmaster.edu.vn/thumb-1.png", result.get(0).getCourseThumbnailUrl()),
          () -> assertEquals(null, result.get(1).getCourseTitle()));

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).countByCourseIdAndNotDeleted(COURSE_ID);
      verify(courseLessonRepository, times(1)).countByCourseIdAndNotDeleted(second.getCourseId());
      verify(lessonProgressRepository, times(1)).countCompletedByEnrollmentId(ENROLLMENT_ID);
      verify(lessonProgressRepository, times(1)).countCompletedByEnrollmentId(second.getId());
      verify(userRepository, times(2)).findById(STUDENT_ID);
    }
  }
}
