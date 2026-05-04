package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionGenerationMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generation request for the unified Blueprint flow.
 *
 * <p>The legacy {@code generationMode} (PARAMETRIC vs AI_FROM_CANONICAL) is gone:
 * generation always reads the Blueprint and selects values via the constraint-aware
 * value selector. The mode field is kept (deprecated) only so existing FE clients
 * that send it do not 400; the value is ignored by the service layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTemplateQuestionsRequest {

  @NotNull(message = "count is required")
  @Min(value = 1, message = "count must be at least 1")
  private Integer count;

  @Builder.Default private boolean avoidDuplicates = true;

  /**
   * Free-text hint forwarded to the value selector, e.g. {@code "vary the sign of b"}
   * or {@code "use small integers"}. Optional — empty means no extra guidance.
   */
  private String distinctnessHint;

  /**
   * @deprecated retained for API compatibility; ignored by the new generator.
   */
  @Deprecated private QuestionGenerationMode generationMode;

  /**
   * @deprecated retained for API compatibility; ignored by the new generator.
   */
  @Deprecated private UUID canonicalQuestionId;
}
