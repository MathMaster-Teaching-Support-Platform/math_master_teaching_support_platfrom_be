package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.SchoolRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.SchoolResponse;
import com.fptu.math_master.service.SchoolService;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "School", description = "APIs for managing schools")
@SecurityRequirement(name = "bearer-key")
public class SchoolController {

  SchoolService schoolService;

  @Operation(summary = "Create school", description = "Admin creates a new school")
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<SchoolResponse> createSchool(@Valid @RequestBody SchoolRequest request) {
    return ApiResponse.<SchoolResponse>builder()
      .result(schoolService.createSchool(request))
      .build();
  }

  @Operation(summary = "Update school", description = "Admin updates school information")
  @PutMapping("/{schoolId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<SchoolResponse> updateSchool(
    @PathVariable UUID schoolId,
    @Valid @RequestBody SchoolRequest request) {
    return ApiResponse.<SchoolResponse>builder()
      .result(schoolService.updateSchool(schoolId, request))
      .build();
  }

  @Operation(summary = "Get school by ID", description = "Get school details by ID")
  @GetMapping("/{schoolId}")
  public ApiResponse<SchoolResponse> getSchoolById(@PathVariable UUID schoolId) {
    return ApiResponse.<SchoolResponse>builder()
      .result(schoolService.getSchoolById(schoolId))
      .build();
  }

  @Operation(summary = "Get all schools (paginated)", description = "Get all schools with pagination")
  @GetMapping
  public ApiResponse<Page<SchoolResponse>> getAllSchools(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<SchoolResponse>>builder()
      .result(schoolService.getAllSchools(pageable))
      .build();
  }

  @Operation(summary = "Get all schools (no pagination)", description = "Get all schools without pagination")
  @GetMapping("/all")
  public ApiResponse<List<SchoolResponse>> getAllSchoolsNoPagination() {
    return ApiResponse.<List<SchoolResponse>>builder()
      .result(schoolService.getAllSchools())
      .build();
  }

  @Operation(summary = "Search schools", description = "Search schools by name")
  @GetMapping("/search")
  public ApiResponse<List<SchoolResponse>> searchSchools(@RequestParam String name) {
    return ApiResponse.<List<SchoolResponse>>builder()
      .result(schoolService.searchSchoolsByName(name))
      .build();
  }

  @Operation(summary = "Delete school", description = "Admin deletes a school")
  @DeleteMapping("/{schoolId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> deleteSchool(@PathVariable UUID schoolId) {
    schoolService.deleteSchool(schoolId);
    return ApiResponse.<Void>builder()
      .message("School deleted successfully")
      .build();
  }
}
