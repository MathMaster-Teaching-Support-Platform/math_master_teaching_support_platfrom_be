package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.*;

/**
 * One row in the exam-matrix table (một dạng bài).
 * <p>
 * Example row from the image:
 * <pre>
 * Dạng bài: "Cực trị của HS"  | ref: 4,5,39,46 | NB:1 | TH:1 | VD:1 | VDC:1 | Total:4
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixRowResponse {

  private UUID rowId;
  private UUID chapterId;
  private UUID lessonId;
  private UUID templateId;

  /** Display name for the dạng bài column. */
  private String questionTypeName;

  /** Reference question numbers from illustrative exam paper, e.g. "3,30". */
  private String referenceQuestions;

  private Integer orderIndex;

  /** Cells keyed by cognitive-level label (NB, TH, VD, VDC). */
  private List<MatrixCellResponse> cells;

  /** question count per cognitive-level label — convenient for the UI table. */
  private Map<String, Integer> countByCognitive;

  private int rowTotalQuestions;
  private BigDecimal rowTotalPoints;
}
