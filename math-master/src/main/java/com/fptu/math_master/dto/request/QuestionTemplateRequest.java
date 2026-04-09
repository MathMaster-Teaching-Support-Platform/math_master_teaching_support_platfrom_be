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

  @NotNull(message = "Template type is required")
  private QuestionType templateType;

  @NotNull(message = "Template text is required")
  private Map<String, Object> templateText;

  @NotNull(message = "Parameters are required")
  private Map<String, Object> parameters;

  @NotBlank(message = "Answer formula is required")
  private String answerFormula;

  private String diagramTemplate;

  private Map<String, Object> optionsGenerator;

  private String[] constraints;

  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  @NotEmpty(message = "At least one tag is required")
  private String[] tags;

  private Boolean isPublic;

  /** Optional: assign this template to a question bank */
  private UUID questionBankId;

  private UUID canonicalQuestionId;
}
