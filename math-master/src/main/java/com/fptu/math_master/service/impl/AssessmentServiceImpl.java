package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.AssessmentRequest;
import com.fptu.math_master.dto.request.PointsOverrideRequest;
import com.fptu.math_master.dto.response.AssessmentResponse;
import com.fptu.math_master.dto.response.AssessmentSummary;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.AssessmentService;
import com.fptu.math_master.service.ExamMatrixService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
public class AssessmentServiceImpl implements AssessmentService {

  AssessmentRepository assessmentRepository;
  AssessmentQuestionRepository assessmentQuestionRepository;
  UserRepository userRepository;
  LessonRepository lessonRepository;
  ExamMatrixRepository examMatrixRepository;
  ExamMatrixService examMatrixService;

  @Override
  @Transactional
  public AssessmentResponse createAssessment(AssessmentRequest request) {
    log.info("Creating assessment: {}", request.getTitle());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);
    validateDates(request.getStartDate(), request.getEndDate());

    Assessment assessment =
        Assessment.builder()
            .teacherId(currentUserId)
            .lessonId(request.getLessonId())
            .title(request.getTitle())
            .description(request.getDescription())
            .assessmentType(request.getAssessmentType())
            .timeLimitMinutes(request.getTimeLimitMinutes())
            .passingScore(request.getPassingScore())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .randomizeQuestions(
                request.getRandomizeQuestions() != null ? request.getRandomizeQuestions() : false)
            .showCorrectAnswers(
                request.getShowCorrectAnswers() != null ? request.getShowCorrectAnswers() : false)
            .hasMatrix(request.getHasMatrix() != null ? request.getHasMatrix() : false)
            .status(AssessmentStatus.DRAFT)
            .build();

    assessment = assessmentRepository.save(assessment);

    log.info("Assessment created successfully with id: {}", assessment.getId());
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse updateAssessment(UUID id, AssessmentRequest request) {
    log.info("Updating assessment with id: {}", id);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    validateOwnerOrAdmin(assessment.getTeacherId(), currentUserId);

    // Cannot edit published assessments
    if (assessment.getStatus() == AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    validateDates(request.getStartDate(), request.getEndDate());

    assessment.setTitle(request.getTitle());
    assessment.setDescription(request.getDescription());
    assessment.setAssessmentType(request.getAssessmentType());
    assessment.setLessonId(request.getLessonId());
    assessment.setTimeLimitMinutes(request.getTimeLimitMinutes());
    assessment.setPassingScore(request.getPassingScore());
    assessment.setStartDate(request.getStartDate());
    assessment.setEndDate(request.getEndDate());

    if (request.getRandomizeQuestions() != null) {
      assessment.setRandomizeQuestions(request.getRandomizeQuestions());
    }
    if (request.getShowCorrectAnswers() != null) {
      assessment.setShowCorrectAnswers(request.getShowCorrectAnswers());
    }
    if (request.getHasMatrix() != null) {
      assessment.setHasMatrix(request.getHasMatrix());
    }

    assessment = assessmentRepository.save(assessment);

    log.info("Assessment updated successfully: {}", id);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse setPointsOverride(UUID assessmentId, PointsOverrideRequest request) {
    log.info(
        "Setting points override for assessment: {}, question: {}",
        assessmentId,
        request.getQuestionId());

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() == AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    AssessmentQuestion aq =
        assessmentQuestionRepository
            .findByAssessmentIdAndQuestionId(assessmentId, request.getQuestionId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_QUESTION_NOT_FOUND));

    aq.setPointsOverride(request.getPointsOverride());
    assessmentQuestionRepository.save(aq);

    log.info("Points override set successfully");
    return mapToResponse(assessment);
  }

  @Override
  @Transactional(readOnly = true)
  public AssessmentResponse getAssessmentPreview(UUID id) {
    log.info("Getting assessment preview: {}", id);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    return mapToResponse(assessment);
  }

  @Override
  @Transactional(readOnly = true)
  public AssessmentSummary getPublishSummary(UUID id) {
    log.info("Getting publish summary for assessment: {}", id);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(id);
    Double totalPointsDouble = assessmentRepository.calculateTotalPoints(id);
    BigDecimal totalPoints =
        totalPointsDouble != null ? BigDecimal.valueOf(totalPointsDouble) : BigDecimal.ZERO;

    // Validation
    boolean canPublish = true;
    String validationMessage = "";

    if (totalQuestions == 0) {
      canPublish = false;
      validationMessage = "Assessment must have at least one question";
    } else if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
      canPublish = false;
      validationMessage = "Total points must be greater than 0";
    } else if (assessment.getStartDate() != null
        && assessment.getStartDate().isBefore(Instant.now())) {
      canPublish = false;
      validationMessage = "Start date cannot be in the past";
    }

    DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

    return AssessmentSummary.builder()
        .totalQuestions(totalQuestions)
        .totalPoints(totalPoints)
        .timeLimitMinutes(assessment.getTimeLimitMinutes())
        .startDate(
            assessment.getStartDate() != null ? formatter.format(assessment.getStartDate()) : null)
        .endDate(assessment.getEndDate() != null ? formatter.format(assessment.getEndDate()) : null)
        .hasSchedule(assessment.getStartDate() != null || assessment.getEndDate() != null)
        .canPublish(canPublish)
        .validationMessage(validationMessage)
        .build();
  }

