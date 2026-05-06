package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.CreateQuestionRequest;
import com.fptu.math_master.dto.request.ImportQuestionsRequest;
import com.fptu.math_master.dto.request.UpdateQuestionRequest;
import com.fptu.math_master.dto.response.ImportQuestionsResponse;
import com.fptu.math_master.dto.response.QuestionResponse;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionSourceType;
import com.fptu.math_master.enums.QuestionStatus;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.util.CSVParser;
import com.fptu.math_master.util.SecurityUtils;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("QuestionServiceImpl - Tests")
class QuestionServiceImplTest extends BaseUnitTest {

  @InjectMocks private QuestionServiceImpl questionService;

  @Mock private QuestionRepository questionRepository;
  @Mock private UserRepository userRepository;
  @Mock private QuestionBankRepository questionBankRepository;

  private static final UUID OWNER_ID = UUID.fromString("6a000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_ID = UUID.fromString("6a000000-0000-0000-0000-000000000002");
  private static final UUID ADMIN_ID = UUID.fromString("6a000000-0000-0000-0000-000000000003");
  private static final UUID QUESTION_ID = UUID.fromString("6a000000-0000-0000-0000-000000000010");
  private static final UUID BANK_ID = UUID.fromString("6a000000-0000-0000-0000-000000000020");

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void mockJwtTeacher(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("question-service-teacher-token")
            .header("alg", "none")
            .subject(userId.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(1800))
            .build();
    SecurityContextHolder
        .getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.TEACHER_ROLE))));
  }

  private void mockJwtAdmin(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("question-service-admin-token")
            .header("alg", "none")
            .subject(userId.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(1800))
            .build();
    SecurityContextHolder
        .getContext()
        .setAuthentication(
            new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.ADMIN_ROLE))));
  }

  private User buildUser(UUID id, String fullName) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    return user;
  }

  private QuestionBank buildBank(UUID id, UUID teacherId, String name) {
    QuestionBank bank = new QuestionBank();
    bank.setId(id);
    bank.setTeacherId(teacherId);
    bank.setName(name);
    bank.setIsPublic(false);
    return bank;
  }

  private Question buildQuestion(UUID id, UUID createdBy, QuestionStatus status) {
    Question question =
        Question.builder()
            .questionText("Giải phương trình bậc nhất với hệ số thực.")
            .questionType(QuestionType.MULTIPLE_CHOICE)
            .options(Map.of("A", "x = 2", "B", "x = -2"))
            .correctAnswer("A")
            .explanation("Chuyển vế và chia hệ số của ẩn.")
            .solutionSteps("B1: Chuyển hằng số sang vế phải. B2: Chia cho hệ số của x.")
            .diagramData("y = ax + b")
            .points(new BigDecimal("2.0"))
            .cognitiveLevel(CognitiveLevel.APPLY)
            .tags(new String[] {"algebra", "linear-equation"})
            .questionBankId(BANK_ID)
            .questionStatus(status)
            .questionSourceType(QuestionSourceType.MANUAL)
            .build();
    question.setId(id);
    question.setCreatedBy(createdBy);
    question.setCreatedAt(Instant.parse("2026-01-15T02:00:00Z"));
    question.setUpdatedAt(Instant.parse("2026-01-15T03:00:00Z"));
    return question;
  }

  private CreateQuestionRequest buildCreateRequest() {
    return CreateQuestionRequest.builder()
        .questionText("  Tính đạo hàm của hàm số f(x) = x^2  ")
        .questionType(QuestionType.MULTIPLE_CHOICE)
        .options(Map.of("A", "2x", "B", "x"))
        .correctAnswer("  A  ")
        .explanation(null)
        .solutionSteps("Áp dụng quy tắc lũy thừa.")
        .diagramData("f(x)=x^2")
        .points(new BigDecimal("1.5"))
        .cognitiveLevel(CognitiveLevel.UNDERSTAND)
        .tags(new String[] {"derivative", "polynomial"})
        .questionBankId(BANK_ID)
        .build();
  }

  @Nested
  @DisplayName("createQuestion()")
  class CreateQuestionTests {

    /**
     * Abnormal case: Ném lỗi khi nội dung câu hỏi rỗng sau khi trim.
     *
     * <p>Input:
     * <ul>
     *   <li>questionText: "   " (blank)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>questionText null/blank check → TRUE branch (throw INVALID_KEY)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code INVALID_KEY}</li>
     *   <li>{@code questionRepository.save()} không được gọi</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_question_text_is_blank() {
      // ===== ARRANGE =====
      CreateQuestionRequest request = buildCreateRequest();
      request.setQuestionText("   ");

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionService.createQuestion(request));
      assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    /**
     * Normal case: Tạo câu hỏi thành công khi dữ liệu hợp lệ và explanation null.
         *
     * <p>Input:
     * <ul>
     *   <li>questionText: có khoảng trắng đầu/cuối</li>
     *   <li>correctAnswer: có khoảng trắng đầu/cuối</li>
     *   <li>explanation: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>questionText null/blank check → FALSE branch</li>
     *   <li>correctAnswer null/blank check → FALSE branch</li>
     *   <li>explanation null check → TRUE branch (set default text)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Câu hỏi được lưu với dữ liệu đã trim và source type MANUAL</li>
     * </ul>
     */
    @Test
    void it_should_create_question_with_default_explanation_when_explanation_is_null() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      CreateQuestionRequest request = buildCreateRequest();
      when(questionRepository.save(any(Question.class)))
          .thenAnswer(
              invocation -> {
                Question q = invocation.getArgument(0);
                q.setId(QUESTION_ID);
                return q;
              });
      when(userRepository.findById(eq(OWNER_ID)))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Nguyen Duc Minh")));
      when(questionBankRepository.findById(eq(BANK_ID)))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Đạo hàm cơ bản")));

      // ===== ACT =====
      QuestionResponse result = questionService.createQuestion(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(QUESTION_ID, result.getId()),
          () -> assertEquals("Tính đạo hàm của hàm số f(x) = x^2", result.getQuestionText()),
          () -> assertEquals("A", result.getCorrectAnswer()),
          () -> assertEquals("No explanation provided", result.getExplanation()),
          () -> assertEquals(QuestionSourceType.MANUAL, result.getQuestionSourceType()));

      // ===== VERIFY =====
      verify(questionRepository, times(1)).save(any(Question.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("updateQuestion()")
  class UpdateQuestionTests {

    /**
     * Abnormal case: Ném lỗi khi người dùng không phải chủ sở hữu câu hỏi.
     *
     * <p>Input:
     * <ul>
     *   <li>question.createdBy: OTHER_ID</li>
     *   <li>currentUser: OWNER_ID</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted → present</li>
     *   <li>owner check → FALSE branch (throw UNAUTHORIZED)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code UNAUTHORIZED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_user_is_not_owner() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question storedQuestion = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      UpdateQuestionRequest request = UpdateQuestionRequest.builder().questionText("Nội dung mới").build();
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(storedQuestion));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> questionService.updateQuestion(QUESTION_ID, request));
      assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    /**
     * Abnormal case: Ném lỗi khi cố chuyển trạng thái APPROVED từ trạng thái không phải AI_DRAFT.
     *
     * <p>Input:
     * <ul>
     *   <li>stored status: APPROVED</li>
     *   <li>request status: APPROVED</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>status != null check → TRUE</li>
     *   <li>request.status == APPROVED and current != AI_DRAFT → TRUE branch (throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code QUESTION_REVIEW_STATUS_INVALID}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_approving_non_draft_question() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question storedQuestion = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.APPROVED);
      UpdateQuestionRequest request = UpdateQuestionRequest.builder().status(QuestionStatus.APPROVED).build();
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(storedQuestion));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> questionService.updateQuestion(QUESTION_ID, request));
      assertEquals(ErrorCode.QUESTION_REVIEW_STATUS_INVALID, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("approveQuestion()")
  class ApproveQuestionTests {

    /**
     * Normal case: Chủ sở hữu duyệt câu hỏi AI_DRAFT thành công.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>owner/admin check → TRUE branch qua owner</li>
     *   <li>status == AI_DRAFT check → FALSE branch của throw condition</li>
     * </ul>
     */
    @Test
    void it_should_approve_question_when_owner_and_status_is_draft() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question storedQuestion = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(storedQuestion));
      when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.findById(eq(OWNER_ID)))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Tran Minh Anh")));
      when(questionBankRepository.findById(eq(BANK_ID)))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Hàm số")));

      // ===== ACT =====
      QuestionResponse result = questionService.approveQuestion(QUESTION_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(QuestionStatus.APPROVED, result.getQuestionStatus()));

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).save(storedQuestion);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    /**
     * Abnormal case: Ném lỗi khi user không phải owner và cũng không có quyền ADMIN.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>owner/admin check → FALSE branch qua hasRole("ADMIN") = false</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_not_owner_and_not_admin() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question storedQuestion = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(storedQuestion));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
        AppException exception =
            assertThrows(AppException.class, () -> questionService.approveQuestion(QUESTION_ID));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_question_status_is_not_ai_draft() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question storedQuestion = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.APPROVED);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(storedQuestion));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionService.approveQuestion(QUESTION_ID));
      assertEquals(ErrorCode.QUESTION_REVIEW_STATUS_INVALID, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_approve_target_not_found() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionService.approveQuestion(QUESTION_ID));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_approve_when_user_is_admin_but_not_owner() {
      // ===== ARRANGE =====
      mockJwtAdmin(ADMIN_ID);
      Question storedQuestion = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(storedQuestion));
      when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
      when(userRepository.findById(OTHER_ID))
          .thenReturn(Optional.of(buildUser(OTHER_ID, "Pham Van An")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OTHER_ID, "Ngân hàng Lượng giác")));

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
        QuestionResponse result = questionService.approveQuestion(QUESTION_ID);
        assertEquals(QuestionStatus.APPROVED, result.getQuestionStatus());
      }

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).save(storedQuestion);
      verify(userRepository, times(1)).findById(OTHER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("importQuestionsFromFile()")
  class ImportQuestionsTests {

    /**
     * Abnormal case: Dừng import tại lỗi đầu tiên khi continueOnError = false.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>questionBankId != null branch → set bank id</li>
     *   <li>catch(Exception) branch → TRUE</li>
     *   <li>continueOnError false branch → trả về FAILURE sớm</li>
     * </ul>
     */
    @Test
    void it_should_stop_on_first_save_error_when_continue_on_error_is_false() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      ImportQuestionsRequest request =
          ImportQuestionsRequest.builder()
              .fileContent("ignored")
              .fileFormat("CSV")
              .questionBankId(BANK_ID)
              .continueOnError(false)
              .build();

      CreateQuestionRequest row = buildCreateRequest();
      CSVParser.ParseResult parseResult = new CSVParser.ParseResult();
      parseResult.questions.add(row);
      parseResult.questions.add(row);

      try (MockedStatic<CSVParser> csvParserMock = mockStatic(CSVParser.class)) {
        csvParserMock.when(() -> CSVParser.parseCSV("ignored")).thenReturn(parseResult);
        when(questionRepository.save(any(Question.class))).thenThrow(new RuntimeException("DB timeout"));

        // ===== ACT =====
        ImportQuestionsResponse result = questionService.importQuestionsFromFile(request);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(2, result.getTotalRows()),
            () -> assertEquals(0, result.getSuccessCount()),
            () -> assertEquals(1, result.getFailureCount()),
            () -> assertEquals("FAILURE", result.getStatus()),
            () -> assertEquals(1, result.getResults().size()),
            () -> assertTrue(result.getResults().get(0).getErrorMessage().contains("DB timeout")));
      }

      // ===== VERIFY =====
      verify(questionRepository, times(1)).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    /**
     * Normal case: Import thành công toàn bộ row và set status SUCCESS.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>for each parsed question → success branch</li>
     *   <li>parse errors loop empty branch</li>
     *   <li>status decision: successCount == totalRows → SUCCESS</li>
     * </ul>
     */
    @Test
    void it_should_return_success_when_all_rows_are_imported() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      ImportQuestionsRequest request =
          ImportQuestionsRequest.builder().fileContent("ignored").continueOnError(true).build();

      CreateQuestionRequest row1 = buildCreateRequest();
      CreateQuestionRequest row2 = buildCreateRequest();
      CSVParser.ParseResult parseResult = new CSVParser.ParseResult();
      parseResult.questions.add(row1);
      parseResult.questions.add(row2);

      try (MockedStatic<CSVParser> csvParserMock = mockStatic(CSVParser.class)) {
        csvParserMock.when(() -> CSVParser.parseCSV("ignored")).thenReturn(parseResult);
        when(questionRepository.save(any(Question.class)))
            .thenAnswer(
                invocation -> {
                  Question q = invocation.getArgument(0);
                  q.setId(UUID.randomUUID());
                  return q;
                });

        // ===== ACT =====
        ImportQuestionsResponse result = questionService.importQuestionsFromFile(request);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(2, result.getTotalRows()),
            () -> assertEquals(2, result.getSuccessCount()),
            () -> assertEquals(0, result.getFailureCount()),
            () -> assertEquals("SUCCESS", result.getStatus()),
            () -> assertEquals(2, result.getResults().size()),
            () -> assertTrue(result.getResults().stream().allMatch(ImportQuestionsResponse.ImportRowResult::getSuccess)));
      }

      // ===== VERIFY =====
      verify(questionRepository, times(2)).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_return_partial_success_when_some_rows_fail_and_continue_true() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      ImportQuestionsRequest request =
          ImportQuestionsRequest.builder()
              .fileContent("ignored")
              .continueOnError(true)
              .questionBankId(BANK_ID)
              .build();

      CSVParser.ParseResult parseResult = new CSVParser.ParseResult();
      parseResult.questions.add(buildCreateRequest());
      parseResult.questions.add(buildCreateRequest());
      parseResult.errors.add("Row 5: thiếu cột question_type");

      try (MockedStatic<CSVParser> csvParserMock = mockStatic(CSVParser.class)) {
        csvParserMock.when(() -> CSVParser.parseCSV("ignored")).thenReturn(parseResult);
        when(questionRepository.save(any(Question.class)))
            .thenAnswer(
                invocation -> {
                  Question q = invocation.getArgument(0);
                  q.setId(QUESTION_ID);
                  return q;
                })
            .thenThrow(new RuntimeException("Constraint violation"));

        // ===== ACT =====
        ImportQuestionsResponse result = questionService.importQuestionsFromFile(request);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(3, result.getTotalRows()),
            () -> assertEquals(1, result.getSuccessCount()),
            () -> assertEquals(2, result.getFailureCount()),
            () -> assertEquals("PARTIAL_SUCCESS", result.getStatus()));
      }

      // ===== VERIFY =====
      verify(questionRepository, times(2)).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("searchQuestions()")
  class SearchQuestionsTests {

    /**
     * Normal case: Tìm kiếm theo searchTerm khi có keyword.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>searchTerm != null && !empty → TRUE branch dùng searchByCreatedBy</li>
     * </ul>
     */
    @Test
    void it_should_search_by_created_by_when_search_term_is_present() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 10);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      Page<Question> page = new PageImpl<>(List.of(question), pageable, 1);
      when(questionRepository.searchByCreatedBy(OWNER_ID, "%dao ham%", pageable)).thenReturn(page);
      when(userRepository.findById(eq(OWNER_ID)))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Pham Gia Bao")));
      when(questionBankRepository.findById(eq(BANK_ID)))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Vi phân")));

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.searchQuestions("dao ham", null, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(1, result.getTotalElements()),
          () -> assertEquals(QUESTION_ID, result.getContent().get(0).getId()));

      // ===== VERIFY =====
      verify(questionRepository, times(1)).searchByCreatedBy(OWNER_ID, "%dao ham%", pageable);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    /**
     * Normal case: Fallback sang findByFilters khi searchTerm rỗng và type invalid.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>searchTerm branch → FALSE</li>
     *   <li>type parse IllegalArgumentException branch → TRUE (typeEnum giữ null)</li>
     * </ul>
     */
    @Test
    void it_should_fallback_to_filters_with_null_type_when_type_is_invalid() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 5);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      Page<Question> page = new PageImpl<>(List.of(question), pageable, 1);
      when(questionRepository.findByFilters(OWNER_ID, null, pageable)).thenReturn(page);
      when(userRepository.findById(eq(OWNER_ID)))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Le Thanh Son")));
      when(questionBankRepository.findById(eq(BANK_ID)))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Số học")));

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.searchQuestions("", "INVALID_TYPE", pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByFilters(OWNER_ID, null, pageable);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_fallback_to_filters_with_null_type_when_type_is_blank() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 5);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByFilters(OWNER_ID, null, pageable))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Pham Minh Chau")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Khảo sát hàm")));

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.searchQuestions(null, "   ", pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByFilters(OWNER_ID, null, pageable);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("assignQuestionsToBank()")
  class AssignQuestionsToBankTests {

    /**
     * Normal case: Gán câu hỏi vào ngân hàng thành công và loại trùng ID.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>empty questionIds check → FALSE</li>
     *   <li>bank owner/admin check → TRUE qua owner</li>
     *   <li>deduplicate questionIds qua LinkedHashSet branch</li>
     * </ul>
     */
    @Test
    void it_should_assign_unique_questions_to_bank_when_owner_is_valid() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      UUID questionId2 = UUID.fromString("6a000000-0000-0000-0000-000000000011");
      QuestionBank bank = buildBank(BANK_ID, OWNER_ID, "Ngân hàng Hình học");
      Question q1 = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      Question q2 = buildQuestion(questionId2, OWNER_ID, QuestionStatus.AI_DRAFT);

      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(q1));
      when(questionRepository.findByIdAndNotDeleted(questionId2)).thenReturn(Optional.of(q2));
      when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      Integer result =
          questionService.assignQuestionsToBank(
              BANK_ID, List.of(QUESTION_ID, QUESTION_ID, questionId2));

      // ===== ASSERT =====
      assertEquals(2, result);

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(questionId2);
      verify(questionRepository, times(2)).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    /**
     * Abnormal case: Ném lỗi khi danh sách có câu hỏi không thuộc current user và không phải ADMIN.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>question ownership/admin check → FALSE branch (throw UNAUTHORIZED)</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_question_owner_mismatch_and_user_not_admin() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      QuestionBank bank = buildBank(BANK_ID, OWNER_ID, "Ngân hàng Logarit");
      Question foreignQuestion = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);

      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(foreignQuestion));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
        AppException exception =
            assertThrows(
                AppException.class,
                () -> questionService.assignQuestionsToBank(BANK_ID, List.of(QUESTION_ID)));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_return_zero_when_assign_question_ids_is_empty() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);

      // ===== ACT =====
      Integer result = questionService.assignQuestionsToBank(BANK_ID, List.of());

      // ===== ASSERT =====
      assertEquals(0, result);

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("removeQuestionsFromBank()")
  class RemoveQuestionsFromBankTests {

    /**
     * Normal case: Chỉ remove các câu hỏi đúng ngân hàng; câu hỏi ở ngân hàng khác giữ nguyên.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>bankId.equals(question.getQuestionBankId()) → TRUE và FALSE đều được cover</li>
     * </ul>
     */
    @Test
    void it_should_remove_only_questions_belonging_to_target_bank() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      UUID questionId2 = UUID.fromString("6a000000-0000-0000-0000-000000000012");
      UUID anotherBankId = UUID.fromString("6a000000-0000-0000-0000-000000000099");

      QuestionBank bank = buildBank(BANK_ID, OWNER_ID, "Ngân hàng Tích phân");
      Question inTargetBank = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      inTargetBank.setQuestionBankId(BANK_ID);
      Question inAnotherBank = buildQuestion(questionId2, OWNER_ID, QuestionStatus.AI_DRAFT);
      inAnotherBank.setQuestionBankId(anotherBankId);

      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(inTargetBank));
      when(questionRepository.findByIdAndNotDeleted(questionId2)).thenReturn(Optional.of(inAnotherBank));
      when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      Integer result = questionService.removeQuestionsFromBank(BANK_ID, List.of(QUESTION_ID, questionId2));

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result),
          () -> assertNull(inTargetBank.getQuestionBankId()),
          () -> assertEquals(anotherBankId, inAnotherBank.getQuestionBankId()));

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(questionId2);
      verify(questionRepository, times(1)).save(inTargetBank);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_return_zero_when_remove_question_ids_is_empty() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);

      // ===== ACT =====
      Integer result = questionService.removeQuestionsFromBank(BANK_ID, List.of());

      // ===== ASSERT =====
      assertEquals(0, result);

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_bank_not_found_while_removing() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionService.removeQuestionsFromBank(BANK_ID, List.of(QUESTION_ID)));
      assertEquals(ErrorCode.QUESTION_BANK_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionRepository, never()).findByIdAndNotDeleted(any(UUID.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_remove_target_question_not_found() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Luyện đề")));
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionService.removeQuestionsFromBank(BANK_ID, List.of(QUESTION_ID)));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_remove_question_owned_by_another_user() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question foreignQuestion = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Số học")));
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(foreignQuestion));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
        AppException exception =
            assertThrows(
                AppException.class,
                () -> questionService.removeQuestionsFromBank(BANK_ID, List.of(QUESTION_ID)));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("read and listing methods")
  class ReadAndListingTests {

    @Test
    void it_should_return_question_when_get_question_by_id_found() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(question));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Nguyen Hoang Long")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Đại số")));

      // ===== ACT =====
      QuestionResponse result = questionService.getQuestionById(QUESTION_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(QUESTION_ID, result.getId()),
          () -> assertEquals("Nguyen Hoang Long", result.getCreatorName()));

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_get_question_by_id_not_found() {
      // ===== ARRANGE =====
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionService.getQuestionById(QUESTION_ID));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_search_my_questions_with_trimmed_patterns_and_unsorted_pageable() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable input = PageRequest.of(1, 3);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      Page<Question> page = new PageImpl<>(List.of(question), input, 1);
      when(questionRepository.findByCreatedByWithSearch(
              eq(OWNER_ID), eq("%đạo hàm%"), eq("%algebra%"), any(Pageable.class)))
          .thenReturn(page);
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Vo Minh Tri")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Vi tích phân")));

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.getMyQuestions("  đạo hàm ", " algebra ", input);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1))
          .findByCreatedByWithSearch(eq(OWNER_ID), eq("%đạo hàm%"), eq("%algebra%"), any(Pageable.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_question_bank_not_found() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 5);
      when(questionBankRepository.findById(BANK_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> questionService.getQuestionsByBank(BANK_ID, pageable));
      assertEquals(ErrorCode.QUESTION_BANK_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verify(questionRepository, never()).findByQuestionBankIdAndNotDeleted(eq(BANK_ID), any(Pageable.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_return_questions_by_template_and_canonical() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      UUID templateId = UUID.fromString("6a000000-0000-0000-0000-000000000030");
      UUID canonicalId = UUID.fromString("6a000000-0000-0000-0000-000000000040");
      Pageable pageable = PageRequest.of(0, 5);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByTemplateIdAndNotDeleted(templateId)).thenReturn(List.of(question));
      when(questionRepository.findByCanonicalQuestionIdAndNotDeleted(canonicalId, pageable))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Le Quang Huy")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Ôn tập")));

      // ===== ACT =====
      List<QuestionResponse> byTemplate = questionService.getQuestionsByTemplate(templateId);
      Page<QuestionResponse> byCanonical =
          questionService.getQuestionsByCanonicalQuestion(canonicalId, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, byTemplate.size()),
          () -> assertEquals(1, byCanonical.getTotalElements()));

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByTemplateIdAndNotDeleted(templateId);
      verify(questionRepository, times(1)).findByCanonicalQuestionIdAndNotDeleted(canonicalId, pageable);
      verify(userRepository, times(2)).findById(OWNER_ID);
      verify(questionBankRepository, times(2)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_get_questions_by_bank_when_bank_exists() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 10);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      question.setQuestionBankId(null);
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Hình giải tích")));
      when(questionRepository.findByQuestionBankIdAndNotDeleted(BANK_ID, pageable))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.getQuestionsByBank(BANK_ID, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.getContent().size()),
          () -> assertEquals("Unknown", result.getContent().get(0).getCreatorName()),
          () -> assertNull(result.getContent().get(0).getQuestionBankName()));

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verify(questionRepository, times(1)).findByQuestionBankIdAndNotDeleted(BANK_ID, pageable);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_get_my_questions_without_filters_when_name_and_tag_blank() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 2);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByCreatedByWithSearch(eq(OWNER_ID), eq(null), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Dang Minh Quan")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Vi phân")));

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.getMyQuestions(" ", "", pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1))
          .findByCreatedByWithSearch(eq(OWNER_ID), eq(null), eq(null), any(Pageable.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_get_my_questions_when_only_name_filter_is_provided() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 2);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByCreatedByWithSearch(eq(OWNER_ID), eq("%đại số%"), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Bui Anh Tuan")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Đại số")));

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.getMyQuestions("đại số", null, pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1))
          .findByCreatedByWithSearch(eq(OWNER_ID), eq("%đại số%"), eq(null), any(Pageable.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_get_my_questions_when_only_tag_filter_is_provided() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 2);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByCreatedByWithSearch(eq(OWNER_ID), eq(null), eq("%geometry%"), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Tran Thi Mai")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Hình học")));

      // ===== ACT =====
      Page<QuestionResponse> result = questionService.getMyQuestions(null, "geometry", pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1))
          .findByCreatedByWithSearch(eq(OWNER_ID), eq(null), eq("%geometry%"), any(Pageable.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("bulkApproveQuestions()")
  class BulkApproveQuestionsTests {

    @Test
    void it_should_return_zero_when_question_ids_is_null() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);

      // ===== ACT =====
      Integer result = questionService.bulkApproveQuestions(null);

      // ===== ASSERT =====
      assertEquals(0, result);

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_approve_all_questions_when_owner_and_draft_status() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      UUID id2 = UUID.fromString("6a000000-0000-0000-0000-000000000013");
      Question q1 = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      Question q2 = buildQuestion(id2, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(q1));
      when(questionRepository.findByIdAndNotDeleted(id2)).thenReturn(Optional.of(q2));
      when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      Integer result = questionService.bulkApproveQuestions(List.of(QUESTION_ID, id2));

      // ===== ASSERT =====
      assertEquals(2, result);

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(id2);
      verify(questionRepository, times(2)).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_bulk_approve_hits_invalid_status() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question q1 = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.APPROVED);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(q1));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionService.bulkApproveQuestions(List.of(QUESTION_ID)));
      assertEquals(ErrorCode.QUESTION_REVIEW_STATUS_INVALID, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_bulk_approve_target_not_found() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> questionService.bulkApproveQuestions(List.of(QUESTION_ID)));
      assertEquals(ErrorCode.QUESTION_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_bulk_approve_user_not_owner_and_not_admin() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question question = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(question));

      // ===== ACT & ASSERT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
        AppException exception =
            assertThrows(
                AppException.class,
                () -> questionService.bulkApproveQuestions(List.of(QUESTION_ID)));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
      }

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_return_zero_when_bulk_approve_list_is_empty() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);

      // ===== ACT =====
      Integer result = questionService.bulkApproveQuestions(List.of());

      // ===== ASSERT =====
      assertEquals(0, result);

      // ===== VERIFY =====
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_allow_bulk_approve_when_user_is_admin() {
      // ===== ARRANGE =====
      mockJwtAdmin(ADMIN_ID);
      Question question = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(question));
      when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
        Integer result = questionService.bulkApproveQuestions(List.of(QUESTION_ID));
        assertEquals(1, result);
      }

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).save(question);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("deleteQuestion()")
  class DeleteQuestionTests {

    @Test
    void it_should_soft_delete_when_owner_requests_delete() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(question));

      // ===== ACT =====
      questionService.deleteQuestion(QUESTION_ID);

      // ===== ASSERT =====
      assertTrue(true);

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).softDeleteById(QUESTION_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_throw_exception_when_delete_requested_by_non_owner() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question question = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(question));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionService.deleteQuestion(QUESTION_ID));
      assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, never()).softDeleteById(any(UUID.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("searchByKeywordAndTags()")
  class SearchByKeywordAndTagsTests {

    @Test
    void it_should_pass_null_tags_param_when_tags_are_blank() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 5);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.searchByKeywordAndTags(
              eq(OWNER_ID), eq("%hàm số%"), eq(null), eq(null), eq(null), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Tran Gia Han")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Hàm số liên tục")));

      // ===== ACT =====
      List<String> rawTags = new java.util.ArrayList<>();
      rawTags.add("  ");
      rawTags.add(null);
      Page<QuestionResponse> result =
          questionService.searchByKeywordAndTags(" hàm số ", rawTags, null, null, pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getTotalElements());

      // ===== VERIFY =====
      verify(questionRepository, times(1))
          .searchByKeywordAndTags(
              eq(OWNER_ID), eq("%hàm số%"), eq(null), eq(null), eq(null), any(Pageable.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_build_tags_param_when_valid_tags_are_provided() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 5);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.searchByKeywordAndTags(
              eq(OWNER_ID),
              eq(null),
              eq("algebra,trigonometry"),
              eq(null),
              eq(null),
              any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Hoang Minh Dat")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Lượng giác")));

      // ===== ACT =====
      Page<QuestionResponse> result =
          questionService.searchByKeywordAndTags(
              null, List.of("algebra", "TRIGONOMETRY"), null, null, pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1))
          .searchByKeywordAndTags(
              eq(OWNER_ID),
              eq(null),
              eq("algebra,trigonometry"),
              eq(null),
              eq(null),
              any(Pageable.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }

  @Nested
  @DisplayName("misc additional branches")
  class AdditionalBranchTests {

    @Test
    void it_should_throw_invalid_key_when_correct_answer_is_blank() {
      // ===== ARRANGE =====
      CreateQuestionRequest request = buildCreateRequest();
      request.setCorrectAnswer("   ");

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> questionService.createQuestion(request));
      assertEquals(ErrorCode.INVALID_KEY, exception.getErrorCode());

      // ===== VERIFY =====
      verify(questionRepository, never()).save(any(Question.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_update_question_fields_and_status_successfully() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      UpdateQuestionRequest request =
          UpdateQuestionRequest.builder()
              .questionText("Nội dung cập nhật")
              .correctAnswer("B")
              .status(QuestionStatus.APPROVED)
              .build();
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(question));
      when(questionRepository.save(question)).thenReturn(question);
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Ngo Tuan Kiet")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Hệ phương trình")));

      // ===== ACT =====
      QuestionResponse result = questionService.updateQuestion(QUESTION_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Nội dung cập nhật", result.getQuestionText()),
          () -> assertEquals("B", result.getCorrectAnswer()),
          () -> assertEquals(QuestionStatus.APPROVED, result.getQuestionStatus()));

      // ===== VERIFY =====
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).save(question);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_allow_admin_to_assign_foreign_question() {
      // ===== ARRANGE =====
      mockJwtAdmin(ADMIN_ID);
      QuestionBank bank = buildBank(BANK_ID, OTHER_ID, "Ngân hàng Xác suất");
      Question question = buildQuestion(QUESTION_ID, OTHER_ID, QuestionStatus.AI_DRAFT);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionRepository.findByIdAndNotDeleted(QUESTION_ID)).thenReturn(Optional.of(question));
      when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> securityUtilsMock = mockStatic(SecurityUtils.class)) {
        securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
        Integer result = questionService.assignQuestionsToBank(BANK_ID, List.of(QUESTION_ID));
        assertEquals(1, result);
      }

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionRepository, times(1)).findByIdAndNotDeleted(QUESTION_ID);
      verify(questionRepository, times(1)).save(question);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_create_question_with_trimmed_explanation_when_explanation_is_provided() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      CreateQuestionRequest request = buildCreateRequest();
      request.setExplanation("  Giải thích chi tiết cho đáp án đúng.  ");
      request.setQuestionBankId(null);
      when(questionRepository.save(any(Question.class)))
          .thenAnswer(
              invocation -> {
                Question q = invocation.getArgument(0);
                q.setId(QUESTION_ID);
                return q;
              });
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Pham Minh Hai")));

      // ===== ACT =====
      QuestionResponse result = questionService.createQuestion(request);

      // ===== ASSERT =====
      assertEquals("Giải thích chi tiết cho đáp án đúng.", result.getExplanation());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).save(any(Question.class));
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, never()).findById(any(UUID.class));
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }

    @Test
    void it_should_use_find_by_filters_with_valid_type_when_search_term_is_null() {
      // ===== ARRANGE =====
      mockJwtTeacher(OWNER_ID);
      Pageable pageable = PageRequest.of(0, 5);
      Question question = buildQuestion(QUESTION_ID, OWNER_ID, QuestionStatus.AI_DRAFT);
      when(questionRepository.findByFilters(OWNER_ID, QuestionType.MULTIPLE_CHOICE, pageable))
          .thenReturn(new PageImpl<>(List.of(question), pageable, 1));
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Nguyen Anh Thu")));
      when(questionBankRepository.findById(BANK_ID))
          .thenReturn(Optional.of(buildBank(BANK_ID, OWNER_ID, "Ngân hàng Chuyên đề")));

      // ===== ACT =====
      Page<QuestionResponse> result =
          questionService.searchQuestions(null, "MULTIPLE_CHOICE", pageable);

      // ===== ASSERT =====
      assertEquals(1, result.getContent().size());

      // ===== VERIFY =====
      verify(questionRepository, times(1))
          .findByFilters(OWNER_ID, QuestionType.MULTIPLE_CHOICE, pageable);
      verify(userRepository, times(1)).findById(OWNER_ID);
      verify(questionBankRepository, times(1)).findById(BANK_ID);
      verifyNoMoreInteractions(questionRepository, userRepository, questionBankRepository);
    }
  }
}
