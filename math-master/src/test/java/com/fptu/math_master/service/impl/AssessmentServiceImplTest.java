package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AddQuestionToAssessmentRequest;
import com.fptu.math_master.dto.request.BatchAddQuestionsRequest;
import com.fptu.math_master.dto.request.CloneAssessmentRequest;
import com.fptu.math_master.dto.request.DistributeAssessmentPointsRequest;
import com.fptu.math_master.dto.request.GenerateAssessmentByPercentageRequest;
import com.fptu.math_master.dto.request.GenerateAssessmentQuestionsRequest;
import com.fptu.math_master.dto.request.AssessmentRequest;
import com.fptu.math_master.dto.request.AutoDistributePointsRequest;
import com.fptu.math_master.dto.request.BatchUpdatePointsRequest;
import com.fptu.math_master.dto.response.AssessmentGenerationResponse;
import com.fptu.math_master.dto.request.PointsOverrideRequest;
import com.fptu.math_master.dto.response.DistributeAssessmentPointsResponse;
import com.fptu.math_master.dto.response.AssessmentResponse;
import com.fptu.math_master.dto.response.AssessmentSummary;
import com.fptu.math_master.dto.response.PercentageBasedGenerationResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.AssessmentMode;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.AttemptScoringPolicy;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentLessonRepository;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.QuestionSelectionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("AssessmentServiceImpl - Tests")
class AssessmentServiceImplTest extends BaseUnitTest {

  @InjectMocks private AssessmentServiceImpl assessmentService;

  @Mock private AssessmentRepository assessmentRepository;
  @Mock private AssessmentLessonRepository assessmentLessonRepository;
  @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
  @Mock private UserRepository userRepository;
  @Mock private ExamMatrixRepository examMatrixRepository;
  @Mock private ExamMatrixBankMappingRepository examMatrixBankMappingRepository;
  @Mock private ExamMatrixRowRepository examMatrixRowRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private QuestionBankRepository questionBankRepository;
  @Mock private QuestionSelectionService questionSelectionService;
  @Mock private QuestionRepository questionRepository;

  private UUID teacherId;
  private UUID assessmentId;
  private UUID matrixId;
  private UUID questionId;
  private Assessment draftAssessment;
  private ExamMatrix approvedMatrix;
  private AssessmentRequest validRequest;