  @Override
  @Transactional
  public AssessmentResponse publishAssessment(UUID id) {
    log.info("Publishing assessment: {}", id);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() == AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    // Validation
    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(id);
    if (totalQuestions == 0) {
      throw new AppException(ErrorCode.ASSESSMENT_NO_QUESTIONS);
    }

    Double totalPointsDouble = assessmentRepository.calculateTotalPoints(id);
    BigDecimal totalPoints =
        totalPointsDouble != null ? BigDecimal.valueOf(totalPointsDouble) : BigDecimal.ZERO;
    if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
      throw new AppException(ErrorCode.ASSESSMENT_NO_QUESTIONS);
    }

    if (assessment.getStartDate() != null && assessment.getStartDate().isBefore(Instant.now())) {
      throw new AppException(ErrorCode.ASSESSMENT_START_DATE_PAST);
    }

    // If assessment has a matrix, lock it
    if (assessment.getHasMatrix()) {
      examMatrixRepository
          .findByAssessmentIdAndNotDeleted(id)
          .ifPresent(
              matrix -> {
                if (matrix.getStatus() != com.fptu.math_master.enums.MatrixStatus.APPROVED) {
                  throw new AppException(ErrorCode.MATRIX_NOT_APPROVED);
                }
                examMatrixService.lockMatrix(matrix.getId());
              });
    }

    assessment.setStatus(AssessmentStatus.PUBLISHED);
    assessment = assessmentRepository.save(assessment);

    log.info("Assessment published successfully: {}", id);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public AssessmentResponse unpublishAssessment(UUID id) {
    log.info("Unpublishing assessment: {}", id);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    if (assessment.getStatus() != AssessmentStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_PUBLISHED);
    }

    // Check for submissions
    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(id);
    if (submissionCount > 0) {
      throw new AppException(ErrorCode.ASSESSMENT_HAS_SUBMISSIONS);
    }

    assessment.setStatus(AssessmentStatus.DRAFT);
    assessment = assessmentRepository.save(assessment);

