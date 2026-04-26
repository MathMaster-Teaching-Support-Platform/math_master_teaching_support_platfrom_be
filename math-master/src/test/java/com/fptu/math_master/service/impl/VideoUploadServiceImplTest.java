package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.request.CompleteUploadRequest;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.enums.CourseProvider;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.util.SecurityUtils;
import io.minio.MinioClient;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

@DisplayName("VideoUploadServiceImpl - Tests")
class VideoUploadServiceImplTest extends BaseUnitTest {

  @InjectMocks private VideoUploadServiceImpl videoUploadService;

  @Mock private MinioClient minioClient;
  @Mock private MinioClient publicMinioClient;
  @Mock private MinioProperties minioProperties;
  @Mock private CourseRepository courseRepository;
  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private EnrollmentRepository enrollmentRepository;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  private static final UUID COURSE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID TEACHER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID LESSON_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID COURSE_LESSON_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  private Course buildCourse(UUID teacherId, CourseProvider provider) {
    Course course = new Course();
    course.setTeacherId(teacherId);
    course.setProvider(provider);
    return course;
  }

  private CourseLesson buildCourseLesson(UUID courseId, String videoUrl, boolean freePreview) {
    CourseLesson lesson = new CourseLesson();
    lesson.setId(COURSE_LESSON_ID);
    lesson.setCourseId(courseId);
    lesson.setVideoUrl(videoUrl);
    lesson.setFreePreview(freePreview);
    return lesson;
  }

  private Enrollment buildEnrollment(EnrollmentStatus status) {
    Enrollment enrollment = new Enrollment();
    enrollment.setStatus(status);
    enrollment.setCourseId(COURSE_ID);
    enrollment.setStudentId(OTHER_USER_ID);
    return enrollment;
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
    try {
      Method method = VideoUploadServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return (T) method.invoke(videoUploadService, args);
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw (Exception) cause;
    }
  }

  @Nested
  @DisplayName("private helpers")
  class PrivateHelperTests {

    @Test
    void it_should_return_course_when_owner_matches_current_user() throws Exception {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      Course course = buildCourse(TEACHER_ID, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      // ===== ACT =====
      Course actual =
          invokePrivate("findCourseAndVerifyOwner", new Class<?>[] {UUID.class}, COURSE_ID);

      // ===== ASSERT =====
      assertEquals(TEACHER_ID, actual.getTeacherId());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void it_should_throw_not_found_when_course_does_not_exist() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> invokePrivate("findCourseAndVerifyOwner", new Class<?>[] {UUID.class}, COURSE_ID));
      assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void it_should_throw_access_denied_when_owner_is_different() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_USER_ID);
      Course course = buildCourse(TEACHER_ID, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> invokePrivate("findCourseAndVerifyOwner", new Class<?>[] {UUID.class}, COURSE_ID));
      assertEquals(ErrorCode.COURSE_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verifyNoMoreInteractions(courseRepository);
    }

    @Test
    void it_should_keep_file_extension_when_building_object_key() throws Exception {
      // ===== ARRANGE =====
      String fileName = "bai-giang-toan.mp4";

      // ===== ACT =====
      String key = invokePrivate("buildObjectKey", new Class<?>[] {UUID.class, String.class}, COURSE_ID, fileName);

      // ===== ASSERT =====
      assertTrue(key.startsWith(COURSE_ID + "/"));
      assertTrue(key.endsWith(".mp4"));
    }

    @Test
    void it_should_generate_key_without_extension_when_filename_has_no_dot() throws Exception {
      // ===== ARRANGE =====
      String fileName = "video_bai_giang";

      // ===== ACT =====
      String key = invokePrivate("buildObjectKey", new Class<?>[] {UUID.class, String.class}, COURSE_ID, fileName);

      // ===== ASSERT =====
      assertTrue(key.startsWith(COURSE_ID + "/"));
      assertFalse(key.endsWith("."));
    }

    @Test
    void it_should_generate_distinct_object_keys_for_same_input() throws Exception {
      // ===== ARRANGE =====
      String fileName = "de-thi-thu.pdf";

      // ===== ACT =====
      String first = invokePrivate("buildObjectKey", new Class<?>[] {UUID.class, String.class}, COURSE_ID, fileName);
      String second = invokePrivate("buildObjectKey", new Class<?>[] {UUID.class, String.class}, COURSE_ID, fileName);

      // ===== ASSERT =====
      assertNotEquals(first, second);
    }

  }

