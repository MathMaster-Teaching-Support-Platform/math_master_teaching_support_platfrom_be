package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.response.StudentDashboardLearningProgressResponse;
import com.fptu.math_master.dto.response.StudentDashboardOverviewResponse;
import com.fptu.math_master.dto.response.StudentDashboardRecentGradeResponse;
import com.fptu.math_master.dto.response.StudentDashboardStreakResponse;
import com.fptu.math_master.dto.response.StudentDashboardSummaryResponse;
import com.fptu.math_master.dto.response.StudentDashboardUpcomingTaskResponse;
import com.fptu.math_master.dto.response.StudentDashboardWeeklyActivityResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseAssessmentRepository;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.repository.NotificationRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;

@DisplayName("StudentDashboardServiceImpl - Tests")
class StudentDashboardServiceImplTest extends BaseUnitTest {

  private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final UUID STUDENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @InjectMocks private StudentDashboardServiceImpl studentDashboardService;

  @Mock private UserRepository userRepository;
  @Mock private NotificationRepository notificationRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private SubmissionRepository submissionRepository;
  @Mock private CourseAssessmentRepository courseAssessmentRepository;
  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private LessonProgressRepository lessonProgressRepository;

  @Nested
  @DisplayName("getSummary()")
  class GetSummaryTests {

    /**
     * Abnormal case: Ném exception khi student không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID (không có trong UserRepository)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findById(studentId) -> empty (nhánh throw USER_NOT_EXISTED)</li>
     *   <li>Nhánh success được cover bởi it_should_return_summary_when_student_exists</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code USER_NOT_EXISTED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_student_does_not_exist() {
      // ===== ARRANGE =====
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        AppException exception =
            assertThrows(AppException.class, () -> studentDashboardService.getSummary());
        assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(STUDENT_ID);
      verifyNoMoreInteractions(userRepository);
      verifyNoMoreInteractions(
          notificationRepository,
          enrollmentRepository,
          submissionRepository,
          courseAssessmentRepository,
          courseLessonRepository,
          lessonProgressRepository);
    }

    /**
     * Normal case: Trả về summary đầy đủ với pending tasks và progress.
     *
     * <p>Input:
     * <ul>
     *   <li>studentId: STUDENT_ID (hợp lệ)</li>
     *   <li>averageScore repository trả về null</li>
     *   <li>2 pending task (1 task due hôm nay)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>averageScore null -> fallback 0.0</li>
     *   <li>buildPendingTasks có submissions list không rỗng</li>
     *   <li>progressPercent: goalAssignments > 0 (nhánh TRUE)</li>
     *   <li>isSameLocalDate: cả TRUE và FALSE trong filter todayTaskCount</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Summary stats và motivation được tính đúng</li>
     * </ul>
     */
    @Test
    void it_should_return_summary_when_student_exists() {
      // ===== ARRANGE =====
      User student = buildUser(STUDENT_ID, "Nguyen Son Nam", "https://cdn.mathmaster.vn/avatar/nam.png");
      Assessment taskToday =
          buildAssessment(
              UUID.fromString("11111111-1111-1111-1111-111111111111"),
              "Bai tap Dai so tuyen tinh",
              AssessmentType.HOMEWORK,
              atStartOfCurrentDate().plusSeconds(5_400));
      Assessment taskTomorrow =
          buildAssessment(
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              "Quiz Giai tich 1",
              AssessmentType.QUIZ,
              atStartOfCurrentDate().plusSeconds(86_400 + 3_600));

      Submission inProgressSubmission =
          buildSubmission(
              UUID.fromString("33333333-3333-3333-3333-333333333333"),
              taskTomorrow.getId(),
              SubmissionStatus.IN_PROGRESS,
              null,
              null,
              new BigDecimal("45.678"),
              Instant.now().minusSeconds(120));

      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
      when(notificationRepository.countByRecipient_IdAndIsReadFalse(STUDENT_ID)).thenReturn(5L);
      when(enrollmentRepository.countByStudentIdAndStatusAndDeletedAtIsNull(
              STUDENT_ID, EnrollmentStatus.ACTIVE))
          .thenReturn(4L);
      when(submissionRepository.countByStudentIdAndStatusInAndDeletedAtIsNull(
              eq(STUDENT_ID), anyCollection()))
          .thenReturn(3L);
      when(submissionRepository.averageScoreOfGradedByStudentId(STUDENT_ID)).thenReturn(null);
      when(courseAssessmentRepository.findUpcomingAssessmentsByStudentId(
              eq(STUDENT_ID), any(Instant.class), eq(PageRequest.of(0, 500))))
          .thenReturn(
              List.of(
                  new Object[] {taskToday, "Dai so"},
                  new Object[] {taskTomorrow, null}));
      when(submissionRepository.findByStudentIdAndAssessmentIdInOrderByCreatedAtDesc(
              eq(STUDENT_ID), anySet()))
          .thenReturn(List.of(inProgressSubmission));

      // ===== ACT =====
      StudentDashboardSummaryResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getSummary();
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals("Nguyen Son Nam", result.getStudent().getName()),
          () -> assertEquals(5L, result.getNotificationCount()),
          () -> assertEquals(4L, result.getStats().getEnrolledCourses()),
          () -> assertEquals(3L, result.getStats().getCompletedAssignments()),
          () -> assertEquals(2L, result.getStats().getPendingAssignments()),
          () -> assertEquals(0.0, result.getStats().getAverageScore()),
          () -> assertEquals(5, result.getMotivation().getGoalAssignments()),
          () -> assertEquals(2L, result.getMotivation().getRemainingAssignments()),
          () -> assertEquals(60.0, result.getMotivation().getProgressPercent()),
          () -> assertEquals(1, result.getTodayTaskCount()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(STUDENT_ID);
      verify(notificationRepository, times(1)).countByRecipient_IdAndIsReadFalse(STUDENT_ID);
      verify(enrollmentRepository, times(1))
          .countByStudentIdAndStatusAndDeletedAtIsNull(STUDENT_ID, EnrollmentStatus.ACTIVE);
      verify(submissionRepository, times(1))
          .countByStudentIdAndStatusInAndDeletedAtIsNull(eq(STUDENT_ID), anyCollection());
      verify(submissionRepository, times(1)).averageScoreOfGradedByStudentId(STUDENT_ID);
      verify(courseAssessmentRepository, times(1))
          .findUpcomingAssessmentsByStudentId(eq(STUDENT_ID), any(Instant.class), eq(PageRequest.of(0, 500)));
      verify(submissionRepository, times(1))
          .findByStudentIdAndAssessmentIdInOrderByCreatedAtDesc(eq(STUDENT_ID), anySet());
      verifyNoMoreInteractions(
          userRepository,
          notificationRepository,
          enrollmentRepository,
          submissionRepository,
          courseAssessmentRepository);
      verifyNoMoreInteractions(courseLessonRepository, lessonProgressRepository);
    }
  }

