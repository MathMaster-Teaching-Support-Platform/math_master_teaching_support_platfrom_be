package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /exam-matrices/{matrixId}/cells/{cellId}/finalize.
 * Persists selected preview candidates to questions, assessment_questions,
 * and matrix_question_mapping in a single atomic transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizePreviewRequest {

  /** Template used to generate the questions (for traceability). */
  @NotNull(message = "templateId is required")
  private UUID templateId;

  /**
   * Points awarded per question in assessment_questions.pointsOverride.
   * Must be > 0.
   */
  @NotNull(message = "pointsPerQuestion is required")
  @DecimalMin(value = "0.01", message = "pointsPerQuestion must be greater than 0")
  private BigDecimal pointsPerQuestion;

  /**
   * When true: existing mappings for this cell are removed and replaced.
   * When false: questions are appended (warns if total exceeds cell target).
   */
  @NotNull(message = "replaceExisting is required")
  private Boolean replaceExisting;

  /** Optional question bank to associate generated questions with. May be null. */
  private UUID questionBankId;

  /** List of questions to persist. Must not be empty. */
  @NotNull
  @NotEmpty(message = "questions list must not be empty")
  @Valid
  private List<QuestionItem> questions;

  // ──────────────────────────────────────────────────────────────────────────
  // Nested: one question from the preview batch to be finalised
  // ──────────────────────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QuestionItem {

    /** Fully rendered question text (required, non-blank). */
    @NotBlank(message = "questionText must not be blank")
    private String questionText;

    /** Question type (must match cell if cell constrains it). */
    @NotNull(message = "questionType is required")
    private QuestionType questionType;

    /**
     * MCQ options map: exactly keys A/B/C/D → option text.
     * Required when questionType = MULTIPLE_CHOICE.
     */
    private Map<String, String> options;

    /**
     * For MCQ: one of A/B/C/D.
     * For other types: the correct answer value as a string.
     */
    @NotBlank(message = "correctAnswer is required")
    private String correctAnswer;

    /** Difficulty determined during generation. */
    @NotNull(message = "difficulty is required")
    private QuestionDifficulty difficulty;

    /** Cognitive level (should match or be compatible with cell requirement). */
    @NotNull(message = "cognitiveLevel is required")
    private CognitiveLevel cognitiveLevel;

    /** Tags for this question (bloom taxonomy / topic tags). */
    private String[] tags;

    /** Optional explanation / solution steps. */
    private String explanation;

    /**
     * Generation metadata for traceability:
     * paramsUsed, answerFormula, seed, generatedAt, answerCalculation, etc.
     */
    private Map<String, Object> generationMetadata;
  }
}

