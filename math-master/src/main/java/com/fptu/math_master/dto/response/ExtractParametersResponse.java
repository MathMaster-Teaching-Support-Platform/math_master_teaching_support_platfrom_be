package com.fptu.math_master.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for Feature 1: AI parameter extraction suggestion.
 * Returned by POST /api/question-templates/{id}/extract-parameters.
 */
@Data
@Builder
public class ExtractParametersResponse {

  /**
   * List of numbers AI identified as CHANGEABLE parameters.
   * Teacher can approve each one to make it a {{paramName}} placeholder.
   */
  private List<SuggestedParam> suggestedParams;

  /**
   * Numbers AI identified as FIXED (structural, derived, or constant).
   * These should NOT be replaced with placeholders.
   */
  private List<FixedValue> fixedValues;

  /**
   * The full template text with suggested replacements already applied,
   * e.g. "Cho phương trình {{a}}x² + {{b}}x + {{c}} = 0...".
   * Teacher can use this as a preview of the "Apply All" action.
   */
  private String templateResult;

  @Data
  @Builder
  public static class SuggestedParam {
    /** The original numeric string, e.g. "2", "-3", "1". */
    private String originalValue;

    /** Description of where in the text this number appears, e.g. "2x² (leading coefficient)". */
    private String location;

    /** Suggested placeholder name, e.g. "a", "b", "c". */
    private String suggestedName;

    /** Human-readable reason for making this a parameter. */
    private String reason;

    /** Always true for suggested params. */
    private boolean changeable;
  }

  @Data
  @Builder
  public static class FixedValue {
    /** The original numeric string, e.g. "2", "3". */
    private String originalValue;

    /** Description of where this appears, e.g. "x² (exponent)". */
    private String location;

    /** Reason why this should stay fixed, e.g. "Structural, defines equation degree". */
    private String reason;
  }
}
