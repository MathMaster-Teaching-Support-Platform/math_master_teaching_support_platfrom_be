package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import com.fptu.math_master.dto.response.QuestionTemplateResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.QuestionBankService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
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

  @Override
  @Transactional
  public QuestionBankResponse createQuestionBank(QuestionBankRequest request) {
    log.info("Creating question bank: {}", request.getName());
    UUID currentUserId = SecurityUtils.getCurrentUserId();

    QuestionBank questionBank =
        QuestionBank.builder()
            .teacherId(currentUserId)
            .name(request.getName())
            .description(request.getDescription())
            .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
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

    boolean inUse = questionBankRepository.hasQuestionsInUse(id);
    if (inUse) {
      throw new AppException(ErrorCode.QUESTION_BANK_HAS_QUESTIONS_IN_USE);
    }

    questionBank.setName(request.getName());
    questionBank.setDescription(request.getDescription());
    if (request.getIsPublic() != null) {
      questionBank.setIsPublic(request.getIsPublic());
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
    Map<String, Long> cognitiveStats = new LinkedHashMap<>();
    for (Object[] row : cognitiveRows) {
      CognitiveLevel level = (CognitiveLevel) row[0];
      Long count = (Long) row[1];
      cognitiveStats.put(level.name(), count);
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
}
