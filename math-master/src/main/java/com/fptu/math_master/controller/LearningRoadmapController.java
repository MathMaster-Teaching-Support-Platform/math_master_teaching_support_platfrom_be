package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CompleteSubtopicRequest;
import com.fptu.math_master.dto.request.GenerateRoadmapRequest;
import com.fptu.math_master.dto.request.UpdateTopicProgressRequest;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.service.LearningRoadmapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Student Learning Roadmap management
 *
 * <p>Endpoints:
 * - Generate personalized learning roadmaps
 * - Retrieve roadmaps and progress
 * - Update topic progress and completion
 * - Analyze weak areas
 * - Link learning materials
 *
 * <p>Access Control:
 * - Students: can view/update their own roadmaps
 * - Teachers: can assign/customize roadmaps
 * - Admin: full access
 */
@RestController
@RequestMapping("/roadmaps")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(
    name = "Learning Roadmaps",
    description = "Generate and manage personalized learning roadmaps for students")
@SecurityRequirement(name = "bearerAuth")
public class LearningRoadmapController {

  LearningRoadmapService roadmapService;

  // ============================================================================
  // ROADMAP GENERATION & RETRIEVAL
  // ============================================================================

  @PostMapping("/generate")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(
      summary = "Generate a learning roadmap for a student",
      description =
          "Generate a personalized learning roadmap based on student performance, "
              + "or create a default grade-based roadmap. "
              + "Supports PERSONALIZED, DEFAULT, and TEACHER_ASSIGNED generation types.")

  public ApiResponse<RoadmapDetailResponse> generateRoadmap(
      @Valid @RequestBody GenerateRoadmapRequest request) {
    // Extract studentId from JWT token
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID studentId = UUID.fromString(userId);

    log.info("POST /roadmaps/generate – studentId={}, subject={}, type={}",
        studentId, request.getSubject(), request.getGenerationType());


    RoadmapDetailResponse result = roadmapService.generateRoadmap(request);

    return ApiResponse.<RoadmapDetailResponse>builder()
        .code(201)
        .message("Roadmap generated successfully")
        .result(result)
        .build();
  }

