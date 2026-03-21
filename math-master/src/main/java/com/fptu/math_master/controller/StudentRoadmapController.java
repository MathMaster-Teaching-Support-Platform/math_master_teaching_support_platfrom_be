package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestResultResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.TopicMaterialResponse;
import com.fptu.math_master.dto.request.SubmitRoadmapEntryTestRequest;
import com.fptu.math_master.service.RoadmapAdminService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
public class StudentRoadmapController {

  RoadmapAdminService roadmapAdminService;

  @GetMapping("/roadmaps")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(
      summary = "Browse available roadmaps",
      description = "Get all published roadmap templates for students")
  public ApiResponse<Page<RoadmapSummaryResponse>> getAvailableRoadmaps(
      @RequestParam(required = false) String name,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<RoadmapSummaryResponse>>builder()
        .result(roadmapAdminService.getPublishedRoadmaps(name, pageable))
        .build();
  }

  @GetMapping("/roadmaps/{id}")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get roadmap", description = "Get full roadmap template with topics and content")
  public ApiResponse<RoadmapDetailResponse> getRoadmap(@PathVariable UUID id) {
    return ApiResponse.<RoadmapDetailResponse>builder()
      .result(roadmapAdminService.getPublishedRoadmap(id))
        .build();
  }

  @GetMapping("/roadmaps/topics/{topicId}/materials")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(
      summary = "Get topic content materials",
      description = "Get lesson content resources for a topic (slide/question/mindmap/document)")
  public ApiResponse<List<TopicMaterialResponse>> getTopicMaterials(@PathVariable UUID topicId) {
    return ApiResponse.<List<TopicMaterialResponse>>builder()
      .result(roadmapAdminService.getTopicMaterials(topicId))
        .build();
  }

  @GetMapping("/roadmaps/topics/{topicId}/materials-by-type")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(
      summary = "Get topic materials by type",
      description = "Filter topic content by resource type (SLIDE, QUESTION, MINDMAP, DOCUMENT)")
  public ApiResponse<List<TopicMaterialResponse>> getTopicMaterialsByType(
      @PathVariable UUID topicId, @RequestParam String resourceType) {
    return ApiResponse.<List<TopicMaterialResponse>>builder()
        .result(roadmapAdminService.getMaterialsByType(topicId, resourceType))
        .build();
  }

  @PostMapping("/roadmaps/{roadmapId}/entry-test/submit")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(
      summary = "Submit roadmap entry test",
      description = "Submit entry test result and return suggested topic level in roadmap")
  public ApiResponse<RoadmapEntryTestResultResponse> submitRoadmapEntryTest(
      @PathVariable UUID roadmapId, @Valid @RequestBody SubmitRoadmapEntryTestRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<RoadmapEntryTestResultResponse>builder()
        .message("Roadmap entry test submitted successfully")
        .result(roadmapAdminService.submitEntryTest(studentId, roadmapId, request))
        .build();
  }
}
