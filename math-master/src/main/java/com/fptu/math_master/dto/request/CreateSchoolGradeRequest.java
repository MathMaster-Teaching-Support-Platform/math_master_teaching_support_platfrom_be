package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchoolGradeRequest {

  @NotNull
  @Min(1)
  @Max(12)
  private Integer gradeLevel;

  @NotBlank
  @Size(max = 100)
  private String name;

  private String description;
}
