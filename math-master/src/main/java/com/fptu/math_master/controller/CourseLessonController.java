package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateCourseLessonRequest;
import com.fptu.math_master.dto.request.UpdateCourseLessonRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.service.CourseLessonService;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/courses/{courseId}/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Course Lesson", description = "APIs for managing lessons within a course")
@SecurityRequirement(name = "bearerAuth")
public class CourseLessonController {

  CourseLessonService courseLessonService;

  @Operation(summary = "Add a lesson to a course (with optional video upload)")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<CourseLessonResponse> addLesson(
      @PathVariable UUID courseId,
      @RequestPart("request") CreateCourseLessonRequest request,
      @RequestPart(value = "video", required = false) MultipartFile videoFile) {
    log.info("POST /courses/{}/lessons", courseId);
    return ApiResponse.<CourseLessonResponse>builder()
        .result(courseLessonService.addLesson(courseId, request, videoFile))
        .build();
  }

  @Operation(summary = "Get all lessons in a course")
  @GetMapping
  public ApiResponse<List<CourseLessonResponse>> getLessons(@PathVariable UUID courseId) {
    return ApiResponse.<List<CourseLessonResponse>>builder()
        .result(courseLessonService.getLessons(courseId))
        .build();
  }

  @Operation(summary = "Update a lesson in a course")
  @PutMapping(value = "/{courseLessonId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<CourseLessonResponse> updateLesson(
      @PathVariable UUID courseId,
      @PathVariable UUID courseLessonId,
      @RequestPart("request") UpdateCourseLessonRequest request,
      @RequestPart(value = "video", required = false) MultipartFile videoFile) {
    log.info("PUT /courses/{}/lessons/{}", courseId, courseLessonId);
    return ApiResponse.<CourseLessonResponse>builder()
        .result(courseLessonService.updateLesson(courseId, courseLessonId, request, videoFile))
        .build();
  }

  @Operation(summary = "Delete a lesson from a course")
  @DeleteMapping("/{courseLessonId}")
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<Void> deleteLesson(
      @PathVariable UUID courseId, @PathVariable UUID courseLessonId) {
    log.info("DELETE /courses/{}/lessons/{}", courseId, courseLessonId);
    courseLessonService.deleteLesson(courseId, courseLessonId);
    return ApiResponse.<Void>builder().message("Lesson deleted successfully").build();
  }
}
