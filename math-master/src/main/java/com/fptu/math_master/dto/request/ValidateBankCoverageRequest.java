package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pre-flight validation: do the chosen bank(s) satisfy the matrix's per-cell
 * requirements (chapter × cognitive × question type)?
 *
 * <p>Surfaces the exact gap per cell so the FE can show actionable messages
 * like "Bank thiếu Chương 2 – Vận dụng cao: cần 3 câu, hiện có 0".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateBankCoverageRequest {

  @NotNull(message = "examMatrixId is required")
  private UUID examMatrixId;

  @NotEmpty(message = "Pick at least one question bank")
  private List<UUID> questionBankIds;
}
