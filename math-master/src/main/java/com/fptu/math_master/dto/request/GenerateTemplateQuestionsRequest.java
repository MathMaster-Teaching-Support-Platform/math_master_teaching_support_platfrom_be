package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionGenerationMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTemplateQuestionsRequest {

  @NotNull(message = "count is required")
  @Min(value = 1, message = "count must be at least 1")
  private Integer count;

  @Builder.Default
  private QuestionGenerationMode generationMode = QuestionGenerationMode.PARAMETRIC;

  private UUID canonicalQuestionId;
  
  @Builder.Default
  private boolean avoidDuplicates = false;
}
