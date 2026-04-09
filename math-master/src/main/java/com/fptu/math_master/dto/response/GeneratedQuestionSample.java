package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.QuestionDifficulty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedQuestionSample {

  private String questionText;
  private Map<String, String> options;
  private String correctAnswer;
  private String explanation;
  private String solutionSteps;
  private String diagramData;
  private QuestionDifficulty calculatedDifficulty;
  private Map<String, Object> usedParameters;
  private String answerCalculation;
}
