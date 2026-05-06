package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenCostConfigRequest {
  @Min(value = 1, message = "COST_PER_USE_MIN")
  @Max(value = 100, message = "COST_PER_USE_MAX")
  private Integer costPerUse;

  private Boolean isActive;
}
