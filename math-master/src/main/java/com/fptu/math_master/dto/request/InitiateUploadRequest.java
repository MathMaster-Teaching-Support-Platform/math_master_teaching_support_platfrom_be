package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateUploadRequest {

  @NotBlank(message = "fileName is required")
  private String fileName;

  @NotBlank(message = "contentType is required")
  private String contentType;

  @NotNull(message = "fileSize is required")
  @Min(value = 1, message = "fileSize must be positive")
  private Long fileSize;
}
