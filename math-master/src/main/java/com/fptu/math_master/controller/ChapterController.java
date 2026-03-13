package com.fptu.math_master.controller;

import java.util.List;
import java.util.UUID;

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

import com.fptu.math_master.dto.request.CreateChapterRequest;
import com.fptu.math_master.dto.request.UpdateChapterRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.ChapterResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.service.ChapterService;
import com.fptu.math_master.service.LessonService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/chapters")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Chapters", description = "CRUD for chapters within a curriculum")
@SecurityRequirement(name = "bearerAuth")
public class ChapterController {

  ChapterService chapterService;
  LessonService lessonService;

  @Operation(
      summary = "Create a chapter",
      description = "Creates a new chapter inside a curriculum. orderIndex is auto-assigned if omitted.")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<ChapterResponse> createChapter(
      @Valid @RequestBody CreateChapterRequest request) {
    log.info("POST /chapters – curriculumId={}", request.getCurriculumId());
    return ApiResponse.<ChapterResponse>builder()
        .result(chapterService.createChapter(request))
        .build();
  }

  @Operation(summary = "Get chapter by ID")
  @GetMapping("/{chapterId}")
  public ApiResponse<ChapterResponse> getChapterById(@PathVariable UUID chapterId) {
    log.info("GET /chapters/{}", chapterId);
    return ApiResponse.<ChapterResponse>builder()
        .result(chapterService.getChapterById(chapterId))
        .build();
  }

  @Operation(
      summary = "List chapters by curriculum",
      description = "Returns all active chapters for a curriculum, ordered by orderIndex.")
  @GetMapping("/curriculum/{curriculumId}")
  public ApiResponse<List<ChapterResponse>> getChaptersByCurriculum(
      @PathVariable UUID curriculumId) {
    log.info("GET /chapters/curriculum/{}", curriculumId);
    return ApiResponse.<List<ChapterResponse>>builder()
        .result(chapterService.getChaptersByCurriculumId(curriculumId))
        .build();
  }

  @Operation(
      summary = "List lessons in a chapter",
      description = "Returns all active lessons inside the given chapter, ordered by orderIndex.")
  @GetMapping("/{chapterId}/lessons")
  public ApiResponse<List<LessonResponse>> getLessonsByChapter(@PathVariable UUID chapterId) {
    log.info("GET /chapters/{}/lessons", chapterId);
    return ApiResponse.<List<LessonResponse>>builder()
        .result(lessonService.getLessonsByChapterId(chapterId))
        .build();
  }

  @Operation(summary = "Update a chapter")
  @PutMapping("/{chapterId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<ChapterResponse> updateChapter(
      @PathVariable UUID chapterId, @Valid @RequestBody UpdateChapterRequest request) {
    log.info("PUT /chapters/{}", chapterId);
    return ApiResponse.<ChapterResponse>builder()
        .result(chapterService.updateChapter(chapterId, request))
        .build();
  }

  @Operation(summary = "Delete (soft-delete) a chapter")
  @DeleteMapping("/{chapterId}")
  @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
  public ApiResponse<Void> deleteChapter(@PathVariable UUID chapterId) {
    log.info("DELETE /chapters/{}", chapterId);
    chapterService.deleteChapter(chapterId);
    return ApiResponse.<Void>builder().message("Chapter deleted successfully").build();
  }
}
