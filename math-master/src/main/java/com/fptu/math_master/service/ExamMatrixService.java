package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AddTemplateMappingRequest;
import com.fptu.math_master.dto.request.BatchAddTemplateMappingsRequest;
import com.fptu.math_master.dto.request.BuildExamMatrixRequest;
import com.fptu.math_master.dto.request.ExamMatrixRequest;
import com.fptu.math_master.dto.request.FinalizePreviewRequest;
import com.fptu.math_master.dto.request.GeneratePreviewRequest;
import com.fptu.math_master.dto.request.MatrixRowRequest;
import com.fptu.math_master.dto.response.BatchTemplateMappingsResponse;
import com.fptu.math_master.dto.response.ExamMatrixResponse;
import com.fptu.math_master.dto.response.ExamMatrixTableResponse;
import com.fptu.math_master.dto.response.FinalizePreviewResponse;
import com.fptu.math_master.dto.response.MatchingTemplatesResponse;
import com.fptu.math_master.dto.response.MatrixValidationReport;
import com.fptu.math_master.dto.response.PreviewCandidatesResponse;
import com.fptu.math_master.dto.response.TemplateMappingResponse;
import java.util.List;
import java.util.UUID;

public interface ExamMatrixService {

  // ── Matrix CRUD ─────────────────────────────────────────────────────────

  ExamMatrixResponse createExamMatrix(ExamMatrixRequest request);

  ExamMatrixResponse updateExamMatrix(UUID matrixId, ExamMatrixRequest request);

  ExamMatrixResponse getExamMatrixById(UUID matrixId);

  ExamMatrixResponse getExamMatrixByAssessmentId(UUID assessmentId);

  List<ExamMatrixResponse> getMyExamMatrices();

  void deleteExamMatrix(UUID matrixId);

  // ── Structured Matrix Builder ───────────────────────────────────────────

  /**
   * Build a fully-specified exam matrix in one request.
   * Creates the {@link com.fptu.math_master.entity.ExamMatrix},
   * one {@link com.fptu.math_master.entity.ExamMatrixRow} per row spec,
   * and one {@link com.fptu.math_master.entity.ExamMatrixTemplateMapping} per cell.
   */
  ExamMatrixTableResponse buildMatrix(BuildExamMatrixRequest request);

  /**
   * Return the hierarchical table view of an existing matrix,
   * grouped by Chapter → Row (dạng bài) → Cells (NB/TH/VD/VDC).
   */
  ExamMatrixTableResponse getMatrixTable(UUID matrixId);

  /**
   * Add a single row (dạng bài) to an existing DRAFT matrix.
   */
  ExamMatrixTableResponse addMatrixRow(UUID matrixId, MatrixRowRequest rowRequest);

  /**
   * Remove a row and all its cells from a DRAFT matrix.
   */
  ExamMatrixTableResponse removeMatrixRow(UUID matrixId, UUID rowId);

  // ── Template Mappings ───────────────────────────────────────────────────

  TemplateMappingResponse addTemplateMapping(UUID matrixId, AddTemplateMappingRequest request);

  BatchTemplateMappingsResponse addTemplateMappings(
      UUID matrixId, BatchAddTemplateMappingsRequest request);

  void removeTemplateMapping(UUID matrixId, UUID mappingId);

  List<TemplateMappingResponse> getTemplateMappings(UUID matrixId);

  // ── Validation & Lifecycle ──────────────────────────────────────────────

  MatrixValidationReport validateMatrix(UUID matrixId);

  ExamMatrixResponse approveMatrix(UUID matrixId);

  void lockMatrix(UUID matrixId);

  ExamMatrixResponse resetMatrix(UUID matrixId);

  // ── Question Generation (template-mapping based) ────────────────────────

  MatchingTemplatesResponse listMatchingTemplates(
      UUID matrixId, String q, int page, int size, boolean onlyMine, boolean publicOnly);

  PreviewCandidatesResponse generatePreview(
      UUID matrixId, UUID mappingId, GeneratePreviewRequest request);

  FinalizePreviewResponse finalizePreview(
      UUID matrixId, UUID mappingId, FinalizePreviewRequest request);
}
