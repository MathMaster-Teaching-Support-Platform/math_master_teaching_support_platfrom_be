package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestDto {

  @NotNull(message = "Amount is required")
  @DecimalMin(value = "10000", message = "Withdrawal amount must be at least 10,000 VND")
  private BigDecimal amount;

  @NotBlank(message = "Bank name is required")
  @Size(max = 100, message = "Bank name must not exceed 100 characters")
  private String bankName;

  @NotBlank(message = "Bank account number is required")
  @Size(max = 50, message = "Bank account number must not exceed 50 characters")
  private String bankAccountNumber;

  @NotBlank(message = "Bank account name is required")
  @Size(max = 100, message = "Bank account name must not exceed 100 characters")
  private String bankAccountName;

  @NotBlank(message = "Password is required")
  private String password;
}
