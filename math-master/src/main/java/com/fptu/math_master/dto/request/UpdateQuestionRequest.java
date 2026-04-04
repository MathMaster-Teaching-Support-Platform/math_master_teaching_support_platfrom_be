package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
public class UpdateQuestionRequest {

  @Size(max = 5000, message = "Question text must not exceed 5000 characters")
  @Schema(description = "The question content/text")
  private String questionText;

  @Schema(description = "Options for multiple choice questions (JSON format)")
  private Map<String, Object> options;

  @Schema(description = "Correct answer")
  private String correctAnswer;

  @Schema(description = "Explanation for the answer")
  private String explanation;

  @Schema(description = "Generated step-by-step solution in LaTeX or plain text")
  private String solutionSteps;

  @Schema(description = "Generated diagram data payload")
  private Map<String, Object> diagramData;

  @DecimalMin(value = "0.0", message = "Points must be at least 0")
  @DecimalMax(value = "1000.0", message = "Points must not exceed 1000")
  @Schema(description = "Points for this question")
  private BigDecimal points;

  @Schema(description = "Difficulty level")
  private QuestionDifficulty difficulty;

  @Schema(description = "Bloom's cognitive level")
  private CognitiveLevel cognitiveLevel;

  @Schema(description = "Tags for categorization")
  private String[] tags;

  @Schema(description = "Question status: DRAFT, PUBLISHED, AI_DRAFT, REJECTED")
  private QuestionStatus status;
}
