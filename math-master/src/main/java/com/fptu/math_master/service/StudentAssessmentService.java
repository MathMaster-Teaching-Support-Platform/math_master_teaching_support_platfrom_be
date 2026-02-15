package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AnswerUpdateRequest;
import com.fptu.math_master.dto.request.FlagUpdateRequest;
import com.fptu.math_master.dto.request.StartAssessmentRequest;
import com.fptu.math_master.dto.request.SubmitAssessmentRequest;
import com.fptu.math_master.dto.response.AnswerAckResponse;
import com.fptu.math_master.dto.response.AttemptStartResponse;
import com.fptu.math_master.dto.response.DraftSnapshotResponse;
import com.fptu.math_master.dto.response.StudentAssessmentResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentAssessmentService {

  // My Assessments
  Page<StudentAssessmentResponse> getMyAssessments(String statusFilter, Pageable pageable);

  StudentAssessmentResponse getAssessmentDetails(UUID assessmentId);

  // Start Assessment
  AttemptStartResponse startAssessment(StartAssessmentRequest request);

  // Answer Questions
  AnswerAckResponse updateAnswer(AnswerUpdateRequest request);

  //  Flag for Review
  AnswerAckResponse updateFlag(FlagUpdateRequest request);

  // Submit Assessment
  void submitAssessment(SubmitAssessmentRequest request);

  // Resume Assessment
  DraftSnapshotResponse getDraftSnapshot(UUID attemptId);

  // Save & Exit
  void saveAndExit(UUID attemptId);
}
