package com.fptu.math_master.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackAttachmentResponse {
  private String id;
  private String fileName;
  private String contentType;
  private Long fileSize;
  private String fileUrl;
}
