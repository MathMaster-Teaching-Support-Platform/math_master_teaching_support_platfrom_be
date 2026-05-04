package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Standard Teacher Flow.
 * Teacher submits a fully concrete question (real numbers, real answer).
 * AI converts it into a reusable Blueprint with placeholders, parameters,
 * constraints, and answer formula. Teacher reviews before final save.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoBlueprintRequest {

  /** The concrete question text as written by the teacher (contains real numbers). */
  @NotBlank(message = "Question text is required")
  private String questionText;

  /** The question type (MCQ, TRUE_FALSE, SHORT_ANSWER). */
  @NotNull(message = "Question type is required")
  private QuestionType questionType;

  /** The correct answer to the concrete question (real number or text). */
  private String correctAnswer;

  /**
   * MCQ options with real values (not formulas yet).
   * Key = "A"/"B"/"C"/"D", value = concrete option text.
   */
  private Map<String, String> options;

  /**
   * TF clause texts with truth values.
   * Key = "A"/"B"/"C"/"D", value = clause text.
   */
  private Map<String, Object> clauses;

  /** Concrete solution steps as written by the teacher. */
  private String solutionSteps;

  /** LaTeX diagram content (may contain concrete numbers). */
  private String diagramLatex;

  /** Cognitive level of the question. */
  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  // --- Optional metadata ---

  /** School grade level (e.g., "10", "11", "12"). */
  private String gradeLevel;

  /** Subject ID to associate the generated template. */
  private UUID subjectId;

  /** Chapter ID to associate the generated template. */
  private UUID chapterId;

  /** Question bank ID to associate the generated template. */
  private UUID questionBankId;
}
