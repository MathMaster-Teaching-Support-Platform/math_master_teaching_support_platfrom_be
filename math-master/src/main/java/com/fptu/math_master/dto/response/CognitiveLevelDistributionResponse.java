package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response showing the distribution of questions by cognitive level.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitiveLevelDistributionResponse {

  private CognitiveLevel cognitiveLevel;
  
  private Double requestedPercentage;
  
  private Integer requestedCount;
  
  private Integer actualCount;
  
  private Integer availableInBank;
  
  private String status; // "SUCCESS", "PARTIAL", "INSUFFICIENT"
  
  private String message;
}
