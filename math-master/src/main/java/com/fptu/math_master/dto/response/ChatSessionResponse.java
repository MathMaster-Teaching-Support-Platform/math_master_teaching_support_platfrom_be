package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.ChatSessionStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionResponse {
  private UUID id;
  private UUID userId;
  private String title;
  private ChatSessionStatus status;
  private String model;
  private Integer totalMessages;
  private Integer totalWords;
  private Instant lastMessageAt;
  private Instant createdAt;
  private Instant updatedAt;
}
