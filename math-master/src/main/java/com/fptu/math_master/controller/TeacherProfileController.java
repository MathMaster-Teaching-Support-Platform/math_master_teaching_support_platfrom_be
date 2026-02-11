package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.ProfileReviewRequest;
import com.fptu.math_master.dto.request.TeacherProfileRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.TeacherProfileResponse;
import com.fptu.math_master.enums.ProfileStatus;
import com.fptu.math_master.service.TeacherProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/teacher-profiles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Teacher Profile", description = "APIs for managing teacher profile submissions")
@SecurityRequirement(name = "bearer-key")
public class TeacherProfileController {

  TeacherProfileService teacherProfileService;

  @Operation(summary = "Submit teacher profile", description = "Student submits profile to become a teacher")
  @PostMapping("/submit")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<TeacherProfileResponse> submitProfile(@Valid @RequestBody TeacherProfileRequest request) {
    UUID userId = getCurrentUserId();
    return ApiResponse.<TeacherProfileResponse>builder()
      .result(teacherProfileService.submitProfile(request, userId))
      .build();
  }

  @Operation(summary = "Update my profile", description = "Update pending or rejected teacher profile")
  @PutMapping("/my-profile")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<TeacherProfileResponse> updateMyProfile(@Valid @RequestBody TeacherProfileRequest request) {
    UUID userId = getCurrentUserId();
    return ApiResponse.<TeacherProfileResponse>builder()
      .result(teacherProfileService.updateProfile(request, userId))
      .build();
  }

  @Operation(summary = "Get my profile", description = "Get current user's teacher profile")
  @GetMapping("/my-profile")
  @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER')")
  public ApiResponse<TeacherProfileResponse> getMyProfile() {
    UUID userId = getCurrentUserId();
    return ApiResponse.<TeacherProfileResponse>builder()
      .result(teacherProfileService.getMyProfile(userId))
      .build();
  }

  @Operation(summary = "Delete my profile", description = "Delete pending or rejected profile")
  @DeleteMapping("/my-profile")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<Void> deleteMyProfile() {
    UUID userId = getCurrentUserId();
    teacherProfileService.deleteMyProfile(userId);
    return ApiResponse.<Void>builder()
      .message("Profile deleted successfully")
      .build();
  }

  @Operation(summary = "Get profile by ID", description = "Admin gets specific profile details")
  @GetMapping("/{profileId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<TeacherProfileResponse> getProfileById(@PathVariable UUID profileId) {
    return ApiResponse.<TeacherProfileResponse>builder()
      .result(teacherProfileService.getProfileById(profileId))
      .build();
  }

  @Operation(summary = "Get profiles by status", description = "Admin gets all profiles with specific status")
  @GetMapping("/status/{status}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Page<TeacherProfileResponse>> getProfilesByStatus(
    @PathVariable ProfileStatus status,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<TeacherProfileResponse>>builder()
      .result(teacherProfileService.getProfilesByStatus(status, pageable))
      .build();
  }

  @Operation(summary = "Review profile", description = "Admin approves or rejects teacher profile")
  @PostMapping("/{profileId}/review")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<TeacherProfileResponse> reviewProfile(
    @PathVariable UUID profileId,
    @Valid @RequestBody ProfileReviewRequest request) {
    UUID adminId = getCurrentUserId();
    return ApiResponse.<TeacherProfileResponse>builder()
      .result(teacherProfileService.reviewProfile(profileId, request, adminId))
      .build();
  }

  @Operation(summary = "Count pending profiles", description = "Get number of profiles waiting for review")
  @GetMapping("/pending/count")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Long> countPendingProfiles() {
    return ApiResponse.<Long>builder()
      .result(teacherProfileService.countPendingProfiles())
      .build();
  }

  private UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return UUID.fromString(authentication.getName());
  }
}
