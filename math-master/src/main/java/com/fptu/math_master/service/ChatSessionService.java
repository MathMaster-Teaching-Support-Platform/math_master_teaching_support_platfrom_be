package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateChatSessionRequest;
import com.fptu.math_master.dto.request.SendChatMessageRequest;
import com.fptu.math_master.dto.request.UpdateChatSessionRequest;
import com.fptu.math_master.dto.response.ChatExchangeResponse;
import com.fptu.math_master.dto.response.ChatMemoryInfoResponse;
import com.fptu.math_master.dto.response.ChatMessageResponse;
import com.fptu.math_master.dto.response.ChatSessionResponse;
import com.fptu.math_master.enums.ChatSessionStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChatSessionService {

  ChatSessionResponse createSession(CreateChatSessionRequest request);

  Page<ChatSessionResponse> getMySessions(ChatSessionStatus status, String keyword, Pageable pageable);

  ChatSessionResponse getSession(UUID sessionId);

  Page<ChatMessageResponse> getSessionMessages(UUID sessionId, Pageable pageable);

  ChatExchangeResponse sendMessage(UUID sessionId, SendChatMessageRequest request);

  ChatSessionResponse updateSession(UUID sessionId, UpdateChatSessionRequest request);

  ChatSessionResponse archiveSession(UUID sessionId);

  void deleteSession(UUID sessionId);

  ChatMemoryInfoResponse getMemoryInfo(UUID sessionId);
}
