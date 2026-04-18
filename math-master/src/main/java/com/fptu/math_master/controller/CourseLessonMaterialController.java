package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.service.CourseLessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/lessons/{lessonId}/materials")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Lesson Materials", description = " supplementary file management for course lessons")
@SecurityRequirement(name = "bearerAuth")
public class CourseLessonMaterialController {

    CourseLessonService courseLessonService;

    @Operation(summary = "Upload a supplementary material file to a lesson")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CourseLessonResponse> addMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /courses/{}/lessons/{}/materials – file={}", courseId, lessonId, file.getOriginalFilename());
        return ApiResponse.<CourseLessonResponse>builder()
                .message("Material uploaded successfully")
                .result(courseLessonService.addMaterial(courseId, lessonId, file))
                .build();
    }

    @Operation(summary = "Remove a material file from a lesson")
    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CourseLessonResponse> removeMaterial(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @PathVariable String materialId) {
        log.info("DELETE /courses/{}/lessons/{}/materials/{}", courseId, lessonId, materialId);
        return ApiResponse.<CourseLessonResponse>builder()
                .message("Material removed successfully")
                .result(courseLessonService.removeMaterial(courseId, lessonId, materialId))
                .build();
    }
}
