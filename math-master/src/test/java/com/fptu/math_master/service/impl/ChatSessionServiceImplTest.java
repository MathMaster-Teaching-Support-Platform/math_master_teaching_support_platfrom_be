package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import com.fptu.math_master.dto.request.CreateChatSessionRequest;
import com.fptu.math_master.dto.request.SendChatMessageRequest;
import com.fptu.math_master.dto.request.UpdateChatSessionRequest;
import com.fptu.math_master.dto.response.ChatExchangeResponse;
import com.fptu.math_master.dto.response.ChatMemoryInfoResponse;
import com.fptu.math_master.dto.response.ChatMessageResponse;
import com.fptu.math_master.dto.response.ChatSessionResponse;
import com.fptu.math_master.entity.ChatMessage;
import com.fptu.math_master.entity.ChatSession;
import com.fptu.math_master.enums.ChatMessageRole;
import com.fptu.math_master.enums.ChatSessionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChatMessageRepository;
import com.fptu.math_master.repository.ChatSessionRepository;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.UserSubscriptionService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("ChatSessionServiceImpl - Tests")
class ChatSessionServiceImplTest extends BaseUnitTest {

  @InjectMocks private ChatSessionServiceImpl chatSessionService;

  @Mock private ChatSessionRepository chatSessionRepository;
  @Mock private ChatMessageRepository chatMessageRepository;
  @Mock private GeminiService geminiService;
  @Mock private UserSubscriptionService userSubscriptionService;
  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOperations;

  private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private MockedStatic<SecurityUtils> securityUtilsMock;
  private ChatSession activeSession;

  @BeforeEach
  void setUp() {
    securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    activeSession = buildSession(SESSION_ID, USER_ID, "New Chat", ChatSessionStatus.ACTIVE);
    activeSession.setTotalMessages(0);
    activeSession.setTotalWords(0);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
    SecurityContextHolder.clearContext();
    ChatSessionServiceImpl.WORD_LIMIT = 1000;
  }

  private ChatSession buildSession(UUID id, UUID userId, String title, ChatSessionStatus status) {
    ChatSession session = new ChatSession();
    session.setId(id);
    session.setUserId(userId);
    session.setTitle(title);
    session.setStatus(status);
    session.setModel("gemini-2.5-flash");
    session.setTotalMessages(0);
    session.setTotalWords(0);
    session.setCreatedAt(Instant.parse("2026-04-26T01:00:00Z"));
    session.setUpdatedAt(Instant.parse("2026-04-26T01:00:00Z"));
    session.setLastMessageAt(Instant.parse("2026-04-26T01:00:00Z"));
    return session;
  }

  private ChatMessage buildMessage(
      UUID id,
      UUID sessionId,
      UUID userId,
      ChatMessageRole role,
      String content,
      Integer wordCount,
      Long sequenceNo,
      Instant createdAt) {
    ChatMessage message = new ChatMessage();
    message.setId(id);
    message.setSessionId(sessionId);
    message.setUserId(userId);
    message.setRole(role);
    message.setContent(content);
    message.setWordCount(wordCount);
    message.setSequenceNo(sequenceNo);
    message.setCreatedAt(createdAt);
    return message;
  }

  @Nested
  @DisplayName("createSession()")
  class CreateSessionTests {

