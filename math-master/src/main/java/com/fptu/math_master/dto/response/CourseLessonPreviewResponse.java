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
public class CourseLessonPreviewResponse {
  private UUID id;
  private UUID courseId;
  private UUID sectionId;
  private String lessonTitle;
  private String customDescription;
  private String videoTitle;
  private Integer durationSeconds;
  private Integer orderIndex;
  private boolean isFreePreview;
  private Instant createdAt;
  private Instant updatedAt;
}
