package com.fptu.math_master.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FR-TPL-002: Template Validation Response
 * Returns real-time validation results with severity levels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateValidationResponse {

  /** Whether the template is valid overall */
  private Boolean isValid;

  /** Total number of errors */
  private Integer errorCount;

  /** Total number of warnings */
  private Integer warningCount;

  /** Critical errors that prevent saving */
  private List<ValidationIssue> errors;

  /** Warnings that should be reviewed but don't prevent saving */
  private List<ValidationIssue> warnings;

  /** Informational messages */
  private List<ValidationIssue> info;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidationIssue {
    /** Issue category: TEMPLATE_TEXT, PARAMETERS, FORMULA, CONSTRAINTS, DIFFICULTY_RULES, OPTIONS_GENERATOR, TAGS */
    private String category;

    /** Specific field that has the issue */
    private String field;

    /** Human-readable issue description */
    private String message;

    /** Severity: ERROR, WARNING, INFO */
    private IssueSeverity severity;

    /** Optional: suggested fix */
    private String suggestion;
  }

  public enum IssueSeverity {
    ERROR,   // Blocks saving
    WARNING, // Can save but should review
    INFO     // Informational only
  }
}

