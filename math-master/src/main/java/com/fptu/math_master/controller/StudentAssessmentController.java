package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.service.StudentAssessmentService;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student-assessments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Student Assessments", description = "APIs for students to take assessments")
@SecurityRequirement(name = "bearerAuth")
public class StudentAssessmentController {

  StudentAssessmentService studentAssessmentService;

  @Operation(
      summary = "Get my assessments",
      description =
          "Get list of available assessments for the current student. "
              + "Filters: UPCOMING, IN_PROGRESS, COMPLETED. "
              + "Returns assessments within schedule (start_date <= now <= end_date)")
  @GetMapping
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<Page<StudentAssessmentResponse>> getMyAssessments(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "dueDate") String sortBy,
      @RequestParam(defaultValue = "ASC") String sortDir,
      @RequestParam(required = false) UUID schoolGradeId,
      @RequestParam(required = false) UUID subjectId,
      @RequestParam(required = false) UUID chapterId,
      @RequestParam(required = false) UUID lessonId) {

    Sort sort =
        sortDir.equalsIgnoreCase("DESC")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return ApiResponse.<Page<StudentAssessmentResponse>>builder()
        .result(
            studentAssessmentService.getMyAssessments(
                status, pageable, schoolGradeId, subjectId, chapterId, lessonId))
        .build();
  }

  @Operation(
      summary = "Get my assessments by course",
      description =
          "Get list of available assessments for the current student in a specific course context.")
  @GetMapping("/course/{courseId}")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<Page<StudentAssessmentResponse>> getMyAssessmentsByCourse(
      @PathVariable UUID courseId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "dueDate") String sortBy,
      @RequestParam(defaultValue = "ASC") String sortDir) {

    Sort sort =
        sortDir.equalsIgnoreCase("DESC")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return ApiResponse.<Page<StudentAssessmentResponse>>builder()
        .result(studentAssessmentService.getMyAssessmentsByCourse(courseId, status, pageable))
        .build();
  }

  @Operation(
      summary = "Get assessment details",
      description = "View details of a specific assessment before starting")
  @GetMapping("/{assessmentId}")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<StudentAssessmentResponse> getAssessmentDetails(
      @PathVariable UUID assessmentId) {

    return ApiResponse.<StudentAssessmentResponse>builder()
        .result(studentAssessmentService.getAssessmentDetails(assessmentId))
        .build();
  }

  @Operation(
      summary = "Start assessment",
      description =
          "Start a new attempt for an assessment. "
              + "Pre-checks: PUBLISHED, within schedule, not exceeded max_attempts. "
              + "Creates submission, quiz_attempt, initializes Redis draft, "
              + "returns Centrifugo connection token and questions.")
  @PostMapping("/start")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<AttemptStartResponse> startAssessment(
      @Valid @RequestBody StartAssessmentRequest request) {

    log.info("Starting assessment: {}", request.getAssessmentId());

    return ApiResponse.<AttemptStartResponse>builder()
        .result(studentAssessmentService.startAssessment(request))
        .build();
  }

  @Operation(
      summary = "Update answer",
      description =
          "Update answer for a question. "
              + "Saves to Redis draft and publishes ACK to Centrifugo channel.")
  @PostMapping("/answer")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<AnswerAckResponse> updateAnswer(
      @Valid @RequestBody AnswerUpdateRequest request) {

    return ApiResponse.<AnswerAckResponse>builder()
        .result(studentAssessmentService.updateAnswer(request))
        .build();
  }

  @Operation(
      summary = "Update flag",
      description =
          "Flag/unflag a question for review. " + "Saves flag state to Redis and publishes ACK.")
  @PostMapping("/flag")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<AnswerAckResponse> updateFlag(@Valid @RequestBody FlagUpdateRequest request) {

    return ApiResponse.<AnswerAckResponse>builder()
        .result(studentAssessmentService.updateFlag(request))
        .build();
  }

  @Operation(
      summary = "Submit assessment",
      description =
          "Submit the assessment attempt. "
              + "Flushes Redis draft to database, marks as SUBMITTED, "
              + "calculates time spent, publishes submitted event.")
  @PostMapping("/submit")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<Void> submitAssessment(@Valid @RequestBody SubmitAssessmentRequest request) {

    log.info("Submitting assessment attempt: {}", request.getAttemptId());

    studentAssessmentService.submitAssessment(request);

    return ApiResponse.<Void>builder().message("Assessment submitted successfully").build();
  }

  @Operation(
      summary = "Get draft snapshot",
      description =
          "Get current draft state from Redis for reconnection/resume. "
              + "Returns answers, flags, time remaining, and progress.")
  @GetMapping("/attempts/{attemptId}/snapshot")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<DraftSnapshotResponse> getDraftSnapshot(@PathVariable UUID attemptId) {

    return ApiResponse.<DraftSnapshotResponse>builder()
        .result(studentAssessmentService.getDraftSnapshot(attemptId))
        .build();
  }

  @Operation(
      summary = "Save and exit",
      description =
          "Save current progress and exit. " + "Attempt remains IN_PROGRESS for later resume.")
  @PostMapping("/attempts/{attemptId}/save-exit")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<Void> saveAndExit(@PathVariable UUID attemptId) {

    log.info("Saving and exiting attempt: {}", attemptId);

    studentAssessmentService.saveAndExit(attemptId);

    return ApiResponse.<Void>builder().message("Progress saved successfully").build();
  }
}
