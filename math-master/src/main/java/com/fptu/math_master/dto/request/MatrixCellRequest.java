package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixCellRequest {

  @NotNull(message = "Chapter ID is required")
  private UUID chapterId;

  private String topic;

  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  @NotNull(message = "Difficulty is required")
  private QuestionDifficulty difficulty;

  private QuestionType questionType;

  @NotNull(message = "Number of questions is required")
  @Min(value = 0, message = "Number of questions must be at least 0")
  private Integer numQuestions;

  @NotNull(message = "Points per question is required")
  @DecimalMin(value = "0.0", message = "Points per question must be at least 0")
  private BigDecimal pointsPerQuestion;

  private String notes;
}
