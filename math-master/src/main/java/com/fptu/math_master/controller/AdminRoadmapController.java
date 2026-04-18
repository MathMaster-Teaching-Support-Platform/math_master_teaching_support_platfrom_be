package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.service.RoadmapAdminService;
import com.fptu.math_master.service.RoadmapFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/roadmaps")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class AdminRoadmapController {

  RoadmapAdminService roadmapAdminService;
  RoadmapFeedbackService roadmapFeedbackService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get all roadmaps")
  public ApiResponse<Page<RoadmapSummaryResponse>> getAllRoadmaps(
      @RequestParam(required = false) String name,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<RoadmapSummaryResponse>>builder()
        .result(roadmapAdminService.getAllRoadmaps(name, pageable)).build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get roadmap detail by ID")
  public ApiResponse<RoadmapDetailResponse> getRoadmap(@PathVariable UUID id) {
    return ApiResponse.<RoadmapDetailResponse>builder()
        .result(roadmapAdminService.getRoadmap(id)).build();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Create roadmap template")
  public ApiResponse<RoadmapDetailResponse> createRoadmap(
      @Valid @RequestBody CreateAdminRoadmapRequest request) {
    return ApiResponse.<RoadmapDetailResponse>builder()
        .message("Roadmap created successfully")
        .result(roadmapAdminService.createRoadmap(request)).build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update roadmap metadata")
  public ApiResponse<RoadmapDetailResponse> updateRoadmap(
      @PathVariable UUID id, @Valid @RequestBody UpdateAdminRoadmapRequest request) {
    return ApiResponse.<RoadmapDetailResponse>builder()
        .message("Roadmap updated successfully")
        .result(roadmapAdminService.updateRoadmap(id, request)).build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Archive roadmap")
  public ApiResponse<String> softDeleteRoadmap(@PathVariable UUID id) {
    roadmapAdminService.softDeleteRoadmap(id);
    return ApiResponse.<String>builder().message("Roadmap archived successfully").result("OK").build();
  }

  @PostMapping("/{id}/topics")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Add topic to roadmap", description = "Each topic links to exactly one course")
  public ApiResponse<RoadmapTopicResponse> addTopic(
      @PathVariable UUID id, @Valid @RequestBody CreateRoadmapTopicRequest request) {
    return ApiResponse.<RoadmapTopicResponse>builder()
        .message("Roadmap topic created successfully")
        .result(roadmapAdminService.addTopic(id, request)).build();
  }

  @PutMapping("/{roadmapId}/topics/{topicId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update roadmap topic")
  public ApiResponse<RoadmapTopicResponse> updateTopic(
      @PathVariable UUID roadmapId, @PathVariable UUID topicId,
      @Valid @RequestBody UpdateRoadmapTopicRequest request) {
    return ApiResponse.<RoadmapTopicResponse>builder()
        .message("Roadmap topic updated successfully")
        .result(roadmapAdminService.updateTopic(roadmapId, topicId, request)).build();
  }

  @DeleteMapping("/{roadmapId}/topics/{topicId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Archive roadmap topic")
  public ApiResponse<String> softDeleteTopic(
      @PathVariable UUID roadmapId, @PathVariable UUID topicId) {
    roadmapAdminService.softDeleteTopic(roadmapId, topicId);
    return ApiResponse.<String>builder().message("Roadmap topic archived successfully").result("OK").build();
  }

  @PostMapping("/{roadmapId}/topics/batch")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Batch create/update/delete topics",
      description = "Process multiple topic operations in a single transaction. "
          + "If topic.id is null → CREATE new topic. "
          + "If topic.id exists and status is ACTIVE → UPDATE existing topic. "
          + "If topic.id exists and status is INACTIVE → SOFT DELETE topic.")
  public ApiResponse<java.util.List<RoadmapTopicResponse>> batchSaveTopics(
      @PathVariable UUID roadmapId, @Valid @RequestBody BatchTopicRequest request) {
    // Ensure roadmapId in path matches request body
    if (!roadmapId.equals(request.getRoadmapId())) {
      throw new IllegalArgumentException("Roadmap ID in path must match request body");
    }
    return ApiResponse.<java.util.List<RoadmapTopicResponse>>builder()
        .message("Batch operation completed successfully")
        .result(roadmapAdminService.batchSaveTopics(request))
        .build();
  }

  @PostMapping("/{roadmapId}/entry-test")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Configure roadmap entry test")
  public ApiResponse<String> configureRoadmapEntryTest(
      @PathVariable UUID roadmapId, @Valid @RequestBody CreateRoadmapEntryTestRequest request) {
    roadmapAdminService.configureEntryTest(roadmapId, request);
    return ApiResponse.<String>builder()
        .message("Roadmap entry test configured successfully").result("OK").build();
  }

  @PutMapping("/{roadmapId}/entry-test")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Set or update roadmap entry test (placement test)")
  public ApiResponse<String> setRoadmapEntryTest(
      @PathVariable UUID roadmapId, @RequestParam UUID entryTestId) {
    roadmapAdminService.setRoadmapEntryTest(roadmapId, entryTestId);
    return ApiResponse.<String>builder()
        .message("Roadmap entry test set successfully").result("OK").build();
  }

  @DeleteMapping("/{roadmapId}/entry-test")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Remove roadmap entry test")
  public ApiResponse<String> removeRoadmapEntryTest(@PathVariable UUID roadmapId) {
    roadmapAdminService.removeRoadmapEntryTest(roadmapId);
    return ApiResponse.<String>builder()
        .message("Roadmap entry test removed successfully").result("OK").build();
  }

  @GetMapping("/{roadmapId}/feedback")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "List roadmap feedback")
  public ApiResponse<Page<RoadmapFeedbackResponse>> getRoadmapFeedbacks(
      @PathVariable UUID roadmapId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<RoadmapFeedbackResponse>>builder()
        .result(roadmapFeedbackService.getRoadmapFeedbacks(roadmapId, pageable)).build();
  }
}
