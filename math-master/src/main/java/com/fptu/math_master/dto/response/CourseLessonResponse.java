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
public class CourseLessonResponse {
  private UUID id;
  private UUID courseId;
  private UUID lessonId;
  private String lessonTitle;
  private String videoUrl;
  private String videoTitle;
  private Integer durationSeconds;
  private Integer orderIndex;
  private boolean isFreePreview;
  /** Raw JSON string of List<MaterialItem> */
  private String materials;
  private Instant createdAt;
  private Instant updatedAt;
}
