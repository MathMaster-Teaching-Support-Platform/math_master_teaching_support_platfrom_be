package com.fptu.math_master.entity;

import com.fptu.math_master.enums.WithdrawalStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The entity of 'WithdrawalRequest'.
 * Represents a manual withdrawal request submitted by a user.
 * Invariants:
 *  - amount >= 10_000
 *  - Wallet balance is only debited when status transitions to SUCCESS
 *  - otp_code is stored as BCrypt hash, never as plaintext
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@AttributeOverride(
    name = "id",
    column = @Column(name = "withdrawal_request_id", updatable = false, nullable = false))
@Table(name = "withdrawal_requests")
public class WithdrawalRequest extends BaseEntity {

  /**
   * wallet — FK to the wallet from which money will be withdrawn
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  /**
   * user — denormalized FK for easy lookup without joining through wallet
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * amount — VND, must be >= 10_000
   */
  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  /**
   * bank_name — recipient bank name
   */
  @Column(name = "bank_name", nullable = false, length = 100)
  private String bankName;

  /**
   * bank_account_number — recipient account number
   */
  @Column(name = "bank_account_number", nullable = false, length = 50)
  private String bankAccountNumber;

  /**
   * bank_account_name — recipient account holder name
   */
  @Column(name = "bank_account_name", nullable = false, length = 100)
  private String bankAccountName;

  /**
   * status — current lifecycle state of this withdrawal request
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private WithdrawalStatus status;

  /**
   * otp_code — BCrypt hash of the 6-digit OTP. Never store or return plaintext.
   */
  @Column(name = "otp_code", length = 72)
  private String otpCode;

  /**
   * otp_expiry — when the OTP expires (now + 10 minutes)
   */
  @Column(name = "otp_expiry")
  private Instant otpExpiry;

  /**
   * proof_image_url — MinIO URL of the transfer proof uploaded by admin
   */
  @Column(name = "proof_image_url", columnDefinition = "TEXT")
  private String proofImageUrl;

  /**
   * admin_note — admin comment or rejection reason
   */
  @Column(name = "admin_note", columnDefinition = "TEXT")
  private String adminNote;

  /**
   * transaction — set when status transitions to SUCCESS
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transaction_id")
  private Transaction transaction;

  /**
   * processed_at — timestamp when admin completed or rejected the request
   */
  @Column(name = "processed_at")
  private Instant processedAt;
}
