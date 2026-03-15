package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.SubmitPlacementTestRequest;
import com.fptu.math_master.dto.request.SubmitTopicAssessmentRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.StudentRoadmapProgressResponse;
import com.fptu.math_master.service.PlacementTestService;
import com.fptu.math_master.service.RoadmapProgressService;
import com.fptu.math_master.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class StudentRoadmapController {

  PlacementTestService placementTestService;
  RoadmapProgressService roadmapProgressService;

  @GetMapping("/roadmaps/{id}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get roadmap", description = "Get full roadmap for current student")
  public ApiResponse<RoadmapDetailResponse> getRoadmap(@PathVariable UUID id) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapDetailResponse>builder()
        .result(roadmapProgressService.getRoadmapForStudent(studentId, id))
        .build();
  }

  @PostMapping("/placement-test/submit")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(
      summary = "Submit placement test",
      description = "Compute suggested roadmap topic from placement submission")
  public ApiResponse<StudentRoadmapProgressResponse> submitPlacementTest(
      @Valid @RequestBody SubmitPlacementTestRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<StudentRoadmapProgressResponse>builder()
        .message("Placement test submitted successfully")
        .result(placementTestService.submitPlacementTest(studentId, request))
        .build();
  }

  @GetMapping("/roadmap-progress/{id}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get roadmap progress", description = "Get progress for current student")
  public ApiResponse<StudentRoadmapProgressResponse> getRoadmapProgress(@PathVariable UUID id) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<StudentRoadmapProgressResponse>builder()
        .result(roadmapProgressService.getRoadmapProgress(studentId, id))
        .build();
  }

  @PostMapping("/topic-assessment/submit")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(
      summary = "Submit topic assessment result",
      description = "Advance topic progression based on pass threshold")
  public ApiResponse<StudentRoadmapProgressResponse> submitTopicAssessment(
      @Valid @RequestBody SubmitTopicAssessmentRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<StudentRoadmapProgressResponse>builder()
        .message("Topic assessment submitted successfully")
        .result(roadmapProgressService.submitTopicAssessment(studentId, request))
        .build();
  }
}
