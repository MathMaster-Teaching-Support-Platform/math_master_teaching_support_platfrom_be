package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMin;
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
public class AddQuestionToAssessmentRequest {

  @NotNull(message = "Question ID is required")
  private UUID questionId;

  private Integer orderIndex;

  @DecimalMin(value = "0.0", message = "Points override must be non-negative")
  private BigDecimal pointsOverride;
}