    log.info("Assessment unpublished successfully: {}", id);
    return mapToResponse(assessment);
  }

  @Override
  @Transactional
  public void deleteAssessment(UUID id) {
    log.info("Deleting assessment: {}", id);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    validateOwnerOrAdmin(assessment.getTeacherId(), getCurrentUserId());

    // Can only delete DRAFT with no submissions
    if (assessment.getStatus() != AssessmentStatus.DRAFT) {
      throw new AppException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED);
    }

    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(id);
    if (submissionCount > 0) {
      throw new AppException(ErrorCode.ASSESSMENT_HAS_SUBMISSIONS);
    }

    // Soft delete
    assessment.setDeletedAt(Instant.now());
    assessmentRepository.save(assessment);

    log.info("Assessment soft deleted successfully: {}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public AssessmentResponse getAssessmentById(UUID id) {
    log.info("Getting assessment: {}", id);

    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();

    // Check access
    if (!assessment.getTeacherId().equals(currentUserId) && !isAdmin(currentUserId)) {
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }

    return mapToResponse(assessment);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AssessmentResponse> getMyAssessments(
      AssessmentStatus status, UUID lessonId, Pageable pageable) {
    log.info("Getting my assessments - status: {}, lessonId: {}", status, lessonId);

    UUID currentUserId = getCurrentUserId();
    Page<Assessment> assessments =
        assessmentRepository.findWithFilters(currentUserId, status, lessonId, pageable);

    return assessments.map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canEditAssessment(UUID id) {
    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    boolean isOwnerOrAdmin =
        assessment.getTeacherId().equals(currentUserId) || isAdmin(currentUserId);

    return isOwnerOrAdmin && assessment.getStatus() == AssessmentStatus.DRAFT;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canDeleteAssessment(UUID id) {
    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    boolean isOwnerOrAdmin =
        assessment.getTeacherId().equals(currentUserId) || isAdmin(currentUserId);

    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(id);

    return isOwnerOrAdmin
        && assessment.getStatus() == AssessmentStatus.DRAFT
        && submissionCount == 0;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canPublishAssessment(UUID id) {
    Assessment assessment =
        assessmentRepository
            .findByIdAndNotDeleted(id)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    UUID currentUserId = getCurrentUserId();
    boolean isOwnerOrAdmin =
        assessment.getTeacherId().equals(currentUserId) || isAdmin(currentUserId);

    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(id);

    return isOwnerOrAdmin && assessment.getStatus() == AssessmentStatus.DRAFT && totalQuestions > 0;
  }

  // Helper methods

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth
        instanceof
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
                jwtAuth) {
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
      throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
    }
  }

  private void validateDates(Instant startDate, Instant endDate) {
    if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
      throw new AppException(ErrorCode.ASSESSMENT_INVALID_SCHEDULE);
    }
  }

  private AssessmentResponse mapToResponse(Assessment assessment) {
    Long totalQuestions = assessmentRepository.countQuestionsByAssessmentId(assessment.getId());
    Double totalPointsDouble = assessmentRepository.calculateTotalPoints(assessment.getId());
    BigDecimal totalPoints =
        totalPointsDouble != null ? BigDecimal.valueOf(totalPointsDouble) : BigDecimal.ZERO;
    Long submissionCount = assessmentRepository.countSubmissionsByAssessmentId(assessment.getId());

    String teacherName =
        userRepository.findById(assessment.getTeacherId()).map(User::getFullName).orElse("Unknown");

    String lessonTitle = null;
    if (assessment.getLessonId() != null) {
      lessonTitle =
          lessonRepository.findById(assessment.getLessonId()).map(Lesson::getTitle).orElse(null);
    }

    return AssessmentResponse.builder()
        .id(assessment.getId())
        .teacherId(assessment.getTeacherId())
        .teacherName(teacherName)
        .lessonId(assessment.getLessonId())
        .lessonTitle(lessonTitle)
        .title(assessment.getTitle())
        .description(assessment.getDescription())
        .assessmentType(assessment.getAssessmentType())
        .timeLimitMinutes(assessment.getTimeLimitMinutes())
        .passingScore(assessment.getPassingScore())
        .startDate(assessment.getStartDate())
        .endDate(assessment.getEndDate())
        .randomizeQuestions(assessment.getRandomizeQuestions())
        .showCorrectAnswers(assessment.getShowCorrectAnswers())
        .hasMatrix(assessment.getHasMatrix())
        .status(assessment.getStatus())
        .totalQuestions(totalQuestions)
        .totalPoints(totalPoints)
        .submissionCount(submissionCount)
        .createdAt(assessment.getCreatedAt())
        .updatedAt(assessment.getUpdatedAt())
        .build();
  }
}
