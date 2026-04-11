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
public class SendChatMessageRequest {

  @NotBlank(message = "Prompt is required")
  @Size(max = 20000, message = "Prompt is too long")
  private String prompt;

  private Double temperature;

  private Integer maxOutputTokens;
}
