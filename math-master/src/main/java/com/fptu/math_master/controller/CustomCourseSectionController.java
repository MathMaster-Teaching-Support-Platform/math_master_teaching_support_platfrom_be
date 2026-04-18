package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateCustomCourseSectionRequest;
import com.fptu.math_master.dto.request.UpdateCustomCourseSectionRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CustomCourseSectionResponse;
import com.fptu.math_master.service.CustomCourseSectionService;
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
@RequestMapping("/courses/{courseId}/sections")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Custom Course Section", description = "CRUD for sections in CUSTOM-provider courses")
public class CustomCourseSectionController {

  CustomCourseSectionService sectionService;

  @Operation(summary = "Create a new section in a CUSTOM course")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CustomCourseSectionResponse> createSection(
      @PathVariable UUID courseId,
      @Valid @RequestBody CreateCustomCourseSectionRequest request) {
    log.info("POST /courses/{}/sections – title={}", courseId, request.getTitle());
    return ApiResponse.<CustomCourseSectionResponse>builder()
        .result(sectionService.createSection(courseId, request))
        .build();
  }

  @Operation(summary = "List all sections in a course (ordered by orderIndex)")
  @GetMapping
  public ApiResponse<List<CustomCourseSectionResponse>> listSections(
      @PathVariable UUID courseId) {
    return ApiResponse.<List<CustomCourseSectionResponse>>builder()
        .result(sectionService.listSections(courseId))
        .build();
  }

  @Operation(summary = "Update a section in a CUSTOM course")
  @PutMapping("/{sectionId}")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CustomCourseSectionResponse> updateSection(
      @PathVariable UUID courseId,
      @PathVariable UUID sectionId,
      @Valid @RequestBody UpdateCustomCourseSectionRequest request) {
    log.info("PUT /courses/{}/sections/{}", courseId, sectionId);
    return ApiResponse.<CustomCourseSectionResponse>builder()
        .result(sectionService.updateSection(courseId, sectionId, request))
        .build();
  }

  @Operation(summary = "Delete a section (soft delete; blocked if it has active lessons)")
  @DeleteMapping("/{sectionId}")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<Void> deleteSection(
      @PathVariable UUID courseId, @PathVariable UUID sectionId) {
    log.info("DELETE /courses/{}/sections/{}", courseId, sectionId);
    sectionService.deleteSection(courseId, sectionId);
    return ApiResponse.<Void>builder().message("Section deleted successfully").build();
  }
}
