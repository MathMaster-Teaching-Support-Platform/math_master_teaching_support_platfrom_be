package com.fptu.math_master.service.impl;

import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.entity.ExamMatrixBankMapping;
import com.fptu.math_master.entity.ExamMatrixRow;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.enums.QuestionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.ExamMatrixBankMappingRepository;
import com.fptu.math_master.repository.ExamMatrixRepository;
import com.fptu.math_master.repository.ExamMatrixRowRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.service.QuestionSelectionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
  ChapterRepository chapterRepository;

  // ========================================================================
  // Public entrypoints
  // ========================================================================

  @Override
  public void validateAvailability(UUID examMatrixId, Collection<UUID> overrideBankIds) {
    ExamMatrix matrix =
        examMatrixRepository
            .findById(examMatrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    Set<UUID> bankIds = resolveQuestionBankIds(matrix, overrideBankIds);

    List<ExamMatrixBankMapping> mappings =
        examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(examMatrixId);

    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.MATRIX_VALIDATION_FAILED);
    }

    Map<UUID, ExamMatrixRow> rowById = loadRowsById(examMatrixId);
    Map<UUID, List<ExamMatrixBankMapping>> tfByRow = new LinkedHashMap<>();
    List<ExamMatrixBankMapping> nonTFValidation = new ArrayList<>();

    for (ExamMatrixBankMapping mapping : mappings) {
      if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
        tfByRow.computeIfAbsent(mapping.getMatrixRowId(), k -> new ArrayList<>()).add(mapping);
      } else {
        nonTFValidation.add(mapping);
      }
    }

    for (ExamMatrixBankMapping mapping : nonTFValidation) {
      int required = requiredQuestions(mapping);
      if (required <= 0) continue;

      ExamMatrixRow row = rowById.get(mapping.getMatrixRowId());
      if (row == null || row.getChapterId() == null) {
        log.warn("Mapping {} missing row/chapter — skipping validation", mapping.getId());
        continue;
      }

      long available = countAvailable(bankIds, row.getChapterId(), mapping);
      if (available < required) {
        log.error(
            "Insufficient questions: banks={}, chapter={}, cognitive={}, required={}, available={}",
            bankIds, row.getChapterId(), mapping.getCognitiveLevel(), required, available);
        throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
      }
    }

    for (Map.Entry<UUID, List<ExamMatrixBankMapping>> entry : tfByRow.entrySet()) {
      ExamMatrixRow row = rowById.get(entry.getKey());
      if (row == null || row.getChapterId() == null) {
        log.warn("TF row {} missing chapter — skipping validation", entry.getKey());
        continue;
      }

      int totalClauses = entry.getValue().stream().mapToInt(this::requiredQuestions).sum();
      int tfQuestionsNeeded = (int) Math.ceil(totalClauses / 4.0);

      Set<UUID> allTFIds = new HashSet<>();
      for (ExamMatrixBankMapping m : entry.getValue()) {
        String level = m.getCognitiveLevel() != null ? m.getCognitiveLevel().name() : null;
        if (level == null) continue;
        allTFIds.addAll(
            questionRepository.findTFIdsByBanksAndChapterAndClauseCognitive(
                bankIds, row.getChapterId(), level));
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
  public SelectionPlan buildSelectionPlan(
      UUID assessmentId,
      UUID examMatrixId,
      Collection<UUID> overrideBankIds,
      int startOrderIndex) {
    validateAvailability(examMatrixId, overrideBankIds);

    ExamMatrix matrix =
        examMatrixRepository
            .findById(examMatrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    Set<UUID> bankIds = resolveQuestionBankIds(matrix, overrideBankIds);

    List<ExamMatrixBankMapping> mappings =
        examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(examMatrixId);

    mappings.sort(
        Comparator.comparingInt(ExamMatrixBankMapping::getPartNumber)
            .thenComparing(m -> m.getCognitiveLevel().ordinal()));

    Map<UUID, ExamMatrixRow> rowById = loadRowsById(examMatrixId);

    SelectionContext context =
        new SelectionContext(new ArrayList<>(), new HashSet<>(), startOrderIndex, 0);

    List<ExamMatrixBankMapping> nonTFMappings = new ArrayList<>();
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

    for (ExamMatrixBankMapping mapping : nonTFMappings) {
      int required = requiredQuestions(mapping);
      ExamMatrixRow row = rowById.get(mapping.getMatrixRowId());

      if (row == null || row.getChapterId() == null) {
        log.warn("Mapping {} missing row/chapter — skipping", mapping.getId());
        continue;
      }

      appendSelectionFromMapping(
          assessmentId, bankIds, row.getChapterId(), mapping, required, context);
    }

    for (Map.Entry<UUID, List<ExamMatrixBankMapping>> entry : tfMappingsByRow.entrySet()) {
      ExamMatrixRow row = rowById.get(entry.getKey());
      if (row == null || row.getChapterId() == null) {
        log.warn("TF row {} missing chapter — skipping", entry.getKey());
        continue;
      }
      appendTFSelectionForRow(
          assessmentId, bankIds, row.getChapterId(), entry.getValue(), context);
    }

    return new SelectionPlan(context.plannedQuestions(), context.totalPoints());
  }

  @Override
  public CoverageReport computeCoverage(UUID examMatrixId, Collection<UUID> bankIds) {
    ExamMatrix matrix =
        examMatrixRepository
            .findById(examMatrixId)
            .orElseThrow(() -> new AppException(ErrorCode.EXAM_MATRIX_NOT_FOUND));

    Set<UUID> resolvedBanks = resolveQuestionBankIds(matrix, bankIds);

    List<ExamMatrixBankMapping> mappings =
        examMatrixBankMappingRepository.findByExamMatrixIdOrderByCreatedAt(examMatrixId);

    Map<UUID, ExamMatrixRow> rowById = loadRowsById(examMatrixId);

    // Pre-load chapter titles for friendlier gap messages
    Set<UUID> chapterIds = new HashSet<>();
    for (ExamMatrixRow row : rowById.values()) {
      if (row.getChapterId() != null) chapterIds.add(row.getChapterId());
    }
    Map<UUID, String> chapterTitleById = new HashMap<>();
    if (!chapterIds.isEmpty()) {
      for (Chapter ch : chapterRepository.findAllById(chapterIds)) {
        chapterTitleById.put(ch.getId(), ch.getTitle());
      }
    }

    // Aggregate by (chapter, cognitive, type). Non-TF cells map 1:1 to a gap;
    // TF cells aggregate per row because TF generation is whole-question.
    List<Gap> gaps = new ArrayList<>();
    Map<UUID, List<ExamMatrixBankMapping>> tfByRow = new LinkedHashMap<>();

    for (ExamMatrixBankMapping mapping : mappings) {
      int required = requiredQuestions(mapping);
      if (required <= 0) continue;

      ExamMatrixRow row = rowById.get(mapping.getMatrixRowId());
      if (row == null || row.getChapterId() == null) continue;

      if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
        tfByRow.computeIfAbsent(mapping.getMatrixRowId(), k -> new ArrayList<>()).add(mapping);
        continue;
      }

      long available = countAvailable(resolvedBanks, row.getChapterId(), mapping);
      gaps.add(
          new Gap(
              row.getChapterId(),
              chapterTitleById.get(row.getChapterId()),
              mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null,
              mapping.getQuestionType() != null ? mapping.getQuestionType().name() : null,
              required,
              available));
    }

    for (Map.Entry<UUID, List<ExamMatrixBankMapping>> entry : tfByRow.entrySet()) {
      ExamMatrixRow row = rowById.get(entry.getKey());
      if (row == null || row.getChapterId() == null) continue;

      int totalClauses = entry.getValue().stream().mapToInt(this::requiredQuestions).sum();
      int tfQuestionsNeeded = (int) Math.ceil(totalClauses / 4.0);

      Set<UUID> tfIds = new HashSet<>();
      for (ExamMatrixBankMapping m : entry.getValue()) {
        String level = m.getCognitiveLevel() != null ? m.getCognitiveLevel().name() : null;
        if (level == null) continue;
        tfIds.addAll(
            questionRepository.findTFIdsByBanksAndChapterAndClauseCognitive(
                resolvedBanks, row.getChapterId(), level));
      }

      // Synthetic TF gap: needed = whole TF questions, available = union of TF candidates
      gaps.add(
          new Gap(
              row.getChapterId(),
              chapterTitleById.get(row.getChapterId()),
              "TRUE_FALSE_CLAUSES",
              QuestionType.TRUE_FALSE.name(),
              tfQuestionsNeeded,
              tfIds.size()));
    }

    boolean ok = gaps.stream().allMatch(g -> g.available() >= g.required());
    return new CoverageReport(ok, gaps);
  }

  // ========================================================================
  // Selection helpers — multi-bank
  // ========================================================================

  private void appendSelectionFromMapping(
      UUID assessmentId,
      Collection<UUID> bankIds,
      UUID chapterId,
      ExamMatrixBankMapping mapping,
      int required,
      SelectionContext context) {
    if (required <= 0) return;

    List<Question> selectedQuestions =
        selectQuestionsForCell(
            assessmentId, bankIds, chapterId, mapping, required, context.usedQuestionIds());

    for (Question selected : selectedQuestions) {
      context
          .plannedQuestions()
          .add(
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
      Collection<UUID> bankIds,
      UUID chapterId,
      ExamMatrixBankMapping mapping,
      int required,
      Set<UUID> usedQuestionIds) {
    List<UUID> candidateIds = findCandidateIds(bankIds, chapterId, mapping);

    candidateIds.removeIf(usedQuestionIds::contains);
    deterministicShuffle(candidateIds, assessmentId, mapping.getId());

    if (candidateIds.size() < required) {
      log.error(
          "Insufficient questions after filtering: banks={}, chapter={}, cognitive={}, required={}, available={}",
          bankIds, chapterId, mapping.getCognitiveLevel(), required, candidateIds.size());
      throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
    }

    return fetchQuestionsInOrder(candidateIds.subList(0, required));
  }

  private long countAvailable(
      Collection<UUID> bankIds, UUID chapterId, ExamMatrixBankMapping mapping) {
    String cognitiveLevel =
        mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null;
    if (cognitiveLevel == null) return 0;

    String questionType =
        mapping.getQuestionType() != null ? mapping.getQuestionType().name() : null;
    if (questionType == null) return 0;

    if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
      return questionRepository.countTFByBanksAndChapterAndClauseCognitive(
          bankIds, chapterId, cognitiveLevel);
    }

    return questionRepository.countApprovedByBanksAndChapterAndCognitiveAndType(
        bankIds, chapterId, cognitiveLevel, questionType);
  }

  private List<UUID> findCandidateIds(
      Collection<UUID> bankIds, UUID chapterId, ExamMatrixBankMapping mapping) {
    String cognitiveLevel =
        mapping.getCognitiveLevel() != null ? mapping.getCognitiveLevel().name() : null;
    if (cognitiveLevel == null) return new ArrayList<>();

    String questionType =
        mapping.getQuestionType() != null ? mapping.getQuestionType().name() : null;
    if (questionType == null) return new ArrayList<>();

    if (mapping.getQuestionType() == QuestionType.TRUE_FALSE) {
      return new ArrayList<>(
          questionRepository.findTFIdsByBanksAndChapterAndClauseCognitive(
              bankIds, chapterId, cognitiveLevel));
    }

    return new ArrayList<>(
        questionRepository.findApprovedIdsByBanksAndChapterAndCognitiveAndType(
            bankIds, chapterId, cognitiveLevel, questionType));
  }

  // ========================================================================
  // TF clause-level selection (multi-bank)
  // ========================================================================

  private void appendTFSelectionForRow(
      UUID assessmentId,
      Collection<UUID> bankIds,
      UUID chapterId,
      List<ExamMatrixBankMapping> tfMappings,
      SelectionContext context) {

    Map<String, Integer> clauseRequirement = new LinkedHashMap<>();
    int totalClausesNeeded = 0;
    ExamMatrixBankMapping firstMapping = tfMappings.get(0);

    for (ExamMatrixBankMapping mapping : tfMappings) {
      int count = requiredQuestions(mapping);
      if (count <= 0) continue;
      String level = mapping.getCognitiveLevel().name();
      clauseRequirement.merge(level, count, Integer::sum);
      totalClausesNeeded += count;
    }

    if (totalClausesNeeded <= 0) return;

    int tfQuestionsNeeded = (int) Math.ceil(totalClausesNeeded / 4.0);

    log.info(
        "TF clause selection: chapter={}, clauseRequirement={}, totalClauses={}, questionsNeeded={}",
        chapterId, clauseRequirement, totalClausesNeeded, tfQuestionsNeeded);

    Set<UUID> allCandidateIds = new LinkedHashSet<>();
    for (String level : clauseRequirement.keySet()) {
      allCandidateIds.addAll(
          questionRepository.findTFIdsByBanksAndChapterAndClauseCognitive(
              bankIds, chapterId, level));
    }

    allCandidateIds.removeIf(context.usedQuestionIds()::contains);

    if (allCandidateIds.size() < tfQuestionsNeeded) {
      log.error(
          "Insufficient TF questions: chapter={}, clauseReq={}, needed={} questions, available={}",
          chapterId, clauseRequirement, tfQuestionsNeeded, allCandidateIds.size());
      throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
    }

    List<UUID> candidateList = new ArrayList<>(allCandidateIds);
    Map<UUID, Question> candidateMap = new HashMap<>();
    for (Question q : questionRepository.findAllById(candidateList)) {
      candidateMap.put(q.getId(), q);
    }

    List<UUID> scoredCandidates = new ArrayList<>(candidateList);
    scoredCandidates.sort(
        (a, b) -> {
          int scoreA = scoreTFQuestion(candidateMap.get(a), chapterId, clauseRequirement);
          int scoreB = scoreTFQuestion(candidateMap.get(b), chapterId, clauseRequirement);
          return Integer.compare(scoreB, scoreA);
        });

    deterministicShuffle(
        scoredCandidates, assessmentId, firstMapping != null ? firstMapping.getId() : UUID.randomUUID());

    scoredCandidates.sort(
        (a, b) -> {
          int scoreA = scoreTFQuestion(candidateMap.get(a), chapterId, clauseRequirement);
          int scoreB = scoreTFQuestion(candidateMap.get(b), chapterId, clauseRequirement);
          return Integer.compare(scoreB, scoreA);
        });

    List<UUID> selected =
        scoredCandidates.subList(0, Math.min(tfQuestionsNeeded, scoredCandidates.size()));

    Map<String, Integer> totalSelectedComposition = new HashMap<>();
    for (UUID questionId : selected) {
      Question q = candidateMap.get(questionId);
      readClauseComposition(q, chapterId)
          .forEach((level, count) -> totalSelectedComposition.merge(level, count, Integer::sum));
    }

    for (Map.Entry<String, Integer> required : clauseRequirement.entrySet()) {
      int needed = required.getValue();
      int actual = totalSelectedComposition.getOrDefault(required.getKey(), 0);
      if (actual < needed) {
        log.error(
            "Selected TF questions do not fulfill matrix requirements for chapter {}. "
                + "Level {} needs {} but selected questions only provide {}.",
            chapterId, required.getKey(), needed, actual);
        throw new AppException(ErrorCode.INSUFFICIENT_QUESTIONS_AVAILABLE);
      }
    }

    for (UUID questionId : selected) {
      int slotIndex = context.nextOrderIndex();
      context
          .plannedQuestions()
          .add(
              AssessmentQuestion.builder()
                  .assessmentId(assessmentId)
                  .questionId(questionId)
                  .matrixBankMappingId(firstMapping.getId())
                  .orderIndex(slotIndex)
                  .pointsOverride(firstMapping.getPointsPerQuestion())
                  .build());

      context.incrementOrderIndex();
      context.usedQuestionIds().add(questionId);
      context.totalPoints +=
          firstMapping.getPointsPerQuestion() != null
              ? firstMapping.getPointsPerQuestion().intValue()
              : 0;
    }
  }

  private int scoreTFQuestion(
      Question question, UUID chapterId, Map<String, Integer> clauseRequirement) {
    if (question == null) return 0;

    Map<String, Integer> actualComposition = readClauseComposition(question, chapterId);
    if (actualComposition.isEmpty()) return 0;

    boolean isPerfectMatch = true;
    int matchingClauses = 0;

    for (Map.Entry<String, Integer> required : clauseRequirement.entrySet()) {
      String level = required.getKey();
      int requiredCount = required.getValue();
      int actualCount = actualComposition.getOrDefault(level, 0);

      if (actualCount < requiredCount) {
        isPerfectMatch = false;
        matchingClauses += actualCount;
      } else if (actualCount == requiredCount) {
        matchingClauses += actualCount;
      } else {
        isPerfectMatch = false;
        matchingClauses += requiredCount;
      }
    }

    for (String level : actualComposition.keySet()) {
      if (!clauseRequirement.containsKey(level)) {
        isPerfectMatch = false;
      }
    }

    if (matchingClauses == 0) return 0;
    return isPerfectMatch ? 1000 + matchingClauses : matchingClauses;
  }

  /**
   * Read the clause composition of a TF question, with a fallback for legacy
   * questions that don't carry {@code generation_metadata->'tfClauses'}.
   *
   * <p>When metadata is present we count clauses per cognitiveLevel (filtered
   * by chapter when provided), folding English Bloom's labels onto the
   * Vietnamese 4-tier scheme. When metadata is absent we approximate the
   * question as 4 clauses at the question's own {@code cognitive_level} —
   * required so legacy/hand-imported TF questions can still satisfy the
   * matrix's strict clause requirement at generate time.
   */
  private Map<String, Integer> readClauseComposition(Question question, UUID chapterId) {
    Map<String, Integer> composition = new HashMap<>();
    if (question == null) return composition;

    Map<String, Object> metadata = question.getGenerationMetadata();
    if (metadata != null) {
      Object raw = metadata.get("tfClauses");
      if (raw instanceof Map<?, ?> tfClauses && !tfClauses.isEmpty()) {
        for (Map.Entry<?, ?> clauseEntry : tfClauses.entrySet()) {
          if (!(clauseEntry.getValue() instanceof Map<?, ?> clauseData)) continue;
          Object clauseChapterId = clauseData.get("chapterId");
          if (chapterId != null
              && clauseChapterId != null
              && !chapterId.toString().equals(clauseChapterId.toString())) continue;
          Object clauseLevel = clauseData.get("cognitiveLevel");
          if (clauseLevel != null) {
            String folded = foldCognitiveLevel(clauseLevel.toString());
            if (folded != null) composition.merge(folded, 1, Integer::sum);
          }
        }
        if (!composition.isEmpty()) return composition;
      }
    }

    if (question.getCognitiveLevel() != null) {
      String folded = foldCognitiveLevel(question.getCognitiveLevel().name());
      if (folded != null) composition.put(folded, 4);
    }
    return composition;
  }

  private static String foldCognitiveLevel(String raw) {
    if (raw == null) return null;
    return switch (raw) {
      case "REMEMBER" -> "NHAN_BIET";
      case "UNDERSTAND" -> "THONG_HIEU";
      case "APPLY", "ANALYZE" -> "VAN_DUNG";
      case "EVALUATE", "CREATE" -> "VAN_DUNG_CAO";
      default -> raw;
    };
  }

  // ========================================================================
  // Misc helpers
  // ========================================================================

  /**
   * Resolve which question banks to draw from.
   *
   * <p>Order of preference:
   * <ol>
   *   <li>Explicit non-empty {@code overrideBankIds} from the request — the
   *       new "matrix is a blueprint, pick banks at generate time" flow.</li>
   *   <li>Matrix's stored default {@code questionBankId} as a singleton —
   *       backward compatibility for legacy matrices.</li>
   * </ol>
   *
   * <p>If neither is set we fail fast with {@link ErrorCode#QUESTION_BANK_REQUIRED}.
   */
  private Set<UUID> resolveQuestionBankIds(
      ExamMatrix matrix, Collection<UUID> overrideBankIds) {
    if (overrideBankIds != null && !overrideBankIds.isEmpty()) {
      Set<UUID> deduped = new LinkedHashSet<>();
      for (UUID b : overrideBankIds) if (b != null) deduped.add(b);
      if (!deduped.isEmpty()) return deduped;
    }
    UUID matrixDefault = matrix.getQuestionBankId();
    if (matrixDefault != null) {
      return new LinkedHashSet<>(Collections.singleton(matrixDefault));
    }
    throw new AppException(ErrorCode.QUESTION_BANK_REQUIRED);
  }

  private Map<UUID, ExamMatrixRow> loadRowsById(UUID examMatrixId) {
    Map<UUID, ExamMatrixRow> rowById = new HashMap<>();
    for (ExamMatrixRow row : examMatrixRowRepository.findByExamMatrixId(examMatrixId)) {
      rowById.put(row.getId(), row);
    }
    return rowById;
  }

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
