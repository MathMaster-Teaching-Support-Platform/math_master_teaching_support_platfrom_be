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
 * FR-TPL-002: Update Template (Edit Draft)
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

  private QuestionType templateType;

  /**
   * Simplified question content with {{param}} placeholders.
   * Example: "Giải phương trình: {{a}}x + {{b}} = {{c}}"
   */
  private String content;

  /**
   * Legacy multi-language template text. If content is provided, this is ignored.
   */
  private Map<String, Object> templateText;

  /**
   * Parameter definitions with type, min, max, step, exclude
   */
  private Map<String, Object> parameters;

  /** Answer formula using parameter names. Example: "(c - b) / a" */
  private String answerFormula;

  /**
   * Step-by-step solution explanation.
   */
  private String solution;

  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  @NotEmpty(message = "At least one tag is required")
  private String[] tags;

  private Boolean isPublic;

  /**
   * If true and template is PUBLISHED, create a new DRAFT version instead of modifying the
   * published one
   */
  private Boolean createNewVersionIfPublished;
}
