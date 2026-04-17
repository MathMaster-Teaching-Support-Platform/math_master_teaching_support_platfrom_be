package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

  private UUID transactionId;

  private UUID walletId;

  private Long orderCode;

  private BigDecimal amount;

  private TransactionType type;

  private TransactionStatus status;

  private String description;

  private String paymentLinkId;

  private String referenceCode;

  private Instant transactionDate;

  /** ISO-8601 expiry for PENDING transactions (15 min after creation) */
  private Instant expiresAt;

  private Instant createdAt;

  private Instant updatedAt;
}
