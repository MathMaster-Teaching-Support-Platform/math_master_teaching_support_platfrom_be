package com.fptu.math_master.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for percentage-based assessment generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PercentageBasedGenerationResponse {

  private UUID assessmentId;
  
  private String assessmentTitle;
  
  private Integer totalQuestionsRequested;
  
  private Integer totalQuestionsGenerated;
  
  private Integer totalPoints;
  
  private List<CognitiveLevelDistributionResponse> distribution;
  
  private List<String> warnings;
  
  private String message;
  
  private Boolean success;
}
