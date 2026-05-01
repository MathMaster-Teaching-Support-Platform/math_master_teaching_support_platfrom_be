package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixRequest {

  @NotBlank(message = "Name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  private String description;

  private Boolean isReusable;

  @Positive(message = "Total questions target must be greater than 0")
  private Integer totalQuestionsTarget;

  @DecimalMin(value = "0.01", message = "Total points target must be greater than 0")
  private BigDecimal totalPointsTarget;

  /**
   * Optional: The single question bank used as the source for all questions in this matrix.
   * Can be assigned later after matrix creation.
   */
  private java.util.UUID questionBankId;

  /**
   * Number of parts in the exam (1-3).
   * DEPRECATED: Use parts[] instead for configurable part types.
   * If parts[] is provided, numberOfParts is derived from parts.length.
   * If parts[] is null, this creates default parts (Part1=MCQ, Part2=TF, Part3=SA).
   * Default: 1 (MCQ only)
   */
  @jakarta.validation.constraints.Min(value = 1, message = "numberOfParts must be 1, 2, or 3")
  @jakarta.validation.constraints.Max(value = 3, message = "numberOfParts must be 1, 2, or 3")
  @Builder.Default
  private Integer numberOfParts = 1;

  /**
   * Configurable parts for the exam matrix.
   * If provided, this takes precedence over numberOfParts.
   * Each part defines its question type (MCQ, TRUE_FALSE, or SHORT_ANSWER).
   * Min: 1 part, Max: 3 parts.
   */
  @Valid
  private List<ExamMatrixPartRequest> parts;

}
