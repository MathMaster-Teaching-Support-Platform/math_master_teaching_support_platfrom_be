package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import java.util.List;
import java.util.UUID;

public interface ExamMatrixService {

  // FR-EM-001: Create Exam Matrix
  ExamMatrixResponse createExamMatrix(UUID assessmentId, ExamMatrixRequest request);

  // FR-EM-002: Configure Matrix Dimensions
  ExamMatrixResponse configureMatrixDimensions(UUID matrixId, MatrixDimensionRequest request);

  // FR-EM-003: Fill Matrix Cells
  MatrixCellResponse createOrUpdateMatrixCell(UUID matrixId, MatrixCellRequest request);

  List<MatrixCellResponse> getMatrixCells(UUID matrixId);

  // FR-EM-004: Auto-Suggest Questions for Cells
  SuggestedQuestionsResponse suggestQuestionsForCell(UUID matrixCellId, Integer limit);

  // FR-EM-005: Manual Question Selection for Cell
  MatrixCellResponse selectQuestionsManually(ManualQuestionSelectionRequest request);

  // FR-EM-006: Validate Matrix Coverage
  MatrixValidationReport validateMatrix(UUID matrixId);

  // FR-EM-007: Approve Matrix
  ExamMatrixResponse approveMatrix(UUID matrixId);

  // FR-EM-008: Lock Matrix (called by system when assessment published)
  void lockMatrix(UUID matrixId);

  // Get Matrix
  ExamMatrixResponse getExamMatrixById(UUID matrixId);

  ExamMatrixResponse getExamMatrixByAssessmentId(UUID assessmentId);

  // Delete Matrix
  void deleteExamMatrix(UUID matrixId);

  /**
   * FR-EM-NEW: List Matching Question Templates for a Matrix Cell.
   * Validates matrixId/cellId ownership, derives requirements from the cell,
   * filters + ranks question templates, and returns a paginated response.
   *
   * @param matrixId   exam matrix ID
   * @param cellId     matrix cell ID (must belong to matrixId)
   * @param q          optional search keyword (name / tags)
   * @param page       0-based page number (default 0)
   * @param size       page size (default 20)
   * @param onlyMine   when true, return only templates created by the requester
   * @param publicOnly when true, return only public templates
   * @return ranked, paginated matching templates + cell requirements summary
   */
  MatchingTemplatesResponse listMatchingTemplatesForCell(
      UUID matrixId,
      UUID cellId,
      String q,
      int page,
      int size,
      boolean onlyMine,
      boolean publicOnly);

  /**
   * FR-EM-NEW: Generate Preview Questions for a Matrix Cell (NO DB WRITE).
   *
   * <p>Generates in-memory candidate questions from a chosen template so the teacher
   * can preview quality before finalising. Nothing is persisted to any table.
   *
   * @param matrixId exam matrix ID
   * @param cellId   matrix cell ID (must belong to matrixId)
   * @param request  templateId, count (1-50), optional difficulty override, optional seed
   * @return preview candidates with question text, options, answer, parameters, warnings
   */
  PreviewCandidatesResponse generatePreview(
      UUID matrixId, UUID cellId, GeneratePreviewRequest request);

  /**
   * FR-EM-NEW: Finalize / Approve Generated Questions for a Matrix Cell (DB Persist).
   *
   * <p>Atomically saves selected preview questions to:
   * <ol>
   *   <li>questions table</li>
   *   <li>assessment_questions table</li>
   *   <li>matrix_question_mapping table</li>
   * </ol>
   * When replaceExisting=true the old cell mappings and assessment links are removed first.
   *
   * @param matrixId exam matrix ID
   * @param cellId   matrix cell ID (must belong to matrixId)
   * @param request  templateId, questions[], pointsPerQuestion, replaceExisting flag
   * @return summary with saved questionIds, mappingIds, warnings
   */
  FinalizePreviewResponse finalizePreview(
      UUID matrixId, UUID cellId, FinalizePreviewRequest request);
}
