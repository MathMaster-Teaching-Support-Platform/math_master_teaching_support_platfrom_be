package com.fptu.math_master.dto.response;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for {@code POST /question-templates/blueprint-from-real-question}.
 * Returns a Blueprint draft for the teacher to review and confirm. Nothing is
 * persisted yet — the FE shows a diff and the teacher edits per-parameter
 * constraints before the final {@code POST /question-templates}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintFromRealQuestionResponse {

  /** Question text with {{a}} {{b}} placeholders substituted in. */
  private String templateText;

  /** Answer formula in terms of placeholders, e.g. {@code "(-{{b}})/{{a}}"}. */
  private String answerFormula;

  /** Solution steps with placeholders propagated through. May be null. */
  private String solutionStepsTemplate;

  /** LaTeX/TikZ diagram with placeholders propagated through. May be null. */
  private String diagramTemplate;

  /**
   * MCQ options with placeholders, e.g. {@code {"A": "$(-{{b}})/{{a}}$", ...}}.
   * Null for non-MCQ.
   */
  private Map<String, String> optionsGenerator;

  /**
   * TF clauses with placeholders propagated through. Each entry has
   * {@code text} (parameterised) and {@code truthValue}. Null for non-TF.
   */
  private List<TfClauseDraft> clauseTemplates;

  /** Parameters extracted from the real question, with constraint guesses. */
  private List<BlueprintParameter> parameters;

  /** Cross-parameter constraints, e.g. {@code "a < b"}. */
  private List<String> globalConstraints;

  /**
   * Diff entries the FE renders side-by-side: real value → templated form.
   * One row per artifact (templateText, answerFormula, options.A, ...).
   */
  private List<DiffEntry> diff;

  /** Warnings the teacher should read before confirming. */
  private List<String> warnings;

  /**
   * Confidence in [0,1]. Reduced when an extracted placeholder has no parameter
   * definition, or when a smoke-eval of the answer formula fails.
   */
  private double confidence;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DiffEntry {
    /** Where this change applied: {@code "templateText"}, {@code "options.A"}, ... */
    private String field;

    private String before;
    private String after;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TfClauseDraft {
    private String key;
    private String text;
    private boolean truthValue;
  }
}
