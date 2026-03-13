package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionSourceType;
import com.fptu.math_master.enums.QuestionStatus;
import com.fptu.math_master.enums.QuestionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

  @Schema(description = "Question ID")
  private UUID id;

  @Schema(description = "Creator user ID")
  private UUID createdBy;

  @Schema(description = "Creator's full name")
  private String creatorName;

  @Schema(description = "Question text")
  private String questionText;

  @Schema(description = "Question type: MCQ, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER, etc.")
  private QuestionType questionType;

  @Schema(description = "Options for multiple choice")
  private Map<String, Object> options;

  @Schema(description = "Correct answer")
  private String correctAnswer;

  @Schema(description = "Explanation")
  private String explanation;

  @Schema(description = "Points value")
  private BigDecimal points;

  @Schema(description = "Difficulty level")
  private QuestionDifficulty difficulty;

  @Schema(description = "Cognitive level (Bloom's taxonomy)")
  private CognitiveLevel cognitiveLevel;

  @Schema(description = "Question status")
  private QuestionStatus questionStatus;

  @Schema(description = "Source: MANUAL, AI_GENERATED, IMPORTED, etc.")
  private QuestionSourceType questionSourceType;

  @Schema(description = "Tags")
  private String[] tags;

  @Schema(description = "Template this was generated from (if applicable)")
  private UUID templateId;

  @Schema(description = "Question bank ID")
  private UUID questionBankId;

  @Schema(description = "Question bank name")
  private String questionBankName;

  @Schema(description = "When created")
  private Instant createdAt;

  @Schema(description = "When last updated")
  private Instant updatedAt;
}
