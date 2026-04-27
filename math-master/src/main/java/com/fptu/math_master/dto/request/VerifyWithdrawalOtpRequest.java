package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyWithdrawalOtpRequest {

  @NotNull(message = "Withdrawal request ID is required")
  private UUID withdrawalRequestId;

  @NotBlank(message = "OTP code is required")
  private String otpCode;
}
