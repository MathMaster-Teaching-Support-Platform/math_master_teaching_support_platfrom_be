package com.fptu.math_master.enums;

/**
 * The enum of 'WithdrawalStatus'.
 * Represents the lifecycle of a manual withdrawal request.
 */
public enum WithdrawalStatus {
  /** User submitted request, waiting for OTP verification */
  PENDING_VERIFY,

  /** OTP verified, waiting for admin to process */
  PENDING_ADMIN,

  /** Admin is actively processing the bank transfer (optional intermediate) */
  PROCESSING,

  /** Transfer completed — wallet debited, bill emailed */
  SUCCESS,

  /** Admin rejected the request */
  REJECTED,

  /** User cancelled or OTP expired */
  CANCELLED
}
