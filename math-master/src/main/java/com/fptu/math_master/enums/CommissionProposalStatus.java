package com.fptu.math_master.enums;

/**
 * Lifecycle states of a teacher's commission-split proposal.
 */
public enum CommissionProposalStatus {

  /** Submitted by teacher, awaiting admin review. */
  PENDING,

  /** Admin approved — this rate is now active for the teacher's future orders. */
  APPROVED,

  /** Admin rejected — teacher may submit a new proposal. */
  REJECTED
}
