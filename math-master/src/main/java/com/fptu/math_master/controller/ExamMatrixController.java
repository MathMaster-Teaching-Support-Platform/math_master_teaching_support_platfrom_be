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

  @GetMapping("/{matrixId}/cells/{cellId}/templates")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "List Matching Question Templates for a Matrix Cell",
      description =
          "Returns a ranked list of question templates that match the requirements of a given "
              + "matrix cell (question type, cognitive level, tags/topic). "
              + "Used when a teacher chooses 'Generate from Template' for a cell. "
              + "\n\nFiltering rules:"
              + "\n- templateType must match cell questionType (when cell has a type constraint)"
              + "\n- cognitiveLevel must match cell cognitiveLevel"
              + "\n- tags overlap with the cell's chapter title and topic"
              + "\n- Only non-deleted templates (deletedAt IS NULL)"
              + "\n- Only templates owned by the teacher OR public templates"
              + "\n\nRanking (descending relevance score):"
              + "\n1. templateType match (+40)"
              + "\n2. cognitiveLevel match (+30)"
              + "\n3. tag overlap with chapter/topic (+20 + up to +30)"
              + "\n4. popularity (usageCount, capped +10)"
              + "\n\nReturns 200 with empty list if no templates match (never 404).")
  public ApiResponse<MatchingTemplatesResponse> listMatchingTemplatesForCell(
      @PathVariable UUID matrixId,
      @PathVariable UUID cellId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "false") boolean onlyMine,
      @RequestParam(defaultValue = "false") boolean publicOnly) {

    log.info(
        "REST request to list matching templates for matrixId={}, cellId={}", matrixId, cellId);

    MatchingTemplatesResponse response =
        examMatrixService.listMatchingTemplatesForCell(
            matrixId, cellId, q, page, size, onlyMine, publicOnly);

    String message =
        response.getTotalTemplatesFound() == 0
            ? "No matching templates found. You can create a new template or loosen filters."
            : String.format(
                "Found %d matching template(s) for this cell.", response.getTotalTemplatesFound());

    return ApiResponse.<MatchingTemplatesResponse>builder()
        .message(message)
        .result(response)
        .build();
  }

  @PostMapping("/{matrixId}/cells/{cellId}/generate-preview")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Generate Preview Questions for a Matrix Cell (No DB Persist)",
      description =
          "Teacher selects a Question Template for a matrix cell and requests the system to "
              + "generate preview candidates to assess question quality before finalising.\n\n"
              + "⚠️ This endpoint is PURE READ — it does NOT write to any table "
              + "(no questions, assessment_questions, or matrix_question_mapping records are created).\n\n"
              + "Request body:\n"
              + "- templateId: UUID of the template to use (must not be soft-deleted; DRAFT status rejected)\n"
              + "- count: number of candidates to generate (1–50)\n"
              + "- difficulty: optional override (EASY/MEDIUM/HARD); null = determined by template rules\n"
              + "- seed: optional long for reproducible results\n\n"
              + "Response contains:\n"
              + "- cellRequirements: summary of the cell constraints for UI display\n"
              + "- candidates[]: questionText, options (A–D for MCQ), correctAnswerKey, "
              + "usedParameters, answerCalculation, calculatedDifficulty, explanation\n"
              + "- warnings[]: partial generation, difficulty override mismatch, type mismatch\n\n"
              + "Validation errors returned as 400/403/404:\n"
              + "- 404 if matrixId, cellId, or templateId not found\n"
              + "- 403 if teacher does not own the matrix\n"
              + "- 400 if cell does not belong to matrix, or request body constraints violated")
  public ApiResponse<PreviewCandidatesResponse> generatePreview(
      @PathVariable UUID matrixId,
      @PathVariable UUID cellId,
      @Valid @RequestBody GeneratePreviewRequest request) {

    log.info(
        "REST request to generate preview for matrixId={}, cellId={}, templateId={}, count={}",
        matrixId,
        cellId,
        request.getTemplateId(),
        request.getCount());

    PreviewCandidatesResponse response =
        examMatrixService.generatePreview(matrixId, cellId, request);

    String message;
    if (response.getGeneratedCount() == 0) {
      message =
          "No questions could be generated. Template constraints may be too strict "
              + "or parameter ranges too narrow. Please adjust the template or try a different one.";
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

  @PostMapping("/{matrixId}/cells/{cellId}/finalize")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Finalize / Approve Generated Questions for a Matrix Cell (Persist to DB)",
      description =
          "Teacher submits selected preview questions to be permanently saved.\n\n"
              + "This endpoint WRITES to 3 tables atomically inside a single transaction:\n"
              + "1. **questions** — creates a new Question record for each item\n"
              + "2. **assessment_questions** — attaches each question to the matrix's assessment\n"
              + "3. **matrix_question_mapping** — maps each question to this matrix cell\n\n"
              + "If ANY step fails the entire transaction is rolled back (no partial state).\n\n"
              + "**Request body fields:**\n"
              + "- `templateId` — UUID of the source template (must exist, not DRAFT)\n"
              + "- `pointsPerQuestion` — points > 0 applied to each question\n"
              + "- `replaceExisting` — when true: old cell mappings + assessment links are removed "
              + "and old question records are soft-deleted before inserting new ones\n"
              + "- `questionBankId` — optional; associates generated questions with a bank\n"
              + "- `questions[]` — list of QuestionItem (from preview candidates):\n"
              + "  - `questionText` (required, non-blank)\n"
              + "  - `questionType` (required)\n"
              + "  - `options` — required map A/B/C/D for MCQ\n"
              + "  - `correctAnswer` — A/B/C/D key for MCQ; answer value for other types\n"
              + "  - `difficulty`, `cognitiveLevel`, `tags`, `explanation`\n"
              + "  - `generationMetadata` — paramsUsed, answerFormula, seed, etc.\n\n"
              + "**Validation failures return 400/403/404.**\n\n"
              + "**Soft skips (warnings, not errors):** duplicate questionText, "
              + "difficulty/cognitiveLevel mismatch with cell.\n\n"
              + "**Hard errors (400):** MCQ with invalid options/correctAnswer, all questions skipped.")
  public ApiResponse<FinalizePreviewResponse> finalizePreview(
      @PathVariable UUID matrixId,
      @PathVariable UUID cellId,
      @Valid @RequestBody FinalizePreviewRequest request) {

    log.info(
        "REST request to finalize preview for matrixId={}, cellId={}, templateId={}, "
            + "count={}, replaceExisting={}",
        matrixId,
        cellId,
        request.getTemplateId(),
        request.getQuestions().size(),
        request.getReplaceExisting());

    FinalizePreviewResponse response = examMatrixService.finalizePreview(matrixId, cellId, request);

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
              "Successfully finalised %d question(s) for cell. "
                  + "Questions saved to DB and linked to assessment.",
              response.getSavedCount());
    }

    return ApiResponse.<FinalizePreviewResponse>builder().message(message).result(response).build();
  }
}
