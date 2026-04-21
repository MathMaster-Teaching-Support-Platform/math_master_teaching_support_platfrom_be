package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectCourseRequest {

  @NotBlank(message = "Rejection reason is required")
  @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
  private String reason;
}
