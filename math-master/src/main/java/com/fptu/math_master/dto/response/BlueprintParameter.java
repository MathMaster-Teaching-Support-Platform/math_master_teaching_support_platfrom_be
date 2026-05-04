package com.fptu.math_master.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One parameter inside a Blueprint. Both Method 1 (AI reverse-templating) and
 * Method 2 (manual) produce this shape, and the generator reads it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintParameter {

  /** The placeholder name without braces, e.g. {@code "a"}. */
  private String name;

  /**
   * Plain-text constraint. The generation-time AI value selector reads this
   * directly. Examples: {@code "integer, 1 ≤ a ≤ 9, a is even"},
   * {@code "real, x ≠ 0"}, {@code "a is prime, a < 100"}.
   */
  private String constraintText;

  /** Teacher-confirmed example value. Used to seed the AI on first generation. */
  private Object sampleValue;

  /**
   * Where this parameter occurs across the Blueprint, e.g.
   * {@code ["templateText", "answerFormula", "options.A", "solutionSteps"]}.
   * Helps the FE show a diff view and lets the substitutor verify coverage.
   */
  private List<String> occurrences;
}
