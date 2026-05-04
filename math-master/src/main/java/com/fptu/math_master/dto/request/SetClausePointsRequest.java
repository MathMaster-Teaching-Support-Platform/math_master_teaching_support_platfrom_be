package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for Feature 4: Teacher sets overdrive_point per clause (TF only).
 *
 * Validation rule: sum(clause_points) must equal total_point.
 *
 * Example:
 *   total_point   = 1.0
 *   clause_points = { "A": 0.25, "B": 0.25, "C": 0.25, "D": 0.25 }
 */
@Data
@Builder
public class SetClausePointsRequest {

  /** Total point value for the whole TRUE_FALSE question, e.g. 1.0. */
  @NotNull
  private BigDecimal totalPoint;

  /**
   * Points per clause. Keys must be A, B, C, D.
   * Values must sum exactly to totalPoint.
   */
  @NotNull
  private Map<String, BigDecimal> clausePoints;
}
