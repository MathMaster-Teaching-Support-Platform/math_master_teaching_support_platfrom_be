package com.fptu.math_master.service.impl;

import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.CreateRoadmapFeedbackRequest;
import com.fptu.math_master.dto.response.RoadmapFeedbackResponse;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.RoadmapFeedback;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.RoadmapFeedbackRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.RoadmapFeedbackService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class RoadmapFeedbackServiceImpl implements RoadmapFeedbackService {

  RoadmapFeedbackRepository roadmapFeedbackRepository;
  LearningRoadmapRepository learningRoadmapRepository;
  UserRepository userRepository;

  @Override
  public RoadmapFeedbackResponse submitFeedback(
      UUID roadmapId, UUID studentId, CreateRoadmapFeedbackRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateStudentCanFeedback(roadmap, studentId);

    RoadmapFeedback feedback =
        roadmapFeedbackRepository
            .findByRoadmapIdAndStudentIdAndNotDeleted(roadmapId, studentId)
            .orElseGet(RoadmapFeedback::new);

    if (feedback.getId() == null) {
      feedback.setRoadmapId(roadmapId);
      feedback.setStudentId(studentId);
      feedback.setCreatedBy(studentId);
    }

    feedback.setRating(request.getRating());
    feedback.setContent(request.getContent());
    feedback.setUpdatedBy(studentId);

    RoadmapFeedback saved = roadmapFeedbackRepository.save(feedback);
    return mapToResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapFeedbackResponse getMyFeedback(UUID roadmapId, UUID studentId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateStudentCanFeedback(roadmap, studentId);

    RoadmapFeedback feedback =
        roadmapFeedbackRepository
            .findByRoadmapIdAndStudentIdAndNotDeleted(roadmapId, studentId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToResponse(feedback);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RoadmapFeedbackResponse> getRoadmapFeedbacks(UUID roadmapId, Pageable pageable) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateReadFeedbackAccess(roadmap);

    return roadmapFeedbackRepository
        .findByRoadmapIdAndNotDeleted(roadmapId, pageable)
        .map(this::mapToResponse);
  }

  private LearningRoadmap getActiveRoadmapOrThrow(UUID roadmapId) {
    LearningRoadmap roadmap =
        learningRoadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    return roadmap;
  }

  private void validateStudentCanFeedback(LearningRoadmap roadmap, UUID studentId) {
    if (SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)) {
      return;
    }

    if (!SecurityUtils.hasRole(PredefinedRole.STUDENT_ROLE)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    // Allow feedback on roadmap templates (studentId null) and own assigned roadmap.
    if (roadmap.getStudentId() != null && !roadmap.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
  }

  private void validateReadFeedbackAccess(LearningRoadmap roadmap) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    boolean isAdmin = SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE);
    boolean isTeacherOwner =
        roadmap.getTeacherId() != null && roadmap.getTeacherId().equals(currentUserId);

    if (!isAdmin && !isTeacherOwner) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
  }

  private RoadmapFeedbackResponse mapToResponse(RoadmapFeedback feedback) {
    String studentName =
        userRepository.findById(feedback.getStudentId()).map(User::getFullName).orElse("Unknown");

    return RoadmapFeedbackResponse.builder()
        .id(feedback.getId())
        .roadmapId(feedback.getRoadmapId())
        .studentId(feedback.getStudentId())
        .studentName(studentName)
        .rating(feedback.getRating())
        .content(feedback.getContent())
        .createdAt(feedback.getCreatedAt())
        .updatedAt(feedback.getUpdatedAt())
        .build();
  }
}
