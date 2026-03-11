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
import org.springframework.transaction.annotation.Transactional;

/**
 * STUB IMPLEMENTATION - Refactoring in progress
 * Transitioning from MatrixCell/MatrixQuestionMapping to ExamMatrixTemplateMapping architecture.
 * All methods throw UnsupportedOperationException() until proper implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamMatrixServiceImpl implements ExamMatrixService {

  @Override
  public ExamMatrixResponse createExamMatrix(UUID assessmentId, ExamMatrixRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse configureMatrixDimensions(UUID matrixId, MatrixDimensionRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public MatrixCellResponse createOrUpdateMatrixCell(UUID matrixId, MatrixCellRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public List<MatrixCellResponse> getMatrixCells(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public SuggestedQuestionsResponse suggestQuestionsForCell(UUID matrixCellId, Integer limit) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public MatrixCellResponse selectQuestionsManually(ManualQuestionSelectionRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

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
  public ExamMatrixResponse getExamMatrixById(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse getExamMatrixByAssessmentId(UUID assessmentId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public void deleteExamMatrix(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public PreviewCandidatesResponse generatePreview(UUID matrixId, UUID cellId, GeneratePreviewRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public FinalizePreviewResponse finalizePreview(UUID matrixId, UUID cellId, FinalizePreviewRequest request) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public MatchingTemplatesResponse listMatchingTemplatesForCell(UUID matrixId, UUID cellId, String q, int page, int size, boolean onlyMine, boolean publicOnly) {
    throw new UnsupportedOperationException("Under refactoring");
  }

  @Override
  public ExamMatrixResponse resetMatrix(UUID matrixId) {
    throw new UnsupportedOperationException("Under refactoring");
  }
}
