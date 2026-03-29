package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CompleteUploadRequest;
import com.fptu.math_master.dto.request.InitiateUploadRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.dto.response.InitiateUploadResponse;
import com.fptu.math_master.dto.response.PartUploadUrlResponse;
import com.fptu.math_master.service.VideoUploadService;
import com.fptu.math_master.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses/{courseId}/lessons/upload")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Video Upload", description = "S3 Multipart Upload APIs for course lesson videos")
@SecurityRequirement(name = "bearerAuth")
public class VideoUploadController {

  VideoUploadService videoUploadService;

  @Operation(
      summary = "Step 1 — Initiate multipart upload",
      description = "Returns uploadId and objectKey to use in subsequent steps")
  @PostMapping("/initiate")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<InitiateUploadResponse> initiateUpload(
      @PathVariable UUID courseId,
      @Valid @RequestBody InitiateUploadRequest request) {
    log.info("POST /courses/{}/lessons/upload/initiate – file={}", courseId, request.getFileName());
    return ApiResponse.<InitiateUploadResponse>builder()
        .result(videoUploadService.initiateUpload(courseId, request))
        .build();
  }

  @Operation(
      summary = "Step 2 — Get presigned URL for a chunk",
      description = "Returns a presigned PUT URL valid for 60 minutes for the given part number")
  @GetMapping("/part-url")
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<PartUploadUrlResponse> getPartUploadUrl(
      @PathVariable UUID courseId,
      @RequestParam String uploadId,
      @RequestParam String objectKey,
      @RequestParam int partNumber) {
    return ApiResponse.<PartUploadUrlResponse>builder()
        .result(videoUploadService.getPartUploadUrl(courseId, uploadId, objectKey, partNumber))
        .build();
  }

  @Operation(
      summary = "Step 2 (Alternative) — Upload chunk via backend proxy",
      description = "Upload chunk through backend to avoid CORS issues. Returns ETag.")
  @PostMapping("/upload-part")
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<PartUploadUrlResponse> uploadPart(
      @PathVariable UUID courseId,
      @RequestParam String uploadId,
      @RequestParam String objectKey,
      @RequestParam int partNumber,
      @RequestBody byte[] chunkData) {
    return ApiResponse.<PartUploadUrlResponse>builder()
        .result(videoUploadService.uploadPartViaBackend(courseId, uploadId, objectKey, partNumber, chunkData))
        .build();
  }

  @Operation(
      summary = "Step 3 — Complete multipart upload",
      description = "Assembles all chunks and saves the CourseLesson record")
  @PostMapping("/complete")
  @PreAuthorize("hasRole('TEACHER')")
  public ApiResponse<CourseLessonResponse> completeUpload(
      @PathVariable UUID courseId,
      @Valid @RequestBody CompleteUploadRequest request) {
    log.info(
        "POST /courses/{}/lessons/upload/complete – parts={}", courseId, request.getParts().size());
    return ApiResponse.<CourseLessonResponse>builder()
        .result(videoUploadService.completeUpload(courseId, request))
        .build();
  }

  @Operation(
      summary = "Get presigned URL to watch a video",
      description = "Returns a time-limited presigned URL directly from MinIO. Requires enrollment or teacher ownership, or lesson must be free preview.")
  @GetMapping("/{courseLessonId}/video-url")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<String> getVideoUrl(
      @PathVariable UUID courseId,
      @PathVariable UUID courseLessonId) {
    UUID requesterId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<String>builder()
        .result(videoUploadService.getVideoPresignedUrl(courseId, courseLessonId, requesterId))
        .build();
  }

  // DEPRECATED: Stream endpoint removed - use presigned URL directly from MinIO instead
  // @Operation(
  //     summary = "Stream video via backend proxy",
  //     description = "Proxy video streaming to avoid CORS issues with MinIO. Token can be passed via query string for video players.")
  // @GetMapping("/{courseLessonId}/stream")
  // public void streamVideo(
  //     @PathVariable UUID courseId,
  //     @PathVariable UUID courseLessonId,
  //     @RequestParam(required = false) String token,
  //     jakarta.servlet.http.HttpServletRequest request,
  //     jakarta.servlet.http.HttpServletResponse response) throws Exception {
  //   
  //   // Try to get user from current auth, fallback to token parameter
  //   UUID requesterId = null;
  //   try {
  //     requesterId = SecurityUtils.getCurrentUserId();
  //   } catch (Exception e) {
  //     // No auth in context, will use token parameter in service
  //   }
  //   
  //   videoUploadService.streamVideo(courseId, courseLessonId, requesterId, token, response);
  // }
}
