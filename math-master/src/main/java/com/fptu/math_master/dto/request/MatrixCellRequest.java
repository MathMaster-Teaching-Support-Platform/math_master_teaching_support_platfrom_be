package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;

/**
 * One cell in an exam-matrix row — specifies how many questions of a given
 * cognitive level to draw from the parent {@link MatrixRowRequest}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixCellRequest {

  @NotNull(message = "cognitiveLevel is required")
  private CognitiveLevel cognitiveLevel;

  @NotNull(message = "questionCount is required")
  @Min(value = 1, message = "questionCount must be at least 1")
  private Integer questionCount;

  @NotNull(message = "pointsPerQuestion is required")
  @DecimalMin(value = "0.01", message = "pointsPerQuestion must be > 0")
  private BigDecimal pointsPerQuestion;
}
