package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.request.CreateCourseLessonRequest;
import com.fptu.math_master.dto.request.ReorderLessonsRequest;
import com.fptu.math_master.dto.request.UpdateCourseLessonRequest;
import com.fptu.math_master.dto.response.CourseLessonResponse;
import com.fptu.math_master.dto.response.MaterialItem;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.entity.CustomCourseSection;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.enums.CourseProvider;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.CustomCourseSectionRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.service.CourseLessonService;
import com.fptu.math_master.service.CourseService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("CourseLessonServiceImpl - Tests")
class CourseLessonServiceImplTest extends BaseUnitTest {

  @InjectMocks private CourseLessonServiceImpl courseLessonService;

  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private CourseRepository courseRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private CustomCourseSectionRepository customCourseSectionRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private UploadService uploadService;
  @Mock private ObjectMapper objectMapper;
  @Mock private MinioProperties minioProperties;
  @Mock private CourseService courseService;
  @Mock private MultipartFile multipartFile;

  private static final UUID TEACHER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ADMIN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OTHER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID COURSE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID COURSE_ID_2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID LESSON_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID COURSE_LESSON_ID =
      UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
  private static final UUID COURSE_LESSON_ID_2 =
      UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
  private static final UUID SECTION_ID = UUID.fromString("abababab-abab-abab-abab-abababababab");
  private static final UUID SECTION_ID_2 = UUID.fromString("cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcdcd");
  private static final UUID CHAPTER_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");

  @BeforeEach
  void setUp() throws Exception {
    lenient()
        .when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
        .thenReturn(new java.util.ArrayList<MaterialItem>());
    lenient().when(objectMapper.writeValueAsString(any(List.class))).thenReturn("[]");
  }

  private Course buildCourse(UUID id, UUID teacherId, CourseProvider provider) {
    Course course = new Course();
    course.setId(id);
    course.setTeacherId(teacherId);
    course.setProvider(provider);
    course.setTitle("Đại số tuyến tính ứng dụng");
    return course;
  }

  private Lesson buildLesson(UUID id, String title) {
    Lesson lesson = new Lesson();
    lesson.setId(id);
    lesson.setTitle(title);
    lesson.setChapterId(CHAPTER_ID);
    lesson.setLessonContent("Nội dung bài học");
    return lesson;
  }

  private CourseLesson buildCourseLesson(UUID id, UUID courseId) {
    CourseLesson cl = new CourseLesson();
    cl.setId(id);
    cl.setCourseId(courseId);
    cl.setOrderIndex(2);
    cl.setVideoTitle("Bài giảng hình học không gian");
    cl.setVideoUrl("course-videos/old-video.mp4");
    cl.setCustomTitle("Tiêu đề tuỳ chỉnh");
    cl.setMaterials("[]");
    cl.setFreePreview(false);
    return cl;
  }

  private CustomCourseSection buildSection(UUID id, UUID courseId) {
    CustomCourseSection section = new CustomCourseSection();
    section.setId(id);
    section.setCourseId(courseId);
    section.setTitle("Chương mở đầu tự biên soạn");
    section.setOrderIndex(1);
    return section;
  }

  private Enrollment buildEnrollment(EnrollmentStatus status) {
    Enrollment enrollment = new Enrollment();
    enrollment.setId(UUID.randomUUID());
    enrollment.setStudentId(STUDENT_ID);
    enrollment.setCourseId(COURSE_ID);
    enrollment.setStatus(status);
    return enrollment;
  }

  @Nested
  @DisplayName("addLesson()")
  class AddLessonTests {

