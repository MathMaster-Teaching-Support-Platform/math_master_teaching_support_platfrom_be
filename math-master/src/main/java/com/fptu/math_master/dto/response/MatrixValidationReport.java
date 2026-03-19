package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixValidationReport {

  private boolean canApprove;
  private List<String> errors;
  private List<String> warnings;

  // Summary
  private Integer totalTemplateMappings;
  private Integer totalQuestions;
  private BigDecimal totalPoints;
  private Integer totalQuestionsTarget;
  private BigDecimal totalPointsTarget;

  // Distribution
  private Map<String, Integer> cognitiveLevelCoverage;

  private boolean questionsMatchTarget;
  private boolean pointsMatchTarget;
  private boolean allCognitiveLevelsCovered;
}
