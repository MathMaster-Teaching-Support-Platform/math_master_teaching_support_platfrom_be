package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.service.LessonSlideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lesson-slides/public")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Lesson Slides Public", description = "Public endpoints for student lesson slide viewing")
public class LessonSlidePublicController {

  LessonSlideService lessonSlideService;

  @GetMapping("/lessons/{lessonId}")
  @Operation(
      summary = "Get published lesson slide content",
      description = "Public endpoint for students to view published lesson slide content.")
  public ApiResponse<LessonResponse> getPublishedLessonSlide(@PathVariable UUID lessonId) {
    log.info("GET /lesson-slides/public/lessons/{}", lessonId);
    return ApiResponse.<LessonResponse>builder()
        .result(lessonSlideService.getPublishedLessonSlide(lessonId))
        .build();
  }
}
