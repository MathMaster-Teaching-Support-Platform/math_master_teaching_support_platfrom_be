package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.EnrollmentResponse;
import com.fptu.math_master.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Enrollment", description = "APIs for course enrollment management")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

  EnrollmentService enrollmentService;

  @Operation(summary = "Enroll in a course")
  @PostMapping("/courses/{courseId}/enroll")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<EnrollmentResponse> enroll(@PathVariable UUID courseId) {
    log.info("POST /courses/{}/enroll", courseId);
    return ApiResponse.<EnrollmentResponse>builder()
        .result(enrollmentService.enroll(courseId))
        .build();
  }

  @Operation(summary = "Get my enrollments")
  @GetMapping("/enrollments/my")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<List<EnrollmentResponse>> getMyEnrollments() {
    return ApiResponse.<List<EnrollmentResponse>>builder()
        .result(enrollmentService.getMyEnrollments())
        .build();
  }

  @Operation(summary = "Drop an enrollment")
  @DeleteMapping("/enrollments/{enrollmentId}")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<EnrollmentResponse> drop(@PathVariable UUID enrollmentId) {
    log.info("DELETE /enrollments/{}", enrollmentId);
    return ApiResponse.<EnrollmentResponse>builder()
        .result(enrollmentService.drop(enrollmentId))
        .build();
  }
}
