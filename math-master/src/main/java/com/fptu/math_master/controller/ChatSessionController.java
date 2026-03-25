package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateChatSessionRequest;
import com.fptu.math_master.dto.request.SendChatMessageRequest;
import com.fptu.math_master.dto.request.UpdateChatSessionRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.ChatExchangeResponse;
import com.fptu.math_master.dto.response.ChatMemoryInfoResponse;
import com.fptu.math_master.dto.response.ChatMessageResponse;
import com.fptu.math_master.dto.response.ChatSessionResponse;
import com.fptu.math_master.enums.ChatSessionStatus;
import com.fptu.math_master.service.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-sessions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat Sessions", description = "Chat session and message history endpoints")
public class ChatSessionController {

  ChatSessionService chatSessionService;

  @PostMapping
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Create chat session", description = "Create a new conversation session")
  public ApiResponse<ChatSessionResponse> createSession(
      @Valid @RequestBody CreateChatSessionRequest request) {
    return ApiResponse.<ChatSessionResponse>builder()
        .message("Session created")
        .result(chatSessionService.createSession(request))
        .build();
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Get my sessions", description = "Get current user chat sessions")
  public ApiResponse<Page<ChatSessionResponse>> getMySessions(
      @RequestParam(required = false) ChatSessionStatus status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "lastMessageAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String direction) {

    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    return ApiResponse.<Page<ChatSessionResponse>>builder()
        .result(chatSessionService.getMySessions(status, keyword, pageable))
        .build();
  }

  @GetMapping("/{sessionId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Get session detail", description = "Get metadata for one chat session")
  public ApiResponse<ChatSessionResponse> getSession(@PathVariable UUID sessionId) {
    return ApiResponse.<ChatSessionResponse>builder()
        .result(chatSessionService.getSession(sessionId))
        .build();
  }

  @GetMapping("/{sessionId}/messages")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Get session messages", description = "Get paged message history by session")
  public ApiResponse<Page<ChatMessageResponse>> getSessionMessages(
      @PathVariable UUID sessionId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "ASC") String direction) {

    Sort.Direction sortDirection =
        direction.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    return ApiResponse.<Page<ChatMessageResponse>>builder()
        .result(chatSessionService.getSessionMessages(sessionId, pageable))
        .build();
  }

  @PostMapping("/{sessionId}/messages")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(
      summary = "Send chat message",
      description =
          "Send user prompt, call AI, save USER/ASSISTANT messages, and maintain rolling 1000-word memory")
  public ApiResponse<ChatExchangeResponse> sendMessage(
      @PathVariable UUID sessionId, @Valid @RequestBody SendChatMessageRequest request) {
    return ApiResponse.<ChatExchangeResponse>builder()
        .message("Message processed")
        .result(chatSessionService.sendMessage(sessionId, request))
        .build();
  }

  @PatchMapping("/{sessionId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Rename session", description = "Update chat session title")
  public ApiResponse<ChatSessionResponse> updateSession(
      @PathVariable UUID sessionId, @Valid @RequestBody UpdateChatSessionRequest request) {
    return ApiResponse.<ChatSessionResponse>builder()
        .message("Session updated")
        .result(chatSessionService.updateSession(sessionId, request))
        .build();
  }

  @PatchMapping("/{sessionId}/archive")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Archive session", description = "Archive a session and block new messages")
  public ApiResponse<ChatSessionResponse> archiveSession(@PathVariable UUID sessionId) {
    return ApiResponse.<ChatSessionResponse>builder()
        .message("Session archived")
        .result(chatSessionService.archiveSession(sessionId))
        .build();
  }

  @DeleteMapping("/{sessionId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Delete session", description = "Soft delete a chat session and messages")
  public ApiResponse<Void> deleteSession(@PathVariable UUID sessionId) {
    chatSessionService.deleteSession(sessionId);
    return ApiResponse.<Void>builder().message("Session deleted").build();
  }

  @GetMapping("/{sessionId}/memory")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Get memory info", description = "Get Redis rolling memory status for session")
  public ApiResponse<ChatMemoryInfoResponse> getMemoryInfo(@PathVariable UUID sessionId) {
    return ApiResponse.<ChatMemoryInfoResponse>builder()
        .result(chatSessionService.getMemoryInfo(sessionId))
        .build();
  }
}
