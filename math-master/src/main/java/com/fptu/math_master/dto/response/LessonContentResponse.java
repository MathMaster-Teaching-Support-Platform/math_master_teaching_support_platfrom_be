package com.fptu.math_master.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * All OCR'd content for one Lesson, optionally scoped to a single Book. Returned by
 * {@code GET /v1/lessons/{lessonId}/content} — the entry point that other services (e.g., the
 * Gemini prompt builder) use to feed validated lesson content into prompts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonContentResponse {
  private UUID lessonId;
  private String lessonTitle;
  private UUID bookId;
  private String bookTitle;
  /** Pages in source-PDF order. Empty if OCR has not yet run for this (book, lesson). */
  private List<LessonPageResponse> pages;
  /** True iff every page in {@code pages} has {@code verified=true}. */
  private boolean lessonVerified;
}
