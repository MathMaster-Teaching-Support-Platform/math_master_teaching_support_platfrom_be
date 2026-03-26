package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateLessonPlanRequest;
import com.fptu.math_master.dto.request.UpdateLessonPlanRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonPlanResponse;
import com.fptu.math_master.service.LessonPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lesson-plans")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Lesson Plan", description = "APIs for managing teacher lesson plans (Giáo án)")
@SecurityRequirement(name = "bearerAuth")
public class LessonPlanController {

  LessonPlanService lessonPlanService;

  @Operation(summary = "Create lesson plan")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<LessonPlanResponse> createLessonPlan(
      @Valid @RequestBody CreateLessonPlanRequest request) {
    log.info("POST /lesson-plans – lessonId={}", request.getLessonId());
    return ApiResponse.<LessonPlanResponse>builder()
        .result(lessonPlanService.createLessonPlan(request))
        .build();
  }

  @Operation(summary = "Update lesson plan")
  @PutMapping("/{lessonPlanId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<LessonPlanResponse> updateLessonPlan(
      @PathVariable UUID lessonPlanId,
      @Valid @RequestBody UpdateLessonPlanRequest request) {
    log.info("PUT /lesson-plans/{}", lessonPlanId);
    return ApiResponse.<LessonPlanResponse>builder()
        .result(lessonPlanService.updateLessonPlan(lessonPlanId, request))
        .build();
  }

  @Operation(summary = "Get lesson plan by ID")
  @GetMapping("/{lessonPlanId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<LessonPlanResponse> getLessonPlanById(@PathVariable UUID lessonPlanId) {
    return ApiResponse.<LessonPlanResponse>builder()
        .result(lessonPlanService.getLessonPlanById(lessonPlanId))
        .build();
  }

  @Operation(summary = "Get all my lesson plans")
  @GetMapping("/my")
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<List<LessonPlanResponse>> getMyLessonPlans() {
    return ApiResponse.<List<LessonPlanResponse>>builder()
        .result(lessonPlanService.getMyLessonPlans())
        .build();
  }

  @Operation(summary = "Get my lesson plan for a specific lesson")
  @GetMapping("/my/lesson/{lessonId}")
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<LessonPlanResponse> getMyLessonPlanByLesson(@PathVariable UUID lessonId) {
    return ApiResponse.<LessonPlanResponse>builder()
        .result(lessonPlanService.getMyLessonPlanByLesson(lessonId))
        .build();
  }

  @Operation(summary = "Get all lesson plans for a lesson")
  @GetMapping("/lesson/{lessonId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<List<LessonPlanResponse>> getLessonPlansByLesson(
      @PathVariable UUID lessonId) {
    return ApiResponse.<List<LessonPlanResponse>>builder()
        .result(lessonPlanService.getLessonPlansByLesson(lessonId))
        .build();
  }

  @Operation(summary = "Delete lesson plan")
  @DeleteMapping("/{lessonPlanId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Void> deleteLessonPlan(@PathVariable UUID lessonPlanId) {
    log.info("DELETE /lesson-plans/{}", lessonPlanId);
    lessonPlanService.deleteLessonPlan(lessonPlanId);
    return ApiResponse.<Void>builder().message("Lesson plan deleted successfully").build();
  }
}