  @Nested
  @DisplayName("getUpcomingTasks()")
  class GetUpcomingTasksTests {

    /**
     * Normal case: Trả về task pending đã sort theo dueDate và limit 10.
     *
     * <p>Input:
     * <ul>
     *   <li>4 assessments với các loại HOMEWORK/TEST/QUIZ/null</li>
     *   <li>1 submission completed để bị loại</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isCompletedSubmission: TRUE (skip task) và FALSE (giữ task)</li>
     *   <li>mapTaskType: HOMEWORK, TEST (exam), QUIZ và null</li>
     *   <li>readSubmissionProgress: completed(100), percentage, null(0)</li>
     *   <li>sort dueDate nullsLast</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Danh sách chỉ gồm pending task, map type đúng và được sort đúng</li>
     * </ul>
     */
    @Test
    void it_should_return_pending_tasks_sorted_and_mapped_when_upcoming_assessments_exist() {
      // ===== ARRANGE =====
      Assessment homework =
          buildAssessment(
              UUID.fromString("44444444-4444-4444-4444-444444444444"),
              "Bai tap Ham so bac hai",
              AssessmentType.HOMEWORK,
              Instant.now().plusSeconds(2_000));
      Assessment testExam =
          buildAssessment(
              UUID.fromString("55555555-5555-5555-5555-555555555555"),
              "Kiem tra giua ky",
              AssessmentType.TEST,
              Instant.now().plusSeconds(6_000));
      Assessment quiz =
          buildAssessment(
              UUID.fromString("66666666-6666-6666-6666-666666666666"),
              "Quiz To hop",
              AssessmentType.QUIZ,
              Instant.now().plusSeconds(4_000));
      Assessment unknownType =
          buildAssessment(
              UUID.fromString("77777777-7777-7777-7777-777777777777"),
              "Nhiem vu bo sung",
              null,
              null);

      Submission completedSubmission =
          buildSubmission(
              UUID.fromString("88888888-8888-8888-8888-888888888888"),
              testExam.getId(),
              SubmissionStatus.GRADED,
              null,
              null,
              null,
              Instant.now());
      Submission progressSubmission =
          buildSubmission(
              UUID.fromString("99999999-9999-9999-9999-999999999999"),
              quiz.getId(),
              SubmissionStatus.IN_PROGRESS,
              null,
              null,
              new BigDecimal("66.666"),
              Instant.now().minusSeconds(10));

      when(courseAssessmentRepository.findUpcomingAssessmentsByStudentId(
              eq(STUDENT_ID), any(Instant.class), eq(PageRequest.of(0, 200))))
          .thenReturn(
              List.of(
                  new Object[] {homework, "Giai tich"},
                  new Object[] {testExam, "Dai so"},
                  new Object[] {quiz, "To hop"},
                  new Object[] {unknownType, null}));
      when(submissionRepository.findByStudentIdAndAssessmentIdInOrderByCreatedAtDesc(
              eq(STUDENT_ID), anySet()))
          .thenReturn(List.of(completedSubmission, progressSubmission));

      // ===== ACT =====
      List<StudentDashboardUpcomingTaskResponse> result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getUpcomingTasks();
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(3, result.size()),
          () -> assertEquals(homework.getId().toString(), result.get(0).getId()),
          () -> assertEquals("homework", result.get(0).getType()),
          () -> assertEquals("quiz", result.get(1).getType()),
          () -> assertEquals(66.67, result.get(1).getProgressPercent()),
          () -> assertEquals("quiz", result.get(2).getType()),
          () -> assertEquals("General", result.get(2).getSubject()),
          () -> assertEquals(0.0, result.get(2).getProgressPercent()));

