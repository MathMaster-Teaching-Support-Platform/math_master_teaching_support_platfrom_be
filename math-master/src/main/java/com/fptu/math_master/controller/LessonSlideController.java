package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.LessonSlideConfirmContentRequest;
import com.fptu.math_master.dto.request.SlidePreviewRenderRequest;
import com.fptu.math_master.dto.request.LessonSlideGenerateContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxFromJsonRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedFileResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedContentResponse;
import com.fptu.math_master.dto.response.SlideTemplateResponse;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.service.LessonSlideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/lesson-slides")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(
    name = "Lesson Slides",
    description = "Teacher workflow for AI lesson content and PPTX generation")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TEACHER')")
public class LessonSlideController {

  LessonSlideService lessonSlideService;

  @PostMapping("/generate-content")
  @Operation(summary = "Generate lesson content draft with AI")
  public ApiResponse<LessonSlideGeneratedContentResponse> generateContentDraft(
      @Valid @RequestBody LessonSlideGenerateContentRequest request) {
    return ApiResponse.<LessonSlideGeneratedContentResponse>builder()
        .message("Lesson content draft generated")
        .result(lessonSlideService.generateLessonContentDraft(request))
        .build();
  }

  @PutMapping("/{lessonId}/confirm-content")
  @Operation(summary = "Confirm and save edited lesson content")
  public ApiResponse<LessonResponse> confirmLessonContent(
      @PathVariable UUID lessonId, @Valid @RequestBody LessonSlideConfirmContentRequest request) {
    return ApiResponse.<LessonResponse>builder()
        .message("Lesson content confirmed")
        .result(lessonSlideService.confirmLessonContent(lessonId, request))
        .build();
  }

  @GetMapping("/lessons/{lessonId}")
  @Operation(summary = "Get lesson slide draft/detail for teacher")
  public ApiResponse<LessonResponse> getLessonSlide(@PathVariable UUID lessonId) {
    return ApiResponse.<LessonResponse>builder()
        .result(lessonSlideService.getLessonSlide(lessonId))
        .build();
  }

  @GetMapping("/lessons")
  @Operation(summary = "List lesson slides by status")
  public ApiResponse<List<LessonResponse>> getLessonSlides(
      @RequestParam(value = "status", defaultValue = "DRAFT") LessonStatus status) {
    return ApiResponse.<List<LessonResponse>>builder()
        .result(lessonSlideService.getLessonSlides(status))
        .build();
  }

  @PatchMapping("/lessons/{lessonId}/publish")
  @Operation(summary = "Publish lesson slide")
  public ApiResponse<LessonResponse> publishLessonSlide(@PathVariable UUID lessonId) {
    return ApiResponse.<LessonResponse>builder()
        .message("Lesson slide published successfully")
        .result(lessonSlideService.publishLessonSlide(lessonId))
        .build();
  }

  @PatchMapping("/lessons/{lessonId}/unpublish")
  @Operation(summary = "Unpublish lesson slide")
  public ApiResponse<LessonResponse> unpublishLessonSlide(@PathVariable UUID lessonId) {
    return ApiResponse.<LessonResponse>builder()
        .message("Lesson slide moved back to draft")
        .result(lessonSlideService.unpublishLessonSlide(lessonId))
        .build();
  }

