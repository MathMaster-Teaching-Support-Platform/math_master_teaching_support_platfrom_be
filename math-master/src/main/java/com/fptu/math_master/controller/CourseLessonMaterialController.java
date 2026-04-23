package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.service.CourseLessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @Operation(summary = "Get a presigned download URL for a material file",
               description = "Accessible by enrolled students, teacher owner. Returns a time-limited URL that forces browser download.")
    @GetMapping("/{materialId}/download-url")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ApiResponse<String> getMaterialDownloadUrl(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @PathVariable String materialId) {
        log.info("GET /courses/{}/lessons/{}/materials/{}/download-url", courseId, lessonId, materialId);
        return ApiResponse.<String>builder()
                .result(courseLessonService.getMaterialDownloadUrl(courseId, lessonId, materialId))
                .build();
    }

    @Operation(summary = "Stream-download a material file directly through the server",
               description = "Streams the file bytes to the client. No presigned URL handling needed on the frontend.")
    @GetMapping("/{materialId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public void downloadMaterial(
            HttpServletResponse response,
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @PathVariable String materialId) throws IOException {
        log.info("GET /courses/{}/lessons/{}/materials/{}/download", courseId, lessonId, materialId);
        CourseLessonService.MaterialDownloadResult result =
                courseLessonService.downloadMaterial(courseId, lessonId, materialId);
        String encodedName = URLEncoder.encode(result.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType(result.contentType());
        response.setContentLength(result.content().length);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedName);
        response.getOutputStream().write(result.content());
        response.getOutputStream().flush();
    }
}