      // ===== VERIFY =====
      verify(courseAssessmentRepository, times(1))
          .findUpcomingAssessmentsByStudentId(eq(STUDENT_ID), any(Instant.class), eq(PageRequest.of(0, 200)));
      verify(submissionRepository, times(1))
          .findByStudentIdAndAssessmentIdInOrderByCreatedAtDesc(eq(STUDENT_ID), anySet());
      verifyNoMoreInteractions(courseAssessmentRepository, submissionRepository);
      verifyNoMoreInteractions(
          userRepository,
          notificationRepository,
          enrollmentRepository,
          courseLessonRepository,
          lessonProgressRepository);
    }
  }

  @Nested
  @DisplayName("getRecentGrades()")
  class GetRecentGradesTests {

    /**
     * Normal case: Không có submission graded thì trả về list rỗng.
     *
     * <p>Input:
     * <ul>
     *   <li>findTop10 graded trả về list rỗng</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>recentGraded.isEmpty() -> TRUE branch</li>
     *   <li>Nhánh mapping được cover bởi it_should_map_recent_grades_when_rows_exist</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về empty list, không query subject mapping</li>
     * </ul>
     */
    @Test
    void it_should_return_empty_list_when_no_graded_submission_exists() {
      // ===== ARRANGE =====
      when(submissionRepository.findTop10ByStudentIdAndStatusAndDeletedAtIsNullOrderByGradedAtDesc(
              STUDENT_ID, SubmissionStatus.GRADED))
          .thenReturn(List.of());

      // ===== ACT =====
      List<StudentDashboardRecentGradeResponse> result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getRecentGrades();
      }

      // ===== ASSERT =====
      assertTrue(result.isEmpty());

      // ===== VERIFY =====
      verify(submissionRepository, times(1))
          .findTop10ByStudentIdAndStatusAndDeletedAtIsNullOrderByGradedAtDesc(
              STUDENT_ID, SubmissionStatus.GRADED);
      verify(courseAssessmentRepository, never()).findSubjectNameByAssessmentIds(anySet());
      verifyNoMoreInteractions(submissionRepository, courseAssessmentRepository);
      verifyNoMoreInteractions(
          userRepository,
          notificationRepository,
          enrollmentRepository,
          courseLessonRepository,
          lessonProgressRepository);
    }

