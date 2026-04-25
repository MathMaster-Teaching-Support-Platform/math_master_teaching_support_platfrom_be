package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FR-TPL-002: Update Template (Edit Draft) Enhanced request with validation and version tracking
 * support
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionTemplateRequest {

  @NotBlank(message = "Template name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  private String description;

  @NotNull(message = "Template type is required")
  private QuestionType templateType;

  /**
   * Multi-language template text with placeholders {{param}} Example: {"vi": "Giải phương trình:
   * {{a}}x + {{b}} = {{c}}", "en": "Solve: {{a}}x + {{b}} = {{c}}"}
   */
  @NotNull(message = "Template text is required")
  private Map<String, Object> templateText;

  /**
   * Parameter definitions with type, min, max, step, exclude Example: {"a": {"type": "integer",
   * "min": 1, "max": 10, "exclude": [0]}}
   */
  @NotNull(message = "Parameters are required")
  private Map<String, Object> parameters;

  /** Answer formula using parameter names Example: "(c - b) / a" */
  @NotBlank(message = "Answer formula is required")
  private String answerFormula;

  /**
   * Options generator configuration for MCQ Example: {"count": 4, "distractors": [...],
   * "correctAnswer": "Use answerFormula"}
   */
  private Map<String, Object> optionsGenerator;

  /**
   * Mathematical constraints for parameter generation Example: ["a != 0", "answer > 0", "answer % 1
   * == 0"]
   */
  private String[] constraints;

  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  @NotEmpty(message = "At least one tag is required")
  @Size(max = 5, message = "Maximum 5 tags allowed")
  private java.util.List<com.fptu.math_master.enums.QuestionTag> tags;

  private Boolean isPublic;

  /**
   * If true and template is PUBLISHED, create a new DRAFT version instead of modifying the
   * published one
   */
  private Boolean createNewVersionIfPublished;
}
