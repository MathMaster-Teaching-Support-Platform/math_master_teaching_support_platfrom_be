package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixValidationReport {

  private boolean canApprove;
  private List<String> errors;
  private List<String> warnings;

  // Summary
  private Integer actualQuestions;
  private Integer targetQuestions;
  private BigDecimal actualPoints;
  private BigDecimal targetPoints;
  private Integer totalCells;
  private Integer filledCells;

  // Distribution
  private Map<String, Double> chapterDistribution;
  private Map<String, Double> difficultyDistribution;
  private Map<String, Integer> cognitiveLevelCoverage;

  private boolean allCellsFilled;
  private boolean questionsMatchTarget;
  private boolean pointsMatchTarget;
  private boolean difficultyBalanced;
  private boolean allCognitiveLevelsCovered;
}

