package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One cell in the exam-matrix table — a single (part × cognitive level) combination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixCellResponse {

  private UUID mappingId;
  
  private Integer partNumber;
  private QuestionType questionType;
  
  private CognitiveLevel cognitiveLevel;

  /** Short display label: NB / TH / VD / VDC. */
  private String cognitiveLevelLabel;

  private Integer questionCount;
  private BigDecimal pointsPerQuestion;
  private BigDecimal totalPoints;
}
