package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Patch a Book — all fields optional; null = leave unchanged. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBookRequest {

  @Size(max = 255)
  private String title;

  @Size(max = 255)
  private String publisher;

  @Size(max = 50)
  private String academicYear;

  @Min(1)
  private Integer totalPages;

  @Min(1)
  private Integer ocrPageFrom;

  @Min(1)
  private Integer ocrPageTo;
}
