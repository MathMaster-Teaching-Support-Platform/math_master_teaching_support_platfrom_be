package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsOverrideRequest {

  @NotNull(message = "Question ID is required")
  private UUID questionId;

  @DecimalMin(value = "0.0", message = "Points must be at least 0")
  private BigDecimal pointsOverride;
}

