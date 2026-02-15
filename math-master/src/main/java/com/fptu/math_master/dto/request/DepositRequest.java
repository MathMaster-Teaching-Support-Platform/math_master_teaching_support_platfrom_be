package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

  @NotNull(message = "Amount is required")
  @Min(value = 10000, message = "Minimum deposit amount is 10,000 VND")
  private BigDecimal amount;

  private String description;
}
