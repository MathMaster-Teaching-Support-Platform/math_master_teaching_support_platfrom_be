package com.fptu.math_master.dto.response;

import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the Standard Teacher Flow auto-blueprint generation.
 * Wraps a pre-filled QuestionTemplateRequest ready for teacher review,
 * along with extraction notes and a confidence score.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoBlueprintResponse {

  /**
   * The AI-generated Blueprint, pre-filled with template text, parameters,
   * answer formula, constraints, and options/clauses. Ready for teacher review.
   */
  private QuestionTemplateRequest blueprint;

  /**
   * Human-readable notes explaining what AI extracted and any warnings.
   * Examples:
   *   "Identified 'a' as leading coefficient (original: 2)"
   *   "Warning: placeholder {{c}} has no parameter definition"
   *   "Warning: answer formula failed smoke test: division by zero"
   */
  private List<String> extractionNotes;

  /**
   * Confidence score (0.0 - 1.0). Starts at 1.0 and is reduced:
   *   - by 0.2x for each undefined placeholder
   *   - by 0.5x if the formula fails the smoke test
   * A score >= 0.9 indicates a clean extraction with no warnings.
   */
  private double confidence;
}
