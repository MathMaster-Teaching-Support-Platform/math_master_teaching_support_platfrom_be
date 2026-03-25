package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatSessionRequest {

  @Size(max = 200, message = "Title must not exceed 200 characters")
  private String title;

  @Size(max = 100, message = "Model must not exceed 100 characters")
  private String model;
}
