package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One PDF page worth of OCR content for a specific (book, lesson). Stored in MongoDB; the
 * canonical lesson_id and book_id link back to Postgres. {@code verified} is the per-page
 * verification flag — rolls up to lesson and then book level.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonPageResponse {
  private String id;
  private UUID bookId;
  private UUID lessonId;
  private Integer pageNumber;
  private List<ContentBlockDto> contentBlocks;
  private String rawImageUrl;
  private Double ocrConfidence;
  private String ocrSource;
  private boolean verified;
  private UUID verifiedBy;
  private Instant verifiedAt;
  private Instant updatedAt;
}