    /**
     * Normal case: Tao session moi voi title/model duoc normalize.
     *
     * <p>Input:
     * <ul>
     *   <li>title: blank</li>
     *   <li>model: blank</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>normalizeTitle -> blank branch</li>
     *   <li>normalizeModel -> blank branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Session duoc save voi title "New Chat" va model default</li>
     * </ul>
     */
    @Test
    void it_should_create_session_with_default_title_and_model_when_input_is_blank() {
      // ===== ARRANGE =====
      CreateChatSessionRequest request = CreateChatSessionRequest.builder().title(" ").model(" ").build();
      when(chatSessionRepository.save(any(ChatSession.class)))
          .thenAnswer(invocation -> {
            ChatSession saved = invocation.getArgument(0);
            saved.setId(SESSION_ID);
            return saved;
          });

      // ===== ACT =====
      ChatSessionResponse response = chatSessionService.createSession(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(SESSION_ID, response.getId()),
          () -> assertEquals("New Chat", response.getTitle()),
          () -> assertEquals("gemini-2.5-flash", response.getModel()),
          () -> assertEquals(ChatSessionStatus.ACTIVE, response.getStatus()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
      verifyNoMoreInteractions(chatSessionRepository);
    }

    @Test
    void it_should_create_session_with_trimmed_title_and_model_when_input_is_present() {
      // ===== ARRANGE =====
      CreateChatSessionRequest request =
          CreateChatSessionRequest.builder().title("  Tro ly Hinh hoc  ").model("  gemini-pro  ").build();
      when(chatSessionRepository.save(any(ChatSession.class)))
          .thenAnswer(
              invocation -> {
                ChatSession saved = invocation.getArgument(0);
                saved.setId(SESSION_ID);
                return saved;
              });

      // ===== ACT =====
      ChatSessionResponse response = chatSessionService.createSession(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Tro ly Hinh hoc", response.getTitle()),
          () -> assertEquals("gemini-pro", response.getModel()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
      verifyNoMoreInteractions(chatSessionRepository);
    }
  }

  @Nested
  @DisplayName("getSession()")
  class GetSessionTests {

    @Test
    void it_should_return_session_when_owned_by_current_user() {
      // ===== ARRANGE =====
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));

      // ===== ACT =====
      ChatSessionResponse response = chatSessionService.getSession(SESSION_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(SESSION_ID, response.getId()),
          () -> assertEquals(USER_ID, response.getUserId()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verifyNoMoreInteractions(chatSessionRepository);
    }

    @Test
    void it_should_throw_exception_when_session_not_found() {
      // ===== ARRANGE =====
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> chatSessionService.getSession(SESSION_ID));
      assertEquals(ErrorCode.CHAT_SESSION_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verifyNoMoreInteractions(chatSessionRepository);
    }

    @Test
    void it_should_throw_exception_when_session_owned_by_other_user() {
      // ===== ARRANGE =====
      ChatSession session = buildSession(SESSION_ID, OTHER_USER_ID, "Other", ChatSessionStatus.ACTIVE);
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(session));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> chatSessionService.getSession(SESSION_ID));
      assertEquals(ErrorCode.CHAT_SESSION_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verifyNoMoreInteractions(chatSessionRepository);
    }
  }

  @Nested
  @DisplayName("sendMessage()")
  class SendMessageTests {

    @Test
    void it_should_throw_exception_when_session_is_archived() {
      // ===== ARRANGE =====
      ChatSession archived = buildSession(SESSION_ID, USER_ID, "Archived", ChatSessionStatus.ARCHIVED);
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(archived));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> chatSessionService.sendMessage(SESSION_ID, SendChatMessageRequest.builder().prompt("Toan cao cap").build()));
      assertEquals(ErrorCode.CHAT_SESSION_ARCHIVED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(userSubscriptionService, times(1)).consumeMyTokens(1, "CHAT");
      verify(chatMessageRepository, never()).save(any(ChatMessage.class));
      verifyNoMoreInteractions(chatSessionRepository, userSubscriptionService, chatMessageRepository);
    }

    @Test
    void it_should_throw_exception_when_prompt_is_empty() {
      // ===== ARRANGE =====
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> chatSessionService.sendMessage(SESSION_ID, SendChatMessageRequest.builder().prompt("   ").build()));
      assertEquals(ErrorCode.CHAT_PROMPT_EMPTY, exception.getErrorCode());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(userSubscriptionService, times(1)).consumeMyTokens(1, "CHAT");
      verify(chatMessageRepository, never()).findMaxSequenceNoBySessionId(any());
      verifyNoMoreInteractions(chatSessionRepository, userSubscriptionService, chatMessageRepository);
    }

    @Test
    void it_should_throw_exception_when_ai_call_fails() {
      // ===== ARRANGE =====
      ChatMessage userMessage =
          buildMessage(
              UUID.fromString("22222222-2222-2222-2222-222222222222"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "Giai bai toan nay",
              4,
              1L,
              Instant.parse("2026-04-26T01:10:00Z"));

      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatMessageRepository.findMaxSequenceNoBySessionId(SESSION_ID)).thenReturn(0L);
      when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(userMessage);
      when(valueOperations.get("chat:ctx:" + SESSION_ID)).thenReturn(null);
      when(chatMessageRepository.findBySessionIdAndDeletedAtIsNull(eq(SESSION_ID), any(PageRequest.class)))
          .thenReturn(Page.empty());
      when(geminiService.sendMessage(any())).thenThrow(new RuntimeException("AI timeout"));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () ->
                  chatSessionService.sendMessage(
                      SESSION_ID, SendChatMessageRequest.builder().prompt("Giai bai toan nay").build()));
      assertEquals(ErrorCode.CHAT_AI_CALL_FAILED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(userSubscriptionService, times(1)).consumeMyTokens(1, "CHAT");
      verify(chatMessageRepository, times(1)).findMaxSequenceNoBySessionId(SESSION_ID);
      verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
      verify(geminiService, times(1)).sendMessage(any());
    }

    @Test
    void it_should_send_message_successfully_and_trim_memory_when_word_limit_exceeded() {
      // ===== ARRANGE =====
      ChatSessionServiceImpl.ChatMemoryContext context = ChatSessionServiceImpl.ChatMemoryContext.builder()
          .messages(new ArrayList<>(List.of(
              ChatSessionServiceImpl.MemoryMessage.builder()
                  .role("USER")
                  .content("alpha beta gamma")
                  .wordCount(3)
                  .createdAt(Instant.parse("2026-04-26T01:01:00Z"))
                  .build())))
          .totalWords(3)
          .build();

      ChatMessage userMessage =
          buildMessage(
              UUID.fromString("33333333-3333-3333-3333-333333333333"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "mot hai ba bon",
              4,
              1L,
              Instant.parse("2026-04-26T01:10:00Z"));

      ChatMessage assistantMessage =
          buildMessage(
              UUID.fromString("44444444-4444-4444-4444-444444444444"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.ASSISTANT,
              "nam sau bay",
              3,
              2L,
              Instant.parse("2026-04-26T01:10:02Z"));

      ChatSessionServiceImpl.WORD_LIMIT = 4;
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatMessageRepository.findMaxSequenceNoBySessionId(SESSION_ID)).thenReturn(0L);
      when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(userMessage, assistantMessage);
      when(valueOperations.get("chat:ctx:" + SESSION_ID)).thenReturn(context);
      when(geminiService.sendMessage(any())).thenReturn("nam sau bay");
      when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(activeSession);

      // ===== ACT =====
      ChatExchangeResponse response =
          chatSessionService.sendMessage(
              SESSION_ID, SendChatMessageRequest.builder().prompt("mot hai ba bon").build());

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertNotNull(response.getUserMessage()),
          () -> assertNotNull(response.getAssistantMessage()),
          () -> assertTrue(response.getMemory().getTrimmed()),
          () -> assertTrue(response.getMemory().getCurrentWords() <= 4),
          () -> assertEquals("mot hai ba bon", response.getUserMessage().getContent()),
          () -> assertEquals("nam sau bay", response.getAssistantMessage().getContent()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(userSubscriptionService, times(1)).consumeMyTokens(1, "CHAT");
      verify(chatMessageRepository, times(1)).findMaxSequenceNoBySessionId(SESSION_ID);
      verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
      verify(geminiService, times(1)).sendMessage(any());
      verify(chatSessionRepository, times(1)).save(activeSession);
      verify(valueOperations, times(1)).set(eq("chat:ctx:" + SESSION_ID), any(), eq(ChatSessionServiceImpl.MEMORY_TTL));
    }
  }

  @Nested
  @DisplayName("updateSession()")
  class UpdateSessionTests {

    @Test
    void it_should_update_title_when_request_is_valid() {
      // ===== ARRANGE =====
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatSessionRepository.save(activeSession)).thenReturn(activeSession);

      // ===== ACT =====
      ChatSessionResponse response =
          chatSessionService.updateSession(
              SESSION_ID, UpdateChatSessionRequest.builder().title("  Luyen de gioi han  ").build());

      // ===== ASSERT =====
      assertEquals("Luyen de gioi han", response.getTitle());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(chatSessionRepository, times(1)).save(activeSession);
      verifyNoMoreInteractions(chatSessionRepository);
    }
  }

  @Nested
  @DisplayName("archiveSession()")
  class ArchiveSessionTests {

    @Test
    void it_should_archive_session_when_session_exists() {
      // ===== ARRANGE =====
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatSessionRepository.save(activeSession)).thenReturn(activeSession);

      // ===== ACT =====
      ChatSessionResponse response = chatSessionService.archiveSession(SESSION_ID);

      // ===== ASSERT =====
      assertEquals(ChatSessionStatus.ARCHIVED, response.getStatus());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(chatSessionRepository, times(1)).save(activeSession);
      verifyNoMoreInteractions(chatSessionRepository);
    }
  }

