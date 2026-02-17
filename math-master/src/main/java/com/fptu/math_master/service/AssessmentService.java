package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AssessmentRequest;
import com.fptu.math_master.dto.request.PointsOverrideRequest;
import com.fptu.math_master.dto.response.AssessmentResponse;
import com.fptu.math_master.dto.response.AssessmentSummary;
import com.fptu.math_master.enums.AssessmentStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssessmentService {

  // FR-A-001: Create Assessment
  AssessmentResponse createAssessment(AssessmentRequest request);

  // Update Assessment (only DRAFT)
  AssessmentResponse updateAssessment(UUID id, AssessmentRequest request);

  // FR-A-002: Set Points Override
  AssessmentResponse setPointsOverride(UUID assessmentId, PointsOverrideRequest request);

  // FR-A-003: Preview Assessment as Student
  AssessmentResponse getAssessmentPreview(UUID id);

  // FR-A-004: Publish Assessment
  AssessmentSummary getPublishSummary(UUID id);

  AssessmentResponse publishAssessment(UUID id);

  // FR-A-005: Unpublish Assessment
  AssessmentResponse unpublishAssessment(UUID id);

  // FR-A-006: Delete Assessment
  void deleteAssessment(UUID id);

  // Get Assessment
  AssessmentResponse getAssessmentById(UUID id);

  // List Assessments
  Page<AssessmentResponse> getMyAssessments(
      AssessmentStatus status, UUID lessonId, Pageable pageable);

  // Validation methods
  boolean canEditAssessment(UUID id);

  boolean canDeleteAssessment(UUID id);

  boolean canPublishAssessment(UUID id);
}
