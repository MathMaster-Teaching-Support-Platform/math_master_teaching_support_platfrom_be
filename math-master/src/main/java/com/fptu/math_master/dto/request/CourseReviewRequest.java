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
public class CourseReviewRequest {
  @Min(value = 1, message = "RATING_MIN_1")
  @Max(value = 5, message = "RATING_MAX_5")
  private int rating;
  
  @NotBlank(message = "COMMENT_REQUIRED")
  private String comment;
}
