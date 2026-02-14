package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GradingService {

    // FR-GR-001: Auto-Grade Objective Questions
    void autoGradeSubmission(UUID submissionId);

    // FR-GR-002: Manual Grade Subjective Questions
    GradingSubmissionResponse getSubmissionForGrading(UUID submissionId);
    GradingSubmissionResponse completeGrading(CompleteGradingRequest request);
    Page<GradingSubmissionResponse> getGradingQueue(Pageable pageable);
    Page<GradingSubmissionResponse> getGradingQueueByTeacher(UUID teacherId, Pageable pageable);

    // FR-GR-003: Batch Grading
    void gradeMultipleSubmissions(UUID teacherId, CompleteGradingRequest... requests);

    // FR-GR-004: Grade Override
    void overrideGrade(GradeOverrideRequest request);

    // FR-GR-005: Add Manual Grade to Submission
    void addManualAdjustment(ManualAdjustmentRequest request);

    // FR-GR-006: View Grading Analytics
    GradingAnalyticsResponse getGradingAnalytics(UUID assessmentId);

    // FR-GR-007: Export Grades (returns CSV content as String)
    String exportGrades(UUID assessmentId);

    // FR-GR-008: Release Grades
    void releaseGrades(UUID assessmentId);
    void releaseGradesForSubmission(UUID submissionId);

    // FR-GR-009: Request Regrade
    RegradeRequestResponse createRegradeRequest(RegradeRequestCreationRequest request);
    RegradeRequestResponse respondToRegradeRequest(RegradeResponseRequest request);
    Page<RegradeRequestResponse> getRegradeRequests(Pageable pageable);
    Page<RegradeRequestResponse> getStudentRegradeRequests(UUID studentId, Pageable pageable);
}

