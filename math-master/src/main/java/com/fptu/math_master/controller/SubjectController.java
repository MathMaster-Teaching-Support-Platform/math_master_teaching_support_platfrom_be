package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateSubjectRequest;
import com.fptu.math_master.dto.request.LinkGradeSubjectRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.SubjectResponse;
import com.fptu.math_master.service.SubjectService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Subjects", description = "Math subject (môn học) management by school grade")
public class SubjectController {

  SubjectService subjectService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Create a new subject",
      description =
          "Admin creates a math subject such as 'Đại Số', 'Hình Học', 'Giải Tích', 'Tổ Hợp - Xác Suất'.")
  public ApiResponse<SubjectResponse> createSubject(
      @Valid @RequestBody CreateSubjectRequest request) {
    log.info(
      "REST request to create subject: name={}, schoolGradeId={}",
      request.getName(),
      request.getSchoolGradeId());
    return ApiResponse.<SubjectResponse>builder()
        .message("Subject created successfully.")
        .result(subjectService.createSubject(request))
        .build();
  }

  @GetMapping
  @Operation(summary = "List all active subjects")
  public ApiResponse<List<SubjectResponse>> getAllSubjects() {
    return ApiResponse.<List<SubjectResponse>>builder()
        .result(subjectService.getAllSubjects())
        .build();
  }

  @GetMapping("/{subjectId}")
  @Operation(summary = "Get subject by ID")
  public ApiResponse<SubjectResponse> getSubjectById(@PathVariable UUID subjectId) {
    return ApiResponse.<SubjectResponse>builder()
        .result(subjectService.getSubjectById(subjectId))
        .build();
  }

  @GetMapping("/grade/{gradeLevel}")
  @Operation(
      summary = "List subjects for a grade level",
      description =
          "Returns all active subjects linked to the specified school grade (lớp), e.g. 12.")
  public ApiResponse<List<SubjectResponse>> getSubjectsByGrade(@PathVariable Integer gradeLevel) {
    log.info("REST request to get subjects for grade {}", gradeLevel);
    return ApiResponse.<List<SubjectResponse>>builder()
        .result(subjectService.getSubjectsByGrade(gradeLevel))
        .build();
  }

  @PostMapping("/{subjectId}/grades")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Link subject to a grade level",
      description = "Assigns the subject to a school grade level, e.g. grade=12.")
  public ApiResponse<SubjectResponse> linkToGrade(
      @PathVariable UUID subjectId, @Valid @RequestBody LinkGradeSubjectRequest request) {
    log.info("REST request to link subject {} to grade {}", subjectId, request.getGradeLevel());
    return ApiResponse.<SubjectResponse>builder()
        .message("Subject linked to grade " + request.getGradeLevel() + " successfully.")
        .result(subjectService.linkToGrade(subjectId, request))
        .build();
  }

  @DeleteMapping("/{subjectId}/grades/{gradeLevel}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Unlink subject from a grade level")
  public ApiResponse<Void> unlinkFromGrade(
      @PathVariable UUID subjectId, @PathVariable Integer gradeLevel) {
    log.info("REST request to unlink subject {} from grade {}", subjectId, gradeLevel);
    subjectService.unlinkFromGrade(subjectId, gradeLevel);
    return ApiResponse.<Void>builder()
        .message("Subject unlinked from grade " + gradeLevel + " successfully.")
        .build();
  }

  @DeleteMapping("/{subjectId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Deactivate subject (soft delete)")
  public ApiResponse<Void> deactivateSubject(@PathVariable UUID subjectId) {
    log.info("REST request to deactivate subject: {}", subjectId);
    subjectService.deactivateSubject(subjectId);
    return ApiResponse.<Void>builder().message("Subject deactivated successfully.").build();
  }
}
