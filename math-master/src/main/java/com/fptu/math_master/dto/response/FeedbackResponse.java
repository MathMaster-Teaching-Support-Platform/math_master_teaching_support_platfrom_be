package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.FeedbackStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackResponse {
  private UUID id;
  private String title;
  private String description;
  private String category;
  private String relatedUrl;
  private FeedbackStatus status;
  private String senderId;
  private String senderName;
  private String senderEmail;
  private String senderRole;
  private String responseMessage;
  private String respondedById;
  private String respondedByName;
  private List<FeedbackAttachmentResponse> attachments;
  private boolean senderRead;
  private boolean adminRead;
  private boolean readByCurrentUser;
  private Instant createdAt;
  private Instant updatedAt;
}
