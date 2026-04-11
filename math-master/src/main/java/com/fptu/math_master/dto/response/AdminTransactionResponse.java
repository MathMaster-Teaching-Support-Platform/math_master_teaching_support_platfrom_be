package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTransactionResponse {

  private UUID id;
  private UUID userId;
  private String userName;
  /**
   * planId is not applicable in the current wallet-deposit model.
   * Will be null until a subscription plan entity is introduced.
   */
  private String planId;
  private String planName;
  private BigDecimal amount;
  /** Mapped from TransactionStatus: SUCCESS→completed, PENDING/PROCESSING→pending, FAILED/CANCELLED→failed */
  private String status;
  /** Always "payos" — the only payment gateway integrated */
  private String paymentMethod;
  private Instant createdAt;
}
