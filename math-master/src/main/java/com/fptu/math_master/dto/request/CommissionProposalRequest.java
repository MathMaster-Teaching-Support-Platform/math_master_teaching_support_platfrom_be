package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload sent by a teacher to submit a new commission-split proposal.
 */
@Data
public class CommissionProposalRequest {

  /**
   * The share (0.00–1.00) the teacher wishes to keep.
   * Allowed range: 0.50 – 0.97  (platform always gets at least 3 %).
   */
  @NotNull(message = "Teacher share is required")
  @DecimalMin(value = "0.50", message = "Teacher share must be at least 50%")
  @DecimalMax(value = "0.97", message = "Teacher share cannot exceed 97%")
  private BigDecimal teacherShare;
}
