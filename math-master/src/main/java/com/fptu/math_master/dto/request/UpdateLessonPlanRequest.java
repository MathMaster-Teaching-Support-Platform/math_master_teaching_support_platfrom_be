package com.fptu.math_master.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonPlanRequest {

  private String[] objectives;

  private String[] materialsNeeded;

  private String teachingStrategy;

  private String assessmentMethods;

  private String notes;
}
