package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CurriculumCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCurriculumRequest {
  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  @Min(value = 1, message = "Grade must be between 1 and 12")
  @Max(value = 12, message = "Grade must be between 1 and 12")
  private Integer grade;

  private CurriculumCategory category;

  @Size(max = 2000, message = "Description must not exceed 2000 characters")
  private String description;
}
