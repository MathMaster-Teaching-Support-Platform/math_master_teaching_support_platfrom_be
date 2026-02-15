package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exam-matrices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class ExamMatrixController {

  ExamMatrixService examMatrixService;

  @PostMapping("/assessment/{assessmentId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create exam matrix",
      description =
          "Teacher creates exam matrix for assessment. "
              + "Validates: assessment must have lesson, lesson must have chapters. "
              + "One assessment can only have one matrix. "
              + "Status = DRAFT by default. Redirects to Matrix Builder.")
  public ApiResponse<ExamMatrixResponse> createExamMatrix(
      @PathVariable UUID assessmentId, @Valid @RequestBody ExamMatrixRequest request) {
    log.info("REST request to create exam matrix for assessment: {}", assessmentId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .message("Exam matrix created successfully. Configure matrix dimensions next.")
        .result(examMatrixService.createExamMatrix(assessmentId, request))
        .build();
  }

  @PutMapping("/{matrixId}/dimensions")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Configure matrix dimensions",
      description =
          "Teacher selects chapters and cognitive levels (Bloom's Taxonomy). "
              + "System generates matrix grid: Chapters × Cognitive Levels. "
              + "Example: 3 chapters × 4 levels = 12 cells.")
  public ApiResponse<ExamMatrixResponse> configureMatrixDimensions(
      @PathVariable UUID matrixId, @Valid @RequestBody MatrixDimensionRequest request) {
    log.info("REST request to configure matrix dimensions for: {}", matrixId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .message(
            String.format(
                "Matrix configured: %d chapters × %d cognitive levels = %d cells",
                request.getChapterIds().size(),
                request.getCognitiveLevels().size(),
                request.getChapterIds().size() * request.getCognitiveLevels().size()))
        .result(examMatrixService.configureMatrixDimensions(matrixId, request))
        .build();
  }

  @PostMapping("/{matrixId}/cells")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Fill matrix cells",
      description =
          "Teacher fills each cell with: "
              + "- Number of questions "
              + "- Points per question "
              + "- Difficulty (optional override) "
              + "- Question type (optional). "
              + "System auto-calculates: total points per cell, total questions, total points in matrix.")
  public ApiResponse<MatrixCellResponse> createOrUpdateMatrixCell(
      @PathVariable UUID matrixId, @Valid @RequestBody MatrixCellRequest request) {
    log.info("REST request to create/update matrix cell for: {}", matrixId);
    return ApiResponse.<MatrixCellResponse>builder()
        .message("Matrix cell updated successfully. Total points recalculated.")
        .result(examMatrixService.createOrUpdateMatrixCell(matrixId, request))
        .build();
  }

  @GetMapping("/{matrixId}/cells")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get all matrix cells",
      description = "Get all cells for the matrix with calculated totals and selection status.")
  public ApiResponse<List<MatrixCellResponse>> getMatrixCells(@PathVariable UUID matrixId) {
    log.info("REST request to get matrix cells for: {}", matrixId);
    return ApiResponse.<List<MatrixCellResponse>>builder()
        .result(examMatrixService.getMatrixCells(matrixId))
        .build();
  }

  @GetMapping("/cells/{cellId}/suggest-questions")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Auto-suggest questions for cell",
      description =
          "System automatically suggests matching questions based on: "
              + "chapter_id, difficulty, cognitive_level (from Bloom's taxonomy tags), question_type. "
              + "Excludes already selected questions. Orders randomly.")
  public ApiResponse<SuggestedQuestionsResponse> suggestQuestionsForCell(
      @PathVariable UUID cellId, @RequestParam(required = false) Integer limit) {
    log.info("REST request to suggest questions for cell: {}, limit: {}", cellId, limit);
    return ApiResponse.<SuggestedQuestionsResponse>builder()
        .result(examMatrixService.suggestQuestionsForCell(cellId, limit))
        .build();
  }

  @PostMapping("/cells/select-questions")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Manual question selection for cell",
      description =
          "Teacher manually selects questions for a cell instead of using auto-suggest. "
              + "Opens question picker filtered by cell criteria. "
              + "Creates matrix_question_mapping entries.")
  public ApiResponse<MatrixCellResponse> selectQuestionsManually(
      @Valid @RequestBody ManualQuestionSelectionRequest request) {
    log.info(
        "REST request to manually select {} questions for cell: {}",
        request.getQuestionIds().size(),
        request.getMatrixCellId());
    return ApiResponse.<MatrixCellResponse>builder()
        .message(
            String.format("%d questions selected successfully", request.getQuestionIds().size()))
        .result(examMatrixService.selectQuestionsManually(request))
        .build();
  }

  @GetMapping("/{matrixId}/validate")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Validate matrix coverage",
      description =
          "System validates: "
              + "- Total questions = target "
              + "- Total points = target "
              + "- All cells filled "
              + "- Difficulty distribution (Easy 30-50%, Medium 30-50%, Hard 10-30%) "
              + "- Cognitive levels balanced "
              + "- Each chapter covered (≥15%). "
              + "Returns validation report with errors/warnings. Blocks approve if errors exist.")
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
              + "Validates first (FR-EM-006). "
              + "If errors → BLOCKED. If only warnings → Allow with confirmation. "
              + "Status → APPROVED. System auto-populates assessment_questions from matrix.")
  public ApiResponse<ExamMatrixResponse> approveMatrix(@PathVariable UUID matrixId) {
    log.info("REST request to approve matrix: {}", matrixId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .message("Matrix approved successfully! Questions populated to assessment.")
        .result(examMatrixService.approveMatrix(matrixId))
        .build();
  }

  @GetMapping("/{matrixId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get exam matrix by ID",
      description = "Get exam matrix details with stats: cells, filled cells, selected questions.")
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
      description = "Get exam matrix for a specific assessment.")
  public ApiResponse<ExamMatrixResponse> getExamMatrixByAssessmentId(
      @PathVariable UUID assessmentId) {
    log.info("REST request to get exam matrix by assessment: {}", assessmentId);
    return ApiResponse.<ExamMatrixResponse>builder()
        .result(examMatrixService.getExamMatrixByAssessmentId(assessmentId))
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

  @PostMapping("/{matrixId}/lock")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Lock matrix - Internal use",
      description =
          "System automatically locks matrix when assessment is published. "
              + "Status → LOCKED. Matrix cannot be edited. Questions cannot be changed. "
              + "This endpoint is for admin/system use only.")
  public ApiResponse<Void> lockMatrix(@PathVariable UUID matrixId) {
    log.info("REST request to lock matrix: {}", matrixId);
    examMatrixService.lockMatrix(matrixId);
    return ApiResponse.<Void>builder().message("Matrix locked successfully.").build();
  }
}
