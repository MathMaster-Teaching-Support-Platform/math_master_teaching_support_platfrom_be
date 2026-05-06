package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.AssessmentSelectionStrategy;
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
public class GenerateAssessmentQuestionsRequest {

  @NotNull(message = "Exam matrix ID is required")
  private UUID examMatrixId;

  /**
   * Single source bank — kept for backward compatibility. New callers should
   * use {@link #questionBankIds} which supports multi-bank pools.
   *
   * <p>The matrix is a pure blueprint and no longer carries a binding bank;
   * callers pick the bank(s) at this step. If both {@code questionBankIds}
   * and {@code questionBankId} are null, the service falls back to the
   * matrix's stored default bank (legacy).
   */
  private UUID questionBankId;

  /**
   * Multi-bank source pool. When provided, takes precedence over
   * {@link #questionBankId} and over the matrix's stored default. Questions
   * are randomly drawn from the union of all listed banks for each cell of
   * the matrix.
   */
  private java.util.List<UUID> questionBankIds;

  private Boolean reuseApprovedQuestions;

  private AssessmentSelectionStrategy selectionStrategy;
}
