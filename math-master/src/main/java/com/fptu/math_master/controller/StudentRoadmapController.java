package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.service.RoadmapAdminService;
import com.fptu.math_master.service.RoadmapFeedbackService;
import com.fptu.math_master.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class StudentRoadmapController {

  RoadmapAdminService roadmapAdminService;
  RoadmapFeedbackService roadmapFeedbackService;

  @GetMapping("/roadmaps")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Browse available roadmaps")
  public ApiResponse<Page<RoadmapSummaryResponse>> getAvailableRoadmaps(
      @RequestParam(required = false) String name,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<RoadmapSummaryResponse>>builder()
        .result(roadmapAdminService.getAllRoadmaps(name, pageable)).build();
  }

  @GetMapping("/roadmaps/{id}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get roadmap detail with entry test state")
  public ApiResponse<RoadmapDetailResponse> getRoadmap(@PathVariable UUID id) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapDetailResponse>builder()
        .result(roadmapAdminService.getRoadmapForStudent(studentId, id)).build();
  }

  @GetMapping("/roadmaps/topics/{topicId}/materials")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get topic learning materials")
  public ApiResponse<List<TopicMaterialResponse>> getTopicMaterials(@PathVariable UUID topicId) {
    return ApiResponse.<List<TopicMaterialResponse>>builder()
        .result(roadmapAdminService.getTopicMaterials(topicId))
        .build();
  }

  @PostMapping("/roadmaps/{roadmapId}/entry-test/submit")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Submit roadmap entry test (legacy)")
  public ApiResponse<RoadmapEntryTestResultResponse> submitRoadmapEntryTest(
      @PathVariable UUID roadmapId, @Valid @RequestBody SubmitRoadmapEntryTestRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapEntryTestResultResponse>builder()
        .message("Roadmap entry test submitted successfully")
        .result(roadmapAdminService.submitEntryTest(studentId, roadmapId, request)).build();
  }

  @GetMapping("/roadmaps/{roadmapId}/entry-test")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get roadmap entry test info")
  public ApiResponse<RoadmapEntryTestInfoResponse> getRoadmapEntryTest(
      @PathVariable UUID roadmapId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapEntryTestInfoResponse>builder()
        .result(roadmapAdminService.getEntryTestForStudent(studentId, roadmapId)).build();
  }

  @PostMapping("/roadmaps/{roadmapId}/entry-test/attempts/{attemptId}/answers")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Save entry test answer")
  public ApiResponse<AnswerAckResponse> saveRoadmapEntryTestAnswer(
      @PathVariable UUID roadmapId, @PathVariable UUID attemptId,
      @Valid @RequestBody RoadmapEntryTestAnswerRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<AnswerAckResponse>builder()
        .result(roadmapAdminService.saveEntryTestAnswer(studentId, roadmapId, attemptId, request))
        .build();
  }

  @PostMapping("/roadmaps/{roadmapId}/entry-test/attempts/{attemptId}/flags")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Update entry test flag")
  public ApiResponse<AnswerAckResponse> updateRoadmapEntryTestFlag(
      @PathVariable UUID roadmapId, @PathVariable UUID attemptId,
      @Valid @RequestBody RoadmapEntryTestFlagRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<AnswerAckResponse>builder()
        .result(roadmapAdminService.updateEntryTestFlag(studentId, roadmapId, attemptId, request))
        .build();
  }

  @GetMapping("/roadmaps/{roadmapId}/entry-test/attempts/{attemptId}/snapshot")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get entry test draft snapshot")
  public ApiResponse<RoadmapEntryTestSnapshotResponse> getRoadmapEntryTestSnapshot(
      @PathVariable UUID roadmapId, @PathVariable UUID attemptId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapEntryTestSnapshotResponse>builder()
        .result(roadmapAdminService.getEntryTestSnapshot(studentId, roadmapId, attemptId)).build();
  }

  @PostMapping("/roadmaps/{roadmapId}/entry-test/attempts/{attemptId}/save-exit")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Save and exit entry test")
  public ApiResponse<Void> saveAndExitRoadmapEntryTest(
      @PathVariable UUID roadmapId, @PathVariable UUID attemptId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    roadmapAdminService.saveEntryTestAndExit(studentId, roadmapId, attemptId);
    return ApiResponse.<Void>builder().message("Progress saved successfully").build();
  }

  @GetMapping("/roadmaps/{roadmapId}/entry-test/active-attempt")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get active roadmap entry test attempt")
  public ApiResponse<RoadmapEntryTestActiveAttemptResponse> getActiveRoadmapEntryTestAttempt(
      @PathVariable UUID roadmapId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapEntryTestActiveAttemptResponse>builder()
        .result(roadmapAdminService.getActiveEntryTestAttempt(studentId, roadmapId)).build();
  }

  @PostMapping("/roadmaps/{roadmapId}/entry-test/start")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Start roadmap entry test")
  public ApiResponse<AttemptStartResponse> startRoadmapEntryTest(@PathVariable UUID roadmapId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<AttemptStartResponse>builder()
        .result(roadmapAdminService.startEntryTest(studentId, roadmapId)).build();
  }

  @PostMapping("/roadmaps/{roadmapId}/entry-test/attempts/{attemptId}/finish")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Finish roadmap entry test")
  public ApiResponse<RoadmapEntryTestResultResponse> finishRoadmapEntryTest(
      @PathVariable UUID roadmapId, @PathVariable UUID attemptId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapEntryTestResultResponse>builder()
        .message("Roadmap entry test finished successfully")
        .result(roadmapAdminService.finishEntryTest(studentId, roadmapId, attemptId)).build();
  }

  @PostMapping("/roadmaps/{roadmapId}/feedback")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Submit roadmap feedback")
  public ApiResponse<RoadmapFeedbackResponse> submitRoadmapFeedback(
      @PathVariable UUID roadmapId, @Valid @RequestBody CreateRoadmapFeedbackRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapFeedbackResponse>builder()
        .message("Roadmap feedback submitted successfully")
        .result(roadmapFeedbackService.submitFeedback(roadmapId, studentId, request)).build();
  }

  @GetMapping("/roadmaps/{roadmapId}/feedback/me")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get my roadmap feedback")
  public ApiResponse<RoadmapFeedbackResponse> getMyRoadmapFeedback(@PathVariable UUID roadmapId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapFeedbackResponse>builder()
        .result(roadmapFeedbackService.getMyFeedback(roadmapId, studentId)).build();
  }
}
