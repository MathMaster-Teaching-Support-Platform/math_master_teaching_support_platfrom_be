package com.fptu.math_master.service.impl;

import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.ExamMatrixBankMapping;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.service.QuestionSelectionService;
import java.util.ArrayList;
import java.util.EnumMap;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionSelectionServiceImpl implements QuestionSelectionService {

  ExamMatrixBankMappingRepository examMatrixBankMappingRepository;
  QuestionRepository questionRepository;

  @Override
  public void validateAvailability(UUID examMatrixId) {
    List<ExamMatrixBankMapping> mappings =
        examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(examMatrixId);

    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
    }

    for (ExamMatrixBankMapping mapping : mappings) {
      int required = totalRequiredQuestions(normalizeDistribution(mapping));
      if (required <= 0) {
        continue;
      }

      long available = countAvailableByBankAndCognitive(mapping);
      if (available < required) {
        throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
      }
    }
  }

  @Override
  public SelectionPlan buildSelectionPlan(UUID assessmentId, UUID examMatrixId, int startOrderIndex) {
    validateAvailability(examMatrixId);

    List<ExamMatrixBankMapping> mappings =
        examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(examMatrixId);

    SelectionContext context =
        new SelectionContext(new ArrayList<>(), new HashSet<>(), startOrderIndex, 0);

    for (ExamMatrixBankMapping mapping : mappings) {
      int required = totalRequiredQuestions(normalizeDistribution(mapping));
      appendSelectionFromMapping(assessmentId, mapping, required, context);
    }

    return new SelectionPlan(context.plannedQuestions(), context.totalPoints());
  }

  private void appendSelectionFromMapping(
      UUID assessmentId,
      ExamMatrixBankMapping mapping,
      int required,
      SelectionContext context) {
    if (required <= 0) {
      return;
    }

    List<Question> selectedQuestions =
        selectQuestionsForCell(assessmentId, mapping, required, context.usedQuestionIds());

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

  private List<Question> selectQuestionsForCell(
      UUID assessmentId,
      ExamMatrixBankMapping mapping,
      int required,
      Set<UUID> usedQuestionIds) {
    List<UUID> candidateIds = findCandidateIdsByBankAndCognitive(mapping);

    candidateIds.removeIf(usedQuestionIds::contains);
    deterministicShuffle(candidateIds, assessmentId, mapping.getId());

    if (candidateIds.size() < required) {
      throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
    }

    return fetchQuestionsInOrder(candidateIds.subList(0, required));
  }

  private long countAvailableByBankAndCognitive(ExamMatrixBankMapping mapping) {
    String cognitiveLevel = mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null;
    return questionRepository.countApprovedByBankAndDifficultyAndCognitiveAndTopic(
        mapping.getQuestionBankId(), null, cognitiveLevel, null);
  }

  private List<UUID> findCandidateIdsByBankAndCognitive(ExamMatrixBankMapping mapping) {
    String cognitiveLevel = mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null;
    return questionRepository.findApprovedIdsByBankAndDifficultyAndCognitiveAndTopic(
        mapping.getQuestionBankId(), null, cognitiveLevel, null);
  }

  private int totalRequiredQuestions(Map<QuestionDifficulty, Integer> distribution) {
    return distribution.values().stream().mapToInt(Integer::intValue).sum();
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

  private Map<QuestionDifficulty, Integer> normalizeDistribution(ExamMatrixBankMapping mapping) {
    Map<QuestionDifficulty, Integer> normalized = new EnumMap<>(QuestionDifficulty.class);
    Map<QuestionDifficulty, Integer> source = mapping.getDifficultyDistribution();

    normalized.put(
        QuestionDifficulty.EASY,
        source != null ? Math.max(0, source.getOrDefault(QuestionDifficulty.EASY, 0)) : 0);
    normalized.put(
        QuestionDifficulty.MEDIUM,
        source != null ? Math.max(0, source.getOrDefault(QuestionDifficulty.MEDIUM, 0)) : 0);
    normalized.put(
        QuestionDifficulty.HARD,
        source != null ? Math.max(0, source.getOrDefault(QuestionDifficulty.HARD, 0)) : 0);

    return normalized;
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
