package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
  private UUID id;
  private UUID chapterId;
  private String title;
  private String learningObjectives;
  private String lessonContent;
  private String summary;
  private Integer orderIndex;
  private Integer durationMinutes;
  private LessonDifficulty difficulty;
  private LessonStatus status;
  /** true nếu lesson đã bị soft-delete (FE hiển thị màu khác). */
  @Builder.Default
  private boolean deleted = false;
  private Instant createdAt;
  private Instant updatedAt;
}
