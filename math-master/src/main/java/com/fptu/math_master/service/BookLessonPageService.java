package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.BulkPageMappingRequest;
import com.fptu.math_master.dto.request.BulkSeriesPageMappingRequest;
import com.fptu.math_master.dto.response.BookLessonPageResponse;
import java.util.List;
import java.util.UUID;

/**
 * Lesson→page mapping management for a given Book. The mapping is what tells Python which
 * pages to OCR and under which lesson_id to store the result.
 */
public interface BookLessonPageService {

  /**
   * Replace the entire mapping for {@code bookId} with the supplied list. Validates page ranges
   * fit the book's OCR window, no duplicate lessons, every lesson belongs to the book's
   * curriculum, and that lessons sorted by chapter/lesson order_index do not page-jump backwards
   * (a single shared page between consecutive lessons is allowed).
   */
  List<BookLessonPageResponse> bulkUpsert(
      UUID bookId, BulkPageMappingRequest request, UUID actorId);

  /** Returns the current mapping for a book, ordered by chapter/lesson order. */
  List<BookLessonPageResponse> listForBook(UUID bookId);

  /** Returns series mapping (lesson -> assigned book + page range). */
  List<BookLessonPageResponse> listForSeriesByBook(UUID bookId);

  /** Replace series mapping and sync per-book mapping snapshots. */
  List<BookLessonPageResponse> bulkUpsertSeriesByBook(
      UUID bookId, BulkSeriesPageMappingRequest request, UUID actorId);
}
