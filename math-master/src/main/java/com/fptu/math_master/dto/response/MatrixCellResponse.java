package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One cell in the exam-matrix table — a single (dạng bài × mức độ) combination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixCellResponse {

  private UUID mappingId;
  private CognitiveLevel cognitiveLevel;

  /** Short display label: NB / TH / VD / VDC. */
  private String cognitiveLevelLabel;

  private Integer questionCount;
  private BigDecimal pointsPerQuestion;
  private BigDecimal totalPoints;
}
