package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AnswerUpdateRequest;
import com.fptu.math_master.dto.request.FlagUpdateRequest;
import com.fptu.math_master.dto.request.StartAssessmentRequest;
import com.fptu.math_master.dto.request.SubmitAssessmentRequest;
import com.fptu.math_master.dto.response.AttemptStartResponse;
import com.fptu.math_master.dto.response.StudentAssessmentResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.CourseAssessment;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuizAttempt;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.enums.AssessmentMode;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.AttemptScoringPolicy;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AnswerRepository;
import com.fptu.math_master.repository.AssessmentLessonRepository;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.CourseAssessmentRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.QuizAttemptRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.service.AssessmentDraftService;
import com.fptu.math_master.service.CentrifugoService;
import com.fptu.math_master.service.GradingService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Unit tests for {@link StudentAssessmentServiceImpl}.
 *
 * <p>Coverage targets student flows: listing assessments, starting attempts, draft updates, and
 * submission with auto-grading side effects.
 */
@DisplayName("StudentAssessmentServiceImpl - Tests")
class StudentAssessmentServiceImplTest extends BaseUnitTest {

  @InjectMocks private StudentAssessmentServiceImpl studentAssessmentService;

  @Mock private AssessmentRepository assessmentRepository;
  @Mock private SubmissionRepository submissionRepository;
  @Mock private QuizAttemptRepository quizAttemptRepository;
  @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
  @Mock private AnswerRepository answerRepository;
  @Mock private AssessmentDraftService draftService;
  @Mock private CentrifugoService centrifugoService;
  @Mock private GradingService gradingService;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CourseAssessmentRepository courseAssessmentRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private AssessmentLessonRepository assessmentLessonRepository;
  @Mock private LessonRepository lessonRepository;

  private UUID studentId;
  private UUID courseId;
  private UUID assessmentId;
  private UUID submissionId;
  private UUID attemptId;
  private UUID questionId;

  @BeforeEach
  void setUp() {
    studentId = UUID.fromString("7c2e4b1a-9d3f-4a1c-b2e8-6f0a1d2c3e4f");
    courseId = UUID.fromString("8d3f5c2b-0e4a-5b2d-c3f9-7a1b2c3d4e5f");
    assessmentId = UUID.fromString("9e4a6d3c-1f5b-6c3e-d4a0-8b2c3d4e5f6a");
    submissionId = UUID.fromString("ae5b7e4d-2a6c-7d4f-e5b1-9c3d4e5f6a7b");
    attemptId = UUID.fromString("bf6c8f5e-3b7d-8e5a-f6c2-0d4e5f6a7b8c");
    questionId = UUID.fromString("c07d9a6f-4c8e-9f6b-a7d3-1e5f6a7b8c9d");

    lenient().when(assessmentLessonRepository.findByAssessmentIdIn(any())).thenReturn(List.of());
    lenient().when(courseRepository.findAllById(any())).thenReturn(List.of());
    lenient().when(courseRepository.findById(any())).thenReturn(Optional.empty());
    lenient().when(lessonRepository.findByChapterIdAndNotDeleted(any())).thenReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsStudent(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("student-assessment-token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("scope", "assessment:read")
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.STUDENT_ROLE)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private Assessment buildPublishedAssessment(
      UUID id,
      String title,
      Instant startDate,
      Instant endDate,
      Boolean randomize,
      Boolean allowMultiple,
      Integer maxAttempts,
      AttemptScoringPolicy policy) {
    Assessment assessment =
        Assessment.builder()
            .teacherId(UUID.fromString("d18e0b7f-5c9d-4e6a-a1f2-3b4c5d6e7f8a"))
            .title(title)
            .description("Nội dung đánh giá năng lực sau chương Hàm số bậc nhất")
            .assessmentType(AssessmentType.QUIZ)
            .assessmentMode(AssessmentMode.MATRIX_BASED)
            .status(AssessmentStatus.PUBLISHED)
            .timeLimitMinutes(45)
            .passingScore(new BigDecimal("5.50"))
            .startDate(startDate)
            .endDate(endDate)
            .randomizeQuestions(randomize)
            .allowMultipleAttempts(allowMultiple)
            .maxAttempts(maxAttempts)
            .attemptScoringPolicy(policy)
            .showScoreImmediately(true)
            .build();
    assessment.setId(id);
    return assessment;
  }

  private Enrollment buildEnrollment(UUID sid, UUID cid, EnrollmentStatus status) {
    return Enrollment.builder()
        .studentId(sid)
        .courseId(cid)
        .status(status)
        .enrolledAt(Instant.now())
        .build();
  }

  private CourseAssessment buildCourseAssessment(UUID cid, UUID aid, int order, boolean required) {
    CourseAssessment ca =
        CourseAssessment.builder()
            .courseId(cid)
            .assessmentId(aid)
            .orderIndex(order)
            .isRequired(required)
            .build();
    ca.setId(UUID.randomUUID());
    return ca;
  }

  private Question buildQuestion(UUID qid) {
    Question q =
        Question.builder()
            .questionType(QuestionType.MULTIPLE_CHOICE)
            .questionText("Tìm nghiệm của phương trình 2x + 6 = 0 trên tập số thực")
            .points(new BigDecimal("2.00"))
            .build();
    q.setId(qid);
    return q;
  }

  private AssessmentQuestion buildAssessmentQuestion(
      UUID aid, Question q, int order, BigDecimal override) {
    AssessmentQuestion aq =
        AssessmentQuestion.builder()
            .assessmentId(aid)
            .questionId(q.getId())
            .orderIndex(order)
            .pointsOverride(override)
            .build();
    aq.setId(UUID.randomUUID());
    aq.setQuestion(q);
    return aq;
  }

  private void stubCommonAssessmentQuestions(Assessment assessment, Question q) {
    when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessment.getId()))
        .thenReturn(List.of(buildAssessmentQuestion(assessment.getId(), q, 1, null)));
  }

  private void stubAccessibleCoursePath(Assessment assessment) {
    when(enrollmentRepository.findByStudentIdAndStatusAndDeletedAtIsNullOrderByEnrolledAtDesc(
            eq(studentId), eq(EnrollmentStatus.ACTIVE)))
        .thenReturn(List.of(buildEnrollment(studentId, courseId, EnrollmentStatus.ACTIVE)));
    when(courseAssessmentRepository.findByCourseIdInAndNotDeleted(Set.of(courseId)))
        .thenReturn(List.of(buildCourseAssessment(courseId, assessment.getId(), 1, true)));
    when(assessmentRepository.findByIdInAndNotDeleted(Set.of(assessment.getId())))
        .thenReturn(List.of(assessment));
  }

  @Nested
  @DisplayName("getMyAssessments()")
  class GetMyAssessmentsTests {

