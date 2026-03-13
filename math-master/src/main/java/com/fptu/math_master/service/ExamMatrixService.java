package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
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
