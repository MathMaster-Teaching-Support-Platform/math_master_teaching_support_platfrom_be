package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All rows that belong to one Chapter, grouped for the matrix table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixChapterGroupResponse {

  private UUID chapterId;
  private String chapterTitle;
  private Integer chapterOrderIndex;

  /** Rows (dạng bài) inside this chapter. */
  private List<MatrixRowResponse> rows;

  /** Aggregated question count per cognitive-level label for the chapter. */
  private Map<String, Integer> totalByCognitive;

  private int chapterTotalQuestions;
  private BigDecimal chapterTotalPoints;
}
