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
public class LessonProgressItem {
  private UUID courseLessonId;
  private String videoTitle;
  private Integer orderIndex;
  private boolean isCompleted;
  private Instant completedAt;
}
