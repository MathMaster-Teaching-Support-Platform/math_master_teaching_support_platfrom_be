package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.service.ExamMatrixService;
import java.util.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * STUB IMPLEMENTATION - Refactoring in progress.
 * Transitioned from MatrixCell/MatrixQuestionMapping to ExamMatrixTemplateMapping architecture.
 * All methods throw UnsupportedOperationException until proper implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamMatrixServiceImpl implements ExamMatrixService {

  // ── Matrix CRUD ─────────────────────────────────────────────────────────

  @Override
  public ExamMatrixResponse createExamMatrix(ExamMatrixRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse updateExamMatrix(UUID matrixId, ExamMatrixRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse getExamMatrixById(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse getExamMatrixByAssessmentId(UUID assessmentId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public List<ExamMatrixResponse> getMyExamMatrices() {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public void deleteExamMatrix(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  // ── Template Mappings ───────────────────────────────────────────────────

  @Override
  public TemplateMappingResponse addTemplateMapping(UUID matrixId, AddTemplateMappingRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public void removeTemplateMapping(UUID matrixId, UUID mappingId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public List<TemplateMappingResponse> getTemplateMappings(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  // ── Validation & Lifecycle ──────────────────────────────────────────────

  @Override
  public MatrixValidationReport validateMatrix(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse approveMatrix(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public void lockMatrix(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse resetMatrix(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  // ── Question Generation ─────────────────────────────────────────────────

  @Override
  public MatchingTemplatesResponse listMatchingTemplates(
      UUID matrixId, String q, int page, int size, boolean onlyMine, boolean publicOnly) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public PreviewCandidatesResponse generatePreview(
      UUID matrixId, UUID mappingId, GeneratePreviewRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public FinalizePreviewResponse finalizePreview(
      UUID matrixId, UUID mappingId, FinalizePreviewRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }
}
