package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.QuestionBankMatrixStatsResponse;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import com.fptu.math_master.dto.response.QuestionBankTreeResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.QuestionBankService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionBankServiceImpl implements QuestionBankService {

  QuestionBankRepository questionBankRepository;
  QuestionRepository questionRepository;
  QuestionTemplateRepository questionTemplateRepository;
  ChapterRepository chapterRepository;
  UserRepository userRepository;
  SchoolGradeRepository schoolGradeRepository;
  SubjectRepository subjectRepository;

  @Override
  @Transactional
  public QuestionBankResponse createQuestionBank(QuestionBankRequest request) {
    log.info("Creating question bank: {}", request.getName());
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    UUID schoolGradeId = request.getSchoolGradeId();
    UUID subjectId = request.getSubjectId();
    if (schoolGradeId != null) {
      schoolGradeRepository
          .findByIdAndNotDeleted(schoolGradeId)
          .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));
    }
    if (subjectId != null) {
      Subject subject =
          subjectRepository
              .findById(subjectId)
              .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
      if (schoolGradeId != null
          && subject.getSchoolGradeId() != null
          && !schoolGradeId.equals(subject.getSchoolGradeId())) {
        throw new AppException(ErrorCode.BANK_GRADE_MISMATCH);
      }
    }

    QuestionBank questionBank =
        QuestionBank.builder()
            .teacherId(currentUserId)
            .name(request.getName())
            .description(request.getDescription())
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
            .schoolGradeId(schoolGradeId)
            .subjectId(subjectId)
            .build();

    questionBank = questionBankRepository.save(questionBank);

    log.info("Question bank created with id: {}", questionBank.getId());
    return mapSingleToResponse(questionBank);
  }

  @Override
  @Transactional
  public QuestionBankResponse updateQuestionBank(UUID id, QuestionBankRequest request) {
    log.info("Updating question bank: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(questionBank.getTeacherId(), currentUserId);

    // Cosmetic fields (name/description/isPublic) are always editable, even
    // when questions of this bank are referenced by published assessments.
    // Scope-changing fields (schoolGradeId/subjectId) are gated below: once a
    // question of this bank has been frozen into an assessment we no longer
    // allow re-anchoring the bank to a different grade or subject — that
    // would silently shift which questions land in the matrix tree.
    questionBank.setName(request.getName());
    questionBank.setDescription(request.getDescription());
    if (request.getIsPublic() != null) {
      questionBank.setIsPublic(request.getIsPublic());
    }

    UUID requestedGradeId = request.getSchoolGradeId();
    UUID requestedSubjectId = request.getSubjectId();
    boolean changingGrade =
        requestedGradeId != null && questionBank.getSchoolGradeId() == null;
    boolean changingSubject =
        !java.util.Objects.equals(requestedSubjectId, questionBank.getSubjectId());

    if ((changingGrade || changingSubject) && questionBankRepository.hasQuestionsInUse(id)) {
      throw new AppException(ErrorCode.QUESTION_BANK_HAS_QUESTIONS_IN_USE);
    }

    // school_grade_id is a one-time backfill: legacy banks (NULL grade) can be
    // upgraded to the new flow, but once anchored to a grade the bank is
    // locked there to keep the chapter tree stable for downstream matrices.
    if (changingGrade) {
      schoolGradeRepository
          .findByIdAndNotDeleted(requestedGradeId)
          .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));
      questionBank.setSchoolGradeId(requestedGradeId);
    }

    // subject_id is freely changeable within the bank's grade — useful when a
    // teacher narrows or widens the bank scope without recreating it.
    UUID effectiveGradeId = questionBank.getSchoolGradeId();
    if (requestedSubjectId == null) {
      questionBank.setSubjectId(null);
    } else if (changingSubject) {
      Subject subject =
          subjectRepository
              .findById(requestedSubjectId)
              .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
      if (effectiveGradeId != null
          && subject.getSchoolGradeId() != null
          && !effectiveGradeId.equals(subject.getSchoolGradeId())) {
        throw new AppException(ErrorCode.BANK_GRADE_MISMATCH);
      }
      questionBank.setSubjectId(requestedSubjectId);
    }

    questionBank = questionBankRepository.save(questionBank);

    log.info("Question bank updated: {}", id);
    return mapSingleToResponse(questionBank);
  }

  @Override
  @Transactional
  public void deleteQuestionBank(UUID id) {
    log.info("Deleting question bank: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(questionBank.getTeacherId(), currentUserId);

    if (questionBankRepository.hasQuestionsInUse(id)) {
      throw new AppException(ErrorCode.QUESTION_BANK_HAS_QUESTIONS_IN_USE);
    }

    int detached = questionRepository.detachFreeQuestionsFromBank(id);
    if (detached > 0) {
      log.info("Detached {} free questions from bank {} before soft-delete", detached, id);
    }

    questionBank.setDeletedAt(Instant.now());
    questionBankRepository.save(questionBank);

    log.info("Question bank soft-deleted: {}", id);
  }

  @Override
  @Transactional
  public QuestionBankResponse togglePublicStatus(UUID id) {
    log.info("Toggling public status for question bank: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(questionBank.getTeacherId(), currentUserId);

    questionBank.setIsPublic(!questionBank.getIsPublic());
    questionBank = questionBankRepository.save(questionBank);

    log.info("Question bank public status toggled to: {}", questionBank.getIsPublic());
    return mapSingleToResponse(questionBank);
  }

  @Override
  @Transactional
  public QuestionTemplateResponse mapTemplateToBank(UUID bankId, UUID templateId) {
    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(bank.getTeacherId(), currentUserId);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreatorAndNotDeleted(templateId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    validateTemplateOwnerOrAdmin(template, currentUserId);

    template.setQuestionBankId(bankId);
    QuestionTemplate saved = questionTemplateRepository.save(template);
    return mapTemplateToResponse(saved);
  }

  @Override
  @Transactional
  public void unmapTemplateFromBank(UUID bankId, UUID templateId) {
    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateOwnerOrAdmin(bank.getTeacherId(), currentUserId);

    QuestionTemplate template =
        questionTemplateRepository
            .findByIdWithCreatorAndNotDeleted(templateId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND));

    if (!bankId.equals(template.getQuestionBankId())) {
      throw new AppException(ErrorCode.QUESTION_TEMPLATE_NOT_IN_BANK);
    }

    validateTemplateOwnerOrAdmin(template, currentUserId);

    template.setQuestionBankId(null);
    questionTemplateRepository.save(template);
  }

  @Override
  @Transactional(readOnly = true)
  public List<QuestionTemplateResponse> getMappedTemplates(UUID bankId) {
    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    if (!bank.getTeacherId().equals(currentUserId)
        && !Boolean.TRUE.equals(bank.getIsPublic())
        && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }

    return questionTemplateRepository.findByQuestionBankIdAndNotDeleted(bankId).stream()
        .map(this::mapTemplateToResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public QuestionBankResponse getQuestionBankById(UUID id) {
    log.info("Getting question bank: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    // owner, admin, or public bank
    if (!questionBank.getTeacherId().equals(currentUserId)
        && !questionBank.getIsPublic()
        && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }

    return mapSingleToResponse(questionBank);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionBankResponse> getMyQuestionBanks(Pageable pageable) {
    log.info("Getting my question banks");

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    return questionBankRepository
        .findByTeacherIdAndNotDeleted(currentUserId, pageable)
        .map(this::mapSingleToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionBankResponse> searchQuestionBanks(
      String searchTerm, UUID chapterId, Boolean mineOnly, Pageable pageable) {
    log.info(
        "Searching question banks - searchTerm: {}, chapterId: {}, mineOnly: {}",
        searchTerm,
        chapterId,
        mineOnly);

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    boolean admin = SecurityUtils.hasRole("ADMIN");
    boolean effectiveMineOnly = !admin || !Boolean.FALSE.equals(mineOnly);

    String normalizedSearchTerm = searchTerm != null ? searchTerm.trim() : null;
    if (normalizedSearchTerm != null && normalizedSearchTerm.isEmpty()) {
      normalizedSearchTerm = null;
    }

    Page<QuestionBank> page =
        effectiveMineOnly
            ? questionBankRepository.searchMineByChapterAndName(
                currentUserId, chapterId, normalizedSearchTerm, pageable)
            : questionBankRepository.searchAllActiveByChapterAndName(
                chapterId, normalizedSearchTerm, pageable);

    return page.map(this::mapSingleToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canEditQuestionBank(UUID id) {
    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    return questionBank.getTeacherId().equals(currentUserId) || SecurityUtils.hasRole("ADMIN");
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canDeleteQuestionBank(UUID id) {
    if (!canEditQuestionBank(id)) {
      return false;
    }
    return !questionBankRepository.hasQuestionsInUse(id);
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }
  }

  private QuestionBankResponse mapSingleToResponse(QuestionBank questionBank) {
    Long questionCount =
        questionBankRepository.countQuestionsByQuestionBankId(questionBank.getId());

    String teacherName =
        userRepository.findById(questionBank.getTeacherId()).map(User::getFullName).orElse(null);

    List<Object[]> cognitiveRows =
        questionRepository.countByCognitiveLevelForBank(questionBank.getId());
    // Always render all four Vietnamese buckets (even when empty) so the FE
    // can show a stable layout, and fold Bloom-style English levels onto the
    // matching bucket — mirrors the tree-view grouping in getBankTree().
    Map<String, Long> cognitiveStats = new LinkedHashMap<>();
    for (CognitiveLevel lvl : TREE_LEVELS) {
      cognitiveStats.put(lvl.name(), 0L);
    }
    for (Object[] row : cognitiveRows) {
      CognitiveLevel raw = (CognitiveLevel) row[0];
      Long count = (Long) row[1];
      CognitiveLevel mapped = mapToVietnameseLevel(raw);
      if (mapped == null) continue;
      cognitiveStats.merge(mapped.name(), count, Long::sum);
    }

    Integer gradeLevel = null;
    String schoolGradeName = null;
    if (questionBank.getSchoolGradeId() != null) {
      SchoolGrade sg =
          schoolGradeRepository.findById(questionBank.getSchoolGradeId()).orElse(null);
      if (sg != null) {
        gradeLevel = sg.getGradeLevel();
        schoolGradeName = sg.getName();
      }
    }

    String subjectName = null;
    if (questionBank.getSubjectId() != null) {
      Subject subj = subjectRepository.findById(questionBank.getSubjectId()).orElse(null);
      if (subj != null) {
        subjectName = subj.getName();
      }
    }

    return QuestionBankResponse.builder()
        .id(questionBank.getId())
        .teacherId(questionBank.getTeacherId())
        .teacherName(teacherName)
        .name(questionBank.getName())
        .description(questionBank.getDescription())
        .isPublic(questionBank.getIsPublic())
        .questionCount(questionCount)
        .cognitiveStats(cognitiveStats)
        .schoolGradeId(questionBank.getSchoolGradeId())
        .gradeLevel(gradeLevel)
        .schoolGradeName(schoolGradeName)
        .subjectId(questionBank.getSubjectId())
        .subjectName(subjectName)
        .createdAt(questionBank.getCreatedAt())
        .updatedAt(questionBank.getUpdatedAt())
        .build();
  }

  private void validateTemplateOwnerOrAdmin(QuestionTemplate template, UUID currentUserId) {
    if (!template.getCreatedBy().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.TEMPLATE_ACCESS_DENIED);
    }
  }

  private QuestionTemplateResponse mapTemplateToResponse(QuestionTemplate template) {
    String creatorName =
        template.getCreator() != null
            ? template.getCreator().getFullName()
            : userRepository.findById(template.getCreatedBy()).map(User::getFullName).orElse(null);

    return QuestionTemplateResponse.builder()
        .id(template.getId())
        .createdBy(template.getCreatedBy())
        .creatorName(creatorName)
        .name(template.getName())
        .description(template.getDescription())
        .templateType(template.getTemplateType())
        .templateVariant(template.getTemplateVariant())
        .templateText(template.getTemplateText())
        .parameters(template.getParameters())
        .answerFormula(template.getAnswerFormula())
        .optionsGenerator(template.getOptionsGenerator())
        .constraints(template.getConstraints())
        .cognitiveLevel(template.getCognitiveLevel())
        .tags(template.getTags())
        .isPublic(template.getIsPublic())
        .status(template.getStatus())
        .usageCount(template.getUsageCount())
        .avgSuccessRate(template.getAvgSuccessRate())
        .createdAt(template.getCreatedAt())
        .updatedAt(template.getUpdatedAt())
        .questionBankId(template.getQuestionBankId())
        .build();
  }

  @Override
  public List<QuestionBankMatrixStatsResponse> getMatrixStats(UUID bankId) {
    log.info("Getting matrix stats for question bank: {}", bankId);

    // Verify bank exists
    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    // Finale2: Split stats into non-TF (question-level) + TF (clause-level)
    // TF clauses are expanded from generation_metadata->'tfClauses' JSONB
    List<Object[]> nonTfStats = questionRepository.findNonTFMatrixStatsByBankId(bankId);
    List<Object[]> tfClauseStats = questionRepository.findTFClauseStatsByBankId(bankId);
    List<Object[]> rawStats = new java.util.ArrayList<>(nonTfStats);
    rawStats.addAll(tfClauseStats);

    // Group by grade level
    Map<String, List<Object[]>> byGrade = new LinkedHashMap<>();
    for (Object[] row : rawStats) {
      // grade_level is INTEGER in database, convert to String for grouping
      String gradeLevel = String.valueOf(row[0]);
      byGrade.computeIfAbsent(gradeLevel, k -> new java.util.ArrayList<>()).add(row);
    }

    // Build response
    List<QuestionBankMatrixStatsResponse> result = new java.util.ArrayList<>();

    for (Map.Entry<String, List<Object[]>> gradeEntry : byGrade.entrySet()) {
      String gradeLevel = gradeEntry.getKey();
      List<Object[]> gradeRows = gradeEntry.getValue();

      // Group by chapter
      Map<UUID, List<Object[]>> byChapter = new LinkedHashMap<>();
      for (Object[] row : gradeRows) {
        UUID chapterId = (UUID) row[1];
        byChapter.computeIfAbsent(chapterId, k -> new java.util.ArrayList<>()).add(row);
      }

      // Build chapter stats
      List<QuestionBankMatrixStatsResponse.ChapterStats> chapterStatsList =
          new java.util.ArrayList<>();

      for (Map.Entry<UUID, List<Object[]>> chapterEntry : byChapter.entrySet()) {
        UUID chapterId = chapterEntry.getKey();
        List<Object[]> chapterRows = chapterEntry.getValue();
        String chapterName = (String) chapterRows.get(0)[2];

        // Group by question type
        Map<String, List<Object[]>> byType = new LinkedHashMap<>();
        for (Object[] row : chapterRows) {
          String questionType = (String) row[3];
          byType.computeIfAbsent(questionType, k -> new java.util.ArrayList<>()).add(row);
        }

        // Build type stats
        List<QuestionBankMatrixStatsResponse.TypeStats> typeStatsList =
            new java.util.ArrayList<>();
        int chapterTotal = 0;

        for (Map.Entry<String, List<Object[]>> typeEntry : byType.entrySet()) {
          String questionType = typeEntry.getKey();
          List<Object[]> typeRows = typeEntry.getValue();

          // Build cognitive counts
          Map<String, Integer> cognitiveCounts = new LinkedHashMap<>();
          int typeTotal = 0;

          for (Object[] row : typeRows) {
            String cognitiveLevel = (String) row[4];
            int count = ((Number) row[5]).intValue();
            cognitiveCounts.put(cognitiveLevel, count);
            typeTotal += count;
          }

          typeStatsList.add(
              QuestionBankMatrixStatsResponse.TypeStats.builder()
                  .questionType(questionType)
                  .totalQuestions(typeTotal)
                  .cognitiveCounts(cognitiveCounts)
                  .build());

          chapterTotal += typeTotal;
        }

        chapterStatsList.add(
            QuestionBankMatrixStatsResponse.ChapterStats.builder()
                .chapterId(chapterId)
                .chapterName(chapterName)
                .totalQuestions(chapterTotal)
                .types(typeStatsList)
                .build());
      }

      int gradeTotal =
          chapterStatsList.stream()
              .mapToInt(QuestionBankMatrixStatsResponse.ChapterStats::getTotalQuestions)
              .sum();

      result.add(
          QuestionBankMatrixStatsResponse.builder()
              .gradeLevel(gradeLevel)
              .totalQuestions(gradeTotal)
              .chapters(chapterStatsList)
              .build());
    }

    log.info("Matrix stats retrieved for bank {}: {} grade levels", bankId, result.size());
    return result;
  }

  // ── Bank tree (happy-case view) ────────────────────────────────────────────

  /**
   * Vietnamese cognitive levels populated for every chapter, even when empty.
   * Keeps the response shape stable so FE can always render NB / TH / VD / VDC tabs.
   */
  private static final List<CognitiveLevel> TREE_LEVELS =
      List.of(
          CognitiveLevel.NHAN_BIET,
          CognitiveLevel.THONG_HIEU,
          CognitiveLevel.VAN_DUNG,
          CognitiveLevel.VAN_DUNG_CAO);

  @Override
  @Transactional(readOnly = true)
  public QuestionBankTreeResponse getBankTree(UUID bankId) {
    log.info("Building bank tree: {}", bankId);

    QuestionBank bank =
        questionBankRepository
            .findByIdAndNotDeleted(bankId)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = SecurityUtils.getCurrentUserId();
    if (!bank.getTeacherId().equals(currentUserId)
        && !Boolean.TRUE.equals(bank.getIsPublic())
        && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }

    SchoolGrade schoolGrade =
        bank.getSchoolGradeId() != null
            ? schoolGradeRepository.findById(bank.getSchoolGradeId()).orElse(null)
            : null;
    Subject subject =
        bank.getSubjectId() != null
            ? subjectRepository.findById(bank.getSubjectId()).orElse(null)
            : null;

    List<Chapter> chapters = loadChaptersForBank(bank);

    Map<UUID, List<Question>> questionsByChapter = new LinkedHashMap<>();
    if (!chapters.isEmpty()) {
      List<UUID> chapterIds = chapters.stream().map(Chapter::getId).toList();
      List<Question> questions =
          questionRepository.findByBankAndChaptersForTree(bankId, chapterIds);
      for (Question q : questions) {
        questionsByChapter
            .computeIfAbsent(q.getChapterId(), k -> new ArrayList<>())
            .add(q);
      }
    }

    List<QuestionBankTreeResponse.ChapterNode> chapterNodes = new ArrayList<>();
    for (Chapter chapter : chapters) {
      List<Question> chapterQuestions =
          questionsByChapter.getOrDefault(chapter.getId(), List.of());
      chapterNodes.add(buildChapterNode(chapter, chapterQuestions));
    }

    return QuestionBankTreeResponse.builder()
        .bankId(bank.getId())
        .bankName(bank.getName())
        .schoolGradeId(bank.getSchoolGradeId())
        .gradeLevel(schoolGrade != null ? schoolGrade.getGradeLevel() : null)
        .schoolGradeName(schoolGrade != null ? schoolGrade.getName() : null)
        .subjectId(bank.getSubjectId())
        .subjectName(subject != null ? subject.getName() : null)
        .chapters(chapterNodes)
        .build();
  }

  private List<Chapter> loadChaptersForBank(QuestionBank bank) {
    if (bank.getSubjectId() != null) {
      return chapterRepository.findBySubjectIdAndNotDeleted(bank.getSubjectId());
    }
    if (bank.getSchoolGradeId() == null) {
      return List.of();
    }
    List<Subject> subjects =
        subjectRepository.findBySchoolGradeIdAndIsActiveTrueOrderByName(bank.getSchoolGradeId());
    List<Chapter> all = new ArrayList<>();
    for (Subject s : subjects) {
      all.addAll(chapterRepository.findBySubjectIdAndNotDeleted(s.getId()));
    }
    all.sort(
        Comparator.comparing(
                Chapter::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Chapter::getTitle, Comparator.nullsLast(Comparator.naturalOrder())));
    return all;
  }

  private QuestionBankTreeResponse.ChapterNode buildChapterNode(
      Chapter chapter, List<Question> chapterQuestions) {
    Map<CognitiveLevel, List<Question>> grouped = new LinkedHashMap<>();
    for (CognitiveLevel level : TREE_LEVELS) {
      grouped.put(level, new ArrayList<>());
    }
    for (Question q : chapterQuestions) {
      CognitiveLevel mapped = mapToVietnameseLevel(q.getCognitiveLevel());
      if (mapped != null) {
        grouped.get(mapped).add(q);
      }
    }

    Map<String, QuestionBankTreeResponse.CognitiveBucket> buckets = new LinkedHashMap<>();
    long total = 0;
    for (CognitiveLevel level : TREE_LEVELS) {
      List<Question> qs = grouped.get(level);
      total += qs.size();
      buckets.put(
          level.name(),
          QuestionBankTreeResponse.CognitiveBucket.builder()
              .level(level)
              .count(qs.size())
              .questions(qs.stream().map(this::toSummary).toList())
              .build());
    }

    return QuestionBankTreeResponse.ChapterNode.builder()
        .chapterId(chapter.getId())
        .title(chapter.getTitle())
        .orderIndex(chapter.getOrderIndex())
        .totalQuestions(total)
        .buckets(buckets)
        .build();
  }

  /** Map Bloom-style English levels onto the four Vietnamese buckets used in the matrix. */
  private CognitiveLevel mapToVietnameseLevel(CognitiveLevel raw) {
    if (raw == null) return null;
    if (EnumSet.of(
            CognitiveLevel.NHAN_BIET,
            CognitiveLevel.THONG_HIEU,
            CognitiveLevel.VAN_DUNG,
            CognitiveLevel.VAN_DUNG_CAO)
        .contains(raw)) {
      return raw;
    }
    return switch (raw) {
      case REMEMBER -> CognitiveLevel.NHAN_BIET;
      case UNDERSTAND -> CognitiveLevel.THONG_HIEU;
      case APPLY, ANALYZE -> CognitiveLevel.VAN_DUNG;
      case EVALUATE, CREATE -> CognitiveLevel.VAN_DUNG_CAO;
      default -> null;
    };
  }

  private QuestionBankTreeResponse.QuestionSummary toSummary(Question q) {
    return QuestionBankTreeResponse.QuestionSummary.builder()
        .id(q.getId())
        .questionText(q.getQuestionText())
        .questionType(q.getQuestionType() != null ? q.getQuestionType().name() : null)
        .questionStatus(q.getQuestionStatus() != null ? q.getQuestionStatus().name() : null)
        .build();
  }
}
