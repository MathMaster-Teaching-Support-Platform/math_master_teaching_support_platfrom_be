package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributeAssessmentPointsResponse {
  private int updated;
  private BigDecimal pointPerQuestion;
  private BigDecimal totalPoints;
  private Integer scale;
  private String strategy;
}
