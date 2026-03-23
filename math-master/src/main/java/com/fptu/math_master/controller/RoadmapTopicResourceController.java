package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.AttachResourcesToRoadmapTopicRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.TeachingResourceResponse;
import com.fptu.math_master.service.RoadmapTopicResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/roadmap-topics")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Roadmap Topic Resources", description = "Attach teaching resources to roadmap topics")
public class RoadmapTopicResourceController {

  RoadmapTopicResourceService roadmapTopicResourceService;

  @PostMapping("/{id}/resources")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "Attach resources to topic")
  public ApiResponse<List<TeachingResourceResponse>> attachResources(
      @PathVariable UUID id, @Valid @RequestBody AttachResourcesToRoadmapTopicRequest request) {
    return ApiResponse.<List<TeachingResourceResponse>>builder()
        .message("Resources attached to topic successfully")
        .result(roadmapTopicResourceService.attachResources(id, request.getResourceIds()))
        .build();
  }

  @GetMapping("/{id}/resources")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(summary = "Get topic resources")
  public ApiResponse<List<TeachingResourceResponse>> getTopicResources(@PathVariable UUID id) {
    return ApiResponse.<List<TeachingResourceResponse>>builder()
        .result(roadmapTopicResourceService.getResourcesOfTopic(id))
        .build();
  }

  @DeleteMapping("/{topicId}/resources/{resourceId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(summary = "Remove a resource from topic")
  public ApiResponse<Void> removeResourceFromTopic(
      @PathVariable UUID topicId, @PathVariable UUID resourceId) {
    roadmapTopicResourceService.removeResource(topicId, resourceId);
    return ApiResponse.<Void>builder().message("Resource removed from topic successfully").build();
  }
}
