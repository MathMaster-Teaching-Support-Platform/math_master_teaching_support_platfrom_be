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
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
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
  private static final long MAX_VIDEO_SIZE = 2L * 1024 * 1024 * 1024; // 2GB
  private static final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of(".mp4", ".mkv", ".mov");
  private static final java.util.Set<String> ALLOWED_MIME_TYPES = java.util.Set.of("video/mp4", "video/x-matroska",
      "video/quicktime");

  // ─── AWS SDK v2 client builders (MinIO is S3-compatible) ─────────────────

  private S3Client buildS3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(minioProperties.getEndpoint()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    minioProperties.getAccessKey(), minioProperties.getSecretKey())))
        .region(Region.US_EAST_1) // MinIO ignores region but SDK requires it
        .forcePathStyle(true) // required for MinIO path-style URLs
        .build();
  }

  /**
   * Presigner uses the PUBLIC endpoint so the generated URL is
   * browser-accessible.
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
      boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
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
    Course course = courseRepository
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
    if (dot > 0)
      ext = fileName.substring(dot);
    return courseId + "/" + UUID.randomUUID() + ext;
  }

  // ─── Step 1: Initiate multipart upload ───────────────────────────────────

  @Override
  public InitiateUploadResponse initiateUpload(UUID courseId, InitiateUploadRequest request) {
    findCourseAndVerifyOwner(courseId);

    // 1. Validate File Size
    if (request.getFileSize() > MAX_VIDEO_SIZE) {
      String message = ErrorCode.RESOURCE_FILE_TOO_LARGE.getMessage()
          .replace("{limit}", com.fptu.math_master.util.FormatUtils.formatFileSize(MAX_VIDEO_SIZE))
          .replace("{actual}", com.fptu.math_master.util.FormatUtils.formatFileSize(request.getFileSize()));
      throw new AppException(ErrorCode.RESOURCE_FILE_TOO_LARGE, message);
    }

    // 2. Validate Extension
    String fileName = request.getFileName();
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex <= 0) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }
    String ext = fileName.substring(dotIndex).toLowerCase();
    if (!ALLOWED_EXTENSIONS.contains(ext)) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }

    // 3. Validate MIME Type
    if (!ALLOWED_MIME_TYPES.contains(request.getContentType().toLowerCase())) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }

    String bucket = minioProperties.getCourseVideosBucket();
    ensureBucketExists(bucket);

    String objectKey = buildObjectKey(courseId, request.getFileName());

    try (S3Client s3 = buildS3Client()) {
      var response = s3.createMultipartUpload(
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
      PresignedUploadPartRequest presigned = presigner.presignUploadPart(
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
    Course course = findCourseAndVerifyOwner(courseId);

    String lessonTitle = null;

    if (course.getProvider() == com.fptu.math_master.enums.CourseProvider.MINISTRY) {
      if (request.getLessonId() == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST); // specific error handled by global handler
      }
      var lesson = lessonRepository
          .findByIdAndNotDeleted(request.getLessonId())
          .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
      lessonTitle = lesson.getTitle();
    } else {
      if (request.getSectionId() == null || request.getCustomTitle() == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }
      lessonTitle = request.getCustomTitle();
    }

    String bucket = minioProperties.getCourseVideosBucket();

    List<CompletedPart> completedParts = request.getParts().stream()
        .map(
            p -> CompletedPart.builder()
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

    // ─── IDEMPOTENCY CHECK: Find existing or create new ──────────────────────
    CourseLesson courseLesson;
    if (course.getProvider() == com.fptu.math_master.enums.CourseProvider.MINISTRY) {
      courseLesson = courseLessonRepository
          .findByCourseIdAndLessonIdAndDeletedAtIsNull(courseId, request.getLessonId())
          .orElse(new CourseLesson());
    } else {
      courseLesson = courseLessonRepository
          .findByCourseIdAndSectionIdAndCustomTitleAndDeletedAtIsNull(
              courseId, request.getSectionId(), request.getCustomTitle())
          .orElse(new CourseLesson());
    }

    // Update fields
    courseLesson.setCourseId(courseId);
    courseLesson.setLessonId(request.getLessonId());
    courseLesson.setSectionId(request.getSectionId());
    courseLesson.setCustomTitle(request.getCustomTitle());
    courseLesson.setCustomDescription(request.getCustomDescription());
    courseLesson.setVideoUrl(request.getObjectKey());
    courseLesson.setVideoTitle(request.getVideoTitle());
    courseLesson.setOrderIndex(request.getOrderIndex());
    courseLesson.setFreePreview(request.isFreePreview());
    courseLesson.setDurationSeconds(request.getDurationSeconds());
    courseLesson.setMaterials(request.getMaterials());

    courseLesson = courseLessonRepository.save(courseLesson);
    log.info("CourseLesson saved (idempotent): {}", courseLesson.getId());

    return CourseLessonResponse.builder()
        .id(courseLesson.getId())
        .courseId(courseLesson.getCourseId())
        .lessonId(courseLesson.getLessonId())
        .sectionId(courseLesson.getSectionId())
        .lessonTitle(lessonTitle)
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
    CourseLesson cl = courseLessonRepository
        .findByIdAndDeletedAtIsNull(courseLessonId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));

    if (!cl.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    // Udemy-style access: only explicit free-preview lessons are publicly viewable.
    // All other lessons require active enrollment, teacher ownership, or admin
    // role.
    if (!cl.isFreePreview()) {
      boolean enrolled = enrollmentRepository
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(requesterId, courseId)
          .map(e -> "ACTIVE".equals(e.getStatus().name()))
          .orElse(false);

      if (!enrolled) {
        Course course = courseRepository
            .findByIdAndDeletedAtIsNull(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        boolean isOwner = requesterId != null && course.getTeacherId().equals(requesterId);
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");

        if (!isOwner && !isAdmin) {
          throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
        }
      }
    }

    // Generate presigned URL with S3Presigner using configured public endpoint.
    // This guarantees the returned URL host is browser-accessible (not internal
    // Docker hostname).
    String bucket = minioProperties.getCourseVideosBucket();
    String objectKey = cl.getVideoUrl();

    try (S3Presigner presigner = buildPresigner()) {
      PresignedGetObjectRequest presigned = presigner.presignGetObject(
          GetObjectPresignRequest.builder()
              .signatureDuration(Duration.ofDays(7))
              .getObjectRequest(
                  GetObjectRequest.builder()
                      .bucket(bucket)
                      .key(objectKey)
                      .build())
              .build());

      String presignedUrl = presigned.url().toString();

      log.info("Generated presigned URL: {}", presignedUrl);
      return presignedUrl;
    } catch (Exception e) {
      log.error("Failed to generate presigned URL for video: {}", objectKey, e);
      throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
  }

  // DEPRECATED: Stream method removed - use presigned URL directly from MinIO
  // instead
  // @Override
  // public void streamVideo(UUID courseId, UUID courseLessonId, UUID requesterId,
  // String token,
  // jakarta.servlet.http.HttpServletResponse response) throws Exception {
  // CourseLesson cl =
  // courseLessonRepository
  // .findByIdAndDeletedAtIsNull(courseLessonId)
  // .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));
  //
  // if (!cl.getCourseId().equals(courseId)) {
  // throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
  // }
  //
  // // Check access permission
  // if (!cl.isFreePreview()) {
  // boolean enrolled =
  // enrollmentRepository
  // .findByStudentIdAndCourseIdAndDeletedAtIsNull(requesterId, courseId)
  // .map(e -> "ACTIVE".equals(e.getStatus().name()))
  // .orElse(false);
  //
  // Course course =
  // courseRepository
  // .findByIdAndDeletedAtIsNull(courseId)
  // .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
  //
  // boolean isOwner = course.getTeacherId().equals(requesterId);
  //
  // if (!enrolled && !isOwner) {
  // throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
  // }
  // }
  //
  // // Stream from MinIO
  // String bucket = minioProperties.getCourseVideosBucket();
  // try (var stream = minioClient.getObject(
  // io.minio.GetObjectArgs.builder()
  // .bucket(bucket)
  // .object(cl.getVideoUrl())
  // .build())) {
  //
  // response.setContentType("video/mp4");
  // response.setHeader("Accept-Ranges", "bytes");
  // response.setHeader("Cache-Control", "no-cache");
  //
  // stream.transferTo(response.getOutputStream());
  // response.getOutputStream().flush();
  // }
  // }
}
