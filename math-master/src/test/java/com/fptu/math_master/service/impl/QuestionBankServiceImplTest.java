package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.UserRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("QuestionBankServiceImpl - Tests")
class QuestionBankServiceImplTest extends BaseUnitTest {

  @InjectMocks private QuestionBankServiceImpl questionBankService;

  @Mock private QuestionBankRepository questionBankRepository;
  @Mock private QuestionRepository questionRepository;
  @Mock private QuestionTemplateRepository questionTemplateRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private UserRepository userRepository;

  private static final UUID OWNER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID ADMIN_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID OTHER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID BANK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID CHAPTER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TEMPLATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private MockedStatic<SecurityUtils> securityUtilsMock;
  private QuestionBank bank;

  @BeforeEach
  void setUp() {
    securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OWNER_ID);
    securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);

    bank = buildBank(BANK_ID, OWNER_ID, CHAPTER_ID, false);
    when(questionBankRepository.countQuestionsByQuestionBankId(any(UUID.class))).thenReturn(5L);
    when(questionRepository.countByCognitiveLevelForBank(any(UUID.class)))
        .thenReturn(List.<Object[]>of(new Object[] {CognitiveLevel.NHAN_BIET, 3L}));
    when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(buildUser(OWNER_ID, "Le Gia Bao")));
    when(chapterRepository.findById(CHAPTER_ID))
        .thenReturn(Optional.of(buildChapter(CHAPTER_ID, "Giai tich nang cao", null)));
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  private QuestionBank buildBank(UUID id, UUID teacherId, UUID chapterId, boolean isPublic) {
    QuestionBank qb = new QuestionBank();
    qb.setId(id);
    qb.setTeacherId(teacherId);
    qb.setName("Ngan hang cau hoi Dao ham");
    qb.setDescription("Bo cau hoi cho chuong Dao ham");
    qb.setChapterId(chapterId);
    qb.setIsPublic(isPublic);
    qb.setCreatedAt(Instant.parse("2026-04-20T00:00:00Z"));
    qb.setUpdatedAt(Instant.parse("2026-04-21T00:00:00Z"));
    return qb;
  }

  private User buildUser(UUID id, String fullName) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    return user;
  }

  private Chapter buildChapter(UUID id, String title, Instant deletedAt) {
    Chapter chapter = new Chapter();
    chapter.setId(id);
    chapter.setTitle(title);
    chapter.setDeletedAt(deletedAt);
    return chapter;
  }

  private QuestionTemplate buildTemplate(UUID id, UUID createdBy, UUID questionBankId) {
    QuestionTemplate template = new QuestionTemplate();
    template.setId(id);
    template.setCreatedBy(createdBy);
    template.setQuestionBankId(questionBankId);
    template.setName("Mau cau hoi dao ham");
    return template;
  }

  @Nested
  @DisplayName("createQuestionBank()")
  class CreateQuestionBankTests {
    @Test
    void it_should_create_bank_with_private_default_when_public_flag_is_null() {
      // ===== ARRANGE =====
      QuestionBankRequest request =
          QuestionBankRequest.builder()
              .name("Ngan hang Ham so")
              .description("Danh cho lop 12")
              .isPublic(null)
              .chapterId(null)
              .build();
      when(questionBankRepository.save(any(QuestionBank.class)))
          .thenAnswer(
              invocation -> {
                QuestionBank saved = invocation.getArgument(0);
                saved.setId(BANK_ID);
                return saved;
              });
      when(userRepository.findById(OWNER_ID))
          .thenReturn(Optional.of(buildUser(OWNER_ID, "Pham Dang Khoi")));

      // ===== ACT =====
      QuestionBankResponse response = questionBankService.createQuestionBank(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(BANK_ID, response.getId()),
          () -> assertEquals("Ngan hang Ham so", response.getName()),
          () -> assertFalse(response.getIsPublic()),
          () -> assertEquals("Pham Dang Khoi", response.getTeacherName()));

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).save(any(QuestionBank.class));
      verify(questionBankRepository, times(1)).countQuestionsByQuestionBankId(BANK_ID);
      verify(questionRepository, times(1)).countByCognitiveLevelForBank(BANK_ID);
    }

    @Test
    void it_should_throw_exception_when_chapter_id_not_found_on_create() {
      // ===== ARRANGE =====
      QuestionBankRequest request =
          QuestionBankRequest.builder()
              .name("Ngan hang chuong moi")
              .description("Khong hop le")
              .isPublic(true)
              .chapterId(CHAPTER_ID)
              .build();
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> questionBankService.createQuestionBank(request));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(chapterRepository, times(1)).findById(CHAPTER_ID);
      verify(questionBankRepository, never()).save(any(QuestionBank.class));
    }

    @Test
    void it_should_throw_exception_when_chapter_is_soft_deleted_on_create() {
      // ===== ARRANGE =====
      QuestionBankRequest request =
          QuestionBankRequest.builder()
              .name("Ngan hang chuong da xoa")
              .description("Khong hop le")
              .isPublic(true)
              .chapterId(CHAPTER_ID)
              .build();
      when(chapterRepository.findById(CHAPTER_ID))
          .thenReturn(Optional.of(buildChapter(CHAPTER_ID, "Chuong cu", Instant.now())));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> questionBankService.createQuestionBank(request));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("updateQuestionBank()")
  class UpdateQuestionBankTests {
    @Test
    void it_should_throw_exception_when_bank_not_found_on_update() {
      // ===== ARRANGE =====
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionBankService.updateQuestionBank(
                      BANK_ID, QuestionBankRequest.builder().name("Cap nhat").build()));
      assertEquals(ErrorCode.QUESTION_BANK_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionBankRepository, never()).save(any(QuestionBank.class));
    }

    @Test
    void it_should_throw_exception_when_user_is_not_owner_or_admin_on_update() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionBankService.updateQuestionBank(
                      BANK_ID, QuestionBankRequest.builder().name("Khong du quyen").build()));
      assertEquals(ErrorCode.QUESTION_BANK_ACCESS_DENIED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionBankRepository, never()).save(any(QuestionBank.class));
    }

    @Test
    void it_should_throw_exception_when_bank_has_questions_in_use_on_update() {
      // ===== ARRANGE =====
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  questionBankService.updateQuestionBank(
                      BANK_ID, QuestionBankRequest.builder().name("Khong the sua").build()));
      assertEquals(ErrorCode.QUESTION_BANK_HAS_QUESTIONS_IN_USE, ex.getErrorCode());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionBankRepository, times(1)).hasQuestionsInUse(BANK_ID);
      verify(questionBankRepository, never()).save(any(QuestionBank.class));
    }

    @Test
    void it_should_update_bank_when_request_is_valid_and_public_flag_provided() {
      // ===== ARRANGE =====
      QuestionBankRequest request =
          QuestionBankRequest.builder()
              .name("Ngan hang Toan xac suat")
              .description("Duoc cap nhat")
              .isPublic(true)
              .chapterId(CHAPTER_ID)
              .build();
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(false);
      when(questionBankRepository.save(any(QuestionBank.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      QuestionBankResponse response = questionBankService.updateQuestionBank(BANK_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Ngan hang Toan xac suat", response.getName()),
          () -> assertEquals("Duoc cap nhat", response.getDescription()),
          () -> assertTrue(response.getIsPublic()),
          () -> assertEquals(CHAPTER_ID, response.getChapterId()));

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionBankRepository, times(1)).hasQuestionsInUse(BANK_ID);
      verify(chapterRepository, times(2)).findById(CHAPTER_ID);
      verify(questionBankRepository, times(1)).save(bank);
    }

    @Test
    void it_should_keep_public_status_unchanged_when_request_public_flag_is_null() {
      // ===== ARRANGE =====
      bank.setIsPublic(false);
      QuestionBankRequest request =
          QuestionBankRequest.builder()
              .name("Ngan hang giu nguyen public")
              .description("Cap nhat ten")
              .isPublic(null)
              .chapterId(CHAPTER_ID)
              .build();
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(false);
      when(questionBankRepository.save(any(QuestionBank.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      // ===== ACT =====
      QuestionBankResponse response = questionBankService.updateQuestionBank(BANK_ID, request);

      // ===== ASSERT =====
      assertFalse(response.getIsPublic());
    }
  }

  @Nested
  @DisplayName("deleteQuestionBank()")
  class DeleteQuestionBankTests {
    @Test
    void it_should_throw_exception_when_questions_in_use_on_delete() {
      // ===== ARRANGE =====
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> questionBankService.deleteQuestionBank(BANK_ID));
      assertEquals(ErrorCode.QUESTION_BANK_HAS_QUESTIONS_IN_USE, ex.getErrorCode());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).hasQuestionsInUse(BANK_ID);
      verify(questionRepository, never()).detachFreeQuestionsFromBank(BANK_ID);
      verify(questionBankRepository, never()).save(any(QuestionBank.class));
    }

    @Test
    void it_should_soft_delete_bank_after_detaching_free_questions() {
      // ===== ARRANGE =====
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(false);
      when(questionRepository.detachFreeQuestionsFromBank(BANK_ID)).thenReturn(2);

      // ===== ACT =====
      questionBankService.deleteQuestionBank(BANK_ID);

      // ===== ASSERT =====
      assertNotNull(bank.getDeletedAt());

      // ===== VERIFY =====
      verify(questionRepository, times(1)).detachFreeQuestionsFromBank(BANK_ID);
      verify(questionBankRepository, times(1)).save(bank);
    }

    @Test
    void it_should_soft_delete_bank_when_no_free_questions_are_detached() {
      // ===== ARRANGE =====
      QuestionBank localBank = buildBank(BANK_ID, OWNER_ID, CHAPTER_ID, false);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(localBank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(false);
      when(questionRepository.detachFreeQuestionsFromBank(BANK_ID)).thenReturn(0);

      // ===== ACT =====
      questionBankService.deleteQuestionBank(BANK_ID);

      // ===== ASSERT =====
      assertNotNull(localBank.getDeletedAt());
    }
  }

  @Nested
  @DisplayName("template mapping methods")
  class TemplateMappingTests {
    @Test
    void it_should_throw_exception_when_template_not_found_on_map() {
      // ===== ARRANGE =====
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionTemplateRepository.findByIdWithCreatorAndNotDeleted(TEMPLATE_ID))
          .thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class, () -> questionBankService.mapTemplateToBank(BANK_ID, TEMPLATE_ID));
      assertEquals(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(questionTemplateRepository, times(1)).findByIdWithCreatorAndNotDeleted(TEMPLATE_ID);
      verify(questionTemplateRepository, never()).save(any(QuestionTemplate.class));
    }

    @Test
    void it_should_throw_exception_when_template_not_in_bank_on_unmap() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildTemplate(TEMPLATE_ID, OWNER_ID, UUID.randomUUID());
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionTemplateRepository.findByIdWithCreatorAndNotDeleted(TEMPLATE_ID))
          .thenReturn(Optional.of(template));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> questionBankService.unmapTemplateFromBank(BANK_ID, TEMPLATE_ID));
      assertEquals(ErrorCode.QUESTION_TEMPLATE_NOT_IN_BANK, ex.getErrorCode());

      // ===== VERIFY =====
      verify(questionTemplateRepository, never()).save(any(QuestionTemplate.class));
    }

    @Test
    void it_should_map_template_to_bank_when_template_owned_by_current_user() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildTemplate(TEMPLATE_ID, OWNER_ID, null);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionTemplateRepository.findByIdWithCreatorAndNotDeleted(TEMPLATE_ID))
          .thenReturn(Optional.of(template));
      when(questionTemplateRepository.save(template)).thenReturn(template);
      when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(buildUser(OWNER_ID, "Le Gia Bao")));

      // ===== ACT =====
      QuestionTemplateResponse response = questionBankService.mapTemplateToBank(BANK_ID, TEMPLATE_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(BANK_ID, response.getQuestionBankId()),
          () -> assertEquals("Le Gia Bao", response.getCreatorName()));

      // ===== VERIFY =====
      verify(questionTemplateRepository, times(1)).save(template);
    }

    @Test
    void it_should_unmap_template_from_bank_when_template_belongs_to_bank() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildTemplate(TEMPLATE_ID, OWNER_ID, BANK_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionTemplateRepository.findByIdWithCreatorAndNotDeleted(TEMPLATE_ID))
          .thenReturn(Optional.of(template));

      // ===== ACT =====
      questionBankService.unmapTemplateFromBank(BANK_ID, TEMPLATE_ID);

      // ===== ASSERT =====
      assertNull(template.getQuestionBankId());

      // ===== VERIFY =====
      verify(questionTemplateRepository, times(1)).save(template);
    }

    @Test
    void it_should_throw_exception_when_mapping_template_without_template_permission() {
      // ===== ARRANGE =====
      QuestionTemplate template = buildTemplate(TEMPLATE_ID, OTHER_ID, null);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionTemplateRepository.findByIdWithCreatorAndNotDeleted(TEMPLATE_ID))
          .thenReturn(Optional.of(template));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class, () -> questionBankService.mapTemplateToBank(BANK_ID, TEMPLATE_ID));
      assertEquals(ErrorCode.TEMPLATE_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_allow_mapping_template_for_admin_even_when_template_owned_by_other_user() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(ADMIN_ID);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
      QuestionTemplate template = buildTemplate(TEMPLATE_ID, OTHER_ID, null);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionTemplateRepository.findByIdWithCreatorAndNotDeleted(TEMPLATE_ID))
          .thenReturn(Optional.of(template));
      when(questionTemplateRepository.save(template)).thenReturn(template);
      when(userRepository.findById(OTHER_ID)).thenReturn(Optional.of(buildUser(OTHER_ID, "Tran Minh Tam")));

      // ===== ACT =====
      QuestionTemplateResponse response = questionBankService.mapTemplateToBank(BANK_ID, TEMPLATE_ID);

      // ===== ASSERT =====
      assertEquals(BANK_ID, response.getQuestionBankId());
    }
  }

  @Nested
  @DisplayName("read and search methods")
  class ReadAndSearchTests {
    @Test
    void it_should_throw_exception_when_get_mapped_templates_without_access() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
      QuestionBank privateBank = buildBank(BANK_ID, OWNER_ID, CHAPTER_ID, false);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(privateBank));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> questionBankService.getMappedTemplates(BANK_ID));
      assertEquals(ErrorCode.QUESTION_BANK_ACCESS_DENIED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(questionTemplateRepository, never()).findByQuestionBankIdAndNotDeleted(any(UUID.class));
    }

    @Test
    void it_should_return_mapped_templates_when_bank_is_public_even_for_non_owner() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
      QuestionBank publicBank = buildBank(BANK_ID, OWNER_ID, CHAPTER_ID, true);
      QuestionTemplate template = buildTemplate(TEMPLATE_ID, OWNER_ID, BANK_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(publicBank));
      when(questionTemplateRepository.findByQuestionBankIdAndNotDeleted(BANK_ID))
          .thenReturn(List.of(template));

      // ===== ACT =====
      List<QuestionTemplateResponse> responses = questionBankService.getMappedTemplates(BANK_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, responses.size()),
          () -> assertEquals(TEMPLATE_ID, responses.get(0).getId()),
          () -> assertEquals("Le Gia Bao", responses.get(0).getCreatorName()));

      // ===== VERIFY =====
      verify(questionTemplateRepository, times(1)).findByQuestionBankIdAndNotDeleted(BANK_ID);
    }

    @Test
    void it_should_throw_exception_when_get_by_id_without_access_and_not_public() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> questionBankService.getQuestionBankById(BANK_ID));
      assertEquals(ErrorCode.QUESTION_BANK_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_return_question_bank_for_admin_even_when_not_owner_and_private() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));

      // ===== ACT =====
      QuestionBankResponse response = questionBankService.getQuestionBankById(BANK_ID);

      // ===== ASSERT =====
      assertEquals(BANK_ID, response.getId());
    }

    @Test
    void it_should_search_all_active_when_admin_and_mine_only_false() {
      // ===== ARRANGE =====
      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
      Page<QuestionBank> page = new PageImpl<>(List.of(bank), PageRequest.of(0, 10), 1);
      when(questionBankRepository.searchAllActiveByChapterAndName(eq(CHAPTER_ID), eq(null), any(PageRequest.class)))
          .thenReturn(page);

      // ===== ACT =====
      Page<QuestionBankResponse> responsePage =
          questionBankService.searchQuestionBanks("   ", CHAPTER_ID, false, PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertEquals(1, responsePage.getTotalElements());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1))
          .searchAllActiveByChapterAndName(eq(CHAPTER_ID), eq(null), any(PageRequest.class));
      verify(questionBankRepository, never())
          .searchMineByChapterAndName(any(UUID.class), any(UUID.class), any(), any(PageRequest.class));
    }

    @Test
    void it_should_force_mine_search_for_non_admin_even_when_mine_only_false() {
      // ===== ARRANGE =====
      Page<QuestionBank> page = new PageImpl<>(List.of(bank), PageRequest.of(0, 10), 1);
      when(questionBankRepository.searchMineByChapterAndName(
              eq(OWNER_ID), eq(CHAPTER_ID), eq("dao ham"), any(PageRequest.class)))
          .thenReturn(page);

      // ===== ACT =====
      Page<QuestionBankResponse> responsePage =
          questionBankService.searchQuestionBanks(
              "  dao ham  ", CHAPTER_ID, false, PageRequest.of(0, 10));

      // ===== ASSERT =====
      assertEquals(1, responsePage.getTotalElements());

      // ===== VERIFY =====
      verify(questionBankRepository, times(1))
          .searchMineByChapterAndName(eq(OWNER_ID), eq(CHAPTER_ID), eq("dao ham"), any(PageRequest.class));
      verify(questionBankRepository, never())
          .searchAllActiveByChapterAndName(any(UUID.class), any(), any(PageRequest.class));
    }

    @Test
    void it_should_return_my_question_banks_page_for_current_user() {
      // ===== ARRANGE =====
      Page<QuestionBank> page = new PageImpl<>(List.of(bank), PageRequest.of(0, 5), 1);
      when(questionBankRepository.findByTeacherIdAndNotDeleted(OWNER_ID, PageRequest.of(0, 5)))
          .thenReturn(page);

      // ===== ACT =====
      Page<QuestionBankResponse> result = questionBankService.getMyQuestionBanks(PageRequest.of(0, 5));

      // ===== ASSERT =====
      assertEquals(1, result.getTotalElements());
    }
  }

  @Nested
  @DisplayName("permission helpers")
  class PermissionTests {
    @Test
    void it_should_return_false_for_can_delete_when_user_cannot_edit() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));

      // ===== ACT =====
      boolean canDelete = questionBankService.canDeleteQuestionBank(BANK_ID);

      // ===== ASSERT =====
      assertFalse(canDelete);

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
      verify(questionBankRepository, never()).hasQuestionsInUse(BANK_ID);
    }

    @Test
    void it_should_return_true_for_can_edit_when_admin() {
      // ===== ARRANGE =====
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(ADMIN_ID);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));

      // ===== ACT =====
      boolean canEdit = questionBankService.canEditQuestionBank(BANK_ID);

      // ===== ASSERT =====
      assertTrue(canEdit);

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).findByIdAndNotDeleted(BANK_ID);
    }

    @Test
    void it_should_return_false_for_can_delete_when_questions_are_in_use() {
      // ===== ARRANGE =====
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(true);

      // ===== ACT =====
      boolean canDelete = questionBankService.canDeleteQuestionBank(BANK_ID);

      // ===== ASSERT =====
      assertFalse(canDelete);

      // ===== VERIFY =====
      verify(questionBankRepository, times(1)).hasQuestionsInUse(BANK_ID);
    }

    @Test
    void it_should_return_true_for_can_delete_when_owner_and_questions_not_in_use() {
      // ===== ARRANGE =====
      when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
      when(questionBankRepository.hasQuestionsInUse(BANK_ID)).thenReturn(false);

      // ===== ACT =====
      boolean canDelete = questionBankService.canDeleteQuestionBank(BANK_ID);

      // ===== ASSERT =====
      assertTrue(canDelete);
    }
  }

  @Test
  void it_should_toggle_public_status_when_owner_requests_toggle() {
    // ===== ARRANGE =====
    bank.setIsPublic(false);
    when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));
    when(questionBankRepository.save(bank)).thenReturn(bank);

    // ===== ACT =====
    QuestionBankResponse response = questionBankService.togglePublicStatus(BANK_ID);

    // ===== ASSERT =====
    assertTrue(response.getIsPublic());

    // ===== VERIFY =====
    verify(questionBankRepository, times(1)).save(bank);
  }

  @Test
  void it_should_throw_exception_when_toggle_public_status_without_permission() {
    // ===== ARRANGE =====
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
    when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(bank));

    // ===== ACT & ASSERT =====
    AppException ex =
        assertThrows(AppException.class, () -> questionBankService.togglePublicStatus(BANK_ID));
    assertEquals(ErrorCode.QUESTION_BANK_ACCESS_DENIED, ex.getErrorCode());
  }

  @Test
  void it_should_return_question_bank_with_null_chapter_title_when_chapter_missing() {
    // ===== ARRANGE =====
    QuestionBank withoutChapter = buildBank(BANK_ID, OWNER_ID, null, false);
    when(questionBankRepository.findByIdAndNotDeleted(BANK_ID)).thenReturn(Optional.of(withoutChapter));

    // ===== ACT =====
    QuestionBankResponse response = questionBankService.getQuestionBankById(BANK_ID);

    // ===== ASSERT =====
    assertNull(response.getChapterTitle());
  }
}
