package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
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

  private Map<String, Object> diagramDefinition;

  @NotNull(message = "problemType is required")
  private QuestionType problemType;

  private QuestionDifficulty difficulty;
}