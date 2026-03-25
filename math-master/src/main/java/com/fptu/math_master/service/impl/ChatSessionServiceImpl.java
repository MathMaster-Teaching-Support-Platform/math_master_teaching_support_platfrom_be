package com.fptu.math_master.service.impl;

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
import com.fptu.math_master.service.ChatSessionService;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatSessionServiceImpl implements ChatSessionService {

  static int WORD_LIMIT = 1000;
  static Duration MEMORY_TTL = Duration.ofDays(7);
  static int REBUILD_FETCH_LIMIT = 200;

  ChatSessionRepository chatSessionRepository;
  ChatMessageRepository chatMessageRepository;
  GeminiService geminiService;
  RedisTemplate<String, Object> redisTemplate;

  @Override
  @Transactional
  public ChatSessionResponse createSession(CreateChatSessionRequest request) {
    UUID userId = SecurityUtils.getCurrentUserId();

    String title = normalizeTitle(request.getTitle());
    String model = normalizeModel(request.getModel());

    ChatSession session =
        ChatSession.builder()
            .userId(userId)
            .title(title)
            .model(model)
            .status(ChatSessionStatus.ACTIVE)
            .totalMessages(0)
            .totalWords(0)
            .lastMessageAt(Instant.now())
            .build();

    session = chatSessionRepository.save(session);
    return toSessionResponse(session);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ChatSessionResponse> getMySessions(
      ChatSessionStatus status, String keyword, Pageable pageable) {
    UUID userId = SecurityUtils.getCurrentUserId();
    String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();

    return chatSessionRepository
        .findByUserAndFilters(userId, status, normalizedKeyword, pageable)
        .map(this::toSessionResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ChatSessionResponse getSession(UUID sessionId) {
    return toSessionResponse(getOwnedSession(sessionId));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ChatMessageResponse> getSessionMessages(UUID sessionId, Pageable pageable) {
    ChatSession session = getOwnedSession(sessionId);
    return chatMessageRepository
        .findBySessionIdAndDeletedAtIsNull(session.getId(), pageable)
        .map(this::toMessageResponse);
  }

  @Override
  @Transactional
  public ChatExchangeResponse sendMessage(UUID sessionId, SendChatMessageRequest request) {
    ChatSession session = getOwnedSession(sessionId);

    if (session.getStatus() == ChatSessionStatus.ARCHIVED) {
      throw new AppException(ErrorCode.CHAT_SESSION_ARCHIVED);
    }

    String prompt = request.getPrompt() == null ? null : request.getPrompt().trim();
    if (prompt == null || prompt.isEmpty()) {
      throw new AppException(ErrorCode.CHAT_PROMPT_EMPTY);
    }

    long nextSeq = chatMessageRepository.findMaxSequenceNoBySessionId(sessionId) + 1;
    ChatMessage userMessage =
        chatMessageRepository.save(
            ChatMessage.builder()
                .sessionId(sessionId)
                .userId(session.getUserId())
                .role(ChatMessageRole.USER)
                .content(prompt)
                .wordCount(countWords(prompt))
                .sequenceNo(nextSeq)
                .build());

    ChatMemoryContext context = loadOrRebuildContext(sessionId);
    boolean trimmedAfterUser = appendAndTrim(context, ChatMessageRole.USER, prompt, userMessage.getCreatedAt());

    String modelName = normalizeModel(session.getModel());
    String aiInput = buildPromptFromContext(context, prompt);

    Instant aiStart = Instant.now();
    String aiReply;
    try {
      aiReply = geminiService.sendMessage(aiInput);
    } catch (Exception e) {
      log.error("AI chat call failed for session {}", sessionId, e);
      throw new AppException(ErrorCode.CHAT_AI_CALL_FAILED);
    }
    int latencyMs = (int) Duration.between(aiStart, Instant.now()).toMillis();

    ChatMessage assistantMessage =
        chatMessageRepository.save(
            ChatMessage.builder()
                .sessionId(sessionId)
                .userId(session.getUserId())
                .role(ChatMessageRole.ASSISTANT)
                .content(aiReply)
                .wordCount(countWords(aiReply))
                .model(modelName)
                .latencyMs(latencyMs)
                .sequenceNo(nextSeq + 1)
                .build());

    boolean trimmedAfterAssistant =
        appendAndTrim(context, ChatMessageRole.ASSISTANT, aiReply, assistantMessage.getCreatedAt());
    persistContext(sessionId, context);

    if ((session.getTitle() == null || session.getTitle().equals("New Chat"))
        && session.getTotalMessages() != null
        && session.getTotalMessages() == 0) {
      session.setTitle(generateTitleFromPrompt(prompt));
    }
    session.setLastMessageAt(assistantMessage.getCreatedAt());
    session.setTotalMessages((session.getTotalMessages() == null ? 0 : session.getTotalMessages()) + 2);
    session.setTotalWords((session.getTotalWords() == null ? 0 : session.getTotalWords()) + countWords(prompt) + countWords(aiReply));
    if (session.getModel() == null || session.getModel().isBlank()) {
      session.setModel(modelName);
    }
    chatSessionRepository.save(session);

    return ChatExchangeResponse.builder()
        .sessionId(sessionId)
        .userMessage(toMessageResponse(userMessage))
        .assistantMessage(toMessageResponse(assistantMessage))
        .memory(
            ChatMemoryInfoResponse.builder()
                .wordLimit(WORD_LIMIT)
                .currentWords(context.getTotalWords())
                .messageCount(context.getMessages().size())
                .trimmed(trimmedAfterUser || trimmedAfterAssistant)
                .build())
        .build();
  }

  @Override
  @Transactional
  public ChatSessionResponse updateSession(UUID sessionId, UpdateChatSessionRequest request) {
    ChatSession session = getOwnedSession(sessionId);
    session.setTitle(normalizeTitle(request.getTitle()));
    return toSessionResponse(chatSessionRepository.save(session));
  }

  @Override
  @Transactional
  public ChatSessionResponse archiveSession(UUID sessionId) {
    ChatSession session = getOwnedSession(sessionId);
    session.setStatus(ChatSessionStatus.ARCHIVED);
    return toSessionResponse(chatSessionRepository.save(session));
  }

  @Override
  @Transactional
  public void deleteSession(UUID sessionId) {
    ChatSession session = getOwnedSession(sessionId);
    Instant now = Instant.now();
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    session.setDeletedAt(now);
    session.setDeletedBy(currentUserId);
    chatSessionRepository.save(session);

    List<ChatMessage> messages = chatMessageRepository.findAllBySessionIdAndDeletedAtIsNull(sessionId);
    for (ChatMessage message : messages) {
      message.setDeletedAt(now);
      message.setDeletedBy(currentUserId);
    }
    if (!messages.isEmpty()) {
      chatMessageRepository.saveAll(messages);
    }

    redisTemplate.delete(buildMemoryKey(sessionId));
  }

  @Override
  @Transactional(readOnly = true)
  public ChatMemoryInfoResponse getMemoryInfo(UUID sessionId) {
    getOwnedSession(sessionId);
    ChatMemoryContext context = loadOrRebuildContext(sessionId);
    return ChatMemoryInfoResponse.builder()
        .wordLimit(WORD_LIMIT)
        .currentWords(context.getTotalWords())
        .messageCount(context.getMessages().size())
        .trimmed(false)
        .build();
  }

  private ChatSession getOwnedSession(UUID sessionId) {
    UUID userId = SecurityUtils.getCurrentUserId();
    ChatSession session =
        chatSessionRepository
            .findByIdAndNotDeleted(sessionId)
            .orElseThrow(() -> new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND));

    if (!session.getUserId().equals(userId)) {
      throw new AppException(ErrorCode.CHAT_SESSION_ACCESS_DENIED);
    }
    return session;
  }

  private String normalizeTitle(String title) {
    if (title == null || title.isBlank()) return "New Chat";
    return title.trim();
  }

  private String normalizeModel(String model) {
    if (model == null || model.isBlank()) {
      return "gemini-2.5-flash";
    }
    return model.trim();
  }

  private ChatSessionResponse toSessionResponse(ChatSession session) {
    return ChatSessionResponse.builder()
        .id(session.getId())
        .userId(session.getUserId())
        .title(session.getTitle())
        .status(session.getStatus())
        .model(session.getModel())
        .totalMessages(session.getTotalMessages())
        .totalWords(session.getTotalWords())
        .lastMessageAt(session.getLastMessageAt())
        .createdAt(session.getCreatedAt())
        .updatedAt(session.getUpdatedAt())
        .build();
  }

  private ChatMessageResponse toMessageResponse(ChatMessage message) {
    return ChatMessageResponse.builder()
        .id(message.getId())
        .sessionId(message.getSessionId())
        .userId(message.getUserId())
        .role(message.getRole())
        .content(message.getContent())
        .wordCount(message.getWordCount())
        .model(message.getModel())
        .latencyMs(message.getLatencyMs())
        .sequenceNo(message.getSequenceNo())
        .createdAt(message.getCreatedAt())
        .build();
  }

  private int countWords(String text) {
    if (text == null || text.trim().isEmpty()) return 0;
    return text.trim().split("\\s+").length;
  }

  private String generateTitleFromPrompt(String prompt) {
    if (prompt == null || prompt.isBlank()) return "New Chat";
    String[] words = prompt.trim().split("\\s+");
    int take = Math.min(8, words.length);
    String title = String.join(" ", java.util.Arrays.copyOfRange(words, 0, take));
    return title.length() > 200 ? title.substring(0, 200) : title;
  }

  private String buildMemoryKey(UUID sessionId) {
    return "chat:ctx:" + sessionId;
  }

  private ChatMemoryContext loadOrRebuildContext(UUID sessionId) {
    Object raw = redisTemplate.opsForValue().get(buildMemoryKey(sessionId));
    if (raw instanceof ChatMemoryContext context) {
      return context;
    }

    Pageable pageable =
        PageRequest.of(0, REBUILD_FETCH_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<ChatMessage> latest = chatMessageRepository.findBySessionIdAndDeletedAtIsNull(sessionId, pageable);

    List<MemoryMessage> selected = new ArrayList<>();
    int words = 0;
    for (ChatMessage message : latest.getContent()) {
      int wc = message.getWordCount() == null ? countWords(message.getContent()) : message.getWordCount();
      if (words + wc > WORD_LIMIT) break;
      selected.add(
          MemoryMessage.builder()
              .role(message.getRole().name())
              .content(message.getContent())
              .wordCount(wc)
              .createdAt(message.getCreatedAt())
              .build());
      words += wc;
    }
    Collections.reverse(selected);

    ChatMemoryContext context = ChatMemoryContext.builder().messages(selected).totalWords(words).build();
    persistContext(sessionId, context);
    return context;
  }

  private void persistContext(UUID sessionId, ChatMemoryContext context) {
    redisTemplate.opsForValue().set(buildMemoryKey(sessionId), context, MEMORY_TTL);
  }

  private boolean appendAndTrim(
      ChatMemoryContext context, ChatMessageRole role, String content, Instant createdAt) {
    if (context.getMessages() == null) {
      context.setMessages(new ArrayList<>());
    }

    int wc = countWords(content);
    context
        .getMessages()
        .add(
            MemoryMessage.builder()
                .role(role.name())
                .content(content)
                .wordCount(wc)
                .createdAt(createdAt)
                .build());
    context.setTotalWords(context.getTotalWords() + wc);

    boolean trimmed = false;
    while (context.getTotalWords() > WORD_LIMIT && !context.getMessages().isEmpty()) {
      MemoryMessage removed = context.getMessages().remove(0);
      int removedWords = removed.getWordCount() == null ? 0 : removed.getWordCount();
      context.setTotalWords(Math.max(0, context.getTotalWords() - removedWords));
      trimmed = true;
    }
    return trimmed;
  }

  private String buildPromptFromContext(ChatMemoryContext context, String currentPrompt) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are a helpful education assistant. Use prior conversation context when relevant.\n\n");
    sb.append("Conversation so far:\n");

    for (MemoryMessage message : context.getMessages()) {
      String role = message.getRole() == null ? "USER" : message.getRole().toUpperCase(Locale.ROOT);
      sb.append(role).append(": ").append(message.getContent()).append("\n");
    }

    sb.append("\nCurrent USER message:\n");
    sb.append(currentPrompt);
    return sb.toString();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChatMemoryContext {
    @Builder.Default private List<MemoryMessage> messages = new ArrayList<>();
    @Builder.Default private Integer totalWords = 0;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MemoryMessage {
    private String role;
    private String content;
    private Integer wordCount;
    private Instant createdAt;
  }
}