  @Nested
  @DisplayName("deleteSession()")
  class DeleteSessionTests {

    @Test
    void it_should_soft_delete_session_and_messages_when_messages_exist() {
      // ===== ARRANGE =====
      ChatMessage first =
          buildMessage(
              UUID.fromString("55555555-5555-5555-5555-555555555555"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "A",
              1,
              1L,
              Instant.parse("2026-04-26T01:11:00Z"));
      ChatMessage second =
          buildMessage(
              UUID.fromString("66666666-6666-6666-6666-666666666666"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.ASSISTANT,
              "B",
              1,
              2L,
              Instant.parse("2026-04-26T01:11:01Z"));

      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatMessageRepository.findAllBySessionIdAndDeletedAtIsNull(SESSION_ID))
          .thenReturn(List.of(first, second));

      // ===== ACT =====
      chatSessionService.deleteSession(SESSION_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(activeSession.getDeletedAt()),
          () -> assertEquals(USER_ID, activeSession.getDeletedBy()),
          () -> assertNotNull(first.getDeletedAt()),
          () -> assertEquals(USER_ID, first.getDeletedBy()),
          () -> assertNotNull(second.getDeletedAt()),
          () -> assertEquals(USER_ID, second.getDeletedBy()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(chatSessionRepository, times(1)).save(activeSession);
      verify(chatMessageRepository, times(1)).findAllBySessionIdAndDeletedAtIsNull(SESSION_ID);
      verify(chatMessageRepository, times(1)).saveAll(List.of(first, second));
      verify(redisTemplate, times(1)).delete("chat:ctx:" + SESSION_ID);
    }

    @Test
    void it_should_not_call_save_all_when_session_has_no_messages() {
      // ===== ARRANGE =====
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatMessageRepository.findAllBySessionIdAndDeletedAtIsNull(SESSION_ID)).thenReturn(List.of());

      // ===== ACT =====
      chatSessionService.deleteSession(SESSION_ID);

      // ===== ASSERT =====
      assertNotNull(activeSession.getDeletedAt());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(chatSessionRepository, times(1)).save(activeSession);
      verify(chatMessageRepository, times(1)).findAllBySessionIdAndDeletedAtIsNull(SESSION_ID);
      verify(chatMessageRepository, never()).saveAll(any());
      verify(redisTemplate, times(1)).delete("chat:ctx:" + SESSION_ID);
    }
  }

  @Nested
  @DisplayName("getMySessions()")
  class GetMySessionsTests {

    @Test
    void it_should_pass_null_keyword_pattern_when_keyword_is_blank() {
      // ===== ARRANGE =====
      PageRequest pageable = PageRequest.of(0, 10);
      when(chatSessionRepository.findByUserAndFilters(USER_ID, ChatSessionStatus.ACTIVE, null, pageable))
          .thenReturn(new PageImpl<>(List.of(activeSession)));

      // ===== ACT =====
      Page<ChatSessionResponse> response =
          chatSessionService.getMySessions(ChatSessionStatus.ACTIVE, "   ", pageable);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, response.getTotalElements()), () -> assertEquals("New Chat", response.getContent().get(0).getTitle()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1))
          .findByUserAndFilters(USER_ID, ChatSessionStatus.ACTIVE, null, pageable);
      verifyNoMoreInteractions(chatSessionRepository);
    }

