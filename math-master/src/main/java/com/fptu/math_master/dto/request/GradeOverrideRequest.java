package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeOverrideRequest {

  @NotNull(message = "Answer ID is required")
  private UUID answerId;

  @NotNull(message = "New points is required")
  private BigDecimal newPoints;

  private String reason;
}
