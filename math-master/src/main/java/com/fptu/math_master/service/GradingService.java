package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GradingService {

  void autoGradeSubmission(UUID submissionId);

  GradingSubmissionResponse getSubmissionForGrading(UUID submissionId);

  GradingSubmissionResponse completeGrading(CompleteGradingRequest request);

  Page<GradingSubmissionResponse> getGradingQueue(Pageable pageable);

  Page<GradingSubmissionResponse> getGradingQueueByTeacher(UUID teacherId, Pageable pageable);

  void gradeMultipleSubmissions(UUID teacherId, CompleteGradingRequest... requests);

  void overrideGrade(GradeOverrideRequest request);

  void addManualAdjustment(ManualAdjustmentRequest request);

  GradingAnalyticsResponse getGradingAnalytics(UUID assessmentId);

  String exportGrades(UUID assessmentId);

  void releaseGrades(UUID assessmentId);

  void releaseGradesForSubmission(UUID submissionId);

  RegradeRequestResponse createRegradeRequest(RegradeRequestCreationRequest request);

  RegradeRequestResponse respondToRegradeRequest(RegradeResponseRequest request);

  Page<RegradeRequestResponse> getRegradeRequests(Pageable pageable);

  Page<RegradeRequestResponse> getStudentRegradeRequests(UUID studentId, Pageable pageable);

  GradingSubmissionResponse invalidateSubmission(UUID submissionId, String reason);

  GradingSubmissionResponse getMyResult(UUID submissionId);

  void triggerAiReview(UUID submissionId);

  Long countPendingSubjectiveSubmissions(UUID teacherId);
}
