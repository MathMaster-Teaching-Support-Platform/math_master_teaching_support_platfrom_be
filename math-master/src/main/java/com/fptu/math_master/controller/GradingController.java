package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.service.GradingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Grading", description = "APIs for grading and managing assessment submissions")
@SecurityRequirement(name = "bearerAuth")
public class GradingController {

  GradingService gradingService;

  // FR-GR-002: Manual Grade Subjective Questions
  @Operation(
      summary = "Get grading queue",
      description = "Get list of submissions that need grading (SUBMITTED status)")
  @GetMapping("/queue")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Page<GradingSubmissionResponse>> getGradingQueue(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

    log.info("Getting grading queue - page: {}, size: {}", page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<GradingSubmissionResponse> queue = gradingService.getGradingQueue(pageable);

    return ApiResponse.<Page<GradingSubmissionResponse>>builder().result(queue).build();
  }

  @Operation(
      summary = "Get grading queue by teacher",
      description = "Get submissions for grading filtered by teacher's assessments")
  @GetMapping("/queue/teacher/{teacherId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Page<GradingSubmissionResponse>> getGradingQueueByTeacher(
      @PathVariable UUID teacherId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    log.info("Getting grading queue for teacher: {} - page: {}, size: {}", teacherId, page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<GradingSubmissionResponse> queue =
        gradingService.getGradingQueueByTeacher(teacherId, pageable);

    return ApiResponse.<Page<GradingSubmissionResponse>>builder().result(queue).build();
  }

  @Operation(
      summary = "Get submission for grading",
      description = "Get detailed submission with all answers for grading")
  @GetMapping("/submissions/{submissionId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<GradingSubmissionResponse> getSubmissionForGrading(
      @PathVariable UUID submissionId) {

    log.info("Getting submission for grading: {}", submissionId);

    GradingSubmissionResponse response = gradingService.getSubmissionForGrading(submissionId);

    return ApiResponse.<GradingSubmissionResponse>builder().result(response).build();
  }

  @Operation(
      summary = "Complete grading",
      description = "Complete manual grading for a submission with subjective questions")
  @PostMapping("/complete")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<GradingSubmissionResponse> completeGrading(
      @Valid @RequestBody CompleteGradingRequest request) {

    log.info("Completing grading for submission: {}", request.getSubmissionId());

    GradingSubmissionResponse response = gradingService.completeGrading(request);

    return ApiResponse.<GradingSubmissionResponse>builder().result(response).build();
  }

  // FR-GR-004: Grade Override
  @Operation(
      summary = "Override grade",
      description = "Override the grade for a specific answer with audit logging")
  @PostMapping("/override")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Void> overrideGrade(@Valid @RequestBody GradeOverrideRequest request) {

    log.info("Overriding grade for answer: {}", request.getAnswerId());

    gradingService.overrideGrade(request);

    return ApiResponse.<Void>builder().message("Grade overridden successfully").build();
  }

  // FR-GR-005: Add Manual Grade to Submission
  @Operation(
      summary = "Add manual adjustment",
      description = "Add bonus or penalty points to a submission (not tied to specific answer)")
  @PostMapping("/adjustment")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Void> addManualAdjustment(
      @Valid @RequestBody ManualAdjustmentRequest request) {

    log.info("Adding manual adjustment to submission: {}", request.getSubmissionId());

    gradingService.addManualAdjustment(request);

    return ApiResponse.<Void>builder().message("Manual adjustment added successfully").build();
  }

  // FR-GR-006: View Grading Analytics
  @Operation(
      summary = "Get grading analytics",
      description = "Get statistical analytics for an assessment's grading")
  @GetMapping("/analytics/{assessmentId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<GradingAnalyticsResponse> getGradingAnalytics(
      @PathVariable UUID assessmentId) {

    log.info("Getting grading analytics for assessment: {}", assessmentId);

    GradingAnalyticsResponse analytics = gradingService.getGradingAnalytics(assessmentId);

    return ApiResponse.<GradingAnalyticsResponse>builder().result(analytics).build();
  }

  // FR-GR-007: Export Grades
  @Operation(
      summary = "Export grades",
      description = "Export all grades for an assessment as CSV file")
  @GetMapping("/export/{assessmentId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ResponseEntity<String> exportGrades(@PathVariable UUID assessmentId) {

    log.info("Exporting grades for assessment: {}", assessmentId);

    String csvContent = gradingService.exportGrades(assessmentId);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=grades_" + assessmentId + ".csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(csvContent);
  }

  // FR-GR-008: Release Grades
  @Operation(
      summary = "Release grades for assessment",
      description = "Release all graded submissions for an assessment to students")
  @PostMapping("/release/{assessmentId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Void> releaseGrades(@PathVariable UUID assessmentId) {

    log.info("Releasing grades for assessment: {}", assessmentId);

    gradingService.releaseGrades(assessmentId);

    return ApiResponse.<Void>builder().message("Grades released successfully").build();
  }

  @Operation(
      summary = "Release grades for submission",
      description = "Release grades for a specific submission to the student")
  @PostMapping("/release/submission/{submissionId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Void> releaseGradesForSubmission(@PathVariable UUID submissionId) {

    log.info("Releasing grades for submission: {}", submissionId);

    gradingService.releaseGradesForSubmission(submissionId);

    return ApiResponse.<Void>builder().message("Grades released successfully").build();
  }

  // FR-GR-009: Request Regrade
  @Operation(
      summary = "Create regrade request",
      description = "Student requests regrade for a specific question")
  @PostMapping("/regrade/request")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<RegradeRequestResponse> createRegradeRequest(
      @Valid @RequestBody RegradeRequestCreationRequest request) {

    log.info(
        "Creating regrade request for submission: {}, question: {}",
        request.getSubmissionId(),
        request.getQuestionId());

    RegradeRequestResponse response = gradingService.createRegradeRequest(request);

    return ApiResponse.<RegradeRequestResponse>builder().result(response).build();
  }

  @Operation(
      summary = "Respond to regrade request",
      description = "Teacher responds to a student's regrade request")
  @PostMapping("/regrade/respond")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<RegradeRequestResponse> respondToRegradeRequest(
      @Valid @RequestBody RegradeResponseRequest request) {

    log.info("Responding to regrade request: {}", request.getRequestId());

    RegradeRequestResponse response = gradingService.respondToRegradeRequest(request);

    return ApiResponse.<RegradeRequestResponse>builder().result(response).build();
  }

  @Operation(summary = "Get regrade requests", description = "Get all pending regrade requests")
  @GetMapping("/regrade/requests")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Page<RegradeRequestResponse>> getRegradeRequests(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

    log.info("Getting regrade requests - page: {}, size: {}", page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<RegradeRequestResponse> requests = gradingService.getRegradeRequests(pageable);

    return ApiResponse.<Page<RegradeRequestResponse>>builder().result(requests).build();
  }

  @Operation(
      summary = "Get student regrade requests",
      description = "Get all regrade requests for a specific student")
  @GetMapping("/regrade/student/{studentId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  public ApiResponse<Page<RegradeRequestResponse>> getStudentRegradeRequests(
      @PathVariable UUID studentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    log.info(
        "Getting regrade requests for student: {} - page: {}, size: {}", studentId, page, size);

    Pageable pageable = PageRequest.of(page, size);
    Page<RegradeRequestResponse> requests =
        gradingService.getStudentRegradeRequests(studentId, pageable);

    return ApiResponse.<Page<RegradeRequestResponse>>builder().result(requests).build();
  }

  @Operation(
      summary = "Invalidate submission",
      description = "Teacher voids a student submission (e.g. academic integrity breach)")
  @PostMapping("/submissions/{submissionId}/invalidate")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<GradingSubmissionResponse> invalidateSubmission(
      @PathVariable UUID submissionId, @RequestParam(required = false) String reason) {
    log.info("Invalidating submission: {}", submissionId);
    return ApiResponse.<GradingSubmissionResponse>builder()
        .message("Submission invalidated successfully.")
        .result(gradingService.invalidateSubmission(submissionId, reason))
        .build();
  }

  @Operation(
      summary = "Get my result",
      description =
          "Student views their own graded result. Gated by gradesReleased or showScoreImmediately.")
  @GetMapping("/submissions/{submissionId}/my-result")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<GradingSubmissionResponse> getMyResult(@PathVariable UUID submissionId) {
    return ApiResponse.<GradingSubmissionResponse>builder()
        .result(gradingService.getMyResult(submissionId))
        .build();
  }

  @Operation(
      summary = "Trigger AI review",
      description = "Teacher triggers AI feedback generation for a graded submission.")
  @PostMapping("/submissions/{submissionId}/ai-review")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Void> triggerAiReview(@PathVariable UUID submissionId) {
    log.info("Triggering AI review for submission: {}", submissionId);
    gradingService.triggerAiReview(submissionId);
    return ApiResponse.<Void>builder().message("AI review triggered successfully.").build();
  }

  @Operation(
      summary = "Count pending subjective submissions",
      description =
          "Returns count of SUBMITTED submissions awaiting manual grading for this teacher.")
  @GetMapping("/pending-count")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Long> getPendingSubjectiveCount() {
    UUID teacherId = com.fptu.math_master.util.SecurityUtils.getCurrentUserId();
    return ApiResponse.<Long>builder()
        .result(gradingService.countPendingSubjectiveSubmissions(teacherId))
        .build();
  }
}
