package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateSchoolGradeRequest;
import com.fptu.math_master.dto.request.UpdateSchoolGradeRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.SchoolGradeResponse;
import com.fptu.math_master.service.SchoolGradeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/school-grades")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "School Grades", description = "CRUD for school grades (lop 1..12)")
@SecurityRequirement(name = "bearerAuth")
public class SchoolGradeController {

  SchoolGradeService schoolGradeService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Create school grade")
  public ApiResponse<SchoolGradeResponse> create(
      @Valid @RequestBody CreateSchoolGradeRequest request) {
    return ApiResponse.<SchoolGradeResponse>builder()
        .message("School grade created successfully")
        .result(schoolGradeService.create(request))
        .build();
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update school grade")
  public ApiResponse<SchoolGradeResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateSchoolGradeRequest request) {
    return ApiResponse.<SchoolGradeResponse>builder()
        .message("School grade updated successfully")
        .result(schoolGradeService.update(id, request))
        .build();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get school grade by id")
  public ApiResponse<SchoolGradeResponse> getById(@PathVariable UUID id) {
    return ApiResponse.<SchoolGradeResponse>builder()
        .result(schoolGradeService.getById(id))
        .build();
  }

  @GetMapping
  @Operation(summary = "List school grades")
  public ApiResponse<List<SchoolGradeResponse>> getAll(
      @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
    return ApiResponse.<List<SchoolGradeResponse>>builder()
        .result(schoolGradeService.getAll(activeOnly))
        .build();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Deactivate school grade")
  public ApiResponse<Void> deactivate(@PathVariable UUID id) {
    schoolGradeService.deactivate(id);
    return ApiResponse.<Void>builder().message("School grade deactivated successfully").build();
  }
}
