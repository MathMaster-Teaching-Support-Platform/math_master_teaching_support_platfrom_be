package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTemplateBatchImportRequest {

  @NotEmpty(message = "Templates list cannot be empty")
  @Valid
  private List<QuestionTemplateRequest> templates;

  /**
   * Optional batch-level chapter used as fallback when a row's own
   * {@code QuestionTemplateRequest.chapterId} is null. Each row should normally
   * carry its own chapterId picked on the FE preview screen; this field exists
   * only to support a single-chapter quick-import flow.
   */
  private UUID chapterId;
}
