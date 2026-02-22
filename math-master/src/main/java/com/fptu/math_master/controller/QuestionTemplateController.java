package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.QuestionTemplateRequest;
import com.fptu.math_master.dto.response.AIEnhancedQuestionResponse;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.dto.response.TemplateImportResponse;
import com.fptu.math_master.dto.response.TemplateTestResponse;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.service.QuestionTemplateService;
import com.fptu.math_master.service.TemplateImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/question-templates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class QuestionTemplateController {

  QuestionTemplateService questionTemplateService;
  TemplateImportService templateImportService;

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create Question Template (FR-AI-001)",
      description =
          "Teacher creates a question template with dynamic parameters. "
              + "Template includes: type, text with placeholders, parameter definitions, "
              + "answer formula, options generator, difficulty rules, cognitive level, and tags.")
  public ApiResponse<QuestionTemplateResponse> createQuestionTemplate(
      @Valid @RequestBody QuestionTemplateRequest request) {
    log.info("REST request to create question template: {}", request.getName());
    return ApiResponse.<QuestionTemplateResponse>builder()
        .message("Question template created successfully")
        .result(questionTemplateService.createQuestionTemplate(request))
        .build();
  }

  @GetMapping("/{id}/test")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Test Existing Template (FR-AI-002)",
      description =
          "Generate test samples for an existing template using Gemini LLM. "
              + "Returns 5 sample questions by default or custom count.")
  public ApiResponse<TemplateTestResponse> testTemplate(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "5") Integer sampleCount,
      @RequestParam(defaultValue = "true") Boolean useAI) {
    log.info("REST request to test template: {} with AI={}", id, useAI);
    return ApiResponse.<TemplateTestResponse>builder()
        .message("AI-enhanced template test generated successfully. Distractors created by LLM based on common student mistakes.")
        .result(questionTemplateService.testTemplate(id, sampleCount, useAI))
        .build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Update Question Template",
      description = "Teacher updates an existing question template. Only owner or admin can edit.")
  public ApiResponse<QuestionTemplateResponse> updateQuestionTemplate(
      @PathVariable UUID id, @Valid @RequestBody QuestionTemplateRequest request) {
    log.info("REST request to update question template: {}", id);
    return ApiResponse.<QuestionTemplateResponse>builder()
        .message("Question template updated successfully")
        .result(questionTemplateService.updateQuestionTemplate(id, request))
        .build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Delete Question Template",
      description = "Teacher deletes a question template. Soft delete is performed.")
  public ApiResponse<Void> deleteQuestionTemplate(@PathVariable UUID id) {
    log.info("REST request to delete question template: {}", id);
    questionTemplateService.deleteQuestionTemplate(id);
    return ApiResponse.<Void>builder().message("Question template deleted successfully").build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Get Question Template by ID",
      description =
          "Get question template details. Accessible by owner, public templates can be viewed by others.")
  public ApiResponse<QuestionTemplateResponse> getQuestionTemplateById(@PathVariable UUID id) {
    log.info("REST request to get question template: {}", id);
    return ApiResponse.<QuestionTemplateResponse>builder()
        .result(questionTemplateService.getQuestionTemplateById(id))
        .build();
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "List My Question Templates",
      description =
          "Teacher views all their question templates. Shows name, type, "
              + "cognitive level, tags, usage count, and created date.")
  public ApiResponse<Page<QuestionTemplateResponse>> getMyQuestionTemplates(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {
    log.info("REST request to get my question templates, page: {}, size: {}", page, size);
    Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    return ApiResponse.<Page<QuestionTemplateResponse>>builder()
        .result(questionTemplateService.getMyQuestionTemplates(pageable))
        .build();
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Search Question Templates",
      description =
          "Search templates by type, cognitive level, public status, tags, or search term. "
              + "Students can only see public templates.")
  public ApiResponse<Page<QuestionTemplateResponse>> searchQuestionTemplates(
      @RequestParam(required = false) QuestionType templateType,
      @RequestParam(required = false) CognitiveLevel cognitiveLevel,
      @RequestParam(required = false) Boolean isPublic,
      @RequestParam(required = false) String searchTerm,
      @RequestParam(required = false) String[] tags,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {
    log.info("REST request to search question templates with filters");
    Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);
    return ApiResponse.<Page<QuestionTemplateResponse>>builder()
        .result(
            questionTemplateService.searchQuestionTemplates(
                templateType, cognitiveLevel, isPublic, searchTerm, tags, pageable))
        .build();
  }

  @PatchMapping("/{id}/toggle-public")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Toggle Template Public Status",
      description = "Teacher toggles whether the template is public or private.")
  public ApiResponse<QuestionTemplateResponse> togglePublicStatus(@PathVariable UUID id) {
    log.info("REST request to toggle public status for template: {}", id);
    return ApiResponse.<QuestionTemplateResponse>builder()
        .message("Template public status toggled successfully")
        .result(questionTemplateService.togglePublicStatus(id))
        .build();
  }

  @PostMapping("/{id}/generate-ai-enhanced")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Generate AI-Enhanced Question (FR-AI-004)",
      description =
          "Generate a single AI-enhanced question from template. "
              + "Gemini returns: improved wording, better distractors reflecting common mistakes, "
              + "detailed explanation/solution steps, and optional alternative solutions. "
              + "Teacher must review and approve before using.")
  public ApiResponse<AIEnhancedQuestionResponse> generateAIEnhancedQuestion(@PathVariable UUID id) {
    log.info("REST request to generate AI-enhanced question from template: {}", id);
    AIEnhancedQuestionResponse response = questionTemplateService.generateAIEnhancedQuestion(id);

    String message;
    if (response.isEnhanced() && response.isValid()) {
      message = "AI-enhanced question generated successfully. Please review before approval.";
    } else if (!response.isValid()) {
      message =
          "AI enhancement validation failed. Fallback to original question. Errors: "
              + String.join(", ", response.getValidationErrors());
    } else {
      message = "AI enhancement failed. Using original question.";
    }

    return ApiResponse.<AIEnhancedQuestionResponse>builder()
        .message(message)
        .result(response)
        .build();
  }

  @PostMapping(value = "/import-from-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Import Template from File (FR-AI-013)",
      description =
          "Teacher uploads Word/PDF/Text file and system analyzes it with AI to suggest a question template. "
              + "Supported formats: PDF (.pdf), Word (.docx), Plain Text (.txt). Max file size: 10MB")
  public ApiResponse<TemplateImportResponse> importTemplateFromFile(
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) String subjectHint,
      @RequestParam(required = false) String contextHint) {
    log.info("REST request to import template from file: {}", file.getOriginalFilename());
    TemplateImportResponse response =
        templateImportService.importTemplateFromFile(file, subjectHint, contextHint);
    String message =
        response.getAnalysisSuccessful()
            ? "Template analysis completed. Please review and edit the suggested template before saving."
            : "Template import completed with warnings. Please review carefully: "
                + String.join(", ", response.getWarnings());
    return ApiResponse.<TemplateImportResponse>builder().message(message).result(response).build();
  }
}
