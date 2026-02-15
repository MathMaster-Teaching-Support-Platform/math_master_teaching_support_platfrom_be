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
}
