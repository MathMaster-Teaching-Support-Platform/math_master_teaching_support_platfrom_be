package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CourseLessonPreviewResponse {
  private UUID id;
  private UUID courseId;
  private UUID sectionId;
  private String lessonTitle;
  private String customTitle;
  private String customDescription;
  private String videoTitle;
  private String videoUrl;
  private Integer durationSeconds;
  private Integer orderIndex;
  @JsonProperty("isFreePreview")
  private boolean isFreePreview;
  private Instant createdAt;
  private Instant updatedAt;
}
