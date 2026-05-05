package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

  private UUID id;
  private UUID recipientId;
  private String type;
  private String title;
  private String content;
  private Map<String, Object> metadata;
  private Boolean read;
  private String actionUrl;
  private Instant createdAt;
  private Instant updatedAt;
}
