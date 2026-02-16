package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingAnalyticsResponse {

  private Long totalSubmissions;
  private Long gradedSubmissions;
  private Long pendingSubmissions;
  private BigDecimal averageScore;
  private BigDecimal medianScore;
  private BigDecimal highestScore;
  private BigDecimal lowestScore;
  private Double passRate;
  private Map<String, Long> scoreDistribution; // score range -> count
  private Map<String, Double> questionDifficulty; // question id -> % correct
  private Long averageTimeSpentSeconds;
}
