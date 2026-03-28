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

  /** Bước 3: Hoàn tất upload, ráp các chunk lại và lưu CourseLesson */
  CourseLessonResponse completeUpload(UUID courseId, CompleteUploadRequest request);

  /** Lấy presigned URL để xem video (cho học sinh đã enroll) */
  String getVideoPresignedUrl(UUID courseId, UUID courseLessonId, UUID requesterId);
}