    /**
     * Normal case: Map graded submissions thành recent grade response.
     *
     * <p>Input:
     * <ul>
     *   <li>3 submissions graded với score source khác nhau</li>
     *   <li>1 assessment có subject mapping, 1 assessment fallback General</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>assessmentIds.isEmpty() -> FALSE, thực hiện query subject rows</li>
     *   <li>title fallback khi assessment null</li>
     *   <li>readSubmissionScore theo thứ tự finalScore -> score -> percentage</li>
     *   <li>firstNonNull cho gradedAt fallback updatedAt/createdAt</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Response map đúng title, subject, score và gradedAt</li>
     * </ul>
     */
    @Test
    void it_should_map_recent_grades_when_rows_exist() {
      // ===== ARRANGE =====
      UUID assessmentA = UUID.fromString("12121212-1212-1212-1212-121212121212");
      UUID assessmentB = UUID.fromString("13131313-1313-1313-1313-131313131313");
      UUID assessmentC = UUID.fromString("14141414-1414-1414-1414-141414141414");
      Instant now = Instant.now();

      Submission subA =
          buildSubmission(
              UUID.fromString("15151515-1515-1515-1515-151515151515"),
              assessmentA,
              SubmissionStatus.GRADED,
              new BigDecimal("88.887"),
              null,
              null,
              now.minusSeconds(60));
      subA.setAssessment(buildAssessment(assessmentA, "Kiem tra Chuong 1", AssessmentType.QUIZ, now.plusSeconds(7200)));
      subA.setGradedAt(now.minusSeconds(30));

      Submission subB =
          buildSubmission(
              UUID.fromString("16161616-1616-1616-1616-161616161616"),
              assessmentB,
              SubmissionStatus.GRADED,
              null,
              new BigDecimal("76.665"),
              null,
              now.minusSeconds(200));
      subB.setAssessment(null);
      subB.setGradedAt(null);
      subB.setUpdatedAt(now.minusSeconds(120));

      Submission subC =
          buildSubmission(
              UUID.fromString("17171717-1717-1717-1717-171717171717"),
              assessmentC,
              SubmissionStatus.GRADED,
              null,
              null,
              new BigDecimal("54.444"),
              now.minusSeconds(300));
      subC.setAssessment(buildAssessment(assessmentC, "Bai tap xac suat", AssessmentType.HOMEWORK, now.plusSeconds(8200)));
      subC.setGradedAt(null);
      subC.setUpdatedAt(null);
      subC.setCreatedAt(now.minusSeconds(280));

      when(submissionRepository.findTop10ByStudentIdAndStatusAndDeletedAtIsNullOrderByGradedAtDesc(
              STUDENT_ID, SubmissionStatus.GRADED))
          .thenReturn(List.of(subA, subB, subC));
      when(courseAssessmentRepository.findSubjectNameByAssessmentIds(anySet()))
          .thenReturn(List.of(new Object[] {assessmentA, "Dai so"}, new Object[] {assessmentC, null}));

      // ===== ACT =====
      List<StudentDashboardRecentGradeResponse> result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getRecentGrades();
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(3, result.size()),
          () -> assertEquals("Kiem tra Chuong 1", result.get(0).getTitle()),
          () -> assertEquals("Dai so", result.get(0).getSubject()),
          () -> assertEquals(88.89, result.get(0).getScore()),
          () -> assertEquals("Assessment", result.get(1).getTitle()),
          () -> assertEquals("General", result.get(1).getSubject()),
          () -> assertEquals(76.67, result.get(1).getScore()),
          () -> assertEquals(54.44, result.get(2).getScore()),
          () -> assertNotNull(result.get(1).getGradedAt()),
          () -> assertNotNull(result.get(2).getGradedAt()));

