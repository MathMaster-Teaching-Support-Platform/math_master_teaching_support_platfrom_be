package com.fptu.math_master.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-cell coverage report for a matrix vs a chosen set of question banks.
 *
 * <p>{@code ok} is true when every cell's available count meets its required
 * count. The {@code cells} list is exhaustive (one entry per matrix cell with
 * non-zero requirement); the FE filters to {@code available < required} when
 * rendering gap messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCoverageResponse {

  private boolean ok;
  private List<CoverageCell> cells;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CoverageCell {
    private UUID chapterId;
    private String chapterTitle;
    private String cognitiveLevel;
    private String questionType;
    private int required;
    private long available;

    public boolean isShortage() {
      return available < required;
    }
  }
}
