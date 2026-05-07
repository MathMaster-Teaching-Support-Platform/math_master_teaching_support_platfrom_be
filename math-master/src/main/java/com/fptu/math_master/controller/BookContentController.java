package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.UpdateLessonPageRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonContentResponse;
import com.fptu.math_master.dto.response.LessonPageResponse;
import com.fptu.math_master.service.BookContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read/write access to OCR'd page content. Dual mount: book-scoped routes for the verify wizard,
 * lesson-scoped routes for the Gemini prompt builder which doesn't care about the source book.
 */
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Book Content", description = "OCR'd page content + per-page verify")
@SecurityRequirement(name = "bearerAuth")
public class BookContentController {

  BookContentService bookContentService;

  // ---------------------------------------------------------------------------
  // Book-scoped (used by verify wizard)
  // ---------------------------------------------------------------------------

  @Operation(summary = "All pages for a (book, lesson) in source-PDF order")
  @GetMapping("/books/{bookId}/lessons/{lessonId}/content")
  public ApiResponse<LessonContentResponse> getLessonContentForBook(
      @PathVariable UUID bookId, @PathVariable UUID lessonId) {
    return ApiResponse.<LessonContentResponse>builder()
        .result(bookContentService.getLessonContentForBook(bookId, lessonId))
        .build();
  }

  @Operation(summary = "All lessons for a book, each with its OCR'd pages")
  @GetMapping("/books/{bookId}/content")
  public ApiResponse<List<LessonContentResponse>> getAllLessonsForBook(
      @PathVariable UUID bookId) {
    return ApiResponse.<List<LessonContentResponse>>builder()
        .result(bookContentService.getAllLessonsForBook(bookId))
        .build();
  }

  @Operation(summary = "Single OCR'd page")
  @GetMapping("/books/{bookId}/lessons/{lessonId}/pages/{pageNumber}")
  public ApiResponse<LessonPageResponse> getPage(
      @PathVariable UUID bookId,
      @PathVariable UUID lessonId,
      @PathVariable int pageNumber) {
    return ApiResponse.<LessonPageResponse>builder()
        .result(bookContentService.getPage(bookId, lessonId, pageNumber))
        .build();
  }

  @Operation(summary = "Edit one OCR'd page (content blocks and/or verified flag)")
  @PatchMapping("/books/{bookId}/lessons/{lessonId}/pages/{pageNumber}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<LessonPageResponse> updatePage(
      @PathVariable UUID bookId,
      @PathVariable UUID lessonId,
      @PathVariable int pageNumber,
      @Valid @RequestBody UpdateLessonPageRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    UUID actorId = UUID.fromString(jwt.getSubject());
    return ApiResponse.<LessonPageResponse>builder()
        .result(bookContentService.updatePage(bookId, lessonId, pageNumber, request, actorId))
        .build();
  }

  // ---------------------------------------------------------------------------
  // Lesson-scoped (used by Gemini prompt builder; book-agnostic)
  // ---------------------------------------------------------------------------

  @Operation(
      summary = "All OCR'd pages for a lesson across every book that maps to it",
      description = "Used by downstream consumers (e.g., the Gemini prompt builder) that don't "
          + "care which textbook the content originated from.")
  @GetMapping("/lessons/{lessonId}/content")
  public ApiResponse<LessonContentResponse> getLessonContent(@PathVariable UUID lessonId) {
    return ApiResponse.<LessonContentResponse>builder()
        .result(bookContentService.getLessonContent(lessonId))
        .build();
  }
}