    /**
     * Normal case: Tạo lesson cho MINISTRY course thành công khi có lessonId hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>course provider: MINISTRY</li>
     *   <li>lessonId: LESSON_ID</li>
     *   <li>videoFile: non-empty</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>videoFile != null && !isEmpty() → TRUE branch</li>
     *   <li>provider == MINISTRY → TRUE branch</li>
     *   <li>lessonId == null check → FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Lesson được lưu và trả về title từ Lesson entity</li>
     *   <li>{@code syncCourseMetrics(courseId)} được gọi đúng 1 lần</li>
     * </ul>
     */
    @Test
    void it_should_add_ministry_lesson_when_input_is_valid() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      Lesson lesson = buildLesson(LESSON_ID, "Bài 1: Hệ phương trình tuyến tính");
      CreateCourseLessonRequest request =
          CreateCourseLessonRequest.builder()
              .lessonId(LESSON_ID)
              .videoTitle("Video hệ tuyến tính")
              .orderIndex(1)
              .materials("[]")
              .isFreePreview(true)
              .build();
      CourseLesson saved = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      saved.setLessonId(LESSON_ID);
      saved.setVideoUrl("course-videos/new-video.mp4");
      saved.setOrderIndex(1);
      saved.setMaterials("[]");
      saved.setFreePreview(true);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(lesson));
      when(multipartFile.isEmpty()).thenReturn(false);
      when(uploadService.uploadFile(multipartFile, "course-videos", "videos-bucket"))
          .thenReturn("course-videos/new-video.mp4");
      when(minioProperties.getCourseVideosBucket()).thenReturn("videos-bucket");
      when(courseLessonRepository.save(any(CourseLesson.class))).thenReturn(saved);

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.addLesson(COURSE_ID, request, multipartFile);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(COURSE_LESSON_ID, result.getId()),
          () -> assertEquals("Bài 1: Hệ phương trình tuyến tính", result.getLessonTitle()),
          () -> assertEquals("course-videos/new-video.mp4", result.getVideoUrl()),
          () -> assertTrue(result.isFreePreview()));

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(lessonRepository, times(2)).findByIdAndNotDeleted(LESSON_ID);
      verify(uploadService, times(1))
          .uploadFile(multipartFile, "course-videos", "videos-bucket");
      verify(courseLessonRepository, times(1)).save(any(CourseLesson.class));
      verify(courseService, times(1)).syncCourseMetrics(COURSE_ID);
      verifyNoMoreInteractions(
          courseRepository,
          lessonRepository,
          uploadService,
          courseLessonRepository,
          customCourseSectionRepository,
          courseService);
    }

    /**
     * Abnormal case: MINISTRY course thiếu lessonId.
     *
     * <p>Input:
     * <ul>
     *   <li>course provider: MINISTRY</li>
     *   <li>lessonId: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>provider == MINISTRY → TRUE branch</li>
     *   <li>lessonId == null → TRUE branch (throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code
     *       {@code LESSON_ID_REQUIRED_FOR_MINISTRY_COURSE}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_ministry_course_has_null_lesson_id() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CreateCourseLessonRequest request =
          CreateCourseLessonRequest.builder()
              .videoTitle("Video giải tích")
              .orderIndex(1)
              .build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                AppException.class, () -> courseLessonService.addLesson(COURSE_ID, request, null));
      }
      assertEquals(ErrorCode.LESSON_ID_REQUIRED_FOR_MINISTRY_COURSE, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(courseLessonRepository, never()).save(any(CourseLesson.class));
      verifyNoMoreInteractions(courseRepository, courseLessonRepository, lessonRepository, courseService);
    }

    /**
     * Normal case: Tạo lesson cho CUSTOM course thành công khi section thuộc đúng course.
     *
     * <p>Input:
     * <ul>
     *   <li>course provider: CUSTOM</li>
     *   <li>sectionId: SECTION_ID</li>
     *   <li>customTitle: "Ôn tập trước kỳ thi"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>provider == MINISTRY → FALSE branch (đi vào CUSTOM)</li>
     *   <li>sectionId == null check → FALSE branch</li>
     *   <li>customTitle hasText check → TRUE path cho valid input</li>
     *   <li>section.courseId.equals(courseId) → TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Lesson custom được lưu và phản hồi lessonTitle bằng customTitle</li>
     * </ul>
     */
    @Test
    void it_should_add_custom_lesson_when_section_belongs_to_course() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CreateCourseLessonRequest request =
          CreateCourseLessonRequest.builder()
              .sectionId(SECTION_ID)
              .customTitle("Ôn tập trước kỳ thi")
              .customDescription("Lộ trình ôn tập 7 ngày")
              .orderIndex(1)
              .videoTitle("Video ôn tập")
              .build();
      CustomCourseSection section = buildSection(SECTION_ID, COURSE_ID);
      CourseLesson saved = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      saved.setSectionId(SECTION_ID);
      saved.setCustomTitle("Ôn tập trước kỳ thi");
      saved.setVideoUrl(null);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(customCourseSectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID))
          .thenReturn(Optional.of(section));
      when(courseLessonRepository.save(any(CourseLesson.class))).thenReturn(saved);

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.addLesson(COURSE_ID, request, null);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals("Ôn tập trước kỳ thi", result.getLessonTitle()),
          () -> assertEquals(SECTION_ID, result.getSectionId()),
          () -> assertNull(result.getVideoUrl()));

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(customCourseSectionRepository, times(1)).findByIdAndDeletedAtIsNull(SECTION_ID);
      verify(courseLessonRepository, times(1)).save(any(CourseLesson.class));
      verify(courseService, times(1)).syncCourseMetrics(COURSE_ID);
      verifyNoMoreInteractions(
          courseRepository, customCourseSectionRepository, courseLessonRepository, courseService);
    }

    /**
     * Abnormal case: CUSTOM course có section không thuộc về course đang thao tác.
     *
     * <p>Input:
     * <ul>
     *   <li>course provider: CUSTOM</li>
     *   <li>section.courseId: COURSE_ID_2 (khác COURSE_ID)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>provider == MINISTRY → FALSE branch</li>
     *   <li>section.courseId.equals(courseId) → FALSE branch (throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code SECTION_NOT_IN_COURSE}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_custom_section_not_in_target_course() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CreateCourseLessonRequest request =
          CreateCourseLessonRequest.builder()
              .sectionId(SECTION_ID)
              .customTitle("Bài học bị sai section")
              .build();
      CustomCourseSection section = buildSection(SECTION_ID, COURSE_ID_2);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(customCourseSectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID))
          .thenReturn(Optional.of(section));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                AppException.class, () -> courseLessonService.addLesson(COURSE_ID, request, null));
      }
      assertEquals(ErrorCode.SECTION_NOT_IN_COURSE, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(customCourseSectionRepository, times(1)).findByIdAndDeletedAtIsNull(SECTION_ID);
      verify(courseLessonRepository, never()).save(any(CourseLesson.class));
      verifyNoMoreInteractions(courseRepository, customCourseSectionRepository, courseLessonRepository);
    }

    /**
     * Abnormal case: Người gọi không phải chủ course.
     *
     * <p>Input:
     * <ul>
     *   <li>currentUserId: OTHER_ID</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>verifyOwnership() teacherId.equals(userId) → FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code COURSE_ACCESS_DENIED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_add_lesson_called_by_non_owner() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CreateCourseLessonRequest request = CreateCourseLessonRequest.builder().lessonId(LESSON_ID).build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(OTHER_ID);
        exception =
            assertThrows(
                AppException.class, () -> courseLessonService.addLesson(COURSE_ID, request, null));
      }
      assertEquals(ErrorCode.COURSE_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseLessonRepository, never()).save(any(CourseLesson.class));
    }
  }

  @Nested
  @DisplayName("updateLesson()")
  class UpdateLessonTests {

    /**
     * Normal case: Cập nhật video mới và metadata khi request hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>videoFile: non-empty</li>
     *   <li>existing videoUrl: non-blank</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>videoFile != null && !isEmpty() → TRUE branch</li>
     *   <li>existing videoUrl hasText → TRUE branch (delete old file)</li>
     *   <li>request field null-checks → TRUE branch for all updatable fields</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Old video bị xoá, video mới được upload và lesson được save</li>
     * </ul>
     */
    @Test
    void it_should_replace_video_and_update_metadata_when_video_file_is_provided() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setLessonId(LESSON_ID);
      UpdateCourseLessonRequest request =
          UpdateCourseLessonRequest.builder()
              .videoTitle("Video mới cho bài đại số")
              .orderIndex(7)
              .isFreePreview(true)
              .materials("[{\"id\":\"m1\",\"name\":\"Phiếu bài tập\"}]")
              .build();
      Lesson ministryLesson = buildLesson(LESSON_ID, "Bài đại số nâng cao");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(multipartFile.isEmpty()).thenReturn(false);
      when(minioProperties.getCourseVideosBucket()).thenReturn("videos-bucket");
      when(uploadService.uploadFile(multipartFile, "course-videos", "videos-bucket"))
          .thenReturn("course-videos/new-path.mp4");
      when(courseLessonRepository.save(lesson)).thenReturn(lesson);
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(ministryLesson));

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.updateLesson(COURSE_ID, COURSE_LESSON_ID, request, multipartFile);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals("course-videos/new-path.mp4", result.getVideoUrl()),
          () -> assertEquals("Video mới cho bài đại số", result.getVideoTitle()),
          () -> assertEquals(7, result.getOrderIndex()),
          () -> assertTrue(result.isFreePreview()),
          () -> assertEquals("Bài đại số nâng cao", result.getLessonTitle()));

      // ===== VERIFY =====
      verify(uploadService, times(1)).deleteFile("course-videos/old-video.mp4", "videos-bucket");
      verify(uploadService, times(1)).uploadFile(multipartFile, "course-videos", "videos-bucket");
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(courseLessonRepository, times(1)).save(lesson);
      verify(courseService, times(1)).syncCourseMetrics(COURSE_ID);
      verify(lessonRepository, times(2)).findByIdAndNotDeleted(LESSON_ID);
      verifyNoMoreInteractions(uploadService, courseService, lessonRepository);
    }

    /**
     * Abnormal case: lesson tồn tại nhưng thuộc course khác.
     *
     * <p>Input:
     * <ul>
     *   <li>courseLesson.courseId: COURSE_ID_2</li>
     *   <li>courseId request: COURSE_ID</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>courseLesson.getCourseId().equals(courseId) → FALSE branch (throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code COURSE_LESSON_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_course_lesson_does_not_belong_to_course() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID_2);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                AppException.class,
                () ->
                    courseLessonService.updateLesson(
                        COURSE_ID, COURSE_LESSON_ID, UpdateCourseLessonRequest.builder().build(), null));
      }
      assertEquals(ErrorCode.COURSE_LESSON_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(courseLessonRepository, never()).save(any(CourseLesson.class));
      verifyNoMoreInteractions(courseRepository, courseLessonRepository, uploadService, courseService);
    }

    /**
     * Normal case: Cập nhật metadata khi không có file video mới.
     *
     * <p>Input:
     * <ul>
     *   <li>videoFile: null</li>
     *   <li>lessonId: null (custom lesson)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>videoFile != null && !isEmpty() → FALSE branch</li>
     *   <li>courseLesson.lessonId != null → FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Không gọi upload/delete video, lessonTitle giữ từ customTitle</li>
     * </ul>
     */
    @Test
    void it_should_update_without_video_upload_when_video_file_is_null() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setLessonId(null);
      lesson.setCustomTitle("Tiêu đề custom ban đầu");
      UpdateCourseLessonRequest request = UpdateCourseLessonRequest.builder().orderIndex(4).build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(courseLessonRepository.save(lesson)).thenReturn(lesson);

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.updateLesson(COURSE_ID, COURSE_LESSON_ID, request, null);
      }

      // ===== ASSERT =====
      assertEquals("Tiêu đề custom ban đầu", result.getLessonTitle());
      assertEquals(4, result.getOrderIndex());

      // ===== VERIFY =====
      verify(uploadService, never()).deleteFile(any(String.class), any(String.class));
      verify(uploadService, never()).uploadFile(any(MultipartFile.class), any(String.class), any(String.class));
    }

    /**
     * Normal case: Upload video mới khi video cũ rỗng thì không cần delete file cũ.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>videoFile != null && !isEmpty() → TRUE</li>
     *   <li>hasText(existingVideoUrl) → FALSE</li>
     * </ul>
     */
    @Test
    void it_should_upload_new_video_without_deleting_old_when_existing_video_url_is_blank() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setVideoUrl(" ");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(multipartFile.isEmpty()).thenReturn(false);
      when(minioProperties.getCourseVideosBucket()).thenReturn("videos-bucket");
      when(uploadService.uploadFile(multipartFile, "course-videos", "videos-bucket"))
          .thenReturn("course-videos/new-v2.mp4");
      when(courseLessonRepository.save(lesson)).thenReturn(lesson);

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        courseLessonService.updateLesson(
            COURSE_ID, COURSE_LESSON_ID, UpdateCourseLessonRequest.builder().build(), multipartFile);
      }

      // ===== VERIFY =====
      verify(uploadService, never()).deleteFile(any(String.class), any(String.class));
      verify(uploadService, times(1)).uploadFile(multipartFile, "course-videos", "videos-bucket");
    }

    /**
     * Normal case: Lesson có lessonId nhưng không tìm thấy Lesson entity thì fallback customTitle.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>lessonRepository.findByIdAndNotDeleted(...).orElse(lessonTitle) → else branch</li>
     * </ul>
     */
    @Test
    void it_should_fallback_to_custom_title_when_ministry_lesson_lookup_is_empty_on_update() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setLessonId(LESSON_ID);
      lesson.setCustomTitle("Fallback custom title");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(courseLessonRepository.save(lesson)).thenReturn(lesson);
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.empty());

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result =
            courseLessonService.updateLesson(
                COURSE_ID, COURSE_LESSON_ID, UpdateCourseLessonRequest.builder().build(), null);
      }

      // ===== ASSERT =====
      assertEquals("Fallback custom title", result.getLessonTitle());
    }
  }

  @Nested
  @DisplayName("deleteLesson()")
  class DeleteLessonTests {

    /**
     * Normal case: Xoá mềm lesson CUSTOM và reorder các lesson cùng section phía sau.
     *
     * <p>Input:
     * <ul>
     *   <li>provider: CUSTOM</li>
     *   <li>deleted lesson orderIndex: 2</li>
     *   <li>remaining lessons: cùng section + khác section</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>videoUrl hasText → TRUE branch (delete video)</li>
     *   <li>material key hasText → TRUE branch (delete material)</li>
     *   <li>CUSTOM reorder filter → TRUE/FALSE branch theo section/order</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Chỉ lesson cùng section và orderIndex lớn hơn mới bị giảm 1</li>
     * </ul>
     */
    @Test
    void it_should_soft_delete_and_reorder_custom_lessons_in_same_section() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson deleting = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      deleting.setSectionId(SECTION_ID);
      deleting.setOrderIndex(2);
      deleting.setVideoUrl("course-videos/lesson-2.mp4");
      deleting.setMaterials("[{\"id\":\"m1\",\"name\":\"Tài liệu\",\"key\":\"course-materials/doc-1.pdf\"}]");
      CourseLesson shouldReorder = buildCourseLesson(COURSE_LESSON_ID_2, COURSE_ID);
      shouldReorder.setSectionId(SECTION_ID);
      shouldReorder.setOrderIndex(3);
      CourseLesson otherSection = buildCourseLesson(UUID.randomUUID(), COURSE_ID);
      otherSection.setSectionId(SECTION_ID_2);
      otherSection.setOrderIndex(5);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(deleting));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(shouldReorder, otherSection));
      when(minioProperties.getCourseVideosBucket()).thenReturn("videos-bucket");
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(objectMapper.readValue(eq(deleting.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(
              List.of(
                  MaterialItem.builder()
                      .id("m1")
                      .name("Tài liệu")
                      .key("course-materials/doc-1.pdf")
                      .build()));

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        courseLessonService.deleteLesson(COURSE_ID, COURSE_LESSON_ID);
      }

      // ===== ASSERT =====
      assertEquals(2, shouldReorder.getOrderIndex());
      assertEquals(5, otherSection.getOrderIndex());
      assertNotNull(deleting.getDeletedAt());
      assertEquals(TEACHER_ID, deleting.getDeletedBy());

      // ===== VERIFY =====
      verify(uploadService, times(1)).deleteFile("course-videos/lesson-2.mp4", "videos-bucket");
      verify(uploadService, times(1))
          .deleteFile("course-materials/doc-1.pdf", "materials-bucket");
      verify(courseLessonRepository, times(1)).save(deleting);
      verify(courseLessonRepository, times(1)).save(shouldReorder);
      verify(courseLessonRepository, never()).save(otherSection);
      verify(courseService, times(1)).syncCourseMetrics(COURSE_ID);
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(courseLessonRepository, times(1)).findByCourseIdAndNotDeleted(COURSE_ID);
      verifyNoMoreInteractions(uploadService, courseService);
    }

    /**
     * Normal case: Xoá lesson MINISTRY chỉ reorder theo orderIndex toàn course.
     *
     * <p>Input:
     * <ul>
     *   <li>provider: MINISTRY</li>
     *   <li>videoUrl: blank, materials: []</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>provider == CUSTOM check trong filter → FALSE branch</li>
     *   <li>videoUrl hasText → FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Không gọi delete video/material, chỉ reorder lesson phía sau</li>
     * </ul>
     */
    @Test
    void it_should_reorder_ministry_lessons_without_resource_cleanup_when_no_files_exist() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson deleting = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      deleting.setOrderIndex(1);
      deleting.setVideoUrl(" ");
      deleting.setMaterials("[]");
      CourseLesson shouldReorder = buildCourseLesson(COURSE_LESSON_ID_2, COURSE_ID);
      shouldReorder.setOrderIndex(2);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(deleting));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(shouldReorder));

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        courseLessonService.deleteLesson(COURSE_ID, COURSE_LESSON_ID);
      }

      // ===== ASSERT =====
      assertEquals(1, shouldReorder.getOrderIndex());

      // ===== VERIFY =====
      verify(uploadService, never()).deleteFile(any(String.class), any(String.class));
      verify(courseLessonRepository, times(1)).save(shouldReorder);
    }
  }

  @Nested
  @DisplayName("reorderLessons()")
  class ReorderLessonsTests {

    /**
     * Abnormal case: Danh sách reorder chứa duplicate orderIndex.
     *
     * <p>Input:
     * <ul>
     *   <li>orders: 2 lesson có cùng orderIndex = 1</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>orderIndexSet.add(orderIndex) → FALSE branch (duplicate detected)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code DUPLICATE_ORDER_INDEX}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_reorder_request_has_duplicate_order_indexes() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      ReorderLessonsRequest request =
          ReorderLessonsRequest.builder()
              .orders(
                  List.of(
                      new ReorderLessonsRequest.LessonOrder(COURSE_LESSON_ID, 1),
                      new ReorderLessonsRequest.LessonOrder(COURSE_LESSON_ID_2, 1)))
              .build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                AppException.class, () -> courseLessonService.reorderLessons(COURSE_ID, request));
      }
      assertEquals(ErrorCode.DUPLICATE_ORDER_INDEX, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_ID);
      verify(courseLessonRepository, never()).findByIdAndDeletedAtIsNull(any(UUID.class));
      verifyNoMoreInteractions(courseRepository, courseLessonRepository);
    }

    /**
     * Normal case: Batch reorder thành công khi lesson đều thuộc cùng course.
     *
     * <p>Input:
     * <ul>
     *   <li>orders: 2 items với orderIndex khác nhau</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>duplicate check → FALSE branch (không duplicate)</li>
     *   <li>lesson.courseId.equals(courseId) → TRUE branch cho tất cả item</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Tất cả lesson trong request được cập nhật orderIndex và lưu</li>
     * </ul>
     */
    @Test
    void it_should_reorder_lessons_when_request_is_valid() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      ReorderLessonsRequest request =
          ReorderLessonsRequest.builder()
              .orders(
                  List.of(
                      new ReorderLessonsRequest.LessonOrder(COURSE_LESSON_ID, 1),
                      new ReorderLessonsRequest.LessonOrder(COURSE_LESSON_ID_2, 2)))
              .build();
      CourseLesson first = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      CourseLesson second = buildCourseLesson(COURSE_LESSON_ID_2, COURSE_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(first));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID_2))
          .thenReturn(Optional.of(second));

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        courseLessonService.reorderLessons(COURSE_ID, request);
      }

      // ===== ASSERT =====
      assertEquals(1, first.getOrderIndex());
      assertEquals(2, second.getOrderIndex());

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID_2);
      verify(courseLessonRepository, times(1)).save(first);
      verify(courseLessonRepository, times(1)).save(second);
    }

    /**
     * Abnormal case: Một lesson trong danh sách reorder không thuộc course.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>lesson.courseId.equals(courseId) → FALSE branch</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_reorder_contains_lesson_from_other_course() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      ReorderLessonsRequest request =
          ReorderLessonsRequest.builder()
              .orders(List.of(new ReorderLessonsRequest.LessonOrder(COURSE_LESSON_ID, 3)))
              .build();
      CourseLesson foreignLesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID_2);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(foreignLesson));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                AppException.class, () -> courseLessonService.reorderLessons(COURSE_ID, request));
      }
      assertEquals(ErrorCode.COURSE_LESSON_NOT_FOUND, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("getAdminVideoUrl()")
  class GetAdminVideoUrlTests {

    /**
     * Abnormal case: Lesson không có video URL.
     *
     * <p>Input:
     * <ul>
     *   <li>courseLesson.videoUrl: blank</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>hasText(videoUrl) → FALSE branch (throw VIDEO_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code VIDEO_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_admin_requests_video_url_but_video_is_missing() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setVideoUrl("   ");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(ADMIN_ID);
        exception =
            assertThrows(
                AppException.class, () -> courseLessonService.getAdminVideoUrl(COURSE_ID, COURSE_LESSON_ID));
      }
      assertEquals(ErrorCode.VIDEO_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(uploadService, never()).getPresignedUrl(any(String.class), any(String.class));
      verifyNoMoreInteractions(uploadService);
    }

    /**
     * Normal case: Admin lấy được presigned URL khi lesson có video.
     *
     * <p>Input:
     * <ul>
     *   <li>lesson.videoUrl: course-videos/a.mp4</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>courseLesson.courseId.equals(courseId) → TRUE branch</li>
     *   <li>videoUrl hasText → TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về presigned URL từ upload service</li>
     * </ul>
     */
    @Test
    void it_should_return_presigned_url_when_admin_video_exists() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setVideoUrl("course-videos/a.mp4");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(minioProperties.getCourseVideosBucket()).thenReturn("videos-bucket");
      when(uploadService.getPresignedUrl("course-videos/a.mp4", "videos-bucket"))
          .thenReturn("https://signed/video");

      // ===== ACT =====
      String result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(ADMIN_ID);
        result = courseLessonService.getAdminVideoUrl(COURSE_ID, COURSE_LESSON_ID);
      }

      // ===== ASSERT =====
      assertEquals("https://signed/video", result);
    }

    /**
     * Abnormal case: Lesson tồn tại nhưng không thuộc courseId truyền vào.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>courseLesson.courseId.equals(courseId) → FALSE branch</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_admin_video_request_targets_lesson_of_other_course() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID_2);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(ADMIN_ID);
        exception =
            assertThrows(
                AppException.class, () -> courseLessonService.getAdminVideoUrl(COURSE_ID, COURSE_LESSON_ID));
      }
      assertEquals(ErrorCode.COURSE_LESSON_NOT_FOUND, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("getLessons()")
  class GetLessonsTests {

    /**
     * Normal case: Admin truy cập lesson list và xem đầy đủ video/material.
     *
     * <p>Input:
     * <ul>
     *   <li>current user: ADMIN</li>
     *   <li>course lessons gồm ministry lesson có materials</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>currentUserId != null → TRUE branch</li>
     *   <li>isAdmin check → TRUE branch</li>
     *   <li>canAccessAll = true → material URL generation branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về lesson có videoUrl + materials đã presign và key bị ẩn</li>
     * </ul>
     */
    @Test
    void it_should_return_full_lesson_access_when_user_is_admin() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      cl.setLessonId(LESSON_ID);
      cl.setVideoUrl("course-videos/intro.mp4");
      cl.setMaterials("[{\"id\":\"m1\",\"name\":\"Slide\",\"key\":\"course-materials/slide.pdf\"}]");
      Lesson lesson = buildLesson(LESSON_ID, "Bài học tích phân cơ bản");
      Chapter chapter = new Chapter();
      chapter.setId(CHAPTER_ID);
      chapter.setTitle("Chương 1: Hàm số");
      lesson.setChapter(chapter);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl));
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(lesson));
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(uploadService.getPresignedUrl("course-materials/slide.pdf", "materials-bucket"))
          .thenReturn("https://signed/material");
      when(objectMapper.readValue(eq(cl.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(
              List.of(
                  MaterialItem.builder()
                      .id("m1")
                      .name("Slide")
                      .key("course-materials/slide.pdf")
                      .build()));
      when(objectMapper.writeValueAsString(any(List.class)))
          .thenReturn("[{\"id\":\"m1\",\"name\":\"Slide\",\"url\":\"https://signed/material\",\"key\":null}]");

      // ===== ACT =====
      List<CourseLessonResponse> result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(ADMIN_ID);
        security.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);
        result = courseLessonService.getLessons(COURSE_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals("Bài học tích phân cơ bản", result.get(0).getLessonTitle()),
          () -> assertEquals("course-videos/intro.mp4", result.get(0).getVideoUrl()),
          () -> assertNotNull(result.get(0).getMaterials()),
          () -> assertEquals("Chương 1: Hàm số", result.get(0).getChapterTitle()));

      // ===== VERIFY =====
      verify(enrollmentRepository, never())
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(any(UUID.class), any(UUID.class));
      verify(uploadService, times(1)).getPresignedUrl("course-materials/slide.pdf", "materials-bucket");
      verifyNoMoreInteractions(enrollmentRepository, uploadService);
    }

    /**
     * Abnormal-like access case: Người dùng chưa đăng nhập chỉ xem được free preview.
     *
     * <p>Input:
     * <ul>
     *   <li>currentUserId: null</li>
     *   <li>lesson.isFreePreview: false</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>currentUserId != null → FALSE branch</li>
     *   <li>canAccess = isAuthorized || isFreePreview → FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>videoUrl và material URL bị ẩn đối với lesson không free preview</li>
     * </ul>
     */
    @Test
    void it_should_hide_locked_content_when_user_is_unauthenticated_and_lesson_is_not_free_preview()
        throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      cl.setVideoUrl("course-videos/protected.mp4");
      cl.setFreePreview(false);
      cl.setMaterials("[{\"id\":\"m2\",\"name\":\"Đề cương\",\"key\":\"course-materials/outline.pdf\"}]");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl));
      when(objectMapper.readValue(eq(cl.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(
              List.of(
                  MaterialItem.builder()
                      .id("m2")
                      .name("Đề cương")
                      .key("course-materials/outline.pdf")
                      .build()));
      when(objectMapper.writeValueAsString(any(List.class)))
          .thenReturn("[{\"id\":\"m2\",\"name\":\"Đề cương\",\"key\":null,\"url\":null}]");

      // ===== ACT =====
      List<CourseLessonResponse> result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(null);
        result = courseLessonService.getLessons(COURSE_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertNull(result.get(0).getVideoUrl()),
          () -> assertEquals("[{\"id\":\"m2\",\"name\":\"Đề cương\",\"key\":null,\"url\":null}]", result.get(0).getMaterials()));

      // ===== VERIFY =====
      verify(uploadService, never()).getPresignedUrl(any(String.class), any(String.class));
      verifyNoMoreInteractions(uploadService, enrollmentRepository);
    }

    /**
     * Normal case: Student có ACTIVE enrollment được truy cập đầy đủ lesson.
     *
     * <p>Input:
     * <ul>
     *   <li>current user: STUDENT_ID</li>
     *   <li>enrollment status: ACTIVE</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isAdmin → FALSE branch</li>
     *   <li>isOwner → FALSE branch</li>
     *   <li>enrollment ACTIVE check → TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>videoUrl được giữ nguyên khi student đã enrolled ACTIVE</li>
     * </ul>
     */
    @Test
    void it_should_allow_full_access_when_student_has_active_enrollment() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      cl.setVideoUrl("course-videos/public-for-enrolled.mp4");
      cl.setFreePreview(false);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(buildEnrollment(EnrollmentStatus.ACTIVE)));

      // ===== ACT =====
      List<CourseLessonResponse> result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(STUDENT_ID);
        security.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
        result = courseLessonService.getLessons(COURSE_ID);
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals("course-videos/public-for-enrolled.mp4", result.get(0).getVideoUrl()));

      // ===== VERIFY =====
      verify(enrollmentRepository, times(1))
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID);
      verifyNoMoreInteractions(enrollmentRepository, uploadService);
    }

    /**
     * Normal case: Chủ course truy cập lesson không cần check enrollment.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isOwner → TRUE branch</li>
     * </ul>
     */
    @Test
    void it_should_allow_full_access_when_user_is_course_owner() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson cl = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      cl.setVideoUrl("course-videos/owner.mp4");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl));

      // ===== ACT =====
      List<CourseLessonResponse> result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(TEACHER_ID);
        security.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
        result = courseLessonService.getLessons(COURSE_ID);
      }

      // ===== ASSERT =====
      assertEquals("course-videos/owner.mp4", result.get(0).getVideoUrl());
      verify(enrollmentRepository, never())
          .findByStudentIdAndCourseIdAndDeletedAtIsNull(any(UUID.class), any(UUID.class));
    }
  }

  @Nested
  @DisplayName("addMaterial()")
  class AddMaterialTests {

    /**
     * Abnormal case: File upload rỗng hoặc null.
     *
     * <p>Input:
     * <ul>
     *   <li>file.isEmpty() = true</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>file == null || file.isEmpty() → TRUE branch (throw INVALID_REQUEST)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code INVALID_REQUEST}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_add_material_file_is_empty() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(multipartFile.isEmpty()).thenReturn(true);

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                AppException.class,
                () -> courseLessonService.addMaterial(COURSE_ID, COURSE_LESSON_ID, multipartFile));
      }
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());

      // ===== VERIFY =====
      verify(uploadService, never()).uploadFile(any(MultipartFile.class), any(String.class), any(String.class));
      verifyNoMoreInteractions(uploadService, courseService);
    }

    /**
     * Normal case: Upload material thành công và trả về response đã resolve title.
     *
     * <p>Input:
     * <ul>
     *   <li>lesson.lessonId: LESSON_ID</li>
     *   <li>file: valid</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>file == null || file.isEmpty() → FALSE branch</li>
     *   <li>getTitleForLesson lessonId != null → TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Material được append, lesson được save và sync metrics</li>
     * </ul>
     */
    @Test
    void it_should_add_material_and_resolve_lesson_title_when_upload_is_valid() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setLessonId(LESSON_ID);
      lesson.setMaterials("[]");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getOriginalFilename()).thenReturn("de-cuong.pdf");
      when(multipartFile.getContentType()).thenReturn("application/pdf");
      when(multipartFile.getSize()).thenReturn(1024L);
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(uploadService.uploadFile(multipartFile, "course-materials", "materials-bucket"))
          .thenReturn("course-materials/de-cuong.pdf");
      when(objectMapper.writeValueAsString(any(List.class))).thenReturn("[{\"id\":\"x\"}]");
      when(courseLessonRepository.save(lesson)).thenReturn(lesson);
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID))
          .thenReturn(Optional.of(buildLesson(LESSON_ID, "Bài lượng giác")));

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.addMaterial(COURSE_ID, COURSE_LESSON_ID, multipartFile);
      }

      // ===== ASSERT =====
      assertEquals("Bài lượng giác", result.getLessonTitle());
      verify(courseService, times(1)).syncCourseMetrics(COURSE_ID);
    }

    /**
     * Abnormal case: Serialize materials thất bại khi thêm material.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>objectMapper.writeValueAsString throws → RuntimeException branch</li>
     * </ul>
     */
    @Test
    void it_should_throw_runtime_exception_when_add_material_serialization_fails() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getOriginalFilename()).thenReturn("file.docx");
      when(multipartFile.getContentType()).thenReturn("application/msword");
      when(multipartFile.getSize()).thenReturn(200L);
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(uploadService.uploadFile(multipartFile, "course-materials", "materials-bucket"))
          .thenReturn("course-materials/file.docx");
      when(objectMapper.writeValueAsString(any(List.class)))
          .thenThrow(new JsonProcessingException("cannot serialize") {});

      // ===== ACT & ASSERT =====
      RuntimeException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                RuntimeException.class,
                () -> courseLessonService.addMaterial(COURSE_ID, COURSE_LESSON_ID, multipartFile));
      }
      assertEquals("Error saving materials metadata", exception.getMessage());
    }

    /**
     * Normal case: Thêm material cho custom lesson không có lessonId.
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getTitleForLesson lessonId != null → FALSE branch</li>
     * </ul>
     */
    @Test
    void it_should_add_material_for_custom_lesson_when_lesson_id_is_null() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setLessonId(null);
      lesson.setCustomTitle("Custom chapter title");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(multipartFile.isEmpty()).thenReturn(false);
      when(multipartFile.getOriginalFilename()).thenReturn("note.txt");
      when(multipartFile.getContentType()).thenReturn("text/plain");
      when(multipartFile.getSize()).thenReturn(50L);
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(uploadService.uploadFile(multipartFile, "course-materials", "materials-bucket"))
          .thenReturn("course-materials/note.txt");
      when(objectMapper.writeValueAsString(any(List.class))).thenReturn("[{\"id\":\"new\"}]");
      when(courseLessonRepository.save(lesson)).thenReturn(lesson);

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.addMaterial(COURSE_ID, COURSE_LESSON_ID, multipartFile);
      }

      // ===== ASSERT =====
      assertEquals("Custom chapter title", result.getLessonTitle());
    }
  }

  @Nested
  @DisplayName("removeMaterial()")
  class RemoveMaterialTests {

    /**
     * Normal case: Xoá material thành công khi materialId tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>materials chứa item id = material-1</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>toRemove != null → TRUE branch</li>
     *   <li>toRemove.key hasText → TRUE branch (delete file)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>File bị xoá, metadata cập nhật và sync metrics được gọi</li>
     * </ul>
     */
    @Test
    void it_should_remove_material_and_sync_metrics_when_material_exists() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setMaterials("[{\"id\":\"material-1\",\"name\":\"PDF\"}]");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(objectMapper.readValue(eq(lesson.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(
              new java.util.ArrayList<>(
                  List.of(
                      MaterialItem.builder()
                          .id("material-1")
                          .name("PDF")
                          .key("course-materials/math.pdf")
                          .build())));
      when(objectMapper.writeValueAsString(any(List.class))).thenReturn("[]");

      // ===== ACT =====
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        courseLessonService.removeMaterial(COURSE_ID, COURSE_LESSON_ID, "material-1");
      }

      // ===== ASSERT =====
      assertEquals("[]", lesson.getMaterials());

      // ===== VERIFY =====
      verify(uploadService, times(1))
          .deleteFile("course-materials/math.pdf", "materials-bucket");
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(courseLessonRepository, times(1)).save(lesson);
      verify(courseService, times(1)).syncCourseMetrics(COURSE_ID);
      verifyNoMoreInteractions(uploadService, courseService);
    }
  }

  @Nested
  @DisplayName("getMaterialDownloadUrl()")
  class GetMaterialDownloadUrlTests {

    /**
     * Abnormal case: User không phải owner và cũng không có ACTIVE enrollment.
     *
     * <p>Input:
     * <ul>
     *   <li>currentUserId: OTHER_ID</li>
     *   <li>enrollment lookup: empty</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isOwner → FALSE branch</li>
     *   <li>enrolled → FALSE branch (throw COURSE_ACCESS_DENIED)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code COURSE_ACCESS_DENIED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_user_has_no_access_to_download_material_url() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(OTHER_ID, COURSE_ID))
          .thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(OTHER_ID);
        exception =
            assertThrows(
                AppException.class,
                () -> courseLessonService.getMaterialDownloadUrl(COURSE_ID, COURSE_LESSON_ID, "m1"));
      }
      assertEquals(ErrorCode.COURSE_ACCESS_DENIED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(courseLessonRepository, never()).findByIdAndDeletedAtIsNull(any(UUID.class));
      verifyNoMoreInteractions(courseLessonRepository, uploadService);
    }

    /**
     * Normal case: Tạo presigned download URL, fallback sang template bucket khi bucket chính lỗi.
     *
     * <p>Input:
     * <ul>
     *   <li>currentUserId: course owner</li>
     *   <li>download URL from materials bucket throws exception</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isOwner → TRUE branch</li>
     *   <li>try getPresignedDownloadUrl(primary) throws → catch fallback branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về URL từ template bucket fallback</li>
     * </ul>
     */
    @Test
    void it_should_fallback_to_template_bucket_when_primary_download_url_generation_fails()
        throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setMaterials("[{\"id\":\"m1\",\"name\":\"Bài tập\",\"key\":\"legacy/worksheet.pdf\"}]");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(objectMapper.readValue(eq(lesson.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(
              List.of(
                  MaterialItem.builder().id("m1").name("Bài tập").key("legacy/worksheet.pdf").build()));
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(minioProperties.getTemplateBucket()).thenReturn("template-bucket");
      when(uploadService.getPresignedDownloadUrl("legacy/worksheet.pdf", "materials-bucket", "Bài tập"))
          .thenThrow(new RuntimeException("Not found in primary bucket"));
      when(uploadService.getPresignedDownloadUrl("legacy/worksheet.pdf", "template-bucket", "Bài tập"))
          .thenReturn("https://signed/template");

      // ===== ACT =====
      String result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.getMaterialDownloadUrl(COURSE_ID, COURSE_LESSON_ID, "m1");
      }

      // ===== ASSERT =====
      assertEquals("https://signed/template", result);

      // ===== VERIFY =====
      verify(uploadService, times(1))
          .getPresignedDownloadUrl("legacy/worksheet.pdf", "materials-bucket", "Bài tập");
      verify(uploadService, times(1))
          .getPresignedDownloadUrl("legacy/worksheet.pdf", "template-bucket", "Bài tập");
      verifyNoMoreInteractions(uploadService);
    }

    /**
     * Abnormal case: materialId không tồn tại trong danh sách materials.
     *
     * <p>Input:
     * <ul>
     *   <li>owner request</li>
     *   <li>materialId: not-found</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>stream.findFirst().orElseThrow() → exception branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code INVALID_REQUEST}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_material_id_is_not_found_for_download_url() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setMaterials("[{\"id\":\"m1\",\"name\":\"A\",\"key\":\"k1\"}]");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(objectMapper.readValue(eq(lesson.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(List.of(MaterialItem.builder().id("m1").name("A").key("k1").build()));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(TEACHER_ID);
        exception =
            assertThrows(
                AppException.class,
                () -> courseLessonService.getMaterialDownloadUrl(COURSE_ID, COURSE_LESSON_ID, "not-found"));
      }
      assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("downloadMaterial()")
  class DownloadMaterialTests {

    /**
     * Normal case: Download material thành công với fallback content type mặc định.
     *
     * <p>Input:
     * <ul>
     *   <li>current user: owner</li>
     *   <li>material.contentType: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isOwner → TRUE branch</li>
     *   <li>downloadFile(primary) thành công → try branch</li>
     *   <li>contentType null-check → TRUE branch (default octet-stream)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về byte[] content đúng và contentType mặc định</li>
     * </ul>
     */
    @Test
    void it_should_download_material_with_default_content_type_when_item_has_null_content_type()
        throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setMaterials("[{\"id\":\"m2\",\"name\":\"Tổng hợp\",\"key\":\"course-materials/all.pdf\"}]");
      byte[] fileBytes = "pdf-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(objectMapper.readValue(eq(lesson.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(
              List.of(
                  MaterialItem.builder()
                      .id("m2")
                      .name("Tổng hợp")
                      .key("course-materials/all.pdf")
                      .contentType(null)
                      .build()));
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(uploadService.downloadFile("course-materials/all.pdf", "materials-bucket"))
          .thenReturn(fileBytes);

      // ===== ACT =====
      CourseLessonService.MaterialDownloadResult result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.downloadMaterial(COURSE_ID, COURSE_LESSON_ID, "m2");
      }

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals("application/octet-stream", result.contentType()),
          () -> assertEquals("Tổng hợp", result.fileName()),
          () -> assertEquals(fileBytes.length, result.content().length));

      // ===== VERIFY =====
      verify(uploadService, times(1)).downloadFile("course-materials/all.pdf", "materials-bucket");
      verifyNoMoreInteractions(uploadService);
    }

    /**
     * Normal case: Download fallback sang template bucket khi bucket chính lỗi.
     *
     * <p>Input:
     * <ul>
     *   <li>owner request, primary download throws exception</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>downloadFile(primary) throws → catch fallback branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Nội dung trả về từ template bucket</li>
     * </ul>
     */
    @Test
    void it_should_fallback_to_template_bucket_when_download_from_primary_bucket_fails()
        throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setMaterials("[{\"id\":\"m3\",\"name\":\"Legacy\",\"key\":\"legacy/f.pdf\"}]");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(objectMapper.readValue(eq(lesson.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenReturn(List.of(MaterialItem.builder().id("m3").name("Legacy").key("legacy/f.pdf").build()));
      when(minioProperties.getCourseMaterialsBucket()).thenReturn("materials-bucket");
      when(minioProperties.getTemplateBucket()).thenReturn("template-bucket");
      when(uploadService.downloadFile("legacy/f.pdf", "materials-bucket"))
          .thenThrow(new RuntimeException("primary missing"));
      when(uploadService.downloadFile("legacy/f.pdf", "template-bucket"))
          .thenReturn("fallback-content".getBytes(java.nio.charset.StandardCharsets.UTF_8));

      // ===== ACT =====
      CourseLessonService.MaterialDownloadResult result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.downloadMaterial(COURSE_ID, COURSE_LESSON_ID, "m3");
      }

      // ===== ASSERT =====
      assertEquals("Legacy", result.fileName());
      assertEquals(16, result.content().length);
    }

    /**
     * Abnormal case: User không phải owner và enrollment không ACTIVE khi download file.
     *
     * <p>Input:
     * <ul>
     *   <li>currentUserId: STUDENT_ID</li>
     *   <li>enrollment status: CANCELLED</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>isOwner → FALSE branch</li>
     *   <li>enrolled ACTIVE check → FALSE branch (throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code COURSE_ACCESS_DENIED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_download_material_access_is_denied() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(buildEnrollment(EnrollmentStatus.DROPPED)));

      // ===== ACT & ASSERT =====
      AppException exception;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getOptionalCurrentUserId).thenReturn(STUDENT_ID);
        exception =
            assertThrows(
                AppException.class,
                () -> courseLessonService.downloadMaterial(COURSE_ID, COURSE_LESSON_ID, "m1"));
      }
      assertEquals(ErrorCode.COURSE_ACCESS_DENIED, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("getMaterialList() legacy behavior via public methods")
  class LegacyMaterialsJsonTests {

    /**
     * Normal fallback case: materials JSON lỗi parse thì service vẫn không văng exception.
     *
     * <p>Input:
     * <ul>
     *   <li>materials: plain text legacy link</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getMaterialList json parse throws → catch branch returns empty list</li>
     *   <li>removeMaterial toRemove == null → FALSE path (skip save/sync)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Không throw exception, và không gọi save/sync khi không tìm thấy material</li>
     * </ul>
     */
    @Test
    void it_should_return_without_changes_when_materials_json_is_legacy_text() throws Exception {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CourseLesson lesson = buildCourseLesson(COURSE_LESSON_ID, COURSE_ID);
      lesson.setMaterials("https://legacy-link.example.com/resource");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.findByIdAndDeletedAtIsNull(COURSE_LESSON_ID))
          .thenReturn(Optional.of(lesson));
      when(objectMapper.readValue(eq(lesson.getMaterials()), any(com.fasterxml.jackson.core.type.TypeReference.class)))
          .thenThrow(new JsonProcessingException("invalid json") {});

      // ===== ACT =====
      CourseLessonResponse result;
      try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
        security.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        result = courseLessonService.removeMaterial(COURSE_ID, COURSE_LESSON_ID, "not-found");
      }

      // ===== ASSERT =====
      assertNotNull(result);
      assertEquals("https://legacy-link.example.com/resource", lesson.getMaterials());

      // ===== VERIFY =====
      verify(courseLessonRepository, times(1)).findByIdAndDeletedAtIsNull(COURSE_LESSON_ID);
      verify(courseLessonRepository, never()).save(any(CourseLesson.class));
      verify(courseService, never()).syncCourseMetrics(any(UUID.class));
      verifyNoMoreInteractions(courseService);
    }
  }
}
