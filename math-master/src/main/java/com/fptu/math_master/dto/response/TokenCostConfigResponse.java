package com.fptu.math_master.dto.response;

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
public class TokenCostConfigResponse {
  private UUID id;
  private String featureKey;
  private String featureLabel;
  private Integer costPerUse;
  private Boolean isActive;
  private Instant updatedAt;
  private UUID updatedBy;
}
