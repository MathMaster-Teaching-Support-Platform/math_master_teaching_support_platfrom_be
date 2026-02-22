package com.fptu.math_master.dto.request;

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
}
