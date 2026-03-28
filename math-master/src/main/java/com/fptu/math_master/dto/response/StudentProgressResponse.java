package com.fptu.math_master.dto.response;

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
public class StudentProgressResponse {
  private UUID enrollmentId;
  private UUID courseId;
  private String courseTitle;
  private int totalLessons;
  private int completedLessons;
  private double completionRate;
  private List<LessonProgressItem> lessons;
}
