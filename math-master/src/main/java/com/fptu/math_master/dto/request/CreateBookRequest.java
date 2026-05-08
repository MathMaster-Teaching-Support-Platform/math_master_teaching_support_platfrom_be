package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Create a Book (textbook PDF metadata). PDF file is uploaded separately. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {

  @NotNull(message = "schoolGradeId is required")
  private UUID schoolGradeId;

  @NotNull(message = "subjectId is required")
  private UUID subjectId;

  /** Optional — admins may attach a curriculum later; tree navigation is by subject. */
  private UUID curriculumId;

  @NotBlank(message = "title is required")
  @Size(max = 255)
  private String title;

  @Size(max = 255)
  private String publisher;

  @Size(max = 50)
  private String academicYear;

  @Min(value = 1, message = "totalPages must be >= 1")
  private Integer totalPages;

  /** First page (1-based) to OCR; user typically skips cover/TOC. */
  @Min(value = 1, message = "ocrPageFrom must be >= 1")
  private Integer ocrPageFrom;

  /** Last page (inclusive) to OCR. Must be >= ocrPageFrom. */
  @Min(value = 1, message = "ocrPageTo must be >= 1")
  private Integer ocrPageTo;
}
