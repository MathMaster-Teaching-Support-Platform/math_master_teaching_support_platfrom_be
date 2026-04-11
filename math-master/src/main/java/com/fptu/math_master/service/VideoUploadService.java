package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CompleteUploadRequest;
import com.fptu.math_master.dto.request.InitiateUploadRequest;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.dto.response.InitiateUploadResponse;
import com.fptu.math_master.dto.response.PartUploadUrlResponse;
import java.util.UUID;

public interface VideoUploadService {

  /** Bước 1: Khởi tạo multipart upload, trả về uploadId + objectKey */
  InitiateUploadResponse initiateUpload(UUID courseId, InitiateUploadRequest request);

  /** Bước 2: Lấy presigned URL cho từng chunk */
  PartUploadUrlResponse getPartUploadUrl(UUID courseId, String uploadId, String objectKey, int partNumber);

  /** Bước 2 (Alternative): Upload chunk qua backend để tránh CORS issues */
  PartUploadUrlResponse uploadPartViaBackend(UUID courseId, String uploadId, String objectKey, int partNumber, byte[] chunkData);

  /** Bước 3: Hoàn tất upload, ráp các chunk lại và lưu CourseLesson */
  CourseLessonResponse completeUpload(UUID courseId, CompleteUploadRequest request);

  /** Lấy presigned URL để xem video (cho học sinh đã enroll hoặc free preview) */
  String getVideoPresignedUrl(UUID courseId, UUID courseLessonId, UUID requesterId);

  // DEPRECATED: Stream method removed - use presigned URL directly from MinIO instead
  // /** Stream video qua backend proxy để tránh CORS issues */
  // void streamVideo(UUID courseId, UUID courseLessonId, UUID requesterId, String token, jakarta.servlet.http.HttpServletResponse response) throws Exception;
}