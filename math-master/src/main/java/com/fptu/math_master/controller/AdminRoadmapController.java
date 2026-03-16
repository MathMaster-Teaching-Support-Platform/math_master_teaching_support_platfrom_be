package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreatePlacementTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.service.RoadmapAdminService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/roadmaps")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class AdminRoadmapController {

  RoadmapAdminService roadmapAdminService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Get all roadmaps",
      description = "Get all roadmaps with pagination and optional student name search")
  public ApiResponse<Page<RoadmapSummaryResponse>> getAllRoadmaps(
      @RequestParam(required = false) String name,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<RoadmapSummaryResponse>>builder()
        .result(roadmapAdminService.getAllRoadmaps(name, pageable))
        .build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Get roadmap", description = "Get roadmap detail by ID")
  public ApiResponse<RoadmapDetailResponse> getRoadmap(@PathVariable UUID id) {
    return ApiResponse.<RoadmapDetailResponse>builder()
        .result(roadmapAdminService.getRoadmap(id))
        .build();
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Create roadmap", description = "Create admin-managed roadmap for a student")
  public ApiResponse<RoadmapDetailResponse> createRoadmap(
      @Valid @RequestBody CreateAdminRoadmapRequest request) {
    return ApiResponse.<RoadmapDetailResponse>builder()
        .message("Roadmap created successfully")
        .result(roadmapAdminService.createRoadmap(request))
        .build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update roadmap", description = "Update roadmap basic metadata")
  public ApiResponse<RoadmapDetailResponse> updateRoadmap(
      @PathVariable UUID id, @Valid @RequestBody UpdateAdminRoadmapRequest request) {
    return ApiResponse.<RoadmapDetailResponse>builder()
        .message("Roadmap updated successfully")
        .result(roadmapAdminService.updateRoadmap(id, request))
        .build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Soft delete roadmap", description = "Archive roadmap via soft delete")
  public ApiResponse<String> softDeleteRoadmap(@PathVariable UUID id) {
    roadmapAdminService.softDeleteRoadmap(id);
    return ApiResponse.<String>builder()
        .message("Roadmap archived successfully")
        .result("OK")
        .build();
  }

  @PostMapping("/{id}/topics")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Add roadmap topic", description = "Add ordered topic to roadmap")
  public ApiResponse<RoadmapTopicResponse> addTopic(
      @PathVariable UUID id, @Valid @RequestBody CreateRoadmapTopicRequest request) {
    return ApiResponse.<RoadmapTopicResponse>builder()
        .message("Roadmap topic created successfully")
        .result(roadmapAdminService.addTopic(id, request))
        .build();
  }

  @PostMapping("/placement-tests")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Configure placement test mappings",
      description = "Bind placement assessment questions to roadmap topics")
  public ApiResponse<String> createPlacementTest(
      @Valid @RequestBody CreatePlacementTestRequest request) {
    roadmapAdminService.createPlacementTest(request);
    return ApiResponse.<String>builder()
        .message("Placement test mappings configured successfully")
        .result("OK")
        .build();
  }
}
