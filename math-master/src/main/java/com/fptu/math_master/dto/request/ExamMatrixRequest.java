package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
   * Part I = MCQ, Part II = TRUE_FALSE, Part III = SHORT_ANSWER
   * Used for Vietnamese THPT exam format.
   * Default: 1 (MCQ only)
   */
  @jakarta.validation.constraints.Min(value = 1, message = "numberOfParts must be 1, 2, or 3")
  @jakarta.validation.constraints.Max(value = 3, message = "numberOfParts must be 1, 2, or 3")
  private Integer numberOfParts;

}
