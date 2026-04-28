package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.ExamMatrixRequest;
import com.fptu.math_master.dto.request.BuildExamMatrixRequest;
import com.fptu.math_master.dto.request.AddTemplateMappingRequest;
import com.fptu.math_master.dto.request.BatchUpsertMatrixRowCellsRequest;
import com.fptu.math_master.dto.request.BatchAddTemplateMappingsRequest;
import com.fptu.math_master.dto.request.FinalizePreviewRequest;
import com.fptu.math_master.dto.request.GeneratePreviewRequest;
import com.fptu.math_master.dto.request.MatrixCellRequest;
import com.fptu.math_master.dto.request.MatrixRowRequest;
import com.fptu.math_master.dto.response.ExamMatrixResponse;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.FinalizePreviewResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.MatchingTemplatesResponse;
import com.fptu.math_master.dto.response.MatrixValidationReport;
import com.fptu.math_master.dto.response.PreviewCandidatesResponse;
import com.fptu.math_master.dto.response.TemplateMappingResponse;
import com.fptu.math_master.dto.response.BatchTemplateMappingsResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.ExamMatrixBankMapping;
import com.fptu.math_master.entity.ExamMatrixRow;
import com.fptu.math_master.entity.ExamMatrixTemplateMapping;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.ExamMatrixTemplateMappingRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.AIEnhancementService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("ExamMatrixServiceImpl - Tests")
class ExamMatrixServiceImplTest extends BaseUnitTest {

  @InjectMocks private ExamMatrixServiceImpl examMatrixService;

  @Mock private ExamMatrixRepository examMatrixRepository;
  @Mock private ExamMatrixBankMappingRepository bankMappingRepository;
  @Mock private ExamMatrixTemplateMappingRepository templateMappingRepository;
  @Mock private ExamMatrixRowRepository examMatrixRowRepository;
  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private QuestionBankRepository questionBankRepository;
  @Mock private AssessmentRepository assessmentRepository;
  @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
  @Mock private UserRepository userRepository;
  @Mock private CurriculumRepository curriculumRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private AIEnhancementService aiEnhancementService;

  private UUID teacherId;
  private UUID matrixId;
  private ExamMatrix draftMatrix;
  private ExamMatrixRequest defaultMatrixRequest;

  @BeforeEach
  void setUp() {
    teacherId = UUID.fromString("2b72df70-3a75-44a0-ae76-7423af8ac1d3");
    matrixId = UUID.fromString("e3ba4b30-9b70-4f45-8a7c-41de8e0a11f2");

    draftMatrix =
        ExamMatrix.builder()
            .teacherId(teacherId)
            .name("Ma trận đề giữa kỳ Toán 10")
            .description("Ma trận đánh giá năng lực học kỳ 1")
            .isReusable(true)
            .status(MatrixStatus.DRAFT)
            .totalQuestionsTarget(10)
            .totalPointsTarget(new BigDecimal("10.0"))
            .build();
    draftMatrix.setId(matrixId);

    defaultMatrixRequest =
        ExamMatrixRequest.builder()
            .name("Ma trận đề giữa kỳ Toán 10")
            .description("Ma trận đánh giá năng lực học kỳ 1")
            .isReusable(true)
            .totalQuestionsTarget(10)
            .totalPointsTarget(new BigDecimal("10.0"))
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsTeacher(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("unit-test-token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("scope", "matrix:write")
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.TEACHER_ROLE)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void authenticateAsNonJwt() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("teacher@fptu.edu.vn", "password"));
  }

  private User buildTeacher(UUID id, String fullName) {
    User teacher = User.builder().fullName(fullName).build();
    teacher.setId(id);
    return teacher;
  }

  private ExamMatrixTemplateMapping buildTemplateMapping(
      UUID id, UUID targetMatrixId, CognitiveLevel level, int count, BigDecimal pointsPerQuestion) {
    ExamMatrixTemplateMapping mapping =
        ExamMatrixTemplateMapping.builder()
            .examMatrixId(targetMatrixId)
            .templateId(UUID.randomUUID())
            .cognitiveLevel(level)
            .questionCount(count)
            .pointsPerQuestion(pointsPerQuestion)
            .build();
    mapping.setId(id);
    return mapping;
  }

