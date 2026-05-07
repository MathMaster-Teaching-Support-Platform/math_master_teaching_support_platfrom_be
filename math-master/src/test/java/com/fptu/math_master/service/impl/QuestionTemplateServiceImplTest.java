package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AIGenerateTemplatesRequest;
import com.fptu.math_master.dto.request.GenerateCanonicalQuestionsRequest;
import com.fptu.math_master.dto.request.GenerateTemplateQuestionsRequest;
import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.AIGeneratedTemplatesResponse;
import com.fptu.math_master.dto.response.GeneratedQuestionSample;
import com.fptu.math_master.dto.response.GeneratedQuestionsBatchResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.entity.CanonicalQuestion;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionGenerationMode;
import com.fptu.math_master.enums.QuestionTag;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.enums.TemplateStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CanonicalQuestionRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.service.AIEnhancementService;
import com.fptu.math_master.service.BlueprintService;
import com.fptu.math_master.service.GeminiService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("QuestionTemplateServiceImpl - Tests")
class QuestionTemplateServiceImplTest extends BaseUnitTest {

  @InjectMocks private QuestionTemplateServiceImpl questionTemplateService;

  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private AIEnhancementService aiEnhancementService;
  @Mock private GeminiService geminiService;
  @Mock private QuestionRepository questionRepository;
  @Mock private QuestionBankRepository questionBankRepository;
  @Mock private CanonicalQuestionRepository canonicalQuestionRepository;
  @Mock private BlueprintService blueprintService;