  @BeforeEach
  void setUp() {
    teacherId = UUID.fromString("6a91f5ba-7f10-4f08-8419-c0f329584610");
    assessmentId = UUID.fromString("761f62e2-3bd8-43f1-9680-9cd4d2dcfd16");
    matrixId = UUID.fromString("fb4fb5c6-7cde-4f70-9e2f-6cb413cc3d33");
    questionId = UUID.fromString("94a45098-faea-4da4-b24a-5d2c10d57c9f");

    draftAssessment =
        Assessment.builder()
            .teacherId(teacherId)
            .title("Kiểm tra Chương Hàm Số Bậc Nhất")
            .description("Bài đánh giá sau khi học xong chương hàm số")
            .assessmentType(AssessmentType.QUIZ)
            .assessmentMode(AssessmentMode.MATRIX_BASED)
            .status(AssessmentStatus.DRAFT)
            .examMatrixId(matrixId)
            .timeLimitMinutes(30)
            .passingScore(new BigDecimal("5.00"))
            .randomizeQuestions(true)
            .showCorrectAnswers(false)
            .allowMultipleAttempts(false)
            .maxAttempts(1)
            .attemptScoringPolicy(AttemptScoringPolicy.BEST)
            .showScoreImmediately(true)
            .build();
    draftAssessment.setId(assessmentId);

    approvedMatrix =
        ExamMatrix.builder()
            .teacherId(teacherId)
            .name("Ma trận đề giữa kỳ Toán 10")
            .status(MatrixStatus.APPROVED)
            .gradeLevel(10)
            .build();
    approvedMatrix.setId(matrixId);

    validRequest =
        AssessmentRequest.builder()
            .title("Đánh giá năng lực Đại số")
            .description("Bài kiểm tra đánh giá năng lực sau khi kết thúc chương")
            .assessmentType(AssessmentType.QUIZ)
            .lessonIds(List.of(UUID.fromString("2ce7ab89-836e-4328-b993-74fa03742990")))
            .timeLimitMinutes(45)
            .passingScore(new BigDecimal("7.50"))
            .startDate(Instant.now().plusSeconds(3600))
            .endDate(Instant.now().plusSeconds(7200))
            .randomizeQuestions(null)
            .showCorrectAnswers(null)
            .assessmentMode(AssessmentMode.MATRIX_BASED)
            .examMatrixId(matrixId)
            .allowMultipleAttempts(null)
            .maxAttempts(null)
            .attemptScoringPolicy(null)
            .showScoreImmediately(null)
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsTeacher(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("assessment-token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("scope", "assessment:write")
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.TEACHER_ROLE)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private User buildTeacher(UUID id, String fullName) {
    User teacher = User.builder().fullName(fullName).roles(Set.of()).build();
    teacher.setId(id);
    return teacher;
  }

  private Assessment buildAssessmentWithStatus(AssessmentStatus status) {
    Assessment assessment =
        Assessment.builder()
            .teacherId(teacherId)
            .title("Bài kiểm tra giữa kỳ")
            .assessmentType(AssessmentType.QUIZ)
            .status(status)
            .assessmentMode(AssessmentMode.MATRIX_BASED)
            .examMatrixId(matrixId)
            .showScoreImmediately(true)
            .build();
    assessment.setId(assessmentId);
    return assessment;
  }

  @Nested
  @DisplayName("createAssessment()")
  class CreateAssessmentTests {

    /**
     * Normal case: Tạo assessment thành công khi request hợp lệ và matrix thuộc teacher hiện tại.
     *
     * <p>Input:
     * <ul>
     *   <li>request: có examMatrixId hợp lệ, có lịch mở/đóng hợp lệ</li>
     *   <li>authentication: JWT subject là teacherId và authority ROLE_TEACHER</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateTeacherRole: hasRole(TEACHER) -> TRUE branch</li>
     *   <li>request randomize/showCorrectAnswers/showScoreImmediately = null -> dùng default branch</li>
     *   <li>autoMapQuestionsFromMatrixOnCreate: findMaxOrderIndex != null -> return sớm</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về assessment mới với status DRAFT và attemptScoringPolicy mặc định BEST</li>
     *   <li>{@code assessmentRepository.save()} được gọi đúng 1 lần với teacherId hiện tại</li>
     * </ul>
     */
    @Test
    void it_should_create_assessment_with_defaults_when_request_is_valid() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.save(any(Assessment.class)))
          .thenAnswer(
              invocation -> {
                Assessment saved = invocation.getArgument(0);
                saved.setId(assessmentId);
                return saved;
              });
      when(examMatrixRowRepository.findByExamMatrixId(matrixId)).thenReturn(Collections.emptyList());
      when(questionBankRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(lessonRepository.findByChapterIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(2);
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Nguyen Minh Khoa")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));

      // ===== ACT =====
      AssessmentResponse result = assessmentService.createAssessment(validRequest);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(assessmentId, result.getId()),
          () -> assertEquals(AssessmentStatus.DRAFT, result.getStatus()),
          () -> assertEquals(AttemptScoringPolicy.BEST, result.getAttemptScoringPolicy()),
          () -> assertTrue(result.getShowScoreImmediately()),
          () -> assertEquals("Nguyen Minh Khoa", result.getTeacherName()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(4)).findByIdAndNotDeleted(matrixId);
      verify(assessmentRepository, times(1)).save(any(Assessment.class));
      verify(examMatrixRowRepository, times(1)).findByExamMatrixId(matrixId);
      verify(questionBankRepository, times(1)).findAllById(Collections.emptyList());
      verify(lessonRepository, times(1)).findByChapterIdIn(Collections.emptyList());
      verify(assessmentQuestionRepository, times(1)).findMaxOrderIndex(assessmentId);
      verify(assessmentRepository, times(1)).countQuestionsByAssessmentId(assessmentId);
      verify(assessmentRepository, times(1)).calculateTotalPoints(assessmentId);
      verify(assessmentRepository, times(1)).countSubmissionsByAssessmentId(assessmentId);
      verify(userRepository, times(1)).findById(teacherId);
      verify(assessmentLessonRepository, times(1)).findLessonIdsByAssessmentId(assessmentId);
      verify(lessonRepository, times(2)).findByIdInAndNotDeleted(Collections.emptyList());
      verifyNoMoreInteractions(
          assessmentRepository,
          assessmentLessonRepository,
          assessmentQuestionRepository,
          userRepository,
          examMatrixRepository,
          examMatrixBankMappingRepository,
          examMatrixRowRepository,
          lessonRepository,
          chapterRepository,
          subjectRepository,
          questionBankRepository,
          questionSelectionService,
          questionRepository);
    }

    @Test
    void it_should_set_show_score_immediately_false_when_request_explicitly_disables_it() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      AssessmentRequest request = AssessmentRequest.builder()
          .title("Đề kiểm tra tắt hiển thị điểm ngay")
          .description("Bài test")
          .assessmentType(AssessmentType.QUIZ)
          .lessonIds(List.of(UUID.randomUUID()))
          .timeLimitMinutes(30)
          .examMatrixId(matrixId)
          .showScoreImmediately(false)
          .assessmentMode(AssessmentMode.MATRIX_BASED)
          .build();
      when(assessmentRepository.save(any(Assessment.class)))
          .thenAnswer(
              invocation -> {
                Assessment saved = invocation.getArgument(0);
                saved.setId(assessmentId);
                return saved;
              });
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixRowRepository.findByExamMatrixId(matrixId)).thenReturn(Collections.emptyList());
      when(questionBankRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(lessonRepository.findByChapterIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(1);
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId, "DisableScore")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId)).thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList())).thenReturn(Collections.emptyList());

      // ===== ACT =====
      AssessmentResponse result = assessmentService.createAssessment(request);

      // ===== ASSERT =====
      assertFalse(result.getShowScoreImmediately());
    }
  }

  @Nested
  @DisplayName("publishAssessment()")
  class PublishAssessmentTests {

    /**
     * Abnormal case: Không thể publish khi assessment chưa có câu hỏi.
     *
     * <p>Input:
     * <ul>
     *   <li>assessmentId: assessment ở trạng thái DRAFT, thuộc về teacher hiện tại</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>status != PUBLISHED và !isTerminal -> đi qua guard checks</li>
     *   <li>countQuestionsByAssessmentId == 0 -> throw nhánh lỗi</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NO_QUESTIONS}</li>
     *   <li>{@code assessmentRepository.save()} không được gọi</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_publishing_assessment_without_questions() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> assessmentService.publishAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_NO_QUESTIONS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(assessmentId);
      verify(assessmentRepository, times(1)).countQuestionsByAssessmentId(assessmentId);
      verify(assessmentRepository, never()).save(any(Assessment.class));
      verifyNoMoreInteractions(
          assessmentRepository,
          assessmentLessonRepository,
          assessmentQuestionRepository,
          userRepository,
          examMatrixRepository,
          examMatrixBankMappingRepository,
          examMatrixRowRepository,
          lessonRepository,
          chapterRepository,
          subjectRepository,
          questionBankRepository,
          questionSelectionService,
          questionRepository);
    }
  }

  @Nested
  @DisplayName("unpublishAssessment()")
  class UnpublishAssessmentTests {

    /**
     * Abnormal case: Không thể unpublish assessment đã có bài nộp.
     *
     * <p>Input:
     * <ul>
     *   <li>assessmentId: assessment ở trạng thái PUBLISHED, thuộc teacher hiện tại</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>status == PUBLISHED -> qua nhánh hợp lệ của status check</li>
     *   <li>countSubmissionsByAssessmentId > 0 -> throw nhánh lỗi</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_HAS_SUBMISSIONS}</li>
     *   <li>{@code assessmentRepository.save()} không được gọi</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_unpublishing_assessment_with_submissions() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment published = buildAssessmentWithStatus(AssessmentStatus.PUBLISHED);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(3L);

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> assessmentService.unpublishAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_HAS_SUBMISSIONS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(assessmentId);
      verify(assessmentRepository, times(1)).countSubmissionsByAssessmentId(assessmentId);
      verify(assessmentRepository, never()).save(any(Assessment.class));
      verify(examMatrixRepository, never()).save(any(ExamMatrix.class));
      verifyNoMoreInteractions(
          assessmentRepository,
          assessmentLessonRepository,
          assessmentQuestionRepository,
          userRepository,
          examMatrixRepository,
          examMatrixBankMappingRepository,
          examMatrixRowRepository,
          lessonRepository,
          chapterRepository,
          subjectRepository,
          questionBankRepository,
          questionSelectionService,
          questionRepository);
    }
  }

  @Nested
  @DisplayName("addQuestion()")
  class AddQuestionTests {

    /**
     * Normal case: Thêm câu hỏi thành công khi assessment là DRAFT không dùng exam matrix.
     *
     * <p>Input:
     * <ul>
     *   <li>assessmentId: assessment DRAFT, examMatrixId = null</li>
     *   <li>request: questionId hợp lệ và không trùng trong assessment</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>request.orderIndex == null -> dùng nhánh auto nextOrder từ maxOrder</li>
     *   <li>duplicate check = FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Question mới được lưu với orderIndex = maxOrder + 1</li>
     *   <li>Trả về response của assessment vừa cập nhật</li>
     * </ul>
     */
    @Test
    void it_should_add_question_with_next_order_when_assessment_is_draft_and_non_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draftNonMatrix = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draftNonMatrix.setExamMatrixId(null);
      AddQuestionToAssessmentRequest request =
          AddQuestionToAssessmentRequest.builder()
              .questionId(questionId)
              .orderIndex(null)
              .pointsOverride(new BigDecimal("2.50"))
              .build();

      Question question =
          Question.builder()
              .questionText("Tính giá trị của biểu thức 2x + 3 khi x = 4")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .points(new BigDecimal("1.00"))
              .build();
      question.setId(questionId);

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draftNonMatrix));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.empty());
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(4);
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(2.5);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Tran Gia Huy")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      AssessmentResponse result = assessmentService.addQuestion(assessmentId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(assessmentId, result.getId()),
          () -> assertEquals("Tran Gia Huy", result.getTeacherName()));

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(assessmentId);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(questionId);
      verify(assessmentQuestionRepository, times(1))
          .findByAssessmentIdAndQuestionId(assessmentId, questionId);
      verify(assessmentQuestionRepository, times(1)).findMaxOrderIndex(assessmentId);
      verify(assessmentQuestionRepository, times(1)).save(any());
      verify(assessmentRepository, times(1)).countQuestionsByAssessmentId(assessmentId);
      verify(assessmentRepository, times(1)).calculateTotalPoints(assessmentId);
      verify(assessmentRepository, times(1)).countSubmissionsByAssessmentId(assessmentId);
      verify(userRepository, times(1)).findById(teacherId);
      verify(assessmentLessonRepository, times(1)).findLessonIdsByAssessmentId(assessmentId);
      verify(lessonRepository, times(2)).findByIdInAndNotDeleted(Collections.emptyList());
      verifyNoMoreInteractions(
          assessmentRepository,
          assessmentLessonRepository,
          assessmentQuestionRepository,
          userRepository,
          examMatrixRepository,
          examMatrixBankMappingRepository,
          examMatrixRowRepository,
          lessonRepository,
          chapterRepository,
          subjectRepository,
          questionBankRepository,
          questionSelectionService,
          questionRepository);
    }

    /**
     * Abnormal case: Không thể thêm câu hỏi trùng trong cùng assessment.
     *
     * <p>Input:
     * <ul>
     *   <li>assessmentId: assessment DRAFT, examMatrixId = null</li>
     *   <li>request: questionId đã tồn tại trong assessment</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>duplicate check = TRUE branch -> throw exception</li>
     *   <li>FALSE branch của duplicate check được cover bởi
     *       {@code it_should_add_question_with_next_order_when_assessment_is_draft_and_non_matrix}</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code QUESTION_ALREADY_IN_ASSESSMENT}</li>
     *   <li>{@code assessmentQuestionRepository.save()} không được gọi</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_question_already_exists_in_assessment() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draftNonMatrix = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draftNonMatrix.setExamMatrixId(null);
      AddQuestionToAssessmentRequest request =
          AddQuestionToAssessmentRequest.builder()
              .questionId(questionId)
              .pointsOverride(new BigDecimal("1.00"))
              .build();

      Question question =
          Question.builder()
              .questionText("Giải bất phương trình x + 5 > 8")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .points(new BigDecimal("1.00"))
              .build();
      question.setId(questionId);

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draftNonMatrix));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.of(new com.fptu.math_master.entity.AssessmentQuestion()));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> assessmentService.addQuestion(assessmentId, request));
      assertEquals(ErrorCode.QUESTION_ALREADY_IN_ASSESSMENT, exception.getErrorCode());

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(assessmentId);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(questionId);
      verify(assessmentQuestionRepository, times(1))
          .findByAssessmentIdAndQuestionId(assessmentId, questionId);
      verify(assessmentQuestionRepository, never()).save(any());
      verify(assessmentRepository, never()).countQuestionsByAssessmentId(assessmentId);
      verifyNoMoreInteractions(
          assessmentRepository,
          assessmentLessonRepository,
          assessmentQuestionRepository,
          userRepository,
          examMatrixRepository,
          examMatrixBankMappingRepository,
          examMatrixRowRepository,
          lessonRepository,
          chapterRepository,
          subjectRepository,
          questionBankRepository,
          questionSelectionService,
          questionRepository);
    }
  }

  @Nested
  @DisplayName("private helper branches")
  class HelperBehaviorTests {

    /**
     * Abnormal case: User có authority ROLE_STUDENT và DB roles cũng không có TEACHER/ADMIN.
     *
     * <p>Input:
     * <ul>
     *   <li>authentication: JWT subject hợp lệ nhưng chỉ có role STUDENT</li>
     *   <li>userRepository.findByIdWithRoles(): trả về user chỉ có role STUDENT</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>hasRole(TEACHER) = FALSE và hasRole(ADMIN) = FALSE -> vào nhánh query DB role</li>
     *   <li>isTeacher = FALSE -> throw NOT_A_TEACHER</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code NOT_A_TEACHER}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_creating_assessment_as_non_teacher_user() {
      // ===== ARRANGE =====
      Jwt jwt =
          Jwt.withTokenValue("student-token")
              .header("alg", "none")
              .subject(teacherId.toString())
              .claim("scope", "assessment:write")
              .build();
      SecurityContextHolder.getContext()
          .setAuthentication(
              new JwtAuthenticationToken(
                  jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.STUDENT_ROLE))));

      Role studentRole = Role.builder().name(PredefinedRole.STUDENT_ROLE).build();
      User student = User.builder().fullName("Le Minh Duc").roles(Set.of(studentRole)).build();
      student.setId(teacherId);
      when(userRepository.findByIdWithRoles(teacherId)).thenReturn(Optional.of(student));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> assessmentService.createAssessment(validRequest));
      assertEquals(ErrorCode.NOT_A_TEACHER, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findByIdWithRoles(teacherId);
      verify(assessmentRepository, never()).save(any(Assessment.class));
      verifyNoMoreInteractions(
          assessmentRepository,
          assessmentLessonRepository,
          assessmentQuestionRepository,
          userRepository,
          examMatrixRepository,
          examMatrixBankMappingRepository,
          examMatrixRowRepository,
          lessonRepository,
          chapterRepository,
          subjectRepository,
          questionBankRepository,
          questionSelectionService,
          questionRepository);
    }
  }

  @Nested
  @DisplayName("access and flags")
  class AccessAndFlagTests {

    @Test
    void it_should_return_assessment_when_get_by_id_as_owner() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(5.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Pham Duc Anh")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));

      // ===== ACT =====
      AssessmentResponse result = assessmentService.getAssessmentById(assessmentId);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(assessmentId, result.getId());

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(assessmentId);
      verify(assessmentRepository, times(1)).countQuestionsByAssessmentId(assessmentId);
      verify(assessmentRepository, times(1)).calculateTotalPoints(assessmentId);
      verify(assessmentRepository, times(1)).countSubmissionsByAssessmentId(assessmentId);
    }

    @Test
    void it_should_allow_published_assessment_for_non_owner_user() {
      // ===== ARRANGE =====
      UUID otherUserId = UUID.fromString("78492a63-e005-493e-a151-1e55f5548d67");
      authenticateAsTeacher(otherUserId);
      Assessment published = buildAssessmentWithStatus(AssessmentStatus.PUBLISHED);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(1.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Do Ha Linh")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));

      // ===== ACT =====
      AssessmentResponse result = assessmentService.getAssessmentById(assessmentId);

      // ===== ASSERT =====
      assertEquals(assessmentId, result.getId());
    }

    @Test
    void it_should_deny_get_assessment_when_non_owner_accesses_unpublished_assessment() {
      // ===== ARRANGE =====
      UUID otherUserId = UUID.fromString("8441884f-f675-4309-a50a-cd62270856f1");
      authenticateAsTeacher(otherUserId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> assessmentService.getAssessmentById(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_return_true_when_can_edit_draft_owned_assessment() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));

      // ===== ACT =====
      boolean canEdit = assessmentService.canEditAssessment(assessmentId);

      // ===== ASSERT =====
      assertTrue(canEdit);
    }

    @Test
    void it_should_return_false_when_can_delete_draft_with_submissions() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(2L);

      // ===== ACT =====
      boolean canDelete = assessmentService.canDeleteAssessment(assessmentId);

      // ===== ASSERT =====
      assertFalse(canDelete);
    }

    @Test
    void it_should_return_true_when_can_publish_draft_with_questions() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(4L);

      // ===== ACT =====
      boolean canPublish = assessmentService.canPublishAssessment(assessmentId);

      // ===== ASSERT =====
      assertTrue(canPublish);
    }
  }

  @Nested
  @DisplayName("list and search methods")
  class ListingTests {

    @Test
    void it_should_return_paged_assessments_with_bulk_summary_when_get_my_assessments() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment assessment = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      Page<Assessment> page = new PageImpl<>(List.of(assessment), PageRequest.of(0, 10), 1);
      Pageable pageable = PageRequest.of(0, 10);
      Object[] summaryRow = new Object[] {assessmentId, 3L, 7.5, 1L};

      when(assessmentRepository.findWithFilters(teacherId, null, "Đại số", pageable)).thenReturn(page);
      when(assessmentRepository.findBulkSummaryByIds(List.of(assessmentId)))
          .thenReturn(Collections.singletonList(summaryRow));
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Vu Quoc Bao")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));

      // ===== ACT =====
      Page<AssessmentResponse> result = assessmentService.getMyAssessments(null, "  Đại số  ", pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getTotalElements());
      assertEquals(new BigDecimal("7.50"), result.getContent().get(0).getTotalPoints());
      assertEquals(3L, result.getContent().get(0).getTotalQuestions());
    }

    @Test
    void it_should_trim_keyword_when_searching_assessments_by_name() {
      // ===== ARRANGE =====
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByTitleContainingAndStatusAndNotDeleted(
              "hàm số", AssessmentStatus.DRAFT))
          .thenReturn(List.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Le Ngoc Han")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));

      // ===== ACT =====
      List<AssessmentResponse> result =
          assessmentService.searchAssessmentsByName("  hàm số  ", AssessmentStatus.DRAFT);

      // ===== ASSERT =====
      assertEquals(1, result.size());
      assertEquals(assessmentId, result.get(0).getId());
    }
  }

  @Nested
  @DisplayName("distribution methods")
  class DistributionTests {

    @Test
    void it_should_return_zero_update_when_distributing_points_for_empty_assessment() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      DistributeAssessmentPointsRequest request =
          new DistributeAssessmentPointsRequest(
              new BigDecimal("10.00"), DistributeAssessmentPointsRequest.Strategy.EQUAL, 2);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      DistributeAssessmentPointsResponse result =
          assessmentService.distributeQuestionPoints(assessmentId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0, result.getUpdated()),
          () -> assertEquals(new BigDecimal("0.00"), result.getPointPerQuestion()),
          () -> assertEquals(new BigDecimal("10.00"), result.getTotalPoints()));
    }

    @Test
    void it_should_distribute_points_equally_with_remainder_when_questions_exist() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      DistributeAssessmentPointsRequest request =
          new DistributeAssessmentPointsRequest(
              new BigDecimal("10.00"), DistributeAssessmentPointsRequest.Strategy.EQUAL, 2);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));

      AssessmentQuestion aq1 = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(UUID.randomUUID()).orderIndex(1).build();
      AssessmentQuestion aq2 = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(UUID.randomUUID()).orderIndex(2).build();
      AssessmentQuestion aq3 = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(UUID.randomUUID()).orderIndex(3).build();
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq1, aq2, aq3));

      // ===== ACT =====
      DistributeAssessmentPointsResponse result =
          assessmentService.distributeQuestionPoints(assessmentId, request);

      // ===== ASSERT =====
      assertEquals(3, result.getUpdated());
      assertEquals(new BigDecimal("3.33"), result.getPointPerQuestion());
      assertEquals(new BigDecimal("3.34"), aq1.getPointsOverride());
      assertEquals(new BigDecimal("3.33"), aq2.getPointsOverride());
      assertEquals(new BigDecimal("3.33"), aq3.getPointsOverride());
    }
  }

  @Nested
  @DisplayName("lesson linkage and lookup")
  class LessonLookupTests {

    @Test
    void it_should_return_empty_when_no_assessment_linked_to_lesson() {
      // ===== ARRANGE =====
      UUID lessonId = UUID.fromString("6b2e5cb7-2c64-40f5-84d1-3980b7d30a35");
      when(assessmentLessonRepository.findAssessmentIdsByLessonId(lessonId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      List<AssessmentResponse> result = assessmentService.getAssessmentsByLessonId(lessonId);

      // ===== ASSERT =====
      assertTrue(result.isEmpty());
    }

    @Test
    void it_should_return_mapped_assessments_when_lesson_has_links() {
      // ===== ARRANGE =====
      UUID lessonId = UUID.fromString("6b2e5cb7-2c64-40f5-84d1-3980b7d30a35");
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentLessonRepository.findAssessmentIdsByLessonId(lessonId))
          .thenReturn(List.of(assessmentId));
      when(assessmentRepository.findByIdInAndNotDeleted(List.of(assessmentId))).thenReturn(List.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(4.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Bui Quang Minh")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));

      // ===== ACT =====
      List<AssessmentResponse> result = assessmentService.getAssessmentsByLessonId(lessonId);

      // ===== ASSERT =====
      assertEquals(1, result.size());
      assertEquals(assessmentId, result.get(0).getId());
    }
  }

  @Nested
  @DisplayName("generation and clone flows")
  class GenerationAndCloneTests {

    @Test
    void it_should_generate_questions_from_matrix_when_assessment_is_draft() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      GenerateAssessmentQuestionsRequest request =
          GenerateAssessmentQuestionsRequest.builder().examMatrixId(matrixId).build();

      AssessmentQuestion generated =
          AssessmentQuestion.builder()
              .assessmentId(assessmentId)
              .questionId(UUID.randomUUID())
              .orderIndex(1)
              .pointsOverride(new BigDecimal("2.00"))
              .build();
      QuestionSelectionService.SelectionPlan selectionPlan =
          new QuestionSelectionService.SelectionPlan(List.of(generated), 2);

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(
              List.of(
                  com.fptu.math_master.entity.ExamMatrixBankMapping.builder()
                      .examMatrixId(matrixId)
                      .matrixRowId(UUID.randomUUID())
                      .questionCount(1)
                      .build()));
      when(examMatrixRowRepository.findByExamMatrixId(matrixId)).thenReturn(Collections.emptyList());
      when(questionBankRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(lessonRepository.findByChapterIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(questionSelectionService.buildSelectionPlan(assessmentId, matrixId, 1)).thenReturn(selectionPlan);

      // ===== ACT =====
      AssessmentGenerationResponse result =
          assessmentService.generateQuestionsFromMatrix(assessmentId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.getTotalQuestionsGenerated()),
          () -> assertEquals(1, result.getQuestionsFromBank()),
          () -> assertEquals(2, result.getTotalPoints()));
    }

    @Test
    void it_should_generate_assessment_from_matrix_with_default_title_when_matrix_name_missing() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      ExamMatrix matrixWithoutName =
          ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.APPROVED).build();
      matrixWithoutName.setId(matrixId);
      GenerateAssessmentQuestionsRequest request =
          GenerateAssessmentQuestionsRequest.builder().examMatrixId(matrixId).build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(matrixWithoutName));
      when(assessmentRepository.save(any(Assessment.class)))
          .thenAnswer(
              invocation -> {
                Assessment saved = invocation.getArgument(0);
                if (saved.getId() == null) {
                  saved.setId(assessmentId);
                }
                return saved;
              });
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(
              List.of(
                  com.fptu.math_master.entity.ExamMatrixBankMapping.builder()
                      .examMatrixId(matrixId)
                      .matrixRowId(UUID.randomUUID())
                      .questionCount(1)
                      .build()));
      when(examMatrixRowRepository.findByExamMatrixId(matrixId)).thenReturn(Collections.emptyList());
      when(questionBankRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(lessonRepository.findByChapterIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(questionSelectionService.buildSelectionPlan(assessmentId, matrixId, 1))
          .thenReturn(new QuestionSelectionService.SelectionPlan(Collections.emptyList(), 0));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Pham Thanh Dat")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      AssessmentResponse result = assessmentService.generateAssessmentFromMatrix(request);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(assessmentId, result.getId());
    }

    @Test
    void it_should_generate_assessment_by_percentage_when_distribution_is_valid() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      GenerateAssessmentByPercentageRequest request =
          GenerateAssessmentByPercentageRequest.builder()
              .examMatrixId(matrixId)
              .totalQuestions(4)
              .assessmentTitle("Đề ôn tập chương trình lớp 10")
              .assessmentDescription("Phân bổ theo mức độ nhận thức")
              .timeLimitMinutes(40)
              .passingScore(6)
              .randomizeQuestions(true)
              .cognitiveLevelPercentages(
                  Map.of(CognitiveLevel.NHAN_BIET, 50.0, CognitiveLevel.THONG_HIEU, 50.0))
              .build();

      Question q1 =
          Question.builder()
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .questionText("Câu hỏi nhận biết 1")
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .points(new BigDecimal("1.00"))
              .build();
      q1.setId(UUID.randomUUID());
      Question q2 =
          Question.builder()
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .questionText("Câu hỏi nhận biết 2")
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .points(new BigDecimal("1.00"))
              .build();
      q2.setId(UUID.randomUUID());
      Question q3 =
          Question.builder()
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .questionText("Câu hỏi thông hiểu 1")
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .points(new BigDecimal("1.00"))
              .build();
      q3.setId(UUID.randomUUID());
      Question q4 =
          Question.builder()
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .questionText("Câu hỏi thông hiểu 2")
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .points(new BigDecimal("1.00"))
              .build();
      q4.setId(UUID.randomUUID());

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(
              List.of(
                  com.fptu.math_master.entity.ExamMatrixBankMapping.builder()
                      .examMatrixId(matrixId)
                      .matrixRowId(UUID.randomUUID())
                      .questionCount(10)
                      .build()));
      when(questionRepository.countApprovedByBanksAndCognitiveLevel(any(), any())).thenReturn(20L);
      when(assessmentRepository.save(any(Assessment.class)))
          .thenAnswer(
              invocation -> {
                Assessment saved = invocation.getArgument(0);
                if (saved.getId() == null) {
                  saved.setId(assessmentId);
                }
                return saved;
              });
      when(questionRepository.findRandomApprovedByBanksAndCognitiveLevel(any(), anyString(), anyInt()))
          .thenAnswer(
              invocation -> {
                String level = invocation.getArgument(1);
                if (CognitiveLevel.NHAN_BIET.name().equals(level)) {
                  return List.of(q1, q2);
                }
                return List.of(q3, q4);
              });

      // ===== ACT =====
      PercentageBasedGenerationResponse result =
          assessmentService.generateAssessmentByPercentage(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertTrue(result.getSuccess()),
          () -> assertEquals(4, result.getTotalQuestionsGenerated()),
          () -> assertEquals(4, result.getTotalPoints()));
    }

    @Test
    void it_should_clone_assessment_and_questions_when_requested_for_non_matrix_source() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID sourceId = UUID.fromString("c75eb5a5-1b95-4649-8e66-6ea68b6e3569");
      Assessment source = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      source.setId(sourceId);
      source.setExamMatrixId(null);
      source.setTitle("Đề gốc học kỳ 1");
      CloneAssessmentRequest request =
          CloneAssessmentRequest.builder().newTitle("Đề sao chép học kỳ 1").cloneQuestions(true).build();

      AssessmentQuestion sourceQuestion =
          AssessmentQuestion.builder()
              .assessmentId(sourceId)
              .questionId(questionId)
              .orderIndex(1)
              .pointsOverride(new BigDecimal("2.00"))
              .build();

      when(assessmentRepository.findByIdAndNotDeleted(sourceId)).thenReturn(Optional.of(source));
      when(assessmentRepository.save(any(Assessment.class)))
          .thenAnswer(
              invocation -> {
                Assessment saved = invocation.getArgument(0);
                if (saved.getId() == null) {
                  saved.setId(assessmentId);
                }
                return saved;
              });
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(sourceId))
          .thenReturn(List.of(sourceQuestion));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(2.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Hoang Bao Vy")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      AssessmentResponse result = assessmentService.cloneAssessment(sourceId, request);

      // ===== ASSERT =====
      assertEquals(assessmentId, result.getId());
      assertEquals(1L, result.getTotalQuestions());
      verify(assessmentQuestionRepository, times(1)).save(any(AssessmentQuestion.class));
    }
  }

  @Nested
  @DisplayName("batch and update operations")
  class BatchOperationTests {

    @Test
    void it_should_batch_add_questions_skipping_duplicates() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID q2 = UUID.fromString("1f5af4af-7032-4ef7-9c7a-4f99589b6ee3");
      Assessment draftNonMatrix = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draftNonMatrix.setExamMatrixId(null);
      BatchAddQuestionsRequest request = new BatchAddQuestionsRequest(List.of(questionId, q2));

      Question question1 =
          Question.builder()
              .questionText("Câu hỏi 1")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .points(new BigDecimal("1.00"))
              .build();
      question1.setId(questionId);
      Question question2 =
          Question.builder()
              .questionText("Câu hỏi 2")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .points(new BigDecimal("1.00"))
              .build();
      question2.setId(q2);
      AssessmentQuestion existing =
          AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).orderIndex(1).build();
      AssessmentQuestion saved =
          AssessmentQuestion.builder().assessmentId(assessmentId).questionId(q2).orderIndex(2).build();

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draftNonMatrix));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question1));
      when(questionRepository.findByIdAndNotDeleted(q2)).thenReturn(Optional.of(question2));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.of(existing));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, q2))
          .thenReturn(Optional.empty());
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(1);
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(existing, saved));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(2.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Nguyen Gia Bao")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(questionRepository.findByIdAndNotDeleted(any()))
          .thenAnswer(
              invocation ->
                  invocation.getArgument(0).equals(questionId)
                      ? Optional.of(question1)
                      : Optional.of(question2));

      // ===== ACT =====
      var result = assessmentService.batchAddQuestions(assessmentId, request);

      // ===== ASSERT =====
      assertEquals(2, result.size());
      verify(assessmentQuestionRepository, times(1)).save(any(AssessmentQuestion.class));
    }
  }

  @Nested
  @DisplayName("remaining core operations")
  class RemainingOperationTests {

    @Test
    void it_should_update_assessment_when_request_is_valid_and_draft() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(assessmentRepository.save(any(Assessment.class))).thenReturn(draft);
      when(examMatrixRowRepository.findByExamMatrixId(matrixId)).thenReturn(Collections.emptyList());
      when(questionBankRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(lessonRepository.findByChapterIdIn(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Nguyen Duc Hieu")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      AssessmentResponse result = assessmentService.updateAssessment(assessmentId, validRequest);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(assessmentId, result.getId());
    }

    @Test
    void it_should_demote_matrix_when_set_points_override_on_approved_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      AssessmentQuestion aq =
          AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).orderIndex(1).build();
      aq.setId(UUID.randomUUID());
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.of(aq));
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(1.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Tran Van Kiet")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      PointsOverrideRequest request =
          PointsOverrideRequest.builder().questionId(questionId).pointsOverride(new BigDecimal("3.00")).build();

      // ===== ACT =====
      AssessmentResponse result = assessmentService.setPointsOverride(assessmentId, request);

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals(MatrixStatus.DRAFT, approvedMatrix.getStatus());
    }

    @Test
    void it_should_return_publish_summary_with_validation_message_when_no_questions() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);

      // ===== ACT =====
      AssessmentSummary summary = assessmentService.getPublishSummary(assessmentId);

      // ===== ASSERT =====
      assertFalse(summary.getCanPublish());
      assertEquals("Assessment must have at least one question", summary.getValidationMessage());
    }

    @Test
    void it_should_unpublish_assessment_and_unlock_locked_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment published = buildAssessmentWithStatus(AssessmentStatus.PUBLISHED);
      ExamMatrix lockedMatrix =
          ExamMatrix.builder().teacherId(teacherId).name("M").status(MatrixStatus.LOCKED).build();
      lockedMatrix.setId(matrixId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(lockedMatrix));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(1.0);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Le Khanh Linh")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      AssessmentResponse result = assessmentService.unpublishAssessment(assessmentId);

      // ===== ASSERT =====
      assertEquals(AssessmentStatus.DRAFT, result.getStatus());
      assertEquals(MatrixStatus.APPROVED, lockedMatrix.getStatus());
    }

    @Test
    void it_should_soft_delete_assessment_and_related_matrix_when_deleting_draft() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      ExamMatrix linkedMatrix =
          ExamMatrix.builder().teacherId(teacherId).name("Linked").status(MatrixStatus.DRAFT).build();
      linkedMatrix.setId(matrixId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(examMatrixRepository.findByAssessmentIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(linkedMatrix));

      // ===== ACT =====
      assessmentService.deleteAssessment(assessmentId);

      // ===== ASSERT =====
      assertNotNull(draft.getDeletedAt());
      assertNotNull(linkedMatrix.getDeletedAt());
    }

    @Test
    void it_should_remove_question_when_assessment_is_draft_non_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draft.setExamMatrixId(null);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.of(AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).orderIndex(1).build()));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Ta Quang Huy")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      AssessmentResponse result = assessmentService.removeQuestion(assessmentId, questionId);

      // ===== ASSERT =====
      assertEquals(assessmentId, result.getId());
      verify(assessmentQuestionRepository, times(1))
          .deleteByAssessmentIdAndQuestionId(assessmentId, questionId);
    }

    @Test
    void it_should_get_assessment_questions_with_points_override_fallback() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      AssessmentQuestion aq =
          AssessmentQuestion.builder()
              .assessmentId(assessmentId)
              .questionId(questionId)
              .orderIndex(1)
              .pointsOverride(null)
              .build();
      Question question =
          Question.builder()
              .questionText("Tính đạo hàm của f(x)=x^2")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .points(new BigDecimal("2.00"))
              .build();
      question.setId(questionId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));

      // ===== ACT =====
      var result = assessmentService.getAssessmentQuestions(assessmentId);

      // ===== ASSERT =====
      assertEquals(1, result.size());
      assertEquals(new BigDecimal("2.00"), result.get(0).getPoints());
    }

    @Test
    void it_should_get_available_questions_with_normalized_filters() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      Question question =
          Question.builder()
              .questionText("Giải phương trình 2x+1=5")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .points(new BigDecimal("1.00"))
              .build();
      question.setId(questionId);
      question.setCreatedBy(teacherId);
      Page<Question> page = new PageImpl<>(List.of(question), PageRequest.of(0, 5), 1);

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(questionRepository.findAvailableByAssessmentId(any(), any(), any(), any(), any())).thenReturn(page);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Nguyen Ngoc Mai")));

      // ===== ACT =====
      var result =
          assessmentService.getAvailableQuestions(assessmentId, "  phương trình  ", "  đại số  ", PageRequest.of(0, 5));

      // ===== ASSERT =====
      assertEquals(1, result.getData().size());
      assertEquals(1L, result.getTotalElements());
    }

    @Test
    void it_should_batch_update_points_and_return_updated_question_list() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      AssessmentQuestion aq =
          AssessmentQuestion.builder()
              .assessmentId(assessmentId)
              .questionId(questionId)
              .orderIndex(1)
              .pointsOverride(new BigDecimal("1.00"))
              .build();
      Question question =
          Question.builder()
              .questionText("Câu hỏi cập nhật điểm")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .points(new BigDecimal("1.00"))
              .build();
      question.setId(questionId);
      BatchUpdatePointsRequest request =
          new BatchUpdatePointsRequest(
              List.of(new BatchUpdatePointsRequest.QuestionPointItem(questionId, new BigDecimal("3.50"))));

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.of(aq));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(question));

      // ===== ACT =====
      var result = assessmentService.batchUpdatePoints(assessmentId, request);

      // ===== ASSERT =====
      assertEquals(1, result.size());
      assertEquals(new BigDecimal("3.50"), aq.getPointsOverride());
    }

    @Test
    void it_should_auto_distribute_points_for_configured_levels() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      AssessmentQuestion aq1 = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).orderIndex(1).build();
      AssessmentQuestion aq2 = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(UUID.randomUUID()).orderIndex(2).build();
      Question q1 = Question.builder().questionType(QuestionType.MULTIPLE_CHOICE).questionText("Q1").cognitiveLevel(CognitiveLevel.NHAN_BIET).points(new BigDecimal("1.0")).build();
      q1.setId(questionId);
      UUID q2Id = aq2.getQuestionId();
      Question q2 = Question.builder().questionType(QuestionType.MULTIPLE_CHOICE).questionText("Q2").cognitiveLevel(CognitiveLevel.THONG_HIEU).points(new BigDecimal("1.0")).build();
      q2.setId(q2Id);
      Map<String, Integer> dist = new HashMap<>();
      dist.put(CognitiveLevel.NHAN_BIET.name(), 50);
      dist.put(CognitiveLevel.THONG_HIEU.name(), 50);
      AutoDistributePointsRequest request = new AutoDistributePointsRequest(new BigDecimal("10.00"), dist);

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq1, aq2));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(q1));
      when(questionRepository.findByIdAndNotDeleted(q2Id)).thenReturn(Optional.of(q2));

      // ===== ACT =====
      var result = assessmentService.autoDistributePoints(assessmentId, request);

      // ===== ASSERT =====
      assertEquals(2, result.size());
      assertEquals(new BigDecimal("5.00"), aq1.getPointsOverride());
      assertEquals(new BigDecimal("5.00"), aq2.getPointsOverride());
    }

    @Test
    void it_should_close_published_assessment_successfully() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      Assessment published = buildAssessmentWithStatus(AssessmentStatus.PUBLISHED);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(published));
      when(assessmentRepository.save(any(Assessment.class))).thenReturn(published);
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(1.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Pham Tuan Anh")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));

      // ===== ACT =====
      AssessmentResponse result = assessmentService.closeAssessment(assessmentId);

      // ===== ASSERT =====
      assertEquals(AssessmentStatus.CLOSED, result.getStatus());
    }
  }

  @Nested
  @DisplayName("error branches coverage")
  class ErrorBranchTests {

    @Test
    void it_should_throw_exception_when_create_assessment_has_invalid_schedule() {
      authenticateAsTeacher(teacherId);
      AssessmentRequest invalid =
          AssessmentRequest.builder()
              .title("Đề sai lịch")
              .assessmentType(AssessmentType.QUIZ)
              .lessonIds(List.of(UUID.randomUUID()))
              .examMatrixId(matrixId)
              .startDate(Instant.now().plusSeconds(3600))
              .endDate(Instant.now())
              .build();
      AppException ex = assertThrows(AppException.class, () -> assessmentService.createAssessment(invalid));
      assertEquals(ErrorCode.ASSESSMENT_INVALID_SCHEDULE, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_update_assessment_is_published() {
      authenticateAsTeacher(teacherId);
      Assessment published = buildAssessmentWithStatus(AssessmentStatus.PUBLISHED);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(published));
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.updateAssessment(assessmentId, validRequest));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_publish_assessment_already_published() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.publishAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_publish_assessment_total_points_is_zero() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.publishAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_ZERO_TOTAL_POINTS, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_unpublish_assessment_is_not_published() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.unpublishAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_NOT_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_delete_assessment_is_not_draft() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.deleteAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_close_assessment_already_closed() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.CLOSED)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.closeAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_CLOSED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_add_question_to_published_assessment() {
      authenticateAsTeacher(teacherId);
      AddQuestionToAssessmentRequest request =
          AddQuestionToAssessmentRequest.builder().questionId(questionId).build();
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.addQuestion(assessmentId, request));
      assertEquals(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_remove_question_not_in_assessment() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draft.setExamMatrixId(null);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.removeQuestion(assessmentId, questionId));
      assertEquals(ErrorCode.QUESTION_NOT_IN_ASSESSMENT, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_generate_questions_from_matrix_has_no_bank_mapping() {
      authenticateAsTeacher(teacherId);
      GenerateAssessmentQuestionsRequest request =
          GenerateAssessmentQuestionsRequest.builder().examMatrixId(matrixId).build();
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      AppException ex =
          assertThrows(
              AppException.class,
              () -> assessmentService.generateQuestionsFromMatrix(assessmentId, request));
      assertEquals(ErrorCode.MATRIX_VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_generate_assessment_by_percentage_has_invalid_total() {
      authenticateAsTeacher(teacherId);
      GenerateAssessmentByPercentageRequest request =
          GenerateAssessmentByPercentageRequest.builder()
              .examMatrixId(matrixId)
              .totalQuestions(5)
              .cognitiveLevelPercentages(
                  Map.of(CognitiveLevel.NHAN_BIET, 30.0, CognitiveLevel.THONG_HIEU, 30.0))
              .build();
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.generateAssessmentByPercentage(request));
      assertEquals(ErrorCode.MATRIX_VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_distribute_question_points_has_null_strategy() {
      authenticateAsTeacher(teacherId);
      DistributeAssessmentPointsRequest request =
          new DistributeAssessmentPointsRequest(new BigDecimal("10.0"), null, 2);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      AppException ex =
          assertThrows(
              AppException.class,
              () -> assessmentService.distributeQuestionPoints(assessmentId, request));
      assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_batch_update_points_on_published_assessment() {
      authenticateAsTeacher(teacherId);
      BatchUpdatePointsRequest request =
          new BatchUpdatePointsRequest(
              List.of(new BatchUpdatePointsRequest.QuestionPointItem(questionId, new BigDecimal("2.0"))));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.batchUpdatePoints(assessmentId, request));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_auto_distribute_points_on_published_assessment() {
      authenticateAsTeacher(teacherId);
      AutoDistributePointsRequest request =
          new AutoDistributePointsRequest(new BigDecimal("10.0"), Map.of(CognitiveLevel.NHAN_BIET.name(), 100));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.autoDistributePoints(assessmentId, request));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("additional branch coverage pack")
  class AdditionalBranchCoverageTests {

    @Test
    void it_should_return_preview_when_owner_requests_assessment_preview() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Preview Owner")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      assertNotNull(assessmentService.getAssessmentPreview(assessmentId));
    }

    @Test
    void it_should_allow_get_assessment_by_id_for_admin_role() {
      UUID adminId = UUID.fromString("62d4c4d9-8e94-4dc9-a6ec-80982f52f009");
      Jwt jwt =
          Jwt.withTokenValue("admin-token")
              .header("alg", "none")
              .subject(adminId.toString())
              .claim("scope", "assessment:read")
              .build();
      SecurityContextHolder.getContext()
          .setAuthentication(
              new JwtAuthenticationToken(
                  jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.ADMIN_ROLE))));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Teacher Name")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      assertNotNull(assessmentService.getAssessmentById(assessmentId));
    }

    @Test
    void it_should_search_all_when_keyword_is_blank() {
      when(assessmentRepository.findByTitleContainingAndStatusAndNotDeleted("", AssessmentStatus.DRAFT))
          .thenReturn(Collections.emptyList());
      assertTrue(assessmentService.searchAssessmentsByName("   ", AssessmentStatus.DRAFT).isEmpty());
    }

    @Test
    void it_should_get_my_assessments_with_null_search_term_branch() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findWithFilters(teacherId, AssessmentStatus.DRAFT, null, PageRequest.of(0, 5)))
          .thenReturn(Page.empty());
      when(assessmentRepository.findBulkSummaryByIds(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Owner")));
      assertEquals(
          0,
          assessmentService.getMyAssessments(
              AssessmentStatus.DRAFT, "   ", PageRequest.of(0, 5)).getTotalElements());
    }

    @Test
    void it_should_clone_with_default_title_when_new_title_blank() {
      authenticateAsTeacher(teacherId);
      UUID sourceId = UUID.fromString("c3a31f88-fa0d-412d-9f83-fde5f54a1f44");
      Assessment source = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      source.setId(sourceId);
      source.setTitle("Đề nguồn chương 2");
      source.setExamMatrixId(UUID.randomUUID());
      when(assessmentRepository.findByIdAndNotDeleted(sourceId)).thenReturn(Optional.of(source));
      when(assessmentRepository.save(any(Assessment.class)))
          .thenAnswer(
              inv -> {
                Assessment a = inv.getArgument(0);
                a.setId(assessmentId);
                return a;
              });
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Clone Owner")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      AssessmentResponse response =
          assessmentService.cloneAssessment(
              sourceId, CloneAssessmentRequest.builder().newTitle(" ").cloneQuestions(true).build());
      assertTrue(response.getTitle().startsWith("Copy of "));
    }

    @Test
    void it_should_throw_exception_when_generate_assessment_from_non_reusable_matrix() {
      authenticateAsTeacher(teacherId);
      ExamMatrix draftMatrix =
          ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.DRAFT).name("draft").build();
      draftMatrix.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.generateAssessmentFromMatrix(
                      GenerateAssessmentQuestionsRequest.builder().examMatrixId(matrixId).build()));
      assertEquals(ErrorCode.EXAM_MATRIX_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_generate_questions_from_matrix_assessment_not_draft() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.generateQuestionsFromMatrix(
                      assessmentId,
                      GenerateAssessmentQuestionsRequest.builder().examMatrixId(matrixId).build()));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_publish_matrix_not_approved() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      ExamMatrix draftMatrix =
          ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.DRAFT).name("draft").build();
      draftMatrix.setId(matrixId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(2.0);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.publishAssessment(assessmentId));
      assertEquals(ErrorCode.MATRIX_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_publish_matrix_cell_not_fully_filled() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      com.fptu.math_master.entity.ExamMatrixBankMapping mapping =
          com.fptu.math_master.entity.ExamMatrixBankMapping.builder()
              .examMatrixId(matrixId)
              .matrixRowId(UUID.randomUUID())
              .questionCount(2)
              .build();
      mapping.setId(UUID.fromString("8e7fcb31-d4ae-4b07-aa57-60534ad637ba"));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(2.0);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mapping));
      when(assessmentQuestionRepository.countByAssessmentIdAndMatrixBankMappingId(assessmentId, mapping.getId()))
          .thenReturn(1L);
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.publishAssessment(assessmentId));
      assertEquals(ErrorCode.MATRIX_CELL_FILL_INCOMPLETE, ex.getErrorCode());
    }

    @Test
    void it_should_publish_when_matrix_cells_are_fully_filled() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      com.fptu.math_master.entity.ExamMatrixBankMapping mapping =
          com.fptu.math_master.entity.ExamMatrixBankMapping.builder()
              .examMatrixId(matrixId)
              .matrixRowId(UUID.randomUUID())
              .questionCount(2)
              .build();
      mapping.setId(UUID.fromString("82f5f591-4b4d-4f94-a406-1f0e598f9b51"));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(2.0);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mapping));
      when(assessmentQuestionRepository.countByAssessmentIdAndMatrixBankMappingId(assessmentId, mapping.getId()))
          .thenReturn(2L);
      when(assessmentRepository.save(any(Assessment.class))).thenReturn(draft);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Publisher")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList()))
          .thenReturn(Collections.emptyList());
      assertEquals(AssessmentStatus.PUBLISHED, assessmentService.publishAssessment(assessmentId).getStatus());
    }

    @Test
    void it_should_return_empty_when_auto_distribute_points_has_no_questions() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(Collections.emptyList());
      assertTrue(
          assessmentService
              .autoDistributePoints(
                  assessmentId, new AutoDistributePointsRequest(new BigDecimal("10"), Map.of()))
              .isEmpty());
    }

    @Test
    void it_should_distribute_remaining_points_to_unmatched_when_percent_not_full() {
      authenticateAsTeacher(teacherId);
      AssessmentQuestion aq = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).orderIndex(1).build();
      Question q =
          Question.builder()
              .questionText("unmatched")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.VAN_DUNG)
              .points(new BigDecimal("1.0"))
              .build();
      q.setId(questionId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(q));
      assessmentService.autoDistributePoints(
          assessmentId,
          new AutoDistributePointsRequest(new BigDecimal("10.00"), Map.of(CognitiveLevel.NHAN_BIET.name(), 30)));
      assertEquals(new BigDecimal("7.00"), aq.getPointsOverride());
    }

    @Test
    void it_should_link_assessment_to_lesson_when_not_linked_before() {
      authenticateAsTeacher(teacherId);
      UUID lessonId = UUID.fromString("f716f2cf-e296-4671-849f-f57f4daef93f");
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(Collections.emptyList());
      assessmentService.linkAssessmentToLesson(assessmentId, lessonId);
      verify(assessmentLessonRepository, times(1)).save(any());
    }

    @Test
    void it_should_skip_link_when_assessment_already_linked_to_lesson() {
      authenticateAsTeacher(teacherId);
      UUID lessonId = UUID.fromString("4f07d2d6-39e7-4f1f-8e80-58fdc6ec5ea4");
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId))
          .thenReturn(List.of(lessonId));
      assessmentService.linkAssessmentToLesson(assessmentId, lessonId);
      verify(assessmentLessonRepository, never()).save(any());
    }

    @Test
    void it_should_unlink_assessment_from_lesson_by_deleting_matching_links() {
      authenticateAsTeacher(teacherId);
      UUID lessonId = UUID.fromString("7bdcce9f-6172-436d-b8ee-1406cd268ecf");
      com.fptu.math_master.entity.AssessmentLesson al =
          com.fptu.math_master.entity.AssessmentLesson.builder()
              .assessmentId(assessmentId)
              .lessonId(lessonId)
              .build();
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentLessonRepository.findByAssessmentIdOrderByCreatedAt(assessmentId))
          .thenReturn(List.of(al));
      assessmentService.unlinkAssessmentFromLesson(assessmentId, lessonId);
      verify(assessmentLessonRepository, times(1)).delete(al);
    }
  }

  @Nested
  @DisplayName("branch-focused gap fillers")
  class BranchGapFillerTests {

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(String name, Class<?>[] types, Object... args) {
      try {
        Method m = AssessmentServiceImpl.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return (T) m.invoke(assessmentService, args);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }

    @Test
    void it_should_return_publish_summary_with_total_points_error_when_points_zero() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(3L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(0.0);
      AssessmentSummary summary = assessmentService.getPublishSummary(assessmentId);
      assertEquals("Total points must be greater than 0", summary.getValidationMessage());
      assertFalse(summary.getCanPublish());
    }

    @Test
    void it_should_return_publish_summary_with_start_date_past_error() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draft.setStartDate(Instant.now().minusSeconds(120));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(3L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(10.0);
      AssessmentSummary summary = assessmentService.getPublishSummary(assessmentId);
      assertEquals("Start date cannot be in the past", summary.getValidationMessage());
      assertFalse(summary.getCanPublish());
    }

    @Test
    void it_should_return_publish_summary_as_publishable_when_all_conditions_valid() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draft.setStartDate(Instant.now().plusSeconds(600));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(3L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(10.0);
      AssessmentSummary summary = assessmentService.getPublishSummary(assessmentId);
      assertTrue(summary.getCanPublish());
      assertEquals("", summary.getValidationMessage());
    }

    @Test
    void it_should_publish_assessment_without_matrix_when_valid() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draft.setExamMatrixId(null);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(2.0);
      when(assessmentRepository.save(any(Assessment.class))).thenReturn(draft);
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId, "NoMatrix")));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(assessmentId)).thenReturn(Collections.emptyList());
      when(lessonRepository.findByIdInAndNotDeleted(Collections.emptyList())).thenReturn(Collections.emptyList());
      assertEquals(AssessmentStatus.PUBLISHED, assessmentService.publishAssessment(assessmentId).getStatus());
    }

    @Test
    void it_should_throw_exception_when_publish_assessment_start_date_is_past() {
      authenticateAsTeacher(teacherId);
      Assessment draft = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      draft.setStartDate(Instant.now().minusSeconds(300));
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(draft));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(assessmentId)).thenReturn(1.0);
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.publishAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_START_DATE_PAST, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_get_preview_by_non_owner() {
      UUID other = UUID.fromString("e4ff4f2c-a5b4-4d29-b025-f14f00817f64");
      authenticateAsTeacher(other);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.getAssessmentPreview(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_get_assessment_questions_contains_deleted_question() {
      authenticateAsTeacher(teacherId);
      AssessmentQuestion aq =
          AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).orderIndex(1).build();
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.empty());
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.getAssessmentQuestions(assessmentId));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_link_assessment_to_lesson_not_owner() {
      UUID other = UUID.fromString("ab4b95f6-5f15-4a8b-a68c-c35f16f7634c");
      authenticateAsTeacher(other);
      UUID lessonId = UUID.randomUUID();
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.linkAssessmentToLesson(assessmentId, lessonId));
      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_unlink_assessment_from_lesson_not_owner() {
      UUID other = UUID.fromString("be6222a8-cf65-4b13-8f6a-7d34e6997ab0");
      authenticateAsTeacher(other);
      UUID lessonId = UUID.randomUUID();
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.unlinkAssessmentFromLesson(assessmentId, lessonId));
      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_return_false_when_can_publish_has_no_questions() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(0L);
      assertFalse(assessmentService.canPublishAssessment(assessmentId));
    }

    @Test
    void it_should_return_false_when_can_edit_not_owner_and_not_admin() {
      UUID other = UUID.fromString("61bc7af0-7913-4292-98d2-ad112f2f423a");
      authenticateAsTeacher(other);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      assertFalse(assessmentService.canEditAssessment(assessmentId));
    }

    @Test
    void it_should_cover_private_is_matrix_reusable_for_generation_branches() {
      ExamMatrix approved = ExamMatrix.builder().status(MatrixStatus.APPROVED).build();
      ExamMatrix locked = ExamMatrix.builder().status(MatrixStatus.LOCKED).build();
      ExamMatrix draft = ExamMatrix.builder().status(MatrixStatus.DRAFT).build();
      assertTrue(
          Boolean.TRUE.equals(
              invokePrivate("isMatrixReusableForGeneration", new Class<?>[] {ExamMatrix.class}, approved)));
      assertTrue(
          Boolean.TRUE.equals(
              invokePrivate("isMatrixReusableForGeneration", new Class<?>[] {ExamMatrix.class}, locked)));
      assertFalse(
          Boolean.TRUE.equals(
              invokePrivate("isMatrixReusableForGeneration", new Class<?>[] {ExamMatrix.class}, draft)));
    }

    @Test
    void it_should_cover_private_normalize_keyword_pattern_branches() {
      assertNull(invokePrivate("normalizeKeywordPattern", new Class<?>[] {String.class}, (Object) null));
      assertNull(invokePrivate("normalizeKeywordPattern", new Class<?>[] {String.class}, "   "));
      assertEquals("%abc%", invokePrivate("normalizeKeywordPattern", new Class<?>[] {String.class}, " abc "));
    }

    @Test
    void it_should_cover_private_safe_total_points_branches() {
      assertEquals(BigDecimal.ZERO, invokePrivate("safeTotalPoints", new Class<?>[] {Double.class}, (Object) null));
      assertEquals(
          new BigDecimal("7.25"),
          invokePrivate("safeTotalPoints", new Class<?>[] {Double.class}, Double.valueOf(7.25)));
    }

    @Test
    void it_should_throw_exception_when_add_question_in_matrix_based_assessment() {
      authenticateAsTeacher(teacherId);
      Assessment matrixAssessment = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      matrixAssessment.setExamMatrixId(matrixId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(matrixAssessment));
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.addQuestion(
                      assessmentId, AddQuestionToAssessmentRequest.builder().questionId(questionId).build()));
      assertEquals(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_add_question_not_found() {
      authenticateAsTeacher(teacherId);
      Assessment nonMatrix = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      nonMatrix.setExamMatrixId(null);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(nonMatrix));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.addQuestion(
                      assessmentId, AddQuestionToAssessmentRequest.builder().questionId(questionId).build()));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_remove_question_for_matrix_assessment() {
      authenticateAsTeacher(teacherId);
      Assessment matrixAssessment = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      matrixAssessment.setExamMatrixId(matrixId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(matrixAssessment));
      AppException ex =
          assertThrows(
              AppException.class, () -> assessmentService.removeQuestion(assessmentId, questionId));
      assertEquals(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_batch_add_question_not_found() {
      authenticateAsTeacher(teacherId);
      Assessment nonMatrix = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      nonMatrix.setExamMatrixId(null);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(nonMatrix));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.batchAddQuestions(
                      assessmentId, new BatchAddQuestionsRequest(List.of(questionId))));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_batch_update_points_question_missing() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.empty());
      BatchUpdatePointsRequest request =
          new BatchUpdatePointsRequest(
              List.of(new BatchUpdatePointsRequest.QuestionPointItem(questionId, new BigDecimal("1.5"))));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.batchUpdatePoints(assessmentId, request));
      assertEquals(ErrorCode.ASSESSMENT_QUESTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_distribute_points_on_published_assessment() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.distributeQuestionPoints(
                      assessmentId,
                      new DistributeAssessmentPointsRequest(
                          new BigDecimal("10"), DistributeAssessmentPointsRequest.Strategy.EQUAL, 2)));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_unpublish_assessment_is_closed() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.CLOSED)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.unpublishAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_IS_CLOSED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_delete_assessment_has_submissions() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(4L);
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.deleteAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_HAS_SUBMISSIONS, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_link_assessment_not_found() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class,
              () -> assessmentService.linkAssessmentToLesson(assessmentId, UUID.randomUUID()));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_unlink_assessment_not_found() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class,
              () -> assessmentService.unlinkAssessmentFromLesson(assessmentId, UUID.randomUUID()));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_cover_private_validate_lesson_selection_matrix_branches() {
      List<UUID> lessons = List.of(UUID.randomUUID(), UUID.randomUUID());
      when(lessonRepository.findExistingIdsByIds(lessons)).thenReturn(List.of(lessons.get(0)));
      RuntimeException ex1 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateLessonSelectionForMatrix",
                      new Class<?>[] {UUID.class, List.class},
                      matrixId,
                      lessons));
      assertTrue(ex1.getCause() instanceof java.lang.reflect.InvocationTargetException);

      when(lessonRepository.findExistingIdsByIds(lessons)).thenReturn(lessons);
      when(examMatrixRowRepository.findDistinctLessonIdsByExamMatrixId(matrixId))
          .thenReturn(Collections.emptyList());
      invokePrivate(
          "validateLessonSelectionForMatrix",
          new Class<?>[] {UUID.class, List.class},
          matrixId,
          lessons);

      when(examMatrixRowRepository.findDistinctLessonIdsByExamMatrixId(matrixId))
          .thenReturn(List.of(lessons.get(0)));
      RuntimeException ex2 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateLessonSelectionForMatrix",
                      new Class<?>[] {UUID.class, List.class},
                      matrixId,
                      lessons));
      assertTrue(ex2.getCause() instanceof java.lang.reflect.InvocationTargetException);
    }

    @Test
    void it_should_cover_private_auto_map_questions_from_matrix_on_create_early_returns() {
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(1);
      invokePrivate(
          "autoMapQuestionsFromMatrixOnCreate",
          new Class<?>[] {UUID.class, UUID.class},
          assessmentId,
          matrixId);

      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(null);
      ExamMatrix draft = ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.DRAFT).build();
      draft.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draft));
      invokePrivate(
          "autoMapQuestionsFromMatrixOnCreate",
          new Class<?>[] {UUID.class, UUID.class},
          assessmentId,
          matrixId);

      ExamMatrix approved = ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.APPROVED).build();
      approved.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approved));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      invokePrivate(
          "autoMapQuestionsFromMatrixOnCreate",
          new Class<?>[] {UUID.class, UUID.class},
          assessmentId,
          matrixId);
    }

    @Test
    void it_should_cover_private_validate_published_matrix_cell_coverage_branches() {
      Assessment assessment = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      assessment.setExamMatrixId(matrixId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.empty());
      RuntimeException ex1 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validatePublishedMatrixCellCoverage",
                      new Class<?>[] {Assessment.class},
                      assessment));
      assertTrue(ex1.getCause() instanceof java.lang.reflect.InvocationTargetException);

      ExamMatrix draftMatrix =
          ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.DRAFT).name("draft").build();
      draftMatrix.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      RuntimeException ex2 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validatePublishedMatrixCellCoverage",
                      new Class<?>[] {Assessment.class},
                      assessment));
      assertTrue(ex2.getCause() instanceof java.lang.reflect.InvocationTargetException);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      RuntimeException ex3 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validatePublishedMatrixCellCoverage",
                      new Class<?>[] {Assessment.class},
                      assessment));
      assertTrue(ex3.getCause() instanceof java.lang.reflect.InvocationTargetException);

      com.fptu.math_master.entity.ExamMatrixBankMapping mapping =
          com.fptu.math_master.entity.ExamMatrixBankMapping.builder()
              .examMatrixId(matrixId)
              .matrixRowId(UUID.randomUUID())
              .questionCount(2)
              .build();
      mapping.setId(UUID.randomUUID());
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mapping));
      when(assessmentQuestionRepository.countByAssessmentIdAndMatrixBankMappingId(assessmentId, mapping.getId()))
          .thenReturn(1L);
      RuntimeException ex4 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validatePublishedMatrixCellCoverage",
                      new Class<?>[] {Assessment.class},
                      assessment));
      assertTrue(ex4.getCause() instanceof java.lang.reflect.InvocationTargetException);

      when(assessmentQuestionRepository.countByAssessmentIdAndMatrixBankMappingId(assessmentId, mapping.getId()))
          .thenReturn(2L);
      invokePrivate(
          "validatePublishedMatrixCellCoverage", new Class<?>[] {Assessment.class}, assessment);
    }

    @Test
    void it_should_cover_private_validate_and_get_accessible_matrix_branches() {
      UUID otherTeacher = UUID.fromString("a36539be-7676-4af0-b5d5-962ea8ac3494");
      authenticateAsTeacher(teacherId);
      ExamMatrix foreign =
          ExamMatrix.builder().teacherId(otherTeacher).status(MatrixStatus.APPROVED).name("foreign").build();
      foreign.setId(matrixId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.empty());
      RuntimeException ex1 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateAndGetAccessibleMatrix",
                      new Class<?>[] {UUID.class, UUID.class},
                      matrixId,
                      teacherId));
      assertTrue(ex1.getCause() instanceof java.lang.reflect.InvocationTargetException);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(foreign));
      RuntimeException ex2 =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateAndGetAccessibleMatrix",
                      new Class<?>[] {UUID.class, UUID.class},
                      matrixId,
                      teacherId));
      assertTrue(ex2.getCause() instanceof java.lang.reflect.InvocationTargetException);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      assertNotNull(
          invokePrivate(
              "validateAndGetAccessibleMatrix",
              new Class<?>[] {UUID.class, UUID.class},
              matrixId,
              teacherId));
    }

    @Test
    void it_should_throw_illegal_state_when_current_auth_is_not_jwt() {
      SecurityContextHolder.clearContext();
      SecurityContextHolder.getContext().setAuthentication(
          new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
              "user", "pass"));
      IllegalStateException ex =
          assertThrows(IllegalStateException.class, () -> assessmentService.getMyAssessments(null, null, PageRequest.of(0, 1)));
      assertEquals("Authentication is not JwtAuthenticationToken", ex.getMessage());
    }

    @Test
    void it_should_throw_user_not_existed_when_validate_teacher_role_cannot_load_user() {
      Jwt jwt =
          Jwt.withTokenValue("student-token")
              .header("alg", "none")
              .subject(teacherId.toString())
              .claim("scope", "assessment:write")
              .build();
      SecurityContextHolder.getContext()
          .setAuthentication(
              new JwtAuthenticationToken(
                  jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.STUDENT_ROLE))));
      when(userRepository.findByIdWithRoles(teacherId)).thenReturn(Optional.empty());
      AppException ex = assertThrows(AppException.class, () -> assessmentService.createAssessment(validRequest));
      assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_generate_assessment_by_percentage_matrix_not_found() {
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.empty());
      GenerateAssessmentByPercentageRequest req =
          GenerateAssessmentByPercentageRequest.builder()
              .examMatrixId(matrixId)
              .totalQuestions(5)
              .cognitiveLevelPercentages(Map.of(CognitiveLevel.NHAN_BIET, 100.0))
              .build();
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.generateAssessmentByPercentage(req));
      assertEquals(ErrorCode.EXAM_MATRIX_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_generate_assessment_by_percentage_has_no_bank_mappings() {
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      GenerateAssessmentByPercentageRequest req =
          GenerateAssessmentByPercentageRequest.builder()
              .examMatrixId(matrixId)
              .totalQuestions(5)
              .cognitiveLevelPercentages(Map.of(CognitiveLevel.NHAN_BIET, 100.0))
              .build();
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.generateAssessmentByPercentage(req));
      assertEquals(ErrorCode.MATRIX_VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    void it_should_generate_assessment_by_percentage_with_insufficient_warnings() {
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(
              List.of(
                  com.fptu.math_master.entity.ExamMatrixBankMapping.builder()
                      .examMatrixId(matrixId)
                      .matrixRowId(UUID.randomUUID())
                      .questionCount(10)
                      .build()));
      when(questionRepository.countApprovedByBanksAndCognitiveLevel(any(), anyString())).thenReturn(1L);
      when(assessmentRepository.save(any(Assessment.class)))
          .thenAnswer(
              inv -> {
                Assessment a = inv.getArgument(0);
                a.setId(assessmentId);
                return a;
              });
      when(questionRepository.findRandomApprovedByBanksAndCognitiveLevel(any(), anyString(), anyInt()))
          .thenReturn(Collections.emptyList());
      PercentageBasedGenerationResponse res =
          assessmentService.generateAssessmentByPercentage(
              GenerateAssessmentByPercentageRequest.builder()
                  .examMatrixId(matrixId)
                  .totalQuestions(10)
                  .assessmentTitle("Đề cảnh báo thiếu câu")
                  .cognitiveLevelPercentages(Map.of(CognitiveLevel.NHAN_BIET, 100.0))
                  .build());
      assertFalse(res.getWarnings() == null || res.getWarnings().isEmpty());
      assertFalse(res.getSuccess());
    }

    @Test
    void it_should_throw_exception_when_generate_assessment_from_matrix_not_found() {
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.generateAssessmentFromMatrix(
                      GenerateAssessmentQuestionsRequest.builder().examMatrixId(matrixId).build()));
      assertEquals(ErrorCode.EXAM_MATRIX_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_delete_assessment_is_closed() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.CLOSED)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.deleteAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_IS_CLOSED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_close_assessment_is_not_published() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      AppException ex =
          assertThrows(AppException.class, () -> assessmentService.closeAssessment(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_NOT_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_return_false_when_can_delete_not_owner() {
      UUID other = UUID.fromString("c428a86f-c22f-4137-bb7b-7025e8f77f8a");
      authenticateAsTeacher(other);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      assertFalse(assessmentService.canDeleteAssessment(assessmentId));
    }

    @Test
    void it_should_return_empty_available_questions_when_no_matches() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(questionRepository.findAvailableByAssessmentId(any(), any(), any(), any(), any()))
          .thenReturn(Page.empty());
      var result = assessmentService.getAvailableQuestions(assessmentId, null, null, PageRequest.of(0, 5));
      assertTrue(result.getData().isEmpty());
    }

    @Test
    void it_should_throw_exception_when_batch_add_on_published_assessment() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(
              AppException.class,
              () -> assessmentService.batchAddQuestions(assessmentId, new BatchAddQuestionsRequest(List.of(questionId))));
      assertEquals(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_batch_add_on_matrix_assessment() {
      authenticateAsTeacher(teacherId);
      Assessment matrixAssessment = buildAssessmentWithStatus(AssessmentStatus.DRAFT);
      matrixAssessment.setExamMatrixId(matrixId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(matrixAssessment));
      AppException ex =
          assertThrows(
              AppException.class,
              () -> assessmentService.batchAddQuestions(assessmentId, new BatchAddQuestionsRequest(List.of(questionId))));
      assertEquals(ErrorCode.ASSESSMENT_QUESTION_EDIT_BLOCKED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_set_points_override_on_published_assessment() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.setPointsOverride(
                      assessmentId,
                      PointsOverrideRequest.builder().questionId(questionId).pointsOverride(new BigDecimal("1")).build()));
      assertEquals(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_set_points_override_question_not_found() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessmentId, questionId))
          .thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  assessmentService.setPointsOverride(
                      assessmentId,
                      PointsOverrideRequest.builder().questionId(questionId).pointsOverride(new BigDecimal("1")).build()));
      assertEquals(ErrorCode.ASSESSMENT_QUESTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_auto_distribute_points_when_remaining_percentage_is_zero_for_unmatched() {
      authenticateAsTeacher(teacherId);
      AssessmentQuestion aq1 = AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).orderIndex(1).build();
      Question q1 =
          Question.builder()
              .questionText("x")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.VAN_DUNG)
              .points(new BigDecimal("1"))
              .build();
      q1.setId(questionId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(aq1));
      when(questionRepository.findByIdAndNotDeleted(questionId)).thenReturn(Optional.of(q1));
      assessmentService.autoDistributePoints(
          assessmentId,
          new AutoDistributePointsRequest(new BigDecimal("10.00"), Map.of(CognitiveLevel.NHAN_BIET.name(), 100)));
      assertEquals(new BigDecimal("10.00"), aq1.getPointsOverride());
    }

    @Test
    void it_should_cover_private_validate_dates_branches() {
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateDates",
                      new Class<?>[] {Instant.class, Instant.class},
                      Instant.now().plusSeconds(500),
                      Instant.now()));
      assertTrue(ex.getCause() instanceof java.lang.reflect.InvocationTargetException);
      invokePrivate(
          "validateDates",
          new Class<?>[] {Instant.class, Instant.class},
          Instant.now(),
          Instant.now().plusSeconds(500));
    }

    @Test
    void it_should_cover_private_validate_owner_or_admin_branches() {
      UUID other = UUID.fromString("54cc0a57-cbe5-4995-92a1-b478f94ac40f");
      authenticateAsTeacher(teacherId);
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "validateOwnerOrAdmin",
                      new Class<?>[] {UUID.class, UUID.class},
                      other,
                      teacherId));
      assertTrue(ex.getCause() instanceof java.lang.reflect.InvocationTargetException);
      invokePrivate(
          "validateOwnerOrAdmin",
          new Class<?>[] {UUID.class, UUID.class},
          teacherId,
          teacherId);
    }

    @Test
    void it_should_cover_private_normalize_lesson_ids_branches() {
      UUID l1 = UUID.randomUUID();
      UUID l2 = UUID.randomUUID();
      List<UUID> normalized =
          invokePrivate("normalizeLessonIds", new Class<?>[] {List.class}, List.of(l1, l1, l2, l2));
      assertEquals(2, normalized.size());
      assertEquals(l1, normalized.get(0));
      assertEquals(l2, normalized.get(1));
    }

    @Test
    void it_should_cover_private_build_summary_map_for_empty_and_null_rows() {
      Map<UUID, long[]> empty =
          invokePrivate("buildSummaryMap", new Class<?>[] {java.util.Collection.class}, Collections.emptyList());
      assertTrue(empty.isEmpty());

      UUID id = UUID.randomUUID();
      when(assessmentRepository.findBulkSummaryByIds(List.of(id)))
          .thenReturn(Collections.singletonList(new Object[] {id, null, null, null}));
      Map<UUID, long[]> map =
          invokePrivate("buildSummaryMap", new Class<?>[] {java.util.Collection.class}, List.of(id));
      assertEquals(1, map.size());
      assertEquals(0L, map.get(id)[0]);
      assertEquals(0L, map.get(id)[1]);
      assertEquals(0L, map.get(id)[2]);
    }

    @Test
    void it_should_return_true_when_can_delete_owned_draft_without_submissions() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.DRAFT)));
      when(assessmentRepository.countSubmissionsByAssessmentId(assessmentId)).thenReturn(0L);
      assertTrue(assessmentService.canDeleteAssessment(assessmentId));
    }

    @Test
    void it_should_return_false_when_can_edit_assessment_is_not_draft() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      assertFalse(assessmentService.canEditAssessment(assessmentId));
    }

    @Test
    void it_should_return_false_when_can_publish_assessment_is_not_draft() {
      authenticateAsTeacher(teacherId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(buildAssessmentWithStatus(AssessmentStatus.PUBLISHED)));
      when(assessmentRepository.countQuestionsByAssessmentId(assessmentId)).thenReturn(10L);
      assertFalse(assessmentService.canPublishAssessment(assessmentId));
    }
  }
}

