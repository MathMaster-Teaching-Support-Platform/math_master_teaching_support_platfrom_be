package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.LessonSlideConfirmContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGenerateContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxFromJsonRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.dto.response.LessonSlideGeneratedContentResponse;
import com.fptu.math_master.dto.response.SlideTemplateResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
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

  @PostMapping(value = "/templates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload slide template (PPTX)")
  public ApiResponse<SlideTemplateResponse> uploadTemplate(
      @RequestParam("name") String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template uploaded successfully")
        .result(lessonSlideService.uploadTemplate(name, description, file))
        .build();
  }

  @PutMapping(value = "/templates/{templateId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Update slide template metadata or file")
  public ApiResponse<SlideTemplateResponse> updateTemplate(
      @PathVariable UUID templateId,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "active", required = false) Boolean active,
      @RequestParam(value = "file", required = false) MultipartFile file) {
    return ApiResponse.<SlideTemplateResponse>builder()
        .message("Template updated successfully")
        .result(lessonSlideService.updateTemplate(templateId, name, description, active, file))
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

  private String contentDisposition(String fileName) {
    return "attachment; filename*=UTF-8''"
        + java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
