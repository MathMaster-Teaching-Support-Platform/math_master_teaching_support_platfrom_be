package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedFileResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.service.LessonSlideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @GetMapping("/lessons/{lessonId}/generated")
  @Operation(
      summary = "List published generated slides by lesson",
      description = "Public endpoint for students to browse downloadable generated slides.")
  public ApiResponse<List<LessonSlideGeneratedFileResponse>> listPublicGeneratedSlides(
      @PathVariable UUID lessonId) {
    return ApiResponse.<List<LessonSlideGeneratedFileResponse>>builder()
        .result(lessonSlideService.getPublicGeneratedSlidesByLesson(lessonId))
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

  private String contentDisposition(String fileName) {
    return "attachment; filename*=UTF-8''"
        + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
