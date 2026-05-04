package com.fptu.math_master.dto.request;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for Feature 2: AI Generates Parameter Values.
 * Backend sends all content fields + existing samples so AI can produce
 * valid, non-duplicate parameter combinations satisfying math constraints.
 */
@Data
@Builder
public class GenerateParametersRequest {

  /** Template text with {{a}} {{b}} {{c}} placeholders already applied. */
  private String templateText;

  /** Answer formula. AI reads this to detect math constraints (e.g. b²-4ac ≥ 0). */
  private String answerFormula;

  /** Solution steps template. AI ensures generated values make the steps meaningful. */
  private String solutionSteps;

  /** LaTeX diagram. AI ensures values render safely. */
  private String diagramLatex;

  /**
   * MCQ option texts with placeholders.
   * Key = "A"/"B"/"C"/"D", value = option text (may contain {{params}}).
   */
  private Map<String, String> options;

  /**
   * TF clause texts with placeholders.
   * Key = "A"/"B"/"C"/"D", value = clause statement (may contain {{params}}).
   */
  private Map<String, String> clauses;

  /**
   * Parameter names detected in the template, e.g. ["a", "b", "c"].
   */
  private List<String> parameters;

  /**
   * Previously generated samples with their parameter combinations.
   * AI uses this to avoid duplicate parameter sets.
   */
  private List<Map<String, Object>> sampleQuestions;
}
