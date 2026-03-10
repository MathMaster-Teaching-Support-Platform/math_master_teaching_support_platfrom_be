package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonRequest {
  private String title;

  private String learningObjectives;

  private String lessonContent;

  private String summary;

  private Integer orderIndex;

  private Integer durationMinutes;

  private LessonDifficulty difficulty;

  private LessonStatus status;
}
