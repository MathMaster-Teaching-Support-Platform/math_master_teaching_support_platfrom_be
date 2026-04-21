package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.request.UpdateProgressRequest;
import com.fptu.math_master.dto.response.LessonProgressItem;
import com.fptu.math_master.dto.response.StudentProgressResponse;
import com.fptu.math_master.service.ProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Progress", description = "APIs for tracking lesson progress")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

  ProgressService progressService;

  @Operation(summary = "Mark a lesson as complete")
  @PostMapping("/{enrollmentId}/lessons/{courseLessonId}/complete")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<LessonProgressItem> markComplete(
      @PathVariable UUID enrollmentId, @PathVariable UUID courseLessonId) {
    log.info("POST /enrollments/{}/lessons/{}/complete", enrollmentId, courseLessonId);
    return ApiResponse.<LessonProgressItem>builder()
        .result(progressService.markComplete(enrollmentId, courseLessonId))
        .build();
  }

  @Operation(summary = "Update lesson progress (watched seconds)")
  @PostMapping("/{enrollmentId}/lessons/{courseLessonId}/progress")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<LessonProgressItem> updateProgress(
      @PathVariable UUID enrollmentId,
      @PathVariable UUID courseLessonId,
      @org.springframework.web.bind.annotation.RequestBody UpdateProgressRequest request) {
    return ApiResponse.<LessonProgressItem>builder()
        .result(progressService.updateProgress(enrollmentId, courseLessonId, request))
        .build();
  }

  @Operation(summary = "Get progress for an enrollment")
  @GetMapping("/{enrollmentId}/progress")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<StudentProgressResponse> getProgress(@PathVariable UUID enrollmentId) {
    return ApiResponse.<StudentProgressResponse>builder()
        .result(progressService.getProgress(enrollmentId))
        .build();
  }
}
