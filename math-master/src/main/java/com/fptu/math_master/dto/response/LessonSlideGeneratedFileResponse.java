package com.fptu.math_master.dto.response;

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
public class LessonSlideGeneratedFileResponse {
  private UUID id;
  private UUID lessonId;
  private UUID templateId;
  private String name;
  private String thumbnail;
  private String fileName;
  private String contentType;
  private Long fileSizeBytes;
  private Boolean isPublic;
  private Instant publishedAt;
  private Instant createdAt;
  private Instant updatedAt;
}
