package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AddQuestionToAssessmentRequest;
import com.fptu.math_master.dto.request.AssessmentRequest;
import com.fptu.math_master.dto.request.CloneAssessmentRequest;
import com.fptu.math_master.dto.request.PointsOverrideRequest;
import com.fptu.math_master.dto.response.AssessmentResponse;
import com.fptu.math_master.dto.response.AssessmentSummary;
import com.fptu.math_master.enums.AssessmentStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssessmentService {

  AssessmentResponse createAssessment(AssessmentRequest request);

  AssessmentResponse updateAssessment(UUID id, AssessmentRequest request);

  AssessmentResponse setPointsOverride(UUID assessmentId, PointsOverrideRequest request);

  AssessmentResponse getAssessmentPreview(UUID id);

  AssessmentSummary getPublishSummary(UUID id);

  AssessmentResponse publishAssessment(UUID id);

  AssessmentResponse unpublishAssessment(UUID id);

  void deleteAssessment(UUID id);

  AssessmentResponse getAssessmentById(UUID id);

  Page<AssessmentResponse> getMyAssessments(
      AssessmentStatus status, UUID lessonId, Pageable pageable);

  boolean canEditAssessment(UUID id);

  boolean canDeleteAssessment(UUID id);

  boolean canPublishAssessment(UUID id);

  AssessmentResponse closeAssessment(UUID id);

  AssessmentResponse cloneAssessment(UUID id, CloneAssessmentRequest request);

  AssessmentResponse addQuestion(UUID assessmentId, AddQuestionToAssessmentRequest request);

  AssessmentResponse removeQuestion(UUID assessmentId, UUID questionId);
}