  private ExamMatrixBankMapping buildBankMapping(
      UUID id, UUID targetMatrixId, UUID bankId, CognitiveLevel level, Integer count, BigDecimal ppq) {
    ExamMatrixBankMapping mapping =
        ExamMatrixBankMapping.builder()
            .examMatrixId(targetMatrixId)
            .matrixRowId(UUID.randomUUID()) // Added matrixRowId as required field
            .cognitiveLevel(level)
            .questionCount(count)
            .pointsPerQuestion(ppq)
            .build();
    mapping.setId(id);
    return mapping;
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = ExamMatrixServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return (T) method.invoke(null, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private static method: " + methodName, ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = ExamMatrixServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return (T) method.invoke(examMatrixService, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private method: " + methodName, ex);
    }
  }

  private Throwable extractRootCause(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current;
  }

  @Nested
  @DisplayName("createExamMatrix()")
  class CreateExamMatrixTests {

    /**
     * Normal case: Tạo ma trận đề thành công khi user đăng nhập bằng JWT và có vai trò TEACHER.
     *
     * <p>Input:
     * <ul>
     *   <li>request: tên ma trận, mục tiêu số câu và tổng điểm hợp lệ</li>
     *   <li>authentication: JWT subject trùng teacherId, authority ROLE_TEACHER</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateTeacherRole: hasRole(TEACHER) = TRUE branch (không cần query user roles)</li>
     *   <li>isReusable != null → dùng giá trị từ request</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về {@link ExamMatrixResponse} với status {@code DRAFT}</li>
     *   <li>{@code examMatrixRepository.save()} được gọi đúng 1 lần</li>
     * </ul>
     */
    @Test
    void it_should_create_exam_matrix_when_request_is_valid_and_user_has_teacher_role() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);

      ExamMatrix savedMatrix =
          ExamMatrix.builder()
              .teacherId(teacherId)
              .name(defaultMatrixRequest.getName())
              .description(defaultMatrixRequest.getDescription())
              .isReusable(true)
              .status(MatrixStatus.DRAFT)
              .totalQuestionsTarget(defaultMatrixRequest.getTotalQuestionsTarget())
              .totalPointsTarget(defaultMatrixRequest.getTotalPointsTarget())
              .build();
      savedMatrix.setId(matrixId);
      savedMatrix.setCreatedAt(Instant.parse("2026-04-26T00:00:00Z"));
      savedMatrix.setUpdatedAt(Instant.parse("2026-04-26T00:00:00Z"));

      when(examMatrixRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrix.class)))
          .thenReturn(savedMatrix);
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Nguyen Gia Huy")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      ExamMatrixResponse result = examMatrixService.createExamMatrix(defaultMatrixRequest);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(matrixId, result.getId()),
          () -> assertEquals(MatrixStatus.DRAFT, result.getStatus()),
          () -> assertEquals("Nguyen Gia Huy", result.getTeacherName()),
          () -> assertEquals(0, result.getBankMappingCount()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ExamMatrix.class));
      verify(userRepository, times(1)).findById(teacherId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(examMatrixRepository, userRepository, bankMappingRepository);
    }

    /**
     * Abnormal case: Ném lỗi khi authentication không phải JWT token.
     *
     * <p>Input:
     * <ul>
     *   <li>authentication: {@link UsernamePasswordAuthenticationToken}</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getCurrentUserId: {@code auth instanceof JwtAuthenticationToken} = FALSE branch</li>
     *   <li>TRUE branch được cover bởi
     *       {@code it_should_create_exam_matrix_when_request_is_valid_and_user_has_teacher_role}</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link IllegalStateException} với message authentication không hợp lệ</li>
     * </ul>
     */
    @Test
    void it_should_throw_illegal_state_exception_when_authentication_is_not_jwt() {
      // ===== ARRANGE =====
      authenticateAsNonJwt();

      // ===== ACT & ASSERT =====
      IllegalStateException exception =
          assertThrows(
              IllegalStateException.class,
              () -> examMatrixService.createExamMatrix(defaultMatrixRequest));

      assertTrue(exception.getMessage().contains("Authentication is not JwtAuthenticationToken"));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          examMatrixRepository, userRepository, bankMappingRepository, templateMappingRepository);
    }
  }

  @Nested
  @DisplayName("deleteExamMatrix()")
  class DeleteExamMatrixTests {

    /**
     * Normal case: Soft-delete ma trận ở trạng thái DRAFT bởi chính chủ sở hữu.
     *
     * <p>Input:
     * <ul>
     *   <li>matrixId: tồn tại, status DRAFT</li>
     *   <li>currentUserId: trùng teacherId của matrix</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>loadMatrixOrThrow: Optional present = TRUE branch</li>
     *   <li>validateOwnerOrAdmin: ownerId == currentUserId = TRUE branch</li>
     *   <li>validateNotApprovedOrLocked: status != APPROVED và != LOCKED = normal branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@code deletedAt} được gán khác null và lưu lại repository</li>
     * </ul>
     */
    @Test
    void it_should_soft_delete_matrix_when_owner_deletes_draft_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRepository.save(draftMatrix)).thenReturn(draftMatrix);

      // ===== ACT =====
      examMatrixService.deleteExamMatrix(matrixId);

      // ===== ASSERT =====
      assertNotNull(draftMatrix.getDeletedAt());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRepository, times(1)).save(draftMatrix);
      verifyNoMoreInteractions(examMatrixRepository);
    }
  }

  @Nested
  @DisplayName("updateExamMatrix()")
  class UpdateExamMatrixTests {

    /**
     * Normal case: Cập nhật ma trận thành công khi owner chỉnh sửa matrix ở trạng thái DRAFT.
     *
     * <p>Input:
     * <ul>
     *   <li>matrixId: hợp lệ, thuộc current teacher</li>
     *   <li>request: đổi tên, mô tả, số câu và tổng điểm mục tiêu</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateOwnerOrAdmin: owner match = TRUE branch</li>
     *   <li>validateNotApprovedOrLocked: matrix không APPROVED/LOCKED</li>
     *   <li>request fields nullable checks: tất cả nhánh set giá trị mới đều được đi qua</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Thông tin matrix được cập nhật và lưu đúng 1 lần</li>
     * </ul>
     */
    @Test
    void it_should_update_matrix_when_owner_updates_draft_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      ExamMatrixRequest updateRequest =
          ExamMatrixRequest.builder()
              .name("Ma trận cuối kỳ Toán 10")
              .description("Cập nhật theo blueprint HK2")
              .isReusable(false)
              .totalQuestionsTarget(20)
              .totalPointsTarget(new BigDecimal("20.0"))
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRepository.save(draftMatrix)).thenReturn(draftMatrix);
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Pham Dang Khoi")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      ExamMatrixResponse result = examMatrixService.updateExamMatrix(matrixId, updateRequest);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals("Ma trận cuối kỳ Toán 10", result.getName()),
          () -> assertEquals("Cập nhật theo blueprint HK2", result.getDescription()),
          () -> assertEquals(20, result.getTotalQuestionsTarget()),
          () -> assertEquals(0, new BigDecimal("20.0").compareTo(result.getTotalPointsTarget())));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRepository, times(1)).save(draftMatrix);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(userRepository, times(1)).findById(teacherId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(
          examMatrixRepository,
          templateMappingRepository,
          userRepository,
          bankMappingRepository);
    }

    /**
     * Abnormal case: Không cho phép cập nhật khi matrix đã APPROVED.
     *
     * <p>Input:
     * <ul>
     *   <li>matrix.status: APPROVED</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateNotApprovedOrLocked: status == APPROVED → TRUE branch throw exception</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code EXAM_MATRIX_APPROVED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_updating_approved_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      ExamMatrix approved = ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.APPROVED).build();
      approved.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approved));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.updateExamMatrix(matrixId, defaultMatrixRequest));
      assertEquals(ErrorCode.EXAM_MATRIX_APPROVED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verifyNoMoreInteractions(examMatrixRepository);
    }
  }

  @Nested
  @DisplayName("lockMatrix()")
  class LockMatrixTests {

    /**
     * Abnormal case: Không thể lock ma trận khi trạng thái chưa APPROVED.
     *
     * <p>Input:
     * <ul>
     *   <li>matrix.status: DRAFT</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>status == LOCKED → FALSE branch</li>
     *   <li>status != APPROVED → TRUE branch, throw {@link AppException}</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code MATRIX_NOT_APPROVED}</li>
     *   <li>{@code examMatrixRepository.save()} không được gọi</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_locking_matrix_that_is_not_approved() {
      // ===== ARRANGE =====
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> examMatrixService.lockMatrix(matrixId));
      assertEquals(ErrorCode.MATRIX_NOT_APPROVED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verifyNoMoreInteractions(examMatrixRepository);
    }

    /**
     * Normal case: Lock thành công khi ma trận đã APPROVED.
     *
     * <p>Input:
     * <ul>
     *   <li>matrix.status: APPROVED</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>status == LOCKED → FALSE branch</li>
     *   <li>status != APPROVED → FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>status mới là {@code LOCKED} và repository save được gọi đúng 1 lần</li>
     * </ul>
     */
    @Test
    void it_should_lock_matrix_when_status_is_approved() {
      // ===== ARRANGE =====
      ExamMatrix approvedMatrix =
          ExamMatrix.builder()
              .teacherId(teacherId)
              .name("Ma trận ôn tập thi thử")
              .status(MatrixStatus.APPROVED)
              .build();
      approvedMatrix.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId))
          .thenReturn(Optional.of(approvedMatrix));
      when(examMatrixRepository.save(approvedMatrix)).thenReturn(approvedMatrix);

      // ===== ACT =====
      examMatrixService.lockMatrix(matrixId);

      // ===== ASSERT =====
      assertEquals(MatrixStatus.LOCKED, approvedMatrix.getStatus());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRepository, times(1)).save(approvedMatrix);
      verifyNoMoreInteractions(examMatrixRepository);
    }
  }

  @Nested
  @DisplayName("approveMatrix() and resetMatrix()")
  class ApproveAndResetMatrixTests {

    /**
     * Abnormal case: Duyệt matrix thất bại khi báo cáo validate không đạt.
     *
     * <p>Input:
     * <ul>
     *   <li>matrix: DRAFT, không có bất kỳ mapping nào</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>approveMatrix: report.canApprove = FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code MATRIX_VALIDATION_FAILED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_approving_matrix_with_invalid_validation_report() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> examMatrixService.approveMatrix(matrixId));
      assertEquals(ErrorCode.MATRIX_VALIDATION_FAILED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(2)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, bankMappingRepository);
    }

    /**
     * Normal case: Reset matrix từ APPROVED về DRAFT.
     *
     * <p>Input:
     * <ul>
     *   <li>matrix.status: APPROVED</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>status != APPROVED check trong resetMatrix → FALSE branch (không ném lỗi)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>status sau reset là {@code DRAFT}</li>
     * </ul>
     */
    @Test
    void it_should_reset_matrix_to_draft_when_status_is_approved() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      ExamMatrix approved = ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.APPROVED).build();
      approved.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approved));
      when(examMatrixRepository.save(approved)).thenReturn(approved);
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Ngo Minh Chau")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      ExamMatrixResponse result = examMatrixService.resetMatrix(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(MatrixStatus.DRAFT, result.getStatus()),
          () -> assertEquals("Ngo Minh Chau", result.getTeacherName()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRepository, times(1)).save(approved);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(userRepository, times(1)).findById(teacherId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(
          examMatrixRepository,
          templateMappingRepository,
          userRepository,
          bankMappingRepository);
    }
  }

  @Nested
  @DisplayName("getExamMatrixByAssessmentId()")
  class GetExamMatrixByAssessmentIdTests {

    /**
     * Abnormal case: Không tìm thấy assessment tương ứng assessmentId.
     *
     * <p>Input:
     * <ul>
     *   <li>assessmentId: không tồn tại</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>assessmentRepository.findByIdAndNotDeleted → Optional empty branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_assessment_not_found_by_id() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID assessmentId = UUID.fromString("0f7dc2db-8d8d-4cfe-bdcf-6c4e98596d52");
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> examMatrixService.getExamMatrixByAssessmentId(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(assessmentId);
      verifyNoMoreInteractions(assessmentRepository);
    }

    /**
     * Abnormal case: User không phải owner và không có quyền ADMIN khi lấy matrix theo assessment.
     *
     * <p>Input:
     * <ul>
     *   <li>assessment.teacherId: khác với current JWT subject</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateOwnerOrAdmin: owner mismatch và hasRole(ADMIN)=FALSE → throw access denied</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code ASSESSMENT_ACCESS_DENIED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_access_denied_when_current_user_is_not_owner_or_admin() {
      // ===== ARRANGE =====
      UUID assessmentId = UUID.fromString("f20c9f2d-0153-4f08-b6f2-3e3a0df9c672");
      UUID anotherTeacherId = UUID.fromString("66cb1b99-1d5d-4f67-a565-0be0e9d52b5f");
      authenticateAsTeacher(teacherId);

      Assessment assessment = Assessment.builder().teacherId(anotherTeacherId).title("KT 15 phút").build();
      assessment.setId(assessmentId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(assessment));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> examMatrixService.getExamMatrixByAssessmentId(assessmentId));
      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(assessmentRepository, times(1)).findByIdAndNotDeleted(assessmentId);
      verifyNoMoreInteractions(assessmentRepository);
    }
  }

  @Nested
  @DisplayName("getMyExamMatricesPaged()")
  class GetMyExamMatricesPagedTests {

    /**
     * Normal case: Trả về page kết quả khi search có khoảng trắng cần trim.
     *
     * <p>Input:
     * <ul>
     *   <li>search: "  giữa kỳ  " (cần trim trước khi query)</li>
     *   <li>status: DRAFT</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>search != null && !isBlank() → TRUE branch, dùng search.trim()</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Repository filter nhận đúng searchTerm đã trim và trả về page có dữ liệu</li>
     * </ul>
     */
    @Test
    void it_should_return_paged_matrices_when_search_term_is_trimmed() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      PageRequest pageable = PageRequest.of(0, 10);
      Page<ExamMatrix> matrixPage = new PageImpl<>(List.of(draftMatrix), pageable, 1);

      when(examMatrixRepository.findByTeacherIdWithFilters(teacherId, MatrixStatus.DRAFT, "giữa kỳ", pageable))
          .thenReturn(matrixPage);
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Le Nguyen Bao Anh")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      Page<ExamMatrixResponse> result =
          examMatrixService.getMyExamMatricesPaged("  giữa kỳ  ", MatrixStatus.DRAFT, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getTotalElements()),
          () -> assertEquals("Le Nguyen Bao Anh", result.getContent().getFirst().getTeacherName()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1))
          .findByTeacherIdWithFilters(teacherId, MatrixStatus.DRAFT, "giữa kỳ", pageable);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(userRepository, times(1)).findById(teacherId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(
          examMatrixRepository,
          templateMappingRepository,
          userRepository,
          bankMappingRepository);
    }
  }

  @Nested
  @DisplayName("private switch coverage")
  class PrivateSwitchCoverageTests {

    /**
     * Normal case: Map đầy đủ các giá trị {@link CognitiveLevel} sang label ngắn.
     *
     * <p>Input:
     * <ul>
     *   <li>10 enum values của CognitiveLevel</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>switch {@code cognitiveLevelLabel()} cover full cases</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Các nhóm level trả về đúng NB/TH/VD/VDC</li>
     * </ul>
     */
    @Test
    void it_should_return_expected_label_when_mapping_each_cognitive_level() {
      // ===== ARRANGE =====
      // Intentionally iterating all enum constants to satisfy full switch coverage.

      // ===== ACT =====
      String nb = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.NHAN_BIET);
      String th = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.THONG_HIEU);
      String vd = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.VAN_DUNG);
      String vdc = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.VAN_DUNG_CAO);
      String remember = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.REMEMBER);
      String understand = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.UNDERSTAND);
      String apply = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.APPLY);
      String analyze = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.ANALYZE);
      String evaluate = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.EVALUATE);
      String create = invokePrivateStatic("cognitiveLevelLabel", new Class<?>[] {CognitiveLevel.class}, CognitiveLevel.CREATE);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("NB", nb),
          () -> assertEquals("TH", th),
          () -> assertEquals("VD", vd),
          () -> assertEquals("VDC", vdc),
          () -> assertEquals("NB", remember),
          () -> assertEquals("TH", understand),
          () -> assertEquals("VD", apply),
          () -> assertEquals("VDC", analyze),
          () -> assertEquals("VDC", evaluate),
          () -> assertEquals("VDC", create));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          examMatrixRepository,
          templateMappingRepository,
          bankMappingRepository,
          userRepository,
          assessmentRepository);
    }
  }

  @Nested
  @DisplayName("generatePreview()")
  class GeneratePreviewTests {

    /**
     * Abnormal case: TemplateId trong request không khớp với mapping.
     *
     * <p>Input:
     * <ul>
     *   <li>mapping.templateId != request.templateId</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>if !mapping.templateId.equals(request.templateId) → TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code TEMPLATE_MAPPING_TEMPLATE_MISMATCH}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_request_template_id_does_not_match_mapping_template_id() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("39b3ee5a-9965-4eb4-aa2c-65391ecf6646");
      UUID templateIdInMapping = UUID.fromString("18bb9107-2b1f-44f2-a90f-31b6e0368cb8");
      UUID templateIdInRequest = UUID.fromString("5e915f4f-74f0-4caf-9db4-f8bf6db5c8a3");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(
              mappingId, matrixId, CognitiveLevel.THONG_HIEU, 2, new BigDecimal("1.0"));
      mapping.setTemplateId(templateIdInMapping);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateIdInRequest).count(2).build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> examMatrixService.generatePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.TEMPLATE_MAPPING_TEMPLATE_MISMATCH, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository);
    }

    /**
     * Normal case: Sinh preview thành công với 1 candidate từ AI service.
     *
     * <p>Input:
     * <ul>
     *   <li>matrix status: APPROVED (chỉ cảnh báo, vẫn cho preview)</li>
     *   <li>request count: 1, difficulty override: HARD</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>matrix status warning branch (APPROVED/LOCKED) = TRUE</li>
     *   <li>difficulty override warning branch = TRUE</li>
     *   <li>while-loop generate candidate thành công và thêm vào list</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>generatedCount = 1 và warnings không rỗng</li>
     * </ul>
     */
    @Test
    void it_should_generate_preview_candidates_when_request_is_valid() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("8f5d822f-fa2e-4e27-864f-884b717f4b30");
      UUID templateId = UUID.fromString("95d0de34-fd47-406a-b6f2-c7dcb668ba31");

      ExamMatrix approvedMatrix =
          ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.APPROVED).build();
      approvedMatrix.setId(matrixId);

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.VAN_DUNG, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);

      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu phương trình bậc nhất một ẩn")
              .status(TemplateStatus.PUBLISHED)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .build();
      template.setId(templateId);

      GeneratedQuestionSample sample =
          GeneratedQuestionSample.builder()
              .questionText("Giải phương trình 2x + 3 = 11. Giá trị x bằng bao nhiêu?")
              .options(
                  java.util.Map.of(
                      "A", "3",
                      "B", "4",
                      "C", "5",
                      "D", "6"))
              .correctAnswer("B")
              .calculatedDifficulty(QuestionDifficulty.HARD)
              .explanation("Chuyển vế rồi chia 2.")
              .usedParameters(java.util.Map.of("a", 2, "b", 3))
              .answerCalculation("x=(11-3)/2")
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(approvedMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(template, 7)).thenReturn(sample);
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder()
              .templateId(templateId)
              .count(1)
              .difficulty(QuestionDifficulty.HARD)
              .seed(7L)
              .build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getGeneratedCount()),
          () -> assertEquals(1, result.getCandidates().size()),
          () -> assertEquals("Mẫu phương trình bậc nhất một ẩn", result.getTemplateName()),
          () -> assertTrue(result.getWarnings() != null && !result.getWarnings().isEmpty()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verify(questionTemplateRepository, times(1)).findByIdWithCreator(templateId);
      verify(aiEnhancementService, times(1)).generateQuestion(template, 7);
      verify(questionTemplateRepository, times(1)).findById(templateId);
      verifyNoMoreInteractions(
          examMatrixRepository,
          templateMappingRepository,
          questionTemplateRepository,
          aiEnhancementService);
    }
  }

  @Nested
  @DisplayName("finalizePreview()")
  class FinalizePreviewTests {

    /**
     * Abnormal case: Ném lỗi khi options MCQ không đủ A/B/C/D.
     *
     * <p>Input:
     * <ul>
     *   <li>questionType = MULTIPLE_CHOICE nhưng options thiếu key bắt buộc</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>MCQ validation branch → invalid options = TRUE branch (throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code MCQ_INVALID_OPTIONS}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_finalizing_mcq_with_invalid_options() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("1b6121da-b368-4de6-a4e1-c4276d0c5dc3");
      UUID templateId = UUID.fromString("c5d16bdc-fcb7-43bc-b9e0-089ef1131b61");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(
              mappingId, matrixId, CognitiveLevel.NHAN_BIET, 2, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);

      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu trắc nghiệm số học")
              .status(TemplateStatus.PUBLISHED)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest.QuestionItem invalidItem =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText("2 + 2 bằng bao nhiêu?")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(java.util.Map.of("A", "3", "B", "4", "C", "5"))
              .correctAnswer("B")
              .difficulty(QuestionDifficulty.EASY)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .build();

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questionBankId(UUID.fromString("a2f9aa8c-7611-4cf7-9c5f-9f743109cbfd"))
              .questions(List.of(invalidItem))
              .build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.MCQ_INVALID_OPTIONS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verify(questionTemplateRepository, times(1)).findByIdWithCreator(templateId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, questionTemplateRepository);
    }

    /**
     * Normal case: Finalize thành công với 1 câu hỏi hợp lệ và không có assessment liên kết.
     *
     * <p>Input:
     * <ul>
     *   <li>replaceExisting = false</li>
     *   <li>1 câu MCQ hợp lệ, mapping target count >= số câu thêm mới</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>replaceExisting = FALSE branch</li>
     *   <li>validItems non-empty branch</li>
     *   <li>linkedAssessments.isEmpty() = TRUE branch ở phần currentMappingQuestionCount</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>savedCount = 1, questionIds có 1 phần tử, currentMappingQuestionCount = 1</li>
     * </ul>
     */
    @Test
    void it_should_finalize_preview_and_persist_questions_when_input_is_valid() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("cf24e6d1-5cd3-4397-a49d-4d9f4aab8e17");
      UUID templateId = UUID.fromString("3ca9da40-f0b6-4d8e-b3ad-5c68f654f991");
      UUID savedQuestionId = UUID.fromString("17ed7db0-8f24-4bd7-903e-d4502d6e7437");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(
              mappingId, matrixId, CognitiveLevel.THONG_HIEU, 3, new BigDecimal("1.5"));
      mapping.setTemplateId(templateId);

      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu hệ phương trình tuyến tính")
              .status(TemplateStatus.PUBLISHED)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .usageCount(10)
              .build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId))
          .thenReturn(Collections.emptyList());
      when(questionRepository.save(org.mockito.ArgumentMatchers.any(Question.class)))
          .thenAnswer(
              invocation -> {
                Question q = invocation.getArgument(0);
                q.setId(savedQuestionId);
                return q;
              });
      when(questionTemplateRepository.save(template)).thenReturn(template);

      FinalizePreviewRequest.QuestionItem validItem =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText("Giải hệ: x + y = 5, x - y = 1.")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(
                  java.util.Map.of(
                      "A", "x=3, y=2",
                      "B", "x=2, y=3",
                      "C", "x=4, y=1",
                      "D", "x=1, y=4"))
              .correctAnswer("A")
              .difficulty(QuestionDifficulty.MEDIUM)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .generationMetadata(java.util.Map.of("seed", 11))
              .explanation("Cộng hai phương trình để tìm x, sau đó thế ngược tìm y.")
              .build();

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.5"))
              .replaceExisting(false)
              .questionBankId(UUID.fromString("29715d41-367d-492f-aad5-6973a1fd8f54"))
              .questions(List.of(validItem))
              .build();

      // ===== ACT =====
      FinalizePreviewResponse result = examMatrixService.finalizePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getSavedCount()),
          () -> assertEquals(1, result.getCurrentMappingQuestionCount()),
          () -> assertEquals(1, result.getQuestionIds().size()),
          () -> assertEquals(savedQuestionId, result.getQuestionIds().getFirst()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verify(questionTemplateRepository, times(1)).findByIdWithCreator(templateId);
      verify(assessmentRepository, times(2)).findByExamMatrixIdAndNotDeleted(matrixId);
      verify(questionRepository, times(1)).save(org.mockito.ArgumentMatchers.any(Question.class));
      verify(questionTemplateRepository, times(1)).save(template);
      verifyNoMoreInteractions(
          examMatrixRepository,
          templateMappingRepository,
          questionTemplateRepository,
          assessmentRepository,
          questionRepository,
          assessmentQuestionRepository);
    }
  }

  @Nested
  @DisplayName("buildMatrix() and getMatrixTable()")
  class StructuredMatrixTests {

    /**
     * Normal case: Build matrix dạng bảng thành công với 1 row và 1 cell.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>gradeLevel auto-resolve branch: curriculumId null nên bỏ qua resolve</li>
     *   <li>persistRow + upsertRowCells path được thực thi</li>
     * </ul>
     */
    @Test
    void it_should_build_structured_matrix_when_row_and_cell_are_valid() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("9aa430f6-efb5-43c8-9957-c33ce96df48b");
      UUID bankId = UUID.fromString("ad594922-ed4c-4dcf-a46f-4fa6f58e9a5d");
      UUID rowId = UUID.fromString("17cf7d12-f653-4902-a388-4f35090d00e1");

      MatrixCellRequest cell =
          MatrixCellRequest.builder()
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .questionCount(2)
              .pointsPerQuestion(new BigDecimal("0.5"))
              .build();
      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionDifficulty(QuestionDifficulty.EASY)
              .questionTypeName("Nhận diện đồ thị hàm số")
              .referenceQuestions("1,2")
              .cells(List.of(cell))
              .build();
      BuildExamMatrixRequest request =
          BuildExamMatrixRequest.builder()
              .name("Ma trận trắc nghiệm chương Hàm số")
              .description("Bản dựng nhanh cho kiểm tra 15 phút")
              .isReusable(true)
              .totalQuestionsTarget(2)
              .totalPointsTarget(new BigDecimal("1.0"))
              .rows(List.of(row))
              .build();

      ExamMatrix matrix =
          ExamMatrix.builder()
              .teacherId(teacherId)
              .name(request.getName())
              .description(request.getDescription())
              .isReusable(true)
              .totalQuestionsTarget(2)
              .totalPointsTarget(new BigDecimal("1.0"))
              .status(MatrixStatus.DRAFT)
              .build();
      matrix.setId(matrixId);
      when(examMatrixRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrix.class))).thenReturn(matrix);

      QuestionBank bank =
          QuestionBank.builder().teacherId(teacherId).name("Ngân hàng hàm số lớp 10").isPublic(false).build();
      bank.setId(bankId);
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));

      Chapter chapter = Chapter.builder().subjectId(null).title("Chương I - Hàm số").orderIndex(1).build();
      chapter.setId(chapterId);
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));

      ExamMatrixRow savedRow =
          ExamMatrixRow.builder()
              .examMatrixId(matrixId)
              .chapterId(chapterId)
              .questionTypeName("Nhận diện đồ thị hàm số")
              .orderIndex(1)
              .build();
      savedRow.setId(rowId);
      when(examMatrixRowRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrixRow.class)))
          .thenReturn(savedRow);
      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
          .thenReturn(List.of(savedRow));

      ExamMatrixBankMapping mapping =
          ExamMatrixBankMapping.builder()
              .examMatrixId(matrixId)
              .matrixRowId(rowId)
              .questionCount(2)
              .pointsPerQuestion(new BigDecimal("0.5"))
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .build();
      mapping.setId(UUID.fromString("f54db885-0199-4f0a-b64a-4bfef4e7df4f"));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId)).thenReturn(List.of(mapping));
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Tran Quoc Tuan")));

      // ===== ACT =====
      ExamMatrixTableResponse result = examMatrixService.buildMatrix(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(matrixId, result.getId()),
          () -> assertEquals(2, result.getGrandTotalQuestions()),
          () -> assertEquals(0, new BigDecimal("1.0").compareTo(result.getGrandTotalPoints())));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ExamMatrix.class));
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(bankId);
      verify(chapterRepository, times(2)).findById(chapterId);
      verify(examMatrixRowRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ExamMatrixRow.class));
      verify(bankMappingRepository, times(1)).deleteByExamMatrixIdAndMatrixRowId(matrixId, rowId);
      verify(bankMappingRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ExamMatrixBankMapping.class));
      verify(examMatrixRowRepository, times(1)).findByExamMatrixIdOrderByOrderIndex(matrixId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(userRepository, times(1)).findById(teacherId);
    }

    /**
     * Normal case: Lấy matrix table với tổng hợp chapter/row/cell.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>buildTableResponse path qua grouping rows by chapter và cộng dồn totals</li>
     * </ul>
     */
    @Test
    void it_should_return_matrix_table_when_matrix_and_rows_exist() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("9aa430f6-efb5-43c8-9957-c33ce96df48b");
      UUID bankId = UUID.fromString("ad594922-ed4c-4dcf-a46f-4fa6f58e9a5d");
      UUID rowId = UUID.fromString("17cf7d12-f653-4902-a388-4f35090d00e1");

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));

      ExamMatrixRow row =
          ExamMatrixRow.builder()
              .examMatrixId(matrixId)
              .chapterId(chapterId)
              .questionTypeName("Tính đạo hàm cơ bản")
              .orderIndex(1)
              .build();
      row.setId(rowId);
      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId)).thenReturn(List.of(row));

      ExamMatrixBankMapping cell =
          ExamMatrixBankMapping.builder()
              .examMatrixId(matrixId)
              .matrixRowId(rowId)
              .questionCount(3)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .build();
      cell.setId(UUID.fromString("db1f2e56-992b-4a78-8f8d-bb675f1211af"));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId)).thenReturn(List.of(cell));

      Chapter chapter = Chapter.builder().title("Chương II - Đạo hàm").orderIndex(2).build();
      chapter.setId(chapterId);
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Nguyen Thanh Long")));

      // ===== ACT =====
      ExamMatrixTableResponse result = examMatrixService.getMatrixTable(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(matrixId, result.getId()),
          () -> assertEquals(3, result.getGrandTotalQuestions()),
          () -> assertEquals(0, new BigDecimal("3.0").compareTo(result.getGrandTotalPoints())),
          () -> assertEquals(1, result.getChapters().size()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRowRepository, times(1)).findByExamMatrixIdOrderByOrderIndex(matrixId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(chapterRepository, times(1)).findById(chapterId);
      verify(userRepository, times(1)).findById(teacherId);
      verifyNoMoreInteractions(
          examMatrixRepository, examMatrixRowRepository, bankMappingRepository, chapterRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("template mapping methods")
  class TemplateMappingMethodTests {

    /**
     * Normal case: Add template mapping thành công với template active.
     */
    @Test
    void it_should_add_template_mapping_when_matrix_and_template_are_valid() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID templateId = UUID.fromString("d37f38ef-b9b3-4f55-9267-ff76a7412205");
      UUID mappingId = UUID.fromString("be8da14c-6dd9-4b86-bdf6-3b8c6e89ab58");

      AddTemplateMappingRequest request =
          AddTemplateMappingRequest.builder()
              .templateId(templateId)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .questionCount(5)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .build();

      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu bài tập phương trình căn thức")
              .status(TemplateStatus.PUBLISHED)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .build();
      template.setId(templateId);

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.THONG_HIEU, 5, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(templateMappingRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrixTemplateMapping.class)))
          .thenReturn(mapping);

      // ===== ACT =====
      TemplateMappingResponse result = examMatrixService.addTemplateMapping(matrixId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(mappingId, result.getId()),
          () -> assertEquals(templateId, result.getTemplateId()),
          () -> assertEquals("Mẫu bài tập phương trình căn thức", result.getTemplateName()),
          () -> assertEquals(5, result.getQuestionCount()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(questionTemplateRepository, times(1)).findByIdWithCreator(templateId);
      verify(templateMappingRepository, times(1))
          .save(org.mockito.ArgumentMatchers.any(ExamMatrixTemplateMapping.class));
      verifyNoMoreInteractions(examMatrixRepository, questionTemplateRepository, templateMappingRepository);
    }

    /**
     * Abnormal case: Remove mapping ném lỗi khi mapping không thuộc matrix.
     */
    @Test
    void it_should_throw_exception_when_removing_mapping_not_found_in_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("82e4cb31-aa36-4ebe-8d7d-a4679584f4dc");
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> examMatrixService.removeTemplateMapping(matrixId, mappingId));
      assertEquals(ErrorCode.EXAM_MATRIX_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository);
    }

    /**
     * Normal case: getTemplateMappings trả về mapping, tên template null khi template không còn.
     */
    @Test
    void it_should_return_template_mappings_with_null_template_name_when_template_missing() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("9eb89308-1e49-4d73-ad0e-e88a6196eaec");
      UUID templateId = UUID.fromString("e3837049-7f2a-4fe6-a9fc-a7596aa40be7");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.VAN_DUNG, 2, new BigDecimal("1.5"));
      mapping.setTemplateId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(mapping));
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

      // ===== ACT =====
      List<TemplateMappingResponse> result = examMatrixService.getTemplateMappings(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.size()),
          () -> assertEquals(mappingId, result.getFirst().getId()),
          () -> assertEquals(null, result.getFirst().getTemplateName()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(questionTemplateRepository, times(1)).findById(templateId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, questionTemplateRepository);
    }
  }

  @Nested
  @DisplayName("list/get matrix methods")
  class ListAndGetMethodTests {

    /**
     * Normal case: getExamMatrixById trả về matrix response cho owner.
     */
    @Test
    void it_should_get_exam_matrix_by_id_when_owner_requests() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Vo Thi My Linh")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      ExamMatrixResponse result = examMatrixService.getExamMatrixById(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(matrixId, result.getId()),
          () -> assertEquals("Vo Thi My Linh", result.getTeacherName()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(userRepository, times(1)).findById(teacherId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(
          examMatrixRepository, templateMappingRepository, userRepository, bankMappingRepository);
    }

    /**
     * Normal case: getMyExamMatrices trả danh sách cho current teacher.
     */
    @Test
    void it_should_get_my_exam_matrices_when_teacher_has_matrices() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByTeacherIdAndNotDeleted(teacherId)).thenReturn(List.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Pham Gia Bao")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      List<ExamMatrixResponse> result = examMatrixService.getMyExamMatrices();

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.size()),
          () -> assertEquals(matrixId, result.getFirst().getId()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByTeacherIdAndNotDeleted(teacherId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(userRepository, times(1)).findById(teacherId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(
          examMatrixRepository, templateMappingRepository, userRepository, bankMappingRepository);
    }
  }

  @Nested
  @DisplayName("listMatchingTemplates()")
  class ListMatchingTemplatesTests {

    /**
     * Normal case: Trả về danh sách template đã xếp hạng theo độ phù hợp.
     */
    @Test
    void it_should_list_matching_templates_when_templates_found() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID templateId = UUID.fromString("f89f8a67-97e0-4d1a-bf8c-ae00d705a99a");

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(
              List.of(
                  buildTemplateMapping(
                      UUID.fromString("640e8f66-9102-4b3c-83f1-2f43199f8b3b"),
                      matrixId,
                      CognitiveLevel.THONG_HIEU,
                      2,
                      new BigDecimal("1.0"))));

      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu tìm điều kiện xác định hàm số")
              .description("Bài toán cơ bản")
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .creator(buildTeacher(teacherId, "Pham Gia Bao"))
              .isPublic(true)
              .usageCount(20)
              .build();
      template.setId(templateId);
      template.setCreatedBy(teacherId);

      PageRequest pageable = PageRequest.of(0, 10);
      when(questionTemplateRepository.findMatchingTemplatesForCell(
              teacherId, false, false, null, CognitiveLevel.THONG_HIEU, "hàm số", pageable))
          .thenReturn(new PageImpl<>(List.of(template), pageable, 1));

      // ===== ACT =====
      MatchingTemplatesResponse result =
          examMatrixService.listMatchingTemplates(matrixId, " hàm số ", 0, 10, false, false);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getTotalTemplatesFound()),
          () -> assertEquals(1, result.getTemplates().size()),
          () -> assertEquals(templateId, result.getTemplates().getFirst().getTemplateId()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(questionTemplateRepository, times(1))
          .findMatchingTemplatesForCell(
              teacherId, false, false, null, CognitiveLevel.THONG_HIEU, "hàm số", pageable);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, questionTemplateRepository);
    }

    /**
     * Abnormal-like case: Không tìm thấy template nào, response có hint.
     */
    @Test
    void it_should_return_hint_when_no_matching_templates_found() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      PageRequest pageable = PageRequest.of(0, 5);
      when(questionTemplateRepository.findMatchingTemplatesForCell(
              teacherId, true, false, null, null, null, pageable))
          .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

      // ===== ACT =====
      MatchingTemplatesResponse result =
          examMatrixService.listMatchingTemplates(matrixId, " ", 0, 5, true, false);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(0, result.getTotalTemplatesFound()),
          () -> assertTrue(result.getTemplates().isEmpty()),
          () -> assertTrue(result.getHint().contains("No matching templates found")));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(questionTemplateRepository, times(1))
          .findMatchingTemplatesForCell(teacherId, true, false, null, null, null, pageable);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, questionTemplateRepository);
    }
  }

  @Nested
  @DisplayName("matrix row operations")
  class MatrixRowOperationTests {

    /**
     * Normal case: Add row thành công vào matrix DRAFT.
     */
    @Test
    void it_should_add_matrix_row_when_input_row_is_valid() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("7065e4cf-55e7-4ee5-a3ed-4ca4ec4ce2c4");
      UUID bankId = UUID.fromString("af22346d-61f7-4946-8f89-eb5da5f6b436");

      MatrixRowRequest rowRequest =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Bài toán cực trị hàm số")
              .questionDifficulty(QuestionDifficulty.HARD)
              .cells(
                  List.of(
                      MatrixCellRequest.builder()
                          .cognitiveLevel(CognitiveLevel.VAN_DUNG)
                          .questionCount(1)
                          .pointsPerQuestion(new BigDecimal("2.0"))
                          .build()))
              .build();

      QuestionBank bank =
          QuestionBank.builder().teacherId(teacherId).name("Ngân hàng giải tích nâng cao").isPublic(false).build();
      bank.setId(bankId);
      Chapter chapter = Chapter.builder().title("Chương III - Cực trị").orderIndex(3).build();
      chapter.setId(chapterId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
          .thenReturn(Collections.emptyList());
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));

      ExamMatrixRow savedRow =
          ExamMatrixRow.builder()
              .examMatrixId(matrixId)
              .chapterId(chapterId)
              .orderIndex(1)
              .questionTypeName("Bài toán cực trị hàm số")
              .build();
      savedRow.setId(UUID.fromString("1f66c6ab-a9f5-4193-a81c-723f8ea2fa08"));
      when(examMatrixRowRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrixRow.class)))
          .thenReturn(savedRow);
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Nguyen Cong Minh")));

      // ===== ACT =====
      ExamMatrixTableResponse result = examMatrixService.addMatrixRow(matrixId, rowRequest);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(matrixId, result.getId()),
          () -> assertEquals("Nguyen Cong Minh", result.getTeacherName()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRowRepository, times(2)).findByExamMatrixIdOrderByOrderIndex(matrixId);
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(bankId);
      verify(chapterRepository, times(1)).findById(chapterId);
      verify(examMatrixRowRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ExamMatrixRow.class));
      verify(bankMappingRepository, times(1))
          .deleteByExamMatrixIdAndMatrixRowId(matrixId, savedRow.getId());
      verify(bankMappingRepository, times(1))
          .save(org.mockito.ArgumentMatchers.any(ExamMatrixBankMapping.class));
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(userRepository, times(1)).findById(teacherId);
    }

    /**
     * Abnormal case: removeMatrixRow ném lỗi khi row không thuộc matrix.
     */
    @Test
    void it_should_throw_exception_when_removing_row_not_belonging_to_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID rowId = UUID.fromString("bba56ba3-b541-47d7-b319-13f5ab48f74b");
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRowRepository.findById(rowId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> examMatrixService.removeMatrixRow(matrixId, rowId));
      assertEquals(ErrorCode.EXAM_MATRIX_ROW_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRowRepository, times(1)).findById(rowId);
      verifyNoMoreInteractions(examMatrixRepository, examMatrixRowRepository);
    }

    /**
     * Abnormal case: upsertMatrixRowCells ném lỗi khi rowId không tồn tại trong matrix.
     */
    @Test
    void it_should_throw_exception_when_upserting_cells_for_missing_row() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID rowId = UUID.fromString("5f5d22db-e8a6-4aa4-b2dc-8f7de513cf43");
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRowRepository.findById(rowId)).thenReturn(Optional.empty());

      List<MatrixCellRequest> cells =
          List.of(
              MatrixCellRequest.builder()
                  .cognitiveLevel(CognitiveLevel.NHAN_BIET)
                  .questionCount(2)
                  .pointsPerQuestion(new BigDecimal("0.5"))
                  .build());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.upsertMatrixRowCells(matrixId, rowId, cells));
      assertEquals(ErrorCode.EXAM_MATRIX_ROW_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRowRepository, times(1)).findById(rowId);
      verifyNoMoreInteractions(examMatrixRepository, examMatrixRowRepository);
    }

    /**
     * Abnormal case: batchUpsertMatrixRowCells ném lỗi khi một row trong batch không tồn tại.
     */
    @Test
    void it_should_throw_exception_when_batch_upsert_contains_row_not_found() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID existingRowId = UUID.fromString("a0f63571-98c9-453e-bf79-97f651c9648d");
      UUID missingRowId = UUID.fromString("15ceca2a-88b5-4e68-8eb7-5cd178c8b1c5");
      UUID bankId = UUID.fromString("e0a2f695-c92d-4ed1-a02d-1c9d89e71909");

      ExamMatrixRow existingRow =
          ExamMatrixRow.builder().examMatrixId(matrixId).orderIndex(1).build();
      existingRow.setId(existingRowId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
          .thenReturn(List.of(existingRow));

      BatchUpsertMatrixRowCellsRequest request =
          BatchUpsertMatrixRowCellsRequest.builder()
              .rows(
                  List.of(
                      BatchUpsertMatrixRowCellsRequest.RowCellsItem.builder()
                          .rowId(missingRowId)
                          .cells(
                              List.of(
                                  MatrixCellRequest.builder()
                                      .cognitiveLevel(CognitiveLevel.THONG_HIEU)
                                      .questionCount(1)
                                      .pointsPerQuestion(new BigDecimal("1.0"))
                                      .build()))
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.batchUpsertMatrixRowCells(matrixId, request));
      assertEquals(ErrorCode.EXAM_MATRIX_ROW_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(examMatrixRowRepository, times(1)).findByExamMatrixIdOrderByOrderIndex(matrixId);
      verifyNoMoreInteractions(examMatrixRepository, examMatrixRowRepository, bankMappingRepository);
    }
  }

  @Nested
  @DisplayName("additional branch coverage")
  class AdditionalBranchCoverageTests {

    @Test
    void it_should_throw_exception_when_preview_template_is_draft() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("9a93b0bd-b0e1-478d-90b9-f33ca747f64d");
      UUID templateId = UUID.fromString("4f963927-aad6-4bba-ad2a-f341c7460700");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate draftTemplate =
          QuestionTemplate.builder()
              .name("Mẫu nháp chưa xuất bản")
              .status(TemplateStatus.DRAFT)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .build();
      draftTemplate.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId))
          .thenReturn(Optional.of(draftTemplate));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateId).count(1).build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> examMatrixService.generatePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verify(questionTemplateRepository, times(1)).findByIdWithCreator(templateId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, questionTemplateRepository);
    }

    @Test
    void it_should_throw_exception_when_finalizing_mcq_with_invalid_correct_option() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("f26e9f2f-d8a2-42ba-9e75-e4d97df9d70a");
      UUID templateId = UUID.fromString("5e1f94ba-4ec9-42eb-8e42-f65f478a2051");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.THONG_HIEU, 2, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu biến đổi biểu thức")
              .status(TemplateStatus.PUBLISHED)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest.QuestionItem item =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText("Giá trị của 3x khi x=2 là?")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(java.util.Map.of("A", "4", "B", "5", "C", "6", "D", "7"))
              .correctAnswer("E")
              .difficulty(QuestionDifficulty.EASY)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .build();
      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(List.of(item))
              .build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.MCQ_INVALID_CORRECT_OPTION, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verify(questionTemplateRepository, times(1)).findByIdWithCreator(templateId);
    }

    @Test
    void it_should_throw_finalize_empty_questions_when_all_items_are_skipped() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("3de4afcc-c7c9-4f38-8a86-c45f6ab39f42");
      UUID templateId = UUID.fromString("ff372893-39a8-462f-860c-848dc1377b90");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 2, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu phép cộng cơ bản")
              .status(TemplateStatus.PUBLISHED)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest.QuestionItem blankItem =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText(" ")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswer("A")
              .difficulty(QuestionDifficulty.EASY)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .build();
      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(List.of(blankItem))
              .build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.FINALIZE_EMPTY_QUESTIONS, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByIdAndExamMatrixId(mappingId, matrixId);
      verify(questionTemplateRepository, times(1)).findByIdWithCreator(templateId);
    }

    @Test
    void it_should_replace_existing_assessment_questions_when_replace_existing_true() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("128e0688-c37c-4fce-a481-c2d147769d30");
      UUID templateId = UUID.fromString("7470d6a1-c25d-4f99-b1f0-e9092092f555");
      UUID assessmentId = UUID.fromString("7a3565c4-d2cb-4fd2-8da7-ea79d22eeb9d");
      UUID oldQuestionId = UUID.fromString("82fd0465-4510-4730-a312-5f2fa4dc1202");
      UUID newQuestionId = UUID.fromString("3fb04f08-876a-4dd9-b4d9-e181f7d7f776");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.VAN_DUNG, 3, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder()
              .name("Mẫu thay thế câu hỏi")
              .status(TemplateStatus.PUBLISHED)
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .usageCount(5)
              .build();
      template.setId(templateId);

      Assessment assessment = Assessment.builder().teacherId(teacherId).title("KT 1 tiết").build();
      assessment.setId(assessmentId);
      AssessmentQuestion existingAq =
          AssessmentQuestion.builder()
              .assessmentId(assessmentId)
              .questionId(oldQuestionId)
              .orderIndex(1)
              .matrixTemplateMappingId(mappingId)
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId)).thenReturn(List.of(assessment));
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(List.of(existingAq))
          .thenReturn(Collections.emptyList());
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(1);
      when(questionRepository.save(org.mockito.ArgumentMatchers.any(Question.class)))
          .thenAnswer(
              invocation -> {
                Question q = invocation.getArgument(0);
                q.setId(newQuestionId);
                return q;
              });
      when(questionTemplateRepository.save(template)).thenReturn(template);

      FinalizePreviewRequest.QuestionItem validItem =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText("Giải phương trình x + 1 = 4")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswer("C")
              .difficulty(QuestionDifficulty.EASY)
              .cognitiveLevel(CognitiveLevel.VAN_DUNG)
              .build();
      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(true)
              .questions(List.of(validItem))
              .build();

      // ===== ACT =====
      FinalizePreviewResponse result = examMatrixService.finalizePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getSavedCount()),
          () -> assertEquals(1, result.getQuestionIds().size()));

      // ===== VERIFY =====
      verify(assessmentQuestionRepository, times(1))
          .deleteByAssessmentIdAndQuestionIdIn(assessmentId, List.of(oldQuestionId));
      verify(assessmentQuestionRepository, times(1)).save(org.mockito.ArgumentMatchers.any(AssessmentQuestion.class));
    }

    @Test
    void it_should_compute_relevance_score_when_type_and_level_match() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .usageCount(120)
              .build();

      // ===== ACT =====
      int score =
          invokePrivate(
              "computeRelevanceScore",
              new Class<?>[] {QuestionTemplate.class, QuestionType.class, CognitiveLevel.class},
              template,
              QuestionType.MULTIPLE_CHOICE,
              CognitiveLevel.THONG_HIEU);

      // ===== ASSERT =====
      assertEquals(80, score);

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          examMatrixRepository,
          templateMappingRepository,
          questionTemplateRepository,
          bankMappingRepository,
          assessmentRepository);
    }

    @Test
    void it_should_skip_llm_failure_sample_and_continue_generating_preview_candidates() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("9659b12f-00b1-4b38-9618-c29ce75e59c4");
      UUID templateId = UUID.fromString("c1234686-ca20-48a6-ad8b-7f15ca21322c");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.THONG_HIEU, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Mẫu preview lỗi LLM").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      GeneratedQuestionSample llmFailed =
          GeneratedQuestionSample.builder()
              .questionText("[LLM generation failed] timeout")
              .build();
      GeneratedQuestionSample valid =
          GeneratedQuestionSample.builder()
              .questionText("Kết quả của 5 + 4 là?")
              .options(java.util.Map.of("A", "7", "B", "8", "C", "9", "D", "10"))
              .correctAnswer("C")
              .calculatedDifficulty(QuestionDifficulty.EASY)
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(
              org.mockito.ArgumentMatchers.eq(template), org.mockito.ArgumentMatchers.anyInt()))
          .thenReturn(llmFailed)
          .thenReturn(valid);
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateId).count(1).seed(11L).build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getGeneratedCount()),
          () -> assertEquals(1, result.getCandidates().size()));
    }

    @Test
    void it_should_add_difficulty_warning_when_preview_includes_non_requested_difficulty_after_relax_phase() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("10c4be37-fa11-414c-a9df-87f6f8ee8256");
      UUID templateId = UUID.fromString("f1cda3de-5140-4650-ac5f-8c39152a453f");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.VAN_DUNG, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Mẫu mismatch difficulty").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      GeneratedQuestionSample mismatchSample =
          GeneratedQuestionSample.builder()
              .questionText("Bài toán số học nâng cao")
              .options(java.util.Map.of("A", "11", "B", "12", "C", "13", "D", "14"))
              .correctAnswer("B")
              .calculatedDifficulty(QuestionDifficulty.EASY)
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(
              org.mockito.ArgumentMatchers.eq(template), org.mockito.ArgumentMatchers.anyInt()))
          .thenReturn(null)
          .thenReturn(null)
          .thenReturn(null)
          .thenReturn(null)
          .thenReturn(mismatchSample);
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder()
              .templateId(templateId)
              .count(1)
              .difficulty(QuestionDifficulty.HARD)
              .seed(9L)
              .build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.getGeneratedCount()),
          () -> assertTrue(result.getWarnings() != null && !result.getWarnings().isEmpty()),
          () ->
              assertTrue(
                  result.getWarnings().stream()
                      .anyMatch(w -> w.contains("Could not generate enough 'HARD' questions"))));
    }

    @Test
    void it_should_return_overflow_warning_when_append_finalize_exceeds_mapping_target_count() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("111627e3-c96f-4d76-98e6-811cab39c501");
      UUID templateId = UUID.fromString("d7892d2b-2bcd-435f-a818-c088877b2577");
      UUID assessmentId = UUID.fromString("4f559f76-473f-4d9f-af48-90f9ed2fdff2");
      UUID savedQuestionId = UUID.fromString("f8900ad4-c8c6-452b-8d0c-facccdd8f53d");

      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.THONG_HIEU, 3, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Mẫu overflow append").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);
      Assessment assessment = Assessment.builder().teacherId(teacherId).title("KT").build();
      assessment.setId(assessmentId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId)).thenReturn(List.of(assessment));
      when(assessmentQuestionRepository.countByAssessmentIdAndMatrixTemplateMappingId(assessmentId, mappingId))
          .thenReturn(3L);
      when(questionRepository.save(org.mockito.ArgumentMatchers.any(Question.class)))
          .thenAnswer(
              invocation -> {
                Question q = invocation.getArgument(0);
                q.setId(savedQuestionId);
                return q;
              });
      when(questionTemplateRepository.save(template)).thenReturn(template);
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(2);
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(
              List.of(
                  AssessmentQuestion.builder()
                      .assessmentId(assessmentId)
                      .questionId(savedQuestionId)
                      .orderIndex(3)
                      .matrixTemplateMappingId(mappingId)
                      .build()));

      FinalizePreviewRequest.QuestionItem validItem =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText("Tìm nghiệm của x + 2 = 6")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(java.util.Map.of("A", "2", "B", "3", "C", "4", "D", "5"))
              .correctAnswer("C")
              .difficulty(QuestionDifficulty.EASY)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .build();
      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(List.of(validItem))
              .build();

      // ===== ACT =====
      FinalizePreviewResponse result = examMatrixService.finalizePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.getSavedCount()),
          () -> assertTrue(result.getWarnings() != null && !result.getWarnings().isEmpty()),
          () ->
              assertTrue(
                  result.getWarnings().stream()
                      .anyMatch(w -> w.contains("will exceed mapping target"))));
    }
  }

  @Nested
  @DisplayName("status and validation extra branches")
  class StatusAndValidationExtraBranchTests {

    @Test
    void it_should_throw_exception_when_locking_matrix_that_is_already_locked() {
      // ===== ARRANGE =====
      ExamMatrix locked = ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.LOCKED).build();
      locked.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(locked));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> examMatrixService.lockMatrix(matrixId));
      assertEquals(ErrorCode.EXAM_MATRIX_LOCKED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verifyNoMoreInteractions(examMatrixRepository);
    }

    @Test
    void it_should_throw_exception_when_resetting_matrix_not_in_approved_status() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> examMatrixService.resetMatrix(matrixId));
      assertEquals(ErrorCode.MATRIX_NOT_APPROVED_FOR_RESET, exception.getErrorCode());

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verifyNoMoreInteractions(examMatrixRepository);
    }

    @Test
    void it_should_throw_access_denied_when_updating_matrix_owned_by_another_teacher() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID anotherTeacherId = UUID.fromString("88539957-8f0a-4261-b645-d51a3e83f709");
      ExamMatrix anotherOwnerMatrix =
          ExamMatrix.builder().teacherId(anotherTeacherId).status(MatrixStatus.DRAFT).build();
      anotherOwnerMatrix.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(anotherOwnerMatrix));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.updateExamMatrix(matrixId, defaultMatrixRequest));
      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_throw_access_denied_when_deleting_matrix_owned_by_another_teacher() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID anotherTeacherId = UUID.fromString("f5fb08de-59f7-4b8f-9d0a-6f831f420a7c");
      ExamMatrix anotherOwnerMatrix =
          ExamMatrix.builder().teacherId(anotherTeacherId).status(MatrixStatus.DRAFT).build();
      anotherOwnerMatrix.setId(matrixId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(anotherOwnerMatrix));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> examMatrixService.deleteExamMatrix(matrixId));
      assertEquals(ErrorCode.ASSESSMENT_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void it_should_warn_about_missing_targets_and_insufficient_bank_questions_when_validating_matrix() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID bankId = UUID.fromString("e09110ca-7506-4db0-91de-df4b65be4f55");
      ExamMatrix noTargetsMatrix =
          ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.DRAFT).build();
      noTargetsMatrix.setId(matrixId);

      ExamMatrixBankMapping bankMapping =
          buildBankMapping(
              UUID.fromString("f4759f57-b387-4f2a-b873-42a1f4180262"),
              matrixId,
              bankId,
              CognitiveLevel.THONG_HIEU,
              3,
              new BigDecimal("1.0"));

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(noTargetsMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(List.of(bankMapping));
      when(questionRepository.countApprovedByBankAndDifficultyAndCognitive(
              bankId, null, CognitiveLevel.THONG_HIEU))
          .thenReturn(1L);

      // ===== ACT =====
      MatrixValidationReport result = examMatrixService.validateMatrix(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertTrue(result.isCanApprove()),
          () -> assertTrue(result.isAiFallbackLikely()),
          () ->
              assertTrue(
                  result.getWarnings().stream()
                      .anyMatch(w -> w.contains("does not define totalQuestionsTarget"))),
          () ->
              assertTrue(
                  result.getWarnings().stream()
                      .anyMatch(w -> w.contains("does not define totalPointsTarget"))),
          () ->
              assertTrue(
                  result.getWarnings().stream()
                      .anyMatch(w -> w.contains("insufficient APPROVED questions"))));
    }

    @Test
    void it_should_report_mismatch_errors_when_actual_totals_do_not_match_targets() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      ExamMatrix mismatchTargetMatrix =
          ExamMatrix.builder()
              .teacherId(teacherId)
              .status(MatrixStatus.DRAFT)
              .totalQuestionsTarget(10)
              .totalPointsTarget(new BigDecimal("12.0"))
              .build();
      mismatchTargetMatrix.setId(matrixId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId))
          .thenReturn(Optional.of(mismatchTargetMatrix));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(
              List.of(
                  buildTemplateMapping(
                      UUID.fromString("a8a8d4ec-ca0c-4fd1-bdea-98c75131b268"),
                      matrixId,
                      CognitiveLevel.NHAN_BIET,
                      3,
                      new BigDecimal("1.0")),
                  buildTemplateMapping(
                      UUID.fromString("0f5ca36f-f2c8-4f36-931a-e27d5fd2f3f8"),
                      matrixId,
                      CognitiveLevel.THONG_HIEU,
                      4,
                      new BigDecimal("1.0"))));

      // ===== ACT =====
      MatrixValidationReport result = examMatrixService.validateMatrix(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(false, result.isCanApprove()),
          () -> assertEquals(false, result.isQuestionsMatchTarget()),
          () -> assertEquals(false, result.isPointsMatchTarget()),
          () ->
              assertTrue(
                  result.getErrors().stream()
                      .anyMatch(e -> e.contains("Total questions mismatch"))),
          () ->
              assertTrue(
                  result.getErrors().stream().anyMatch(e -> e.contains("Total points mismatch"))));
    }
  }

  @Nested
  @DisplayName("remaining service branch paths")
  class RemainingServiceBranchPathTests {

    @Test
    void it_should_get_exam_matrix_by_assessment_id_when_assessment_and_matrix_exist() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID assessmentId = UUID.fromString("edc7a64d-16f1-4e66-8828-a5fb2648c032");
      Assessment assessment = Assessment.builder().teacherId(teacherId).title("Thi giữa kỳ").build();
      assessment.setId(assessmentId);

      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(assessment));
      when(examMatrixRepository.findByAssessmentIdAndNotDeleted(assessmentId))
          .thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Le Minh Tri")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      ExamMatrixResponse result = examMatrixService.getExamMatrixByAssessmentId(assessmentId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(matrixId, result.getId()),
          () -> assertEquals("Le Minh Tri", result.getTeacherName()));
    }

    @Test
    void it_should_throw_exception_when_matrix_not_found_by_existing_assessment_id() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID assessmentId = UUID.fromString("97ad1ef9-c9d0-4d7f-ae70-bccca1f4a1da");
      Assessment assessment = Assessment.builder().teacherId(teacherId).title("Thi cuối kỳ").build();
      assessment.setId(assessmentId);
      when(assessmentRepository.findByIdAndNotDeleted(assessmentId)).thenReturn(Optional.of(assessment));
      when(examMatrixRepository.findByAssessmentIdAndNotDeleted(assessmentId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.getExamMatrixByAssessmentId(assessmentId));
      assertEquals(ErrorCode.EXAM_MATRIX_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_add_batch_template_mappings_when_all_templates_exist() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID templateA = UUID.fromString("a4ec9f75-0e6b-4f89-bc75-8f828ed1ee29");
      UUID templateB = UUID.fromString("8e94df97-6fee-4fe0-b4bc-7f6576728398");

      AddTemplateMappingRequest a =
          AddTemplateMappingRequest.builder()
              .templateId(templateA)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .questionCount(2)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .build();
      AddTemplateMappingRequest b =
          AddTemplateMappingRequest.builder()
              .templateId(templateB)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .questionCount(3)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .build();
      BatchAddTemplateMappingsRequest request =
          BatchAddTemplateMappingsRequest.builder().mappings(List.of(a, b)).build();

      QuestionTemplate t1 =
          QuestionTemplate.builder().name("Mẫu 1").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      t1.setId(templateA);
      QuestionTemplate t2 =
          QuestionTemplate.builder().name("Mẫu 2").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      t2.setId(templateB);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(questionTemplateRepository.findByIdWithCreator(templateA)).thenReturn(Optional.of(t1));
      when(questionTemplateRepository.findByIdWithCreator(templateB)).thenReturn(Optional.of(t2));
      when(templateMappingRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrixTemplateMapping.class)))
          .thenAnswer(
              invocation -> {
                ExamMatrixTemplateMapping m = invocation.getArgument(0);
                m.setId(UUID.randomUUID());
                return m;
              });

      // ===== ACT =====
      BatchTemplateMappingsResponse result = examMatrixService.addTemplateMappings(matrixId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(2, result.getTotalMappingsAdded()),
          () -> assertEquals(2, result.getAddedMappings().size()));
    }

    @Test
    void it_should_throw_exception_when_batch_template_contains_nonexistent_template() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID templateA = UUID.fromString("24f8ab95-c06e-4d0a-b853-d2af2f191647");
      AddTemplateMappingRequest a =
          AddTemplateMappingRequest.builder()
              .templateId(templateA)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .questionCount(2)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .build();
      BatchAddTemplateMappingsRequest request =
          BatchAddTemplateMappingsRequest.builder().mappings(List.of(a)).build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(questionTemplateRepository.findByIdWithCreator(templateA)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> examMatrixService.addTemplateMappings(matrixId, request));
      assertEquals(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_generate_preview_with_partial_candidates_when_generation_returns_null_and_duplicates() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("37889acd-b815-4dd8-a75b-37ac7332e43e");
      UUID templateId = UUID.fromString("6f79f6b2-7bd0-4cc2-b8d7-5621367ebb0b");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 2, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Mẫu preview lặp").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      GeneratedQuestionSample duplicate =
          GeneratedQuestionSample.builder()
              .questionText("Tính 1 + 1")
              .options(java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswer("B")
              .calculatedDifficulty(QuestionDifficulty.EASY)
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(
              org.mockito.ArgumentMatchers.eq(template), org.mockito.ArgumentMatchers.anyInt()))
          .thenReturn(null)
          .thenReturn(duplicate)
          .thenReturn(duplicate)
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("Tính 2 + 2")
                  .options(java.util.Map.of("A", "2", "B", "3", "C", "4", "D", "5"))
                  .correctAnswer("C")
                  .calculatedDifficulty(QuestionDifficulty.EASY)
                  .build());
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateId).count(2).seed(3L).build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(2, result.getGeneratedCount()),
          () -> assertEquals(2, result.getCandidates().size()));
    }
  }

  @Nested
  @DisplayName("private helper branch coverage")
  class PrivateHelperBranchCoverageTests {

    @Test
    void it_should_throw_not_a_teacher_when_validate_teacher_role_with_non_teacher_user() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("7110a5e0-07a8-43d5-8d58-f904caeb7ca3");
      User nonTeacher =
          User.builder()
              .fullName("Student User")
              .roles(Set.of(Role.builder().name("STUDENT").build()))
              .build();
      when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(nonTeacher));
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken("user", "pwd", Collections.emptyList()));

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () -> invokePrivate("validateTeacherRole", new Class<?>[] {UUID.class}, userId));
      Throwable root = extractRootCause(exception);
      assertTrue(root instanceof AppException);
      assertEquals(ErrorCode.NOT_A_TEACHER, ((AppException) root).getErrorCode());
    }

    @Test
    void it_should_throw_question_bank_access_denied_when_persist_row_uses_private_non_public_bank() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("9fc3ee5e-6b2e-4f1d-8128-57ced7af0bb0");
      UUID bankId = UUID.fromString("9f5de2cb-bb7a-45cd-8fba-5782dcf2f74f");
      UUID otherTeacher = UUID.fromString("cd4726c8-9887-4608-b2ce-713df3f74eb1");

      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Bài toán đạo hàm")
              .build();
      QuestionBank privateBank =
          QuestionBank.builder().teacherId(otherTeacher).name("Bank private").isPublic(false).build();
      privateBank.setId(bankId);
      Chapter chapter = Chapter.builder().subjectId(null).title("Chương đạo hàm").orderIndex(1).build();
      chapter.setId(chapterId);

      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(privateBank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));

      // ===== ACT & ASSERT =====
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "persistRow",
                      new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
                      matrixId,
                      row,
                      1));
      Throwable root = extractRootCause(exception);
      assertTrue(root instanceof AppException);
      assertEquals(
          ErrorCode.QUESTION_BANK_ACCESS_DENIED, ((AppException) root).getErrorCode());
    }

    @Test
    void it_should_build_table_response_with_uncategorized_row_and_curriculum_subject_info() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("ae442f53-f3c6-48d0-b737-7287f31ed4b8");
      UUID subjectId = UUID.fromString("312f4cb5-127f-464a-8ea4-9e964af742eb");
      UUID bankId = UUID.fromString("77dbba1f-63a0-4ea2-acf2-6ffbe33476f2");
      UUID rowId = UUID.fromString("6305d527-dcf8-4458-96ec-1c212147984c");

      ExamMatrix matrix =
          ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.DRAFT).build();
      matrix.setId(matrixId);
      ExamMatrixRow uncategorizedRow =
          ExamMatrixRow.builder()
              .examMatrixId(matrixId)
              .chapterId(null)
              .questionTypeName("Ôn tập tổng hợp")
              .orderIndex(1)
              .build();
      uncategorizedRow.setId(rowId);
      ExamMatrixBankMapping cell =
          buildBankMapping(
              UUID.fromString("ab14b449-32a9-4b8c-a542-f5f8dd44a369"),
              matrixId,
              bankId,
              CognitiveLevel.NHAN_BIET,
              2,
              new BigDecimal("1.0"));
      cell.setMatrixRowId(rowId);

      Curriculum curriculum =
          Curriculum.builder()
              .name("Chương trình GDPT 2018")
              .grade(10)
              .category(CurriculumCategory.GEOMETRY)
              .subjectId(subjectId)
              .build();
      Subject subject = Subject.builder().name("Toán học").code("MATH10").build();

      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
          .thenReturn(List.of(uncategorizedRow));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId)).thenReturn(List.of(cell));
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(curriculum));
      when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
      when(userRepository.findById(teacherId))
          .thenReturn(Optional.of(buildTeacher(teacherId, "Tran Minh Quan")));

      // ===== ACT =====
      ExamMatrixTableResponse response =
          invokePrivate("buildTableResponse", new Class<?>[] {ExamMatrix.class}, matrix);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals("Chương trình GDPT 2018", response.getCurriculumName()),
          () -> assertEquals("Toán học", response.getSubjectName()),
          () -> assertEquals(1, response.getChapters().size()),
          () -> assertEquals(2, response.getGrandTotalQuestions()));
    }

    @Test
    void it_should_populate_subject_and_school_grade_snapshot_when_persist_row_with_subject() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("95b38a44-04bd-4de5-b442-64945597f2e8");
      UUID bankId = UUID.fromString("d4147be1-5296-4fa3-af39-5fa9180f1b5f");
      UUID subjectId = UUID.fromString("42caf4be-28e5-414b-b3c0-d277f72ac2f4");

      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Hàm bậc nhất")
              .questionDifficulty(QuestionDifficulty.EASY)
              .build();
      QuestionBank bank = QuestionBank.builder().teacherId(teacherId).name("Public bank").isPublic(true).build();
      bank.setId(bankId);
      Chapter chapter = Chapter.builder().subjectId(subjectId).title("Chương I").orderIndex(1).build();
      chapter.setId(chapterId);
      Subject subject = Subject.builder().name("Toán").code("MATH").build();
      SchoolGrade grade = SchoolGrade.builder().name("Lớp 10").gradeLevel(10).build();
      subject.setSchoolGrade(grade);

      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
      when(examMatrixRowRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrixRow.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      invokePrivate(
          "persistRow",
          new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
          matrixId,
          row,
          1);

      // ===== ASSERT =====
      verify(examMatrixRowRepository, times(1))
          .save(
              org.mockito.ArgumentMatchers.argThat(
                  saved ->
                      "Toán".equals(saved.getSubjectName())
                          && "Lớp 10".equals(saved.getSchoolGradeName())
                          && "Chương I".equals(saved.getChapterName())));
    }
  }

  @Nested
  @DisplayName("targeted branch boost")
  class TargetedBranchBoostTests {

    @Test
    void it_should_throw_user_not_existed_when_validate_teacher_role_user_missing() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("f89f3bbb-5d26-49c6-aeff-a4f8b3f20e1e");
      SecurityContextHolder.getContext()
          .setAuthentication(new UsernamePasswordAuthenticationToken("u", "p", Collections.emptyList()));
      when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () -> invokePrivate("validateTeacherRole", new Class<?>[] {UUID.class}, userId));
      Throwable root = extractRootCause(ex);
      assertTrue(root instanceof AppException);
      assertEquals(ErrorCode.USER_NOT_EXISTED, ((AppException) root).getErrorCode());
    }

    @Test
    void it_should_throw_question_bank_not_found_when_persist_row_bank_missing() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("48852320-d464-4c5e-a4fe-e785538e6a8f");
      UUID bankId = UUID.fromString("62aa8217-f4a2-4311-af4f-357f51f39a43");
      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Hàm số")
              .build();
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "persistRow",
                      new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
                      matrixId,
                      row,
                      1));
      Throwable root = extractRootCause(ex);
      assertTrue(root instanceof AppException);
      assertEquals(ErrorCode.QUESTION_BANK_NOT_FOUND, ((AppException) root).getErrorCode());
    }

    @Test
    void it_should_throw_chapter_not_found_when_persist_row_chapter_missing() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("2b80da1f-3fd4-4f65-b6d5-f0239074b7ee");
      UUID bankId = UUID.fromString("df17e11c-36ec-45d9-8d86-1dc6f9e98ad4");
      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Dạng bài cực trị")
              .build();
      QuestionBank bank = QuestionBank.builder().teacherId(teacherId).name("B").isPublic(false).build();
      bank.setId(bankId);
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "persistRow",
                      new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
                      matrixId,
                      row,
                      1));
      Throwable root = extractRootCause(ex);
      assertTrue(root instanceof AppException);
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, ((AppException) root).getErrorCode());
    }

    @Test
    void it_should_skip_saving_when_upsert_row_cells_are_empty_or_zero_count() {
      // ===== ARRANGE =====
      UUID rowId = UUID.fromString("6957f7c7-e23b-4135-b6bc-99231c5d3df8");
      ExamMatrixRow row =
          ExamMatrixRow.builder().examMatrixId(matrixId).build();
      row.setId(rowId);

      // ===== ACT =====
      invokePrivate(
          "upsertRowCells",
          new Class<?>[] {UUID.class, ExamMatrixRow.class, List.class, BigDecimal.class},
          matrixId,
          row,
          List.of(MatrixCellRequest.builder().cognitiveLevel(CognitiveLevel.NHAN_BIET).questionCount(0).build()),
          null);

      // ===== VERIFY =====
      verify(bankMappingRepository, times(1)).deleteByExamMatrixIdAndMatrixRowId(matrixId, rowId);
      verify(bankMappingRepository, times(0)).save(org.mockito.ArgumentMatchers.any(ExamMatrixBankMapping.class));
    }

    @Test
    void it_should_use_default_points_per_question_when_cell_points_are_null() {
      // ===== ARRANGE =====
      UUID rowId = UUID.fromString("ab5ee995-d37d-4934-80eb-8c9bb8e2ff0f");
      UUID bankId = UUID.fromString("e5d8cb6d-5ef4-43bc-924c-a4dcca239ff2");
      ExamMatrixRow row =
          ExamMatrixRow.builder().examMatrixId(matrixId).build();
      row.setId(rowId);

      // ===== ACT =====
      invokePrivate(
          "upsertRowCells",
          new Class<?>[] {UUID.class, ExamMatrixRow.class, List.class, BigDecimal.class},
          matrixId,
          row,
          List.of(MatrixCellRequest.builder().cognitiveLevel(CognitiveLevel.THONG_HIEU).questionCount(2).build()),
          new BigDecimal("1.25"));

      // ===== VERIFY =====
      verify(bankMappingRepository, times(1))
          .save(
              org.mockito.ArgumentMatchers.argThat(
                  saved -> new BigDecimal("1.25").compareTo(saved.getPointsPerQuestion()) == 0));
    }

    @Test
    void it_should_warn_when_generate_preview_cannot_reach_requested_count() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("c8d71fd0-40dd-450d-bffd-65a4d9537334");
      UUID templateId = UUID.fromString("d8a95d8e-3f32-4085-9a1a-13df856efc17");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 3, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Mẫu thiếu candidates").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(
              org.mockito.ArgumentMatchers.eq(template), org.mockito.ArgumentMatchers.anyInt()))
          .thenReturn(null);
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateId).count(2).seed(5L).build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0, result.getGeneratedCount()),
          () -> assertTrue(result.getWarnings() != null && !result.getWarnings().isEmpty()),
          () ->
              assertTrue(
                  result.getWarnings().stream()
                      .anyMatch(w -> w.contains("Only 0 of 2 requested questions could be generated"))));
    }

    @Test
    void it_should_cover_compute_relevance_score_branches_with_null_requirements_and_zero_usage() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.ESSAY)
              .cognitiveLevel(CognitiveLevel.NHAN_BIET)
              .usageCount(0)
              .build();

      // ===== ACT =====
      int score =
          invokePrivate(
              "computeRelevanceScore",
              new Class<?>[] {QuestionTemplate.class, QuestionType.class, CognitiveLevel.class},
              template,
              null,
              null);

      // ===== ASSERT =====
      assertEquals(0, score);
    }

    @Test
    void it_should_cover_compute_relevance_score_branch_when_usage_count_is_null() {
      // ===== ARRANGE =====
      QuestionTemplate template =
          QuestionTemplate.builder()
              .templateType(QuestionType.MULTIPLE_CHOICE)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .usageCount(null)
              .build();

      // ===== ACT =====
      int score =
          invokePrivate(
              "computeRelevanceScore",
              new Class<?>[] {QuestionTemplate.class, QuestionType.class, CognitiveLevel.class},
              template,
              QuestionType.MULTIPLE_CHOICE,
              CognitiveLevel.THONG_HIEU);

      // ===== ASSERT =====
      assertEquals(70, score);
    }

    @Test
    void it_should_throw_template_mismatch_when_finalizing_with_different_template_id() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("8f14f8fe-e7e6-4a9c-8f68-452ccda38902");
      UUID mappingTemplateId = UUID.fromString("31a99d5c-9e58-4c45-8623-66547beee2bd");
      UUID requestTemplateId = UUID.fromString("156ab08f-3fe6-4f7b-bb51-7f7fe53203d8");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(mappingTemplateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(requestTemplateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("1+1=?")
                          .questionType(QuestionType.MULTIPLE_CHOICE)
                          .options(java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
                          .correctAnswer("B")
                          .difficulty(QuestionDifficulty.EASY)
                          .cognitiveLevel(CognitiveLevel.NHAN_BIET)
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.TEMPLATE_MAPPING_TEMPLATE_MISMATCH, exception.getErrorCode());
    }

    @Test
    void it_should_throw_template_not_usable_when_finalizing_with_draft_template() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("4f190c95-d958-4e72-97a8-8f369657f5aa");
      UUID templateId = UUID.fromString("e152de43-17c3-4df4-a3ba-07a9cea319c1");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate draft =
          QuestionTemplate.builder().name("Draft").status(TemplateStatus.DRAFT).templateType(QuestionType.MULTIPLE_CHOICE).build();
      draft.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(draft));

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("2+2=?")
                          .questionType(QuestionType.MULTIPLE_CHOICE)
                          .options(java.util.Map.of("A", "2", "B", "3", "C", "4", "D", "5"))
                          .correctAnswer("C")
                          .difficulty(QuestionDifficulty.EASY)
                          .cognitiveLevel(CognitiveLevel.NHAN_BIET)
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, exception.getErrorCode());
    }

    @Test
    void it_should_skip_duplicate_question_text_in_finalize_request_and_still_save_unique_items() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("431462ca-e839-4867-ae5c-6071231965b7");
      UUID templateId = UUID.fromString("41546381-96a3-41a2-82e5-4d3fa2fa5a44");
      UUID savedQuestionId = UUID.fromString("42ce72fd-e7b4-4762-817f-f9f30dcf9282");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.THONG_HIEU, 3, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Unique only").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId))
          .thenReturn(Collections.emptyList());
      when(questionRepository.save(org.mockito.ArgumentMatchers.any(Question.class)))
          .thenAnswer(
              invocation -> {
                Question q = invocation.getArgument(0);
                q.setId(savedQuestionId);
                return q;
              });
      when(questionTemplateRepository.save(template)).thenReturn(template);

      FinalizePreviewRequest.QuestionItem q1 =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText("Giải phương trình x+1=3")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswer("B")
              .difficulty(QuestionDifficulty.EASY)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .build();
      FinalizePreviewRequest.QuestionItem q2 =
          FinalizePreviewRequest.QuestionItem.builder()
              .questionText("Giải phương trình x+1=3")
              .questionType(QuestionType.MULTIPLE_CHOICE)
              .options(java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
              .correctAnswer("B")
              .difficulty(QuestionDifficulty.EASY)
              .cognitiveLevel(CognitiveLevel.THONG_HIEU)
              .build();
      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(List.of(q1, q2))
              .build();

      // ===== ACT =====
      FinalizePreviewResponse result = examMatrixService.finalizePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.getSavedCount()),
          () -> assertTrue(result.getWarnings() != null && !result.getWarnings().isEmpty()));
    }

    @Test
    void it_should_add_matrix_row_with_existing_null_order_index_rows() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("e6bbd2ea-7593-4429-95b2-8f6f6f6f9c30");
      UUID bankId = UUID.fromString("086f4c4d-f66f-4131-b40e-4df8de28fdfe");
      UUID rowId = UUID.fromString("e6fd0b9a-5bdc-44f8-8c68-37a95e0cd1d1");

      ExamMatrixRow existingNull = ExamMatrixRow.builder().examMatrixId(matrixId).orderIndex(null).build();
      existingNull.setId(UUID.fromString("e15b7e07-9cb0-4d0a-95cd-4f5b5977d920"));
      ExamMatrixRow existingThree = ExamMatrixRow.builder().examMatrixId(matrixId).orderIndex(3).build();
      existingThree.setId(UUID.fromString("adf6d237-d45a-4c2a-9f1a-716d258f4f2a"));

      MatrixRowRequest rowRequest =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Cộng trừ đa thức")
              .build();

      QuestionBank bank = QuestionBank.builder().teacherId(teacherId).isPublic(true).name("Bank").build();
      bank.setId(bankId);
      Chapter chapter = Chapter.builder().title("Chương").orderIndex(1).subjectId(null).build();
      chapter.setId(chapterId);
      ExamMatrixRow saved = ExamMatrixRow.builder().examMatrixId(matrixId).orderIndex(4).build();
      saved.setId(rowId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
          .thenReturn(List.of(existingNull, existingThree))
          .thenReturn(List.of(existingNull, existingThree, saved));
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(examMatrixRowRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrixRow.class))).thenReturn(saved);
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId)).thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId, "T")));

      // ===== ACT =====
      examMatrixService.addMatrixRow(matrixId, rowRequest);

      // ===== VERIFY =====
      verify(examMatrixRowRepository, times(1))
          .save(org.mockito.ArgumentMatchers.argThat(r -> r.getOrderIndex() != null && r.getOrderIndex() == 4));
    }

    @Test
    void it_should_batch_upsert_cells_successfully_when_all_rows_exist() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID rowId = UUID.fromString("7a89f708-6161-420f-b879-a4a8f49a0d54");
      UUID bankId = UUID.fromString("67b6b98a-0491-4de8-ba77-a11dfb9658f0");
      ExamMatrixRow row = ExamMatrixRow.builder().examMatrixId(matrixId).orderIndex(1).build();
      row.setId(rowId);

      BatchUpsertMatrixRowCellsRequest request =
          BatchUpsertMatrixRowCellsRequest.builder()
              .rows(
                  List.of(
                      BatchUpsertMatrixRowCellsRequest.RowCellsItem.builder()
                          .rowId(rowId)
                          .cells(
                              List.of(
                                  MatrixCellRequest.builder()
                                      .cognitiveLevel(CognitiveLevel.NHAN_BIET)
                                      .questionCount(2)
                                      .build()))
                          .build()))
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
          .thenReturn(List.of(row))
          .thenReturn(List.of(row));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId)).thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId, "T")));

      // ===== ACT =====
      examMatrixService.batchUpsertMatrixRowCells(matrixId, request);

      // ===== VERIFY =====
      verify(bankMappingRepository, times(1)).deleteByExamMatrixIdAndMatrixRowId(matrixId, rowId);
      verify(bankMappingRepository, times(1)).save(org.mockito.ArgumentMatchers.any(ExamMatrixBankMapping.class));
    }

    @Test
    void it_should_get_my_exam_matrices_paged_with_blank_search_as_null() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      PageRequest pageable = PageRequest.of(0, 5);
      when(examMatrixRepository.findByTeacherIdWithFilters(teacherId, MatrixStatus.DRAFT, null, pageable))
          .thenReturn(new PageImpl<>(List.of(draftMatrix), pageable, 1));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId, "T")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      examMatrixService.getMyExamMatricesPaged("   ", MatrixStatus.DRAFT, pageable);

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1))
          .findByTeacherIdWithFilters(teacherId, MatrixStatus.DRAFT, null, pageable);
    }

    @Test
    void it_should_list_matching_templates_with_default_page_size_when_size_is_non_positive() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(questionTemplateRepository.findMatchingTemplatesForCell(
              teacherId, false, false, null, null, null, PageRequest.of(0, 20)))
          .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

      // ===== ACT =====
      examMatrixService.listMatchingTemplates(matrixId, null, 0, 0, false, false);

      // ===== VERIFY =====
      verify(questionTemplateRepository, times(1))
          .findMatchingTemplatesForCell(
              teacherId, false, false, null, null, null, PageRequest.of(0, 20));
    }

    @Test
    void it_should_cover_build_bank_mapping_response_with_found_and_missing_bank_name() {
      // ===== ARRANGE =====
      UUID bankId = UUID.fromString("66ea2e96-0909-46d8-8e4e-8316ef810490");
      ExamMatrixBankMapping mapping =
          buildBankMapping(
              UUID.fromString("39620953-c2c3-47ef-87a1-9f88cf09dba7"),
              matrixId,
              bankId,
              CognitiveLevel.NHAN_BIET,
              1,
              null);
      QuestionBank bank = QuestionBank.builder().name("Known Bank").teacherId(teacherId).build();
      bank.setId(bankId);

      // ===== ACT =====
      when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank));
      invokePrivate("buildBankMappingResponse", new Class<?>[] {ExamMatrixBankMapping.class}, mapping);
      when(questionBankRepository.findById(bankId)).thenReturn(Optional.empty());
      invokePrivate("buildBankMappingResponse", new Class<?>[] {ExamMatrixBankMapping.class}, mapping);

      // ===== VERIFY =====
      verify(questionBankRepository, times(2)).findById(bankId);
    }

    @Test
    void it_should_handle_generate_preview_when_ai_service_throws_exception_during_attempts() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("ef7f23a6-b610-4530-b09a-f72f30be7887");
      UUID templateId = UUID.fromString("a12f5ecf-6364-4f48-9155-acab95944355");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Exception preview").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(
              org.mockito.ArgumentMatchers.eq(template), org.mockito.ArgumentMatchers.anyInt()))
          .thenThrow(new RuntimeException("LLM temporary error"));
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateId).count(1).seed(1L).build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertEquals(0, result.getGeneratedCount());
    }

    @Test
    void it_should_add_cognitive_mismatch_warning_when_finalize_item_level_differs_from_mapping() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("3af46f57-4467-4033-986e-02d97ed588f2");
      UUID templateId = UUID.fromString("a365e647-c173-4f65-bf81-fcb450406bcb");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 2, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Mismatch warning").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId))
          .thenReturn(Collections.emptyList());
      when(questionRepository.save(org.mockito.ArgumentMatchers.any(Question.class)))
          .thenAnswer(invocation -> {
            Question q = invocation.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
          });
      when(questionTemplateRepository.save(template)).thenReturn(template);

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("5+5=?")
                          .questionType(QuestionType.MULTIPLE_CHOICE)
                          .options(java.util.Map.of("A", "8", "B", "9", "C", "10", "D", "11"))
                          .correctAnswer("C")
                          .difficulty(QuestionDifficulty.EASY)
                          .cognitiveLevel(CognitiveLevel.THONG_HIEU)
                          .build()))
              .build();

      // ===== ACT =====
      FinalizePreviewResponse result = examMatrixService.finalizePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertTrue(result.getWarnings() != null && !result.getWarnings().isEmpty());
    }

    @Test
    void it_should_set_next_order_when_build_matrix_resolves_grade_from_curriculum() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID curriculumId = UUID.fromString("0a2c7ba8-2d52-4954-97af-39411f80fc0c");
      UUID chapterId = UUID.fromString("65aa072b-6c2e-4855-b3e3-1519dafadf17");
      UUID bankId = UUID.fromString("1374a729-b746-4c6b-a883-7e1263397fc4");
      BuildExamMatrixRequest request =
          BuildExamMatrixRequest.builder()
              .name("Build with curriculum grade")
              .questionBankId(bankId)
              .rows(List.of(MatrixRowRequest.builder().chapterId(chapterId).questionTypeName("Dạng 1").build()))
              .build();

      Curriculum curriculum =
          Curriculum.builder().name("CT").grade(11).category(CurriculumCategory.GEOMETRY).build();
      when(curriculumRepository.findByIdAndNotDeleted(curriculumId)).thenReturn(Optional.of(curriculum));
      when(examMatrixRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrix.class)))
          .thenAnswer(inv -> {
            ExamMatrix m = inv.getArgument(0);
            m.setId(matrixId);
            return m;
          });
      QuestionBank bank = QuestionBank.builder().teacherId(teacherId).isPublic(true).name("B").build();
      bank.setId(bankId);
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      Chapter chapter = Chapter.builder().title("Chap").subjectId(null).orderIndex(1).build();
      chapter.setId(chapterId);
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(examMatrixRowRepository.save(org.mockito.ArgumentMatchers.any(ExamMatrixRow.class)))
          .thenAnswer(inv -> {
            ExamMatrixRow r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
          });
      when(examMatrixRowRepository.findByExamMatrixIdOrderByOrderIndex(matrixId))
          .thenReturn(Collections.emptyList());
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId, "T")));

      // ===== ACT =====
      examMatrixService.buildMatrix(request);

      // ===== VERIFY =====
      verify(curriculumRepository, times(2)).findByIdAndNotDeleted(curriculumId);
    }

    @Test
    void it_should_update_matrix_without_overwriting_nullable_targets_when_request_fields_are_null() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      ExamMatrixRequest request =
          ExamMatrixRequest.builder().name("Only rename").description("desc").build();
      Integer oldTarget = draftMatrix.getTotalQuestionsTarget();
      BigDecimal oldPoints = draftMatrix.getTotalPointsTarget();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(examMatrixRepository.save(draftMatrix)).thenReturn(draftMatrix);
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(userRepository.findById(teacherId)).thenReturn(Optional.of(buildTeacher(teacherId, "T")));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      examMatrixService.updateExamMatrix(matrixId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(oldTarget, draftMatrix.getTotalQuestionsTarget()),
          () -> assertEquals(0, oldPoints.compareTo(draftMatrix.getTotalPointsTarget())));
    }

    @Test
    void it_should_finalize_non_mcq_item_with_null_options_and_append_after_existing_order() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("55708448-48d6-47f9-a390-0356e3f65bce");
      UUID templateId = UUID.fromString("6c924f7d-cf88-48e2-bf4d-8b6f9345a398");
      UUID assessmentId = UUID.fromString("41358f85-3d70-4554-a7e4-cf0f72d9a3ff");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 5, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Essay template").status(TemplateStatus.PUBLISHED).templateType(QuestionType.ESSAY).build();
      template.setId(templateId);

      Assessment assessment = Assessment.builder().teacherId(teacherId).examMatrixId(matrixId).build();
      assessment.setId(assessmentId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(assessmentRepository.findByExamMatrixIdAndNotDeleted(matrixId))
          .thenReturn(List.of(assessment))
          .thenReturn(List.of(assessment));
      when(questionRepository.save(org.mockito.ArgumentMatchers.any(Question.class)))
          .thenAnswer(
              invocation -> {
                Question q = invocation.getArgument(0);
                q.setId(UUID.randomUUID());
                return q;
              });
      when(questionTemplateRepository.save(template)).thenReturn(template);
      when(assessmentQuestionRepository.findMaxOrderIndex(assessmentId)).thenReturn(5);
      when(assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(assessmentId))
          .thenReturn(Collections.emptyList());

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .pointsPerQuestion(new BigDecimal("1.0"))
              .replaceExisting(false)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("Phân tích tính đơn điệu của hàm số")
                          .questionType(QuestionType.ESSAY)
                          .options(null)
                          .correctAnswer(null)
                          .difficulty(QuestionDifficulty.MEDIUM)
                          .cognitiveLevel(CognitiveLevel.NHAN_BIET)
                          .build()))
              .build();

      // ===== ACT =====
      FinalizePreviewResponse response = examMatrixService.finalizePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertEquals(1, response.getSavedCount());
      verify(assessmentQuestionRepository, times(1))
          .save(org.mockito.ArgumentMatchers.argThat(aq -> aq.getOrderIndex() != null && aq.getOrderIndex() == 6));
    }

    @Test
    void it_should_throw_finalize_empty_questions_when_all_items_have_blank_text() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("e2f9987f-a721-4f4d-b413-9f26bf79f887");
      UUID templateId = UUID.fromString("4dc05f74-523f-4bbf-bfc9-17b0cf8c58dc");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Blank text").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .replaceExisting(false)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("   ")
                          .questionType(QuestionType.ESSAY)
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.FINALIZE_EMPTY_QUESTIONS, ex.getErrorCode());
    }

    @Test
    void it_should_throw_invalid_options_when_finalize_mcq_options_are_null() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("166e8980-c6be-475e-82bd-66d79109f2c6");
      UUID templateId = UUID.fromString("a042a216-2450-4166-bebc-5ef735e44289");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Null opts").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("Câu hỏi")
                          .questionType(QuestionType.MULTIPLE_CHOICE)
                          .options(null)
                          .correctAnswer("A")
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.MCQ_INVALID_OPTIONS, ex.getErrorCode());
    }

    @Test
    void it_should_throw_invalid_options_when_finalize_mcq_options_have_duplicate_values() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("d03e590c-8354-4371-b8e8-452fdaaf3607");
      UUID templateId = UUID.fromString("ec0df64d-f1c5-462e-a0b8-f4cbfd1cc56f");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Dup opts").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("Câu hỏi")
                          .questionType(QuestionType.MULTIPLE_CHOICE)
                          .options(java.util.Map.of("A", "x", "B", "x", "C", "y", "D", "z"))
                          .correctAnswer("A")
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.MCQ_INVALID_OPTIONS, ex.getErrorCode());
    }

    @Test
    void it_should_throw_invalid_correct_option_when_finalize_mcq_correct_answer_is_null() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("355048b5-94e2-4129-8328-f328dc7ebf32");
      UUID templateId = UUID.fromString("531846ec-2909-4cf3-90ca-5b4ce66662ae");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Null correct").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("Câu hỏi")
                          .questionType(QuestionType.MULTIPLE_CHOICE)
                          .options(java.util.Map.of("A", "1", "B", "2", "C", "3", "D", "4"))
                          .correctAnswer(null)
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.MCQ_INVALID_CORRECT_OPTION, ex.getErrorCode());
    }

    @Test
    void it_should_throw_template_not_found_when_generate_preview_template_is_deleted() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("4f4453e4-0737-4228-b574-5f5b63f53da4");
      UUID templateId = UUID.fromString("b3a1c036-ea08-4a31-a562-1141c80ec94f");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate deletedTemplate =
          QuestionTemplate.builder().name("Deleted").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      deletedTemplate.setId(templateId);
      deletedTemplate.setDeletedAt(Instant.now());

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId))
          .thenReturn(Optional.of(deletedTemplate));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateId).count(1).build();

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> examMatrixService.generatePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_generate_preview_with_locked_matrix_warning_and_null_seed() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("ad71f1ef-c578-443d-9395-a66df1f6f492");
      UUID templateId = UUID.fromString("f8719ae2-6f01-46c7-bfa7-7ed4a6cebbe5");
      ExamMatrix locked = ExamMatrix.builder().teacherId(teacherId).status(MatrixStatus.LOCKED).build();
      locked.setId(matrixId);
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Locked preview").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);
      GeneratedQuestionSample sample = GeneratedQuestionSample.builder()
          .questionText("1+2=?")
          .options(java.util.Map.of("A", "2", "B", "3", "C", "4", "D", "5"))
          .correctAnswer("B")
          .calculatedDifficulty(null)
          .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(locked));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(
              org.mockito.ArgumentMatchers.eq(template), org.mockito.ArgumentMatchers.anyInt()))
          .thenReturn(sample);
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder()
              .templateId(templateId)
              .count(1)
              .difficulty(QuestionDifficulty.EASY)
              .seed(null)
              .build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.getGeneratedCount()),
          () -> assertTrue(result.getWarnings() != null && !result.getWarnings().isEmpty()));
    }

    @Test
    void it_should_persist_row_without_subject_snapshot_when_subject_is_deleted() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("32f76494-4372-4dbd-b5d2-8b68ee56093a");
      UUID bankId = UUID.fromString("27229e0f-2837-486a-86a0-d811fcb48f51");
      UUID subjectId = UUID.fromString("27d41d2f-d229-42f8-bab7-4cfbf0db5f1a");
      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Dạng bài")
              .build();
      QuestionBank bank = QuestionBank.builder().teacherId(teacherId).isPublic(true).name("B").build();
      bank.setId(bankId);
      Chapter chapter = Chapter.builder().title("Chương").subjectId(subjectId).orderIndex(1).build();
      chapter.setId(chapterId);
      Subject deletedSubject = Subject.builder().name("Deleted").build();
      deletedSubject.setId(subjectId);
      deletedSubject.setDeletedAt(Instant.now());

      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(deletedSubject));

      // ===== ACT =====
      invokePrivate(
          "persistRow",
          new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
          matrixId,
          row,
          2);

      // ===== VERIFY =====
      verify(examMatrixRowRepository, times(1))
          .save(
              org.mockito.ArgumentMatchers.argThat(
                  r -> r.getSubjectName() == null && r.getSchoolGradeName() == null));
    }

    @Test
    void it_should_allow_admin_in_validate_owner_or_admin_even_when_not_owner() {
      // ===== ARRANGE =====
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  "admin", "p", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

      // ===== ACT =====
      invokePrivate(
          "validateOwnerOrAdmin",
          new Class<?>[] {UUID.class, UUID.class},
          UUID.fromString("1a695939-4d9b-4f48-ad38-05189dcc431f"),
          UUID.fromString("7587465e-024b-4f8e-bc01-11f04719856b"));
    }

    @Test
    void it_should_return_false_when_has_role_is_called_without_authentication() {
      // ===== ARRANGE =====
      SecurityContextHolder.clearContext();

      // ===== ACT =====
      boolean result = invokePrivate("hasRole", new Class<?>[] {String.class}, PredefinedRole.ADMIN_ROLE);

      // ===== ASSERT =====
      assertEquals(false, result);
    }

    @Test
    void it_should_skip_db_lookup_when_validate_teacher_role_has_teacher_authority() {
      // ===== ARRANGE =====
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  "teacher", "p", List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));

      // ===== ACT =====
      invokePrivate(
          "validateTeacherRole",
          new Class<?>[] {UUID.class},
          UUID.fromString("f5334be8-ce31-40f1-85d4-b35d3eb2a80f"));

      // ===== VERIFY =====
      verify(userRepository, times(0)).findByIdWithRoles(org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void it_should_not_throw_when_validate_not_approved_or_locked_receives_draft() {
      // ===== ARRANGE =====
      ExamMatrix matrix = ExamMatrix.builder().status(MatrixStatus.DRAFT).teacherId(teacherId).build();

      // ===== ACT =====
      invokePrivate("validateNotApprovedOrLocked", new Class<?>[] {ExamMatrix.class}, matrix);
    }

    @Test
    void it_should_build_bank_mapping_response_with_default_points_and_zero_question_count() {
      // ===== ARRANGE =====
      UUID bankId = UUID.fromString("cb2cb6aa-f8c3-4d4b-b164-c7ba5fdf95cd");
      ExamMatrixBankMapping mapping =
          buildBankMapping(
              UUID.fromString("677adf09-7af6-40fc-8857-95b6068e7e31"),
              matrixId,
              bankId,
              CognitiveLevel.THONG_HIEU,
              0,
              null);
      mapping.setQuestionCount(null);
      QuestionBank bank = QuestionBank.builder().name("B-default").teacherId(teacherId).build();
      bank.setId(bankId);
      when(questionBankRepository.findById(bankId)).thenReturn(Optional.of(bank));

      // ===== ACT =====
      invokePrivate(
          "buildBankMappingResponse",
          new Class<?>[] {ExamMatrixBankMapping.class},
          mapping);

      // ===== ASSERT =====
      verify(questionBankRepository, times(1)).findById(bankId);
    }

    @Test
    void it_should_build_mapping_response_using_explicit_total_points_when_present() {
      // ===== ARRANGE =====
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(
              UUID.fromString("4ef7a078-9cb8-4f9f-a811-6402230b6619"),
              matrixId,
              CognitiveLevel.VAN_DUNG,
              3,
              new BigDecimal("2.0"));
      mapping.setTotalPoints(new BigDecimal("9.5"));

      // ===== ACT =====
      TemplateMappingResponse response =
          invokePrivate(
              "buildMappingResponse",
              new Class<?>[] {ExamMatrixTemplateMapping.class, String.class},
              mapping,
              "Template X");

      // ===== ASSERT =====
      assertEquals(0, new BigDecimal("9.5").compareTo(response.getTotalPoints()));
    }

    @Test
    void it_should_throw_invalid_options_when_finalize_mcq_missing_required_keys() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("0549be8f-d57f-4375-98af-06f309f8f65d");
      UUID templateId = UUID.fromString("5d9b1e07-e6c2-4ef2-a53a-039bdca5462d");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Missing keys").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);
      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));

      FinalizePreviewRequest request =
          FinalizePreviewRequest.builder()
              .templateId(templateId)
              .questions(
                  List.of(
                      FinalizePreviewRequest.QuestionItem.builder()
                          .questionText("MCQ thiếu đáp án")
                          .questionType(QuestionType.MULTIPLE_CHOICE)
                          .options(java.util.Map.of("A", "1", "B", "2", "C", "3"))
                          .correctAnswer("A")
                          .build()))
              .build();

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> examMatrixService.finalizePreview(matrixId, mappingId, request));
      assertEquals(ErrorCode.MCQ_INVALID_OPTIONS, ex.getErrorCode());
    }

    @Test
    void it_should_return_false_when_has_role_authorities_do_not_match() {
      // ===== ARRANGE =====
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken(
                  "u", "p", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));

      // ===== ACT =====
      boolean result =
          invokePrivate("hasRole", new Class<?>[] {String.class}, PredefinedRole.ADMIN_ROLE);

      // ===== ASSERT =====
      assertEquals(false, result);
    }

    @Test
    void it_should_accept_admin_role_from_db_when_validate_teacher_role_fallback_checks_roles() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("af8c2ad2-3ea2-4aab-b435-08a6a2165ca6");
      SecurityContextHolder.getContext()
          .setAuthentication(new UsernamePasswordAuthenticationToken("u", "p", Collections.emptyList()));
      Role adminRole = Role.builder().name(PredefinedRole.ADMIN_ROLE).build();
      User adminUser = User.builder().fullName("Admin").roles(new java.util.HashSet<>(Set.of(adminRole))).build();
      adminUser.setId(userId);
      when(userRepository.findByIdWithRoles(userId)).thenReturn(Optional.of(adminUser));

      // ===== ACT =====
      invokePrivate("validateTeacherRole", new Class<?>[] {UUID.class}, userId);
    }

    @Test
    void it_should_throw_required_error_when_persist_row_question_type_name_is_blank() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(UUID.fromString("eeef0d3b-c4e0-4d0d-ab96-bdcabf990b24"))
              .questionTypeName("   ")
              .build();

      // ===== ACT & ASSERT =====
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "persistRow",
                      new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
                      matrixId,
                      row,
                      1));
      Throwable root = extractRootCause(ex);
      assertTrue(root instanceof AppException);
      assertEquals(ErrorCode.MATRIX_ROW_QUESTION_TYPE_REQUIRED, ((AppException) root).getErrorCode());
    }

    @Test
    void it_should_throw_chapter_not_found_when_persist_row_chapter_is_soft_deleted() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("d0deec0d-9414-4747-ae16-53deedecf16f");
      UUID bankId = UUID.fromString("de5ab05d-8334-430b-b1f2-440fd7d3ccb8");
      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Dạng bài")
              .build();
      QuestionBank bank = QuestionBank.builder().teacherId(teacherId).name("B").isPublic(false).build();
      bank.setId(bankId);
      Chapter deletedChapter = Chapter.builder().title("Deleted").build();
      deletedChapter.setId(chapterId);
      deletedChapter.setDeletedAt(Instant.now());
      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(deletedChapter));

      // ===== ACT & ASSERT =====
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "persistRow",
                      new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
                      matrixId,
                      row,
                      1));
      Throwable root = extractRootCause(ex);
      assertTrue(root instanceof AppException);
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, ((AppException) root).getErrorCode());
    }

    @Test
    void it_should_persist_subject_name_with_null_school_grade_when_subject_has_no_grade() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID chapterId = UUID.fromString("2646efdb-6ef0-4eb7-ba72-b3020c90238e");
      UUID bankId = UUID.fromString("2556fcc3-5f42-4f48-a17a-e8f3f06f9f77");
      UUID subjectId = UUID.fromString("e4c62808-5f51-4263-b58f-5f38a99de97e");
      MatrixRowRequest row =
          MatrixRowRequest.builder()
              .chapterId(chapterId)
              .questionTypeName("Dạng chuẩn")
              .build();

      QuestionBank bank = QuestionBank.builder().teacherId(teacherId).name("Bank").isPublic(true).build();
      bank.setId(bankId);
      Chapter chapter = Chapter.builder().title("Chapter").subjectId(subjectId).build();
      chapter.setId(chapterId);
      Subject subject = Subject.builder().name("Toán").build();
      subject.setId(subjectId);

      when(questionBankRepository.findByIdAndNotDeleted(bankId)).thenReturn(Optional.of(bank));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

      // ===== ACT =====
      invokePrivate(
          "persistRow",
          new Class<?>[] {UUID.class, MatrixRowRequest.class, int.class},
          matrixId,
          row,
          3);

      // ===== ASSERT =====
      verify(examMatrixRowRepository, times(1))
          .save(
              org.mockito.ArgumentMatchers.argThat(
                  r -> "Toán".equals(r.getSubjectName()) && r.getSchoolGradeName() == null));
    }

    @Test
    void it_should_skip_null_text_sample_then_generate_valid_preview_candidate() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);
      UUID mappingId = UUID.fromString("df8b22f4-ef95-4c00-94df-1839ce8be915");
      UUID templateId = UUID.fromString("c042c9d4-39e1-40be-84ea-b62b47f3d965");
      ExamMatrixTemplateMapping mapping =
          buildTemplateMapping(mappingId, matrixId, CognitiveLevel.NHAN_BIET, 1, new BigDecimal("1.0"));
      mapping.setTemplateId(templateId);
      QuestionTemplate template =
          QuestionTemplate.builder().name("Null-text sample").status(TemplateStatus.PUBLISHED).templateType(QuestionType.MULTIPLE_CHOICE).build();
      template.setId(templateId);
      GeneratedQuestionSample emptyText = GeneratedQuestionSample.builder().questionText(null).build();
      GeneratedQuestionSample valid =
          GeneratedQuestionSample.builder()
              .questionText("2+3=?")
              .options(java.util.Map.of("A", "4", "B", "5", "C", "6", "D", "7"))
              .correctAnswer("B")
              .build();

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(templateMappingRepository.findByIdAndExamMatrixId(mappingId, matrixId))
          .thenReturn(Optional.of(mapping));
      when(questionTemplateRepository.findByIdWithCreator(templateId)).thenReturn(Optional.of(template));
      when(aiEnhancementService.generateQuestion(
              org.mockito.ArgumentMatchers.eq(template), org.mockito.ArgumentMatchers.anyInt()))
          .thenReturn(emptyText)
          .thenReturn(valid);
      when(questionTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

      GeneratePreviewRequest request =
          GeneratePreviewRequest.builder().templateId(templateId).count(1).seed(1L).build();

      // ===== ACT =====
      PreviewCandidatesResponse result = examMatrixService.generatePreview(matrixId, mappingId, request);

      // ===== ASSERT =====
      assertEquals(1, result.getGeneratedCount());
    }
  }

  @Nested
  @DisplayName("validateMatrix()")
  class ValidateMatrixTests {

    /**
     * Abnormal case: Báo lỗi khi matrix không có mapping nào và tổng mục tiêu không hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>matrix.totalQuestionsTarget: 0</li>
     *   <li>matrix.totalPointsTarget: 0</li>
     *   <li>templateMappings: rỗng, bankMappings: rỗng</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>mappings.isEmpty() && bankMappings.isEmpty() → TRUE branch</li>
     *   <li>totalQuestions == 0 → TRUE branch</li>
     *   <li>totalPoints &lt;= 0 → TRUE branch</li>
     *   <li>target validations với giá trị không hợp lệ (<=0) → TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@code canApprove = false} và danh sách errors có đầy đủ lỗi trọng yếu</li>
     * </ul>
     */
    @Test
    void it_should_return_errors_when_matrix_has_no_mappings_and_invalid_targets() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);

      ExamMatrix invalidTargetMatrix =
          ExamMatrix.builder()
              .teacherId(teacherId)
              .status(MatrixStatus.DRAFT)
              .totalQuestionsTarget(0)
              .totalPointsTarget(BigDecimal.ZERO)
              .build();
      invalidTargetMatrix.setId(matrixId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId))
          .thenReturn(Optional.of(invalidTargetMatrix));
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());

      // ===== ACT =====
      MatrixValidationReport result = examMatrixService.validateMatrix(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(false, result.isCanApprove()),
          () -> assertTrue(result.getErrors().contains("Matrix has no mappings. Add at least one bank mapping.")),
          () ->
              assertTrue(
                  result
                      .getErrors()
                      .contains("Total question count across all mappings must be greater than 0.")),
          () ->
              assertTrue(
                  result
                      .getErrors()
                      .contains("Total points across all mappings must be greater than 0.")),
          () ->
              assertTrue(
                  result.getErrors().contains("Total questions target must be greater than 0.")),
          () -> assertTrue(result.getErrors().contains("Total points target must be greater than 0.")));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, bankMappingRepository);
    }

    /**
     * Normal case: Matrix hợp lệ khi tổng số câu, tổng điểm và độ phủ cognitive level đều đạt.
     *
     * <p>Input:
     * <ul>
     *   <li>3 template mappings với cognitive level khác nhau và tổng 10 câu, 10 điểm</li>
     *   <li>totalQuestionsTarget = 10, totalPointsTarget = 10.0</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>mappings.isEmpty() && bankMappings.isEmpty() → FALSE branch</li>
     *   <li>questionsMatchTarget và pointsMatchTarget → TRUE branch</li>
     *   <li>distinctLevels &gt;= 3 → allCognitiveLevelsCovered TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@code canApprove = true}, không có error, và map coverage có đủ 3 mức độ nhận thức</li>
     * </ul>
     */
    @Test
    void it_should_return_approvable_report_when_totals_and_cognitive_coverage_match_targets() {
      // ===== ARRANGE =====
      authenticateAsTeacher(teacherId);

      when(examMatrixRepository.findByIdAndNotDeleted(matrixId)).thenReturn(Optional.of(draftMatrix));
      when(bankMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(Collections.emptyList());
      when(templateMappingRepository.findByExamMatrixIdOrderByCreatedAt(matrixId))
          .thenReturn(
              List.of(
                  buildTemplateMapping(
                      UUID.fromString("40fc6dc7-1f2b-455f-b8a3-a3e1e9ca0f6a"),
                      matrixId,
                      CognitiveLevel.NHAN_BIET,
                      3,
                      new BigDecimal("1.0")),
                  buildTemplateMapping(
                      UUID.fromString("41fc6dc7-1f2b-455f-b8a3-a3e1e9ca0f6a"),
                      matrixId,
                      CognitiveLevel.THONG_HIEU,
                      3,
                      new BigDecimal("1.0")),
                  buildTemplateMapping(
                      UUID.fromString("42fc6dc7-1f2b-455f-b8a3-a3e1e9ca0f6a"),
                      matrixId,
                      CognitiveLevel.VAN_DUNG,
                      4,
                      new BigDecimal("1.0"))));

      // ===== ACT =====
      MatrixValidationReport result = examMatrixService.validateMatrix(matrixId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertTrue(result.isCanApprove()),
          () -> assertTrue(result.getErrors().isEmpty()),
          () -> assertEquals(10, result.getTotalQuestions()),
          () -> assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalPoints())),
          () -> assertTrue(result.isAllCognitiveLevelsCovered()),
          () -> assertEquals(3, result.getCognitiveLevelCoverage().size()));

      // ===== VERIFY =====
      verify(examMatrixRepository, times(1)).findByIdAndNotDeleted(matrixId);
      verify(templateMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verify(bankMappingRepository, times(1)).findByExamMatrixIdOrderByCreatedAt(matrixId);
      verifyNoMoreInteractions(examMatrixRepository, templateMappingRepository, bankMappingRepository);
    }
  }
}
