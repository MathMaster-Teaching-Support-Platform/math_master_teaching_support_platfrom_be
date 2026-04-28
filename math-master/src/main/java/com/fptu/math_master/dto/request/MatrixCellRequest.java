package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One cell in an exam-matrix row — specifies how many questions of a given
 * cognitive level to draw from the parent {@link MatrixRowRequest}.
 *
 * The partNumber (1, 2, or 3) determines the question type:
 * - Part I (1) → MULTIPLE_CHOICE
 * - Part II (2) → TRUE_FALSE
 * - Part III (3) → SHORT_ANSWER
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixCellRequest {

  @NotNull(message = "partNumber is required")
  @Min(value = 1, message = "partNumber must be 1, 2, or 3")
  @Max(value = 3, message = "partNumber must be 1, 2, or 3")
  private Integer partNumber;

  @NotNull(message = "cognitiveLevel is required")
  private CognitiveLevel cognitiveLevel;

  @NotNull(message = "questionCount is required")
  @Min(value = 0, message = "questionCount must be at least 0")
  private Integer questionCount;

  @DecimalMin(value = "0.01", message = "pointsPerQuestion must be > 0")
  private BigDecimal pointsPerQuestion;
}