    /**
     * Normal case: Trả về trang rỗng khi học sinh không có khóa học hoạt động nào liên kết đề.
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>accessibleAssessmentIds rỗng → PageImpl rỗng sớm</li>
     * </ul>
     */
    @Test
    @DisplayName("Normal: trang rỗng khi không có assessment khả dụng")
    void it_should_return_empty_page_when_no_accessible_assessments() {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndStatusAndDeletedAtIsNullOrderByEnrolledAtDesc(
              eq(studentId), eq(EnrollmentStatus.ACTIVE)))
          .thenReturn(List.of());
      Pageable pageable = PageRequest.of(0, 10);

      Page<?> result = studentAssessmentService.getMyAssessments(null, pageable);

      assertNotNull(result);
      assertTrue(result.getContent().isEmpty());
      assertEquals(0, result.getTotalElements());
      verify(assessmentRepository, never()).findByIdInAndNotDeleted(any());
    }

    /**
     * Normal case: Lọc bản nháp và chỉ giữ đề đã xuất bản trong phạm vi thời gian mở.
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>status != PUBLISHED → loại</li>
     *   <li>startDate trong tương lai → isAssessmentAvailable false</li>
     * </ul>
     */
    @Test
    @DisplayName("Normal: chỉ trả về đề đã xuất bản và đang trong khung thời gian")
    void it_should_filter_unpublished_and_future_start_assessments() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Kiểm tra nhanh Đại số 10",
              now.minusSeconds(3600),
              now.plusSeconds(7200),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      Assessment draft =
          buildPublishedAssessment(
              UUID.randomUUID(),
              "Bản nháp chưa phát hành",
              now.minusSeconds(3600),
              now.plusSeconds(7200),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      draft.setStatus(AssessmentStatus.DRAFT);
      Assessment futureStart =
          buildPublishedAssessment(
              UUID.randomUUID(),
              "Đề mở sau một tuần",
              now.plusSeconds(864000),
              now.plusSeconds(900000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      when(enrollmentRepository.findByStudentIdAndStatusAndDeletedAtIsNullOrderByEnrolledAtDesc(
              eq(studentId), eq(EnrollmentStatus.ACTIVE)))
          .thenReturn(List.of(buildEnrollment(studentId, courseId, EnrollmentStatus.ACTIVE)));
      when(courseAssessmentRepository.findByCourseIdInAndNotDeleted(Set.of(courseId)))
          .thenReturn(
              List.of(
                  buildCourseAssessment(courseId, published.getId(), 1, true),
                  buildCourseAssessment(courseId, draft.getId(), 2, false),
                  buildCourseAssessment(courseId, futureStart.getId(), 3, false)));
      when(assessmentRepository.findByIdInAndNotDeleted(any()))
          .thenReturn(List.of(published, draft, futureStart));
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(10.0);
      when(submissionRepository.findByAssessmentIdAndStudentId(any(), eq(studentId)))
          .thenReturn(Optional.empty());
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              any(), eq(studentId), eq(SubmissionStatus.IN_PROGRESS)))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(any()))
          .thenReturn(List.of());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(any()))
          .thenReturn(List.of());

      Page<StudentAssessmentResponse> result =
          studentAssessmentService.getMyAssessments(null, PageRequest.of(0, 10));

      assertEquals(1, result.getTotalElements());
      assertEquals(published.getId(), result.getContent().get(0).getId());
    }

    /**
     * Normal case: Phân trang khi offset vượt quá số phần tử sau lọc.
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>start &gt;= responses.size() → PageImpl rỗng với total đúng</li>
     * </ul>
     */
    @Test
    @DisplayName("Normal: trang rỗng khi offset vượt quá tổng bản ghi")
    void it_should_return_empty_slice_when_page_offset_exceeds_total() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Bài kiểm tra 15 phút chương Lũy thừa",
              now.minusSeconds(7200),
              now.plusSeconds(7200),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(8.0);
      when(submissionRepository.findByAssessmentIdAndStudentId(any(), eq(studentId)))
          .thenReturn(Optional.empty());
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              any(), eq(studentId), eq(SubmissionStatus.IN_PROGRESS)))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(any()))
          .thenReturn(List.of());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(any()))
          .thenReturn(List.of());

      Page<?> page = studentAssessmentService.getMyAssessments(null, PageRequest.of(5, 10));

      assertTrue(page.getContent().isEmpty());
      assertEquals(1, page.getTotalElements());
    }

    /**
     * Normal case: statusFilter rỗng cho phép mọi studentStatus; lọc theo chuỗi khớp không phân biệt hoa
     * thường.
     *
     * <p>Branch coverage:
     *
     * <ul>
     *   <li>matchesStatusFilter null và rỗng → true</li>
     *   <li>equalsIgnoreCase khi filter khác hoa thường</li>
     * </ul>
     */
    @Test
    @DisplayName("Normal: lọc theo trạng thái học sinh khi có filter")
    void it_should_apply_status_filter_case_insensitively() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Đánh giá cuối chương Số học",
              now.minusSeconds(3600),
              now.plusSeconds(360000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(12.0);
      Submission submission =
          Submission.builder()
              .assessmentId(published.getId())
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now.minusSeconds(600))
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findByAssessmentIdAndStudentId(published.getId(), studentId))
          .thenReturn(Optional.of(submission));
      QuizAttempt active =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(published.getId())
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now.minusSeconds(300))
              .build();
      active.setId(attemptId);
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              published.getId(), studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of(active));
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(List.of(active));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(any()))
          .thenReturn(List.of());

      assertEquals(
          1,
          studentAssessmentService.getMyAssessments("", PageRequest.of(0, 10)).getTotalElements());
      assertEquals(
          1,
          studentAssessmentService
              .getMyAssessments("in_progress", PageRequest.of(0, 10))
              .getTotalElements());
      assertEquals(
          0,
          studentAssessmentService
              .getMyAssessments("completed", PageRequest.of(0, 10))
              .getTotalElements());
    }

    /**
     * Abnormal case: getCurrentUserId khi authentication không phải JWT.
     *
     * <p>Expectation:
     *
     * <ul>
     *   <li>Ném {@link IllegalStateException}</li>
     * </ul>
     */
    @Test
    @DisplayName("Abnormal: IllegalState khi không phải JwtAuthenticationToken")
    void it_should_throw_illegal_state_when_authentication_is_not_jwt() {
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken("tran.thi.huong", "n/a", List.of()));

      assertThrows(
          IllegalStateException.class,
          () -> studentAssessmentService.getMyAssessments(null, PageRequest.of(0, 5)));
    }
  }

  @Nested
  @DisplayName("getMyAssessmentsByCourse()")
  class GetMyAssessmentsByCourseTests {

    @Test
    @DisplayName("Normal: trang rỗng khi chưa ghi danh khóa học")
    void it_should_return_empty_when_enrollment_missing() {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
          .thenReturn(Optional.empty());

      Page<?> page =
          studentAssessmentService.getMyAssessmentsByCourse(courseId, null, PageRequest.of(0, 10));

      assertTrue(page.getContent().isEmpty());
      verify(courseAssessmentRepository, never()).findByCourseIdAndNotDeleted(any());
    }

    @Test
    @DisplayName("Normal: trang rỗng khi ghi danh không ACTIVE")
    void it_should_return_empty_when_enrollment_not_active() {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
          .thenReturn(Optional.of(buildEnrollment(studentId, courseId, EnrollmentStatus.DROPPED)));

      Page<?> page =
          studentAssessmentService.getMyAssessmentsByCourse(courseId, null, PageRequest.of(0, 10));

      assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Normal: trang rỗng khi khóa học chưa gắn đề")
    void it_should_return_empty_when_course_has_no_assessments() {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
          .thenReturn(Optional.of(buildEnrollment(studentId, courseId, EnrollmentStatus.ACTIVE)));
      when(courseAssessmentRepository.findByCourseIdAndNotDeleted(courseId)).thenReturn(List.of());

      Page<?> page =
          studentAssessmentService.getMyAssessmentsByCourse(courseId, null, PageRequest.of(0, 10));

      assertTrue(page.getContent().isEmpty());
    }

    @Test
    @DisplayName("Normal: trả về đề theo thứ tự course và due date")
    void it_should_return_assessments_ordered_by_course_index_then_due_date() {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
          .thenReturn(Optional.of(buildEnrollment(studentId, courseId, EnrollmentStatus.ACTIVE)));
      Instant now = Instant.now();
      UUID idLate = UUID.fromString("11111111-1111-1111-1111-111111111111");
      UUID idEarly = UUID.fromString("22222222-2222-2222-2222-222222222222");
      Assessment aLate =
          buildPublishedAssessment(
              idLate,
              "Đề thứ hai theo lịch",
              now.minusSeconds(1000),
              now.plusSeconds(50000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      Assessment aEarly =
          buildPublishedAssessment(
              idEarly,
              "Đề thứ nhất theo lịch",
              now.minusSeconds(1000),
              now.plusSeconds(40000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      List<CourseAssessment> links = new ArrayList<>();
      links.add(buildCourseAssessment(courseId, idLate, 2, true));
      links.add(buildCourseAssessment(courseId, idEarly, 1, false));
      links.add(buildCourseAssessment(courseId, idLate, 2, true));
      when(courseAssessmentRepository.findByCourseIdAndNotDeleted(courseId)).thenReturn(links);
      when(assessmentRepository.findByIdInAndNotDeleted(Set.of(idLate, idEarly)))
          .thenReturn(List.of(aLate, aEarly));
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(10.0);
      when(submissionRepository.findByAssessmentIdAndStudentId(any(), eq(studentId)))
          .thenReturn(Optional.empty());
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              any(), eq(studentId), eq(SubmissionStatus.IN_PROGRESS)))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(any()))
          .thenReturn(List.of());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(any()))
          .thenReturn(List.of());

      var page =
          studentAssessmentService.getMyAssessmentsByCourse(courseId, null, PageRequest.of(0, 10));

      assertEquals(2, page.getTotalElements());
      assertEquals(idEarly, page.getContent().get(0).getId());
    }
  }

  @Nested
  @DisplayName("getAssessmentDetails()")
  class GetAssessmentDetailsTests {

    @Test
    @DisplayName("Abnormal: ASSESSMENT_ACCESS_DENIED khi không thuộc khóa học liên kết")
    void it_should_throw_when_assessment_not_accessible() {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndStatusAndDeletedAtIsNullOrderByEnrolledAtDesc(
              eq(studentId), eq(EnrollmentStatus.ACTIVE)))
          .thenReturn(List.of());

      AppException ex =
          assertThrows(
              AppException.class,
              () -> studentAssessmentService.getAssessmentDetails(assessmentId));

      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ASSESSMENT_NOT_FOUND khi id không tồn tại sau kiểm tra quyền")
    void it_should_throw_not_found_when_repository_empty() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Bài tập về nhà Tuần 6",
              now.minusSeconds(1000),
              now.plusSeconds(100000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(
              AppException.class,
              () -> studentAssessmentService.getAssessmentDetails(assessmentId));

      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ASSESSMENT_NOT_PUBLISHED khi trạng thái khác PUBLISHED")
    void it_should_throw_when_assessment_not_published() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment closed =
          buildPublishedAssessment(
              assessmentId,
              "Đề đã đóng",
              now.minusSeconds(10000),
              now.plusSeconds(1000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      closed.setStatus(AssessmentStatus.CLOSED);
      stubAccessibleCoursePath(closed);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(closed));
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(5.0);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.empty());
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(any()))
          .thenReturn(List.of());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      AppException ex =
          assertThrows(
              AppException.class,
              () -> studentAssessmentService.getAssessmentDetails(assessmentId));

      assertEquals(ErrorCode.ASSESSMENT_NOT_PUBLISHED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Normal: trả về chi tiết khi đề hợp lệ")
    void it_should_return_details_when_assessment_valid() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Kiểm tra định kỳ Hình học không gian",
              now.minusSeconds(2000),
              now.plusSeconds(200000),
              false,
              true,
              3,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(null);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.empty());
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(any()))
          .thenReturn(List.of());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      var res = studentAssessmentService.getAssessmentDetails(assessmentId);

      assertEquals(assessmentId, res.getId());
      assertEquals(BigDecimal.ZERO, res.getTotalPoints());
    }

    @Test
    @DisplayName("Normal: studentStatus COMPLETED khi submission đã GRADED")
    void it_should_mark_completed_when_submission_graded() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Bài đã được giảng viên chấm điểm",
              now.minusSeconds(50000),
              now.plusSeconds(500000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(10.0);
      Submission submission =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.GRADED)
              .startedAt(now.minusSeconds(40000))
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.of(submission));
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(List.of());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      var res = studentAssessmentService.getAssessmentDetails(assessmentId);

      assertEquals("COMPLETED", res.getStudentStatus());
    }

    @Test
    @DisplayName("Normal: cannotStartReason khi đã hết lượt làm nhiều lần")
    void it_should_set_cannot_start_reason_when_max_attempts_reached_with_multi_policy() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Đề cho phép hai lần và đã dùng hết",
              now.minusSeconds(10000),
              now.plusSeconds(200000),
              false,
              true,
              2,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(10.0);
      Submission submission =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now.minusSeconds(5000))
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.of(submission));
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(
              List.of(
                  QuizAttempt.builder()
                      .submissionId(submissionId)
                      .assessmentId(assessmentId)
                      .studentId(studentId)
                      .attemptNumber(1)
                      .status(SubmissionStatus.SUBMITTED)
                      .startedAt(now.minusSeconds(4000))
                      .build(),
                  QuizAttempt.builder()
                      .submissionId(submissionId)
                      .assessmentId(assessmentId)
                      .studentId(studentId)
                      .attemptNumber(2)
                      .status(SubmissionStatus.SUBMITTED)
                      .startedAt(now.minusSeconds(2000))
                      .build()));
      when(quizAttemptRepository.countBySubmissionId(submissionId)).thenReturn(2);
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      var res = studentAssessmentService.getAssessmentDetails(assessmentId);

      assertFalse(Boolean.TRUE.equals(res.getCanStart()));
      assertEquals("Maximum attempts reached", res.getCannotStartReason());
    }

    @Test
    @DisplayName("Normal: studentStatus COMPLETED khi đã quá endDate dù không có submission")
    void it_should_mark_completed_after_end_date_without_submission() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Đề khảo sát đã đóng cổng nộp",
              now.minusSeconds(200000),
              now.minusSeconds(3600),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(5.0);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.empty());
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      var res = studentAssessmentService.getAssessmentDetails(assessmentId);

      assertEquals("COMPLETED", res.getStudentStatus());
      assertFalse(Boolean.TRUE.equals(res.getCanStart()));
      assertEquals("Assessment has expired", res.getCannotStartReason());
    }
  }

  @Nested
  @DisplayName("startAssessment()")
  class StartAssessmentTests {

    private StartAssessmentRequest buildStartRequest(UUID aid) {
      return StartAssessmentRequest.builder().assessmentId(aid).build();
    }

    @Test
    @DisplayName("Abnormal: ASSESSMENT_NOT_AVAILABLE khi chưa đến startDate")
    void it_should_throw_not_available_when_start_date_in_future() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Đề mở sau giờ ra chơi",
              now.plusSeconds(3600),
              now.plusSeconds(72000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));

      AppException ex =
          assertThrows(
              AppException.class,
              () -> studentAssessmentService.startAssessment(buildStartRequest(assessmentId)));

      assertEquals(ErrorCode.ASSESSMENT_NOT_AVAILABLE, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ASSESSMENT_ACCESS_DENIED khi không có quyền")
    void it_should_throw_access_denied_when_starting_unlinked_assessment() {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndStatusAndDeletedAtIsNullOrderByEnrolledAtDesc(
              eq(studentId), eq(EnrollmentStatus.ACTIVE)))
          .thenReturn(List.of());

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  studentAssessmentService.startAssessment(
                      StartAssessmentRequest.builder().assessmentId(assessmentId).build()));

      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ASSESSMENT_NOT_FOUND khi id không tồn tại")
    void it_should_throw_not_found_when_start_after_access_check() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Đề biến mất giữa chừng",
              now.minusSeconds(2000),
              now.plusSeconds(200000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(
              AppException.class,
              () -> studentAssessmentService.startAssessment(buildStartRequest(assessmentId)));

      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ASSESSMENT_EXPIRED khi quá endDate")
    void it_should_throw_expired_when_end_date_passed() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Đề đã hết hạn nộp",
              now.minusSeconds(100000),
              now.minusSeconds(3600),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));

      AppException ex =
          assertThrows(
              AppException.class,
              () -> studentAssessmentService.startAssessment(buildStartRequest(assessmentId)));

      assertEquals(ErrorCode.ASSESSMENT_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: MAX_ATTEMPTS khi không cho nhiều lần và đã có attempt")
    void it_should_throw_max_attempts_when_single_attempt_policy_and_count_positive() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Một lần làm duy nhất",
              now.minusSeconds(5000),
              now.plusSeconds(50000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      Submission submission =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now.minusSeconds(2000))
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.of(submission));
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.countBySubmissionId(submissionId)).thenReturn(1);

      AppException ex =
          assertThrows(
              AppException.class,
              () -> studentAssessmentService.startAssessment(buildStartRequest(assessmentId)));

      assertEquals(ErrorCode.MAX_ATTEMPTS_REACHED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Normal: trả về attempt hiện có khi đang IN_PROGRESS")
    void it_should_return_existing_attempt_when_already_in_progress() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Tiếp tục làm bài đang dở",
              now.minusSeconds(8000),
              now.plusSeconds(80000),
              false,
              true,
              3,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      Submission submission =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now.minusSeconds(1000))
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.of(submission));
      QuizAttempt existing =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now.minusSeconds(400))
              .build();
      existing.setId(attemptId);
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of(existing));
      Question q = buildQuestion(questionId);
      stubCommonAssessmentQuestions(published, q);
      when(centrifugoService.generateConnectionToken(studentId, attemptId))
          .thenReturn("token-học-sinh");
      when(centrifugoService.getAttemptChannel(attemptId)).thenReturn("attempt:channel");

      AttemptStartResponse res =
          studentAssessmentService.startAssessment(buildStartRequest(assessmentId));

      assertEquals(attemptId, res.getAttemptId());
      assertNotNull(res.getQuestions());
      verify(quizAttemptRepository, never()).save(any());
      verify(draftService, never()).initDraft(any(), any(), any());
    }

    @Test
    @DisplayName("Normal: tạo attempt mới, randomize câu hỏi khi cấu hình bật")
    void it_should_create_new_attempt_and_shuffle_when_randomize_enabled() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Đề trắc nghiệm ngẫu nhiên thứ tự câu",
              now.minusSeconds(9000),
              now.plusSeconds(90000),
              true,
              true,
              5,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.empty());
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(15.5);
      when(submissionRepository.save(any(Submission.class)))
          .thenAnswer(
              inv -> {
                Submission s = inv.getArgument(0);
                s.setId(submissionId);
                return s;
              });
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.countBySubmissionId(submissionId)).thenReturn(0);
      when(quizAttemptRepository.save(any(QuizAttempt.class)))
          .thenAnswer(
              inv -> {
                QuizAttempt a = inv.getArgument(0);
                a.setId(attemptId);
                return a;
              });
      Question q = buildQuestion(questionId);
      AssessmentQuestion aq1 = buildAssessmentQuestion(assessmentId, q, 1, new BigDecimal("1.50"));
      Question q2 = buildQuestion(UUID.randomUUID());
      AssessmentQuestion aq2 = buildAssessmentQuestion(assessmentId, q2, 2, null);
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(new ArrayList<>(List.of(aq1, aq2)));
      when(centrifugoService.generateConnectionToken(studentId, attemptId))
          .thenReturn("centrifugo-jwt");
      when(centrifugoService.getAttemptChannel(attemptId)).thenReturn("quiz:attempt");

      AttemptStartResponse res =
          studentAssessmentService.startAssessment(buildStartRequest(assessmentId));

      assertEquals(2L, res.getTotalQuestions());
      assertNotNull(res.getExpiresAt());
      verify(draftService, times(1))
          .initDraft(eq(attemptId), eq(assessmentId), eq(published.getTimeLimitMinutes()));
    }

    @Test
    @DisplayName("Normal: tạo submission sau xung đột DataIntegrity (concurrent create)")
    void it_should_recover_submission_after_data_integrity_conflict() {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Hai tab cùng mở đề",
              now.minusSeconds(3000),
              now.plusSeconds(60000),
              false,
              true,
              4,
              AttemptScoringPolicy.BEST);
      published.setTimeLimitMinutes(null);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      Submission recovered =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now)
              .build();
      recovered.setId(submissionId);
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.empty())
          .thenReturn(Optional.of(recovered));
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(submissionRepository.save(any(Submission.class)))
          .thenThrow(new DataIntegrityViolationException("dup"));
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              assessmentId, studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.countBySubmissionId(submissionId)).thenReturn(0);
      when(quizAttemptRepository.save(any(QuizAttempt.class)))
          .thenAnswer(
              inv -> {
                QuizAttempt a = inv.getArgument(0);
                a.setId(attemptId);
                return a;
              });
      Question q = buildQuestion(questionId);
      stubCommonAssessmentQuestions(published, q);
      when(centrifugoService.generateConnectionToken(studentId, attemptId)).thenReturn("tok");
      when(centrifugoService.getAttemptChannel(attemptId)).thenReturn("ch");

      AttemptStartResponse res =
          studentAssessmentService.startAssessment(buildStartRequest(assessmentId));

      assertEquals(attemptId, res.getAttemptId());
      assertNull(res.getExpiresAt());
    }
  }

  @Nested
  @DisplayName("updateAnswer() / updateFlag()")
  class DraftMutationTests {

    private QuizAttempt buildInProgressAttempt() {
      return QuizAttempt.builder()
          .submissionId(submissionId)
          .assessmentId(assessmentId)
          .studentId(studentId)
          .attemptNumber(1)
          .status(SubmissionStatus.IN_PROGRESS)
          .startedAt(Instant.now().minusSeconds(120))
          .build();
    }

    @Test
    @DisplayName("Abnormal: TIME_LIMIT_EXCEEDED khi đã quá thời gian làm bài")
    void it_should_throw_when_attempt_time_expired() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt = buildInProgressAttempt();
      attempt.setId(attemptId);
      attempt.setStartedAt(Instant.now().minusSeconds(5000));
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      Assessment timed =
          buildPublishedAssessment(
              assessmentId,
              "Đề giới hạn một phút",
              Instant.now().minusSeconds(10000),
              Instant.now().plusSeconds(100000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      timed.setTimeLimitMinutes(1);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(timed));

      AnswerUpdateRequest req =
          AnswerUpdateRequest.builder()
              .attemptId(attemptId)
              .questionId(questionId)
              .answerValue("A")
              .sequenceNumber(4L)
              .build();

      AppException ex =
          assertThrows(AppException.class, () -> studentAssessmentService.updateAnswer(req));

      assertEquals(ErrorCode.TIME_LIMIT_EXCEEDED, ex.getErrorCode());
      verify(draftService, never()).saveAnswer(any(), any(), any());
    }

    @Test
    @DisplayName("Normal: lưu đáp án và gửi ack qua Centrifugo")
    void it_should_save_answer_and_publish_ack() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt = buildInProgressAttempt();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      Assessment a =
          buildPublishedAssessment(
              assessmentId,
              "Đề không giới hạn phút",
              Instant.now().minusSeconds(5000),
              Instant.now().plusSeconds(50000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      a.setTimeLimitMinutes(null);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(a));
      AnswerUpdateRequest req =
          AnswerUpdateRequest.builder()
              .attemptId(attemptId)
              .questionId(questionId)
              .answerValue(List.of("lựa chọn B"))
              .sequenceNumber(9L)
              .build();

      var ack = studentAssessmentService.updateAnswer(req);

      assertTrue(Boolean.TRUE.equals(ack.getSuccess()));
      assertEquals("ack", ack.getType());
      verify(draftService, times(1)).saveAnswer(attemptId, questionId, req.getAnswerValue());
      verify(centrifugoService, times(1)).publishAnswerAck(attemptId, questionId, 9L);
    }

    @Test
    @DisplayName("Normal: cập nhật cờ câu hỏi")
    void it_should_save_flag_and_publish_flag_ack() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt = buildInProgressAttempt();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      FlagUpdateRequest req =
          FlagUpdateRequest.builder()
              .attemptId(attemptId)
              .questionId(questionId)
              .flagged(true)
              .build();

      var ack = studentAssessmentService.updateFlag(req);

      assertEquals("flag_ack", ack.getType());
      verify(draftService, times(1)).saveFlag(attemptId, questionId, true);
      verify(centrifugoService, times(1)).publishFlagAck(attemptId, questionId, true);
    }

    @Test
    @DisplayName("Abnormal: QUIZ_ATTEMPT_NOT_FOUND khi attempt không tồn tại")
    void it_should_throw_when_quiz_attempt_missing_for_update_answer() {
      authenticateAsStudent(studentId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.empty());
      AnswerUpdateRequest req =
          AnswerUpdateRequest.builder()
              .attemptId(attemptId)
              .questionId(questionId)
              .answerValue("A")
              .sequenceNumber(1L)
              .build();

      AppException ex =
          assertThrows(AppException.class, () -> studentAssessmentService.updateAnswer(req));

      assertEquals(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ATTEMPT_ACCESS_DENIED khi attempt thuộc học sinh khác")
    void it_should_throw_when_attempt_belongs_to_another_student() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now())
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  studentAssessmentService.updateAnswer(
                      AnswerUpdateRequest.builder()
                          .attemptId(attemptId)
                          .questionId(questionId)
                          .sequenceNumber(1L)
                          .build()));

      assertEquals(ErrorCode.ATTEMPT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ATTEMPT_NOT_IN_PROGRESS khi attempt đã nộp")
    void it_should_throw_when_attempt_already_submitted() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.SUBMITTED)
              .startedAt(Instant.now().minusSeconds(100))
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  studentAssessmentService.updateFlag(
                      FlagUpdateRequest.builder()
                          .attemptId(attemptId)
                          .questionId(questionId)
                          .flagged(false)
                          .build()));

      assertEquals(ErrorCode.ATTEMPT_NOT_IN_PROGRESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("Abnormal: ASSESSMENT_NOT_FOUND khi kiểm tra hết giờ nhưng assessment bị xóa")
    void it_should_throw_assessment_not_found_when_missing_during_expiry_check() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt = buildInProgressAttempt();
      attempt.setId(attemptId);
      attempt.setStartedAt(Instant.now().minusSeconds(30));
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

      AnswerUpdateRequest req =
          AnswerUpdateRequest.builder()
              .attemptId(attemptId)
              .questionId(questionId)
              .answerValue("B")
              .sequenceNumber(2L)
              .build();

      AppException ex =
          assertThrows(AppException.class, () -> studentAssessmentService.updateAnswer(req));

      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("submitAssessment()")
  class SubmitAssessmentTests {

    @Test
    @DisplayName("Normal: nộp bài, auto-grade thành công, cập nhật submission BEST score")
    void it_should_submit_flush_draft_and_auto_grade() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now().minusSeconds(200))
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      Submission submission =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .manualAdjustment(null)
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      QuizAttempt submitted =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.SUBMITTED)
              .score(new BigDecimal("7.00"))
              .startedAt(attempt.getStartedAt())
              .build();
      submitted.setId(attemptId);
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(List.of(submitted));
      Assessment assess =
          buildPublishedAssessment(
              assessmentId,
              "Đề chấm theo điểm cao nhất",
              Instant.now().minusSeconds(10000),
              Instant.now().plusSeconds(100000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assess));

      studentAssessmentService.submitAssessment(
          SubmitAssessmentRequest.builder().attemptId(attemptId).build());

      verify(draftService, times(1)).flushDraftToDatabase(attemptId);
      verify(quizAttemptRepository, times(1)).save(attempt);
      verify(gradingService, times(1)).autoGradeSubmission(submissionId);
      verify(draftService, times(1)).deleteDraft(attemptId);
      verify(centrifugoService, times(1)).publishSubmitted(attemptId);
      verify(submissionRepository, times(1)).save(submission);
      assertEquals(0, submission.getFinalScore().compareTo(new BigDecimal("7.00")));
    }

    @Test
    @DisplayName("Normal: auto-grade lỗi không làm fail nộp bài")
    void it_should_complete_submission_when_auto_grade_fails() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now().minusSeconds(50))
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      Submission submission =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(
              List.of(
                  QuizAttempt.builder()
                      .submissionId(submissionId)
                      .assessmentId(assessmentId)
                      .studentId(studentId)
                      .attemptNumber(1)
                      .status(SubmissionStatus.SUBMITTED)
                      .score(new BigDecimal("4.00"))
                      .startedAt(attempt.getStartedAt())
                      .build()));
      when(assessmentRepository.findById(assessmentId))
          .thenReturn(
              Optional.of(
                  buildPublishedAssessment(
                      assessmentId,
                      "Đề gây lỗi pipeline chấm",
                      Instant.now().minusSeconds(8000),
                      Instant.now().plusSeconds(80000),
                      false,
                      false,
                      1,
                      AttemptScoringPolicy.BEST)));
      doThrow(new RuntimeException("grading pipeline unavailable"))
          .when(gradingService)
          .autoGradeSubmission(submissionId);

      assertDoesNotThrow(
          () ->
              studentAssessmentService.submitAssessment(
                  SubmitAssessmentRequest.builder().attemptId(attemptId).build()));

      verify(gradingService, times(1)).autoGradeSubmission(submissionId);
      verify(centrifugoService, times(1)).publishSubmitted(attemptId);
    }
  }

  @Nested
  @DisplayName("getDraftSnapshot() / saveAndExit()")
  class DraftReadTests {

    @Test
    @DisplayName("Normal: snapshot parse answers, flags, timeRemaining về 0 khi hết giờ")
    void it_should_build_snapshot_with_parsed_maps_and_zero_remaining() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now().minusSeconds(4000))
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      Map<String, Object> snapshot = new HashMap<>();
      snapshot.put("answers", Map.of(questionId.toString(), "C"));
      snapshot.put("flags", Map.of(questionId.toString(), true));
      snapshot.put("noise", "ignored");
      when(draftService.getDraftSnapshot(attemptId)).thenReturn(snapshot);
      when(draftService.getAnsweredCount(attemptId)).thenReturn(1);
      Assessment assess =
          buildPublishedAssessment(
              assessmentId,
              "Đề một phút để kiểm tra countdown",
              Instant.now().minusSeconds(6000),
              Instant.now().plusSeconds(60000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      assess.setTimeLimitMinutes(1);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assess));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(
              List.of(buildAssessmentQuestion(assessmentId, buildQuestion(questionId), 1, null)));

      var res = studentAssessmentService.getDraftSnapshot(attemptId);

      assertEquals(1, res.getAnsweredCount());
      assertEquals(1, res.getTotalQuestions());
      assertEquals(0, res.getTimeRemainingSeconds());
      assertTrue(res.getFlags().containsKey(questionId));
    }

    @Test
    @DisplayName("Normal: snapshot khi không giới hạn thời gian")
    void it_should_return_null_time_remaining_without_time_limit() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now().minusSeconds(10))
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      when(draftService.getDraftSnapshot(attemptId))
          .thenReturn(Map.of("answers", "not-a-map", "flags", 1));
      when(draftService.getAnsweredCount(attemptId)).thenReturn(0);
      Assessment assess =
          buildPublishedAssessment(
              assessmentId,
              "Đề tự do thời lượng",
              Instant.now().minusSeconds(1000),
              Instant.now().plusSeconds(100000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      assess.setTimeLimitMinutes(null);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assess));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      var res = studentAssessmentService.getDraftSnapshot(attemptId);

      assertNull(res.getTimeRemainingSeconds());
      assertTrue(res.getAnswers().isEmpty());
    }

    @Test
    @DisplayName("Normal: bỏ qua khóa đáp án không phải UUID hợp lệ")
    void it_should_skip_invalid_uuid_keys_in_snapshot_maps() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now().minusSeconds(5))
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      Map<String, Object> rawAnswers = new HashMap<>();
      rawAnswers.put(questionId.toString(), "Đáp án hợp lệ");
      rawAnswers.put("not-a-valid-uuid-key", "skip");
      Map<String, Boolean> rawFlags = new HashMap<>();
      rawFlags.put(questionId.toString(), true);
      rawFlags.put("also-invalid-flag-key", false);
      when(draftService.getDraftSnapshot(attemptId))
          .thenReturn(Map.of("answers", rawAnswers, "flags", rawFlags));
      when(draftService.getAnsweredCount(attemptId)).thenReturn(1);
      Assessment assess =
          buildPublishedAssessment(
              assessmentId,
              "Đề kiểm tra parser snapshot",
              Instant.now().minusSeconds(1000),
              Instant.now().plusSeconds(100000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      assess.setTimeLimitMinutes(null);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assess));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      var res = studentAssessmentService.getDraftSnapshot(attemptId);

      assertEquals(1, res.getAnswers().size());
      assertEquals(1, res.getFlags().size());
    }

    @Test
    @DisplayName("Normal: timeRemaining dương khi còn nhiều thời gian")
    void it_should_return_positive_time_remaining_when_within_limit() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now().minusSeconds(30))
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
      when(draftService.getDraftSnapshot(attemptId)).thenReturn(Map.of());
      when(draftService.getAnsweredCount(attemptId)).thenReturn(0);
      Assessment assess =
          buildPublishedAssessment(
              assessmentId,
              "Đề 60 phút còn gần đủ thời lượng",
              Instant.now().minusSeconds(1000),
              Instant.now().plusSeconds(100000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      assess.setTimeLimitMinutes(60);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assess));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of());

      var res = studentAssessmentService.getDraftSnapshot(attemptId);

      assertNotNull(res.getTimeRemainingSeconds());
      assertTrue(res.getTimeRemainingSeconds() > 3500);
    }

    @Test
    @DisplayName("Normal: saveAndExit chỉ kiểm tra quyền attempt")
    void it_should_validate_attempt_on_save_and_exit() {
      authenticateAsStudent(studentId);
      QuizAttempt attempt =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now())
              .build();
      attempt.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

      assertDoesNotThrow(() -> studentAssessmentService.saveAndExit(attemptId));

      verify(quizAttemptRepository, times(1)).findById(attemptId);
    }
  }

  @Nested
  @DisplayName("Private helpers via reflection")
  class ReflectionCoverageTests {

    @Test
    @DisplayName("Normal: getAccessibleAssessmentIds theo courseId trả về assessment liên kết")
    void it_should_resolve_accessible_ids_for_specific_course() throws Exception {
      authenticateAsStudent(studentId);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId))
          .thenReturn(Optional.of(buildEnrollment(studentId, courseId, EnrollmentStatus.ACTIVE)));
      when(courseAssessmentRepository.findByCourseIdInAndNotDeleted(Set.of(courseId)))
          .thenReturn(List.of(buildCourseAssessment(courseId, assessmentId, 1, true)));

      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "getAccessibleAssessmentIds", UUID.class, UUID.class);
      m.setAccessible(true);

      @SuppressWarnings("unchecked")
      Set<UUID> ids = (Set<UUID>) m.invoke(studentAssessmentService, studentId, courseId);

      assertTrue(ids.contains(assessmentId));
    }

    @Test
    @DisplayName("Abnormal: updateSubmissionStatus khi submission không tồn tại")
    void it_should_throw_when_update_submission_status_missing() throws Exception {
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.empty());
      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "updateSubmissionStatus", UUID.class);
      m.setAccessible(true);

      InvocationTargetException wrapper =
          assertThrows(
              InvocationTargetException.class,
              () -> m.invoke(studentAssessmentService, submissionId));

      assertTrue(wrapper.getCause() instanceof AppException);
      assertEquals(
          ErrorCode.SUBMISSION_NOT_FOUND, ((AppException) wrapper.getCause()).getErrorCode());
    }

    @Test
    @DisplayName("Normal: LATEST scoring policy chọn điểm attempt mới nhất có score")
    void it_should_apply_latest_scoring_policy() throws Exception {
      UUID aId = assessmentId;
      Submission submission =
          Submission.builder()
              .assessmentId(aId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      QuizAttempt first =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(aId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.SUBMITTED)
              .score(new BigDecimal("5.00"))
              .build();
      first.setId(UUID.randomUUID());
      QuizAttempt second =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(aId)
              .studentId(studentId)
              .attemptNumber(2)
              .status(SubmissionStatus.SUBMITTED)
              .score(new BigDecimal("9.50"))
              .build();
      second.setId(UUID.randomUUID());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(List.of(second, first));
      Assessment assess =
          buildPublishedAssessment(
              aId,
              "Đề lấy điểm lần làm gần nhất",
              Instant.now().minusSeconds(5000),
              Instant.now().plusSeconds(50000),
              false,
              true,
              3,
              AttemptScoringPolicy.LATEST);
      when(assessmentRepository.findById(aId)).thenReturn(Optional.of(assess));

      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "updateSubmissionStatus", UUID.class);
      m.setAccessible(true);
      m.invoke(studentAssessmentService, submissionId);

      assertEquals(0, submission.getFinalScore().compareTo(new BigDecimal("9.50")));
    }

    @Test
    @DisplayName("Normal: AVERAGE policy và cộng manualAdjustment")
    void it_should_apply_average_policy_with_manual_adjustment() throws Exception {
      UUID aId = assessmentId;
      Submission submission =
          Submission.builder()
              .assessmentId(aId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .manualAdjustment(new BigDecimal("0.50"))
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      QuizAttempt a1 =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(aId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.SUBMITTED)
              .score(new BigDecimal("6.00"))
              .build();
      a1.setId(UUID.randomUUID());
      QuizAttempt a2 =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(aId)
              .studentId(studentId)
              .attemptNumber(2)
              .status(SubmissionStatus.SUBMITTED)
              .score(new BigDecimal("8.00"))
              .build();
      a2.setId(UUID.randomUUID());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(List.of(a2, a1));
      Assessment assess =
          buildPublishedAssessment(
              aId,
              "Đề lấy trung bình có điều chỉnh tay",
              Instant.now().minusSeconds(4000),
              Instant.now().plusSeconds(40000),
              false,
              true,
              5,
              AttemptScoringPolicy.AVERAGE);
      when(assessmentRepository.findById(aId)).thenReturn(Optional.of(assess));

      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "updateSubmissionStatus", UUID.class);
      m.setAccessible(true);
      m.invoke(studentAssessmentService, submissionId);

      assertEquals(0, submission.getFinalScore().compareTo(new BigDecimal("7.50")));
    }

    @Test
    @DisplayName("Normal: policy null rơi về BEST và không set finalScore khi không có score")
    void it_should_default_policy_and_skip_final_score_without_numeric_scores() throws Exception {
      UUID aId = assessmentId;
      Submission submission =
          Submission.builder()
              .assessmentId(aId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .build();
      submission.setId(submissionId);
      when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));
      QuizAttempt a1 =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(aId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.SUBMITTED)
              .score(null)
              .build();
      a1.setId(UUID.randomUUID());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(submissionId))
          .thenReturn(List.of(a1));
      Assessment assess =
          buildPublishedAssessment(
              aId,
              "Đề chưa có điểm số",
              Instant.now().minusSeconds(3000),
              Instant.now().plusSeconds(30000),
              false,
              false,
              1,
              null);
      when(assessmentRepository.findById(aId)).thenReturn(Optional.of(assess));

      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "updateSubmissionStatus", UUID.class);
      m.setAccessible(true);
      m.invoke(studentAssessmentService, submissionId);

      assertNull(submission.getFinalScore());
      verify(submissionRepository, times(1)).save(submission);
    }

    @Test
    @DisplayName("Reflection: getCannotStartReason trả lời chưa mở, hết hạn, một lần, đang làm")
    void it_should_cover_get_cannot_start_reason_branches_via_reflection() throws Exception {
      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "getCannotStartReason", Assessment.class, Optional.class, Instant.class);
      m.setAccessible(true);
      Instant now = Instant.now();
      Assessment notStarted =
          buildPublishedAssessment(
              assessmentId,
              "Chưa mở cổng làm bài",
              now.plusSeconds(6000),
              now.plusSeconds(120000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      assertEquals(
          "Assessment has not started yet",
          m.invoke(studentAssessmentService, notStarted, Optional.empty(), now));

      Assessment expired =
          buildPublishedAssessment(
              UUID.randomUUID(),
              "Đã đóng hạn nộp",
              now.minusSeconds(100000),
              now.minusSeconds(1000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      assertEquals(
          "Assessment has expired",
          m.invoke(studentAssessmentService, expired, Optional.empty(), now));

      Assessment single =
          buildPublishedAssessment(
              UUID.randomUUID(),
              "Chỉ một lần làm",
              now.minusSeconds(5000),
              now.plusSeconds(50000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      Submission sub =
          Submission.builder()
              .assessmentId(single.getId())
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .build();
      sub.setId(submissionId);
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              single.getId(), studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      when(quizAttemptRepository.countBySubmissionId(submissionId)).thenReturn(1);
      assertEquals(
          "Only one attempt allowed",
          m.invoke(studentAssessmentService, single, Optional.of(sub), now));

      QuizAttempt inProg =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(single.getId())
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now)
              .build();
      inProg.setId(attemptId);
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              single.getId(), studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of(inProg));
      assertNull(m.invoke(studentAssessmentService, single, Optional.of(sub), now));
    }

    @Test
    @DisplayName("Reflection: determineStudentStatus các nhánh UPCOMING và IN_PROGRESS")
    void it_should_cover_determine_student_status_branches_via_reflection() throws Exception {
      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "determineStudentStatus", Assessment.class, Optional.class, Instant.class);
      m.setAccessible(true);
      Instant now = Instant.now();
      Assessment future =
          buildPublishedAssessment(
              assessmentId,
              "Sắp diễn ra tuần sau",
              now.plusSeconds(86400),
              now.plusSeconds(172800),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      assertEquals("UPCOMING", m.invoke(studentAssessmentService, future, Optional.empty(), now));

      Assessment active =
          buildPublishedAssessment(
              UUID.randomUUID(),
              "Đang mở cho lớp",
              now.minusSeconds(1000),
              now.plusSeconds(100000),
              false,
              false,
              1,
              AttemptScoringPolicy.BEST);
      Submission sub =
          Submission.builder()
              .assessmentId(active.getId())
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .build();
      sub.setId(submissionId);
      QuizAttempt inProg =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(active.getId())
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(now.minusSeconds(50))
              .build();
      inProg.setId(attemptId);
      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              active.getId(), studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of(inProg));
      assertEquals(
          "IN_PROGRESS", m.invoke(studentAssessmentService, active, Optional.of(sub), now));

      when(quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
              active.getId(), studentId, SubmissionStatus.IN_PROGRESS))
          .thenReturn(List.of());
      Submission done =
          Submission.builder()
              .assessmentId(active.getId())
              .studentId(studentId)
              .status(SubmissionStatus.SUBMITTED)
              .build();
      done.setId(submissionId);
      assertEquals("COMPLETED", m.invoke(studentAssessmentService, active, Optional.of(done), now));
    }

    @Test
    @DisplayName("Reflection: validateAttemptAccess đủ nhánh lỗi")
    void it_should_cover_validate_attempt_access_via_reflection() throws Exception {
      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "validateAttemptAccess", UUID.class, UUID.class);
      m.setAccessible(true);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.empty());
      InvocationTargetException ex1 =
          assertThrows(
              InvocationTargetException.class,
              () -> m.invoke(studentAssessmentService, attemptId, studentId));
      assertEquals(
          ErrorCode.QUIZ_ATTEMPT_NOT_FOUND, ((AppException) ex1.getCause()).getErrorCode());

      QuizAttempt wrong =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now())
              .build();
      wrong.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(wrong));
      InvocationTargetException ex2 =
          assertThrows(
              InvocationTargetException.class,
              () -> m.invoke(studentAssessmentService, attemptId, studentId));
      assertEquals(ErrorCode.ATTEMPT_ACCESS_DENIED, ((AppException) ex2.getCause()).getErrorCode());

      QuizAttempt done =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.SUBMITTED)
              .startedAt(Instant.now())
              .build();
      done.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(done));
      InvocationTargetException ex3 =
          assertThrows(
              InvocationTargetException.class,
              () -> m.invoke(studentAssessmentService, attemptId, studentId));
      assertEquals(
          ErrorCode.ATTEMPT_NOT_IN_PROGRESS, ((AppException) ex3.getCause()).getErrorCode());

      QuizAttempt ok =
          QuizAttempt.builder()
              .submissionId(submissionId)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now())
              .build();
      ok.setId(attemptId);
      when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(ok));
      assertEquals(ok, m.invoke(studentAssessmentService, attemptId, studentId));
    }

    @Test
    @DisplayName("Reflection: updateSubmissionStatus khi chưa tất cả attempt đã nộp")
    void it_should_not_finalize_submission_when_mixed_attempt_statuses() throws Exception {
      UUID sid = submissionId;
      Submission submission =
          Submission.builder()
              .assessmentId(assessmentId)
              .studentId(studentId)
              .status(SubmissionStatus.IN_PROGRESS)
              .build();
      submission.setId(sid);
      when(submissionRepository.findById(sid)).thenReturn(Optional.of(submission));
      QuizAttempt a1 =
          QuizAttempt.builder()
              .submissionId(sid)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(1)
              .status(SubmissionStatus.SUBMITTED)
              .score(new BigDecimal("5.00"))
              .build();
      a1.setId(UUID.randomUUID());
      QuizAttempt a2 =
          QuizAttempt.builder()
              .submissionId(sid)
              .assessmentId(assessmentId)
              .studentId(studentId)
              .attemptNumber(2)
              .status(SubmissionStatus.IN_PROGRESS)
              .startedAt(Instant.now())
              .build();
      a2.setId(UUID.randomUUID());
      when(quizAttemptRepository.findBySubmissionIdOrderByAttemptNumberDesc(sid))
          .thenReturn(List.of(a2, a1));

      Method m =
          StudentAssessmentServiceImpl.class.getDeclaredMethod(
              "updateSubmissionStatus", UUID.class);
      m.setAccessible(true);
      m.invoke(studentAssessmentService, sid);

      verify(submissionRepository, never()).save(submission);
    }

    @Test
    @DisplayName("Reflection: findOrCreateSubmission khi vẫn không có submission sau lỗi")
    void it_should_propagate_data_integrity_when_retry_find_empty() throws Exception {
      authenticateAsStudent(studentId);
      Instant now = Instant.now();
      Assessment published =
          buildPublishedAssessment(
              assessmentId,
              "Xung đột tạo submission không hồi phục",
              now.minusSeconds(3000),
              now.plusSeconds(60000),
              false,
              true,
              4,
              AttemptScoringPolicy.BEST);
      stubAccessibleCoursePath(published);
      when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(published));
      when(submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId))
          .thenReturn(Optional.empty())
          .thenReturn(Optional.empty());
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(1.0);
      when(submissionRepository.save(any(Submission.class)))
          .thenThrow(new DataIntegrityViolationException("dup"));

      DataIntegrityViolationException ex =
          assertThrows(
              DataIntegrityViolationException.class,
              () ->
                  studentAssessmentService.startAssessment(
                      StartAssessmentRequest.builder().assessmentId(assessmentId).build()));

      assertNotNull(ex);
    }
  }
}
