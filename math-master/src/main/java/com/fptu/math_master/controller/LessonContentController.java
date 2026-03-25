package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateLessonRequest;
import com.fptu.math_master.dto.request.GenerateLessonContentRequest;
import com.fptu.math_master.dto.request.UpdateLessonRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.ChapterResponse;
import com.fptu.math_master.dto.response.GenerateLessonContentResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.service.LessonContentService;
import com.fptu.math_master.service.LessonService;
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
@RequestMapping("/lessons")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Lessons & Chapters", description = "AI-powered Math lesson curriculum management")
@SecurityRequirement(name = "bearerAuth")
public class LessonContentController {

  LessonContentService lessonContentService;
  LessonService lessonService;

  // -----------------------------------------------------------------------
  // AI Generation
  // -----------------------------------------------------------------------

  @PostMapping("/generate")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Generate full lesson curriculum with Gemini AI",
      description =
          "Calls Gemini API to auto-generate lessons and chapters for a given Math subject "
              + "and grade level, then persists them to the database. "
              + "Idempotent by default: skips lessons that already exist.")
  public ApiResponse<GenerateLessonContentResponse> generateContent(
      @Valid @RequestBody GenerateLessonContentRequest request) {
    log.info(
        "POST /lessons/generate – gradeLevel={}, subject={}",
        request.getGradeLevel(),
        request.getSubject());
    GenerateLessonContentResponse result = lessonContentService.generateAndSaveContent(request);
    return ApiResponse.<GenerateLessonContentResponse>builder()
        .result(result)
        .message("Lesson content generated and saved successfully")
        .build();
  }

  // -----------------------------------------------------------------------
  // Read
  // -----------------------------------------------------------------------

  @GetMapping
  @Operation(
      summary = "List lessons by grade level and subject",
      description = "Returns all non-deleted lessons for the given gradeLevel and subject.")
  public ApiResponse<List<LessonResponse>> getLessons(
      @Parameter(description = "e.g. Lớp 10") @RequestParam String gradeLevel,
      @Parameter(description = "e.g. Đại số") @RequestParam String subject) {
    log.info("GET /lessons?gradeLevel={}&subject={}", gradeLevel, subject);
    return ApiResponse.<List<LessonResponse>>builder()
        .result(lessonContentService.getLessonsByGradeAndSubject(gradeLevel, subject))
        .build();
  }

  @GetMapping("/{lessonId}")
  @Operation(summary = "Get a single lesson with its chapters")
  public ApiResponse<LessonResponse> getLessonById(@PathVariable UUID lessonId) {
    log.info("GET /lessons/{}", lessonId);
    return ApiResponse.<LessonResponse>builder()
        .result(lessonContentService.getLessonById(lessonId))
        .build();
  }

  @GetMapping("/{lessonId}/chapters")
  @Operation(summary = "List chapters of a lesson")
  public ApiResponse<List<ChapterResponse>> getChapters(@PathVariable UUID lessonId) {
    log.info("GET /lessons/{}/chapters", lessonId);
    return ApiResponse.<List<ChapterResponse>>builder()
        .result(lessonContentService.getChaptersByLessonId(lessonId))
        .build();
  }

  @GetMapping("/chapters/{chapterId}/lessons")
  @Operation(summary = "List lessons of a chapter")
  public ApiResponse<List<LessonResponse>> getLessonsByChapter(
      @PathVariable UUID chapterId,
      @RequestParam(required = false) String name) {
    log.info("GET /lessons/chapters/{}/lessons", chapterId);
    return ApiResponse.<List<LessonResponse>>builder()
        .result(lessonService.searchLessonsByChapterId(chapterId, name))
        .build();
  }

  // -----------------------------------------------------------------------
  // Delete
  // -----------------------------------------------------------------------

  @DeleteMapping("/{lessonId}")
  @Operation(
      summary = "Soft-delete a lesson and all its chapters",
      description = "Only the owning teacher or an ADMIN can delete a lesson.")
  public ApiResponse<Void> deleteLesson(@PathVariable UUID lessonId) {
    log.info("DELETE /lessons/{}", lessonId);
    lessonContentService.deleteLesson(lessonId);
    return ApiResponse.<Void>builder().message("Lesson deleted successfully").build();
  }

  @DeleteMapping("/chapters/{chapterId}")
  @Operation(
      summary = "Soft-delete a single chapter",
      description = "Only the owning teacher or an ADMIN can delete a chapter.")
  public ApiResponse<Void> deleteChapter(@PathVariable UUID chapterId) {
    log.info("DELETE /lessons/chapters/{}", chapterId);
    lessonContentService.deleteChapter(chapterId);
    return ApiResponse.<Void>builder().message("Chapter deleted successfully").build();
  }

  // -----------------------------------------------------------------------
  // Lesson CRUD
  // -----------------------------------------------------------------------

  @Operation(
      summary = "Create a lesson",
      description = "Manually creates a lesson inside a chapter (chapterId required in body).")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<LessonResponse> createLesson(@Valid @RequestBody CreateLessonRequest request) {
    log.info("POST /lessons – chapterId={}", request.getChapterId());
    return ApiResponse.<LessonResponse>builder()
        .result(lessonService.createLesson(request))
        .build();
  }

  @Operation(summary = "Update a lesson")
  @PutMapping("/{lessonId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<LessonResponse> updateLesson(
      @PathVariable UUID lessonId, @Valid @RequestBody UpdateLessonRequest request) {
    log.info("PUT /lessons/{}", lessonId);
    return ApiResponse.<LessonResponse>builder()
        .result(lessonService.updateLesson(lessonId, request))
        .build();
  }
}
