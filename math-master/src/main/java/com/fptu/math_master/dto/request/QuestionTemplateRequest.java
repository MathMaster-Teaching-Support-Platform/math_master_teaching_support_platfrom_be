package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTemplateRequest {

  @NotBlank(message = "Template name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  private String description;

  private QuestionType templateType;

  /**
   * Simplified question content with {{param}} placeholders.
   * Example: "Giải phương trình {{a}}x + {{b}} = 0"
   */
  private String content;

  /**
   * Legacy multi-language map. If content is provided, templateText is ignored.
   */
  private Map<String, Object> templateText;

  /** Parameter definitions. May be empty for simple templates. */
  private Map<String, Object> parameters;

  /** Answer formula using parameter names. Example: "(-b)/a" */
  private String answerFormula;

  /**
   * Step-by-step solution explanation.
   * Example: "Chuyển vế: {{a}}x = -{{b}}, chia hai vế: x = -{{b}}/{{a}}"
   */
  private String solution;

  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  @NotEmpty(message = "At least one tag is required")
  private String[] tags;

  private Boolean isPublic;

  /** Optional: assign this template to a question bank */
  private UUID questionBankId;

  private UUID canonicalQuestionId;
}
