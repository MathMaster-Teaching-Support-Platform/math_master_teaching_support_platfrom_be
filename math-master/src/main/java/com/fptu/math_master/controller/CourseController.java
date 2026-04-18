package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.AddAssessmentToCourseRequest;
import com.fptu.math_master.dto.request.CreateCourseRequest;
import com.fptu.math_master.dto.request.PublishCourseRequest;
import com.fptu.math_master.dto.request.UpdateCourseAssessmentRequest;
import com.fptu.math_master.dto.request.UpdateCourseRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.AvailableCourseAssessmentResponse;
import com.fptu.math_master.dto.response.CourseAssessmentResponse;
import com.fptu.math_master.dto.response.CourseResponse;
import com.fptu.math_master.dto.response.StudentInCourseResponse;
import com.fptu.math_master.service.CourseService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Course", description = "APIs for course management")
public class CourseController {

  CourseService courseService;

  @Operation(summary = "Create a new course")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
    log.info("POST /courses – title={}", request.getTitle());
    return ApiResponse.<CourseResponse>builder().result(courseService.createCourse(request, null)).build();
  }

  @Operation(summary = "Create a new course with optional thumbnail upload")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CourseResponse> createCourseMultipart(
      @Valid @RequestPart("request") CreateCourseRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile) {
    log.info("POST /courses (multipart) – title={}", request.getTitle());
    return ApiResponse.<CourseResponse>builder()
        .result(courseService.createCourse(request, thumbnailFile))
        .build();
  }

  @Operation(summary = "Get my courses (teacher)")
  @GetMapping("/my")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<List<CourseResponse>> getMyCourses() {
    return ApiResponse.<List<CourseResponse>>builder()
        .result(courseService.getMyCourses())
        .build();
  }

  @Operation(summary = "Get course by ID")
  @GetMapping("/{courseId}")
  public ApiResponse<CourseResponse> getCourseById(@PathVariable UUID courseId) {
    return ApiResponse.<CourseResponse>builder()
        .result(courseService.getCourseById(courseId))
        .build();
  }

  @Operation(summary = "Update course")
  @PutMapping(value = "/{courseId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CourseResponse> updateCourse(
      @PathVariable UUID courseId, @Valid @RequestBody UpdateCourseRequest request) {
    log.info("PUT /courses/{}", courseId);
    return ApiResponse.<CourseResponse>builder()
        .result(courseService.updateCourse(courseId, request, null))
        .build();
  }

  @Operation(summary = "Update course with optional thumbnail upload")
  @PutMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CourseResponse> updateCourseMultipart(
      @PathVariable UUID courseId,
      @Valid @RequestPart("request") UpdateCourseRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnailFile) {
    log.info("PUT /courses/{} (multipart)", courseId);
    return ApiResponse.<CourseResponse>builder()
        .result(courseService.updateCourse(courseId, request, thumbnailFile))
        .build();
  }

  @Operation(summary = "Delete course (soft delete)")
  @DeleteMapping("/{courseId}")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<Void> deleteCourse(@PathVariable UUID courseId) {
    log.info("DELETE /courses/{}", courseId);
    courseService.deleteCourse(courseId);
    return ApiResponse.<Void>builder().message("Course deleted successfully").build();
  }

  @Operation(summary = "Publish or unpublish course")
  @PatchMapping("/{courseId}/publish")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CourseResponse> publishCourse(
      @PathVariable UUID courseId, @Valid @RequestBody PublishCourseRequest request) {
    log.info("PATCH /courses/{}/publish – published={}", courseId, request.getPublished());
    return ApiResponse.<CourseResponse>builder()
        .result(courseService.publishCourse(courseId, request.getPublished()))
        .build();
  }

  @Operation(summary = "Get students enrolled in a course")
  @GetMapping("/{courseId}/students")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<Page<StudentInCourseResponse>> getStudentsInCourse(
      @PathVariable UUID courseId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var pageable = PageRequest.of(page, size, Sort.by("enrolledAt").descending());
    return ApiResponse.<Page<StudentInCourseResponse>>builder()
        .result(courseService.getStudentsInCourse(courseId, pageable))
        .build();
  }

  @Operation(summary = "Browse published courses (public)")
  @GetMapping
  public ApiResponse<Page<CourseResponse>> getPublicCourses(
      @RequestParam(required = false) UUID schoolGradeId,
      @RequestParam(required = false) UUID subjectId,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return ApiResponse.<Page<CourseResponse>>builder()
        .result(courseService.getPublicCourses(schoolGradeId, subjectId, keyword, pageable))
        .build();
  }

  @Operation(summary = "Admin: search all courses (including unpublished)")
  @GetMapping("/admin/search")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<Page<CourseResponse>> adminSearchCourses(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    log.info("GET /courses/admin/search – keyword={}", keyword);
    var pageable = PageRequest.of(page, size);
    return ApiResponse.<Page<CourseResponse>>builder()
        .result(courseService.searchCoursesForAdmin(keyword, pageable))
        .build();
  }

  // ─── Course Assessment Management ─────────────────────────────────────────

  @Operation(summary = "Add assessment to course")
  @PostMapping("/{courseId}/assessments")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CourseAssessmentResponse> addAssessmentToCourse(
      @PathVariable UUID courseId, @Valid @RequestBody AddAssessmentToCourseRequest request) {
    log.info("POST /courses/{}/assessments – assessmentId={}", courseId, request.getAssessmentId());
    return ApiResponse.<CourseAssessmentResponse>builder()
        .message("Assessment added to course successfully")
        .result(courseService.addAssessmentToCourse(courseId, request))
        .build();
  }

  @Operation(summary = "Get all assessments in a course with optional filtering")
  @GetMapping("/{courseId}/assessments")
  public ApiResponse<List<CourseAssessmentResponse>> getCourseAssessments(
      @PathVariable UUID courseId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) Boolean isRequired) {
    return ApiResponse.<List<CourseAssessmentResponse>>builder()
        .result(courseService.getCourseAssessments(courseId, status, type, isRequired))
        .build();
  }

  @Operation(summary = "Get available assessments matched by course lessons")
  @GetMapping("/{courseId}/assessments/available")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<List<AvailableCourseAssessmentResponse>> getAvailableAssessmentsForCourse(
    @PathVariable UUID courseId,
    @RequestParam(defaultValue = "false") boolean includeOutOfCourseLessons) {
    return ApiResponse.<List<AvailableCourseAssessmentResponse>>builder()
      .result(courseService.getAvailableAssessmentsForCourse(courseId, includeOutOfCourseLessons))
        .build();
  }

  @Operation(summary = "Update course assessment settings")
  @PatchMapping("/{courseId}/assessments/{assessmentId}")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<CourseAssessmentResponse> updateCourseAssessment(
      @PathVariable UUID courseId,
      @PathVariable UUID assessmentId,
      @Valid @RequestBody UpdateCourseAssessmentRequest request) {
    log.info("PATCH /courses/{}/assessments/{}", courseId, assessmentId);
    return ApiResponse.<CourseAssessmentResponse>builder()
        .message("Course assessment updated successfully")
        .result(courseService.updateCourseAssessment(courseId, assessmentId, request))
        .build();
  }

  @Operation(summary = "Remove assessment from course")
  @DeleteMapping("/{courseId}/assessments/{assessmentId}")
  @PreAuthorize("hasRole('TEACHER')")
  @SecurityRequirement(name = "bearerAuth")
  public ApiResponse<Void> removeAssessmentFromCourse(
      @PathVariable UUID courseId, @PathVariable UUID assessmentId) {
    log.info("DELETE /courses/{}/assessments/{}", courseId, assessmentId);
    courseService.removeAssessmentFromCourse(courseId, assessmentId);
    return ApiResponse.<Void>builder()
        .message("Assessment removed from course successfully")
        .build();
  }
}
