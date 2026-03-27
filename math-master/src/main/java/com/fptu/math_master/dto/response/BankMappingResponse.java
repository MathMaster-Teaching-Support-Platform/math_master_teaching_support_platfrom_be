package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import java.math.BigDecimal;
import java.time.Instant;
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
public class BankMappingResponse {

  private UUID id;
  private UUID examMatrixId;
  private UUID questionBankId;
  private String questionBankName;
  private UUID matrixRowId;
  private Map<QuestionDifficulty, Integer> difficultyDistribution;
  private CognitiveLevel cognitiveLevel;
  private BigDecimal pointsPerQuestion;
  private Instant createdAt;
  private Instant updatedAt;
}