    @Test
    void it_should_build_like_pattern_when_keyword_is_provided() {
      // ===== ARRANGE =====
      PageRequest pageable = PageRequest.of(0, 10);
      when(chatSessionRepository.findByUserAndFilters(USER_ID, null, "%toan roi rac%", pageable))
          .thenReturn(new PageImpl<>(List.of(activeSession)));

      // ===== ACT =====
      Page<ChatSessionResponse> response = chatSessionService.getMySessions(null, "  ToAn Roi Rac  ", pageable);

      // ===== ASSERT =====
      assertEquals(1, response.getContent().size());

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1))
          .findByUserAndFilters(USER_ID, null, "%toan roi rac%", pageable);
      verifyNoMoreInteractions(chatSessionRepository);
    }
  }

  @Nested
  @DisplayName("getSessionMessages()")
  class GetSessionMessagesTests {

    @Test
    void it_should_return_session_messages_when_session_is_owned() {
      // ===== ARRANGE =====
      PageRequest pageable = PageRequest.of(0, 10);
      ChatMessage message =
          buildMessage(
              UUID.fromString("77777777-7777-7777-7777-777777777777"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "Hoi dap toan hoc",
              3,
              1L,
              Instant.parse("2026-04-26T01:20:00Z"));
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatMessageRepository.findBySessionIdAndDeletedAtIsNull(SESSION_ID, pageable))
          .thenReturn(new PageImpl<>(List.of(message)));

      // ===== ACT =====
      Page<ChatMessageResponse> response = chatSessionService.getSessionMessages(SESSION_ID, pageable);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, response.getTotalElements()),
          () -> assertEquals(ChatMessageRole.USER, response.getContent().get(0).getRole()),
          () -> assertEquals("Hoi dap toan hoc", response.getContent().get(0).getContent()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(chatMessageRepository, times(1)).findBySessionIdAndDeletedAtIsNull(SESSION_ID, pageable);
      verifyNoMoreInteractions(chatSessionRepository, chatMessageRepository);
    }
  }

  @Nested
  @DisplayName("getMemoryInfo()")
  class GetMemoryInfoTests {

    @Test
    void it_should_rebuild_memory_context_from_messages_when_redis_cache_is_missing() {
      // ===== ARRANGE =====
      ChatMessage older =
          buildMessage(
              UUID.fromString("88888888-8888-8888-8888-888888888888"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "xin chao ban",
              null,
              1L,
              Instant.parse("2026-04-26T00:59:00Z"));
      ChatMessage newer =
          buildMessage(
              UUID.fromString("99999999-9999-9999-9999-999999999999"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.ASSISTANT,
              "toi la tro ly",
              4,
              2L,
              Instant.parse("2026-04-26T01:00:00Z"));

      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(valueOperations.get("chat:ctx:" + SESSION_ID)).thenReturn(null);
      when(chatMessageRepository.findBySessionIdAndDeletedAtIsNull(eq(SESSION_ID), any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(newer, older)));

      // ===== ACT =====
      ChatMemoryInfoResponse response = chatSessionService.getMemoryInfo(SESSION_ID);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals(7, response.getCurrentWords()),
          () -> assertEquals(2, response.getMessageCount()),
          () -> assertFalse(response.getTrimmed()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(chatMessageRepository, times(1))
          .findBySessionIdAndDeletedAtIsNull(eq(SESSION_ID), any(PageRequest.class));
      verify(valueOperations, times(1)).set(eq("chat:ctx:" + SESSION_ID), any(), eq(ChatSessionServiceImpl.MEMORY_TTL));
    }

    @Test
    void it_should_return_cached_memory_context_when_redis_has_context() {
      // ===== ARRANGE =====
      ChatSessionServiceImpl.ChatMemoryContext cachedContext =
          ChatSessionServiceImpl.ChatMemoryContext.builder()
              .messages(
                  List.of(
                      ChatSessionServiceImpl.MemoryMessage.builder()
                          .role("USER")
                          .content("da co cache")
                          .wordCount(3)
                          .createdAt(Instant.parse("2026-04-26T01:00:00Z"))
                          .build()))
              .totalWords(3)
              .build();
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(valueOperations.get("chat:ctx:" + SESSION_ID)).thenReturn(cachedContext);

      // ===== ACT =====
      ChatMemoryInfoResponse response = chatSessionService.getMemoryInfo(SESSION_ID);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(3, response.getCurrentWords()), () -> assertEquals(1, response.getMessageCount()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).findByIdAndNotDeleted(SESSION_ID);
      verify(chatMessageRepository, never()).findBySessionIdAndDeletedAtIsNull(eq(SESSION_ID), any());
    }

    @Test
    void it_should_stop_rebuild_when_first_message_exceeds_word_limit() {
      // ===== ARRANGE =====
      ChatMessage hugeMessage =
          buildMessage(
              UUID.fromString("12121212-1212-1212-1212-121212121212"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "mot hai ba bon nam sau",
              6,
              1L,
              Instant.parse("2026-04-26T01:00:00Z"));
      ChatSessionServiceImpl.WORD_LIMIT = 4;
      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(valueOperations.get("chat:ctx:" + SESSION_ID)).thenReturn(null);
      when(chatMessageRepository.findBySessionIdAndDeletedAtIsNull(eq(SESSION_ID), any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(hugeMessage)));

      // ===== ACT =====
      ChatMemoryInfoResponse response = chatSessionService.getMemoryInfo(SESSION_ID);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(0, response.getCurrentWords()), () -> assertEquals(0, response.getMessageCount()));

      // ===== VERIFY =====
      verify(chatMessageRepository, times(1))
          .findBySessionIdAndDeletedAtIsNull(eq(SESSION_ID), any(PageRequest.class));
    }
  }

  @Nested
  @DisplayName("sendMessage() extra branches")
  class SendMessageExtraBranchTests {

    @Test
    void it_should_generate_title_and_set_default_model_when_first_message_and_session_fields_are_null() {
      // ===== ARRANGE =====
      ChatSession firstSession = buildSession(SESSION_ID, USER_ID, null, ChatSessionStatus.ACTIVE);
      firstSession.setModel("  ");
      firstSession.setTotalMessages(0);
      firstSession.setTotalWords(null);

      ChatSessionServiceImpl.ChatMemoryContext nullMessagesContext =
          ChatSessionServiceImpl.ChatMemoryContext.builder().messages(null).totalWords(0).build();

      ChatMessage userMessage =
          buildMessage(
              UUID.fromString("13131313-1313-1313-1313-131313131313"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "mot hai ba bon nam sau bay tam chin",
              9,
              1L,
              Instant.parse("2026-04-26T01:05:00Z"));
      ChatMessage assistantMessage =
          buildMessage(
              UUID.fromString("14141414-1414-1414-1414-141414141414"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.ASSISTANT,
              "tra loi",
              2,
              2L,
              Instant.parse("2026-04-26T01:05:02Z"));

      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(firstSession));
      when(chatMessageRepository.findMaxSequenceNoBySessionId(SESSION_ID)).thenReturn(0L);
      when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(userMessage, assistantMessage);
      when(valueOperations.get("chat:ctx:" + SESSION_ID)).thenReturn(nullMessagesContext);
      when(geminiService.sendMessage(any())).thenReturn("tra loi");
      when(chatSessionRepository.save(firstSession)).thenReturn(firstSession);

      // ===== ACT =====
      ChatExchangeResponse response =
          chatSessionService.sendMessage(
              SESSION_ID,
              SendChatMessageRequest.builder().prompt("mot hai ba bon nam sau bay tam chin").build());

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertEquals("mot hai ba bon nam sau bay tam", firstSession.getTitle()),
          () -> assertEquals("gemini-2.5-flash", firstSession.getModel()),
          () -> assertEquals(2, firstSession.getTotalMessages()),
          () -> assertEquals(11, firstSession.getTotalWords()));

      // ===== VERIFY =====
      verify(chatSessionRepository, times(1)).save(firstSession);
      verify(geminiService, times(1)).sendMessage(any());
    }

    @Test
    void it_should_trim_with_null_removed_word_count_and_null_role_in_prompt_history() {
      // ===== ARRANGE =====
      ChatSessionServiceImpl.WORD_LIMIT = 2;
      ChatSessionServiceImpl.ChatMemoryContext context =
          ChatSessionServiceImpl.ChatMemoryContext.builder()
              .messages(
                  new ArrayList<>(
                      List.of(
                          ChatSessionServiceImpl.MemoryMessage.builder()
                              .role(null)
                              .content("x y z")
                              .wordCount(null)
                              .createdAt(Instant.parse("2026-04-26T01:00:00Z"))
                              .build(),
                          ChatSessionServiceImpl.MemoryMessage.builder()
                              .role("USER")
                              .content("p q r s t")
                              .wordCount(100)
                              .createdAt(Instant.parse("2026-04-26T01:00:01Z"))
                              .build())))
              .totalWords(100)
              .build();

      ChatMessage userMessage =
          buildMessage(
              UUID.fromString("15151515-1515-1515-1515-151515151515"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.USER,
              "mot",
              1,
              1L,
              Instant.parse("2026-04-26T01:07:00Z"));
      ChatMessage assistantMessage =
          buildMessage(
              UUID.fromString("16161616-1616-1616-1616-161616161616"),
              SESSION_ID,
              USER_ID,
              ChatMessageRole.ASSISTANT,
              "hai",
              1,
              2L,
              Instant.parse("2026-04-26T01:07:02Z"));

      when(chatSessionRepository.findByIdAndNotDeleted(SESSION_ID)).thenReturn(Optional.of(activeSession));
      when(chatMessageRepository.findMaxSequenceNoBySessionId(SESSION_ID)).thenReturn(0L);
      when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(userMessage, assistantMessage);
      when(valueOperations.get("chat:ctx:" + SESSION_ID)).thenReturn(context);
      when(geminiService.sendMessage(any())).thenReturn("hai");
      when(chatSessionRepository.save(activeSession)).thenReturn(activeSession);

      // ===== ACT =====
      ChatExchangeResponse response =
          chatSessionService.sendMessage(SESSION_ID, SendChatMessageRequest.builder().prompt("mot").build());

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(response),
          () -> assertTrue(response.getMemory().getTrimmed()),
          () -> assertTrue(response.getMemory().getCurrentWords() <= 2));

      // ===== VERIFY =====
      verify(geminiService, times(1)).sendMessage(any());
      verify(valueOperations, times(1)).set(eq("chat:ctx:" + SESSION_ID), any(), eq(ChatSessionServiceImpl.MEMORY_TTL));
    }
  }

  @Nested
  @DisplayName("inner memory dto classes")
  class InnerMemoryDtoClassTests {

    @Test
    void it_should_cover_memory_message_lombok_contract_for_equals_hashcode_builder_and_null_variants() {
      // ===== ARRANGE =====
      Instant createdAt = Instant.parse("2026-04-26T02:00:00Z");
      ChatSessionServiceImpl.MemoryMessage base = new ChatSessionServiceImpl.MemoryMessage();
      base.setRole("USER");
      base.setContent("Xin giai bai toan");
      base.setWordCount(4);
      base.setCreatedAt(createdAt);

      ChatSessionServiceImpl.MemoryMessage same =
          ChatSessionServiceImpl.MemoryMessage.builder()
              .role("USER")
              .content("Xin giai bai toan")
              .wordCount(4)
              .createdAt(createdAt)
              .build();
      ChatSessionServiceImpl.MemoryMessage diffRole =
          ChatSessionServiceImpl.MemoryMessage.builder()
              .role("ASSISTANT")
              .content("Xin giai bai toan")
              .wordCount(4)
              .createdAt(createdAt)
              .build();
      ChatSessionServiceImpl.MemoryMessage diffContent =
          ChatSessionServiceImpl.MemoryMessage.builder()
              .role("USER")
              .content("Noi dung khac")
              .wordCount(4)
              .createdAt(createdAt)
              .build();
      ChatSessionServiceImpl.MemoryMessage diffWordCount =
          ChatSessionServiceImpl.MemoryMessage.builder()
              .role("USER")
              .content("Xin giai bai toan")
              .wordCount(5)
              .createdAt(createdAt)
              .build();
      ChatSessionServiceImpl.MemoryMessage diffCreatedAt =
          ChatSessionServiceImpl.MemoryMessage.builder()
              .role("USER")
              .content("Xin giai bai toan")
              .wordCount(4)
              .createdAt(Instant.parse("2026-04-26T02:00:01Z"))
              .build();
      ChatSessionServiceImpl.MemoryMessage nullFields =
          new ChatSessionServiceImpl.MemoryMessage(null, null, null, null);
      ChatSessionServiceImpl.MemoryMessage nullFieldsSame =
          new ChatSessionServiceImpl.MemoryMessage(null, null, null, null);

      // ===== ACT & ASSERT =====
      assertEquals("USER", base.getRole());
      assertEquals("Xin giai bai toan", base.getContent());
      assertEquals(4, base.getWordCount());
      assertEquals(createdAt, base.getCreatedAt());
      assertEquals(base, same);
      assertEquals(nullFields, nullFieldsSame);
      assertNotEquals(base, diffRole);
      assertNotEquals(base, diffContent);
      assertNotEquals(base, diffWordCount);
      assertNotEquals(base, diffCreatedAt);
      assertNotEquals(base, nullFields);
      assertNotEquals(base, null);
      assertNotEquals(base, "not-memory-message");
      assertNotEquals(base.hashCode(), diffRole.hashCode());
      assertTrue(base.toString().contains("MemoryMessage"));

      // ===== VERIFY =====
      verifyNoMoreInteractions(chatSessionRepository, chatMessageRepository, geminiService);
    }

    @Test
    void it_should_cover_chat_memory_context_lombok_contract_for_equals_hashcode_builder_and_defaults() {
      // ===== ARRANGE =====
      ChatSessionServiceImpl.MemoryMessage msg =
          ChatSessionServiceImpl.MemoryMessage.builder()
              .role("USER")
              .content("Hoc toan")
              .wordCount(2)
              .createdAt(Instant.parse("2026-04-26T02:01:00Z"))
              .build();

      ChatSessionServiceImpl.ChatMemoryContext defaults =
          ChatSessionServiceImpl.ChatMemoryContext.builder().build();
      ChatSessionServiceImpl.ChatMemoryContext base =
          ChatSessionServiceImpl.ChatMemoryContext.builder()
              .messages(new ArrayList<>(List.of(msg)))
              .totalWords(2)
              .build();
      ChatSessionServiceImpl.ChatMemoryContext same =
          ChatSessionServiceImpl.ChatMemoryContext.builder()
              .messages(new ArrayList<>(List.of(msg)))
              .totalWords(2)
              .build();
      ChatSessionServiceImpl.ChatMemoryContext diffMessages =
          ChatSessionServiceImpl.ChatMemoryContext.builder()
              .messages(new ArrayList<>())
              .totalWords(2)
              .build();
      ChatSessionServiceImpl.ChatMemoryContext diffTotalWords =
          ChatSessionServiceImpl.ChatMemoryContext.builder()
              .messages(new ArrayList<>(List.of(msg)))
              .totalWords(3)
              .build();
      ChatSessionServiceImpl.ChatMemoryContext nullFields =
          new ChatSessionServiceImpl.ChatMemoryContext(null, null);
      ChatSessionServiceImpl.ChatMemoryContext nullFieldsSame =
          new ChatSessionServiceImpl.ChatMemoryContext(null, null);

      // ===== ACT & ASSERT =====
      assertNotNull(defaults.getMessages());
      assertEquals(0, defaults.getTotalWords());
      assertEquals(base, same);
      assertEquals(nullFields, nullFieldsSame);
      assertNotEquals(base, diffMessages);
      assertNotEquals(base, diffTotalWords);
      assertNotEquals(base, nullFields);
      assertNotEquals(base, null);
      assertNotEquals(base, "not-chat-memory-context");
      assertNotEquals(base.hashCode(), diffMessages.hashCode());
      assertTrue(base.toString().contains("ChatMemoryContext"));

      // ===== VERIFY =====
      verifyNoMoreInteractions(chatSessionRepository, chatMessageRepository, geminiService);
    }

    @Test
    void it_should_cover_memory_message_equals_branches_for_null_mismatch_pairs() {
      // ===== ARRANGE =====
      Instant at = Instant.parse("2026-04-26T02:05:00Z");
      ChatSessionServiceImpl.MemoryMessage nullRole =
          new ChatSessionServiceImpl.MemoryMessage(null, "A", 1, at);
      ChatSessionServiceImpl.MemoryMessage nonNullRole =
          new ChatSessionServiceImpl.MemoryMessage("USER", "A", 1, at);
      ChatSessionServiceImpl.MemoryMessage nullContent =
          new ChatSessionServiceImpl.MemoryMessage("USER", null, 1, at);
      ChatSessionServiceImpl.MemoryMessage nonNullContent =
          new ChatSessionServiceImpl.MemoryMessage("USER", "A", 1, at);
      ChatSessionServiceImpl.MemoryMessage nullWordCount =
          new ChatSessionServiceImpl.MemoryMessage("USER", "A", null, at);
      ChatSessionServiceImpl.MemoryMessage nonNullWordCount =
          new ChatSessionServiceImpl.MemoryMessage("USER", "A", 1, at);
      ChatSessionServiceImpl.MemoryMessage nullCreatedAt =
          new ChatSessionServiceImpl.MemoryMessage("USER", "A", 1, null);
      ChatSessionServiceImpl.MemoryMessage nonNullCreatedAt =
          new ChatSessionServiceImpl.MemoryMessage("USER", "A", 1, at);

      // ===== ACT & ASSERT =====
      assertNotEquals(nullRole, nonNullRole);
      assertNotEquals(nonNullRole, nullRole);
      assertNotEquals(nullContent, nonNullContent);
      assertNotEquals(nonNullContent, nullContent);
      assertNotEquals(nullWordCount, nonNullWordCount);
      assertNotEquals(nonNullWordCount, nullWordCount);
      assertNotEquals(nullCreatedAt, nonNullCreatedAt);
      assertNotEquals(nonNullCreatedAt, nullCreatedAt);

      // ===== VERIFY =====
      verifyNoMoreInteractions(chatSessionRepository, chatMessageRepository, geminiService);
    }

    @Test
    void it_should_cover_chat_memory_context_equals_branches_for_null_mismatch_pairs() {
      // ===== ARRANGE =====
      ChatSessionServiceImpl.ChatMemoryContext nullMessages =
          new ChatSessionServiceImpl.ChatMemoryContext(null, 2);
      ChatSessionServiceImpl.ChatMemoryContext nonNullMessages =
          new ChatSessionServiceImpl.ChatMemoryContext(new ArrayList<>(), 2);
      ChatSessionServiceImpl.ChatMemoryContext nullTotalWords =
          new ChatSessionServiceImpl.ChatMemoryContext(new ArrayList<>(), null);
      ChatSessionServiceImpl.ChatMemoryContext nonNullTotalWords =
          new ChatSessionServiceImpl.ChatMemoryContext(new ArrayList<>(), 2);

      // ===== ACT & ASSERT =====
      assertNotEquals(nullMessages, nonNullMessages);
      assertNotEquals(nonNullMessages, nullMessages);
      assertNotEquals(nullTotalWords, nonNullTotalWords);
      assertNotEquals(nonNullTotalWords, nullTotalWords);

      // ===== VERIFY =====
      verifyNoMoreInteractions(chatSessionRepository, chatMessageRepository, geminiService);
    }
  }
}
