package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonPlanRequest {

  @NotNull(message = "lessonId is required")
  private UUID lessonId;

  private String[] objectives;

  private String[] materialsNeeded;

  private String teachingStrategy;

  private String assessmentMethods;

  private String notes;
}
