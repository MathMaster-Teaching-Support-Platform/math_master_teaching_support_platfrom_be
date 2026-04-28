package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to add a bank mapping to an exam matrix.
 * Phase 3: questionBankId removed - bank comes from the matrix level.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBankMappingRequest {

  @NotNull(message = "matrixRowId is required")
  private UUID matrixRowId;

  @NotNull(message = "questionCount is required")
  @Min(value = 0, message = "questionCount must be at least 0")
  private Integer questionCount;

  @NotNull(message = "cognitiveLevel is required")
  private CognitiveLevel cognitiveLevel;

  @DecimalMin(value = "0.01", message = "pointsPerQuestion must be > 0")
  private BigDecimal pointsPerQuestion;
}
