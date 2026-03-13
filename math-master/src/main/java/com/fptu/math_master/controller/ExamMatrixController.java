package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.AddTemplateMappingRequest;
import com.fptu.math_master.dto.request.BatchAddTemplateMappingsRequest;
import com.fptu.math_master.dto.request.BuildExamMatrixRequest;
import com.fptu.math_master.dto.request.ExamMatrixRequest;
import com.fptu.math_master.dto.request.FinalizePreviewRequest;
import com.fptu.math_master.dto.request.GeneratePreviewRequest;
import com.fptu.math_master.dto.request.MatrixRowRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.BatchTemplateMappingsResponse;
import com.fptu.math_master.dto.response.ExamMatrixResponse;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.FinalizePreviewResponse;
import com.fptu.math_master.dto.response.MatchingTemplatesResponse;
import com.fptu.math_master.dto.response.MatrixValidationReport;
import com.fptu.math_master.dto.response.PreviewCandidatesResponse;
import com.fptu.math_master.dto.response.TemplateMappingResponse;
import com.fptu.math_master.service.ExamMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exam-matrices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class ExamMatrixController {

  ExamMatrixService examMatrixService;

  // ── Matrix CRUD ─────────────────────────────────────────────────────────

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create exam matrix",
      description =
          "Teacher creates a reusable exam matrix. "
              + "The matrix is owned by the teacher and can later be linked to assessments. "
              + "Status = DRAFT by default.")
  public ApiResponse<ExamMatrixResponse> createExamMatrix(
      @Valid @RequestBody ExamMatrixRequest request) {
    log.info("REST request to create exam matrix: {}", request.getName());
    return ApiResponse.<ExamMatrixResponse>builder()
        .message("Exam matrix created successfully. Add template mappings next.")
        .result(examMatrixService.createExamMatrix(request))
        .build();
  }

  @PutMapping("/{matrixId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Update exam matrix",
      description =
          "Update matrix name, description, or reusable flag. Only DRAFT matrices can be updated.")
  public ApiResponse<ExamMatrixResponse> updateExamMatrix(
      @PathVariable UUID matrixId, @Valid @RequestBody ExamMatrixRequest request) {
    log.info("REST request to update exam matrix: {}", matrixId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .message("Exam matrix updated successfully.")
        .result(examMatrixService.updateExamMatrix(matrixId, request))
        .build();
  }

  @GetMapping("/{matrixId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get exam matrix by ID",
      description = "Get exam matrix details with template mapping count.")
  public ApiResponse<ExamMatrixResponse> getExamMatrixById(@PathVariable UUID matrixId) {
    log.info("REST request to get exam matrix: {}", matrixId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .result(examMatrixService.getExamMatrixById(matrixId))
        .build();
  }

  @GetMapping("/assessment/{assessmentId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get exam matrix by assessment ID",
      description = "Get exam matrix linked to a specific assessment via assessment.examMatrixId.")
  public ApiResponse<ExamMatrixResponse> getExamMatrixByAssessmentId(
      @PathVariable UUID assessmentId) {
    log.info("REST request to get exam matrix by assessment: {}", assessmentId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .result(examMatrixService.getExamMatrixByAssessmentId(assessmentId))
        .build();
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "List my exam matrices",
      description = "Get all exam matrices owned by the current teacher.")
  public ApiResponse<List<ExamMatrixResponse>> getMyExamMatrices() {
    log.info("REST request to get my exam matrices");
    return ApiResponse.<List<ExamMatrixResponse>>builder()
        .result(examMatrixService.getMyExamMatrices())
        .build();
  }

  @DeleteMapping("/{matrixId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Delete exam matrix",
      description = "Delete exam matrix. Cannot delete if status = LOCKED. Soft delete.")
  public ApiResponse<Void> deleteExamMatrix(@PathVariable UUID matrixId) {
    log.info("REST request to delete exam matrix: {}", matrixId);
    examMatrixService.deleteExamMatrix(matrixId);
    return ApiResponse.<Void>builder().message("Exam matrix deleted successfully.").build();
  }

  // ── Template Mappings ───────────────────────────────────────────────────

  @PostMapping("/{matrixId}/template-mappings")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Add template mapping",
      description =
          "Add a question template mapping to the matrix. "
              + "Specifies which template to use, cognitive level, question count, and points per question.")
  public ApiResponse<TemplateMappingResponse> addTemplateMapping(
      @PathVariable UUID matrixId, @Valid @RequestBody AddTemplateMappingRequest request) {
    log.info("REST request to add template mapping to matrix: {}", matrixId);
    return ApiResponse.<TemplateMappingResponse>builder()
        .message("Template mapping added successfully.")
        .result(examMatrixService.addTemplateMapping(matrixId, request))
        .build();
  }

  @PostMapping("/{matrixId}/templates")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Add multiple template mappings in batch",
      description =
          "Add multiple question template mappings to the matrix in a single transaction. "
              + "Validates all templateIds exist before adding. "
              + "Returns a list of all added mappings.")
  public ApiResponse<BatchTemplateMappingsResponse> addTemplateMappings(
      @PathVariable UUID matrixId, @Valid @RequestBody BatchAddTemplateMappingsRequest request) {
    log.info(
        "REST request to add batch template mappings to matrix: {}, count={}",
        matrixId,
        request.getMappings().size());
    return ApiResponse.<BatchTemplateMappingsResponse>builder()
        .message("Template mappings added successfully in batch.")
        .result(examMatrixService.addTemplateMappings(matrixId, request))
        .build();
  }

  @DeleteMapping("/{matrixId}/template-mappings/{mappingId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Remove template mapping",
      description =
          "Remove a template mapping from the matrix. Only DRAFT matrices can be modified.")
  public ApiResponse<Void> removeTemplateMapping(
      @PathVariable UUID matrixId, @PathVariable UUID mappingId) {
    log.info("REST request to remove template mapping {} from matrix: {}", mappingId, matrixId);
    examMatrixService.removeTemplateMapping(matrixId, mappingId);
    return ApiResponse.<Void>builder().message("Template mapping removed successfully.").build();
  }

  @GetMapping("/{matrixId}/template-mappings")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get all template mappings",
      description = "Get all template mappings for a matrix.")
  public ApiResponse<List<TemplateMappingResponse>> getTemplateMappings(
      @PathVariable UUID matrixId) {
    log.info("REST request to get template mappings for matrix: {}", matrixId);
    return ApiResponse.<List<TemplateMappingResponse>>builder()
        .result(examMatrixService.getTemplateMappings(matrixId))
        .build();
  }

  // ── Validation & Lifecycle ──────────────────────────────────────────────

  @GetMapping("/{matrixId}/validate")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Validate matrix coverage",
      description =
          "Validates matrix template mappings: all mappings have templates, "
              + "cognitive levels are covered, question counts and points are reasonable. "
              + "Returns validation report with errors/warnings.")
  public ApiResponse<MatrixValidationReport> validateMatrix(@PathVariable UUID matrixId) {
    log.info("REST request to validate matrix: {}", matrixId);
    MatrixValidationReport report = examMatrixService.validateMatrix(matrixId);
    return ApiResponse.<MatrixValidationReport>builder()
        .message(
            report.isCanApprove()
                ? "Matrix validation passed. Ready to approve."
                : "Matrix validation failed. Please fix errors before approving.")
        .result(report)
        .build();
  }

  @PostMapping("/{matrixId}/approve")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Approve matrix",
      description =
          "Teacher approves matrix after validation. "
              + "Validates first. If errors → BLOCKED. If only warnings → allow with confirmation. "
              + "Status → APPROVED.")
  public ApiResponse<ExamMatrixResponse> approveMatrix(@PathVariable UUID matrixId) {
    log.info("REST request to approve matrix: {}", matrixId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .message("Matrix approved successfully!")
        .result(examMatrixService.approveMatrix(matrixId))
        .build();
  }

  @PostMapping("/{matrixId}/lock")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Lock matrix - Internal use",
      description =
          "System automatically locks matrix when an associated assessment is published. "
              + "Status → LOCKED. Matrix cannot be edited.")
  public ApiResponse<Void> lockMatrix(@PathVariable UUID matrixId) {
    log.info("REST request to lock matrix: {}", matrixId);
    examMatrixService.lockMatrix(matrixId);
    return ApiResponse.<Void>builder().message("Matrix locked successfully.").build();
  }

  @PostMapping("/{matrixId}/reset")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Reset matrix to DRAFT",
      description = "Reset an APPROVED matrix back to DRAFT so template mappings can be re-edited.")
  public ApiResponse<ExamMatrixResponse> resetMatrix(@PathVariable UUID matrixId) {
    log.info("REST request to reset matrix: {}", matrixId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .message("Matrix reset to DRAFT. You can now edit template mappings.")
        .result(examMatrixService.resetMatrix(matrixId))
        .build();
  }

  // ── Question Generation (template-mapping based) ────────────────────────

  @GetMapping("/{matrixId}/matching-templates")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "List matching question templates for this matrix",
      description =
          "Returns a ranked list of question templates that can be used for this matrix. "
              + "Helps teacher find suitable templates before adding a template mapping.")
  public ApiResponse<MatchingTemplatesResponse> listMatchingTemplates(
      @PathVariable UUID matrixId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean onlyMine,
      @RequestParam(defaultValue = "false") boolean publicOnly) {

    log.info("REST request to list matching templates for matrixId={}", matrixId);

    MatchingTemplatesResponse response =
        examMatrixService.listMatchingTemplates(matrixId, q, page, size, onlyMine, publicOnly);

    String message =
        response.getTotalTemplatesFound() == 0
            ? "No matching templates found. You can create a new template or loosen filters."
            : String.format("Found %d matching template(s).", response.getTotalTemplatesFound());

    return ApiResponse.<MatchingTemplatesResponse>builder()
        .message(message)
        .result(response)
        .build();
  }

  @PostMapping("/{matrixId}/template-mappings/{mappingId}/generate-preview")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Generate preview questions for a template mapping (no DB persist)",
      description =
          "Generates in-memory candidate questions from the template associated with a mapping "
              + "so the teacher can preview quality before finalising. Nothing is persisted.")
  public ApiResponse<PreviewCandidatesResponse> generatePreview(
      @PathVariable UUID matrixId,
      @PathVariable UUID mappingId,
      @Valid @RequestBody GeneratePreviewRequest request) {

    log.info(
        "REST request to generate preview for matrixId={}, mappingId={}, templateId={}, count={}",
        matrixId,
        mappingId,
        request.getTemplateId(),
        request.getCount());

    PreviewCandidatesResponse response =
        examMatrixService.generatePreview(matrixId, mappingId, request);

    String message;
    if (response.getGeneratedCount() == 0) {
      message =
          "No questions could be generated. Template constraints may be too strict "
              + "or parameter ranges too narrow.";
    } else if (response.getGeneratedCount() < response.getRequestedCount()) {
      message =
          String.format(
              "Partial preview: generated %d of %d requested question(s). "
                  + "Check warnings for details.",
              response.getGeneratedCount(), response.getRequestedCount());
    } else {
      message =
          String.format(
              "Preview generated successfully: %d question(s). "
                  + "Review candidates before finalising.",
              response.getGeneratedCount());
    }

    return ApiResponse.<PreviewCandidatesResponse>builder()
        .message(message)
        .result(response)
        .build();
  }

  @PostMapping("/{matrixId}/template-mappings/{mappingId}/finalize")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Finalize generated questions for a template mapping (persist to DB)",
      description =
          "Persists selected preview questions to the questions table "
              + "and creates assessment_questions entries when linked to an assessment. "
              + "Atomic transaction — rollback on any failure.")
  public ApiResponse<FinalizePreviewResponse> finalizePreview(
      @PathVariable UUID matrixId,
      @PathVariable UUID mappingId,
      @Valid @RequestBody FinalizePreviewRequest request) {

    log.info(
        "REST request to finalize preview for matrixId={}, mappingId={}, templateId={}, "
            + "count={}, replaceExisting={}",
        matrixId,
        mappingId,
        request.getTemplateId(),
        request.getQuestions().size(),
        request.getReplaceExisting());

    FinalizePreviewResponse response =
        examMatrixService.finalizePreview(matrixId, mappingId, request);

    String message;
    if (response.getSavedCount() == 0) {
      message =
          "No questions were saved. All submitted questions were duplicates or invalid. "
              + "Check warnings for details.";
    } else if (response.getSavedCount() < response.getRequestedCount()) {
      message =
          String.format(
              "Partially saved: %d of %d question(s) committed to DB. "
                  + "Check warnings for skipped items.",
              response.getSavedCount(), response.getRequestedCount());
    } else {
      message =
          String.format(
              "Successfully finalised %d question(s). Questions saved to DB.",
              response.getSavedCount());
    }

    return ApiResponse.<FinalizePreviewResponse>builder().message(message).result(response).build();
  }

  // ── Structured Matrix Builder ───────────────────────────────────────────

  @PostMapping("/build")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Build exam matrix from structured spec",
      description =
          "Creates a fully-specified exam matrix in one request. "
              + "The body defines each 'dạng bài' row with chapter, optional lesson/template, "
              + "and per-cognitive-level question counts and points. "
              + "Returns the hierarchical table view (Lớp → Chương → Dạng bài → NB/TH/VD/VDC).")
  public ApiResponse<ExamMatrixTableResponse> buildMatrix(
      @Valid @RequestBody BuildExamMatrixRequest request) {
    log.info("REST request to build structured exam matrix: name={}", request.getName());
    return ApiResponse.<ExamMatrixTableResponse>builder()
        .message("Exam matrix built successfully.")
        .result(examMatrixService.buildMatrix(request))
        .build();
  }

  @GetMapping("/{matrixId}/table")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get exam matrix as table view",
      description =
          "Returns the matrix grouped by Chapter → Row (dạng bài) → Cells (NB/TH/VD/VDC), "
              + "with per-chapter and grand-total aggregates. "
              + "Matches the visual layout of the Vietnamese THPT exam-matrix diagram.")
  public ApiResponse<ExamMatrixTableResponse> getMatrixTable(@PathVariable UUID matrixId) {
    log.info("REST request to get matrix table: {}", matrixId);
    return ApiResponse.<ExamMatrixTableResponse>builder()
        .result(examMatrixService.getMatrixTable(matrixId))
        .build();
  }

  @PostMapping("/{matrixId}/rows")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Add a row to exam matrix",
      description =
          "Appends one 'dạng bài' row (with cells) to an existing DRAFT matrix. "
              + "Returns the updated table view.")
  public ApiResponse<ExamMatrixTableResponse> addMatrixRow(
      @PathVariable UUID matrixId, @Valid @RequestBody MatrixRowRequest request) {
    log.info("REST request to add row to matrix: {}", matrixId);
    return ApiResponse.<ExamMatrixTableResponse>builder()
        .message("Matrix row added successfully.")
        .result(examMatrixService.addMatrixRow(matrixId, request))
        .build();
  }

  @DeleteMapping("/{matrixId}/rows/{rowId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Remove a row from exam matrix",
      description = "Deletes a 'dạng bài' row and all its cells. Matrix must be in DRAFT status.")
  public ApiResponse<ExamMatrixTableResponse> removeMatrixRow(
      @PathVariable UUID matrixId, @PathVariable UUID rowId) {
    log.info("REST request to remove row {} from matrix {}", rowId, matrixId);
    return ApiResponse.<ExamMatrixTableResponse>builder()
        .message("Matrix row removed successfully.")
        .result(examMatrixService.removeMatrixRow(matrixId, rowId))
        .build();
  }
}
