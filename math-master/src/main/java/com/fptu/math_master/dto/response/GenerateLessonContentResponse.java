package com.fptu.math_master.dto.response;

import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateLessonContentResponse {
  private String gradeLevel;
  private String subject;
  private int totalLessonsCreated;
  private int totalChaptersCreated;
  private int skippedLessons;
  private List<LessonResponse> lessons;
}
