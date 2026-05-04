package com.fptu.math_master.dto.request;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for Feature 1: AI Auto-Extract Parameters From Question Text.
 * Teacher submits all content fields; AI reads them and suggests which numbers
 * should become {{param}} placeholders.
 */
@Data
@Builder
public class ExtractParametersRequest {

  /** The raw question text as written by the teacher (may contain numbers not yet parameterized). */
  private String templateText;

  /**
   * The answer formula, e.g. "(-b + sqrt(b^2-4*a*c)) / (2*a)".
   * Null if not yet defined.
   */
  private String answerFormula;

  /**
   * The solution steps template written by the teacher. Null if not yet written.
   */
  private String solutionSteps;

  /** LaTeX diagram content. Null if no diagram. */
  private String diagramLatex;

  /**
   * MCQ option texts, if the question type is MULTIPLE_CHOICE.
   * Key = "A"/"B"/"C"/"D", value = option text.
   */
  private Map<String, String> options;

  /**
   * TF clause texts, if the question type is TRUE_FALSE.
   * Key = "A"/"B"/"C"/"D", value = clause text.
   */
  private Map<String, String> clauses;
}
