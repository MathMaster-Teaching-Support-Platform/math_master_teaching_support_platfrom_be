package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartUploadUrlResponse {
  private String presignedUrl;
  private int partNumber;
  private String eTag; // Returned when using backend proxy upload
}