  @PostMapping("/generate-from-wish/{wishId}")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(
      summary = "Generate AI-powered roadmap from student wish",
      description = "Generate a personalized learning roadmap based on student's learning wishes "
          + "and goals using AI analysis")
  public ApiResponse<RoadmapDetailResponse> generateRoadmapFromWish(
      @Parameter(description = "Student Wish ID", example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable UUID wishId) {
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID studentId = UUID.fromString(userId);

    log.info("POST /roadmaps/generate-from-wish/{} – studentId={}", wishId, studentId);

    RoadmapDetailResponse result = roadmapService.generateRoadmapFromWish(wishId);

    return ApiResponse.<RoadmapDetailResponse>builder()
        .code(201)
        .message("AI-powered roadmap generated successfully from student wish")
        .result(result)
        .build();
  }

  @GetMapping("/{roadmapId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get roadmap details", description = "Retrieve detailed roadmap with all topics and materials")
  public ApiResponse<RoadmapDetailResponse> getRoadmapById(
      @Parameter(description = "Roadmap ID", example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable UUID roadmapId) {
    log.info("GET /roadmaps/{} – retrieving roadmap", roadmapId);

    RoadmapDetailResponse result = roadmapService.getRoadmapById(roadmapId);

    return ApiResponse.<RoadmapDetailResponse>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/student/{studentId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Get all roadmaps for a student",
      description = "List all roadmaps for a specific student with pagination")
  public ApiResponse<Page<RoadmapSummaryResponse>> getStudentRoadmaps(
      @Parameter(description = "Student ID") @PathVariable UUID studentId,
      @Parameter(description = "Pagination parameters") Pageable pageable) {
    log.info("GET /roadmaps/student/{} – listing roadmaps", studentId);

    Page<RoadmapSummaryResponse> result = roadmapService.getStudentRoadmaps(studentId, pageable);

    return ApiResponse.<Page<RoadmapSummaryResponse>>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/student/{studentId}/subject/{subject}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Get active roadmap by subject",
      description = "Retrieve the active (most recent) roadmap for a student in a specific subject")
  public ApiResponse<RoadmapDetailResponse> getActiveRoadmapBySubject(
      @Parameter(description = "Student ID") @PathVariable UUID studentId,
      @Parameter(description = "Subject name") @PathVariable String subject) {
    log.info("GET /roadmaps/student/{}/subject/{} – getting active roadmap", studentId, subject);

    RoadmapDetailResponse result = roadmapService.getActiveRoadmapBySubject(studentId, subject);

    return ApiResponse.<RoadmapDetailResponse>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/student/{studentId}/list")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "List all student roadmaps", description = "Get all roadmaps for a student without pagination")
  public ApiResponse<List<RoadmapSummaryResponse>> getStudentRoadmapsList(
      @Parameter(description = "Student ID") @PathVariable UUID studentId) {
    log.info("GET /roadmaps/student/{}/list – listing all roadmaps", studentId);

    List<RoadmapSummaryResponse> result = roadmapService.getStudentRoadmapsList(studentId);

    return ApiResponse.<List<RoadmapSummaryResponse>>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/teacher/{teacherId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get teacher-assigned roadmaps", description = "Get all roadmaps assigned by a teacher")
  public ApiResponse<List<RoadmapSummaryResponse>> getTeacherAssignedRoadmaps(
      @Parameter(description = "Teacher ID") @PathVariable UUID teacherId) {
    log.info("GET /roadmaps/teacher/{} – getting assigned roadmaps", teacherId);

    List<RoadmapSummaryResponse> result = roadmapService.getTeacherAssignedRoadmaps(teacherId);

    return ApiResponse.<List<RoadmapSummaryResponse>>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  // ============================================================================
  // PROGRESS TRACKING
  // ============================================================================

  @PutMapping("/topics/{topicId}/progress")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update topic progress", description = "Update the progress status of a topic in roadmap")
  public ApiResponse<RoadmapTopicResponse> updateTopicProgress(
      @Parameter(description = "Topic ID") @PathVariable UUID topicId,
      @Valid @RequestBody UpdateTopicProgressRequest request) {
    log.info("PUT /roadmaps/topics/{}/progress – updating progress", topicId);

    request.setTopicId(topicId);
    RoadmapTopicResponse result = roadmapService.updateTopicProgress(request);

    return ApiResponse.<RoadmapTopicResponse>builder()
        .code(1000)
        .message("Topic progress updated successfully")
        .result(result)
        .build();
  }

  @PostMapping("/subtopics/{subtopicId}/complete")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Complete a subtopic", description = "Mark a subtopic as completed and update parent topic progress")
  public ApiResponse<RoadmapSubtopicResponse> completeSubtopic(
      @Parameter(description = "Subtopic ID") @PathVariable UUID subtopicId,
      @Valid @RequestBody CompleteSubtopicRequest request) {
    log.info("POST /roadmaps/subtopics/{}/complete – completing subtopic", subtopicId);

    request.setSubtopicId(subtopicId);
    RoadmapSubtopicResponse result = roadmapService.completeSubtopic(request);

    return ApiResponse.<RoadmapSubtopicResponse>builder()
        .code(1000)
        .message("Subtopic completed successfully")
        .result(result)
        .build();
  }

  @GetMapping("/{roadmapId}/next-topic")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Get next topic to learn",
      description = "Get the recommended next topic for the student to study")
  public ApiResponse<RoadmapTopicResponse> getNextTopic(
      @Parameter(description = "Roadmap ID") @PathVariable UUID roadmapId) {
    log.info("GET /roadmaps/{}/next-topic – getting next topic", roadmapId);

    RoadmapTopicResponse result = roadmapService.getNextTopic(roadmapId);

    return ApiResponse.<RoadmapTopicResponse>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/{roadmapId}/stats")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get roadmap statistics", description = "Get progress statistics and analytics for a roadmap")
  public ApiResponse<RoadmapStatsResponse> getRoadmapStats(
      @Parameter(description = "Roadmap ID") @PathVariable UUID roadmapId) {
    log.info("GET /roadmaps/{}/stats – getting statistics", roadmapId);

    RoadmapStatsResponse result = roadmapService.getRoadmapStats(roadmapId);

    return ApiResponse.<RoadmapStatsResponse>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  // ============================================================================
  // TOPIC & MATERIALS MANAGEMENT
  // ============================================================================

  @GetMapping("/topics/{topicId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get topic details", description = "Retrieve detailed information about a specific topic")
  public ApiResponse<RoadmapTopicResponse> getTopicDetails(
      @Parameter(description = "Topic ID") @PathVariable UUID topicId) {
    log.info("GET /roadmaps/topics/{} – getting topic details", topicId);

    RoadmapTopicResponse result = roadmapService.getTopicDetails(topicId);

    return ApiResponse.<RoadmapTopicResponse>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/topics/{topicId}/materials")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get topic materials", description = "Get all learning materials (lessons, questions) for a topic")
  public ApiResponse<List<TopicMaterialResponse>> getTopicMaterials(
      @Parameter(description = "Topic ID") @PathVariable UUID topicId) {
    log.info("GET /roadmaps/topics/{}/materials – getting materials", topicId);

    List<TopicMaterialResponse> result = roadmapService.getTopicMaterials(topicId);

    return ApiResponse.<List<TopicMaterialResponse>>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/topics/{topicId}/materials-by-type")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Get materials by type",
      description = "Get learning materials filtered by type (LESSON, QUESTION, PRACTICE, etc.)")
  public ApiResponse<List<TopicMaterialResponse>> getMaterialsByType(
      @Parameter(description = "Topic ID") @PathVariable UUID topicId,
      @Parameter(description = "Resource type (LESSON, QUESTION, PRACTICE, EXAMPLE, ASSESSMENT)")
      @RequestParam String resourceType) {
    log.info("GET /roadmaps/topics/{}/materials-by-type – getting {} materials", topicId, resourceType);

    List<TopicMaterialResponse> result = roadmapService.getMaterialsByType(topicId, resourceType);

    return ApiResponse.<List<TopicMaterialResponse>>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @PostMapping("/topics/{topicId}/materials")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Link material to topic", description = "Add a learning resource (lesson/question) to a topic")
  public ApiResponse<TopicMaterialResponse> linkMaterialToTopic(
      @Parameter(description = "Topic ID") @PathVariable UUID topicId,
      @Parameter(description = "Lesson ID (optional)") @RequestParam(required = false) UUID lessonId,
      @Parameter(description = "Question ID (optional)") @RequestParam(required = false) UUID questionId,
      @Parameter(description = "Resource type") @RequestParam String resourceType,
      @Parameter(description = "Is required") @RequestParam(defaultValue = "true") Boolean isRequired) {
    log.info("POST /roadmaps/topics/{}/materials – linking material", topicId);

    TopicMaterialResponse result = roadmapService.linkMaterialToTopic(topicId, lessonId, questionId, resourceType,
        isRequired);

    return ApiResponse.<TopicMaterialResponse>builder()
        .code(1000)
        .message("Material linked successfully")
        .result(result)
        .build();
  }

  @DeleteMapping("/materials/{materialId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Remove material from topic", description = "Remove a learning resource from a topic")
  public ApiResponse<String> removeMaterialFromTopic(
      @Parameter(description = "Material ID") @PathVariable UUID materialId) {
    log.info("DELETE /roadmaps/materials/{} – removing material", materialId);

    roadmapService.removeMaterialFromTopic(materialId);

    return ApiResponse.<String>builder()
        .code(1000)
        .message("Material removed successfully")
        .result("OK")
        .build();
  }

  // ============================================================================
  // WEAK AREA ANALYSIS
  // ============================================================================

  @GetMapping("/analysis/weak-topics")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Analyze weak topics",
      description = "Identify and analyze weak topics for a student based on performance data")
  public ApiResponse<List<RoadmapTopicResponse>> analyzeWeakTopics(
      @Parameter(description = "Student ID") @RequestParam UUID studentId,
      @Parameter(description = "Subject name") @RequestParam String subject) {
    log.info("GET /roadmaps/analysis/weak-topics – analyzing student={}, subject={}", studentId, subject);

    List<RoadmapTopicResponse> result = roadmapService.analyzeWeakTopics(studentId, subject);

    return ApiResponse.<List<RoadmapTopicResponse>>builder()
        .code(1000)
        .message("Analysis complete")
        .result(result)
        .build();
  }

  @PutMapping("/{roadmapId}/refresh-with-performance")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Refresh roadmap with performance data",
      description = "Re-analyze and update roadmap priorities based on latest performance data")
  public ApiResponse<RoadmapDetailResponse> refreshRoadmapWithPerformanceData(
      @Parameter(description = "Roadmap ID") @PathVariable UUID roadmapId) {
    log.info("PUT /roadmaps/{}/refresh-with-performance – refreshing roadmap", roadmapId);

    RoadmapDetailResponse result = roadmapService.refreshRoadmapWithPerformanceData(roadmapId);

    return ApiResponse.<RoadmapDetailResponse>builder()
        .code(1000)
        .message("Roadmap refreshed with updated priorities")
        .result(result)
        .build();
  }

  // ============================================================================
  // UTILITY & ADMINISTRATION
  // ============================================================================

  @GetMapping("/check-exists")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Check if roadmap exists", description = "Check if an active roadmap exists for student and subject")
  public ApiResponse<Boolean> existsActiveRoadmap(
      @Parameter(description = "Student ID") @RequestParam UUID studentId,
      @Parameter(description = "Subject name") @RequestParam String subject) {
    log.info("GET /roadmaps/check-exists – checking student={}, subject={}", studentId, subject);

    boolean result = roadmapService.existsActiveRoadmap(studentId, subject);

    return ApiResponse.<Boolean>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @DeleteMapping("/{roadmapId}/archive")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Archive roadmap", description = "Soft delete and archive a roadmap")
  public ApiResponse<String> archiveRoadmap(
      @Parameter(description = "Roadmap ID") @PathVariable UUID roadmapId) {
    log.info("DELETE /roadmaps/{}/archive – archiving roadmap", roadmapId);

    roadmapService.archiveRoadmap(roadmapId);

    return ApiResponse.<String>builder()
        .code(1000)
        .message("Roadmap archived successfully")
        .result("OK")
        .build();
  }

  @GetMapping("/{roadmapId}/completion-estimate")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Estimate completion days", description = "Get estimated days to complete roadmap")
  public ApiResponse<Integer> estimateCompletionDays(
      @Parameter(description = "Roadmap ID") @PathVariable UUID roadmapId) {
    log.info("GET /roadmaps/{}/completion-estimate – estimating days", roadmapId);

    Integer result = roadmapService.estimateCompletionDays(roadmapId);

    return ApiResponse.<Integer>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }

  @GetMapping("/{roadmapId}/progress")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get progress percentage", description = "Get overall progress percentage for roadmap")
  public ApiResponse<java.math.BigDecimal> calculateRoadmapProgress(
      @Parameter(description = "Roadmap ID") @PathVariable UUID roadmapId) {
    log.info("GET /roadmaps/{}/progress – calculating progress", roadmapId);

    java.math.BigDecimal result = roadmapService.calculateRoadmapProgress(roadmapId);

    return ApiResponse.<java.math.BigDecimal>builder()
        .code(1000)
        .message("Success")
        .result(result)
        .build();
  }
}
