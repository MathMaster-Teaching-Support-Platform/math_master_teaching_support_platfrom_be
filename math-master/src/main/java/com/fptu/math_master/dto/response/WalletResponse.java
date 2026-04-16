package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fptu.math_master.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

  private UUID walletId;

  private UUID userId;

  private BigDecimal balance;

  /** Lifetime total of all SUCCESS DEPOSIT transactions */
  private BigDecimal totalDeposited;

  /** Lifetime total of all SUCCESS PAYMENT transactions */
  private BigDecimal totalSpent;

  /** Total number of transactions (all statuses) */
  private long transactionCount;

  private Status status;

  private Instant createdAt;

  private Instant updatedAt;
}