  @Nested
  @DisplayName("completeUpload()")
  class CompleteUploadTests {

    @Test
    void it_should_throw_invalid_request_for_ministry_course_when_lesson_id_is_missing() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.MINISTRY)));

      CompleteUploadRequest request =
          CompleteUploadRequest.builder()
              .uploadId("upload-001")
              .objectKey("course/video-001.mp4")
              .parts(List.of(new CompleteUploadRequest.PartInfo(1, "etag-001")))
              .lessonId(null)
              .build();

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> videoUploadService.completeUpload(COURSE_ID, request));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(lessonRepository, never()).findByIdAndNotDeleted(any());
      verifyNoMoreInteractions(courseRepository, lessonRepository, courseLessonRepository);
    }

    @Test
    void it_should_throw_invalid_request_for_custom_course_when_section_or_title_is_missing() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      CompleteUploadRequest request =
          CompleteUploadRequest.builder()
              .uploadId("upload-002")
              .objectKey("course/video-002.mp4")
              .parts(List.of(new CompleteUploadRequest.PartInfo(1, "etag-002")))
              .sectionId(null)
              .customTitle(null)
              .build();

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> videoUploadService.completeUpload(COURSE_ID, request));
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verifyNoMoreInteractions(courseRepository, lessonRepository, courseLessonRepository);
    }

    @Test
    void it_should_complete_upload_and_save_course_lesson_when_request_is_valid_for_custom_course() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));
      when(courseLessonRepository.save(any(CourseLesson.class)))
          .thenAnswer(
              invocation -> {
                CourseLesson saved = invocation.getArgument(0);
                saved.setId(COURSE_LESSON_ID);
                return saved;
              });

      S3Client s3Client = Mockito.mock(S3Client.class);
      S3ClientBuilder s3Builder = Mockito.mock(S3ClientBuilder.class);
      when(s3Builder.endpointOverride(any(URI.class))).thenReturn(s3Builder);
      when(s3Builder.credentialsProvider(any())).thenReturn(s3Builder);
      when(s3Builder.region(any())).thenReturn(s3Builder);
      when(s3Builder.forcePathStyle(true)).thenReturn(s3Builder);
      when(s3Builder.build()).thenReturn(s3Client);
      when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
          .thenReturn(CompleteMultipartUploadResponse.builder().build());

      CompleteUploadRequest request =
          CompleteUploadRequest.builder()
              .uploadId("upload-success-001")
              .objectKey("11111111-1111-1111-1111-111111111111/video.mp4")
              .parts(
                  List.of(
                      new CompleteUploadRequest.PartInfo(1, "etag-1"),
                      new CompleteUploadRequest.PartInfo(2, "etag-2")))
              .sectionId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
              .customTitle("Bai giang ham so bac hai")
              .customDescription("Noi dung tuan 1")
              .videoTitle("Video bai giang")
              .orderIndex(1)
              .isFreePreview(true)
              .durationSeconds(420)
              .materials("{\"slides\":\"link\"}")
              .build();

      // ===== ACT =====
      try (MockedStatic<S3Client> s3ClientStatic = Mockito.mockStatic(S3Client.class)) {
        s3ClientStatic.when(S3Client::builder).thenReturn(s3Builder);
        var response = videoUploadService.completeUpload(COURSE_ID, request);

        // ===== ASSERT =====
        assertEquals(COURSE_LESSON_ID, response.getId());
        assertEquals(COURSE_ID, response.getCourseId());
        assertEquals("Bai giang ham so bac hai", response.getLessonTitle());
        assertEquals("11111111-1111-1111-1111-111111111111/video.mp4", response.getVideoUrl());
      }

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(courseLessonRepository, times(1)).save(any(CourseLesson.class));
    }

    @Test
    void it_should_abort_multipart_upload_and_throw_runtime_exception_when_complete_upload_fails() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      S3Client s3Client = Mockito.mock(S3Client.class);
      S3ClientBuilder s3Builder = Mockito.mock(S3ClientBuilder.class);
      when(s3Builder.endpointOverride(any(URI.class))).thenReturn(s3Builder);
      when(s3Builder.credentialsProvider(any())).thenReturn(s3Builder);
      when(s3Builder.region(any())).thenReturn(s3Builder);
      when(s3Builder.forcePathStyle(true)).thenReturn(s3Builder);
      when(s3Builder.build()).thenReturn(s3Client);
      when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
          .thenThrow(new RuntimeException("s3 complete failed"));
      when(s3Client.abortMultipartUpload(any(software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest.class)))
          .thenReturn(AbortMultipartUploadResponse.builder().build());

      CompleteUploadRequest request =
          CompleteUploadRequest.builder()
              .uploadId("upload-fail-001")
              .objectKey("11111111-1111-1111-1111-111111111111/video-fail.mp4")
              .parts(List.of(new CompleteUploadRequest.PartInfo(1, "etag-1")))
              .sectionId(UUID.fromString("55555555-5555-5555-5555-555555555555"))
              .customTitle("Bai giang xac suat")
              .build();

      // ===== ACT & ASSERT =====
      try (MockedStatic<S3Client> s3ClientStatic = Mockito.mockStatic(S3Client.class)) {
        s3ClientStatic.when(S3Client::builder).thenReturn(s3Builder);
        RuntimeException exception =
            assertThrows(RuntimeException.class, () -> videoUploadService.completeUpload(COURSE_ID, request));
        assertTrue(exception.getMessage().contains("Failed to complete video upload"));
      }

      // ===== VERIFY =====
      verify(s3Client, times(1)).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
      verify(s3Client, times(1))
          .abortMultipartUpload(any(software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest.class));
      verify(courseLessonRepository, never()).save(any());
    }

    @Test
    void it_should_throw_lesson_not_found_when_ministry_course_lesson_does_not_exist() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.MINISTRY)));
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.empty());

      CompleteUploadRequest request =
          CompleteUploadRequest.builder()
              .uploadId("upload-ministry-404")
              .objectKey("course/video-ministry.mp4")
              .parts(List.of(new CompleteUploadRequest.PartInfo(1, "etag-001")))
              .lessonId(LESSON_ID)
              .build();

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> videoUploadService.completeUpload(COURSE_ID, request));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(LESSON_ID);
      verify(courseLessonRepository, never()).save(any());
    }

    @Test
    void it_should_complete_upload_when_ministry_course_has_valid_lesson() throws Exception {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.MINISTRY)));
      Lesson lesson = new Lesson();
      lesson.setId(LESSON_ID);
      lesson.setTitle("Bai 1: Ham so");
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(lesson));
      when(courseLessonRepository.save(any(CourseLesson.class)))
          .thenAnswer(
              invocation -> {
                CourseLesson saved = invocation.getArgument(0);
                saved.setId(COURSE_LESSON_ID);
                return saved;
              });

      S3Client s3Client = Mockito.mock(S3Client.class);
      S3ClientBuilder s3Builder = Mockito.mock(S3ClientBuilder.class);
      when(s3Builder.endpointOverride(any(URI.class))).thenReturn(s3Builder);
      when(s3Builder.credentialsProvider(any())).thenReturn(s3Builder);
      when(s3Builder.region(any())).thenReturn(s3Builder);
      when(s3Builder.forcePathStyle(true)).thenReturn(s3Builder);
      when(s3Builder.build()).thenReturn(s3Client);
      when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
          .thenReturn(CompleteMultipartUploadResponse.builder().build());

      CompleteUploadRequest request =
          CompleteUploadRequest.builder()
              .uploadId("upload-ministry-001")
              .objectKey("course/video-ministry-001.mp4")
              .parts(List.of(new CompleteUploadRequest.PartInfo(1, "etag-1")))
              .lessonId(LESSON_ID)
              .videoTitle("Video ministry")
              .build();

      // ===== ACT =====
      com.fptu.math_master.dto.response.CourseLessonResponse response;
      try (MockedStatic<S3Client> s3ClientStatic = Mockito.mockStatic(S3Client.class)) {
        s3ClientStatic.when(S3Client::builder).thenReturn(s3Builder);
        response = videoUploadService.completeUpload(COURSE_ID, request);
      }

      // ===== ASSERT =====
      assertEquals("Bai 1: Ham so", response.getLessonTitle());
      assertEquals(LESSON_ID, response.getLessonId());
    }
  }

  @Nested
  @DisplayName("getVideoPresignedUrl()")
  class GetVideoPresignedUrlTests {

    @Test
    void it_should_throw_course_lesson_not_found_when_course_lesson_does_not_exist() {
      // ===== ARRANGE =====
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID));
      assertEquals(ErrorCode.COURSE_LESSON_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verifyNoMoreInteractions(courseLessonRepository, enrollmentRepository, courseRepository);
    }

    @Test
    void it_should_throw_course_lesson_not_found_when_lesson_belongs_to_different_course() {
      // ===== ARRANGE =====
      CourseLesson lesson =
          buildCourseLesson(UUID.fromString("99999999-9999-9999-9999-999999999999"), "video/path.mp4", true);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(lesson));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID));
      assertEquals(ErrorCode.COURSE_LESSON_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verifyNoMoreInteractions(courseLessonRepository, enrollmentRepository, courseRepository);
    }

    @Test
    void it_should_throw_access_denied_when_lesson_is_not_free_preview_and_user_has_no_access() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
      CourseLesson lesson = buildCourseLesson(COURSE_ID, "videos/bai-001.mp4", false);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(lesson));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(OTHER_USER_ID, COURSE_ID))
          .thenReturn(Optional.empty());
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID));
      assertEquals(ErrorCode.COURSE_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(OTHER_USER_ID, COURSE_ID);
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verifyNoMoreInteractions(courseLessonRepository, enrollmentRepository, courseRepository);
    }

    @Test
    void it_should_lookup_course_when_enrollment_status_is_not_active() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);

      CourseLesson lesson = buildCourseLesson(COURSE_ID, "videos/bai-002.mp4", false);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(lesson));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(OTHER_USER_ID, COURSE_ID))
          .thenReturn(Optional.of(buildEnrollment(EnrollmentStatus.PENDING)));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID));
      assertEquals(ErrorCode.COURSE_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(OTHER_USER_ID, COURSE_ID);
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verifyNoMoreInteractions(courseLessonRepository, enrollmentRepository, courseRepository);
    }

    @Test
    void it_should_return_presigned_url_when_lesson_is_free_preview() throws Exception {
      // ===== ARRANGE =====
      when(minioProperties.getPublicEndpoint()).thenReturn("http://cdn.mathmaster.local:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      CourseLesson lesson = buildCourseLesson(COURSE_ID, "videos/bai-free.mp4", true);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(lesson));

      S3Presigner presigner = Mockito.mock(S3Presigner.class);
      Builder builder = Mockito.mock(Builder.class);
      PresignedGetObjectRequest presigned = Mockito.mock(PresignedGetObjectRequest.class);
      when(builder.endpointOverride(any(URI.class))).thenReturn(builder);
      when(builder.credentialsProvider(any())).thenReturn(builder);
      when(builder.region(any())).thenReturn(builder);
      when(builder.serviceConfiguration(any())).thenReturn(builder);
      when(builder.build()).thenReturn(presigner);
      when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
      when(presigned.url()).thenReturn(java.net.URL.of(URI.create("http://cdn.mathmaster.local:9000/course-videos/videos/bai-free.mp4"), null));

      // ===== ACT =====
      String url;
      try (MockedStatic<S3Presigner> presignerStatic = Mockito.mockStatic(S3Presigner.class)) {
        presignerStatic.when(S3Presigner::builder).thenReturn(builder);
        url = videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID);
      }

      // ===== ASSERT =====
      assertTrue(url.contains("bai-free.mp4"));

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verifyNoMoreInteractions(courseLessonRepository, enrollmentRepository, courseRepository);
    }

    @Test
    void it_should_return_presigned_url_when_user_has_active_enrollment_for_non_free_preview() throws Exception {
      // ===== ARRANGE =====
      when(minioProperties.getPublicEndpoint()).thenReturn("http://cdn.mathmaster.local:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      CourseLesson lesson = buildCourseLesson(COURSE_ID, "videos/bai-paid.mp4", false);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(lesson));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(OTHER_USER_ID, COURSE_ID))
          .thenReturn(Optional.of(buildEnrollment(EnrollmentStatus.ACTIVE)));

      S3Presigner presigner = Mockito.mock(S3Presigner.class);
      Builder builder = Mockito.mock(Builder.class);
      PresignedGetObjectRequest presigned = Mockito.mock(PresignedGetObjectRequest.class);
      when(builder.endpointOverride(any(URI.class))).thenReturn(builder);
      when(builder.credentialsProvider(any())).thenReturn(builder);
      when(builder.region(any())).thenReturn(builder);
      when(builder.serviceConfiguration(any())).thenReturn(builder);
      when(builder.build()).thenReturn(presigner);
      when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
      when(presigned.url()).thenReturn(java.net.URL.of(URI.create("http://cdn.mathmaster.local:9000/course-videos/videos/bai-paid.mp4"), null));

      // ===== ACT =====
      String url;
      try (MockedStatic<S3Presigner> presignerStatic = Mockito.mockStatic(S3Presigner.class)) {
        presignerStatic.when(S3Presigner::builder).thenReturn(builder);
        url = videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID);
      }

      // ===== ASSERT =====
      assertTrue(url.contains("bai-paid.mp4"));
      verify(courseRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void it_should_return_uncategorized_exception_when_presigner_generation_fails() {
      // ===== ARRANGE =====
      when(minioProperties.getPublicEndpoint()).thenReturn("::bad-uri::");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      CourseLesson lesson = buildCourseLesson(COURSE_ID, "videos/bai-broken.mp4", true);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(lesson));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class,
              () -> videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID));
      assertEquals(ErrorCode.UNCATEGORIZED_EXCEPTION, exception.getErrorCode());
    }

    @Test
    void it_should_return_presigned_url_when_user_is_admin_even_without_enrollment() throws Exception {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
      when(minioProperties.getPublicEndpoint()).thenReturn("http://cdn.mathmaster.local:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      CourseLesson lesson = buildCourseLesson(COURSE_ID, "videos/bai-admin.mp4", false);
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID)).thenReturn(Optional.of(lesson));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(OTHER_USER_ID, COURSE_ID))
          .thenReturn(Optional.empty());
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      S3Presigner presigner = Mockito.mock(S3Presigner.class);
      Builder builder = Mockito.mock(Builder.class);
      PresignedGetObjectRequest presigned = Mockito.mock(PresignedGetObjectRequest.class);
      when(builder.endpointOverride(any(URI.class))).thenReturn(builder);
      when(builder.credentialsProvider(any())).thenReturn(builder);
      when(builder.region(any())).thenReturn(builder);
      when(builder.serviceConfiguration(any())).thenReturn(builder);
      when(builder.build()).thenReturn(presigner);
      when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
      when(presigned.url()).thenReturn(java.net.URL.of(URI.create("http://cdn.mathmaster.local:9000/course-videos/videos/bai-admin.mp4"), null));

      // ===== ACT =====
      String url;
      try (MockedStatic<S3Presigner> presignerStatic = Mockito.mockStatic(S3Presigner.class)) {
        presignerStatic.when(S3Presigner::builder).thenReturn(builder);
        url = videoUploadService.getVideoPresignedUrl(COURSE_ID, COURSE_LESSON_ID, OTHER_USER_ID);
      }

      // ===== ASSERT =====
      assertTrue(url.contains("bai-admin.mp4"));
    }
  }

  @Nested
  @DisplayName("multipart steps")
  class MultipartStepTests {

    @Test
    void it_should_initiate_upload_and_return_upload_id_and_object_key_when_request_is_valid() throws Exception {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));
      when(minioClient.bucketExists(any())).thenReturn(true);

      S3Client s3Client = Mockito.mock(S3Client.class);
      S3ClientBuilder s3Builder = Mockito.mock(S3ClientBuilder.class);
      when(s3Builder.endpointOverride(any(URI.class))).thenReturn(s3Builder);
      when(s3Builder.credentialsProvider(any())).thenReturn(s3Builder);
      when(s3Builder.region(any())).thenReturn(s3Builder);
      when(s3Builder.forcePathStyle(true)).thenReturn(s3Builder);
      when(s3Builder.build()).thenReturn(s3Client);
      when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
          .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-abc-001").build());

      var request = com.fptu.math_master.dto.request.InitiateUploadRequest.builder()
          .fileName("bai-giang.mp4")
          .contentType("video/mp4")
          .build();

      // ===== ACT =====
      com.fptu.math_master.dto.response.InitiateUploadResponse response;
      try (MockedStatic<S3Client> s3ClientStatic = Mockito.mockStatic(S3Client.class)) {
        s3ClientStatic.when(S3Client::builder).thenReturn(s3Builder);
        response = videoUploadService.initiateUpload(COURSE_ID, request);
      }

      // ===== ASSERT =====
      assertEquals("upload-abc-001", response.getUploadId());
      assertTrue(response.getObjectKey().startsWith(COURSE_ID + "/"));
      assertTrue(response.getObjectKey().endsWith(".mp4"));
    }

    @Test
    void it_should_return_presigned_url_for_part_upload_when_request_is_valid() throws Exception {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(minioProperties.getPublicEndpoint()).thenReturn("http://cdn.mathmaster.local:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      S3Presigner presigner = Mockito.mock(S3Presigner.class);
      Builder builder = Mockito.mock(Builder.class);
      PresignedUploadPartRequest presigned = Mockito.mock(PresignedUploadPartRequest.class);
      when(builder.endpointOverride(any(URI.class))).thenReturn(builder);
      when(builder.credentialsProvider(any())).thenReturn(builder);
      when(builder.region(any())).thenReturn(builder);
      when(builder.serviceConfiguration(any())).thenReturn(builder);
      when(builder.build()).thenReturn(presigner);
      when(presigner.presignUploadPart(any(UploadPartPresignRequest.class))).thenReturn(presigned);
      when(presigned.url()).thenReturn(java.net.URL.of(URI.create("http://cdn.mathmaster.local:9000/upload-part-url"), null));

      // ===== ACT =====
      var response =
          (com.fptu.math_master.dto.response.PartUploadUrlResponse)
              null;
      try (MockedStatic<S3Presigner> presignerStatic = Mockito.mockStatic(S3Presigner.class)) {
        presignerStatic.when(S3Presigner::builder).thenReturn(builder);
        response = videoUploadService.getPartUploadUrl(COURSE_ID, "upload-001", "course/video.mp4", 3);
      }

      // ===== ASSERT =====
      assertEquals(3, response.getPartNumber());
      assertTrue(response.getPresignedUrl().contains("upload-part-url"));
    }

    @Test
    void it_should_upload_part_via_backend_and_strip_quotes_from_etag() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      S3Client s3Client = Mockito.mock(S3Client.class);
      S3ClientBuilder s3Builder = Mockito.mock(S3ClientBuilder.class);
      when(s3Builder.endpointOverride(any(URI.class))).thenReturn(s3Builder);
      when(s3Builder.credentialsProvider(any())).thenReturn(s3Builder);
      when(s3Builder.region(any())).thenReturn(s3Builder);
      when(s3Builder.forcePathStyle(true)).thenReturn(s3Builder);
      when(s3Builder.build()).thenReturn(s3Client);
      when(s3Client.uploadPart(any(UploadPartRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
          .thenReturn(UploadPartResponse.builder().eTag("\"etag-quoted\"").build());

      byte[] chunk = new byte[] {1, 2, 3, 4};

      // ===== ACT =====
      com.fptu.math_master.dto.response.PartUploadUrlResponse response;
      try (MockedStatic<S3Client> s3ClientStatic = Mockito.mockStatic(S3Client.class)) {
        s3ClientStatic.when(S3Client::builder).thenReturn(s3Builder);
        response = videoUploadService.uploadPartViaBackend(COURSE_ID, "upload-002", "course/video.mp4", 2, chunk);
      }

      // ===== ASSERT =====
      assertEquals(2, response.getPartNumber());
      assertEquals("etag-quoted", response.getETag());
      assertEquals(null, response.getPresignedUrl());
    }

    @Test
    void it_should_keep_etag_null_when_backend_upload_returns_no_etag() {
      // ===== ARRANGE =====
      securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
      securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
      when(minioProperties.getEndpoint()).thenReturn("http://localhost:9000");
      when(minioProperties.getAccessKey()).thenReturn("minioadmin");
      when(minioProperties.getSecretKey()).thenReturn("minioadmin");
      when(minioProperties.getCourseVideosBucket()).thenReturn("course-videos");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(TEACHER_ID, CourseProvider.CUSTOM)));

      S3Client s3Client = Mockito.mock(S3Client.class);
      S3ClientBuilder s3Builder = Mockito.mock(S3ClientBuilder.class);
      when(s3Builder.endpointOverride(any(URI.class))).thenReturn(s3Builder);
      when(s3Builder.credentialsProvider(any())).thenReturn(s3Builder);
      when(s3Builder.region(any())).thenReturn(s3Builder);
      when(s3Builder.forcePathStyle(true)).thenReturn(s3Builder);
      when(s3Builder.build()).thenReturn(s3Client);
      when(s3Client.uploadPart(any(UploadPartRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
          .thenReturn(UploadPartResponse.builder().eTag(null).build());

      // ===== ACT =====
      com.fptu.math_master.dto.response.PartUploadUrlResponse response;
      try (MockedStatic<S3Client> s3ClientStatic = Mockito.mockStatic(S3Client.class)) {
        s3ClientStatic.when(S3Client::builder).thenReturn(s3Builder);
        response = videoUploadService.uploadPartViaBackend(COURSE_ID, "upload-003", "course/video.mp4", 4, new byte[] {1, 2});
      }

      // ===== ASSERT =====
      assertEquals(4, response.getPartNumber());
      assertEquals(null, response.getETag());
    }
  }
}
