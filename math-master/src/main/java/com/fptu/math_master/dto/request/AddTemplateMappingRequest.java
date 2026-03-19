package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class AddTemplateMappingRequest {

  @NotNull(message = "Template ID is required")
  private UUID templateId;

  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  @NotNull(message = "Question count is required")
  @Min(value = 1, message = "Question count must be at least 1")
  private Integer questionCount;

  @NotNull(message = "Points per question is required")
  @DecimalMin(value = "0.01", message = "Points per question must be greater than 0")
  private BigDecimal pointsPerQuestion;
}
