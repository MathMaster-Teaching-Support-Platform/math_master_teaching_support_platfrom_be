package com.fptu.math_master.service.impl;

import software.amazon.awssdk.services.s3.S3Configuration;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.request.CompleteUploadRequest;
import com.fptu.math_master.dto.request.InitiateUploadRequest;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.dto.response.InitiateUploadResponse;
import com.fptu.math_master.dto.response.PartUploadUrlResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.service.VideoUploadService;
import com.fptu.math_master.util.SecurityUtils;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class VideoUploadServiceImpl implements VideoUploadService {

  MinioClient minioClient;
  
  @org.springframework.beans.factory.annotation.Qualifier("publicMinioClient")
  MinioClient publicMinioClient;
  
  MinioProperties minioProperties;
  CourseRepository courseRepository;
  CourseLessonRepository courseLessonRepository;
  LessonRepository lessonRepository;
  EnrollmentRepository enrollmentRepository;

  private static final int PRESIGNED_EXPIRY_MINUTES = 60;

  // ─── AWS SDK v2 client builders (MinIO is S3-compatible) ─────────────────

  private S3Client buildS3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(minioProperties.getEndpoint()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    minioProperties.getAccessKey(), minioProperties.getSecretKey())))
        .region(Region.US_EAST_1) // MinIO ignores region but SDK requires it
        .forcePathStyle(true)     // required for MinIO path-style URLs
        .build();
  }

  /**
   * Presigner uses the PUBLIC endpoint so the generated URL is browser-accessible.
   * MinIO validates the signature against the host in the URL, so signing and
   * serving must use the same host.
   */
  private S3Presigner buildPresigner() {
    String signingEndpoint = minioProperties.getPublicEndpoint();
    if (signingEndpoint == null || signingEndpoint.isBlank()) {
      signingEndpoint = minioProperties.getEndpoint();
    }
    return S3Presigner.builder()
        .endpointOverride(URI.create(signingEndpoint))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    minioProperties.getAccessKey(), minioProperties.getSecretKey())))
        .region(Region.US_EAST_1)
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private void ensureBucketExists(String bucket) {
    try {
      boolean exists =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!exists) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        log.info("Created MinIO bucket: {}", bucket);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to ensure bucket exists: " + bucket, e);
    }
  }

  private Course findCourseAndVerifyOwner(UUID courseId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    Course course =
        courseRepository
            .findByIdAndDeletedAtIsNull(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
    if (!course.getTeacherId().equals(currentUserId)) {
      throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
    }
    return course;
  }

  private String buildObjectKey(UUID courseId, String fileName) {
    String ext = "";
    int dot = fileName.lastIndexOf('.');
    if (dot > 0) ext = fileName.substring(dot);
    return courseId + "/" + UUID.randomUUID() + ext;
  }

  // ─── Step 1: Initiate multipart upload ───────────────────────────────────

  @Override
  public InitiateUploadResponse initiateUpload(UUID courseId, InitiateUploadRequest request) {
    findCourseAndVerifyOwner(courseId);

    String bucket = minioProperties.getCourseVideosBucket();
    ensureBucketExists(bucket);

    String objectKey = buildObjectKey(courseId, request.getFileName());

    try (S3Client s3 = buildS3Client()) {
      var response =
          s3.createMultipartUpload(
              CreateMultipartUploadRequest.builder()
                  .bucket(bucket)
                  .key(objectKey)
                  .contentType(request.getContentType())
                  .build());

      log.info("Initiated multipart upload: uploadId={}, key={}", response.uploadId(), objectKey);

      return InitiateUploadResponse.builder()
          .uploadId(response.uploadId())
          .objectKey(objectKey)
          .build();
    }
  }

  // ─── Step 2: Get presigned URL for a chunk ────────────────────────────────

  @Override
  public PartUploadUrlResponse getPartUploadUrl(
      UUID courseId, String uploadId, String objectKey, int partNumber) {
    findCourseAndVerifyOwner(courseId);

    String bucket = minioProperties.getCourseVideosBucket();

    try (S3Presigner presigner = buildPresigner()) {
      PresignedUploadPartRequest presigned =
          presigner.presignUploadPart(
              UploadPartPresignRequest.builder()
                  .signatureDuration(Duration.ofMinutes(PRESIGNED_EXPIRY_MINUTES))
                  .uploadPartRequest(
                      UploadPartRequest.builder()
                          .bucket(bucket)
                          .key(objectKey)
                          .uploadId(uploadId)
                          .partNumber(partNumber)
                          .build())
                  .build());

      String presignedUrl = presigned.url().toString();

      return PartUploadUrlResponse.builder()
          .presignedUrl(presignedUrl)
          .partNumber(partNumber)
          .build();
    }
  }

  // ─── Step 2 (Alternative): Upload via backend proxy ──────────────────────

  @Override
  public PartUploadUrlResponse uploadPartViaBackend(
      UUID courseId, String uploadId, String objectKey, int partNumber, byte[] chunkData) {
    findCourseAndVerifyOwner(courseId);

    String bucket = minioProperties.getCourseVideosBucket();

    try (S3Client s3 = buildS3Client()) {
      var uploadResponse = s3.uploadPart(
          UploadPartRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .uploadId(uploadId)
              .partNumber(partNumber)
              .contentLength((long) chunkData.length)
              .build(),
          software.amazon.awssdk.core.sync.RequestBody.fromBytes(chunkData));

      // S3 returns ETag with quotes, strip them for consistency
      String eTag = uploadResponse.eTag();
      if (eTag != null) {
        eTag = eTag.replaceAll("^\"|\"$", "");
      }
      
      log.info("Uploaded part {} for {}, ETag: {}", partNumber, objectKey, eTag);

      return PartUploadUrlResponse.builder()
          .presignedUrl(null) // Not needed for backend upload
          .partNumber(partNumber)
          .eTag(eTag)
          .build();
    }
  }

  // ─── Step 3: Complete multipart upload ───────────────────────────────────

  @Override
  public CourseLessonResponse completeUpload(UUID courseId, CompleteUploadRequest request) {
    findCourseAndVerifyOwner(courseId);

    var lesson =
        lessonRepository
            .findByIdAndNotDeleted(request.getLessonId())
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    String bucket = minioProperties.getCourseVideosBucket();

    List<CompletedPart> completedParts =
        request.getParts().stream()
            .map(
                p ->
                    CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getETag())
                        .build())
            .collect(Collectors.toList());

    log.info(
        "Completing multipart upload: bucket={}, key={}, uploadId={}, parts={}",
        bucket,
        request.getObjectKey(),
        request.getUploadId(),
        completedParts.stream()
            .map(p -> String.format("part=%d,etag=%s", p.partNumber(), p.eTag()))
            .collect(Collectors.joining("; ")));

    try (S3Client s3 = buildS3Client()) {
      s3.completeMultipartUpload(
          CompleteMultipartUploadRequest.builder()
              .bucket(bucket)
              .key(request.getObjectKey())
              .uploadId(request.getUploadId())
              .multipartUpload(
                  CompletedMultipartUpload.builder().parts(completedParts).build())
              .build());

      log.info(
          "Completed multipart upload: key={}, parts={}",
          request.getObjectKey(),
          completedParts.size());
    } catch (Exception e) {
      log.error("Failed to complete multipart upload", e);
      // Best-effort abort to free MinIO resources
      try (S3Client s3 = buildS3Client()) {
        s3.abortMultipartUpload(
            AbortMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(request.getObjectKey())
                .uploadId(request.getUploadId())
                .build());
      } catch (Exception ignored) {
        // silent
      }
      throw new RuntimeException("Failed to complete video upload", e);
    }

    CourseLesson courseLesson =
        CourseLesson.builder()
            .courseId(courseId)
            .lessonId(request.getLessonId())
            .videoUrl(request.getObjectKey()) // store object key; serve via presigned GET URL
            .videoTitle(request.getVideoTitle())
            .orderIndex(request.getOrderIndex())
            .isFreePreview(request.isFreePreview())
            .durationSeconds(request.getDurationSeconds())
            .materials(request.getMaterials())
            .build();

    courseLesson = courseLessonRepository.save(courseLesson);
    log.info("CourseLesson saved: {}", courseLesson.getId());

    return CourseLessonResponse.builder()
        .id(courseLesson.getId())
        .courseId(courseLesson.getCourseId())
        .lessonId(courseLesson.getLessonId())
        .lessonTitle(lesson.getTitle())
        .videoUrl(courseLesson.getVideoUrl())
        .videoTitle(courseLesson.getVideoTitle())
        .durationSeconds(courseLesson.getDurationSeconds())
        .orderIndex(courseLesson.getOrderIndex())
        .isFreePreview(courseLesson.isFreePreview())
        .materials(courseLesson.getMaterials())
        .createdAt(courseLesson.getCreatedAt())
        .updatedAt(courseLesson.getUpdatedAt())
        .build();
  }

  // ─── Get presigned GET URL to stream video ────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public String getVideoPresignedUrl(UUID courseId, UUID courseLessonId, UUID requesterId) {
    CourseLesson cl =
        courseLessonRepository
            .findByIdAndDeletedAtIsNull(courseLessonId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    if (!cl.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    // Free preview: anyone can watch without enrollment
    if (!cl.isFreePreview()) {
      boolean enrolled =
          enrollmentRepository
              .findByStudentIdAndCourseIdAndDeletedAtIsNull(requesterId, courseId)
              .map(e -> "ACTIVE".equals(e.getStatus().name()))
              .orElse(false);

      Course course =
          courseRepository
              .findByIdAndDeletedAtIsNull(courseId)
              .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

      boolean isOwner = course.getTeacherId().equals(requesterId);

      if (!enrolled && !isOwner) {
        throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
      }
    }

    // Generate presigned URL with publicMinioClient
    // publicMinioClient uses MINIO_PUBLIC_ENDPOINT (http://localhost:9000)
    // This creates signature with correct hostname for browser access
    String bucket = minioProperties.getCourseVideosBucket();
    String objectKey = cl.getVideoUrl();
    
    try {
      String presignedUrl = publicMinioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.GET)
              .bucket(bucket)
              .object(objectKey)
              .expiry(7, java.util.concurrent.TimeUnit.DAYS)
              .build());
      
      log.info("Generated presigned URL: {}", presignedUrl);
      return presignedUrl;
    } catch (Exception e) {
      log.error("Failed to generate presigned URL for video: {}", objectKey, e);
      // Fallback: try with internal client
      try {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectKey)
                .expiry(7, java.util.concurrent.TimeUnit.DAYS)
                .build());
      } catch (Exception e2) {
        throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
      }
    }
  }

  // DEPRECATED: Stream method removed - use presigned URL directly from MinIO instead
  // @Override
  // public void streamVideo(UUID courseId, UUID courseLessonId, UUID requesterId, String token,
  //     jakarta.servlet.http.HttpServletResponse response) throws Exception {
  //   CourseLesson cl =
  //       courseLessonRepository
  //           .findByIdAndDeletedAtIsNull(courseLessonId)
  //           .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));
  //
  //   if (!cl.getCourseId().equals(courseId)) {
  //     throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
  //   }
  //
  //   // Check access permission
  //   if (!cl.isFreePreview()) {
  //     boolean enrolled =
  //         enrollmentRepository
  //             .findByStudentIdAndCourseIdAndDeletedAtIsNull(requesterId, courseId)
  //             .map(e -> "ACTIVE".equals(e.getStatus().name()))
  //             .orElse(false);
  //
  //     Course course =
  //         courseRepository
  //             .findByIdAndDeletedAtIsNull(courseId)
  //             .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
  //
  //     boolean isOwner = course.getTeacherId().equals(requesterId);
  //
  //     if (!enrolled && !isOwner) {
  //       throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
  //     }
  //   }
  //
  //   // Stream from MinIO
  //   String bucket = minioProperties.getCourseVideosBucket();
  //   try (var stream = minioClient.getObject(
  //       io.minio.GetObjectArgs.builder()
  //           .bucket(bucket)
  //           .object(cl.getVideoUrl())
  //           .build())) {
  //     
  //     response.setContentType("video/mp4");
  //     response.setHeader("Accept-Ranges", "bytes");
  //     response.setHeader("Cache-Control", "no-cache");
  //     
  //     stream.transferTo(response.getOutputStream());
  //     response.getOutputStream().flush();
  //   }
  // }
}
