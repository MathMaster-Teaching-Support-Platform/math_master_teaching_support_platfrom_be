package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@AttributeOverride(
    name = "id",
    column = @Column(name = "transaction_id", updatable = false, nullable = false))
@Table(name = "transactions")
/**
 * The entity of 'Transaction'.
 */
public class Transaction extends BaseEntity {

  /**
   * wallet
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  /**
   * order_code
   */
  @Column(name = "order_code", unique = true)
  private Long orderCode;

  /**
   * amount
   */
  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  /**
   * type
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private TransactionType type;

  /**
   * status
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TransactionStatus status;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * payment_link_id
   */
  @Column(name = "payment_link_id")
  private String paymentLinkId;

  /**
   * reference_code
   */
  @Column(name = "reference_code")
  private String referenceCode;

  /**
   * transaction_date
   */
  @Column(name = "transaction_date")
  private Instant transactionDate;

  /**
   * expires_at — when a PENDING transaction expires (15 minutes after creation)
   */
  @Column(name = "expires_at")
  private Instant expiresAt;
}
