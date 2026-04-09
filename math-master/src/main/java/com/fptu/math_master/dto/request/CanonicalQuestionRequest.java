package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalQuestionRequest {

  private String title;

  @NotBlank(message = "problemText is required")
  private String problemText;

  private String solutionSteps;

  private String diagramDefinition;

  @NotNull(message = "problemType is required")
  private QuestionType problemType;

  @NotNull(message = "cognitiveLevel is required")
  private CognitiveLevel cognitiveLevel;
}