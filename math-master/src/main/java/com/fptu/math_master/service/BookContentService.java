package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.UpdateLessonPageRequest;
import com.fptu.math_master.dto.response.LessonContentResponse;
import com.fptu.math_master.dto.response.LessonPageResponse;
import java.util.List;
import java.util.UUID;

/**
 * Read/write façade over OCR'd page content. The Java BE never touches MongoDB directly — every
 * call here delegates to {@link PythonCrawlerClient}. The service adds Postgres-side enrichment
 * (lesson titles, book titles) and triggers verification rollup when a page's verified flag
 * changes.
 */
public interface BookContentService {

  /** All pages for a single (book, lesson), in source-PDF order. */
  LessonContentResponse getLessonContentForBook(UUID bookId, UUID lessonId);

  /**
   * All pages for a lesson across every book that maps to it. Used by the Gemini prompt builder
   * when it doesn't care which textbook the content came from.
   */
  LessonContentResponse getLessonContent(UUID lessonId);

  /** Single page lookup. */
  LessonPageResponse getPage(UUID bookId, UUID lessonId, int pageNumber);

  /** All pages for a book, grouped per lesson. */
  List<LessonContentResponse> getAllLessonsForBook(UUID bookId);

  /**
   * Patch one OCR'd page's content blocks and/or verified flag. If the patch toggles the verified
   * flag, the parent book's cached {@code verified} status is refreshed afterwards.
   */
  LessonPageResponse updatePage(
      UUID bookId, UUID lessonId, int pageNumber, UpdateLessonPageRequest request, UUID actorId);
}
