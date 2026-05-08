package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateBookRequest;
import com.fptu.math_master.dto.request.UpdateBookRequest;
import com.fptu.math_master.dto.response.BookPdfPreviewUrlResponse;
import com.fptu.math_master.dto.response.BookProgressResponse;
import com.fptu.math_master.dto.response.BookResponse;
import com.fptu.math_master.dto.response.OcrTriggerResponse;
import com.fptu.math_master.enums.BookStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {

  BookResponse create(CreateBookRequest request, UUID actorId);

  BookResponse getById(UUID id);

  Page<BookResponse> search(
      UUID schoolGradeId,
      UUID subjectId,
      UUID curriculumId,
      UUID chapterId,
      UUID lessonId,
      BookStatus status,
      Pageable pageable);

  BookResponse update(UUID id, UpdateBookRequest request, UUID actorId);

  /** Persists a PDF reference (e.g., MinIO key or URL) after upload. */
  BookResponse setPdfPath(UUID id, String pdfPath, UUID actorId);

  /** Presigned GET URL for admin PDF preview (same bucket/key as upload). */
  BookPdfPreviewUrlResponse getPdfPreviewUrl(UUID id);

  void delete(UUID id, UUID actorId);

  /** Computes per-page/lesson/book verification progress by querying the Python crawler. */
  BookProgressResponse getProgress(UUID id);

  /**
   * Validates the page mapping is complete and triggers OCR on the Python service. Transitions
   * book status DRAFT/MAPPING/READY/OCR_DONE/OCR_FAILED → OCR_RUNNING.
   */
  OcrTriggerResponse triggerOcr(UUID id, UUID actorId);

  /** Stops an in-flight OCR job (best-effort) and returns the book to READY. */
  BookResponse cancelOcr(UUID id, UUID actorId);

  /** Refreshes book.verified flag from Python's per-page verification state. */
  void refreshVerificationStatus(UUID id, UUID actorId);
}