      // ===== VERIFY =====
      verify(submissionRepository, times(1))
          .findTop10ByStudentIdAndStatusAndDeletedAtIsNullOrderByGradedAtDesc(
              STUDENT_ID, SubmissionStatus.GRADED);
      verify(courseAssessmentRepository, times(1)).findSubjectNameByAssessmentIds(anySet());
      verifyNoMoreInteractions(submissionRepository, courseAssessmentRepository);
      verifyNoMoreInteractions(
          userRepository,
          notificationRepository,
          enrollmentRepository,
          courseLessonRepository,
          lessonProgressRepository);
    }
  }

  @Nested
  @DisplayName("getLearningProgress()")
  class GetLearningProgressTests {

    /**
     * Normal case: Không có enrollment active thì trả về list rỗng.
     *
     * <p>Input:
     * <ul>
     *   <li>findByStudentIdAndStatusWithCourseAndSubject trả về empty list</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>enrollments.isEmpty() -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Không gọi các repository đếm lesson và trả về list rỗng</li>
     * </ul>
     */
    @Test
    void it_should_return_empty_list_when_no_active_enrollments_exist() {
      // ===== ARRANGE =====
      when(enrollmentRepository.findByStudentIdAndStatusWithCourseAndSubject(
              STUDENT_ID, EnrollmentStatus.ACTIVE))
          .thenReturn(List.of());

      // ===== ACT =====
      List<StudentDashboardLearningProgressResponse> result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getLearningProgress();
      }

      // ===== ASSERT =====
      assertTrue(result.isEmpty());

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndStatusWithCourseAndSubject(STUDENT_ID, EnrollmentStatus.ACTIVE);
      verify(courseLessonRepository, never()).countByCourseIdsAndNotDeleted(anyList());
      verify(lessonProgressRepository, never()).countCompletedByEnrollmentIds(anyList());
      verifyNoMoreInteractions(enrollmentRepository, courseLessonRepository, lessonProgressRepository);
      verifyNoMoreInteractions(
          userRepository, notificationRepository, submissionRepository, courseAssessmentRepository);
    }

    /**
     * Normal case: Gộp progress theo subject/course và sort theo subject.
     *
     * <p>Input:
     * <ul>
     *   <li>3 enrollments với subjectName từ subject, course title và fallback General</li>
     *   <li>total/completed lesson trả về một phần để kích hoạt default 0</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>subjectName: subject.name / course.title / General</li>
     *   <li>percent: total > 0 và total == 0</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Danh sách được gộp đúng và sort tăng dần theo subject</li>
     * </ul>
     */
    @Test
    void it_should_aggregate_and_sort_learning_progress_when_enrollments_exist() {
      // ===== ARRANGE =====
      Enrollment e1 =
          buildEnrollment(
              UUID.fromString("21212121-2121-2121-2121-212121212121"),
              UUID.fromString("31313131-3131-3131-3131-313131313131"),
              buildCourse(
                  UUID.fromString("31313131-3131-3131-3131-313131313131"),
                  "Khoa hoc Giai tich",
                  buildSubject("Giai tich")));
      Enrollment e2 =
          buildEnrollment(
              UUID.fromString("22222222-2222-2222-2222-222222222220"),
              UUID.fromString("32323232-3232-3232-3232-323232323232"),
              buildCourse(
                  UUID.fromString("32323232-3232-3232-3232-323232323232"),
                  "Khoa hoc To hop",
                  null));
      Enrollment e3 =
          buildEnrollment(
              UUID.fromString("23232323-2323-2323-2323-232323232323"),
              UUID.fromString("33333333-3333-3333-3333-333333333330"),
              null);

      when(enrollmentRepository.findByStudentIdAndStatusWithCourseAndSubject(
              STUDENT_ID, EnrollmentStatus.ACTIVE))
          .thenReturn(List.of(e1, e2, e3));
      when(courseLessonRepository.countByCourseIdsAndNotDeleted(anyList()))
          .thenReturn(List.of(new Object[] {e1.getCourseId(), 10L}, new Object[] {e2.getCourseId(), 4L}));
      when(lessonProgressRepository.countCompletedByEnrollmentIds(anyList()))
          .thenReturn(List.of(new Object[] {e1.getId(), 4L}, new Object[] {e2.getId(), 2L}));

      // ===== ACT =====
      List<StudentDashboardLearningProgressResponse> result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getLearningProgress();
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(3, result.size()),
          () -> assertEquals("General", result.get(0).getSubject()),
          () -> assertEquals(0L, result.get(0).getDoneLessons()),
          () -> assertEquals(0L, result.get(0).getTotalLessons()),
          () -> assertEquals(0.0, result.get(0).getPercent()),
          () -> assertEquals("Giai tich", result.get(1).getSubject()),
          () -> assertEquals(40.0, result.get(1).getPercent()),
          () -> assertEquals("Khoa hoc To hop", result.get(2).getSubject()),
          () -> assertEquals(50.0, result.get(2).getPercent()));

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndStatusWithCourseAndSubject(STUDENT_ID, EnrollmentStatus.ACTIVE);
      verify(courseLessonRepository, times(1)).countByCourseIdsAndNotDeleted(anyList());
      verify(lessonProgressRepository, times(1)).countCompletedByEnrollmentIds(anyList());
      verifyNoMoreInteractions(enrollmentRepository, courseLessonRepository, lessonProgressRepository);
      verifyNoMoreInteractions(
          userRepository, notificationRepository, submissionRepository, courseAssessmentRepository);
    }
  }

  @Nested
  @DisplayName("getWeeklyActivity()")
  class GetWeeklyActivityTests {

    /**
     * Normal case: previous week = 0 và current week > 0 thì delta = 100.
     *
     * <p>Input:
     * <ul>
     *   <li>Watch/submission activity có dữ liệu hợp lệ và dữ liệu invalid</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>previousTotalSeconds == 0 và currentTotalSeconds > 0</li>
     *   <li>accumulateByDay bỏ qua timestamp null hoặc seconds <= 0</li>
     *   <li>toDayLabel cover đủ 7 ngày</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>totalHours > 0, deltaPercent = 100, days có 7 phần tử</li>
     * </ul>
     */
    @Test
    void it_should_return_weekly_activity_with_delta_100_when_previous_week_has_no_activity() {
      // ===== ARRANGE =====
      LocalDate today = LocalDate.now(DASHBOARD_ZONE);
      Instant dayMinus1 = today.minusDays(1).atStartOfDay(DASHBOARD_ZONE).toInstant();
      Instant dayMinus2 = today.minusDays(2).atStartOfDay(DASHBOARD_ZONE).toInstant();

      when(lessonProgressRepository.findWatchActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class)))
          .thenReturn(List.of(new Object[] {dayMinus1, 1800L}, new Object[] {null, 900L}, new Object[] {dayMinus2, 0L}))
          .thenReturn(List.of());
      when(submissionRepository.findSubmissionActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class)))
          .thenReturn(List.<Object[]>of(new Object[] {dayMinus1, 3600L}))
          .thenReturn(List.<Object[]>of());

      // ===== ACT =====
      StudentDashboardWeeklyActivityResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getWeeklyActivity();
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1.5, result.getTotalHours()),
          () -> assertEquals(100.0, result.getDeltaPercentVsPreviousWeek()),
          () -> assertEquals(7, result.getDays().size()),
          () -> assertNotNull(result.getRange()),
          () -> assertTrue(result.getDays().stream().anyMatch(day -> day.getHours() > 0)));

      // ===== VERIFY =====
      verify(lessonProgressRepository, times(2))
          .findWatchActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class));
      verify(submissionRepository, times(2))
          .findSubmissionActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class));
      verifyNoMoreInteractions(lessonProgressRepository, submissionRepository);
      verifyNoMoreInteractions(
          userRepository, notificationRepository, enrollmentRepository, courseAssessmentRepository, courseLessonRepository);
    }

    /**
     * Normal case: previous week có activity thì tính delta theo công thức chuẩn.
     *
     * <p>Input:
     * <ul>
     *   <li>Current week = 1800s, previous week = 3600s</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>previousTotalSeconds != 0 (nhánh ELSE)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>deltaPercentVsPreviousWeek = -50.0</li>
     * </ul>
     */
    @Test
    void it_should_return_negative_delta_when_current_week_has_less_activity_than_previous_week() {
      // ===== ARRANGE =====
      LocalDate today = LocalDate.now(DASHBOARD_ZONE);
      Instant currentDay = today.minusDays(1).atStartOfDay(DASHBOARD_ZONE).toInstant();
      Instant previousDay = today.minusDays(8).atStartOfDay(DASHBOARD_ZONE).toInstant();

      when(lessonProgressRepository.findWatchActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class)))
          .thenReturn(List.<Object[]>of(new Object[] {currentDay, 1800L}))
          .thenReturn(List.<Object[]>of(new Object[] {previousDay, 3600L}));
      when(submissionRepository.findSubmissionActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class)))
          .thenReturn(List.of())
          .thenReturn(List.of());

      // ===== ACT =====
      StudentDashboardWeeklyActivityResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getWeeklyActivity();
      }

      // ===== ASSERT =====
      assertEquals(-50.0, result.getDeltaPercentVsPreviousWeek());

      // ===== VERIFY =====
      verify(lessonProgressRepository, times(2))
          .findWatchActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class));
      verify(submissionRepository, times(2))
          .findSubmissionActivityForWindow(eq(STUDENT_ID), any(Instant.class), any(Instant.class));
      verifyNoMoreInteractions(lessonProgressRepository, submissionRepository);
      verifyNoMoreInteractions(
          userRepository, notificationRepository, enrollmentRepository, courseAssessmentRepository, courseLessonRepository);
    }
  }

  @Nested
  @DisplayName("getStreak()")
  class GetStreakTests {

    /**
     * Normal case: Student có active liên tiếp từ hôm nay.
     *
     * <p>Input:
     * <ul>
     *   <li>activeDays gồm today và yesterday</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>while(activeDays.contains(cursor)) chạy 2 vòng rồi dừng</li>
     *   <li>message branch streak > 0</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>currentStreakDays = 2 và message chứa "2-day"</li>
     * </ul>
     */
    @Test
    void it_should_return_positive_streak_when_today_and_previous_days_are_active() {
      // ===== ARRANGE =====
      LocalDate today = LocalDate.now(DASHBOARD_ZONE);
      Instant activeToday = today.atStartOfDay(DASHBOARD_ZONE).toInstant();
      Instant activeYesterday = today.minusDays(1).atStartOfDay(DASHBOARD_ZONE).toInstant();

      when(lessonProgressRepository.findLastWatchedAtAfter(eq(STUDENT_ID), any(Instant.class)))
          .thenReturn(List.of(activeToday));
      when(submissionRepository.findSubmittedAtAfter(eq(STUDENT_ID), any(Instant.class)))
          .thenReturn(List.of(activeYesterday));

      // ===== ACT =====
      StudentDashboardStreakResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getStreak();
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(2, result.getCurrentStreakDays()),
          () -> assertTrue(result.getMessage().contains("2-day")),
          () -> assertEquals(7, result.getDays().size()),
          () -> assertTrue(result.getDays().stream().anyMatch(StudentDashboardStreakResponse.StreakDay::isActive)),
          () -> assertTrue(result.getDays().stream().anyMatch(day -> !day.isActive())));

      // ===== VERIFY =====
      verify(lessonProgressRepository, times(1)).findLastWatchedAtAfter(eq(STUDENT_ID), any(Instant.class));
      verify(submissionRepository, times(1)).findSubmittedAtAfter(eq(STUDENT_ID), any(Instant.class));
      verifyNoMoreInteractions(lessonProgressRepository, submissionRepository);
      verifyNoMoreInteractions(
          userRepository, notificationRepository, enrollmentRepository, courseAssessmentRepository, courseLessonRepository);
    }

    /**
     * Normal case: Student chưa active ngày nào trong lookback.
     *
     * <p>Input:
     * <ul>
     *   <li>watchTimes và submittedTimes đều rỗng</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>while loop không chạy</li>
     *   <li>message branch streak == 0</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>currentStreakDays = 0 và message khuyến khích bắt đầu streak</li>
     * </ul>
     */
    @Test
    void it_should_return_zero_streak_message_when_no_active_day_exists() {
      // ===== ARRANGE =====
      when(lessonProgressRepository.findLastWatchedAtAfter(eq(STUDENT_ID), any(Instant.class)))
          .thenReturn(List.of());
      when(submissionRepository.findSubmittedAtAfter(eq(STUDENT_ID), any(Instant.class)))
          .thenReturn(List.of());

      // ===== ACT =====
      StudentDashboardStreakResponse result;
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(STUDENT_ID);
        result = studentDashboardService.getStreak();
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0, result.getCurrentStreakDays()),
          () -> assertTrue(result.getMessage().contains("Start your streak")),
          () -> assertEquals(7, result.getDays().size()));

      // ===== VERIFY =====
      verify(lessonProgressRepository, times(1)).findLastWatchedAtAfter(eq(STUDENT_ID), any(Instant.class));
      verify(submissionRepository, times(1)).findSubmittedAtAfter(eq(STUDENT_ID), any(Instant.class));
      verifyNoMoreInteractions(lessonProgressRepository, submissionRepository);
      verifyNoMoreInteractions(
          userRepository, notificationRepository, enrollmentRepository, courseAssessmentRepository, courseLessonRepository);
    }
  }

  @Nested
  @DisplayName("getOverview()")
  class GetOverviewTests {

    /**
     * Normal case: Tổng hợp đầy đủ 6 widget dashboard vào overview.
     *
     * <p>Input:
     * <ul>
     *   <li>Spy service và stub 6 method thành phần</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>Builder overview map đúng toàn bộ field</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Overview chứa đúng object tham chiếu đã stub</li>
     * </ul>
     */
    @Test
    void it_should_return_overview_with_all_component_sections() {
      // ===== ARRANGE =====
      StudentDashboardServiceImpl spyService = Mockito.spy(studentDashboardService);
      StudentDashboardSummaryResponse summary = StudentDashboardSummaryResponse.builder().notificationCount(1L).build();
      List<StudentDashboardUpcomingTaskResponse> tasks = List.of(StudentDashboardUpcomingTaskResponse.builder().id(OTHER_ID.toString()).build());
      List<StudentDashboardRecentGradeResponse> grades = List.of(StudentDashboardRecentGradeResponse.builder().id(OTHER_ID.toString()).build());
      List<StudentDashboardLearningProgressResponse> progress = List.of(StudentDashboardLearningProgressResponse.builder().subject("Dai so").build());
      StudentDashboardWeeklyActivityResponse weekly = StudentDashboardWeeklyActivityResponse.builder().totalHours(3.5).build();
      StudentDashboardStreakResponse streak = StudentDashboardStreakResponse.builder().currentStreakDays(4).build();

      Mockito.doReturn(summary).when(spyService).getSummary();
      Mockito.doReturn(tasks).when(spyService).getUpcomingTasks();
      Mockito.doReturn(grades).when(spyService).getRecentGrades();
      Mockito.doReturn(progress).when(spyService).getLearningProgress();
      Mockito.doReturn(weekly).when(spyService).getWeeklyActivity();
      Mockito.doReturn(streak).when(spyService).getStreak();

      // ===== ACT =====
      StudentDashboardOverviewResponse result = spyService.getOverview();

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(summary, result.getSummary()),
          () -> assertEquals(tasks, result.getUpcomingTasks()),
          () -> assertEquals(grades, result.getRecentGrades()),
          () -> assertEquals(progress, result.getLearningProgress()),
          () -> assertEquals(weekly, result.getWeeklyActivity()),
          () -> assertEquals(streak, result.getStreak()));

      // ===== VERIFY =====
      verify(spyService, times(1)).getSummary();
      verify(spyService, times(1)).getUpcomingTasks();
      verify(spyService, times(1)).getRecentGrades();
      verify(spyService, times(1)).getLearningProgress();
      verify(spyService, times(1)).getWeeklyActivity();
      verify(spyService, times(1)).getStreak();
      verify(spyService, times(1)).getOverview();
      verifyNoMoreInteractions(spyService);
      verifyNoMoreInteractions(
          userRepository,
          notificationRepository,
          enrollmentRepository,
          submissionRepository,
          courseAssessmentRepository,
          courseLessonRepository,
          lessonProgressRepository);
    }
  }

  private User buildUser(UUID id, String fullName, String avatar) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    user.setAvatar(avatar);
    return user;
  }

  private Assessment buildAssessment(UUID id, String title, AssessmentType type, Instant endDate) {
    Assessment assessment = new Assessment();
    assessment.setId(id);
    assessment.setTitle(title);
    assessment.setAssessmentType(type);
    assessment.setEndDate(endDate);
    return assessment;
  }

  private Submission buildSubmission(
      UUID id,
      UUID assessmentId,
      SubmissionStatus status,
      BigDecimal finalScore,
      BigDecimal score,
      BigDecimal percentage,
      Instant createdAt) {
    Submission submission = new Submission();
    submission.setId(id);
    submission.setAssessmentId(assessmentId);
    submission.setStudentId(STUDENT_ID);
    submission.setStatus(status);
    submission.setFinalScore(finalScore);
    submission.setScore(score);
    submission.setPercentage(percentage);
    submission.setCreatedAt(createdAt);
    return submission;
  }

  private Enrollment buildEnrollment(UUID id, UUID courseId, Course course) {
    Enrollment enrollment = new Enrollment();
    enrollment.setId(id);
    enrollment.setStudentId(STUDENT_ID);
    enrollment.setCourseId(courseId);
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setCourse(course);
    return enrollment;
  }

  private Course buildCourse(UUID id, String title, Subject subject) {
    Course course = new Course();
    course.setId(id);
    course.setTitle(title);
    course.setSubject(subject);
    return course;
  }

  private Subject buildSubject(String name) {
    Subject subject = new Subject();
    subject.setName(name);
    return subject;
  }

  private Instant atStartOfCurrentDate() {
    return LocalDate.now(DASHBOARD_ZONE).atStartOfDay(DASHBOARD_ZONE).toInstant();
  }
}
