package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
  private UUID id;
  private UUID teacherId;
  private String title;
  private String description;
  private String subject;
  private String gradeLevel;
  private Integer durationMinutes;
  private LessonDifficulty difficulty;
  private LessonStatus status;
  private List<ChapterResponse> chapters;
}

