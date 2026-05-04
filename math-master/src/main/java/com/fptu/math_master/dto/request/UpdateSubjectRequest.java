package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubjectRequest {

  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  private String description;

  @Min(value = 1, message = "gradeMin must be between 1 and 12")
  @Max(value = 12, message = "gradeMin must be between 1 and 12")
  private Integer gradeMin;

  @Min(value = 1, message = "gradeMax must be between 1 and 12")
  @Max(value = 12, message = "gradeMax must be between 1 and 12")
  private Integer gradeMax;

  private Boolean isActive;

  private UUID schoolGradeId;
}