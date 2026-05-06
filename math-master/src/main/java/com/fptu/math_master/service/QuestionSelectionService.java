package com.fptu.math_master.service;

import com.fptu.math_master.entity.AssessmentQuestion;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface QuestionSelectionService {

  /**
   * Validate availability using the matrix's stored default bank (legacy).
   */
  default void validateAvailability(UUID examMatrixId) {
    validateAvailability(examMatrixId, (Collection<UUID>) null);
  }

  /**
   * Single-bank override variant — equivalent to passing a singleton set.
   */
  default void validateAvailability(UUID examMatrixId, UUID overrideQuestionBankId) {
    validateAvailability(
        examMatrixId,
        overrideQuestionBankId != null ? List.of(overrideQuestionBankId) : null);
  }

  /**
   * Validate availability across an explicit set of source banks.
   *
   * <p>The matrix is a pure blueprint; the bank set chosen at generation time
   * defines the source pool for question selection. When {@code overrideBankIds}
   * is null/empty the service falls back to the matrix's stored default bank
   * for backward compatibility.
   */
  void validateAvailability(UUID examMatrixId, Collection<UUID> overrideBankIds);

  /** Single-bank build (legacy). */
  default SelectionPlan buildSelectionPlan(
      UUID assessmentId, UUID examMatrixId, int startOrderIndex) {
    return buildSelectionPlan(
        assessmentId, examMatrixId, (Collection<UUID>) null, startOrderIndex);
  }

  /** Single-bank override build — equivalent to passing a singleton set. */
  default SelectionPlan buildSelectionPlan(
      UUID assessmentId,
      UUID examMatrixId,
      UUID overrideQuestionBankId,
      int startOrderIndex) {
    return buildSelectionPlan(
        assessmentId,
        examMatrixId,
        overrideQuestionBankId != null ? List.of(overrideQuestionBankId) : null,
        startOrderIndex);
  }

  /**
   * Build a selection plan from the union of an explicit set of banks.
   * Questions are deterministically shuffled (seed = assessmentId+mappingId)
   * across the union before each cell's top-N pick, so re-running with the
   * same inputs yields identical assessments.
   */
  SelectionPlan buildSelectionPlan(
      UUID assessmentId,
      UUID examMatrixId,
      Collection<UUID> overrideBankIds,
      int startOrderIndex);

  /**
   * Compute per-cell coverage of the matrix against an explicit bank set
   * without persisting anything. Surfaces the exact (chapter × level × type)
   * tuples that fall short so the FE can render a concrete gap report.
   */
  CoverageReport computeCoverage(UUID examMatrixId, Collection<UUID> bankIds);

  record SelectionPlan(List<AssessmentQuestion> assessmentQuestions, int totalPoints) {}

  /**
   * Per-cell coverage diagnostic.
   *
   * @param ok true when every gap.required <= gap.available
   * @param gaps one entry per matrix cell (chapter, cognitiveLevel, questionType)
   *     including healthy cells; FE filters to {@code required > available} to
   *     show problem rows.
   */
  record CoverageReport(boolean ok, List<Gap> gaps) {}

  record Gap(
      UUID chapterId,
      String chapterTitle,
      String cognitiveLevel,
      String questionType,
      int required,
      long available) {}
}
