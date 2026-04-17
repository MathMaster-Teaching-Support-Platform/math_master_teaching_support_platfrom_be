package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedFileResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.service.LessonSlideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lesson-slides/public")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Lesson Slides Public", description = "Public endpoints for student lesson slide viewing")
public class LessonSlidePublicController {

  LessonSlideService lessonSlideService;

  @GetMapping("/lessons/{lessonId}")
  @Operation(
      summary = "Get published lesson slide content",
      description = "Public endpoint for students to view published lesson slide content.")
  public ApiResponse<LessonResponse> getPublishedLessonSlide(@PathVariable UUID lessonId) {
    log.info("GET /lesson-slides/public/lessons/{}", lessonId);
    return ApiResponse.<LessonResponse>builder()
        .result(lessonSlideService.getPublishedLessonSlide(lessonId))
        .build();
  }

    @GetMapping("/generated")
    @Operation(
            summary = "List all public generated slides",
            description = "Public endpoint for students to browse all published generated slides.")
      public ApiResponse<Page<LessonSlideGeneratedFileResponse>> listAllPublicGeneratedSlides(
        @RequestParam(required = false) UUID lessonId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String direction) {
      Sort.Direction sortDirection =
        direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
      Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

      return ApiResponse.<Page<LessonSlideGeneratedFileResponse>>builder()
        .result(lessonSlideService.getAllPublicGeneratedSlides(lessonId, keyword, pageable))
                .build();
    }

  @GetMapping("/lessons/{lessonId}/generated")
  @Operation(
      summary = "List published generated slides by lesson",
      description = "Public endpoint for students to browse downloadable generated slides.")
  public ApiResponse<Page<LessonSlideGeneratedFileResponse>> listPublicGeneratedSlides(
      @PathVariable UUID lessonId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String direction) {
    Sort.Direction sortDirection =
      direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    return ApiResponse.<Page<LessonSlideGeneratedFileResponse>>builder()
      .result(lessonSlideService.getPublicGeneratedSlidesByLesson(lessonId, keyword, pageable))
        .build();
  }

  @GetMapping("/generated/{generatedFileId}/download")
  @Operation(
      summary = "Download a published generated slide",
      description = "Public endpoint for students to download a published PPTX file.")
  public ResponseEntity<byte[]> downloadPublicGeneratedSlide(@PathVariable UUID generatedFileId) {
    LessonSlideService.BinaryFileData fileData =
        lessonSlideService.downloadPublicGeneratedSlide(generatedFileId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

      @GetMapping("/generated/{generatedFileId}/preview-pdf")
      @Operation(
        summary = "Convert published generated PPTX to PDF and preview inline",
        description = "Public endpoint for students to preview generated slide as PDF in browser.")
      public ResponseEntity<byte[]> previewPublicGeneratedSlidePdf(@PathVariable UUID generatedFileId) {
      LessonSlideService.BinaryFileData fileData =
        lessonSlideService.getPublicGeneratedSlidePreviewPdf(generatedFileId);
      return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
      }

  @GetMapping("/generated/{generatedFileId}/preview-url")
  @Operation(
      summary = "Get pre-signed preview URL for a published generated slide",
      description = "Public endpoint for students to preview a published PPTX file via viewer.")
  public ApiResponse<String> getPublicGeneratedSlidePreviewUrl(@PathVariable UUID generatedFileId) {
    return ApiResponse.<String>builder()
        .result(lessonSlideService.getPublicGeneratedSlidePreviewUrl(generatedFileId))
        .build();
  }

  @GetMapping("/generated/{generatedFileId}/thumbnail-image")
  @Operation(
      summary = "Get thumbnail image of a published generated slide",
      description = "Public endpoint for students to view generated slide thumbnail image.")
  public ResponseEntity<byte[]> getPublicGeneratedSlideThumbnailImage(@PathVariable UUID generatedFileId) {
    LessonSlideService.BinaryFileData fileData =
        lessonSlideService.getPublicGeneratedSlideThumbnailImage(generatedFileId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  private String contentDisposition(String fileName) {
    return "attachment; filename*=UTF-8''"
        + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String inlineDisposition(String fileName) {
    return "inline; filename*=UTF-8''"
        + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