  private static final UUID OWNER_ID = UUID.fromString("4a000000-0000-0000-0000-000000000010");
  private static final UUID OTHER_USER_ID = UUID.fromString("4a000000-0000-0000-0000-000000000020");
  private static final UUID ADMIN_ID = UUID.fromString("4a000000-0000-0000-0000-000000000030");
  private static final UUID TEMPLATE_ID = UUID.fromString("4a000000-0000-0000-0000-000000000001");
  private static final UUID BANK_ID = UUID.fromString("4a000000-0000-0000-0000-0000000000b1");
  private static final UUID CANONICAL_ID = UUID.fromString("4a000000-0000-0000-0000-0000000000c1");
  private static final UUID LESSON_ID = UUID.fromString("4a000000-0000-0000-0000-0000000000e1");

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void mockJwtTeacher(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("question-template-teacher-token")
            .header("alg", "none")
            .subject(userId.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder
        .getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.TEACHER_ROLE))));
  }

  private void mockJwtAdmin(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("question-template-admin-token")
            .header("alg", "none")
            .subject(userId.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder
        .getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.ADMIN_ROLE))));
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = QuestionTemplateServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return (T) method.invoke(questionTemplateService, args);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException re) {
        throw re;
      }
      throw new RuntimeException(cause);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private QuestionTemplateRequest buildValidQuestionTemplateRequest(UUID questionBankId) {
    Map<String, Object> templateText =
        Map.of("vi", "Tìm nghiệm phương trình khi hệ số a = {{coefficient}}.");
    Map<String, Object> parameters =
        Map.of(
            "coefficient",
            Map.of("type", "integer", "min", 1, "max", 12));
    return QuestionTemplateRequest.builder()
        .name("Mẫu phương trình bậc nhất tham số hóa")
        .description("Sinh câu hỏi trắc nghiệm theo hệ số nguyên dương trong khoảng cho trước.")
        .templateType(QuestionType.MULTIPLE_CHOICE)
        .templateText(templateText)
        .parameters(parameters)
        .answerFormula("coefficient * 2 + 1")
        .cognitiveLevel(CognitiveLevel.APPLY)
        .tags(List.of(QuestionTag.LINEAR_EQUATIONS, QuestionTag.FUNCTIONS))
        .questionBankId(questionBankId)
        .canonicalQuestionId(null)
        .isPublic(Boolean.FALSE)
        .build();
  }

  private QuestionBank buildQuestionBank(UUID id, UUID teacherId, boolean isPublic) {
    QuestionBank bank = new QuestionBank();
    bank.setId(id);
    bank.setTeacherId(teacherId);
    bank.setIsPublic(isPublic);
    bank.setName("Ngân hàng đề Giải tích lớp 12");
    return bank;
  }

  private QuestionTemplate buildQuestionTemplate(
      UUID id,
      UUID createdBy,
      TemplateStatus status,
      Boolean isPublic,
      Instant deletedAt) {
    QuestionTemplate template =
        QuestionTemplate.builder()
            .name("Mẫu đạo hàm cơ bản")
            .description("Ứng dụng quy tắc chuỗi cho hàm hợp đa thức.")
            .templateType(QuestionType.MULTIPLE_CHOICE)
            .templateText(Map.of("vi", "Tính đạo hàm tại điểm {{x}}."))
            .parameters(Map.of("x", Map.of("type", "integer", "min", 1, "max", 10)))
            .answerFormula("2 * x")
            .cognitiveLevel(CognitiveLevel.UNDERSTAND)
            .tags(List.of(QuestionTag.DERIVATIVES))
            .isPublic(isPublic != null ? isPublic : false)
            .status(status)
            .usageCount(3)
            .questionBankId(BANK_ID)
            .canonicalQuestionId(CANONICAL_ID)
            .build();
    template.setId(id);
    template.setCreatedBy(createdBy);
    template.setDeletedAt(deletedAt);
    template.setCreatedAt(Instant.parse("2025-01-10T08:00:00Z"));
    template.setUpdatedAt(Instant.parse("2025-01-11T09:30:00Z"));
    template.setAvgSuccessRate(new BigDecimal("72.50"));
    return template;
  }

  private void stubSaveTemplateReturnsSelf() {
    when(questionTemplateRepository.save(any(QuestionTemplate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Nested
  @DisplayName("createQuestionTemplate()")
  class CreateQuestionTemplateTests {

    /**
     * Normal case: tạo mẫu câu hỏi khi không gán ngân hàng và cú pháp hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>questionBankId: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>bankId == null → không gọi validateCanUseQuestionBank</li>
     *   <li>validationErrors rỗng → không ném INVALID_TEMPLATE_SYNTAX</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về {@link QuestionTemplateResponse} có tên trùng request</li>
     * </ul>
     */
    @Test
    void it_should_create_template_when_bank_id_is_null_and_syntax_is_valid() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(null);
      stubSaveTemplateReturnsSelf();

      QuestionTemplateResponse result = questionTemplateService.createQuestionTemplate(request);

      assertNotNull(result);
      assertEquals(request.getName(), result.getName());
      assertEquals(TemplateStatus.DRAFT, result.getStatus());
      verify(questionTemplateRepository, times(1)).save(any(QuestionTemplate.class));
      verify(questionBankRepository, never()).findByIdAndNotDeleted(any());
      verifyNoMoreInteractions(questionBankRepository);
    }

    /**
     * Normal case: tạo mẫu khi gán ngân hàng công khai mà giáo viên được phép dùng.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>bankId != null → gọi validateCanUseQuestionBank</li>
     *   <li>ngân hàng public → không ném QUESTION_BANK_ACCESS_DENIED</li>
     * </ul>
     */
    @Test
    void it_should_create_template_when_public_bank_is_used() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(BANK_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID))
          .thenReturn(Optional.of(buildQuestionBank(BANK_ID, OTHER_USER_ID, true)));
      stubSaveTemplateReturnsSelf();

      QuestionTemplateResponse result = questionTemplateService.createQuestionTemplate(request);

      assertNotNull(result);
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionTemplateRepository, times(1)).save(any(QuestionTemplate.class));
    }

    /**
     * Abnormal case: cú pháp mẫu có placeholder chưa khai báo trong parameters.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Ném {@link AppException} với {@link ErrorCode#INVALID_TEMPLATE_SYNTAX}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_placeholder_missing_in_parameters() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(null);
      request.setTemplateText(Map.of("vi", "Giá trị biểu thức với {{missing}} là bao nhiêu?"));

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.createQuestionTemplate(request));

      assertEquals(ErrorCode.INVALID_TEMPLATE_SYNTAX, ex.getErrorCode());
      verify(questionTemplateRepository, never()).save(any());
    }

    /**
     * Abnormal case: không có thẻ phân loại.
     */
    @Test
    void it_should_throw_when_tags_are_empty() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(null);
      request.setTags(List.of());

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.createQuestionTemplate(request));

      assertEquals(ErrorCode.TAGS_REQUIRED, ex.getErrorCode());
    }

    /**
     * Abnormal case: vượt quá năm thẻ phân loại.
     */
    @Test
    void it_should_throw_when_more_than_five_tags() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(null);
      request.setTags(
          List.of(
              QuestionTag.LINEAR_EQUATIONS,
              QuestionTag.QUADRATIC_EQUATIONS,
              QuestionTag.INEQUALITIES,
              QuestionTag.POLYNOMIALS,
              QuestionTag.RATIONAL_EXPRESSIONS,
              QuestionTag.EXPONENTS_LOGARITHMS));

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.createQuestionTemplate(request));

      assertEquals(ErrorCode.TOO_MANY_TAGS, ex.getErrorCode());
    }

    /**
     * Abnormal case: ngân hàng đề không tồn tại.
     */
    @Test
    void it_should_throw_when_question_bank_not_found() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(BANK_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.createQuestionTemplate(request));

      assertEquals(ErrorCode.QUESTION_BANK_NOT_FOUND, ex.getErrorCode());
    }

    /**
     * Abnormal case: ngân hàng riêng tư của giáo viên khác.
     */
    @Test
    void it_should_throw_when_private_bank_belongs_to_another_teacher() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(BANK_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID))
          .thenReturn(Optional.of(buildQuestionBank(BANK_ID, OTHER_USER_ID, false)));

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.createQuestionTemplate(request));

      assertEquals(ErrorCode.QUESTION_BANK_ACCESS_DENIED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("updateQuestionTemplate()")
  class UpdateQuestionTemplateTests {

    /**
     * Normal case: chủ sở hữu cập nhật mẫu ở trạng thái DRAFT.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Lưu repository và trả về tên mới</li>
     * </ul>
     */
    @Test
    void it_should_update_template_when_owner_and_draft() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(BANK_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID))
          .thenReturn(Optional.of(buildQuestionBank(BANK_ID, OWNER_ID, false)));

      QuestionTemplateResponse result =
          questionTemplateService.updateQuestionTemplate(TEMPLATE_ID, request);

      assertEquals(request.getName(), result.getName());
      verify(questionTemplateRepository, times(1)).save(existing);
    }

    /**
     * Normal case: bỏ gán ngân hàng khi request gửi questionBankId null.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trường questionBankId trên entity được đặt null</li>
     * </ul>
     */
    @Test
    void it_should_clear_bank_when_question_bank_id_is_null() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(null);

      questionTemplateService.updateQuestionTemplate(TEMPLATE_ID, request);

      assertNull(existing.getQuestionBankId());
      verify(questionBankRepository, never()).findByIdAndNotDeleted(any());
    }

    /**
     * Abnormal case: không cho sửa mẫu đã PUBLISHED.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Ném {@link AppException} với {@link ErrorCode#TEMPLATE_ALREADY_PUBLISHED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_template_published() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      QuestionTemplateRequest request = buildValidQuestionTemplateRequest(null);

      AppException ex =
          assertThrows(
              AppException.class,
              () -> questionTemplateService.updateQuestionTemplate(TEMPLATE_ID, request));

      assertEquals(ErrorCode.TEMPLATE_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    /**
     * Abnormal case: người dùng không phải chủ sở hữu và không phải admin.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Ném {@link AppException} với {@link ErrorCode#TEMPLATE_ACCESS_DENIED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_non_owner_updates() {
      mockJwtTeacher(OTHER_USER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionTemplateService.updateQuestionTemplate(
                      TEMPLATE_ID, buildValidQuestionTemplateRequest(null)));

      assertEquals(ErrorCode.TEMPLATE_ACCESS_DENIED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("deleteQuestionTemplate()")
  class DeleteQuestionTemplateTests {

    /**
     * Normal case: xóa mềm mẫu DRAFT.
     *
     * <p>Expectation:
     * <ul>
     *   <li>deletedAt được gán thời điểm hiện tại</li>
     * </ul>
     */
    @Test
    void it_should_soft_delete_when_template_is_draft() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();

      questionTemplateService.deleteQuestionTemplate(TEMPLATE_ID);

      assertNotNull(existing.getDeletedAt());
      verify(questionTemplateRepository, times(1)).save(existing);
    }

    /**
     * Normal case: mẫu PUBLISHED bị chuyển sang ARCHIVED thay vì soft-delete.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trạng thái ARCHIVED và không đặt deletedAt</li>
     * </ul>
     */
    @Test
    void it_should_archive_when_template_is_published() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();

      questionTemplateService.deleteQuestionTemplate(TEMPLATE_ID);

      assertEquals(TemplateStatus.ARCHIVED, existing.getStatus());
      assertNull(existing.getDeletedAt());
    }
  }

  @Nested
  @DisplayName("publishTemplate() / unpublishTemplate() / archiveTemplate()")
  class PublishFlowTests {

    /**
     * Normal case: xuất bản mẫu DRAFT.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trạng thái PUBLISHED sau khi lưu</li>
     * </ul>
     */
    @Test
    void it_should_publish_when_draft() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();

      QuestionTemplateResponse result = questionTemplateService.publishTemplate(TEMPLATE_ID);

      assertEquals(TemplateStatus.PUBLISHED, result.getStatus());
    }

    /**
     * Abnormal case: publish khi đã PUBLISHED.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_ALREADY_PUBLISHED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_publish_on_already_published() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      AppException ex =
          assertThrows(AppException.class, () -> questionTemplateService.publishTemplate(TEMPLATE_ID));

      assertEquals(ErrorCode.TEMPLATE_ALREADY_PUBLISHED, ex.getErrorCode());
    }

    /**
     * Abnormal case: publish khi mẫu đã ARCHIVED.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_ALREADY_ARCHIVED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_publish_on_archived() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.ARCHIVED, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      AppException ex =
          assertThrows(AppException.class, () -> questionTemplateService.publishTemplate(TEMPLATE_ID));

      assertEquals(ErrorCode.TEMPLATE_ALREADY_ARCHIVED, ex.getErrorCode());
    }

    /**
     * Normal case: gỡ xuất bản mẫu PUBLISHED về DRAFT.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trạng thái DRAFT</li>
     * </ul>
     */
    @Test
    void it_should_unpublish_when_published() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();

      QuestionTemplateResponse result = questionTemplateService.unpublishTemplate(TEMPLATE_ID);

      assertEquals(TemplateStatus.DRAFT, result.getStatus());
    }

    /**
     * Abnormal case: unpublish khi chưa PUBLISHED.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_NOT_PUBLISHED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_unpublish_non_published() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.unpublishTemplate(TEMPLATE_ID));

      assertEquals(ErrorCode.TEMPLATE_NOT_PUBLISHED, ex.getErrorCode());
    }

    /**
     * Normal case: lưu trữ mẫu chưa ở trạng thái ARCHIVED.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trạng thái ARCHIVED</li>
     * </ul>
     */
    @Test
    void it_should_archive_when_not_archived() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();

      QuestionTemplateResponse result = questionTemplateService.archiveTemplate(TEMPLATE_ID);

      assertEquals(TemplateStatus.ARCHIVED, result.getStatus());
    }

    /**
     * Abnormal case: lưu trữ mẫu đã ARCHIVED.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_ALREADY_ARCHIVED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_archive_already_archived() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.ARCHIVED, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      AppException ex =
          assertThrows(AppException.class, () -> questionTemplateService.archiveTemplate(TEMPLATE_ID));

      assertEquals(ErrorCode.TEMPLATE_ALREADY_ARCHIVED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("getQuestionTemplateById()")
  class GetByIdTests {

    /**
     * Normal case: chủ sở hữu xem mẫu riêng tư.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về response có đúng id</li>
     * </ul>
     */
    @Test
    void it_should_return_template_when_owner_views_private_template() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      QuestionTemplateResponse result = questionTemplateService.getQuestionTemplateById(TEMPLATE_ID);

      assertEquals(TEMPLATE_ID, result.getId());
    }

    /**
     * Normal case: admin xem mẫu riêng tư của giáo viên khác.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Truy cập được và trả về id mẫu</li>
     * </ul>
     */
    @Test
    void it_should_return_template_when_admin_views_private_template() {
      mockJwtAdmin(ADMIN_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      QuestionTemplateResponse result = questionTemplateService.getQuestionTemplateById(TEMPLATE_ID);

      assertEquals(TEMPLATE_ID, result.getId());
    }

    /**
     * Normal case: người khác xem mẫu công khai.
     *
     * <p>Expectation:
     * <ul>
     *   <li>isPublic true trên response</li>
     * </ul>
     */
    @Test
    void it_should_return_public_template_when_non_owner() {
      mockJwtTeacher(OTHER_USER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      QuestionTemplateResponse result = questionTemplateService.getQuestionTemplateById(TEMPLATE_ID);

      assertEquals(Boolean.TRUE, result.getIsPublic());
    }

    /**
     * Abnormal case: người không phải chủ xem mẫu riêng tư.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_ACCESS_DENIED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_stranger_views_private_template() {
      mockJwtTeacher(OTHER_USER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.getQuestionTemplateById(TEMPLATE_ID));

      assertEquals(ErrorCode.TEMPLATE_ACCESS_DENIED, ex.getErrorCode());
    }

    /**
     * Normal case: map tên người tạo khi quan hệ creator được fetch.
     *
     * <p>Expectation:
     * <ul>
     *   <li>creatorName khớp fullName</li>
     * </ul>
     */
    @Test
    void it_should_map_creator_name_when_creator_present() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      User creator = new User();
      creator.setFullName("Trần Thị Minh Châu");
      existing.setCreator(creator);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      QuestionTemplateResponse result = questionTemplateService.getQuestionTemplateById(TEMPLATE_ID);

      assertEquals("Trần Thị Minh Châu", result.getCreatorName());
    }
  }

  @Nested
  @DisplayName("list & search")
  class ListAndSearchTests {

    /**
     * Normal case: phân trang mẫu của tôi.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Page trả về đúng tổng phần tử</li>
     * </ul>
     */
    @Test
    void it_should_page_my_templates() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate row = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      Pageable pageable = PageRequest.of(0, 10);
      when(questionTemplateRepository.findByCreatedByAndNotDeleted(OWNER_ID, pageable))
          .thenReturn(new PageImpl<>(List.of(row), pageable, 1));

      var page = questionTemplateService.getMyQuestionTemplates(pageable);

      assertEquals(1, page.getTotalElements());
      verify(questionTemplateRepository, times(1)).findByCreatedByAndNotDeleted(OWNER_ID, pageable);
    }

    /**
     * Normal case: chuỗi tìm kiếm chỉ gồm khoảng trắng được chuẩn hóa thành null.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Repository nhận search null</li>
     * </ul>
     */
    @Test
    void it_should_pass_null_search_when_blank() {
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 5);
      when(questionTemplateRepository.findByCreatedByWithSearch(
              eq(OWNER_ID), eq(TemplateStatus.DRAFT), eq(null), eq(null), eq(null), eq(pageable)))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      questionTemplateService.getMyQuestionTemplatesFiltered(
          "   \t", TemplateStatus.DRAFT, null, null, pageable);

      verify(questionTemplateRepository, times(1))
          .findByCreatedByWithSearch(OWNER_ID, TemplateStatus.DRAFT, null, null, null, pageable);
    }

    /**
     * Normal case: trim hai đầu chuỗi tìm kiếm.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Tham số search truyền repository không còn khoảng thừa</li>
     * </ul>
     */
    @Test
    void it_should_trim_search_term_when_non_blank() {
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 5);
      when(questionTemplateRepository.findByCreatedByWithSearch(
              eq(OWNER_ID), eq(null), eq("đạo hàm"), eq(null), eq(null), eq(pageable)))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      questionTemplateService.getMyQuestionTemplatesFiltered(
          "  đạo hàm  ", null, null, null, pageable);

      verify(questionTemplateRepository, times(1))
          .findByCreatedByWithSearch(OWNER_ID, null, "đạo hàm", null, null, pageable);
    }

    /**
     * Normal case: tìm kiếm mẫu công khai theo bộ lọc.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Gọi searchTemplates với đúng tham số (mảng tags không dùng trong service)</li>
     * </ul>
     */
    @Test
    void it_should_delegate_search_templates() {
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 8);
      when(questionTemplateRepository.searchTemplates(
              eq(OWNER_ID),
              eq(true),
              eq(QuestionType.MULTIPLE_CHOICE),
              eq(CognitiveLevel.ANALYZE),
              eq("tích phân"),
              eq(pageable)))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      questionTemplateService.searchQuestionTemplates(
          QuestionType.MULTIPLE_CHOICE,
          CognitiveLevel.ANALYZE,
          true,
          "tích phân",
          new String[] {"tag-a"},
          pageable);

      verify(questionTemplateRepository, times(1))
          .searchTemplates(
              OWNER_ID, true, QuestionType.MULTIPLE_CHOICE, CognitiveLevel.ANALYZE, "tích phân", pageable);
    }
  }

  @Nested
  @DisplayName("togglePublicStatus()")
  class TogglePublicTests {

    /**
     * Abnormal case: mẫu DRAFT chưa public không được bật/tắt công khai.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_NOT_USABLE}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_draft_and_not_public() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));

      AppException ex =
          assertThrows(AppException.class, () -> questionTemplateService.togglePublicStatus(TEMPLATE_ID));

      assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, ex.getErrorCode());
    }

    /**
     * Normal case: đảo trạng thái công khai khi mẫu DRAFT đã public.
     *
     * <p>Expectation:
     * <ul>
     *   <li>isPublic đảo từ true sang false</li>
     * </ul>
     */
    @Test
    void it_should_toggle_when_draft_and_already_public() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate existing = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(existing));
      stubSaveTemplateReturnsSelf();

      QuestionTemplateResponse result = questionTemplateService.togglePublicStatus(TEMPLATE_ID);

      assertNotEquals(Boolean.TRUE, result.getIsPublic());
    }
  }

  @Nested
  @DisplayName("testTemplate()")
  class TestTemplateTests {

    /**
     * Normal case: sampleCount null thì mặc định ba mẫu.
     *
     * <p>Expectation:
     * <ul>
     *   <li>generateQuestion được gọi ba lần</li>
     * </ul>
     */
    @Test
    void it_should_use_default_sample_count_when_null() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(aiEnhancementService.generateQuestion(eq(published), any(Integer.class)))
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("Câu hỏi minh họa từ mẫu đã xuất bản.")
                  .correctAnswer("A")
                  .build());
      UUID savedId = UUID.fromString("4a000000-0000-0000-0000-000000000099");
      when(questionRepository.save(any(Question.class)))
          .thenAnswer(
              inv -> {
                Question q = inv.getArgument(0);
                q.setId(savedId);
                return q;
              });

      TemplateTestResponse response = questionTemplateService.testTemplate(TEMPLATE_ID, null, true);

      assertNotNull(response);
      verify(aiEnhancementService, times(3)).generateQuestion(eq(published), any(Integer.class));
    }

    /**
     * Abnormal case: văn bản mẫu bắt đầu bằng tiền tố lỗi LLM.
     *
     * <p>Expectation:
     * <ul>
     *   <li>isValid false và có lỗi trong validationErrors</li>
     * </ul>
     */
    @Test
    void it_should_record_llm_failure_prefix_in_errors() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(aiEnhancementService.generateQuestion(eq(published), eq(0)))
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("[LLM generation failed] upstream timeout")
                  .build());

      TemplateTestResponse response = questionTemplateService.testTemplate(TEMPLATE_ID, 1, false);

      assertFalse(response.getIsValid());
      assertEquals(1, response.getValidationErrors().size());
    }

    /**
     * Abnormal case: lưu câu hỏi thử nghiệm ném exception.
     *
     * <p>Expectation:
     * <ul>
     *   <li>validationErrors chứa cảnh báo lưu</li>
     * </ul>
     */
    @Test
    void it_should_append_warning_when_save_sample_fails() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(aiEnhancementService.generateQuestion(eq(published), eq(0)))
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("Phân tích cực trị hàm số bậc ba trên đoạn đã cho.")
                  .build());
      when(questionRepository.save(any(Question.class))).thenThrow(new IllegalStateException("flush failed"));

      TemplateTestResponse response = questionTemplateService.testTemplate(TEMPLATE_ID, 1, false);

      assertEquals(1, response.getValidationErrors().size());
    }

    /**
     * Abnormal case: generateQuestion ném exception cho một chỉ số.
     *
     * <p>Expectation:
     * <ul>
     *   <li>validationErrors ghi nhận lỗi sinh mẫu</li>
     * </ul>
     */
    @Test
    void it_should_handle_generation_exception_per_sample() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(aiEnhancementService.generateQuestion(eq(published), eq(0)))
          .thenThrow(new RuntimeException("Gemini circuit open"));

      TemplateTestResponse response = questionTemplateService.testTemplate(TEMPLATE_ID, 1, false);

      assertTrue(response.getValidationErrors().get(0).contains("generation failed"));
    }
  }

  @Nested
  @DisplayName("generateAIEnhancedQuestion()")
  class GenerateAiEnhancedTests {

    /**
     * Normal case: sinh câu nâng cao và lưu Question nháp.
     *
     * <p>Expectation:
     * <ul>
     *   <li>generatedQuestionId khớp id đã mock lưu</li>
     * </ul>
     */
    @Test
    void it_should_return_enhanced_payload_and_save_question() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      Map<String, String> options = Map.of("A", "một", "B", "hai");
      GeneratedQuestionSample sample =
          GeneratedQuestionSample.builder()
              .questionText("Tìm giới hạn dãy số cho trước.")
              .options(options)
              .correctAnswer("A")
              .usedParameters(Map.of("n", 5))
              .solutionSteps("Áp dụng định nghĩa giới hạn dãy.")
              .diagramData(null)
              .build();
      when(aiEnhancementService.generateQuestion(eq(published), eq(0))).thenReturn(sample);
      AIEnhancedQuestionResponse enhanced =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Diễn đạt lại: xác định giới hạn của dãy đã cho.")
              .enhancedOptions(Map.of("A", "một (đã chỉnh)", "B", "hai (đã chỉnh)"))
              .correctAnswerKey("A")
              .explanation("Giải thích ngắn gọn bằng định nghĩa epsilon.")
              .enhanced(true)
              .build();
      when(aiEnhancementService.enhanceQuestion(any())).thenReturn(enhanced);
      UUID savedId = UUID.fromString("4a000000-0000-0000-0000-000000000088");
      when(questionRepository.save(any(Question.class)))
          .thenAnswer(
              inv -> {
                Question q = inv.getArgument(0);
                q.setId(savedId);
                return q;
              });

      AIEnhancedQuestionResponse result = questionTemplateService.generateAIEnhancedQuestion(TEMPLATE_ID);

      assertEquals(savedId.toString(), result.getGeneratedQuestionId());
      assertEquals(sample.getQuestionText(), result.getOriginalQuestionText());
      verify(aiEnhancementService, times(1)).enhanceQuestion(any());
    }

    /**
     * Abnormal case: lưu Question thất bại và danh sách validationErrors ban đầu null.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Response vẫn trả về kèm cảnh báo lưu DB</li>
     * </ul>
     */
    @Test
    void it_should_add_warning_when_save_fails_and_errors_list_was_null() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(aiEnhancementService.generateQuestion(eq(published), eq(0)))
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("Ước lượng tích phân xác định.")
                  .options(null)
                  .correctAnswer(null)
                  .build());
      AIEnhancedQuestionResponse enhanced =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText(null)
              .enhancedOptions(null)
              .correctAnswerKey("A")
              .explanation("Dùng nguyên hàm sơ cấp.")
              .enhanced(false)
              .build();
      when(aiEnhancementService.enhanceQuestion(any())).thenReturn(enhanced);
      when(questionRepository.save(any(Question.class))).thenThrow(new IllegalStateException("deadlock"));

      AIEnhancedQuestionResponse result = questionTemplateService.generateAIEnhancedQuestion(TEMPLATE_ID);

      assertNotNull(result.getValidationErrors());
      assertTrue(
          result.getValidationErrors().stream().anyMatch(m -> m.contains("not saved to database")));
    }

    /**
     * Abnormal case: lưu thất bại khi đã có validationErrors từ bước enhance.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Danh sách lỗi được nối thêm cảnh báo</li>
     * </ul>
     */
    @Test
    void it_should_append_save_warning_when_validation_errors_already_exist() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(aiEnhancementService.generateQuestion(eq(published), eq(0)))
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("Bài toán tối ưu ràng buộc tuyến tính.")
                  .build());
      List<String> existingErrors = new ArrayList<>();
      existingErrors.add("Minor formatting issue");
      AIEnhancedQuestionResponse enhanced =
          AIEnhancedQuestionResponse.builder()
              .enhancedQuestionText("Phiên bản đã biên tập.")
              .validationErrors(existingErrors)
              .enhanced(true)
              .correctAnswerKey("B")
              .build();
      when(aiEnhancementService.enhanceQuestion(any())).thenReturn(enhanced);
      when(questionRepository.save(any(Question.class))).thenThrow(new RuntimeException("disk full"));

      AIEnhancedQuestionResponse result = questionTemplateService.generateAIEnhancedQuestion(TEMPLATE_ID);

      assertTrue(result.getValidationErrors().size() >= 2);
    }

    /**
     * Abnormal case: generateQuestion ném exception trước khi enhance.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_GENERATION_FAILED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_enhancement_pipeline_fails() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(aiEnhancementService.generateQuestion(eq(published), eq(0)))
          .thenThrow(new IllegalStateException("quota exceeded"));

      AppException ex =
          assertThrows(
              AppException.class, () -> questionTemplateService.generateAIEnhancedQuestion(TEMPLATE_ID));

      assertEquals(ErrorCode.TEMPLATE_GENERATION_FAILED, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("generateQuestionsFromTemplate()")
  class GenerateBatchTests {

    /**
     * Normal case: sinh lô câu hỏi chế độ PARAMETRIC.
     *
     * <p>Expectation:
     * <ul>
     *   <li>totalGenerated bằng count và không có warnings</li>
     * </ul>
     */
    @Test
    void it_should_generate_parametric_batch() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      // BlueprintService now supplies the constraint-aware tuples that drive
      // generation. Stub two so the batch can produce two questions.
      when(blueprintService.selectValueSets(eq(published), any(Integer.class), any(), any()))
          .thenReturn(java.util.List.of(Map.of("a", 1), Map.of("a", 2)));
      when(blueprintService.selectionPromptVersion()).thenReturn("test-prompt-v1");
      when(aiEnhancementService.generateQuestion(eq(published), any(Integer.class), any()))
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("Xác định miền xác định của hàm số cho trước.")
                  .correctAnswer("A")
                  .options(Map.of("A", "ℝ", "B", "ℝ \\ {0}"))
                  .build());
      when(questionRepository.save(any(Question.class)))
          .thenAnswer(
              inv -> {
                Question q = inv.getArgument(0);
                q.setId(UUID.randomUUID());
                return q;
              });

      GenerateTemplateQuestionsRequest req =
          GenerateTemplateQuestionsRequest.builder()
              .count(2)
              .generationMode(QuestionGenerationMode.PARAMETRIC)
              .build();

      GeneratedQuestionsBatchResponse result =
          questionTemplateService.generateQuestionsFromTemplate(TEMPLATE_ID, req);

      assertEquals(2, result.getTotalRequested());
      assertEquals(2, result.getTotalGenerated());
      assertNull(result.getWarnings());
    }

    // The legacy AI_FROM_CANONICAL generation mode and its "missing canonicalId"
    // validation were removed when generation was unified around the Blueprint
    // value selector. The two tests that asserted that behaviour
    // (it_should_use_ai_from_canonical_when_requested,
    // it_should_throw_when_canonical_mode_without_canonical_id) have been
    // dropped intentionally — they verified surface area that no longer exists.

    /**
     * Abnormal case: count không dương.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#INVALID_KEY}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_count_not_positive() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));

      GenerateTemplateQuestionsRequest req =
          GenerateTemplateQuestionsRequest.builder().count(0).build();

      AppException ex =
          assertThrows(
              AppException.class,
              () -> questionTemplateService.generateQuestionsFromTemplate(TEMPLATE_ID, req));

      assertEquals(ErrorCode.INVALID_KEY, ex.getErrorCode());
    }

    /**
     * Abnormal case: mẫu sinh ra có questionText rỗng.
     *
     * <p>Expectation:
     * <ul>
     *   <li>warnings chứa mục empty sample</li>
     * </ul>
     */
    @Test
    void it_should_warn_when_generated_text_blank() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(blueprintService.selectValueSets(eq(published), any(Integer.class), any(), any()))
          .thenReturn(java.util.List.of(Map.of("a", 1)));
      when(blueprintService.selectionPromptVersion()).thenReturn("test-prompt-v1");
      when(aiEnhancementService.generateQuestion(eq(published), any(Integer.class), any()))
          .thenReturn(GeneratedQuestionSample.builder().questionText("   ").build());

      GenerateTemplateQuestionsRequest req =
          GenerateTemplateQuestionsRequest.builder().count(1).build();

      GeneratedQuestionsBatchResponse result =
          questionTemplateService.generateQuestionsFromTemplate(TEMPLATE_ID, req);

      assertEquals(0, result.getTotalGenerated());
      assertNotNull(result.getWarnings());
    }

    /**
     * Abnormal case: exception trong một vòng lặp sinh câu.
     *
     * <p>Expectation:
     * <ul>
     *   <li>warnings ghi nhận lỗi theo chỉ số</li>
     * </ul>
     */
    @Test
    void it_should_warn_when_single_iteration_throws() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      when(blueprintService.selectValueSets(eq(published), any(Integer.class), any(), any()))
          .thenReturn(java.util.List.of(Map.of("a", 1)));
      when(blueprintService.selectionPromptVersion()).thenReturn("test-prompt-v1");
      when(aiEnhancementService.generateQuestion(eq(published), any(Integer.class), any()))
          .thenThrow(new RuntimeException("model overloaded"));

      GenerateTemplateQuestionsRequest req =
          GenerateTemplateQuestionsRequest.builder().count(1).build();

      GeneratedQuestionsBatchResponse result =
          questionTemplateService.generateQuestionsFromTemplate(TEMPLATE_ID, req);

      assertEquals(0, result.getTotalGenerated());
      // Per-iteration substitutor failures are recorded with the new wording.
      assertTrue(result.getWarnings().get(0).contains("Substitutor failed"));
    }
  }

  @Nested
  @DisplayName("generateQuestionsFromCanonical()")
  class GenerateFromCanonicalDelegateTests {

    /**
     * Normal case: API canonical ủy quyền sang generateQuestionsFromTemplate.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Sinh đủ một câu khi count là một</li>
     * </ul>
     */
    @Test
    void it_should_delegate_to_generate_from_template() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));
      // The canonical-delegate now goes through the unified Blueprint path —
      // no separate canonical generation method is called.
      when(blueprintService.selectValueSets(eq(published), any(Integer.class), any(), any()))
          .thenReturn(java.util.List.of(Map.of("a", 1)));
      when(blueprintService.selectionPromptVersion()).thenReturn("test-prompt-v1");
      when(aiEnhancementService.generateQuestion(eq(published), any(Integer.class), any()))
          .thenReturn(
              GeneratedQuestionSample.builder()
                  .questionText("Phiên bản sinh từ câu chuẩn hóa.")
                  .build());
      when(questionRepository.save(any(Question.class)))
          .thenAnswer(
              inv -> {
                Question q = inv.getArgument(0);
                q.setId(UUID.randomUUID());
                return q;
              });

      GenerateCanonicalQuestionsRequest req =
          GenerateCanonicalQuestionsRequest.builder().templateId(TEMPLATE_ID).count(1).build();

      GeneratedQuestionsBatchResponse result =
          questionTemplateService.generateQuestionsFromCanonical(CANONICAL_ID, req);

      assertEquals(1, result.getTotalGenerated());
    }
  }

  @Nested
  @DisplayName("aiGenerateTemplates()")
  class AiGenerateTemplatesTests {

    /**
     * Normal case: Gemini trả JSON rỗng sau khi phân tích bài học.
     *
     * <p>Expectation:
     * <ul>
     *   <li>totalTemplatesGenerated bằng không</li>
     * </ul>
     */
    @Test
    void it_should_return_zero_templates_when_gemini_returns_parseable_empty() {
      mockJwtTeacher(OWNER_ID);
      Lesson lesson = new Lesson();
      lesson.setId(LESSON_ID);
      lesson.setTitle("Bài 8 — Tích phân xác định");
      lesson.setSummary("Ôn lại định nghĩa và tính chất.");
      lesson.setLessonContent("Nội dung chi tiết về đổi biến số và tích phân từng phần.");
      lesson.setLearningObjectives("Học sinh vận dụng công thức Newton–Leibniz.");
      when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
      when(geminiService.sendMessage(any())).thenReturn("{\"templates\":[]}");

      AIGenerateTemplatesRequest req =
          AIGenerateTemplatesRequest.builder().lessonId(LESSON_ID).templateCount(2).build();

      AIGeneratedTemplatesResponse response = questionTemplateService.aiGenerateTemplates(req);

      assertEquals(0, response.getTotalTemplatesGenerated());
      assertEquals(lesson.getTitle(), response.getLessonName());
      verify(lessonRepository, times(1)).findById(LESSON_ID);
      verify(geminiService, times(1)).sendMessage(any());
    }

    /**
     * Abnormal case: lesson không tồn tại.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#LESSON_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_lesson_missing() {
      mockJwtTeacher(OWNER_ID);
      when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionTemplateService.aiGenerateTemplates(
                      AIGenerateTemplatesRequest.builder().lessonId(LESSON_ID).build()));

      assertEquals(ErrorCode.LESSON_NOT_FOUND, ex.getErrorCode());
    }

    /**
     * Abnormal case: Gemini ném exception.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_GENERATION_FAILED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_when_gemini_fails() {
      mockJwtTeacher(OWNER_ID);
      Lesson lesson = new Lesson();
      lesson.setId(LESSON_ID);
      lesson.setTitle("Bài đọc thêm về xác suất có điều kiện");
      lesson.setLessonContent("Nội dung bài học đầy đủ cho phân tích AI.");
      when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
      when(geminiService.sendMessage(any())).thenThrow(new RuntimeException("network reset"));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionTemplateService.aiGenerateTemplates(
                      AIGenerateTemplatesRequest.builder().lessonId(LESSON_ID).build()));

      assertEquals(ErrorCode.TEMPLATE_GENERATION_FAILED, ex.getErrorCode());
    }

    /**
     * Abnormal case: phản hồi Gemini null dẫn tới parse an toàn (không template).
     *
     * <p>Expectation:
     * <ul>
     *   <li>Không ném exception và số mẫu sinh bằng không</li>
     * </ul>
     */
    @Test
    void it_should_handle_null_gemini_response_in_parse_path() {
      mockJwtTeacher(OWNER_ID);
      Lesson lesson = new Lesson();
      lesson.setId(LESSON_ID);
      lesson.setTitle("Bài tập củng cố hình học không gian");
      lesson.setLessonContent("Diện tích và thể tích khối đa diện.");
      when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));
      when(geminiService.sendMessage(any())).thenReturn(null);

      AIGeneratedTemplatesResponse response =
          questionTemplateService.aiGenerateTemplates(
              AIGenerateTemplatesRequest.builder().lessonId(LESSON_ID).templateCount(1).build());

      assertEquals(0, response.getTotalTemplatesGenerated());
    }
  }

  @Nested
  @DisplayName("fetchTemplateForTesting() & access guards")
  class FetchTemplateTests {

    /**
     * Abnormal case: người không phải chủ cố sinh câu từ mẫu private dù đã publish.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_ACCESS_DENIED}</li>
     * </ul>
     */
    @Test
    void it_should_deny_non_owner_for_private_published_template() {
      mockJwtTeacher(OTHER_USER_ID);
      QuestionTemplate published =
          buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.PUBLISHED, false, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(published));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionTemplateService.generateQuestionsFromTemplate(
                      TEMPLATE_ID, GenerateTemplateQuestionsRequest.builder().count(1).build()));

      assertEquals(ErrorCode.TEMPLATE_ACCESS_DENIED, ex.getErrorCode());
    }

    /**
     * Abnormal case: mẫu chưa PUBLISHED không dùng để sinh câu.
     *
     * <p>Expectation:
     * <ul>
     *   <li>{@link ErrorCode#TEMPLATE_NOT_USABLE}</li>
     * </ul>
     */
    @Test
    void it_should_reject_non_published_for_generation() {
      mockJwtTeacher(OWNER_ID);
      QuestionTemplate draft = buildQuestionTemplate(TEMPLATE_ID, OWNER_ID, TemplateStatus.DRAFT, true, null);
      when(questionTemplateRepository.findByIdWithCreator(TEMPLATE_ID))
          .thenReturn(Optional.of(draft));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionTemplateService.generateQuestionsFromTemplate(
                      TEMPLATE_ID, GenerateTemplateQuestionsRequest.builder().count(1).build()));

      assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("private helpers via reflection")
  class PrivateHelperReflectionTests {

    /**
     * Normal case: extractJsonFromResponse với fence ```json.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Chuỗi JSON thuần được tách đúng</li>
     * </ul>
     */
    @Test
    void it_should_extract_json_from_markdown_json_fence() {
      String raw =
          "Intro text\n```json\n{\"templates\":[]}\n```\nTrailing";
      String extracted = invokePrivate("extractJsonFromResponse", new Class<?>[] {String.class}, raw);
      assertEquals("{\"templates\":[]}", extracted);
    }

    /**
     * Normal case: extractJsonFromResponse với fence ``` thường.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Nội dung giữa fence được trim</li>
     * </ul>
     */
    @Test
    void it_should_extract_json_from_generic_fence() {
      String raw = "Preamble\n```\n{\"a\":1}\n```";
      String extracted = invokePrivate("extractJsonFromResponse", new Class<?>[] {String.class}, raw);
      assertEquals("{\"a\":1}", extracted);
    }

    /**
     * Normal case: không có markdown fence thì trả nguyên chuỗi.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Output giống input</li>
     * </ul>
     */
    @Test
    void it_should_return_raw_when_no_fence() {
      String raw = "{\"templates\":[]}";
      String extracted = invokePrivate("extractJsonFromResponse", new Class<?>[] {String.class}, raw);
      assertEquals(raw, extracted);
    }

    /**
     * Normal case: buildLessonContent gom summary, nội dung và mục tiêu.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Chuỗi chứa cả ba phần mô tả</li>
     * </ul>
     */
    @Test
    void it_should_combine_lesson_fields_in_build_lesson_content() {
      Lesson lesson = new Lesson();
      lesson.setTitle("Chương lượng giác cơ bản");
      lesson.setSummary("Tóm tắt: định nghĩa và đồ thị.");
      lesson.setLessonContent("Nội dung: công thức cộng và nhân góc.");
      lesson.setLearningObjectives("Mục tiêu: giải phương trình lượng giác đơn giản.");

      String content = invokePrivate("buildLessonContent", new Class<?>[] {Lesson.class}, lesson);

      assertAll(
          () -> assertTrue(content.contains("Summary")),
          () -> assertTrue(content.contains("Content")),
          () -> assertTrue(content.contains("Learning Objectives")));
    }

    /**
     * Normal case: buildLessonContent khi không có khối văn bản chi tiết.
     *
     * <p>Expectation:
     * <ul>
     *   <li>Dùng tiêu đề bài học làm ngữ cảnh dự phòng</li>
     * </ul>
     */
    @Test
    void it_should_fallback_to_title_when_lesson_has_no_text_blocks() {
      Lesson lesson = new Lesson();
      lesson.setTitle("Ôn tập tổng hợp cuối học kỳ");
      lesson.setSummary(null);
      lesson.setLessonContent(null);
      lesson.setLearningObjectives(null);

      String content = invokePrivate("buildLessonContent", new Class<?>[] {Lesson.class}, lesson);

      assertTrue(content.contains(lesson.getTitle()));
    }
  }
}
