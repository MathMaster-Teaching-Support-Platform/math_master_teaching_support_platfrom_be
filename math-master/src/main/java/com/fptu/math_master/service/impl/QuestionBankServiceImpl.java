package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import com.fptu.math_master.dto.response.QuestionBankSummaryProjection;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.QuestionBankService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
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
            .subject(request.getSubject())
            .gradeLevel(request.getGradeLevel())
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
      boolean subjectChanged =
          !java.util.Objects.equals(questionBank.getSubject(), request.getSubject());
      boolean gradeChanged =
          !java.util.Objects.equals(questionBank.getGradeLevel(), request.getGradeLevel());
      if (subjectChanged || gradeChanged) {
        throw new AppException(ErrorCode.QUESTION_BANK_HAS_QUESTIONS_IN_USE);
      }
    }

    questionBank.setName(request.getName());
    questionBank.setDescription(request.getDescription());
    questionBank.setSubject(request.getSubject());
    questionBank.setGradeLevel(request.getGradeLevel());
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

    //single query projection — no N+1
    return questionBankRepository
        .findSummaryByTeacherIdAndNotDeleted(currentUserId, pageable)
        .map(this::mapProjectionToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionBankResponse> searchQuestionBanks(
      String subject, String gradeLevel, Boolean isPublic, String searchTerm, Pageable pageable) {
    log.info(
        "Searching question banks – subject: {}, gradeLevel: {}, isPublic: {}, searchTerm: {}",
        subject, gradeLevel, isPublic, searchTerm);

    UUID currentUserId = SecurityUtils.getCurrentUserId();

    //single query projection — no N+1
    return questionBankRepository
        .findSummaryWithFilters(currentUserId, subject, gradeLevel, isPublic, searchTerm, pageable)
        .map(this::mapProjectionToResponse);
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
        userRepository
            .findById(questionBank.getTeacherId())
            .map(User::getFullName)
            .orElse(null);

    return QuestionBankResponse.builder()
        .id(questionBank.getId())
        .teacherId(questionBank.getTeacherId())
        .teacherName(teacherName)
        .name(questionBank.getName())
        .description(questionBank.getDescription())
        .subject(questionBank.getSubject())
        .gradeLevel(questionBank.getGradeLevel())
        .isPublic(questionBank.getIsPublic())
        .questionCount(questionCount)
        .createdAt(questionBank.getCreatedAt())
        .updatedAt(questionBank.getUpdatedAt())
        .build();
  }

  private QuestionBankResponse mapProjectionToResponse(QuestionBankSummaryProjection p) {
    return QuestionBankResponse.builder()
        .id(p.getId())
        .teacherId(p.getTeacherId())
        .teacherName(p.getTeacherName())
        .name(p.getName())
        .description(p.getDescription())
        .subject(p.getSubject())
        .gradeLevel(p.getGradeLevel())
        .isPublic(p.getIsPublic())
        .questionCount(p.getQuestionCount())
        .createdAt(p.getCreatedAt())
        .updatedAt(p.getUpdatedAt())
        .build();
  }
}
