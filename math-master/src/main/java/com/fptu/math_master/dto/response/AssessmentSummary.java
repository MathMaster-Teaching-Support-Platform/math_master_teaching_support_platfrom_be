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
