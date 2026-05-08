package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.BookStatus;
import java.time.Instant;
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
public class BookProgressResponse {

  private UUID bookId;
  private BookStatus status;
  private boolean bookVerified;

  private int totalLessons;
  private int verifiedLessons;
  private int totalPages;
  private int verifiedPages;

  /**
   * Live OCR pipeline fields (Mongo/Python) — populated while {@link #status} is {@code
   * OCR_RUNNING}; otherwise typically null.
   */
  private String ocrRunnerStatus;

  private Integer ocrJobProgressPercent;
  private String ocrJobPhase;
  private Integer ocrJobProcessedPages;
  private Integer ocrJobTotalPages;
  private String ocrJobErrorMessage;

  /**
   * {@code false} when Postgres says OCR is running but the Java BE could not reach the Python
   * crawler for {@code /ocr-status}. {@code null} when not applicable.
   */
  private Boolean ocrCrawlerReachable;

  /** True when live fields below were filled from Postgres snapshot (crawler unreachable). */
  private Boolean ocrProgressFromCache;

  /** When the snapshot was last written (successful crawl poll). */
  private Instant ocrProgressCachedAt;

  private List<LessonProgress> lessons;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LessonProgress {
    private UUID lessonId;
    private String lessonTitle;
    private Integer pageStart;
    private Integer pageEnd;
    private int totalPages;
    private int verifiedPages;
    private boolean lessonVerified;
  }
}
