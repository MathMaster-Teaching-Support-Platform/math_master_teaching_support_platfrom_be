package com.fptu.math_master.service.impl;

import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.ExamMatrixBankMapping;
import com.fptu.math_master.entity.ExamMatrixRow;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.service.QuestionSelectionService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    // ISSUE-04: Separate TF and non-TF mappings for validation
    Map<UUID, List<ExamMatrixBankMapping>> tfByRow = new LinkedHashMap<>();
    List<ExamMatrixBankMapping> nonTFValidation = new ArrayList<>();

    for (ExamMatrixBankMapping mapping : mappings) {
      if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
        tfByRow.computeIfAbsent(mapping.getMatrixRowId(), k -> new ArrayList<>()).add(mapping);
      } else {
        nonTFValidation.add(mapping);
      }
    }

    // Validate non-TF as before
    for (ExamMatrixBankMapping mapping : nonTFValidation) {
      int required = requiredQuestions(mapping);
      if (required <= 0) {
        continue;
      }

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

    // ISSUE-04: Validate TF: aggregate clauses per row → check whole question availability
    for (Map.Entry<UUID, List<ExamMatrixBankMapping>> entry : tfByRow.entrySet()) {
      ExamMatrixRow row = rowById.get(entry.getKey());
      if (row == null || row.getChapterId() == null) {
        log.warn("TF row {} has no chapter - skipping validation", entry.getKey());
        continue;
      }

      int totalClauses = entry.getValue().stream()
          .mapToInt(this::requiredQuestions).sum();
      int tfQuestionsNeeded = (int) Math.ceil(totalClauses / 4.0);

      // Count ALL TF questions for this chapter (any clause cognitive level)
      Set<UUID> allTFIds = new HashSet<>();
      for (ExamMatrixBankMapping m : entry.getValue()) {
        String level = m.getCognitiveLevel() != null ? m.getCognitiveLevel().name() : null;
        if (level == null) continue;
        allTFIds.addAll(questionRepository.findTFIdsByBankAndChapterAndClauseCognitive(
            questionBankId, row.getChapterId(), level));
      }

      if (allTFIds.size() < tfQuestionsNeeded) {
        log.error(
            "Insufficient TF questions: chapter={}, clausesNeeded={}, questionsNeeded={}, available={}",
            row.getChapterId(), totalClauses, tfQuestionsNeeded, allTFIds.size());
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

    // BUG-4 FIX: Sort mappings by partNumber then cognitiveLevel to ensure correct question order
    mappings.sort(Comparator
        .comparingInt(ExamMatrixBankMapping::getPartNumber)
        .thenComparing(m -> m.getCognitiveLevel().ordinal()));

    // Build a map of matrixRowId -> ExamMatrixRow for chapter lookup
    Map<UUID, ExamMatrixRow> rowById = new HashMap<>();
    for (ExamMatrixRow row : examMatrixRowRepository.findByExamMatrixId(examMatrixId)) {
      rowById.put(row.getId(), row);
    }

    SelectionContext context =
        new SelectionContext(new ArrayList<>(), new HashSet<>(), startOrderIndex, 0);

    // ISSUE-04: Separate TF and non-TF mappings
    List<ExamMatrixBankMapping> nonTFMappings = new ArrayList<>();
    // Group TF mappings by matrixRowId
    Map<UUID, List<ExamMatrixBankMapping>> tfMappingsByRow = new LinkedHashMap<>();

    for (ExamMatrixBankMapping mapping : mappings) {
      if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
        tfMappingsByRow
            .computeIfAbsent(mapping.getMatrixRowId(), k -> new ArrayList<>())
            .add(mapping);
      } else {
        nonTFMappings.add(mapping);
      }
    }

    // Process non-TF mappings as before (1 mapping = 1 selection round)
    for (ExamMatrixBankMapping mapping : nonTFMappings) {
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

    // ISSUE-04: Process TF mappings: aggregate clause requirements per row → select whole questions
    for (Map.Entry<UUID, List<ExamMatrixBankMapping>> entry : tfMappingsByRow.entrySet()) {
      UUID matrixRowId = entry.getKey();
      List<ExamMatrixBankMapping> tfMappings = entry.getValue();
      ExamMatrixRow row = rowById.get(matrixRowId);
      if (row == null || row.getChapterId() == null) {
        log.warn("TF row {} has no chapter - skipping", matrixRowId);
        continue;
      }
      appendTFSelectionForRow(
          assessmentId, questionBankId, row.getChapterId(), tfMappings, context);
    }

    return new SelectionPlan(context.plannedQuestions(), context.totalPoints());
  }

  // ========================================================================
  // TF Clause-Level Selection (ISSUE-04)
  // ========================================================================

  /**
   * TF clause-level selection.
   * Matrix cells for TF represent CLAUSE counts, not question counts.
   * Example: NB=2, TH=1, VD=1 means 4 total clauses = 1 TF question.
   * 
   * Algorithm:
   * 1. Sum all clause requirements: totalClausesNeeded = sum of all questionCount
   * 2. Calculate whole TF questions needed: ceil(totalClausesNeeded / 4)
   * 3. Find TF questions in the bank for this chapter that have at least one
   *    matching clause for ANY of the required cognitive levels
   * 4. Score each candidate by how well its clause composition matches the requirement
   * 5. Select the best-matching questions
   */
  private void appendTFSelectionForRow(
      UUID assessmentId,
      UUID questionBankId,
      UUID chapterId,
      List<ExamMatrixBankMapping> tfMappings,
      SelectionContext context) {

    // Step 1: Build clause requirement map
    Map<String, Integer> clauseRequirement = new LinkedHashMap<>();
    int totalClausesNeeded = 0;
    ExamMatrixBankMapping firstMapping = tfMappings.get(0); // For pointsPerQuestion

    for (ExamMatrixBankMapping mapping : tfMappings) {
      int count = requiredQuestions(mapping); // This is CLAUSE count for TF
      if (count <= 0) continue;
      String level = mapping.getCognitiveLevel().name();
      clauseRequirement.merge(level, count, Integer::sum);
      totalClausesNeeded += count;
    }

    if (totalClausesNeeded <= 0) return;

    // Step 2: Calculate whole TF questions needed (each has 4 clauses)
    int tfQuestionsNeeded = (int) Math.ceil(totalClausesNeeded / 4.0);

    log.info(
        "TF clause selection: chapter={}, clauseRequirement={}, totalClauses={}, questionsNeeded={}",
        chapterId, clauseRequirement, totalClausesNeeded, tfQuestionsNeeded);

    // Step 3: Find ALL TF candidate questions for this chapter
    // Use a union of all cognitive levels to get the widest candidate pool
    Set<UUID> allCandidateIds = new LinkedHashSet<>();
    for (String level : clauseRequirement.keySet()) {
      List<UUID> idsForLevel = questionRepository.findTFIdsByBankAndChapterAndClauseCognitive(
          questionBankId, chapterId, level);
      allCandidateIds.addAll(idsForLevel);
    }

    // Remove already-used questions
    allCandidateIds.removeIf(context.usedQuestionIds()::contains);

    if (allCandidateIds.size() < tfQuestionsNeeded) {
      log.error(
          "Insufficient TF questions: chapter={}, clauseReq={}, needed={} questions, available={}",
          chapterId, clauseRequirement, tfQuestionsNeeded, allCandidateIds.size());
      throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
    }

    // Step 4: Score candidates by clause composition match
    List<UUID> candidateList = new ArrayList<>(allCandidateIds);
    Map<UUID, Question> candidateMap = new HashMap<>();
    for (Question q : questionRepository.findAllById(candidateList)) {
      candidateMap.put(q.getId(), q);
    }

    // Score each candidate: how many of its clauses match the required levels
    List<UUID> scoredCandidates = new ArrayList<>(candidateList);
    scoredCandidates.sort((a, b) -> {
      int scoreA = scoreTFQuestion(candidateMap.get(a), clauseRequirement);
      int scoreB = scoreTFQuestion(candidateMap.get(b), clauseRequirement);
      return Integer.compare(scoreB, scoreA); // Higher score first
    });

    // Add deterministic shuffle among equally-scored candidates
    deterministicShuffle(scoredCandidates, assessmentId,
        firstMapping != null ? firstMapping.getId() : UUID.randomUUID());

    // Re-sort by score after shuffle to maintain best-match priority
    scoredCandidates.sort((a, b) -> {
      int scoreA = scoreTFQuestion(candidateMap.get(a), clauseRequirement);
      int scoreB = scoreTFQuestion(candidateMap.get(b), clauseRequirement);
      return Integer.compare(scoreB, scoreA);
    });

    // Step 5: Select top N questions
    List<UUID> selected = scoredCandidates.subList(0, Math.min(tfQuestionsNeeded, scoredCandidates.size()));

    for (UUID questionId : selected) {
      context.plannedQuestions().add(
          AssessmentQuestion.builder()
              .assessmentId(assessmentId)
              .questionId(questionId)
              .matrixBankMappingId(firstMapping.getId())
              .orderIndex(context.nextOrderIndex())
              .pointsOverride(firstMapping.getPointsPerQuestion())
              .build());

      context.incrementOrderIndex();
      context.usedQuestionIds().add(questionId);
      context.totalPoints +=
          firstMapping.getPointsPerQuestion() != null
              ? firstMapping.getPointsPerQuestion().intValue() : 0;
    }

    log.info("Selected {} TF questions for chapter {} (clause requirement: {})",
        selected.size(), chapterId, clauseRequirement);
  }

  /**
   * Score a TF question by how well its clause cognitive levels match the requirement.
   * Higher score = better match.
   *
   * Reads generation_metadata -> tfClauses -> {A: {cognitiveLevel: "NHAN_BIET"}, ...}
   */
  private int scoreTFQuestion(Question question, Map<String, Integer> clauseRequirement) {
    if (question == null || question.getGenerationMetadata() == null) return 0;

    @SuppressWarnings("unchecked")
    Map<String, Object> tfClauses =
        (Map<String, Object>) question.getGenerationMetadata().get("tfClauses");
    if (tfClauses == null) return 0;

    int score = 0;
    // Count how many of this question's clauses match any required level
    for (Map.Entry<String, Object> clauseEntry : tfClauses.entrySet()) {
      if (clauseEntry.getValue() instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> clauseData = (Map<String, Object>) clauseEntry.getValue();
        String clauseLevel = (String) clauseData.get("cognitiveLevel");
        if (clauseLevel != null && clauseRequirement.containsKey(clauseLevel)) {
          score++; // Each matching clause adds 1 to the score
        }
      }
    }
    return score;
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
    
    // Phase 5: Include questionType in the count query
    String questionType =
        mapping.getQuestionType() != null ? mapping.getQuestionType().name() : null;
    if (questionType == null) {
      return 0;
    }

    // Finale2: TF clause-level matching via JSONB generation_metadata
    if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
      return questionRepository.countTFByBankAndChapterAndClauseCognitive(
          bankId, chapterId, cognitiveLevel);
    }
    
    return questionRepository.countApprovedByBankAndChapterAndCognitiveAndType(
        bankId, chapterId, cognitiveLevel, questionType);
  }

  private List<UUID> findCandidateIdsByBankAndChapterAndCognitive(
      UUID bankId, UUID chapterId, ExamMatrixBankMapping mapping) {
    String cognitiveLevel =
        mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null;
    if (cognitiveLevel == null) {
      return new ArrayList<>();
    }
    
    // Phase 5: Include questionType in the ID lookup query
    String questionType =
        mapping.getQuestionType() != null ? mapping.getQuestionType().name() : null;
    if (questionType == null) {
      return new ArrayList<>();
    }

    // Finale2: TF clause-level matching via JSONB generation_metadata
    if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
      return questionRepository.findTFIdsByBankAndChapterAndClauseCognitive(
          bankId, chapterId, cognitiveLevel);
    }
    
    return questionRepository.findApprovedIdsByBankAndChapterAndCognitiveAndType(
        bankId, chapterId, cognitiveLevel, questionType);
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
