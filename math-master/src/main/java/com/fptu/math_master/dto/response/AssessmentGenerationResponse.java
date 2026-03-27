package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentGenerationResponse {

  private Integer totalQuestionsGenerated;
  private Integer questionsFromBank;
  private Integer questionsFromAi;
  private Integer totalPoints;
  private java.util.List<String> warnings;
  private String message;
}
