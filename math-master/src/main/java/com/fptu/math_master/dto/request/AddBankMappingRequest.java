package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBankMappingRequest {

  @NotNull(message = "questionBankId is required")
  private UUID questionBankId;

  @NotNull(message = "difficultyDistribution is required")
  private Map<QuestionDifficulty, Integer> difficultyDistribution;

  @NotNull(message = "cognitiveLevel is required")
  private CognitiveLevel cognitiveLevel;

  @DecimalMin(value = "0.01", message = "pointsPerQuestion must be > 0")
  private BigDecimal pointsPerQuestion;
}
