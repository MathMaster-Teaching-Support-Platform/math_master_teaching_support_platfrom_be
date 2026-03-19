package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
public class CreateQuestionRequest {

  @NotBlank(message = "Question text is required")
  @Size(max = 5000, message = "Question text must not exceed 5000 characters")
  @Schema(description = "The question content/text", example = "What is 2 + 2?")
  private String questionText;

  @NotNull(message = "Question type is required")
  @Schema(description = "Type of question: MCQ, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER, etc.")
  private QuestionType questionType;

  @Schema(
      description = "Options for multiple choice questions (JSON format)",
      example = "{\"A\": \"4\", \"B\": \"5\", \"C\": \"3\"}")
  private Map<String, Object> options;

  @Schema(description = "Correct answer (can be single value or multiple)", example = "A or 4")
  private String correctAnswer;

  @Schema(description = "Explanation for the answer")
  private String explanation;

  @DecimalMin(value = "0.0", message = "Points must be at least 0")
  @DecimalMax(value = "1000.0", message = "Points must not exceed 1000")
  @Builder.Default
  @Schema(description = "Points for this question", example = "1.0")
  private BigDecimal points = BigDecimal.valueOf(1.0);

  @Schema(description = "Difficulty level", example = "MEDIUM")
  @Builder.Default
  private QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;

  @NotNull(message = "Cognitive level is required")
  @Schema(
      description =
          "Bloom's cognitive level: REMEMBER, UNDERSTAND, APPLY, ANALYZE, EVALUATE, CREATE")
  private CognitiveLevel cognitiveLevel;

  @Schema(description = "Tags for categorization")
  private String[] tags;

  @Schema(description = "Question bank ID (optional)")
  private java.util.UUID questionBankId;

  @Schema(description = "Template ID if generated from template")
  private java.util.UUID templateId;
}
