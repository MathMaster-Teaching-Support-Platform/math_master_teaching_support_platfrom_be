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
public class LessonPlanResponse {
  private UUID id;
  private UUID lessonId;
  private UUID teacherId;
  private String lessonTitle;
  private String teacherName;
  private String[] objectives;
  private String[] materialsNeeded;
  private String teachingStrategy;
  private String assessmentMethods;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
}
