package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSummary {

  private Long totalQuestions;
  private BigDecimal totalPoints;
  private Integer timeLimitMinutes;
  private String startDate;
  private String endDate;
  private Boolean hasSchedule;
  private Boolean canPublish;
  private String validationMessage;
}
