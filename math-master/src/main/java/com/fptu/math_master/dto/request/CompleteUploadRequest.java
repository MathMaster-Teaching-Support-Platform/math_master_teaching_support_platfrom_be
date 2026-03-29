package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteUploadRequest {

  @NotBlank(message = "uploadId is required")
  private String uploadId;

  @NotBlank(message = "objectKey is required")
  private String objectKey;

  @NotEmpty(message = "parts must not be empty")
  private List<PartInfo> parts;

  // CourseLesson metadata
  @NotNull(message = "lessonId is required")
  private UUID lessonId;

  private String videoTitle;
  private Integer orderIndex;
  private boolean isFreePreview;
  private Integer durationSeconds;
  private String materials;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PartInfo {
    private int partNumber;
    
    @com.fasterxml.jackson.annotation.JsonProperty(value = "eTag", access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_WRITE)
    @com.fasterxml.jackson.annotation.JsonAlias({"eTag", "etag", "ETag"})
    private String eTag;
  }
}