  @PostMapping(value = "/templates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload slide template (PPTX)")
  public ApiResponse<SlideTemplateResponse> uploadTemplate(
      @RequestParam("name") String name,
      @RequestParam(value = "description", required = false) String description,
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "previewImage", required = false) MultipartFile previewImage) {
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template uploaded successfully")
      .result(lessonSlideService.uploadTemplate(name, description, file, previewImage))
        .build();
  }

  @PutMapping(value = "/templates/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Update slide template metadata or file")
  public ApiResponse<SlideTemplateResponse> updateTemplate(
      @PathVariable UUID templateId,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "active", required = false) Boolean active,
      @RequestParam(value = "file", required = false) MultipartFile file,
      @RequestParam(value = "previewImage", required = false) MultipartFile previewImage) {
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template updated successfully")
        .result(
            lessonSlideService.updateTemplate(
                templateId, name, description, active, file, previewImage))
        .build();
  }

  @GetMapping("/templates")
  @Operation(summary = "List slide templates")
  public ApiResponse<List<SlideTemplateResponse>> listTemplates(
      @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
    return ApiResponse.<List<SlideTemplateResponse>>builder()
        .result(lessonSlideService.getTemplates(activeOnly))
        .build();
  }

  @GetMapping("/templates/{templateId}/download")
  @Operation(summary = "Download original template file")
  public ResponseEntity<byte[]> downloadTemplate(@PathVariable UUID templateId) {
    LessonSlideService.BinaryFileData fileData = lessonSlideService.downloadTemplate(templateId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @GetMapping("/templates/{templateId}/preview-image")
  @Operation(summary = "Download template preview image")
  public ResponseEntity<byte[]> downloadTemplatePreviewImage(@PathVariable UUID templateId) {
    LessonSlideService.BinaryFileData fileData =
        lessonSlideService.downloadTemplatePreviewImage(templateId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @PostMapping("/generate-pptx")
  @Operation(summary = "Generate PPTX from lesson content and selected template")
  public ResponseEntity<byte[]> generatePptx(
      @Valid @RequestBody LessonSlideGeneratePptxRequest request) {
    LessonSlideService.BinaryFileData fileData = lessonSlideService.generatePptx(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @PostMapping("/generate-pptx-from-json")
  @Operation(summary = "Generate PPTX from confirmed JSON slide structure")
  public ResponseEntity<byte[]> generatePptxFromJson(
      @Valid @RequestBody LessonSlideGeneratePptxFromJsonRequest request) {
    LessonSlideService.BinaryFileData fileData = lessonSlideService.generatePptxFromJson(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @GetMapping("/generated")
  @Operation(summary = "List generated slide files of current teacher")
  public ApiResponse<List<LessonSlideGeneratedFileResponse>> getGeneratedSlides(
      @RequestParam(value = "gradeId", required = false) UUID gradeId,
      @RequestParam(value = "subjectId", required = false) UUID subjectId,
      @RequestParam(value = "chapterId", required = false) UUID chapterId,
      @RequestParam(value = "lessonId", required = false) UUID lessonId,
      @RequestParam(value = "keyword", required = false) String keyword) {
    return ApiResponse.<List<LessonSlideGeneratedFileResponse>>builder()
        .result(
            lessonSlideService.getMyGeneratedSlides(
                gradeId, subjectId, chapterId, lessonId, keyword))
        .build();
  }

  @GetMapping("/generated/{generatedFileId}/download")
  @Operation(summary = "Download a previously generated slide file")
  public ResponseEntity<byte[]> downloadGeneratedSlide(@PathVariable UUID generatedFileId) {
    LessonSlideService.BinaryFileData fileData =
        lessonSlideService.downloadGeneratedSlide(generatedFileId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @GetMapping("/generated/{generatedFileId}/preview-pdf")
  @Operation(summary = "Convert generated PPTX to PDF and preview inline")
  public ResponseEntity<byte[]> previewGeneratedSlidePdf(@PathVariable UUID generatedFileId) {
    LessonSlideService.BinaryFileData fileData =
        lessonSlideService.getGeneratedSlidePreviewPdf(generatedFileId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

        @GetMapping("/generated/{generatedFileId}/preview-url")
        @Operation(summary = "Get pre-signed preview URL for a generated slide")
        public ApiResponse<String> getGeneratedSlidePreviewUrl(@PathVariable UUID generatedFileId) {
          return ApiResponse.<String>builder()
          .result(lessonSlideService.getGeneratedSlidePreviewUrl(generatedFileId))
          .build();
        }

  @PatchMapping("/generated/{generatedFileId}/publish")
  @Operation(summary = "Publish a generated slide file for student access")
  public ApiResponse<LessonSlideGeneratedFileResponse> publishGeneratedSlide(
      @PathVariable UUID generatedFileId) {
    return ApiResponse.<LessonSlideGeneratedFileResponse>builder()
        .message("Generated slide published successfully")
        .result(lessonSlideService.publishGeneratedSlide(generatedFileId))
        .build();
  }

  @PatchMapping("/generated/{generatedFileId}/unpublish")
  @Operation(summary = "Unpublish a generated slide file")
  public ApiResponse<LessonSlideGeneratedFileResponse> unpublishGeneratedSlide(
      @PathVariable UUID generatedFileId) {
    return ApiResponse.<LessonSlideGeneratedFileResponse>builder()
        .message("Generated slide moved back to private")
        .result(lessonSlideService.unpublishGeneratedSlide(generatedFileId))
        .build();
  }

  @PatchMapping(value = "/generated/{generatedFileId}/metadata", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Update generated slide metadata (name, thumbnail file)")
  public ApiResponse<LessonSlideGeneratedFileResponse> updateGeneratedSlideMetadata(
      @PathVariable UUID generatedFileId,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail) {
    return ApiResponse.<LessonSlideGeneratedFileResponse>builder()
        .message("Generated slide metadata updated successfully")
        .result(lessonSlideService.updateGeneratedSlideMetadata(generatedFileId, name, thumbnail))
        .build();
  }

  @GetMapping("/generated/{generatedFileId}/thumbnail-image")
  @Operation(summary = "Get generated slide thumbnail image")
  public ResponseEntity<byte[]> getGeneratedSlideThumbnailImage(@PathVariable UUID generatedFileId) {
    LessonSlideService.BinaryFileData fileData =
        lessonSlideService.getGeneratedSlideThumbnailImage(generatedFileId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @DeleteMapping("/generated/{generatedFileId}")
  @Operation(summary = "Delete a generated slide file (soft-delete + removes MinIO object)")
  public ApiResponse<Void> deleteGeneratedSlide(@PathVariable UUID generatedFileId) {
    lessonSlideService.deleteGeneratedSlide(generatedFileId);
    return ApiResponse.<Void>builder()
        .message("Generated slide deleted successfully")
        .build();
  }

  @PostMapping("/render-slide-preview")
  @Operation(summary = "Re-render a single slide heading+content via QuickLaTeX and return the image URL")
  public ApiResponse<String> renderSlidePreview(
      @RequestBody SlidePreviewRenderRequest request) {
    return ApiResponse.<String>builder()
        .result(lessonSlideService.renderSlidePreview(request.getHeading(), request.getContent()))
        .build();
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
