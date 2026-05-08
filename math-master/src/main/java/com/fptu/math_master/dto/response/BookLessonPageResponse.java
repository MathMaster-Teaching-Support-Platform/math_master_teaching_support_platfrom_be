package com.fptu.math_master.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row in the page-mapping; includes denormalized lesson info for the FE wizard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookLessonPageResponse {
  private UUID id;
  private UUID bookSeriesId;
  private UUID bookId;
  private String bookTitle;
  private UUID lessonId;
  private String lessonTitle;
  private UUID chapterId;
  private String chapterTitle;
  private Integer chapterOrderIndex;
  private Integer lessonOrderIndex;
  private Integer pageStart;
  private Integer pageEnd;
  /** Aggregated from Mongo: total OCR'd pages in this range. */
  private Integer ocrPageCount;
  /** Aggregated from Mongo: pages flagged verified=true. */
  private Integer verifiedPageCount;
}
