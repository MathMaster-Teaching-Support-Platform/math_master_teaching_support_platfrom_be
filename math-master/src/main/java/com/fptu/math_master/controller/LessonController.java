package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateLessonRequest;
import com.fptu.math_master.dto.request.ReorderLessonsRequest;
import com.fptu.math_master.dto.request.UpdateLessonRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.service.LessonService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/lessons", "/api/lessons"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Lessons", description = "CRUD for lessons within a chapter")
@SecurityRequirement(name = "bearerAuth")
public class LessonController {

  LessonService lessonService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Create lesson")
  public ApiResponse<LessonResponse> createLesson(@Valid @RequestBody CreateLessonRequest request) {
    log.info("POST /lessons - chapterId={}", request.getChapterId());
    return ApiResponse.<LessonResponse>builder()
        .message("Lesson created successfully")
        .result(lessonService.createLesson(request))
        .build();
  }

  @GetMapping("/{lessonId}")
  @Operation(summary = "Get lesson by id")
  public ApiResponse<LessonResponse> getLessonById(@PathVariable UUID lessonId) {
    return ApiResponse.<LessonResponse>builder().result(lessonService.getLessonById(lessonId)).build();
  }

  @GetMapping("/chapter/{chapterId}")
  @Operation(summary = "List lessons by chapter")
  public ApiResponse<List<LessonResponse>> getLessonsByChapter(
      @PathVariable UUID chapterId,
      @RequestParam(value = "name", required = false) String name) {
    return ApiResponse.<List<LessonResponse>>builder()
        .result(lessonService.searchLessonsByChapterId(chapterId, name))
        .build();
  }

  @PutMapping("/{lessonId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Update lesson")
  public ApiResponse<LessonResponse> updateLesson(
      @PathVariable UUID lessonId, @Valid @RequestBody UpdateLessonRequest request) {
    log.info("PUT /lessons/{}", lessonId);
    return ApiResponse.<LessonResponse>builder()
        .message("Lesson updated successfully")
        .result(lessonService.updateLesson(lessonId, request))
        .build();
  }

  @DeleteMapping("/{lessonId}")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "Delete lesson (soft delete)")
  public ApiResponse<Void> deleteLesson(@PathVariable UUID lessonId) {
    log.info("DELETE /lessons/{}", lessonId);
    lessonService.deleteLesson(lessonId);
    return ApiResponse.<Void>builder().message("Lesson deleted successfully").build();
  }

  @PutMapping("/chapter/{chapterId}/reorder")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Reorder lessons in a chapter",
      description = "Bulk-update the orderIndex of lessons within a chapter.")
  public ApiResponse<List<LessonResponse>> reorderLessons(
      @PathVariable UUID chapterId, @Valid @RequestBody ReorderLessonsRequest request) {
    log.info("PUT /lessons/chapter/{}/reorder", chapterId);
    return ApiResponse.<List<LessonResponse>>builder()
        .message("Lessons reordered successfully")
        .result(lessonService.reorderLessons(chapterId, request))
        .build();
  }

  @PatchMapping("/{lessonId}/restore")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "Restore a deleted lesson",
      description = "Sets deletedAt=null, making the lesson visible again.")
  public ApiResponse<LessonResponse> restoreLesson(@PathVariable UUID lessonId) {
    log.info("PATCH /lessons/{}/restore", lessonId);
    return ApiResponse.<LessonResponse>builder()
        .message("Lesson restored successfully")
        .result(lessonService.restoreLesson(lessonId))
        .build();
  }

  @GetMapping("/chapter/{chapterId}/all")
  @Operation(
      summary = "List lessons by chapter including deleted",
      description = "Returns all lessons (active and soft-deleted). deleted=true means it was removed. For admin management.")
  public ApiResponse<List<LessonResponse>> getLessonsIncludingDeleted(
      @PathVariable UUID chapterId) {
    log.info("GET /lessons/chapter/{}/all", chapterId);
    return ApiResponse.<List<LessonResponse>>builder()
        .result(lessonService.getLessonsIncludingDeleted(chapterId))
        .build();
  }
}