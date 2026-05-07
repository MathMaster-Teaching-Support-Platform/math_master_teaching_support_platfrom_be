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

  // Answer formula is required for MCQ and SHORT_ANSWER, but not for TRUE_FALSE
  // Validation is handled in service layer based on question type
  private String answerFormula;

  private String diagramTemplate;

  private String solutionStepsTemplate;

  private Map<String, Object> optionsGenerator;

  private String[] constraints;

  /** Statement mutations for TRUE_FALSE questions - contains clause templates with truth values */
  private Map<String, Object> statementMutations;

  @NotNull(message = "Cognitive level is required")
  private CognitiveLevel cognitiveLevel;

  @Size(max = 5, message = "Maximum 5 tags allowed")
  private java.util.List<com.fptu.math_master.enums.QuestionTag> tags;

  private Boolean isPublic;

  /** Optional: assign this template to a question bank */
  private UUID questionBankId;

  /** 
   * Optional: chapter this template belongs to.
   * If not provided, will be auto-resolved from lessonId or questionBankId.
   */
  private UUID chapterId;

  /** Optional: lesson this template belongs to (implies chapterId) */
  private UUID lessonId;

  private UUID canonicalQuestionId;
}
