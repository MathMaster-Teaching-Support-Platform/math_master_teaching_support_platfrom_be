package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.QuestionBankRequest;
import com.fptu.math_master.dto.response.QuestionBankResponse;
import com.fptu.math_master.entity.QuestionBank;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.QuestionBankRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.QuestionBankService;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuestionBankServiceImpl implements QuestionBankService {

  QuestionBankRepository questionBankRepository;
  UserRepository userRepository;

  private static final List<String> MATH_SUBJECT = Arrays.asList("Math");

  @Override
  @Transactional
  public QuestionBankResponse createQuestionBank(QuestionBankRequest request) {
    log.info("Creating question bank: {}", request.getName());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);
    validateSubject(request.getSubject());

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

    log.info("Question bank created successfully with id: {}", questionBank.getId());
    return mapToResponse(questionBank);
  }

  @Override
  @Transactional
  public QuestionBankResponse updateQuestionBank(UUID id, QuestionBankRequest request) {
    log.info("Updating question bank with id: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(questionBank.getTeacherId(), currentUserId);
    validateSubject(request.getSubject());

    // Check if questions are in use and warn
    if (questionBankRepository.hasQuestionsInUse(id)) {
      log.warn("Question bank {} has questions in use in assessments", id);
    }

    questionBank.setName(request.getName());
    questionBank.setDescription(request.getDescription());
    questionBank.setSubject(request.getSubject());
    questionBank.setGradeLevel(request.getGradeLevel());
    if (request.getIsPublic() != null) {
      questionBank.setIsPublic(request.getIsPublic());
    }

    questionBank = questionBankRepository.save(questionBank);

    log.info("Question bank updated successfully: {}", id);
    return mapToResponse(questionBank);
  }

  @Override
  @Transactional
  public void deleteQuestionBank(UUID id) {
    log.info("Deleting question bank with id: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(questionBank.getTeacherId(), currentUserId);

    // Soft delete
    questionBank.setDeletedAt(Instant.now());
    questionBankRepository.save(questionBank);

    log.info("Question bank soft deleted successfully: {}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public QuestionBankResponse getQuestionBankById(UUID id) {
    log.info("Getting question bank with id: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();

    // Check access: owner, admin, or public
    if (!questionBank.getTeacherId().equals(currentUserId)
        && !questionBank.getIsPublic()
        && !isAdmin(currentUserId)) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }

    return mapToResponse(questionBank);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionBankResponse> getMyQuestionBanks(Pageable pageable) {
    log.info("Getting my question banks");

    UUID currentUserId = getCurrentUserId();
    Page<QuestionBank> questionBanks =
        questionBankRepository.findByTeacherIdAndNotDeleted(currentUserId, pageable);

    return questionBanks.map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<QuestionBankResponse> searchQuestionBanks(
      String subject, String gradeLevel, Boolean isPublic, String searchTerm, Pageable pageable) {
    log.info(
        "Searching question banks with filters - subject: {}, gradeLevel: {}, isPublic: {}, searchTerm: {}",
        subject,
        gradeLevel,
        isPublic,
        searchTerm);

    UUID currentUserId = getCurrentUserId();
    Page<QuestionBank> questionBanks =
        questionBankRepository.findWithFilters(
            currentUserId, subject, gradeLevel, isPublic, searchTerm, pageable);

    return questionBanks.map(this::mapToResponse);
  }

  @Override
  @Transactional
  public QuestionBankResponse togglePublicStatus(UUID id) {
    log.info("Toggling public status for question bank: {}", id);

    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(questionBank.getTeacherId(), currentUserId);

    questionBank.setIsPublic(!questionBank.getIsPublic());
    questionBank = questionBankRepository.save(questionBank);

    log.info("Question bank public status toggled to: {}", questionBank.getIsPublic());
    return mapToResponse(questionBank);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canEditQuestionBank(UUID id) {
    QuestionBank questionBank =
        questionBankRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_BANK_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    return questionBank.getTeacherId().equals(currentUserId) || isAdmin(currentUserId);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canDeleteQuestionBank(UUID id) {
    return canEditQuestionBank(id);
  }

  // Helper methods

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      String sub = jwtAuth.getToken().getSubject();
      return UUID.fromString(sub);
    }
    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private void validateTeacherRole(UUID userId) {
    User user =
        userRepository
            .findByIdWithRoles(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    boolean isTeacher =
        user.getRoles().stream()
            .anyMatch(role -> role.getName().equals("TEACHER") || role.getName().equals("ADMIN"));

    if (!isTeacher) {
      throw new AppException(ErrorCode.NOT_A_TEACHER);
    }
  }

  private boolean isAdmin(UUID userId) {
    User user =
        userRepository
            .findByIdWithRoles(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    return user.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN"));
  }

  private void validateOwnerOrAdmin(UUID ownerId, UUID currentUserId) {
    if (!ownerId.equals(currentUserId) && !isAdmin(currentUserId)) {
      throw new AppException(ErrorCode.QUESTION_BANK_ACCESS_DENIED);
    }
  }

  private void validateSubject(String subject) {
    if (subject != null && !MATH_SUBJECT.contains(subject)) {
      throw new AppException(ErrorCode.INVALID_SUBJECT);
    }
  }

  private QuestionBankResponse mapToResponse(QuestionBank questionBank) {
    Long questionCount =
        questionBankRepository.countQuestionsByQuestionBankId(questionBank.getId());

    // Get teacher name
    String teacherName =
        userRepository
            .findById(questionBank.getTeacherId())
            .map(User::getFullName)
            .orElse("Unknown");

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
}
