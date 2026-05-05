package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CommissionProposalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload sent by an admin to approve or reject a commission proposal.
 */
@Data
public class ReviewCommissionProposalRequest {

  /**
   * Must be either APPROVED or REJECTED.
   */
  @NotNull(message = "Action is required")
  private CommissionProposalStatus action;

  /** Optional explanation visible to the teacher. */
  private String adminNote;
}
