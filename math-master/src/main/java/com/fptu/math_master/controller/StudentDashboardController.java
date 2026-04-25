package com.fptu.math_master.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.StudentDashboardLearningProgressResponse;
import com.fptu.math_master.dto.response.StudentDashboardOverviewResponse;
import com.fptu.math_master.dto.response.StudentDashboardRecentGradeResponse;
import com.fptu.math_master.dto.response.StudentDashboardStreakResponse;
import com.fptu.math_master.dto.response.StudentDashboardSummaryResponse;
import com.fptu.math_master.dto.response.StudentDashboardUpcomingTaskResponse;
import com.fptu.math_master.dto.response.StudentDashboardWeeklyActivityResponse;
import com.fptu.math_master.service.StudentDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping({"/student/dashboard", "/api/student/dashboard"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Student Dashboard", description = "APIs for student dashboard")
@SecurityRequirement(name = "bearerAuth")
public class StudentDashboardController {

  StudentDashboardService studentDashboardService;

  @GetMapping("/summary")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get student dashboard summary")
  public ApiResponse<StudentDashboardSummaryResponse> getSummary() {
    return ApiResponse.<StudentDashboardSummaryResponse>builder()
        .result(studentDashboardService.getSummary())
        .build();
  }

  @GetMapping("/upcoming-tasks")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get upcoming tasks")
  public ApiResponse<List<StudentDashboardUpcomingTaskResponse>> getUpcomingTasks() {
    return ApiResponse.<List<StudentDashboardUpcomingTaskResponse>>builder()
        .result(studentDashboardService.getUpcomingTasks())
        .build();
  }

  @GetMapping("/recent-grades")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get recent grades")
  public ApiResponse<List<StudentDashboardRecentGradeResponse>> getRecentGrades() {
    return ApiResponse.<List<StudentDashboardRecentGradeResponse>>builder()
        .result(studentDashboardService.getRecentGrades())
        .build();
  }

  @GetMapping("/learning-progress")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get learning progress by subject")
  public ApiResponse<List<StudentDashboardLearningProgressResponse>> getLearningProgress() {
    return ApiResponse.<List<StudentDashboardLearningProgressResponse>>builder()
        .result(studentDashboardService.getLearningProgress())
        .build();
  }

  @GetMapping("/weekly-activity")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get weekly activity")
  public ApiResponse<StudentDashboardWeeklyActivityResponse> getWeeklyActivity() {
    return ApiResponse.<StudentDashboardWeeklyActivityResponse>builder()
        .result(studentDashboardService.getWeeklyActivity())
        .build();
  }

  @GetMapping("/streak")
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get study streak")
  public ApiResponse<StudentDashboardStreakResponse> getStreak() {
    return ApiResponse.<StudentDashboardStreakResponse>builder()
        .result(studentDashboardService.getStreak())
        .build();
  }

  @GetMapping
  @PreAuthorize("hasRole('STUDENT')")
  @Operation(summary = "Get full dashboard payload")
  public ApiResponse<StudentDashboardOverviewResponse> getOverview() {
    return ApiResponse.<StudentDashboardOverviewResponse>builder()
        .result(studentDashboardService.getOverview())
        .build();
  }
}
