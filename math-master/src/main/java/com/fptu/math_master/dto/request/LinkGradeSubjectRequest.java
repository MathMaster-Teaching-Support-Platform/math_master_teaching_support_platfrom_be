package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkGradeSubjectRequest {

  @NotNull(message = "gradeLevel is required")
  @Min(value = 1, message = "gradeLevel must be between 1 and 12")
  @Max(value = 12, message = "gradeLevel must be between 1 and 12")
  private Integer gradeLevel;
}
