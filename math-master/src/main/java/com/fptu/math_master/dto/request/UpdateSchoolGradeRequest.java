package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSchoolGradeRequest {

  @Min(1)
  @Max(12)
  private Integer gradeLevel;

  @Size(max = 100)
  private String name;

  private String description;

  private Boolean active;
}
