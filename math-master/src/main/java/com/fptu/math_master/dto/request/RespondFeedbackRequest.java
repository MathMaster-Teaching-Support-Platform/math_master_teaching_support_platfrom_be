package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RespondFeedbackRequest {

  @NotBlank(message = "Response message is required")
  @Size(max = 5000, message = "Response message cannot exceed 5000 characters")
  private String responseMessage;
}
