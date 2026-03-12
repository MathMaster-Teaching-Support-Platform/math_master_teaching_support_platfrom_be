package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.StudentWishRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.StudentWishResponse;
import com.fptu.math_master.service.StudentWishService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing student learning wishes and preferences
 *
 * <p>Endpoints:
 * - Create/update learning wishes
 * - Retrieve wishes
 * - Manage wish preferences
 */
@RestController
@RequestMapping("/student-wishes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Student Wishes", description = "Manage student learning goals and preferences")
@SecurityRequirement(name = "bearerAuth")
public class StudentWishController {

  StudentWishService studentWishService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(
      summary = "Create or update student wish",
      description = "Create a new student learning wish or update existing one for a subject")
  public ApiResponse<StudentWishResponse> upsertWish(
      @Valid @RequestBody StudentWishRequest request) {
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID studentId = UUID.fromString(userId);

    log.info("POST /student-wishes – studentId={}, subject={}", studentId, request.getSubject());

    StudentWishResponse result = studentWishService.upsertWish(studentId, request);

    return ApiResponse.<StudentWishResponse>builder()
        .code(201)
        .message("Student wish created/updated successfully")
        .result(result)
        .build();
  }

  @GetMapping("/active/{subject}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get active wish for subject",
      description = "Retrieve the active student wish for a specific subject")
  public ApiResponse<StudentWishResponse> getActiveWish(
      @Parameter(description = "Subject name", example = "Algebra") @PathVariable String subject) {
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID studentId = UUID.fromString(userId);

    log.info("GET /student-wishes/active/{} – studentId={}", subject, studentId);

    StudentWishResponse result = studentWishService.getActiveWish(studentId, subject);

    return ApiResponse.<StudentWishResponse>builder()
        .code(1000)
        .message("Active wish retrieved successfully")
        .result(result)
        .build();
  }

  @GetMapping("/active")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get all active wishes",
      description = "Retrieve all active learning wishes for the student")
  public ApiResponse<List<StudentWishResponse>> getActiveWishes() {
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID studentId = UUID.fromString(userId);

    log.info("GET /student-wishes/active – studentId={}", studentId);

    List<StudentWishResponse> result = studentWishService.getActiveWishes(studentId);

    return ApiResponse.<List<StudentWishResponse>>builder()
        .code(1000)
        .message("Active wishes retrieved successfully")
        .result(result)
        .build();
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(
      summary = "Get all wishes",
      description = "Retrieve all learning wishes (active and inactive)")
  public ApiResponse<List<StudentWishResponse>> getAllWishes() {
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID studentId = UUID.fromString(userId);

    log.info("GET /student-wishes – studentId={}", studentId);

    List<StudentWishResponse> result = studentWishService.getAllWishes(studentId);

    return ApiResponse.<List<StudentWishResponse>>builder()
        .code(1000)
        .message("Wishes retrieved successfully")
        .result(result)
        .build();
  }

  @PutMapping("/{wishId}/deactivate")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Deactivate wish", description = "Deactivate a student wish")
  public ApiResponse<Void> deactivateWish(
      @Parameter(description = "Wish ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID wishId) {
    log.info("PUT /student-wishes/{}/deactivate", wishId);

    studentWishService.deactivateWish(wishId);

    return ApiResponse.<Void>builder().code(1000).message("Wish deactivated successfully").build();
  }

  @DeleteMapping("/{wishId}")
  @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
  @Operation(summary = "Delete wish", description = "Delete (soft delete) a student wish")
  public ApiResponse<Void> deleteWish(
      @Parameter(description = "Wish ID", example = "550e8400-e29b-41d4-a716-446655440000")
          @PathVariable
          UUID wishId) {
    log.info("DELETE /student-wishes/{}", wishId);

    studentWishService.deleteWish(wishId);

    return ApiResponse.<Void>builder().code(1000).message("Wish deleted successfully").build();
  }
}
