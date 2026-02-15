package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.AssessmentRequest;
import com.fptu.math_master.dto.request.PointsOverrideRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.AssessmentResponse;
import com.fptu.math_master.dto.response.AssessmentSummary;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.service.AssessmentService;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assessments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class AssessmentController {

  AssessmentService assessmentService;

  @PostMapping
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create a new assessment",
      description =
          "Teacher creates a new assessment (quiz/test/exam/homework). "
              + "Title is required (max 255 chars), description is optional. "
              + "Can set type, link to lesson, time limit, passing score, schedule, and options. "
              + "Status is DRAFT by default. Redirects to Assessment Builder after creation.")
  public ApiResponse<AssessmentResponse> createAssessment(
      @Valid @RequestBody AssessmentRequest request) {
    log.info("REST request to create assessment: {}", request.getTitle());
    return ApiResponse.<AssessmentResponse>builder()
        .message("Assessment created successfully. You can now add questions to your assessment.")
        .result(assessmentService.createAssessment(request))
        .build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Update assessment",
      description =
          "Update assessment details. Only DRAFT assessments can be edited. "
              + "Published assessments cannot be modified unless unpublished first.")
  public ApiResponse<AssessmentResponse> updateAssessment(
      @PathVariable UUID id, @Valid @RequestBody AssessmentRequest request) {
    log.info("REST request to update assessment: {}", id);
    return ApiResponse.<AssessmentResponse>builder()
        .message("Assessment updated successfully")
        .result(assessmentService.updateAssessment(id, request))
        .build();
  }

  @PatchMapping("/{assessmentId}/points-override")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Set points override for a question",
      description =
          "Teacher can override default points for a question in this specific assessment. "
              + "Default is null (uses question.points). Teacher can edit inline. "
              + "System recalculates total points automatically. "
              + "Example: Question default = 2.0, override in this assessment = 5.0 → student gets 5 points if correct.")
  public ApiResponse<AssessmentResponse> setPointsOverride(
      @PathVariable UUID assessmentId, @Valid @RequestBody PointsOverrideRequest request) {
    log.info(
        "REST request to set points override for assessment: {}, question: {}",
        assessmentId,
        request.getQuestionId());
    return ApiResponse.<AssessmentResponse>builder()
        .message("Points override updated successfully. Total points recalculated.")
        .result(assessmentService.setPointsOverride(assessmentId, request))
        .build();
  }

  @GetMapping("/{id}/preview")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Preview assessment as student",
      description =
          "Teacher can preview how the assessment will look to students. "
              + "Shows exact student UI, can test-take (doesn't save submission), displays timer if set. "
              + "Open in new tab/modal for best experience.")
  public ApiResponse<AssessmentResponse> getAssessmentPreview(@PathVariable UUID id) {
    log.info("REST request to preview assessment: {}", id);
    return ApiResponse.<AssessmentResponse>builder()
        .result(assessmentService.getAssessmentPreview(id))
        .build();
  }

  @GetMapping("/{id}/publish-summary")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get publish summary for validation",
      description =
          "Get assessment summary before publishing with validation checks: "
              + "- At least 1 question required "
              + "- All questions must be valid "
              + "- Total points > 0 "
              + "- Schedule valid (if set) "
              + "Shows: total questions, total points, time limit, schedule. "
              + "Warning: Assessment cannot be edited after publishing.")
  public ApiResponse<AssessmentSummary> getPublishSummary(@PathVariable UUID id) {
    log.info("REST request to get publish summary for assessment: {}", id);
    return ApiResponse.<AssessmentSummary>builder()
        .result(assessmentService.getPublishSummary(id))
        .build();
  }

  @PostMapping("/{id}/publish")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Publish assessment",
      description =
          "Publish assessment to make it available to students. "
              + "Validates: at least 1 question, all questions valid, total points > 0, valid schedule. "
              + "After publishing: Status = PUBLISHED, notifications sent to students (if assigned), "
              + "assessment cannot be edited (only unpublished if no submissions).")
  public ApiResponse<AssessmentResponse> publishAssessment(@PathVariable UUID id) {
    log.info("REST request to publish assessment: {}", id);
    return ApiResponse.<AssessmentResponse>builder()
        .message("Assessment published successfully! Students can now take this assessment.")
        .result(assessmentService.publishAssessment(id))
        .build();
  }

  @PostMapping("/{id}/unpublish")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Unpublish assessment",
      description =
          "Unpublish assessment to edit it. "
              + "Can only unpublish if no submissions exist. "
              + "If submissions exist → BLOCKED with error message. "
              + "After unpublishing: Status = DRAFT, students cannot access.")
  public ApiResponse<AssessmentResponse> unpublishAssessment(@PathVariable UUID id) {
    log.info("REST request to unpublish assessment: {}", id);
    return ApiResponse.<AssessmentResponse>builder()
        .message("Assessment unpublished successfully. You can now edit it.")
        .result(assessmentService.unpublishAssessment(id))
        .build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Delete assessment",
      description =
          "Delete assessment. Can only delete if Status = DRAFT and no submissions exist. "
              + "If has submissions → BLOCKED. Soft delete (sets deleted_at). "
              + "Questions are NOT deleted, only the assessment.")
  public ApiResponse<Void> deleteAssessment(@PathVariable UUID id) {
    log.info("REST request to delete assessment: {}", id);
    assessmentService.deleteAssessment(id);
    return ApiResponse.<Void>builder()
        .message("Assessment deleted successfully. Questions have been preserved.")
        .build();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'STUDENT')")
  @Operation(
      summary = "Get assessment by ID",
      description =
          "Get assessment details. Teachers/admins can view all, students can view published assessments.")
  public ApiResponse<AssessmentResponse> getAssessmentById(@PathVariable UUID id) {
    log.info("REST request to get assessment: {}", id);
    return ApiResponse.<AssessmentResponse>builder()
        .result(assessmentService.getAssessmentById(id))
        .build();
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "List my assessments",
      description =
          "Get all assessments created by current teacher. "
              + "Supports filtering by status (DRAFT/PUBLISHED/CLOSED) and lessonId. "
              + "Pagination supported (default 20 items/page).")
  public ApiResponse<Page<AssessmentResponse>> getMyAssessments(
      @RequestParam(required = false) AssessmentStatus status,
      @RequestParam(required = false) UUID lessonId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESC") String sortDirection) {

    log.info("REST request to get my assessments - status: {}, lessonId: {}", status, lessonId);

    Sort sort =
        sortDirection.equalsIgnoreCase("ASC")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();

    Pageable pageable = PageRequest.of(page, size, sort);

    return ApiResponse.<Page<AssessmentResponse>>builder()
        .result(assessmentService.getMyAssessments(status, lessonId, pageable))
        .build();
  }

  @GetMapping("/{id}/can-edit")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Check if user can edit assessment",
      description =
          "Verify if current user can edit the assessment (owner/admin and status = DRAFT).")
  public ApiResponse<Boolean> canEditAssessment(@PathVariable UUID id) {
    log.info("REST request to check edit permission for assessment: {}", id);
    return ApiResponse.<Boolean>builder().result(assessmentService.canEditAssessment(id)).build();
  }

  @GetMapping("/{id}/can-delete")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Check if user can delete assessment",
      description =
          "Verify if current user can delete the assessment (owner/admin, DRAFT, no submissions).")
  public ApiResponse<Boolean> canDeleteAssessment(@PathVariable UUID id) {
    log.info("REST request to check delete permission for assessment: {}", id);
    return ApiResponse.<Boolean>builder().result(assessmentService.canDeleteAssessment(id)).build();
  }

  @GetMapping("/{id}/can-publish")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  @Operation(
      summary = "Check if user can publish assessment",
      description =
          "Verify if current user can publish the assessment (owner/admin, DRAFT, has questions).")
  public ApiResponse<Boolean> canPublishAssessment(@PathVariable UUID id) {
    log.info("REST request to check publish permission for assessment: {}", id);
    return ApiResponse.<Boolean>builder()
        .result(assessmentService.canPublishAssessment(id))
        .build();
  }
}
