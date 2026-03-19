package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateMindmapRequest {

  @NotBlank(message = "Prompt is required")
  private String prompt;

  private String lessonId;

  private String title;

  @Min(value = 2, message = "Levels must be greater than 1")
  @Max(value = 6, message = "Levels must be less than 7")
  private Integer levels = 3; // Default 3 levels
}
