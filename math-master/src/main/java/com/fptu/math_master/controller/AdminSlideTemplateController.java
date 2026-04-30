package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.SlideTemplateResponse;
import com.fptu.math_master.service.LessonSlideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/slide-templates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin — Slide Templates", description = "Admin management of PPTX slide templates")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSlideTemplateController {

  LessonSlideService lessonSlideService;

  // ─────────────────────────────────────────────────────────────
  // List & Get
  // ─────────────────────────────────────────────────────────────

  @GetMapping
  @Operation(
      summary = "List all slide templates",
      description =
          "Returns all non-deleted templates. Use `activeOnly=false` to include inactive ones.")
  public ApiResponse<List<SlideTemplateResponse>> listTemplates(
      @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
    log.info("Admin listing slide templates, activeOnly={}", activeOnly);
    return ApiResponse.<List<SlideTemplateResponse>>builder()
        .result(lessonSlideService.getTemplates(activeOnly))
        .build();
  }

  @GetMapping("/{templateId}")
  @Operation(summary = "Get a single slide template by ID")
  public ApiResponse<SlideTemplateResponse> getTemplate(@PathVariable UUID templateId) {
    log.info("Admin get slide template id={}", templateId);
    return ApiResponse.<SlideTemplateResponse>builder()
        .result(lessonSlideService.getTemplateById(templateId))
        .build();
  }

  // ─────────────────────────────────────────────────────────────
  // Upload & Update
  // ─────────────────────────────────────────────────────────────

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Upload a new PPTX slide template",
      description =
          "Upload a `.pptx` file as a new slide template. An optional preview image (PNG/JPG/WEBP) can be included.")
  public ApiResponse<SlideTemplateResponse> uploadTemplate(
      @RequestParam("name") String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "previewImage", required = false) MultipartFile previewImage) {
    log.info("Admin uploading slide template name={}", name);
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template uploaded successfully")
        .result(lessonSlideService.uploadTemplate(name, description, file, previewImage))
        .build();
  }

  @PutMapping(value = "/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Update a slide template",
      description =
          "Update metadata (name, description, active) and/or replace the PPTX file / preview image. "
              + "All fields are optional — only provided fields are changed.")
  public ApiResponse<SlideTemplateResponse> updateTemplate(
      @PathVariable UUID templateId,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "active", required = false) Boolean active,
      @RequestParam(value = "file", required = false) MultipartFile file,
      @RequestParam(value = "previewImage", required = false) MultipartFile previewImage) {
    log.info("Admin updating slide template id={}", templateId);
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template updated successfully")
        .result(
            lessonSlideService.updateTemplate(
                templateId, name, description, active, file, previewImage))
        .build();
  }

  // ─────────────────────────────────────────────────────────────
  // Activate / Deactivate shortcuts
  // ─────────────────────────────────────────────────────────────

  @PatchMapping("/{templateId}/activate")
  @Operation(summary = "Activate a template (makes it available to teachers)")
  public ApiResponse<SlideTemplateResponse> activateTemplate(@PathVariable UUID templateId) {
    log.info("Admin activating slide template id={}", templateId);
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template activated")
        .result(lessonSlideService.updateTemplate(templateId, null, null, true, null, null))
        .build();
  }

  @PatchMapping("/{templateId}/deactivate")
  @Operation(summary = "Deactivate a template (hides it from teachers)")
  public ApiResponse<SlideTemplateResponse> deactivateTemplate(@PathVariable UUID templateId) {
    log.info("Admin deactivating slide template id={}", templateId);
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template deactivated")
        .result(lessonSlideService.updateTemplate(templateId, null, null, false, null, null))
        .build();
  }

  // ─────────────────────────────────────────────────────────────
  // Delete
  // ─────────────────────────────────────────────────────────────

  @DeleteMapping("/{templateId}")
  @Operation(
      summary = "Delete a slide template (soft delete)",
      description =
          "Marks the template as deleted. The MinIO file is retained for existing generated slides that reference it.")
  public ApiResponse<Void> deleteTemplate(@PathVariable UUID templateId) {
    log.info("Admin deleting slide template id={}", templateId);
    lessonSlideService.deleteTemplate(templateId);
    return ApiResponse.<Void>builder().message("Template deleted successfully").build();
  }

  // ─────────────────────────────────────────────────────────────
  // Download & Preview
  // ─────────────────────────────────────────────────────────────

  @GetMapping("/{templateId}/download")
  @Operation(summary = "Download the original PPTX template file")
  public ResponseEntity<byte[]> downloadTemplate(@PathVariable UUID templateId) {
    LessonSlideService.BinaryFileData fileData = lessonSlideService.downloadTemplate(templateId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(fileData.fileName()))
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  @GetMapping("/{templateId}/preview-image")
  @Operation(summary = "Get the template preview image")
  public ResponseEntity<byte[]> downloadTemplatePreviewImage(@PathVariable UUID templateId) {
    LessonSlideService.BinaryFileData fileData =
        lessonSlideService.downloadTemplatePreviewImage(templateId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(fileData.contentType()))
        .body(fileData.content());
  }

  // ─────────────────────────────────────────────────────────────
  // Helpers
  // ─────────────────────────────────────────────────────────────

  private static String contentDisposition(String fileName) {
    String encoded =
        java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    return "attachment; filename*=UTF-8''" + encoded;
  }
}
