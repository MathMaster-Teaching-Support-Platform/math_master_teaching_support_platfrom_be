package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
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

  private Instant createdAt;

  private Instant updatedAt;
}
