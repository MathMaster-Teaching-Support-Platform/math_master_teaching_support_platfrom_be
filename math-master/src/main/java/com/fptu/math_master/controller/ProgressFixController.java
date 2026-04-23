package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.service.ProgressRecalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/progress-fix")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class ProgressFixController {

  ProgressRecalculationService progressRecalculationService;

  @PostMapping("/recalculate/{roadmapId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Recalculate progress for a specific roadmap")
  public ApiResponse<String> recalculateRoadmapProgress(@PathVariable UUID roadmapId) {
    progressRecalculationService.recalculateRoadmapProgress(roadmapId);
    return ApiResponse.<String>builder()
        .message("Progress recalculated successfully")
        .result("OK")
        .build();
  }

  @PostMapping("/recalculate-all")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Recalculate progress for all roadmaps")
  public ApiResponse<String> recalculateAllProgress() {
    progressRecalculationService.recalculateAllRoadmapProgress();
    return ApiResponse.<String>builder()
        .message("All roadmap progress recalculated successfully")
        .result("OK")
        .build();
  }

  @PostMapping("/cleanup-entry-tests")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Clean up invalid entry test references")
  public ApiResponse<String> cleanupInvalidEntryTests() {
    progressRecalculationService.cleanupInvalidEntryTests();
    return ApiResponse.<String>builder()
        .message("Invalid entry test references cleaned up successfully")
        .result("OK")
        .build();
  }

  @GetMapping("/diagnose/{roadmapId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Diagnose progress calculation issues for a roadmap")
  public ApiResponse<String> diagnoseProgressIssues(@PathVariable UUID roadmapId) {
    String diagnosis = progressRecalculationService.diagnoseProgressIssues(roadmapId);
    return ApiResponse.<String>builder()
        .message("Progress diagnosis completed")
        .result(diagnosis)
        .build();
  }
}