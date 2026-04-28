package com.fptu.math_master.service.impl;

import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.ExamMatrixBankMapping;
import com.fptu.math_master.entity.ExamMatrixRow;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.service.QuestionSelectionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QuestionSelectionServiceImpl implements QuestionSelectionService {

  ExamMatrixBankMappingRepository examMatrixBankMappingRepository;
  ExamMatrixRepository examMatrixRepository;
  ExamMatrixRowRepository examMatrixRowRepository;
  QuestionRepository questionRepository;

  @Override
  public void validateAvailability(UUID examMatrixId) {
    // Load the matrix to get questionBankId
    ExamMatrix matrix =
        examMatrixRepository
            .findById(examMatrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    UUID questionBankId = matrix.getQuestionBankId();

    // Phase 4: questionBankId is now required (NOT NULL), no legacy support needed
    if (questionBankId == null) {
      throw new AppException(ErrorCode.QUESTION_BANK_REQUIRED);
    }

    List<ExamMatrixBankMapping> mappings =
        examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(examMatrixId);

    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
    }

    // Build a map of matrixRowId -> ExamMatrixRow for chapter lookup
    Map<UUID, ExamMatrixRow> rowById = new HashMap<>();
    for (ExamMatrixRow row : examMatrixRowRepository.findByExamMatrixId(examMatrixId)) {
      rowById.put(row.getId(), row);
    }

    for (ExamMatrixBankMapping mapping : mappings) {
      int required = requiredQuestions(mapping);
      if (required <= 0) {
        continue;
      }

      // Get chapter from the row
      ExamMatrixRow row = rowById.get(mapping.getMatrixRowId());
      if (row == null || row.getChapterId() == null) {
        log.warn(
            "Mapping {} has no associated row or chapter - skipping validation",
            mapping.getId());
        continue;
      }

      long available =
          countAvailableByBankAndChapterAndCognitive(
              questionBankId, row.getChapterId(), mapping);
      if (available < required) {
        log.error(
            "Insufficient questions: bank={}, chapter={}, cognitive={}, required={}, available={}",
            questionBankId,
            row.getChapterId(),
            mapping.getCognitiveLevel(),
            required,
            available);
        throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
      }
    }
  }

  @Override
  public SelectionPlan buildSelectionPlan(UUID assessmentId, UUID examMatrixId, int startOrderIndex) {
    validateAvailability(examMatrixId);

    // Load the matrix to get questionBankId
    ExamMatrix matrix =
        examMatrixRepository
            .findById(examMatrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    UUID questionBankId = matrix.getQuestionBankId();

    // Phase 4: questionBankId is now required (NOT NULL), no legacy support needed
    if (questionBankId == null) {
      throw new AppException(ErrorCode.QUESTION_BANK_REQUIRED);
    }

    List<ExamMatrixBankMapping> mappings =
        examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(examMatrixId);

    // Build a map of matrixRowId -> ExamMatrixRow for chapter lookup
    Map<UUID, ExamMatrixRow> rowById = new HashMap<>();
    for (ExamMatrixRow row : examMatrixRowRepository.findByExamMatrixId(examMatrixId)) {
      rowById.put(row.getId(), row);
    }

    SelectionContext context =
        new SelectionContext(new ArrayList<>(), new HashSet<>(), startOrderIndex, 0);

    for (ExamMatrixBankMapping mapping : mappings) {
      int required = requiredQuestions(mapping);
      ExamMatrixRow row = rowById.get(mapping.getMatrixRowId());

      if (row == null || row.getChapterId() == null) {
        log.warn(
            "Mapping {} has no associated row or chapter - skipping", mapping.getId());
        continue;
      }

      appendSelectionFromMappingWithChapter(
          assessmentId, questionBankId, row.getChapterId(), mapping, required, context);
    }

    return new SelectionPlan(context.plannedQuestions(), context.totalPoints());
  }

  // ========================================================================
  // Chapter-Based Query Methods (Phase 2)
  // ========================================================================

  private void appendSelectionFromMappingWithChapter(
      UUID assessmentId,
      UUID questionBankId,
      UUID chapterId,
      ExamMatrixBankMapping mapping,
      int required,
      SelectionContext context) {
    if (required <= 0) {
      return;
    }

    List<Question> selectedQuestions =
        selectQuestionsForCellWithChapter(
            assessmentId, questionBankId, chapterId, mapping, required, context.usedQuestionIds());

    for (Question selected : selectedQuestions) {
      context.plannedQuestions().add(
          AssessmentQuestion.builder()
              .assessmentId(assessmentId)
              .questionId(selected.getId())
              .matrixBankMappingId(mapping.getId())
              .orderIndex(context.nextOrderIndex())
              .pointsOverride(mapping.getPointsPerQuestion())
              .build());

      context.incrementOrderIndex();
      context.usedQuestionIds().add(selected.getId());
      context.totalPoints +=
          mapping.getPointsPerQuestion() != null ? mapping.getPointsPerQuestion().intValue() : 0;
    }
  }

  private List<Question> selectQuestionsForCellWithChapter(
      UUID assessmentId,
      UUID questionBankId,
      UUID chapterId,
      ExamMatrixBankMapping mapping,
      int required,
      Set<UUID> usedQuestionIds) {
    List<UUID> candidateIds =
        findCandidateIdsByBankAndChapterAndCognitive(questionBankId, chapterId, mapping);

    candidateIds.removeIf(usedQuestionIds::contains);
    deterministicShuffle(candidateIds, assessmentId, mapping.getId());

    if (candidateIds.size() < required) {
      log.error(
          "Insufficient questions after filtering: bank={}, chapter={}, cognitive={}, required={}, available={}",
          questionBankId,
          chapterId,
          mapping.getCognitiveLevel(),
          required,
          candidateIds.size());
      throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
    }

    return fetchQuestionsInOrder(candidateIds.subList(0, required));
  }

  private long countAvailableByBankAndChapterAndCognitive(
      UUID bankId, UUID chapterId, ExamMatrixBankMapping mapping) {
    String cognitiveLevel =
        mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null;
    if (cognitiveLevel == null) {
      return 0;
    }
    return questionRepository.countApprovedByBankAndChapterAndCognitive(
        bankId, chapterId, cognitiveLevel);
  }

  private List<UUID> findCandidateIdsByBankAndChapterAndCognitive(
      UUID bankId, UUID chapterId, ExamMatrixBankMapping mapping) {
    String cognitiveLevel =
        mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null;
    if (cognitiveLevel == null) {
      return new ArrayList<>();
    }
    return questionRepository.findApprovedIdsByBankAndChapterAndCognitive(
        bankId, chapterId, cognitiveLevel);
  }

  // ========================================================================
  // Helper methods
  // ========================================================================

  private int requiredQuestions(ExamMatrixBankMapping mapping) {
    return mapping.getQuestionCount() != null ? Math.max(0, mapping.getQuestionCount()) : 0;
  }

  private List<Question> fetchQuestionsInOrder(List<UUID> selectedIds) {
    Map<UUID, Question> questionById = new HashMap<>();
    for (Question q : questionRepository.findAllById(selectedIds)) {
      questionById.put(q.getId(), q);
    }

    List<Question> ordered = new ArrayList<>(selectedIds.size());
    for (UUID questionId : selectedIds) {
      Question q = questionById.get(questionId);
      if (q == null) {
        throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
      }
      ordered.add(q);
    }
    return ordered;
  }

  private void deterministicShuffle(List<UUID> questionIds, UUID assessmentId, UUID mappingId) {
    long seed = Objects.hash(assessmentId, mappingId);
    Random random = new Random(seed);

    for (int i = questionIds.size() - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      UUID tmp = questionIds.get(i);
      questionIds.set(i, questionIds.get(j));
      questionIds.set(j, tmp);
    }
  }

  private static final class SelectionContext {
    private final List<AssessmentQuestion> plannedQuestions;
    private final Set<UUID> usedQuestionIds;
    private int nextOrderIndex;
    private int totalPoints;

    private SelectionContext(
        List<AssessmentQuestion> plannedQuestions,
        Set<UUID> usedQuestionIds,
        int nextOrderIndex,
        int totalPoints) {
      this.plannedQuestions = plannedQuestions;
      this.usedQuestionIds = usedQuestionIds;
      this.nextOrderIndex = nextOrderIndex;
      this.totalPoints = totalPoints;
    }

    private List<AssessmentQuestion> plannedQuestions() {
      return plannedQuestions;
    }

    private Set<UUID> usedQuestionIds() {
      return usedQuestionIds;
    }

    private int nextOrderIndex() {
      return nextOrderIndex;
    }

    private void incrementOrderIndex() {
      this.nextOrderIndex++;
    }

    private int totalPoints() {
      return totalPoints;
    }
  }
}
