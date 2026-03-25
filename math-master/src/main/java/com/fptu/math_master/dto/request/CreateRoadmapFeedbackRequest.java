package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class CreateRoadmapFeedbackRequest {

  @NotNull(message = "INVALID_KEY")
  @Min(value = 1, message = "INVALID_KEY")
  @Max(value = 5, message = "INVALID_KEY")
  private Integer rating;

  @Size(max = 2000, message = "INVALID_KEY")
  private String content;
}
