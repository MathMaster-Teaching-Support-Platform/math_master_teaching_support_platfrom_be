package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.ChatMessageRole;
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
public class ChatMessageResponse {
  private UUID id;
  private UUID sessionId;
  private UUID userId;
  private ChatMessageRole role;
  private String content;
  private Integer wordCount;
  private String model;
  private Integer latencyMs;
  private Long sequenceNo;
  private Instant createdAt;
}
