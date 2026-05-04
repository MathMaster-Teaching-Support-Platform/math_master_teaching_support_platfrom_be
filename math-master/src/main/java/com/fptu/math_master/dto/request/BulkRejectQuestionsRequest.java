package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reject multiple UNDER_REVIEW questions in one call. Rejection is a soft delete:
 * the question moves to ARCHIVED and is excluded from the review queue and
 * downstream selection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRejectQuestionsRequest {

  @NotEmpty(message = "questionIds must not be empty")
  private List<UUID> questionIds;

  /** Optional reason persisted into Question.generationMetadata.rejectionReason. */
  private String reason;
}
