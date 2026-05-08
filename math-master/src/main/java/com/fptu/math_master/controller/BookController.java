package com.fptu.math_master.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fptu.math_master.dto.request.BulkPageMappingRequest;
import com.fptu.math_master.dto.request.CreateBookRequest;
import com.fptu.math_master.dto.request.UpdateBookRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.BookLessonPageResponse;
import com.fptu.math_master.dto.response.BookPdfPreviewUrlResponse;
import com.fptu.math_master.dto.response.BookProgressResponse;
import com.fptu.math_master.dto.response.BookResponse;
import com.fptu.math_master.dto.response.OcrTriggerResponse;
import com.fptu.math_master.enums.BookStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.BookLessonPageService;
import com.fptu.math_master.service.BookService;
import com.fptu.math_master.service.UploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/books")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Books", description = "Textbook lifecycle: create, page mapping, OCR trigger, verify")
@SecurityRequirement(name = "bearerAuth")
public class BookController {

  BookService bookService;
  BookLessonPageService bookLessonPageService;
  UploadService uploadService;

  // ---------------------------------------------------------------------------
  // Book CRUD
  // ---------------------------------------------------------------------------

  @Operation(summary = "Create a new book")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookResponse> create(
      @Valid @RequestBody CreateBookRequest request, @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    return ApiResponse.<BookResponse>builder().result(bookService.create(request, actorId)).build();
  }

  @Operation(summary = "Get book by ID")
  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookResponse> getById(@PathVariable UUID id) {
    return ApiResponse.<BookResponse>builder().result(bookService.getById(id)).build();
  }

  @Operation(
      summary = "Presigned URL to preview the uploaded PDF",
      description =
          "Returns a short-lived MinIO GET URL suitable for iframe/embed in the admin wizard.")
  @GetMapping("/{id}/pdf-preview-url")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookPdfPreviewUrlResponse> getPdfPreviewUrl(@PathVariable UUID id) {
    return ApiResponse.<BookPdfPreviewUrlResponse>builder()
        .result(bookService.getPdfPreviewUrl(id))
        .build();
  }

  @Operation(summary = "Search books with filters")
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Page<BookResponse>> search(
      @RequestParam(required = false) UUID schoolGradeId,
      @RequestParam(required = false) UUID subjectId,
      @RequestParam(required = false) UUID curriculumId,
      @RequestParam(required = false) UUID chapterId,
      @RequestParam(required = false) UUID lessonId,
      @RequestParam(required = false) BookStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<BookResponse>>builder()
        .result(
            bookService.search(
                schoolGradeId, subjectId, curriculumId, chapterId, lessonId, status, pageable))
        .build();
  }

  @Operation(summary = "Update book metadata or OCR window")
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateBookRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    return ApiResponse.<BookResponse>builder()
        .result(bookService.update(id, request, actorId))
        .build();
  }

  @Operation(
      summary = "Set the PDF path/key after upload",
      description = "Persists the MinIO object key (or URL) so the OCR trigger has something to "
          + "hand off to Python.")
  @PatchMapping("/{id}/pdf-path")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookResponse> setPdfPath(
      @PathVariable UUID id,
      @RequestBody Map<String, String> body,
      @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    String pdfPath = body == null ? null : body.get("pdfPath");
    return ApiResponse.<BookResponse>builder()
        .result(bookService.setPdfPath(id, pdfPath, actorId))
        .build();
  }

  @Operation(summary = "Upload book PDF and persist the uploaded object key")
  @PostMapping(value = "/{id}/pdf-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookResponse> uploadPdf(
      @PathVariable UUID id,
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal Jwt jwt) {
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    UUID actorId = UUID.fromString(jwt.getSubject());
    String objectKey = uploadService.uploadFile(file, "books/pdfs");
    return ApiResponse.<BookResponse>builder()
        .result(bookService.setPdfPath(id, objectKey, actorId))
        .build();
  }

  @Operation(summary = "Soft-delete a book and drop its OCR'd pages from Mongo")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    bookService.delete(id, actorId);
    return ApiResponse.<Void>builder().message("Book deleted").build();
  }

  // ---------------------------------------------------------------------------
  // Page mapping
  // ---------------------------------------------------------------------------

  @Operation(
      summary = "Get the current lesson→page mapping for a book",
      description = "Includes denormalized lesson/chapter info plus per-lesson OCR/verify counts.")
  @GetMapping("/{id}/page-mapping")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<List<BookLessonPageResponse>> getPageMapping(@PathVariable UUID id) {
    return ApiResponse.<List<BookLessonPageResponse>>builder()
        .result(bookLessonPageService.listForBook(id))
        .build();
  }

  @Operation(
      summary = "Replace the lesson→page mapping for a book",
      description = "Validates page ranges, lesson curriculum membership, and ordering.")
  @PutMapping("/{id}/page-mapping")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<List<BookLessonPageResponse>> bulkUpsertPageMapping(
      @PathVariable UUID id,
      @Valid @RequestBody BulkPageMappingRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    return ApiResponse.<List<BookLessonPageResponse>>builder()
        .result(bookLessonPageService.bulkUpsert(id, request, actorId))
        .build();
  }

  // ---------------------------------------------------------------------------
  // OCR & verification
  // ---------------------------------------------------------------------------

  @Operation(
      summary = "Trigger OCR on the Python crawler",
      description = "Async — poll the book status to track progress.")
  @PostMapping("/{id}/ocr")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<OcrTriggerResponse> triggerOcr(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    return ApiResponse.<OcrTriggerResponse>builder()
        .result(bookService.triggerOcr(id, actorId))
        .build();
  }

  @Operation(
      summary = "Cancel an in-flight OCR job",
      description =
          "Signals the Python crawler to stop cooperatively and sets book status back to READY.")
  @PostMapping("/{id}/ocr/cancel")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookResponse> cancelOcr(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    return ApiResponse.<BookResponse>builder()
        .result(bookService.cancelOcr(id, actorId))
        .build();
  }

  @Operation(
      summary = "Per-page/lesson/book verification progress",
      description = "Aggregates the page-level verified flag from Mongo into lesson and book "
          + "rollups for the verify UI.")
  @GetMapping("/{id}/progress")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<BookProgressResponse> getProgress(@PathVariable UUID id) {
    return ApiResponse.<BookProgressResponse>builder()
        .result(bookService.getProgress(id))
        .build();
  }

  @Operation(
      summary = "Refresh the cached book.verified flag from Mongo",
      description = "Cheap fast-sync — calls Python's /verification endpoint.")
  @PostMapping("/{id}/refresh-verification")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> refreshVerification(
      @PathVariable @Parameter(description = "Book ID") UUID id,
      @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    bookService.refreshVerificationStatus(id, actorId);
    return ApiResponse.<Void>builder().message("Verification status refreshed").build();
  }
}
