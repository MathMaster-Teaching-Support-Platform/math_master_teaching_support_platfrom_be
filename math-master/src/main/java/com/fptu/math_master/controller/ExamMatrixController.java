package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.BuildExamMatrixRequest;
import com.fptu.math_master.dto.request.BatchUpsertMatrixRowCellsRequest;
import com.fptu.math_master.dto.request.ExamMatrixRequest;
import com.fptu.math_master.dto.request.MatrixRowRequest;
import com.fptu.math_master.dto.request.UpdateMatrixRowCellsRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.ExamMatrixResponse;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.MatrixValidationReport;
import com.fptu.math_master.service.ExamMatrixService;
import com.fptu.math_master.service.impl.ExamMatrixPdfExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exam-matrices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class ExamMatrixController {

  ExamMatrixService examMatrixService;
  ExamMatrixPdfExportService examMatrixPdfExportService;

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
        .message("Exam matrix created successfully.")
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

  @GetMapping("/available")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get available exam matrices",
      description = "Get all available exam matrices that can be used for assessment generation.")
  public ApiResponse<List<ExamMatrixResponse>> getAvailableExamMatrices() {
    log.info("REST request to get available exam matrices");
    return ApiResponse.<List<ExamMatrixResponse>>builder()
        .result(examMatrixService.getMyExamMatrices())
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

  @PutMapping("/{matrixId}/rows/{rowId}/cells")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create/update cells for a row",
      description =
          "Upserts all cognitive-level cells for one row. "
              + "Supports FE flow: create row first, then edit cell numbers flexibly.")
  public ApiResponse<ExamMatrixTableResponse> upsertMatrixRowCells(
      @PathVariable UUID matrixId,
      @PathVariable UUID rowId,
      @Valid @RequestBody UpdateMatrixRowCellsRequest request) {
    log.info("REST request to upsert cells for row {} in matrix {}", rowId, matrixId);
    return ApiResponse.<ExamMatrixTableResponse>builder()
        .message("Matrix row cells updated successfully.")
        .result(examMatrixService.upsertMatrixRowCells(matrixId, rowId, request.getCells()))
        .build();
  }

    @PutMapping("/{matrixId}/rows/cells:batch")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Operation(
            summary = "Batch create/update cells for multiple rows",
            description =
                    "FE calculates percentages locally, then sends final question counts for many rows in one call.")
    public ApiResponse<ExamMatrixTableResponse> batchUpsertMatrixRowCells(
            @PathVariable UUID matrixId,
            @Valid @RequestBody BatchUpsertMatrixRowCellsRequest request) {
        log.info("REST request to batch upsert row cells in matrix {}", matrixId);
        return ApiResponse.<ExamMatrixTableResponse>builder()
                .message("Matrix row cells batch updated successfully.")
                .result(examMatrixService.batchUpsertMatrixRowCells(matrixId, request))
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

  // ── PDF Export ────────────────────────────────────────────────────────────

  @GetMapping(value = "/{matrixId}/export-pdf", produces = "application/pdf")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Export exam matrix as PDF",
      description =
          "Generates a printable A4-landscape PDF of the exam matrix table, "
              + "matching the standard Vietnamese THPT exam-matrix layout "
              + "(Lớp / Chương / Dạng bài / NB / TH / VD / VDC / Tổng).")
  public ResponseEntity<byte[]> exportMatrixPdf(@PathVariable UUID matrixId) {
    log.info("REST request to export matrix as PDF: {}", matrixId);
    byte[] pdf = examMatrixPdfExportService.exportToPdf(matrixId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);
    headers.setContentDisposition(
        ContentDisposition.attachment()
            .filename("exam-matrix-" + matrixId + ".pdf")
            .build());
    return ResponseEntity.ok().headers(headers).body(pdf);
  }
}
