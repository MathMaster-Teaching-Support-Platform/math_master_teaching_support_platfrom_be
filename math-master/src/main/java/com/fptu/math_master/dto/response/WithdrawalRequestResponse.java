package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.WithdrawalStatus;
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
public class WithdrawalRequestResponse {

  private UUID withdrawalRequestId;
  private BigDecimal amount;
  private String bankName;
  private String bankAccountNumber;
  private String bankAccountName;
  private WithdrawalStatus status;
  private String proofImageUrl;
  private String adminNote;
  private UUID transactionId;
  private Instant processedAt;
  private Instant createdAt;
  private Instant updatedAt;
}
