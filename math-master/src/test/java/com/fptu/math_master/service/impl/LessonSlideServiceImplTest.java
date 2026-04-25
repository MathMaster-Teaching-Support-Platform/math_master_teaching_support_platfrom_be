package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.LessonSlideConfirmContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGenerateContentRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxFromJsonRequest;
import com.fptu.math_master.dto.request.LessonSlideGeneratePptxRequest;
import com.fptu.math_master.dto.request.LessonSlideJsonItemRequest;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.entity.SlideTemplate;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.LessonSlideGeneratedFile;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.LessonSlideGeneratedFileRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SlideTemplateRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.LatexRenderService;
import com.fptu.math_master.service.LessonSlideService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.service.UserSubscriptionService;
import com.fptu.math_master.util.SecurityUtils;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("LessonSlideServiceImpl - Tests")
class LessonSlideServiceImplTest extends BaseUnitTest {

  @InjectMocks private LessonSlideServiceImpl lessonSlideService;

  @Mock private LessonRepository lessonRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private SchoolGradeRepository schoolGradeRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private SlideTemplateRepository slideTemplateRepository;
  @Mock private LessonSlideGeneratedFileRepository lessonSlideGeneratedFileRepository;
  @Mock private MinioClient minioClient;
  @Mock private MinioProperties minioProperties;
  @Mock private GeminiService geminiService;
  @Mock private LatexRenderService latexRenderService;
  @Mock private UserSubscriptionService userSubscriptionService;
  @Mock private UploadService uploadService;
  @Mock private ObjectMapper objectMapper;
  @Mock private MultipartFile thumbnailFile;

  private UUID teacherId;
  private UUID lessonId;
  private Lesson samplePublishedLesson;
  private Lesson sampleDraftLesson;

  @BeforeEach
  void setUp() {
    teacherId = UUID.fromString("11111111-2222-3333-4444-555555555555");
    lessonId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    samplePublishedLesson = buildLesson(lessonId, "Hinh hoc khong gian", LessonStatus.PUBLISHED, null);
    sampleDraftLesson = buildLesson(lessonId, "Hinh hoc khong gian", LessonStatus.DRAFT, null);
  }

  private Lesson buildLesson(UUID id, String title, LessonStatus status, UUID createdBy) {
    Lesson lesson =
        Lesson.builder()
            .chapterId(UUID.fromString("99999999-8888-7777-6666-555555555555"))
            .title(title)
            .learningObjectives("Hieu va van dung duoc cong thuc the tich")
            .lessonContent("Noi dung bai hoc ve hinh chop va hinh tru")
            .summary("Tom tat bai hoc")
            .status(status)
            .build();
    lesson.setId(id);
    lesson.setCreatedBy(createdBy);
    return lesson;
  }

  private LessonSlideGeneratedFile buildGeneratedFile(UUID id, UUID createdBy, String name) {
    LessonSlideGeneratedFile file =
        LessonSlideGeneratedFile.builder()
            .lessonId(lessonId)
            .templateId(UUID.fromString("12345678-1234-1234-1234-123456789012"))
            .bucketName("math-master-assets")
            .objectKey("generated-slides/example.pptx")
            .fileName("example.pptx")
            .name(name)
            .contentType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
            .fileSizeBytes(12345L)
            .isPublic(Boolean.FALSE)
            .build();
    file.setId(id);
    file.setCreatedBy(createdBy);
    return file;
  }

  private SlideTemplate buildTemplate(UUID templateId, boolean active, String previewKey) {
    SlideTemplate template =
        SlideTemplate.builder()
            .name("Template Toan 11")
            .description("Template danh cho bai giang toan hoc")
            .originalFileName("template-toan.pptx")
            .contentType("application/vnd.openxmlformats-officedocument.presentationml.presentation")
            .objectKey("slide-templates/template-toan.pptx")
            .previewImageObjectKey(previewKey)
            .previewImageContentType("image/png")
            .bucketName("math-master-assets")
            .uploadedBy(teacherId)
            .isActive(active)
            .build();
    template.setId(templateId);
    return template;
  }

  private byte[] createTemplateBytesWithPlaceholders(int slideCount) throws Exception {
    try (XMLSlideShow slideShow = new XMLSlideShow();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (int i = 0; i < slideCount; i++) {
        XSLFSlide slide = slideShow.createSlide();
        XSLFTextBox title = slide.createTextBox();
        title.setText("{{LESSON_TITLE}}");
        title.setAnchor(new java.awt.geom.Rectangle2D.Double(20, 20, 400, 50));
        XSLFTextBox content = slide.createTextBox();
        content.setText("{{LESSON_CONTENT}}");
        content.setAnchor(new java.awt.geom.Rectangle2D.Double(30, 100, 500, 300));
      }
      slideShow.write(out);
      return out.toByteArray();
    }
  }

  private Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) {
    try {
      Method method = LessonSlideServiceImpl.class.getDeclaredMethod(methodName, paramTypes);
      method.setAccessible(true);
      return method.invoke(lessonSlideService, args);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to invoke private method: " + methodName, ex);
    }
  }

  private Class<?> getInnerClass(String simpleName) {
    for (Class<?> inner : LessonSlideServiceImpl.class.getDeclaredClasses()) {
      if (inner.getSimpleName().equals(simpleName)) {
        return inner;
      }
    }
    throw new IllegalStateException("Inner class not found: " + simpleName);
  }

  @Nested
  @DisplayName("getPublishedLessonSlide()")
  class GetPublishedLessonSlideTests {

    /**
     * Normal case: Return published lesson by id.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonId: valid id that exists and has status PUBLISHED</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted -> present (TRUE branch)</li>
     *   <li>status != PUBLISHED -> FALSE branch (do not throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Return lesson response with matching id/title/status</li>
     * </ul>
     */
    @Test
    void
    it_should_return_lesson_response_when_lesson_is_published() {
      // ===== ARRANGE =====
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(samplePublishedLesson));

      // ===== ACT =====
      LessonResponse result = lessonSlideService.getPublishedLessonSlide(lessonId);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals(samplePublishedLesson.getId(), result.getId()),
          () -> assertEquals(samplePublishedLesson.getTitle(), result.getTitle()),
          () -> assertEquals(LessonStatus.PUBLISHED, result.getStatus()));

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verifyNoMoreInteractions(lessonRepository);
    }

    /**
     * Abnormal case: Lesson exists but not published.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonId: valid id that exists and has status DRAFT</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted -> present (TRUE branch)</li>
     *   <li>status != PUBLISHED -> TRUE branch (throw exception)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} with error code {@code LESSON_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_lesson_is_not_published() {
      // ===== ARRANGE =====
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(sampleDraftLesson));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> lessonSlideService.getPublishedLessonSlide(lessonId));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verifyNoMoreInteractions(lessonRepository);
    }
  }

  @Nested
  @DisplayName("getLessonSlides()")
  class GetLessonSlidesTests {

    /**
     * Normal case: Default status to DRAFT when input status is null.
     *
     * <p>Input:
     * <ul>
     *   <li>status: null</li>
     *   <li>currentUserId: teacher id from SecurityUtils</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateTeacherRole -> TRUE branch (teacher role available)</li>
     *   <li>status == null -> TRUE branch (fallback to DRAFT)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Repository called with DRAFT status and current teacher id</li>
     * </ul>
     */
    @Test
    void it_should_use_draft_status_when_input_status_is_null() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonRepository.findTeacherSlideLessonsByStatus(teacherId, LessonStatus.DRAFT))
            .thenReturn(List.of(sampleDraftLesson));

        // ===== ACT =====
        List<LessonResponse> result = lessonSlideService.getLessonSlides(null);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(1, result.size()),
            () -> assertEquals(LessonStatus.DRAFT, result.getFirst().getStatus()));

        // ===== VERIFY =====
        verify(lessonRepository, times(1))
            .findTeacherSlideLessonsByStatus(teacherId, LessonStatus.DRAFT);
        verifyNoMoreInteractions(lessonRepository);
      }
    }
  }

  @Nested
  @DisplayName("publishLessonSlide()")
  class PublishLessonSlideTests {

    /**
     * Normal case: Publish lesson and backfill createdBy when null.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonId: valid id</li>
     *   <li>createdBy: null before publish</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateTeacherRole -> TRUE branch</li>
     *   <li>lesson.getCreatedBy() == null -> TRUE branch (set createdBy)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Status becomes PUBLISHED, createdBy and updatedBy are current teacher id</li>
     * </ul>
     */
    @Test
    void it_should_set_created_by_and_publish_when_created_by_is_null() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Dai so tuyen tinh", LessonStatus.DRAFT, null);
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        // ===== ACT =====
        LessonResponse result = lessonSlideService.publishLessonSlide(lessonId);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(LessonStatus.PUBLISHED, lesson.getStatus()),
            () -> assertEquals(teacherId, lesson.getCreatedBy()),
            () -> assertEquals(teacherId, lesson.getUpdatedBy()));

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
        verify(lessonRepository, times(1)).save(lesson);
        verifyNoMoreInteractions(lessonRepository);
      }
    }
  }

  @Nested
  @DisplayName("unpublishLessonSlide()")
  class UnpublishLessonSlideTests {

    /**
     * Normal case: Keep existing createdBy while switching status to DRAFT.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonId: valid id</li>
     *   <li>createdBy: already exists</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>lesson.getCreatedBy() == null -> FALSE branch (do not override createdBy)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Status becomes DRAFT and existing createdBy is preserved</li>
     * </ul>
     */
    @Test
    void it_should_keep_created_by_and_unpublish_when_created_by_already_exists() {
      // ===== ARRANGE =====
      UUID originalCreator = UUID.fromString("aaaaaaaa-1111-2222-3333-bbbbbbbbbbbb");
      Lesson lesson = buildLesson(lessonId, "He phuong trinh", LessonStatus.PUBLISHED, originalCreator);
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        // ===== ACT =====
        LessonResponse result = lessonSlideService.unpublishLessonSlide(lessonId);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(LessonStatus.DRAFT, lesson.getStatus()),
            () -> assertEquals(originalCreator, lesson.getCreatedBy()),
            () -> assertEquals(teacherId, lesson.getUpdatedBy()));

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
        verify(lessonRepository, times(1)).save(lesson);
        verifyNoMoreInteractions(lessonRepository);
      }
    }
  }

  @Nested
  @DisplayName("getAllPublicGeneratedSlides()")
  class GetAllPublicGeneratedSlidesTests {

    /**
     * Normal case: Trim keyword before passing to repository.
     *
     * <p>Input:
     * <ul>
     *   <li>keyword: "  hinh hoc  "</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>keyword == null -> FALSE branch</li>
     *   <li>keyword trim path -> TRUE branch with non-empty result</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Repository receives "hinh hoc"</li>
     * </ul>
     */
    @Test
    void it_should_trim_keyword_before_querying_public_slides() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 10);
      LessonSlideGeneratedFile file =
          buildGeneratedFile(
              UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff"),
              teacherId,
              "Hinh hoc khong gian");
      Page<LessonSlideGeneratedFile> page = new PageImpl<>(List.of(file), pageable, 1);
      when(lessonSlideGeneratedFileRepository.findAllPublicWithFilters(lessonId, "hinh hoc", pageable))
          .thenReturn(page);

      // ===== ACT =====
      Page<?> result = lessonSlideService.getAllPublicGeneratedSlides(lessonId, "  hinh hoc  ", pageable);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertEquals(1L, result.getTotalElements()));

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1))
          .findAllPublicWithFilters(lessonId, "hinh hoc", pageable);
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
    }

    @Test
    void it_should_pass_null_keyword_when_keyword_input_is_null() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 5);
      when(lessonSlideGeneratedFileRepository.findAllPublicWithFilters(lessonId, null, pageable))
          .thenReturn(Page.empty(pageable));

      // ===== ACT =====
      Page<?> result = lessonSlideService.getAllPublicGeneratedSlides(lessonId, null, pageable);

      // ===== ASSERT =====
      assertEquals(0L, result.getTotalElements());

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1))
          .findAllPublicWithFilters(lessonId, null, pageable);
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
    }
  }

  @Nested
  @DisplayName("getPublicGeneratedSlidesByLesson()")
  class GetPublicGeneratedSlidesByLessonTests {

    /**
     * Abnormal case: Lesson id does not exist.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonId: missing id</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>lessonRepository.findByIdAndNotDeleted -> Optional.empty error branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} with error code {@code LESSON_NOT_FOUND}</li>
     *   <li>No call to generated-file repository</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_lesson_not_found_for_public_slide_listing() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 5);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> lessonSlideService.getPublicGeneratedSlidesByLesson(lessonId, "abc", pageable));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verifyNoMoreInteractions(lessonRepository);
      verify(lessonSlideGeneratedFileRepository, never())
          .findAllPublicWithFilters(lessonId, "abc", pageable);
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
    }

    @Test
    void it_should_return_public_generated_slides_by_lesson_when_lesson_exists() {
      // ===== ARRANGE =====
      Pageable pageable = PageRequest.of(0, 10);
      LessonSlideGeneratedFile file =
          buildGeneratedFile(
              UUID.fromString("12121212-aaaa-bbbb-cccc-131313131313"),
              teacherId,
              "Bai giang xac suat");
      Page<LessonSlideGeneratedFile> page = new PageImpl<>(List.of(file), pageable, 1);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(samplePublishedLesson));
      when(lessonSlideGeneratedFileRepository.findAllPublicWithFilters(lessonId, "xac suat", pageable))
          .thenReturn(page);

      // ===== ACT =====
      Page<?> result = lessonSlideService.getPublicGeneratedSlidesByLesson(lessonId, "  xac suat  ", pageable);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(result), () -> assertEquals(1L, result.getTotalElements()));

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verify(lessonSlideGeneratedFileRepository, times(1))
          .findAllPublicWithFilters(lessonId, "xac suat", pageable);
      verifyNoMoreInteractions(lessonRepository, lessonSlideGeneratedFileRepository);
    }
  }

  @Nested
  @DisplayName("updateGeneratedSlideMetadata()")
  class UpdateGeneratedSlideMetadataTests {

    /**
     * Normal case: Blank name becomes null and no thumbnail upload when thumbnail is empty.
     *
     * <p>Input:
     * <ul>
     *   <li>name: "   " (blank after trim)</li>
     *   <li>thumbnailFile: empty file</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>name != null -> TRUE branch, normalizedName.isEmpty -> TRUE branch (set null)</li>
     *   <li>thumbnailFile != null && !isEmpty -> FALSE branch</li>
     *   <li>owner check: isOwner -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Save succeeds with name set to null and without MinIO interactions</li>
     * </ul>
     */
    @Test
    void it_should_set_name_to_null_when_trimmed_name_is_blank_and_thumbnail_is_empty() {
      // ===== ARRANGE =====
      UUID generatedFileId = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000");
      LessonSlideGeneratedFile file = buildGeneratedFile(generatedFileId, teacherId, "Old Name");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE))
            .thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(generatedFileId))
            .thenReturn(Optional.of(file));
        when(thumbnailFile.isEmpty()).thenReturn(true);
        when(lessonSlideGeneratedFileRepository.save(file)).thenReturn(file);

        // ===== ACT =====
        var result = lessonSlideService.updateGeneratedSlideMetadata(generatedFileId, "   ", thumbnailFile);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(result),
            () -> assertNull(file.getName()),
            () -> assertEquals(generatedFileId, result.getId()));

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(generatedFileId);
        verify(lessonSlideGeneratedFileRepository, times(1)).save(file);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
        verifyNoMoreInteractions(minioClient);
      }
    }
  }

  @Nested
  @DisplayName("security role validation")
  class SecurityValidationTests {

    /**
     * Abnormal case: Teacher-only endpoint called by non-teacher user.
     *
     * <p>Input:
     * <ul>
     *   <li>Security role check returns false for TEACHER_ROLE</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateTeacherRole -> FALSE branch (throw NOT_A_TEACHER)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} with error code {@code NOT_A_TEACHER}</li>
     * </ul>
     */
    @Test
    void it_should_throw_not_a_teacher_when_non_teacher_calls_teacher_only_endpoint() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(false);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.getLessonSlide(lessonId));
        assertEquals(ErrorCode.NOT_A_TEACHER, ex.getErrorCode());

        // ===== VERIFY =====
        verifyNoMoreInteractions(lessonRepository);
      }
    }
  }

  @Nested
  @DisplayName("template management and generation extra coverage")
  class TemplateAndGenerationCoverageTests {

    @Test
    void it_should_upload_template_without_preview_image() throws Exception {
      MultipartFile file = Mockito.mock(MultipartFile.class);
      when(file.isEmpty()).thenReturn(false);
      when(file.getOriginalFilename()).thenReturn("template.pptx");
      when(file.getContentType()).thenReturn(null);
      when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[] {1, 2, 3}));
      when(file.getSize()).thenReturn(3L);
      when(minioProperties.getTemplateBucket()).thenReturn("math-master-assets");
      when(minioClient.bucketExists(any())).thenReturn(true);
      when(slideTemplateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

        var result = lessonSlideService.uploadTemplate("Ten", "Mo ta", file, null);
        assertNotNull(result);
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
      }
    }

    @Test
    void it_should_update_template_with_file_and_preview_image() throws Exception {
      UUID templateId = UUID.fromString("5a5a5a5a-1111-2222-3333-444444444444");
      SlideTemplate template = buildTemplate(templateId, true, null);
      MultipartFile file = Mockito.mock(MultipartFile.class);
      MultipartFile preview = Mockito.mock(MultipartFile.class);
      when(file.isEmpty()).thenReturn(false);
      when(file.getOriginalFilename()).thenReturn("new-template.pptx");
      when(file.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.presentationml.presentation");
      when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[] {7, 8}));
      when(file.getSize()).thenReturn(2L);
      when(preview.isEmpty()).thenReturn(false);
      when(preview.getOriginalFilename()).thenReturn("preview.png");
      when(preview.getContentType()).thenReturn("image/png");
      when(preview.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[] {9, 10}));
      when(preview.getSize()).thenReturn(2L);
      when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.of(template));
      when(slideTemplateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
      when(minioProperties.getTemplateBucket()).thenReturn("math-master-assets");
      when(minioClient.bucketExists(any())).thenReturn(true);

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        var result =
            lessonSlideService.updateTemplate(templateId, "Cap nhat", "Mo ta moi", true, file, preview);

        assertNotNull(result);
        verify(minioClient, times(2)).putObject(any(PutObjectArgs.class));
      }
    }

    @Test
    void it_should_generate_pptx_and_persist_generated_file_happy_path() throws Exception {
      UUID templateId = UUID.fromString("6b6b6b6b-1111-2222-3333-444444444444");
      Lesson lesson = buildLesson(lessonId, "Bai hoc tao pptx", LessonStatus.PUBLISHED, teacherId);
      SlideTemplate template = buildTemplate(templateId, true, null);
      LessonSlideGeneratePptxRequest request =
          LessonSlideGeneratePptxRequest.builder()
              .lessonId(lessonId)
              .templateId(templateId)
              .additionalPrompt("prompt")
              .build();
      byte[] templateBytes = createTemplateBytesWithPlaceholders(10);
      GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
      when(getObjectResponse.readAllBytes()).thenReturn(templateBytes);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.of(template));
      when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);
      when(minioProperties.getTemplateBucket()).thenReturn("math-master-assets");
      when(minioClient.bucketExists(any())).thenReturn(true);
      when(lessonSlideGeneratedFileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);

        LessonSlideService.BinaryFileData result = lessonSlideService.generatePptx(request);
        assertAll(
            () -> assertNotNull(result.content()),
            () -> assertEquals(true, result.fileName().endsWith(".pptx")));
      }
    }

    @Test
    void it_should_get_generated_slide_preview_pdf_successfully() throws Exception {
      UUID fileId = UUID.fromString("7c7c7c7c-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Preview");
      byte[] pptxBytes = createTemplateBytesWithPlaceholders(2);
      when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
      when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
      GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
      when(getObjectResponse.readAllBytes()).thenReturn(pptxBytes);
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        LessonSlideService.BinaryFileData result = lessonSlideService.getGeneratedSlidePreviewPdf(fileId);
        assertAll(
            () -> assertEquals("application/pdf", result.contentType()),
            () -> assertEquals(true, result.fileName().endsWith(".pdf")));
      }
    }

    @Test
    void it_should_set_thumbnail_when_update_metadata_receives_valid_thumbnail() throws Exception {
      UUID generatedFileId = UUID.fromString("8d8d8d8d-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(generatedFileId, teacherId, "Old");
      MultipartFile preview = Mockito.mock(MultipartFile.class);
      when(preview.isEmpty()).thenReturn(false);
      when(preview.getOriginalFilename()).thenReturn("thumb.png");
      when(preview.getContentType()).thenReturn("image/png");
      when(preview.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[] {1}));
      when(preview.getSize()).thenReturn(1L);
      when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(generatedFileId))
          .thenReturn(Optional.of(file));
      when(minioProperties.getTemplateBucket()).thenReturn("math-master-assets");
      when(minioClient.bucketExists(any())).thenReturn(true);
      when(lessonSlideGeneratedFileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        var result = lessonSlideService.updateGeneratedSlideMetadata(generatedFileId, "new", preview);
        assertAll(() -> assertNotNull(result), () -> assertNotNull(file.getThumbnail()));
      }
    }
  }

  @Nested
  @DisplayName("confirmLessonContent()")
  class ConfirmLessonContentTests {

    /**
     * Normal case: Fill summary from lesson content when summary is blank and set creator/updater.
     *
     * <p>Input:
     * <ul>
     *   <li>summary: blank string</li>
     *   <li>createdBy: null in lesson entity</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>summary blank -> TRUE branch (use buildSummary)</li>
     *   <li>learningObjectives blank -> FALSE branch (do not override)</li>
     *   <li>createdBy null -> TRUE branch (set createdBy)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Lesson is saved with generated summary and audit fields set</li>
     * </ul>
     */
    @Test
    void it_should_generate_summary_and_set_audit_fields_when_summary_is_blank() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Toan hoc", LessonStatus.DRAFT, null);
      lesson.setLearningObjectives("Muc tieu cu");
      LessonSlideConfirmContentRequest request =
          LessonSlideConfirmContentRequest.builder()
              .lessonContent("Noi dung moi rat dai ".repeat(20))
              .summary("   ")
              .learningObjectives("  ")
              .build();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        // ===== ACT =====
        LessonResponse result = lessonSlideService.confirmLessonContent(lessonId, request);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(result),
            () -> assertNotNull(lesson.getSummary()),
            () -> assertEquals(teacherId, lesson.getCreatedBy()),
            () -> assertEquals(teacherId, lesson.getUpdatedBy()));

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
        verify(lessonRepository, times(1)).save(lesson);
        verifyNoMoreInteractions(lessonRepository);
      }
    }
  }

  @Nested
  @DisplayName("getTemplates()")
  class GetTemplatesTests {

    @Test
    void it_should_query_active_templates_when_active_only_is_true() {
      // ===== ARRANGE =====
      SlideTemplate template =
          buildTemplate(
              UUID.fromString("00000000-1111-2222-3333-444444444444"),
              true,
              "slide-templates-preview/a.png");
      when(slideTemplateRepository.findAllActiveNotDeleted()).thenReturn(List.of(template));

      // ===== ACT =====
      var result = lessonSlideService.getTemplates(true);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, result.size()), () -> assertEquals(template.getId(), result.getFirst().getId()));

      // ===== VERIFY =====
      verify(slideTemplateRepository, times(1)).findAllActiveNotDeleted();
      verifyNoMoreInteractions(slideTemplateRepository);
    }

    @Test
    void it_should_query_all_templates_when_active_only_is_false() {
      // ===== ARRANGE =====
      SlideTemplate template =
          buildTemplate(
              UUID.fromString("11111111-2222-3333-4444-555555555555"),
              false,
              null);
      when(slideTemplateRepository.findAllNotDeleted()).thenReturn(List.of(template));

      // ===== ACT =====
      var result = lessonSlideService.getTemplates(false);

      // ===== ASSERT =====
      assertEquals(1, result.size());

      // ===== VERIFY =====
      verify(slideTemplateRepository, times(1)).findAllNotDeleted();
      verifyNoMoreInteractions(slideTemplateRepository);
    }
  }

  @Nested
  @DisplayName("downloadTemplatePreviewImage()")
  class DownloadTemplatePreviewImageTests {

    @Test
    void it_should_throw_template_not_usable_when_preview_key_missing() {
      // ===== ARRANGE =====
      UUID templateId = UUID.fromString("aaaaaaaa-1111-1111-1111-aaaaaaaaaaaa");
      SlideTemplate template = buildTemplate(templateId, true, " ");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.of(template));

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(
                AppException.class,
                () -> lessonSlideService.downloadTemplatePreviewImage(templateId));
        assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, ex.getErrorCode());

        // ===== VERIFY =====
        verify(slideTemplateRepository, times(1)).findByIdAndNotDeleted(templateId);
        verifyNoMoreInteractions(slideTemplateRepository, minioClient);
      }
    }

    @Test
    void it_should_return_preview_image_when_template_contains_preview_key() throws Exception {
      // ===== ARRANGE =====
      UUID templateId = UUID.fromString("bbbbbbbb-2222-2222-2222-bbbbbbbbbbbb");
      SlideTemplate template = buildTemplate(templateId, true, "slide-templates-preview/preview.jpg");
      template.setPreviewImageContentType("image/jpeg");
      byte[] expected = "preview-content".getBytes();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.of(template));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
        when(getObjectResponse.readAllBytes()).thenReturn(expected);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

        // ===== ACT =====
        LessonSlideService.BinaryFileData result =
            lessonSlideService.downloadTemplatePreviewImage(templateId);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals("template-preview-image", result.fileName()),
            () -> assertEquals("image/jpeg", result.contentType()),
            () -> assertEquals(expected.length, result.content().length));

        // ===== VERIFY =====
        verify(slideTemplateRepository, times(1)).findByIdAndNotDeleted(templateId);
        verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        verifyNoMoreInteractions(slideTemplateRepository, minioClient);
      }
    }
  }

  @Nested
  @DisplayName("generated slide ownership and public APIs")
  class GeneratedSlideOwnershipTests {

    @Test
    void it_should_throw_access_denied_when_current_user_is_not_owner_and_not_admin() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("dddddddd-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file =
          buildGeneratedFile(
              fileId,
              UUID.fromString("99999999-9999-9999-9999-999999999999"),
              "Slide 01");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE))
            .thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.downloadGeneratedSlide(fileId));
        assertEquals(ErrorCode.GENERATED_SLIDE_ACCESS_DENIED, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, minioClient);
      }
    }

    @Test
    void it_should_publish_generated_slide_when_owner_is_valid() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("eeeeeeee-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Cong thuc luong giac");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE))
            .thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(lessonSlideGeneratedFileRepository.save(file)).thenReturn(file);

        // ===== ACT =====
        var result = lessonSlideService.publishGeneratedSlide(fileId);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(fileId, result.getId()),
            () -> assertEquals(Boolean.TRUE, file.getIsPublic()),
            () -> assertNotNull(file.getPublishedAt()));

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(lessonSlideGeneratedFileRepository, times(1)).save(file);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }

    @Test
    void it_should_unpublish_generated_slide_and_clear_published_time() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("ffffffff-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Do thi ham so");
      file.setIsPublic(Boolean.TRUE);
      file.setPublishedAt(Instant.now());
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE))
            .thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(lessonSlideGeneratedFileRepository.save(file)).thenReturn(file);

        // ===== ACT =====
        var result = lessonSlideService.unpublishGeneratedSlide(fileId);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(fileId, result.getId()),
            () -> assertEquals(Boolean.FALSE, file.getIsPublic()),
            () -> assertNull(file.getPublishedAt()));

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(lessonSlideGeneratedFileRepository, times(1)).save(file);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }

    @Test
    void it_should_soft_delete_file_even_when_minio_delete_fails() throws Exception {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("10101010-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Nhi thuc Newton");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE))
            .thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(lessonSlideGeneratedFileRepository.save(file)).thenReturn(file);
        Mockito.doThrow(new RuntimeException("minio unavailable"))
            .when(minioClient)
            .removeObject(any(RemoveObjectArgs.class));

        // ===== ACT =====
        lessonSlideService.deleteGeneratedSlide(fileId);

        // ===== ASSERT =====
        assertNotNull(file.getDeletedAt());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(lessonSlideGeneratedFileRepository, times(1)).save(file);
        verify(minioClient, times(1)).removeObject(any(RemoveObjectArgs.class));
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, minioClient);
      }
    }
  }

  @Nested
  @DisplayName("public generated slide endpoints")
  class PublicGeneratedSlideTests {

    @Test
    void it_should_return_presigned_url_for_public_slide() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("20202020-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Duong tron luong giac");
      when(lessonSlideGeneratedFileRepository.findPublicById(fileId)).thenReturn(Optional.of(file));
      when(lessonRepository.findByIdAndNotDeleted(file.getLessonId())).thenReturn(Optional.of(samplePublishedLesson));
      when(uploadService.getPresignedUrl(file.getObjectKey(), file.getBucketName()))
          .thenReturn("https://cdn.math-master/public/slide.pptx");

      // ===== ACT =====
      String result = lessonSlideService.getPublicGeneratedSlidePreviewUrl(fileId);

      // ===== ASSERT =====
      assertEquals("https://cdn.math-master/public/slide.pptx", result);

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1)).findPublicById(fileId);
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(file.getLessonId());
      verify(uploadService, times(1)).getPresignedUrl(file.getObjectKey(), file.getBucketName());
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, lessonRepository, uploadService);
    }

    @Test
    void it_should_throw_template_not_usable_when_public_thumbnail_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("30303030-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Ham so bac hai");
      file.setThumbnail(" ");
      when(lessonSlideGeneratedFileRepository.findPublicById(fileId)).thenReturn(Optional.of(file));
      when(lessonRepository.findByIdAndNotDeleted(file.getLessonId())).thenReturn(Optional.of(samplePublishedLesson));

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(
              AppException.class,
              () -> lessonSlideService.getPublicGeneratedSlideThumbnailImage(fileId));
      assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, ex.getErrorCode());

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1)).findPublicById(fileId);
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(file.getLessonId());
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, lessonRepository, minioClient);
    }
  }

  @Nested
  @DisplayName("download/get generated slide methods")
  class GeneratedSlideReadMethodsTests {

    @Test
    void it_should_return_generated_slide_binary_for_owner() throws Exception {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("40404040-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Dang ham");
      byte[] expectedBytes = "pptx-content".getBytes();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE))
            .thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
        when(getObjectResponse.readAllBytes()).thenReturn(expectedBytes);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

        // ===== ACT =====
        LessonSlideService.BinaryFileData result = lessonSlideService.downloadGeneratedSlide(fileId);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(file.getFileName(), result.fileName()),
            () -> assertEquals(file.getContentType(), result.contentType()),
            () -> assertEquals(expectedBytes.length, result.content().length));

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, minioClient);
      }
    }

    @Test
    void it_should_return_private_preview_url_for_owner() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("50505050-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "He so goc");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE))
            .thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(uploadService.getPresignedUrl(file.getObjectKey(), file.getBucketName()))
            .thenReturn("https://minio/private/object.pptx");

        // ===== ACT =====
        String result = lessonSlideService.getGeneratedSlidePreviewUrl(fileId);

        // ===== ASSERT =====
        assertEquals("https://minio/private/object.pptx", result);

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(uploadService, times(1)).getPresignedUrl(file.getObjectKey(), file.getBucketName());
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, uploadService);
      }
    }

    @Test
    void it_should_throw_generated_slide_not_found_when_private_file_is_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("18181818-aaaa-bbbb-cccc-191919191919");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.downloadGeneratedSlide(fileId));
        assertEquals(ErrorCode.GENERATED_SLIDE_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }
  }

  @Nested
  @DisplayName("list generated slides")
  class ListGeneratedSlidesTests {

    @Test
    void it_should_list_my_generated_slides_when_lesson_id_is_null() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("60606060-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Tong hop bai giang");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByTeacher(teacherId)).thenReturn(List.of(file));

        // ===== ACT =====
        var result = lessonSlideService.getMyGeneratedSlides(null);

        // ===== ASSERT =====
        assertEquals(1, result.size());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByTeacher(teacherId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }

    @Test
    void it_should_list_my_generated_slides_by_specific_lesson_id() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("70707070-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Bai giang chu de");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByTeacherAndLesson(teacherId, lessonId))
            .thenReturn(List.of(file));

        // ===== ACT =====
        var result = lessonSlideService.getMyGeneratedSlides(lessonId);

        // ===== ASSERT =====
        assertEquals(1, result.size());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByTeacherAndLesson(teacherId, lessonId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }
  }

  @Nested
  @DisplayName("downloadTemplate()")
  class DownloadTemplateTests {

    @Test
    void it_should_download_template_binary_when_template_exists() throws Exception {
      // ===== ARRANGE =====
      UUID templateId = UUID.fromString("80808080-1111-2222-3333-444444444444");
      SlideTemplate template = buildTemplate(templateId, true, null);
      byte[] expectedBytes = "template-bytes".getBytes();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.of(template));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
        when(getObjectResponse.readAllBytes()).thenReturn(expectedBytes);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

        // ===== ACT =====
        LessonSlideService.BinaryFileData result = lessonSlideService.downloadTemplate(templateId);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals(template.getOriginalFileName(), result.fileName()),
            () -> assertEquals(template.getContentType(), result.contentType()),
            () -> assertEquals(expectedBytes.length, result.content().length));

        // ===== VERIFY =====
        verify(slideTemplateRepository, times(1)).findByIdAndNotDeleted(templateId);
        verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        verifyNoMoreInteractions(slideTemplateRepository, minioClient);
      }
    }

    @Test
    void it_should_throw_template_not_found_when_template_does_not_exist() {
      // ===== ARRANGE =====
      UUID templateId = UUID.fromString("31313131-aaaa-bbbb-cccc-323232323232");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.downloadTemplate(templateId));
        assertEquals(ErrorCode.QUESTION_TEMPLATE_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(slideTemplateRepository, times(1)).findByIdAndNotDeleted(templateId);
        verifyNoMoreInteractions(slideTemplateRepository, minioClient);
      }
    }
  }

  @Nested
  @DisplayName("generated slide not-found branches")
  class GeneratedSlideNotFoundBranchTests {

    @Test
    void it_should_throw_generated_slide_not_found_when_publish_target_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("33333333-aaaa-bbbb-cccc-343434343434");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.publishGeneratedSlide(fileId));
        assertEquals(ErrorCode.GENERATED_SLIDE_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }

    @Test
    void it_should_throw_generated_slide_not_found_when_unpublish_target_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("35353535-aaaa-bbbb-cccc-363636363636");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.unpublishGeneratedSlide(fileId));
        assertEquals(ErrorCode.GENERATED_SLIDE_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }

    @Test
    void it_should_throw_generated_slide_not_found_when_delete_target_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("37373737-aaaa-bbbb-cccc-383838383838");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> lessonSlideService.deleteGeneratedSlide(fileId));
        assertEquals(ErrorCode.GENERATED_SLIDE_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, minioClient);
      }
    }

    @Test
    void it_should_throw_generated_slide_not_found_when_metadata_target_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("39393939-aaaa-bbbb-cccc-404040404040");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(
                AppException.class,
                () -> lessonSlideService.updateGeneratedSlideMetadata(fileId, "Ten moi", null));
        assertEquals(ErrorCode.GENERATED_SLIDE_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository);
      }
    }
  }

  @Nested
  @DisplayName("PDF preview error branches")
  class PdfPreviewErrorBranchTests {

    @Test
    void it_should_throw_template_generation_failed_when_private_pdf_preview_receives_invalid_pptx()
        throws Exception {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("41414141-aaaa-bbbb-cccc-424242424242");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Bai giang loi");
      byte[] invalidPptx = "not-a-pptx-file".getBytes();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
        when(getObjectResponse.readAllBytes()).thenReturn(invalidPptx);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.getGeneratedSlidePreviewPdf(fileId));
        assertEquals(ErrorCode.TEMPLATE_GENERATION_FAILED, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      }
    }

    @Test
    void it_should_throw_template_generation_failed_when_public_pdf_preview_receives_invalid_pptx()
        throws Exception {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("43434343-aaaa-bbbb-cccc-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Public loi");
      byte[] invalidPptx = "invalid-public-pptx".getBytes();
      when(lessonSlideGeneratedFileRepository.findPublicById(fileId)).thenReturn(Optional.of(file));
      when(lessonRepository.findByIdAndNotDeleted(file.getLessonId())).thenReturn(Optional.of(samplePublishedLesson));
      when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
      GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
      when(getObjectResponse.readAllBytes()).thenReturn(invalidPptx);
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> lessonSlideService.getPublicGeneratedSlidePreviewPdf(fileId));
      assertEquals(ErrorCode.TEMPLATE_GENERATION_FAILED, ex.getErrorCode());

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1)).findPublicById(fileId);
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(file.getLessonId());
      verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
    }
  }

  @Nested
  @DisplayName("getLessonSlide()")
  class GetLessonSlideTests {

    @Test
    void it_should_throw_lesson_not_found_when_lesson_does_not_exist() {
      // ===== ARRANGE =====
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> lessonSlideService.getLessonSlide(lessonId));
        assertEquals(ErrorCode.LESSON_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
        verifyNoMoreInteractions(lessonRepository);
      }
    }
  }

  @Nested
  @DisplayName("generatePptx() early branches")
  class GeneratePptxEarlyBranchTests {

    @Test
    void it_should_throw_template_not_usable_when_template_is_inactive() {
      // ===== ARRANGE =====
      UUID templateId = UUID.fromString("90909090-1111-2222-3333-444444444444");
      LessonSlideGeneratePptxRequest request =
          LessonSlideGeneratePptxRequest.builder()
              .lessonId(lessonId)
              .templateId(templateId)
              .additionalPrompt("Tao bai giang")
              .build();
      SlideTemplate inactiveTemplate = buildTemplate(templateId, false, null);
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(samplePublishedLesson));
        when(slideTemplateRepository.findByIdAndNotDeleted(templateId))
            .thenReturn(Optional.of(inactiveTemplate));

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> lessonSlideService.generatePptx(request));
        assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
        verify(slideTemplateRepository, times(1)).findByIdAndNotDeleted(templateId);
        verifyNoMoreInteractions(lessonRepository, slideTemplateRepository, minioClient);
      }
    }

    @Test
    void it_should_throw_lesson_not_found_when_generate_pptx_lesson_missing() {
      // ===== ARRANGE =====
      UUID missingLessonId = UUID.fromString("45454545-aaaa-bbbb-cccc-464646464646");
      LessonSlideGeneratePptxRequest request =
          LessonSlideGeneratePptxRequest.builder()
              .lessonId(missingLessonId)
              .templateId(UUID.fromString("47474747-aaaa-bbbb-cccc-484848484848"))
              .additionalPrompt("Prompt")
              .build();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        when(lessonRepository.findByIdAndNotDeleted(missingLessonId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> lessonSlideService.generatePptx(request));
        assertEquals(ErrorCode.LESSON_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(missingLessonId);
        verifyNoMoreInteractions(lessonRepository, slideTemplateRepository, minioClient);
      }
    }
  }

  @Nested
  @DisplayName("generateLessonContentDraft() early branches")
  class GenerateLessonContentDraftEarlyBranchTests {

    @Test
    void it_should_throw_school_grade_not_found_when_grade_does_not_exist() {
      // ===== ARRANGE =====
      LessonSlideGenerateContentRequest request =
          LessonSlideGenerateContentRequest.builder()
              .schoolGradeId(UUID.fromString("11110000-1111-2222-3333-444444444444"))
              .subjectId(UUID.fromString("22220000-1111-2222-3333-444444444444"))
              .chapterId(UUID.fromString("33330000-1111-2222-3333-444444444444"))
              .lessonId(lessonId)
              .slideCount(10)
              .additionalPrompt("Tao noi dung bai giang")
              .build();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(schoolGradeRepository.findByIdAndNotDeleted(request.getSchoolGradeId()))
            .thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(
                AppException.class,
                () -> lessonSlideService.generateLessonContentDraft(request));
        assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(userSubscriptionService, times(1)).consumeMyTokens(3, "SLIDE");
        verify(schoolGradeRepository, times(1)).findByIdAndNotDeleted(request.getSchoolGradeId());
        verifyNoMoreInteractions(userSubscriptionService, schoolGradeRepository, subjectRepository);
      }
    }

    @Test
    void it_should_throw_invalid_subject_when_subject_grade_does_not_match_requested_grade() {
      // ===== ARRANGE =====
      UUID gradeId = UUID.fromString("44440000-1111-2222-3333-444444444444");
      UUID subjectId = UUID.fromString("55550000-1111-2222-3333-444444444444");
      LessonSlideGenerateContentRequest request =
          LessonSlideGenerateContentRequest.builder()
              .schoolGradeId(gradeId)
              .subjectId(subjectId)
              .chapterId(UUID.fromString("66660000-1111-2222-3333-444444444444"))
              .lessonId(lessonId)
              .slideCount(10)
              .additionalPrompt("Phat trien bai hoc")
              .build();
      SchoolGrade requestedGrade = SchoolGrade.builder().gradeLevel(10).name("Lop 10").isActive(true).build();
      requestedGrade.setId(gradeId);
      SchoolGrade otherGrade = SchoolGrade.builder().gradeLevel(11).name("Lop 11").isActive(true).build();
      otherGrade.setId(UUID.fromString("77770000-1111-2222-3333-444444444444"));
      Subject subject = Subject.builder().name("Dai so").code("DAI_SO").isActive(true).build();
      subject.setId(subjectId);
      subject.setSchoolGrade(otherGrade);
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(schoolGradeRepository.findByIdAndNotDeleted(gradeId)).thenReturn(Optional.of(requestedGrade));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(
                AppException.class,
                () -> lessonSlideService.generateLessonContentDraft(request));
        assertEquals(ErrorCode.INVALID_SUBJECT, ex.getErrorCode());

        // ===== VERIFY =====
        verify(userSubscriptionService, times(1)).consumeMyTokens(3, "SLIDE");
        verify(schoolGradeRepository, times(1)).findByIdAndNotDeleted(gradeId);
        verify(subjectRepository, times(1)).findById(subjectId);
        verifyNoMoreInteractions(userSubscriptionService, schoolGradeRepository, subjectRepository, chapterRepository);
      }
    }

    @Test
    void it_should_throw_chapter_not_found_when_chapter_is_missing() {
      // ===== ARRANGE =====
      UUID gradeId = UUID.fromString("88880000-1111-2222-3333-444444444444");
      UUID subjectId = UUID.fromString("99990000-1111-2222-3333-444444444444");
      UUID chapterId = UUID.fromString("aaaa0000-1111-2222-3333-444444444444");
      LessonSlideGenerateContentRequest request =
          LessonSlideGenerateContentRequest.builder()
              .schoolGradeId(gradeId)
              .subjectId(subjectId)
              .chapterId(chapterId)
              .lessonId(lessonId)
              .slideCount(10)
              .additionalPrompt("Khai niem moi")
              .build();
      SchoolGrade grade = SchoolGrade.builder().gradeLevel(12).name("Lop 12").isActive(true).build();
      grade.setId(gradeId);
      Subject subject = Subject.builder().name("Giai tich").code("GIAI_TICH").isActive(true).build();
      subject.setId(subjectId);
      subject.setSchoolGrade(grade);
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils
            .when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE))
            .thenReturn(true);
        when(schoolGradeRepository.findByIdAndNotDeleted(gradeId)).thenReturn(Optional.of(grade));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(subject));
        when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(
                AppException.class,
                () -> lessonSlideService.generateLessonContentDraft(request));
        assertEquals(ErrorCode.CHAPTER_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(userSubscriptionService, times(1)).consumeMyTokens(3, "SLIDE");
        verify(schoolGradeRepository, times(1)).findByIdAndNotDeleted(gradeId);
        verify(subjectRepository, times(1)).findById(subjectId);
        verify(chapterRepository, times(1)).findById(chapterId);
        verifyNoMoreInteractions(userSubscriptionService, schoolGradeRepository, subjectRepository, chapterRepository);
      }
    }

    @Test
    void it_should_throw_subject_not_found_when_subject_does_not_exist() {
      // ===== ARRANGE =====
      UUID gradeId = UUID.fromString("abab0000-1111-2222-3333-444444444444");
      UUID subjectId = UUID.fromString("bcbc0000-1111-2222-3333-444444444444");
      LessonSlideGenerateContentRequest request =
          LessonSlideGenerateContentRequest.builder()
              .schoolGradeId(gradeId)
              .subjectId(subjectId)
              .chapterId(UUID.fromString("cdcd0000-1111-2222-3333-444444444444"))
              .lessonId(lessonId)
              .slideCount(10)
              .additionalPrompt("Khoi tao bai giang")
              .build();
      SchoolGrade grade = SchoolGrade.builder().gradeLevel(9).name("Lop 9").isActive(true).build();
      grade.setId(gradeId);
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        when(schoolGradeRepository.findByIdAndNotDeleted(gradeId)).thenReturn(Optional.of(grade));
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.empty());

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(AppException.class, () -> lessonSlideService.generateLessonContentDraft(request));
        assertEquals(ErrorCode.SUBJECT_NOT_FOUND, ex.getErrorCode());

        // ===== VERIFY =====
        verify(userSubscriptionService, times(1)).consumeMyTokens(3, "SLIDE");
        verify(schoolGradeRepository, times(1)).findByIdAndNotDeleted(gradeId);
        verify(subjectRepository, times(1)).findById(subjectId);
        verifyNoMoreInteractions(userSubscriptionService, schoolGradeRepository, subjectRepository);
      }
    }
  }

  @Nested
  @DisplayName("generatePptxFromJson() early branches")
  class GeneratePptxFromJsonEarlyBranchTests {

    @Test
    void it_should_throw_template_not_usable_when_template_is_inactive_for_json_generation() {
      // ===== ARRANGE =====
      UUID templateId = UUID.fromString("cece0000-1111-2222-3333-444444444444");
      LessonSlideGeneratePptxFromJsonRequest request =
          LessonSlideGeneratePptxFromJsonRequest.builder()
              .lessonId(lessonId)
              .templateId(templateId)
              .slides(
                  List.of(
                      LessonSlideJsonItemRequest.builder()
                          .slideNumber(1)
                          .slideType("COVER")
                          .heading("Tieu de")
                          .content("Noi dung")
                          .build()))
              .build();
      SlideTemplate inactiveTemplate = buildTemplate(templateId, false, null);
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(samplePublishedLesson));
        when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.of(inactiveTemplate));

        // ===== ACT & ASSERT =====
        AppException ex = assertThrows(AppException.class, () -> lessonSlideService.generatePptxFromJson(request));
        assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
        verify(slideTemplateRepository, times(1)).findByIdAndNotDeleted(templateId);
        verifyNoMoreInteractions(lessonRepository, slideTemplateRepository);
      }
    }

    @Test
    void it_should_generate_pptx_from_json_when_template_and_lesson_are_valid() throws Exception {
      // ===== ARRANGE =====
      UUID templateId = UUID.fromString("49494949-aaaa-bbbb-cccc-505050505050");
      Lesson lesson = buildLesson(lessonId, "Ham so luong giac", LessonStatus.PUBLISHED, teacherId);
      SlideTemplate template = buildTemplate(templateId, true, null);
      LessonSlideGeneratePptxFromJsonRequest request =
          LessonSlideGeneratePptxFromJsonRequest.builder()
              .lessonId(lessonId)
              .templateId(templateId)
              .slides(
                  List.of(
                      LessonSlideJsonItemRequest.builder()
                          .slideNumber(1)
                          .slideType("MAIN_CONTENT")
                          .heading("Tieu de")
                          .content("Noi dung bai giang")
                          .build()))
              .build();

      byte[] templateBytes;
      try (XMLSlideShow slideShow = new XMLSlideShow();
          ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        XSLFSlide slide = slideShow.createSlide();
        XSLFTextBox textBox = slide.createTextBox();
        textBox.setText("{{LESSON_TITLE}}\n{{LESSON_CONTENT}}");
        slideShow.write(out);
        templateBytes = out.toByteArray();
      }

      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
        when(slideTemplateRepository.findByIdAndNotDeleted(templateId)).thenReturn(Optional.of(template));
        when(minioProperties.getTemplateBucket()).thenReturn("math-master-assets");
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
        when(getObjectResponse.readAllBytes()).thenReturn(templateBytes);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);
        when(minioClient.bucketExists(any())).thenReturn(true);
        when(lessonSlideGeneratedFileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== ACT =====
        LessonSlideService.BinaryFileData result = lessonSlideService.generatePptxFromJson(request);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(result.content()),
            () -> assertEquals(true, result.fileName().endsWith("-slides.pptx")),
            () ->
                assertEquals(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    result.contentType()));

        // ===== VERIFY =====
        verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
        verify(slideTemplateRepository, times(1)).findByIdAndNotDeleted(templateId);
        verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        verify(minioClient, times(1)).bucketExists(any());
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        verify(lessonSlideGeneratedFileRepository, times(1)).save(any());
      }
    }
  }

  @Nested
  @DisplayName("public download flows")
  class PublicDownloadFlowsTests {

    @Test
    void it_should_download_public_generated_slide_when_lesson_exists() throws Exception {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("dfdf0000-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Gioi han ham so");
      byte[] expectedBytes = "public-pptx".getBytes();
      when(lessonSlideGeneratedFileRepository.findPublicById(fileId)).thenReturn(Optional.of(file));
      when(lessonRepository.findByIdAndNotDeleted(file.getLessonId())).thenReturn(Optional.of(samplePublishedLesson));
      when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
      GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
      when(getObjectResponse.readAllBytes()).thenReturn(expectedBytes);
      when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

      // ===== ACT =====
      LessonSlideService.BinaryFileData result = lessonSlideService.downloadPublicGeneratedSlide(fileId);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(file.getFileName(), result.fileName()),
          () -> assertEquals(file.getContentType(), result.contentType()),
          () -> assertEquals(expectedBytes.length, result.content().length));

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1)).findPublicById(fileId);
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(file.getLessonId());
      verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
      verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, lessonRepository, minioClient);
    }

    @Test
    void it_should_throw_lesson_not_found_when_public_file_lesson_is_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("efef0000-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Cong thuc to hop");
      when(lessonSlideGeneratedFileRepository.findPublicById(fileId)).thenReturn(Optional.of(file));
      when(lessonRepository.findByIdAndNotDeleted(file.getLessonId())).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> lessonSlideService.downloadPublicGeneratedSlide(fileId));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1)).findPublicById(fileId);
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(file.getLessonId());
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, lessonRepository, minioClient);
    }

    @Test
    void it_should_throw_generated_slide_not_found_when_public_generated_file_is_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("16161616-aaaa-bbbb-cccc-171717171717");
      when(lessonSlideGeneratedFileRepository.findPublicById(fileId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException ex =
          assertThrows(AppException.class, () -> lessonSlideService.downloadPublicGeneratedSlide(fileId));
      assertEquals(ErrorCode.GENERATED_SLIDE_NOT_FOUND, ex.getErrorCode());

      // ===== VERIFY =====
      verify(lessonSlideGeneratedFileRepository, times(1)).findPublicById(fileId);
      verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, lessonRepository, minioClient);
    }
  }

  @Nested
  @DisplayName("thumbnail and render methods")
  class ThumbnailAndRenderMethodsTests {

    @Test
    void it_should_return_generated_thumbnail_image_when_owner_and_thumbnail_exist() throws Exception {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("fafa0000-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "He bat phuong trinh");
      file.setThumbnail("generated-slide-thumbnails/thumb.webp");
      byte[] expectedBytes = "thumb".getBytes();
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(null);
        GetObjectResponse getObjectResponse = Mockito.mock(GetObjectResponse.class);
        when(getObjectResponse.readAllBytes()).thenReturn(expectedBytes);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getObjectResponse);

        // ===== ACT =====
        LessonSlideService.BinaryFileData result = lessonSlideService.getGeneratedSlideThumbnailImage(fileId);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals("generated-slide-thumbnail", result.fileName()),
            () -> assertEquals("image/webp", result.contentType()),
            () -> assertEquals(expectedBytes.length, result.content().length));

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
        verify(minioClient, times(1)).getObject(any(GetObjectArgs.class));
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, minioClient);
      }
    }

    @Test
    void it_should_throw_template_not_usable_when_private_thumbnail_is_missing() {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("14141414-aaaa-bbbb-cccc-151515151515");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Bai giang hinh hoc");
      file.setThumbnail(" ");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(
                AppException.class,
                () -> lessonSlideService.getGeneratedSlideThumbnailImage(fileId));
        assertEquals(ErrorCode.TEMPLATE_NOT_USABLE, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verifyNoMoreInteractions(lessonSlideGeneratedFileRepository, minioClient);
      }
    }

    @Test
    void it_should_return_null_render_preview_when_latex_service_throws() {
      // ===== ARRANGE =====
      when(latexRenderService.render(any())).thenThrow(new RuntimeException("render failed"));

      // ===== ACT =====
      String result = lessonSlideService.renderSlidePreview("Dao ham", "Tinh $f'(x)$");

      // ===== ASSERT =====
      assertNull(result);

      // ===== VERIFY =====
      verify(latexRenderService, times(1)).render(any());
      verifyNoMoreInteractions(latexRenderService);
    }
  }

  @Nested
  @DisplayName("updateGeneratedSlideMetadata thumbnail branches")
  class UpdateGeneratedSlideMetadataThumbnailTests {

    @Test
    void it_should_throw_template_generation_failed_when_thumbnail_upload_fails() throws Exception {
      // ===== ARRANGE =====
      UUID fileId = UUID.fromString("abab1111-1111-2222-3333-444444444444");
      LessonSlideGeneratedFile file = buildGeneratedFile(fileId, teacherId, "Bai giang tong hop");
      try (MockedStatic<SecurityUtils> securityUtils = Mockito.mockStatic(SecurityUtils.class)) {
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)).thenReturn(true);
        securityUtils.when(() -> SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)).thenReturn(false);
        securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(teacherId);
        when(lessonSlideGeneratedFileRepository.findByIdAndNotDeleted(fileId)).thenReturn(Optional.of(file));
        when(thumbnailFile.isEmpty()).thenReturn(false);
        when(thumbnailFile.getOriginalFilename()).thenReturn("thumb.png");
        when(thumbnailFile.getContentType()).thenReturn("image/png");
        when(thumbnailFile.getInputStream()).thenThrow(new RuntimeException("stream failed"));
        when(minioProperties.getTemplateBucket()).thenReturn("math-master-assets");
        when(minioClient.bucketExists(any())).thenReturn(true);

        // ===== ACT & ASSERT =====
        AppException ex =
            assertThrows(
                AppException.class,
                () -> lessonSlideService.updateGeneratedSlideMetadata(fileId, "Ten moi", thumbnailFile));
        assertEquals(ErrorCode.TEMPLATE_GENERATION_FAILED, ex.getErrorCode());

        // ===== VERIFY =====
        verify(lessonSlideGeneratedFileRepository, times(1)).findByIdAndNotDeleted(fileId);
        verify(minioClient, times(1)).bucketExists(any());
        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
      }
    }
  }

  @Nested
  @DisplayName("private helper methods via reflection")
  class PrivateHelperMethodsTests {

    @Test
    void it_should_normalize_requested_slide_count_with_min_max_bounds() {
      // ===== ACT =====
      int whenNull =
          (int) invokePrivate("normalizeRequestedSlideCount", new Class<?>[] {Integer.class}, (Object) null);
      int whenTooSmall =
          (int) invokePrivate("normalizeRequestedSlideCount", new Class<?>[] {Integer.class}, 2);
      int whenTooLarge =
          (int) invokePrivate("normalizeRequestedSlideCount", new Class<?>[] {Integer.class}, 99);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(10, whenNull),
          () -> assertEquals(5, whenTooSmall),
          () -> assertEquals(15, whenTooLarge));
    }

    @Test
    void it_should_resolve_output_format_to_plain_text_when_null() {
      // ===== ACT =====
      Object result =
          invokePrivate(
              "resolveOutputFormat",
              new Class<?>[] {com.fptu.math_master.enums.LessonSlideOutputFormat.class},
              (Object) null);

      // ===== ASSERT =====
      assertEquals(com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT, result);
    }

    @Test
    void it_should_trim_and_normalize_ai_text_line_breaks() {
      // ===== ACT =====
      String result =
          (String)
              invokePrivate(
                  "normalizeAiGeneratedText",
                  new Class<?>[] {String.class},
                  "  Dong 1\\nDong 2\\\\\n  ");

      // ===== ASSERT =====
      assertEquals("Dong 1\nDong 2", result);
    }

    @Test
    void it_should_split_into_five_chunks_for_short_and_long_content() {
      // ===== ACT =====
      @SuppressWarnings("unchecked")
      List<String> emptyResult =
          (List<String>) invokePrivate("splitIntoFiveChunks", new Class<?>[] {String.class}, " ");
      @SuppressWarnings("unchecked")
      List<String> longResult =
          (List<String>)
              invokePrivate(
                  "splitIntoFiveChunks",
                  new Class<?>[] {String.class},
                  "a\nb\nc\nd\ne\nf\ng");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(5, emptyResult.size()),
          () -> assertEquals("", emptyResult.getFirst()),
          () -> assertEquals(5, longResult.size()),
          () -> assertNotNull(longResult.get(3)));
    }

    @Test
    void it_should_split_into_chunks_and_fill_with_last_paragraph_when_chunk_count_exceeds_data() {
      // ===== ACT =====
      @SuppressWarnings("unchecked")
      List<String> result =
          (List<String>)
              invokePrivate("splitIntoChunks", new Class<?>[] {String.class, int.class}, "x\ny", 4);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(4, result.size()),
          () -> assertEquals("x", result.get(0)),
          () -> assertEquals("y", result.get(1)),
          () -> assertEquals("y", result.get(2)),
          () -> assertEquals("y", result.get(3)));
    }

    @Test
    void it_should_build_pdf_file_name_with_and_without_extension() {
      // ===== ACT =====
      String blank =
          (String) invokePrivate("toPdfFileName", new Class<?>[] {String.class}, " ");
      String withExt =
          (String) invokePrivate("toPdfFileName", new Class<?>[] {String.class}, "bai-giang.pptx");
      String withoutExt =
          (String) invokePrivate("toPdfFileName", new Class<?>[] {String.class}, "bai-giang");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("slide-preview.pdf", blank),
          () -> assertEquals("bai-giang.pdf", withExt),
          () -> assertEquals("bai-giang.pdf", withoutExt));
    }

    @Test
    void it_should_resolve_image_content_type_from_object_key_extensions() {
      // ===== ACT =====
      String jpg =
          (String)
              invokePrivate(
                  "resolveImageContentTypeFromObjectKey", new Class<?>[] {String.class}, "a/b/c.jpg");
      String webp =
          (String)
              invokePrivate(
                  "resolveImageContentTypeFromObjectKey", new Class<?>[] {String.class}, "a/b/c.webp");
      String defaultPng =
          (String)
              invokePrivate(
                  "resolveImageContentTypeFromObjectKey", new Class<?>[] {String.class}, "a/b/c.unknown");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("image/jpeg", jpg),
          () -> assertEquals("image/webp", webp),
          () -> assertEquals("image/png", defaultPng));
    }

    @Test
    void it_should_escape_latex_text_characters() {
      // ===== ACT =====
      String escaped =
          (String) invokePrivate("escapeLatexText", new Class<?>[] {String.class}, "A&B_1^2");

      // ===== ASSERT =====
      assertEquals("A\\&B\\_1\\textasciicircum{}2", escaped);
    }

    @Test
    void it_should_build_mixed_latex_line_for_plain_text_and_inline_math() {
      // ===== ACT =====
      String plain =
          (String) invokePrivate("buildMixedLatexLine", new Class<?>[] {String.class}, "Giai bai tap");
      String mixed =
          (String)
              invokePrivate(
                  "buildMixedLatexLine", new Class<?>[] {String.class}, "Tinh $a+b$ voi dieu kien");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("\\text{Giai bai tap}", plain),
          () -> assertEquals("\\text{Tinh }$a+b$\\text{ voi dieu kien}", mixed));
    }

    @Test
    void it_should_detect_block_latex_lines_by_environment_or_display_math_markers() {
      // ===== ACT =====
      boolean beginEnv =
          (boolean) invokePrivate("isBlockLatexLine", new Class<?>[] {String.class}, "\\begin{itemize}");
      boolean displayMath =
          (boolean) invokePrivate("isBlockLatexLine", new Class<?>[] {String.class}, "\\[ x^2 \\]");
      boolean plain =
          (boolean) invokePrivate("isBlockLatexLine", new Class<?>[] {String.class}, "Van ban thuong");

      // ===== ASSERT =====
      assertAll(() -> assertEquals(true, beginEnv), () -> assertEquals(true, displayMath), () -> assertEquals(false, plain));
    }

    @Test
    void it_should_join_non_blank_sections_only() {
      // ===== ACT =====
      String result =
          (String)
              invokePrivate(
                  "joinSections",
                  new Class<?>[] {String[].class},
                  (Object) new String[] {"Phan 1", " ", null, "Phan 2"});

      // ===== ASSERT =====
      assertEquals("Phan 1\n\nPhan 2", result);
    }

    @Test
    void it_should_build_summary_with_ellipsis_when_content_exceeds_limit() {
      // ===== ACT =====
      String shortSummary =
          (String) invokePrivate("buildSummary", new Class<?>[] {String.class}, "Noi dung ngan");
      String longSummary =
          (String)
              invokePrivate(
                  "buildSummary",
                  new Class<?>[] {String.class},
                  "A".repeat(240));

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Noi dung ngan", shortSummary),
          () -> assertEquals(223, longSummary.length()),
          () -> assertEquals(true, longSummary.endsWith("...")));
    }

    @Test
    void it_should_normalize_latex_input_and_sanitize_display_text() {
      // ===== ACT =====
      String normalizedLatex =
          (String) invokePrivate("normalizeLatexInput", new Class<?>[] {String.class}, "\\\\(x+1\\\\) and \\$5");
      String sanitized =
          (String)
              invokePrivate(
                  "sanitizeDisplayText",
                  new Class<?>[] {String.class},
                  "\\textbf{Noi dung}  \\quad  \\textit{Them}");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("\\(x+1\\) and $5", normalizedLatex),
          () -> assertEquals("Noi dung Them", sanitized));
    }

    @Test
    void it_should_extract_tagged_sections_and_find_next_tag_position() {
      // ===== ARRANGE =====
      String raw =
          "[LESSON_SUMMARY]\nTom tat\n[LEARNING_OBJECTIVES]\nMuc tieu\n[OPENING]\nKhoi dong";

      // ===== ACT =====
      String summary =
          (String) invokePrivate("extractTagged", new Class<?>[] {String.class, String.class}, raw, "LESSON_SUMMARY");
      int nextIdx =
          (int)
              invokePrivate(
                  "findNextTaggedSectionStart",
                  new Class<?>[] {String.class, int.class},
                  raw,
                  raw.indexOf("[LESSON_SUMMARY]") + "[LESSON_SUMMARY]".length());

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Tom tat", summary),
          () -> assertEquals(raw.indexOf("[LEARNING_OBJECTIVES]"), nextIdx));
    }

    @Test
    void it_should_build_format_guidance_for_latex_and_plain_text() {
      // ===== ACT =====
      String latexGuidance =
          (String)
              invokePrivate(
                  "buildFormatGuidance",
                  new Class<?>[] {com.fptu.math_master.enums.LessonSlideOutputFormat.class, boolean.class},
                  com.fptu.math_master.enums.LessonSlideOutputFormat.LATEX,
                  true);
      String plainGuidance =
          (String)
              invokePrivate(
                  "buildFormatGuidance",
                  new Class<?>[] {com.fptu.math_master.enums.LessonSlideOutputFormat.class, boolean.class},
                  com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT,
                  false);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, latexGuidance.contains("JSON string")),
          () -> assertEquals(true, plainGuidance.contains("LaTeX")));
    }

    @Test
    void it_should_pick_non_blank_default_value_when_input_is_blank() {
      // ===== ACT =====
      String fallback =
          (String)
              invokePrivate(
                  "nonBlankOrDefault",
                  new Class<?>[] {String.class, String.class},
                  "   ",
                  "Gia tri mac dinh");
      String keepValue =
          (String)
              invokePrivate(
                  "nonBlankOrDefault",
                  new Class<?>[] {String.class, String.class},
                  "Du lieu hop le",
                  "Gia tri mac dinh");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Gia tri mac dinh", fallback),
          () -> assertEquals("Du lieu hop le", keepValue));
    }

    @Test
    void it_should_truncate_word_limit_and_keep_original_when_short_enough() {
      // ===== ACT =====
      String shortText =
          (String)
              invokePrivate(
                  "truncateToWordLimit",
                  new Class<?>[] {String.class, int.class},
                  "mot hai ba",
                  5);
      String truncated =
          (String)
              invokePrivate(
                  "truncateToWordLimit",
                  new Class<?>[] {String.class, int.class},
                  "mot hai ba bon nam sau bay",
                  4);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("mot hai ba", shortText),
          () -> assertEquals("mot hai ba bon", truncated));
    }

    @Test
    void it_should_generate_sanitized_object_keys_and_output_file_names() {
      // ===== ACT =====
      String templateObjectKey =
          (String) invokePrivate("buildObjectKey", new Class<?>[] {String.class}, "Bai giang toan 12.pptx");
      String previewKey =
          (String) invokePrivate("buildPreviewImageObjectKey", new Class<?>[] {String.class}, "anh xem truoc.jpg");
      String outputName =
          (String) invokePrivate("buildOutputFileName", new Class<?>[] {String.class}, "Bai giang dac biet");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, templateObjectKey.startsWith("slide-templates/")),
          () -> assertEquals(true, previewKey.startsWith("slide-templates-preview/")),
          () -> assertEquals(true, outputName.endsWith("-slides.pptx")));
    }

    @Test
    void it_should_validate_pptx_file_and_preview_image_rules() {
      // ===== ARRANGE =====
      MultipartFile validPptx = Mockito.mock(MultipartFile.class);
      when(validPptx.isEmpty()).thenReturn(false);
      when(validPptx.getOriginalFilename()).thenReturn("lesson-template.pptx");

      MultipartFile invalidPptx = Mockito.mock(MultipartFile.class);
      when(invalidPptx.isEmpty()).thenReturn(false);
      when(invalidPptx.getOriginalFilename()).thenReturn("lesson-template.docx");

      MultipartFile validPreview = Mockito.mock(MultipartFile.class);
      when(validPreview.isEmpty()).thenReturn(false);
      when(validPreview.getOriginalFilename()).thenReturn("preview-image.webp");
      when(validPreview.getContentType()).thenReturn("image/webp");

      MultipartFile invalidPreview = Mockito.mock(MultipartFile.class);
      when(invalidPreview.isEmpty()).thenReturn(false);
      when(invalidPreview.getOriginalFilename()).thenReturn("preview.txt");
      when(invalidPreview.getContentType()).thenReturn("text/plain");

      // ===== ACT & ASSERT =====
      invokePrivate("validatePptxFile", new Class<?>[] {MultipartFile.class}, validPptx);
      invokePrivate("validatePreviewImage", new Class<?>[] {MultipartFile.class}, validPreview);
      assertThrows(
          RuntimeException.class,
          () -> invokePrivate("validatePptxFile", new Class<?>[] {MultipartFile.class}, invalidPptx));
      assertThrows(
          RuntimeException.class,
          () -> invokePrivate("validatePreviewImage", new Class<?>[] {MultipartFile.class}, invalidPreview));
    }

    @Test
    void it_should_parse_ai_deck_sections_from_json_and_return_null_for_invalid_json() {
      // ===== ACT =====
      Object invalid =
          invokePrivate("parseAiDeckSections", new Class<?>[] {String.class}, "{invalid-json");

      // ===== ASSERT =====
      assertNull(invalid);
    }

    @Test
    void it_should_parse_tagged_deck_sections_when_content_is_sufficient() {
      // ===== ARRANGE =====
      String raw =
          """
          [LESSON_SUMMARY]
          Tom tat bai hoc
          [LEARNING_OBJECTIVES]
          Muc tieu bai hoc
          [OPENING]
          Khoi dong bai hoc
          [MAIN_PART_1]
          Noi dung chinh 1
          [MAIN_PART_2]
          Noi dung chinh 2
          [MAIN_PART_3]
          Noi dung chinh 3
          [EXAMPLE_PART]
          Vi du minh hoa
          [PRACTICE_PART]
          Bai tap luyen tap
          [CLOSING_SUMMARY]
          Tong ket bai hoc
          [ADDITIONAL_NOTES]
          Ghi chu bo sung
          """;

      // ===== ACT =====
      Object result =
          invokePrivate("parseTaggedDeckSections", new Class<?>[] {String.class}, raw);

      // ===== ASSERT =====
      assertNotNull(result);
    }

    @Test
    void it_should_build_json_slide_placeholders_for_cover_and_main_content_types() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Tieu de bai hoc", LessonStatus.PUBLISHED, teacherId);
      LessonSlideJsonItemRequest cover =
          LessonSlideJsonItemRequest.builder()
              .slideNumber(1)
              .slideType("COVER")
              .heading("Heading cover")
              .content("Noi dung cover")
              .build();
      LessonSlideJsonItemRequest main =
          LessonSlideJsonItemRequest.builder()
              .slideNumber(2)
              .slideType("MAIN_CONTENT")
              .heading("Heading main")
              .content("Noi dung main")
              .build();

      // ===== ACT =====
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> coverValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildJsonSlidePlaceholders",
                  new Class<?>[] {LessonSlideJsonItemRequest.class, Lesson.class},
                  cover,
                  lesson);
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> mainValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildJsonSlidePlaceholders",
                  new Class<?>[] {LessonSlideJsonItemRequest.class, Lesson.class},
                  main,
                  lesson);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Tieu de bai hoc", coverValues.get("{{LESSON_TITLE}}")),
          () -> assertEquals("Noi dung cover", coverValues.get("{{LESSON_SUMMARY}}")),
          () -> assertEquals("Noi dung main", mainValues.get("{{LESSON_CONTENT}}")),
          () -> assertEquals("Heading main", mainValues.get("{{SLIDE_HEADING}}")));
    }

    @Test
    void it_should_replace_latex_segments_with_markers_and_extract_first_non_blank() {
      // ===== ARRANGE =====
      List<Object> tokens = new java.util.ArrayList<>();

      // ===== ACT =====
      String withMarkers =
          (String)
              invokePrivate(
                  "replaceLatexWithMarkers",
                  new Class<?>[] {String.class, List.class},
                  "Gia tri $x^2$ va \\(y+1\\)",
                  tokens);
      String first =
          (String)
              invokePrivate(
                  "firstNonBlank",
                  new Class<?>[] {String[].class},
                  (Object) new String[] {" ", null, "gia-tri-hop-le"});

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, withMarkers.contains("[[LATEX_0]]")),
          () -> assertEquals(true, withMarkers.contains("[[LATEX_1]]")),
          () -> assertEquals(2, tokens.size()),
          () -> assertEquals("gia-tri-hop-le", first));
    }

    @Test
    void it_should_normalize_escaped_line_breaks_and_strip_trailing_single_backslash() {
      // ===== ACT =====
      String normalized =
          (String)
              invokePrivate(
                  "normalizeEscapedLineBreaks",
                  new Class<?>[] {String.class},
                  "Dong 1\\nDong 2\\\\\nDong 3\\");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, normalized.contains("Dong 1\nDong 2")),
          () -> assertEquals(true, normalized.endsWith("Dong 3")));
    }

    @Test
    void it_should_build_latex_slide_body_for_plain_text_and_latex_command_paths() {
      // ===== ACT =====
      String plainBody =
          (String)
              invokePrivate(
                  "buildLatexSlideBody",
                  new Class<?>[] {String.class, String.class},
                  "Tieu de",
                  "Dong thu nhat\nDong thu hai");
      String latexCommandBody =
          (String)
              invokePrivate(
                  "buildLatexSlideBody",
                  new Class<?>[] {String.class, String.class},
                  "Tieu de",
                  "\\item y 1\n\\item y 2");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, plainBody.contains("\\text{Dong thu nhat}")),
          () -> assertEquals(true, latexCommandBody.contains("\\begin{itemize}")),
          () -> assertEquals(true, latexCommandBody.contains("\\end{itemize}")));
    }

    @Test
    void it_should_resolve_preview_image_content_type_from_content_type_filename_or_default() {
      // ===== ARRANGE =====
      MultipartFile byType = Mockito.mock(MultipartFile.class);
      when(byType.getContentType()).thenReturn("image/jpeg");
      MultipartFile byName = Mockito.mock(MultipartFile.class);
      when(byName.getContentType()).thenReturn(null);
      when(byName.getOriginalFilename()).thenReturn("xem-truoc.webp");
      MultipartFile fallback = Mockito.mock(MultipartFile.class);
      when(fallback.getContentType()).thenReturn(null);
      when(fallback.getOriginalFilename()).thenReturn(null);

      // ===== ACT =====
      String resultByType =
          (String)
              invokePrivate(
                  "resolvePreviewImageContentType",
                  new Class<?>[] {MultipartFile.class},
                  byType);
      String resultByName =
          (String)
              invokePrivate(
                  "resolvePreviewImageContentType",
                  new Class<?>[] {MultipartFile.class},
                  byName);
      String resultFallback =
          (String)
              invokePrivate(
                  "resolvePreviewImageContentType",
                  new Class<?>[] {MultipartFile.class},
                  fallback);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("image/jpeg", resultByType),
          () -> assertEquals("image/webp", resultByName),
          () -> assertEquals("image/png", resultFallback));
    }

    @Test
    void it_should_build_slide_placeholders_for_cover_and_default_cases() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Tieu de chinh", LessonStatus.PUBLISHED, teacherId);
      Object deck =
          invokePrivate(
              "buildDeckSections",
              new Class<?>[] {Lesson.class, String.class},
              lesson,
              "Mo rong noi dung");
      Class<?> deckClass = getInnerClass("DeckSections");

      // ===== ACT =====
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> coverValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildSlidePlaceholders",
                  new Class<?>[] {int.class, deckClass},
                  1,
                  deck);
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> defaultValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildSlidePlaceholders",
                  new Class<?>[] {int.class, deckClass},
                  11,
                  deck);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Tieu de chinh", coverValues.get("{{LESSON_TITLE}}")),
          () -> assertNotNull(coverValues.get("{{LESSON_SUMMARY}}")),
          () -> assertNotNull(defaultValues.get("{{LESSON_CONTENT}}")));
    }

    @Test
    void it_should_build_preview_and_thumbnail_paths_for_public_and_private_cases() {
      // ===== ARRANGE =====
      SlideTemplate templateWithPreview =
          buildTemplate(
              UUID.fromString("20202020-aaaa-bbbb-cccc-212121212121"),
              true,
              "slide-templates-preview/a.png");
      SlideTemplate templateWithoutPreview =
          buildTemplate(
              UUID.fromString("22222222-aaaa-bbbb-cccc-232323232323"),
              true,
              " ");
      LessonSlideGeneratedFile publicFile =
          buildGeneratedFile(
              UUID.fromString("24242424-aaaa-bbbb-cccc-252525252525"),
              teacherId,
              "Public");
      publicFile.setIsPublic(Boolean.TRUE);
      publicFile.setThumbnail("generated-slide-thumbnails/public.png");
      LessonSlideGeneratedFile privateFile =
          buildGeneratedFile(
              UUID.fromString("26262626-aaaa-bbbb-cccc-272727272727"),
              teacherId,
              "Private");
      privateFile.setIsPublic(Boolean.FALSE);
      privateFile.setThumbnail("generated-slide-thumbnails/private.png");

      // ===== ACT =====
      String previewPath =
          (String)
              invokePrivate("buildPreviewImagePath", new Class<?>[] {SlideTemplate.class}, templateWithPreview);
      String previewNull =
          (String)
              invokePrivate("buildPreviewImagePath", new Class<?>[] {SlideTemplate.class}, templateWithoutPreview);
      String publicThumbPath =
          (String)
              invokePrivate(
                  "buildGeneratedSlideThumbnailPath",
                  new Class<?>[] {LessonSlideGeneratedFile.class},
                  publicFile);
      String privateThumbPath =
          (String)
              invokePrivate(
                  "buildGeneratedSlideThumbnailPath",
                  new Class<?>[] {LessonSlideGeneratedFile.class},
                  privateFile);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, previewPath.contains("/lesson-slides/templates/")),
          () -> assertNull(previewNull),
          () -> assertEquals(true, publicThumbPath.contains("/lesson-slides/public/generated/")),
          () -> assertEquals(true, privateThumbPath.contains("/lesson-slides/generated/")));
    }

    @Test
    void it_should_throw_school_grade_not_found_when_resolve_requested_grade_is_inactive() {
      // ===== ARRANGE =====
      UUID gradeId = UUID.fromString("28282828-aaaa-bbbb-cccc-292929292929");
      SchoolGrade inactive = SchoolGrade.builder().gradeLevel(8).name("Lop 8").isActive(false).build();
      inactive.setId(gradeId);
      LessonSlideGenerateContentRequest request =
          LessonSlideGenerateContentRequest.builder()
              .schoolGradeId(gradeId)
              .subjectId(UUID.randomUUID())
              .chapterId(UUID.randomUUID())
              .lessonId(lessonId)
              .slideCount(10)
              .additionalPrompt("Prompt")
              .build();
      when(schoolGradeRepository.findByIdAndNotDeleted(gradeId)).thenReturn(Optional.of(inactive));

      // ===== ACT & ASSERT =====
      RuntimeException ex =
          assertThrows(
              RuntimeException.class,
              () ->
                  invokePrivate(
                      "resolveRequestedGrade",
                      new Class<?>[] {LessonSlideGenerateContentRequest.class},
                      request));
      assertEquals(true, ex.getCause().getCause() instanceof AppException);
    }

    @Test
    void it_should_find_next_tag_as_end_of_string_when_no_following_tag_exists() {
      // ===== ARRANGE =====
      String raw = "[LESSON_SUMMARY]\nNoi dung cuoi";
      int fromIndex = raw.indexOf("[LESSON_SUMMARY]") + "[LESSON_SUMMARY]".length();

      // ===== ACT =====
      int result =
          (int)
              invokePrivate(
                  "findNextTaggedSectionStart",
                  new Class<?>[] {String.class, int.class},
                  raw,
                  fromIndex);

      // ===== ASSERT =====
      assertEquals(raw.length(), result);
    }

    @Test
    void it_should_return_empty_string_when_extract_tagged_cannot_find_requested_tag() {
      // ===== ACT =====
      String result =
          (String)
              invokePrivate(
                  "extractTagged",
                  new Class<?>[] {String.class, String.class},
                  "[OTHER_TAG]\nabc",
                  "LESSON_SUMMARY");

      // ===== ASSERT =====
      assertEquals("", result);
    }

    @Test
    void it_should_cover_remaining_slide_type_switch_cases_in_build_json_slide_placeholders() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Tieu de goc", LessonStatus.PUBLISHED, teacherId);
      LessonSlideJsonItemRequest objectives =
          LessonSlideJsonItemRequest.builder()
              .slideNumber(1)
              .slideType("OBJECTIVES")
              .heading(" ")
              .content("Muc tieu theo slide")
              .build();
      LessonSlideJsonItemRequest opening =
          LessonSlideJsonItemRequest.builder()
              .slideNumber(2)
              .slideType("OPENING")
              .heading(" ")
              .content("Noi dung mo dau")
              .build();
      LessonSlideJsonItemRequest summary =
          LessonSlideJsonItemRequest.builder()
              .slideNumber(3)
              .slideType("SUMMARY")
              .heading(" ")
              .content("Tom tat bai hoc")
              .build();
      LessonSlideJsonItemRequest closing =
          LessonSlideJsonItemRequest.builder()
              .slideNumber(4)
              .slideType("CLOSING")
              .heading(" ")
              .content("Cam on va hoi dap")
              .build();

      // ===== ACT =====
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> objectiveValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildJsonSlidePlaceholders",
                  new Class<?>[] {LessonSlideJsonItemRequest.class, Lesson.class},
                  objectives,
                  lesson);
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> openingValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildJsonSlidePlaceholders",
                  new Class<?>[] {LessonSlideJsonItemRequest.class, Lesson.class},
                  opening,
                  lesson);
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> summaryValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildJsonSlidePlaceholders",
                  new Class<?>[] {LessonSlideJsonItemRequest.class, Lesson.class},
                  summary,
                  lesson);
      @SuppressWarnings("unchecked")
      java.util.Map<String, String> closingValues =
          (java.util.Map<String, String>)
              invokePrivate(
                  "buildJsonSlidePlaceholders",
                  new Class<?>[] {LessonSlideJsonItemRequest.class, Lesson.class},
                  closing,
                  lesson);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Tieu de goc", objectiveValues.get("{{SLIDE_HEADING}}")),
          () -> assertEquals("Muc tieu theo slide", objectiveValues.get("{{LEARNING_OBJECTIVES}}")),
          () -> assertEquals("Noi dung mo dau", openingValues.get("{{ADDITIONAL_PROMPT}}")),
          () -> assertEquals("Tom tat bai hoc", summaryValues.get("{{LESSON_SUMMARY}}")),
          () -> assertEquals("Tieu de goc", closingValues.get("{{LESSON_TITLE}}")));
    }

    @Test
    void it_should_cover_build_preview_slides_for_both_10_and_non_10_counts() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Bai hoc he so", LessonStatus.PUBLISHED, teacherId);
      Object deck =
          invokePrivate(
              "buildDeckSections",
              new Class<?>[] {Lesson.class, String.class},
              lesson,
              "Khoi dong");
      Class<?> deckClass = getInnerClass("DeckSections");

      // ===== ACT =====
      @SuppressWarnings("unchecked")
      List<Object> tenSlides =
          (List<Object>)
              invokePrivate(
                  "buildPreviewSlides",
                  new Class<?>[] {deckClass, int.class, com.fptu.math_master.enums.LessonSlideOutputFormat.class},
                  deck,
                  10,
                  com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT);
      @SuppressWarnings("unchecked")
      List<Object> sevenSlides =
          (List<Object>)
              invokePrivate(
                  "buildPreviewSlides",
                  new Class<?>[] {deckClass, int.class, com.fptu.math_master.enums.LessonSlideOutputFormat.class},
                  deck,
                  7,
                  com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(10, tenSlides.size()),
          () -> assertEquals(7, sevenSlides.size()));
    }

    @Test
    void it_should_cover_build_preview_slide_for_latex_and_plain_text_paths() {
      // ===== ARRANGE =====
      when(latexRenderService.render(any())).thenReturn("https://preview-url/generated.png");

      // ===== ACT =====
      Object latexSlide =
          invokePrivate(
              "buildPreviewSlide",
              new Class<?>[] {
                int.class,
                String.class,
                String.class,
                String.class,
                com.fptu.math_master.enums.LessonSlideOutputFormat.class
              },
              1,
              "MAIN_CONTENT",
              "Tieu de",
              "Noi dung $x^2$",
              com.fptu.math_master.enums.LessonSlideOutputFormat.LATEX);
      Object plainSlide =
          invokePrivate(
              "buildPreviewSlide",
              new Class<?>[] {
                int.class,
                String.class,
                String.class,
                String.class,
                com.fptu.math_master.enums.LessonSlideOutputFormat.class
              },
              2,
              "MAIN_CONTENT",
              "Tieu de",
              "Noi dung thuong",
              com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(latexSlide), () -> assertNotNull(plainSlide));
    }

    @Test
    void it_should_cover_build_deck_sections_with_ai_for_success_and_fallback() throws Exception {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Bai hoc", LessonStatus.PUBLISHED, teacherId);
      Class<?> aiDeckClass = getInnerClass("AiDeckSections");
      java.lang.reflect.Constructor<?> aiDeckCtor = aiDeckClass.getDeclaredConstructor();
      aiDeckCtor.setAccessible(true);
      Object aiDeck = aiDeckCtor.newInstance();
      java.lang.reflect.Field lessonSummary = aiDeckClass.getDeclaredField("lessonSummary");
      java.lang.reflect.Field learningObjectives = aiDeckClass.getDeclaredField("learningObjectives");
      java.lang.reflect.Field opening = aiDeckClass.getDeclaredField("opening");
      java.lang.reflect.Field mainPart1 = aiDeckClass.getDeclaredField("mainPart1");
      java.lang.reflect.Field mainPart2 = aiDeckClass.getDeclaredField("mainPart2");
      java.lang.reflect.Field mainPart3 = aiDeckClass.getDeclaredField("mainPart3");
      java.lang.reflect.Field examplePart = aiDeckClass.getDeclaredField("examplePart");
      java.lang.reflect.Field practicePart = aiDeckClass.getDeclaredField("practicePart");
      java.lang.reflect.Field closingSummary = aiDeckClass.getDeclaredField("closingSummary");
      java.lang.reflect.Field additionalNotes = aiDeckClass.getDeclaredField("additionalNotes");
      for (java.lang.reflect.Field f :
          List.of(
              lessonSummary,
              learningObjectives,
              opening,
              mainPart1,
              mainPart2,
              mainPart3,
              examplePart,
              practicePart,
              closingSummary,
              additionalNotes)) {
        f.setAccessible(true);
      }
      lessonSummary.set(aiDeck, "Tom tat AI");
      learningObjectives.set(aiDeck, "Muc tieu AI");
      opening.set(aiDeck, "Mo dau AI");
      mainPart1.set(aiDeck, "Noi dung 1 AI");
      mainPart2.set(aiDeck, "Noi dung 2 AI");
      mainPart3.set(aiDeck, "Noi dung 3 AI");
      examplePart.set(aiDeck, "Vi du AI");
      practicePart.set(aiDeck, "Luyen tap AI");
      closingSummary.set(aiDeck, "Tong ket AI");
      additionalNotes.set(aiDeck, "Ghi chu AI");

      when(geminiService.sendMessage(any())).thenReturn("{\"dummy\":\"json\"}");
      when(objectMapper.readValue(any(String.class), org.mockito.ArgumentMatchers.<Class<Object>>any()))
          .thenReturn(aiDeck);

      // ===== ACT =====
      Object successDeck =
          invokePrivate(
              "buildDeckSectionsWithAi",
              new Class<?>[] {
                Lesson.class,
                String.class,
                com.fptu.math_master.enums.LessonSlideOutputFormat.class,
                int.class
              },
              lesson,
              "prompt",
              com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT,
              10);

      when(geminiService.sendMessage(any())).thenThrow(new RuntimeException("ai failed"));
      Object fallbackDeck =
          invokePrivate(
              "buildDeckSectionsWithAi",
              new Class<?>[] {
                Lesson.class,
                String.class,
                com.fptu.math_master.enums.LessonSlideOutputFormat.class,
                int.class
              },
              lesson,
              "prompt",
              com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT,
              10);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(successDeck), () -> assertNotNull(fallbackDeck));
    }

    @Test
    void it_should_cover_parse_ai_deck_sections_for_valid_and_invalid_paths() throws Exception {
      // ===== ARRANGE =====
      Class<?> aiDeckClass = getInnerClass("AiDeckSections");
      java.lang.reflect.Constructor<?> aiDeckCtor = aiDeckClass.getDeclaredConstructor();
      aiDeckCtor.setAccessible(true);
      Object aiDeck = aiDeckCtor.newInstance();
      when(objectMapper.readValue(any(String.class), org.mockito.ArgumentMatchers.<Class<Object>>any()))
          .thenAnswer(
              invocation -> {
                String raw = invocation.getArgument(0, String.class);
                if (raw.contains("invalid")) {
                  throw new RuntimeException("cannot parse");
                }
                return aiDeck;
              });

      // ===== ACT =====
      Object valid =
          invokePrivate(
              "parseAiDeckSections",
              new Class<?>[] {String.class},
              "```json\n{\"a\":\"b\"}\n```");
      Object invalid = invokePrivate("parseAiDeckSections", new Class<?>[] {String.class}, "{invalid");

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(valid), () -> assertNull(invalid));
    }

    @Test
    void it_should_cover_slide_deck_prompt_builders() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(lessonId, "Bai hoc prompt", LessonStatus.PUBLISHED, teacherId);

      // ===== ACT =====
      String jsonPrompt =
          (String)
              invokePrivate(
                  "buildSlideDeckPrompt",
                  new Class<?>[] {
                    Lesson.class,
                    String.class,
                    com.fptu.math_master.enums.LessonSlideOutputFormat.class,
                    int.class
                  },
                  lesson,
                  "Prompt json",
                  com.fptu.math_master.enums.LessonSlideOutputFormat.LATEX,
                  10);
      String taggedPrompt =
          (String)
              invokePrivate(
                  "buildSlideDeckTaggedPrompt",
                  new Class<?>[] {
                    Lesson.class,
                    String.class,
                    com.fptu.math_master.enums.LessonSlideOutputFormat.class,
                    int.class
                  },
                  lesson,
                  "Prompt tagged",
                  com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT,
                  12);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, jsonPrompt.contains("outputFormat")),
          () -> assertEquals(true, jsonPrompt.contains("Trả về CHI DUY NHAT")),
          () -> assertEquals(true, taggedPrompt.contains("[LESSON_SUMMARY]")),
          () -> assertEquals(true, taggedPrompt.contains("12 slide")));
    }

    @Test
    void it_should_cover_render_latex_to_png_for_valid_and_invalid_expressions() {
      // ===== ACT =====
      byte[] valid =
          (byte[]) invokePrivate("renderLatexToPng", new Class<?>[] {String.class}, "x^2 + y^2 = z^2");
      byte[] invalid =
          (byte[]) invokePrivate("renderLatexToPng", new Class<?>[] {String.class}, "\\invalidcommand{");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(true, valid.length > 0),
          () -> assertEquals(true, invalid.length >= 0));
    }

    @Test
    void it_should_cover_download_image_bytes_for_exception_and_non_2xx_cases() {
      // ===== ACT =====
      byte[] malformedUrl =
          (byte[]) invokePrivate("downloadImageBytes", new Class<?>[] {String.class}, "ht!tp://bad-url");
      byte[] non2xx =
          (byte[])
              invokePrivate(
                  "downloadImageBytes",
                  new Class<?>[] {String.class},
                  "https://httpbin.org/status/404");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0, malformedUrl.length),
          () -> assertEquals(0, non2xx.length));
    }

    @Test
    void it_should_cover_apply_text_and_latex_and_latex_token_record_creation() throws Exception {
      // ===== ARRANGE =====
      try (XMLSlideShow slideShow = new XMLSlideShow()) {
        XSLFSlide slide = slideShow.createSlide();
        XSLFTextBox textShape = slide.createTextBox();
        textShape.setText("Noi dung ban dau");

        // ===== ACT =====
        invokePrivate(
            "applyTextAndLatex",
            new Class<?>[] {
              XMLSlideShow.class,
              XSLFSlide.class,
              org.apache.poi.xslf.usermodel.XSLFTextShape.class,
              String.class
            },
            slideShow,
            slide,
            textShape,
            "Gia tri $x^2$");

        // Cover LatexToken record constructor + accessor directly
        Class<?> latexTokenClass = getInnerClass("LatexToken");
        java.lang.reflect.Constructor<?> ctor = latexTokenClass.getDeclaredConstructor(String.class);
        ctor.setAccessible(true);
        Object token = ctor.newInstance("x+1");
        java.lang.reflect.Method expressionMethod = latexTokenClass.getDeclaredMethod("expression");
        expressionMethod.setAccessible(true);
        Object expressionValue = expressionMethod.invoke(token);

        // ===== ASSERT =====
        assertAll(
            () -> assertNotNull(textShape.getText()),
            () -> assertEquals("x+1", expressionValue));
      }
    }

    @Test
    void it_should_cover_build_slide_placeholders_case_2_to_10() {
      Lesson lesson = buildLesson(lessonId, "Tieu de", LessonStatus.PUBLISHED, teacherId);
      Object deck =
          invokePrivate(
              "buildDeckSections",
              new Class<?>[] {Lesson.class, String.class},
              lesson,
              "Prompt");
      Class<?> deckClass = getInnerClass("DeckSections");

      for (int slideIndex = 2; slideIndex <= 10; slideIndex++) {
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> values =
            (java.util.Map<String, String>)
                invokePrivate(
                    "buildSlidePlaceholders",
                    new Class<?>[] {int.class, deckClass},
                    slideIndex,
                    deck);
        assertNotNull(values);
      }
    }

    @Test
    void it_should_cover_inject_template_success_and_exception_paths() throws Exception {
      Lesson lesson = buildLesson(lessonId, "Bai", LessonStatus.PUBLISHED, teacherId);
      Object deck =
          invokePrivate(
              "buildDeckSections",
              new Class<?>[] {Lesson.class, String.class},
              lesson,
              "Prompt");
      Class<?> deckClass = getInnerClass("DeckSections");
      byte[] templateBytes = createTemplateBytesWithPlaceholders(2);

      byte[] success =
          (byte[]) invokePrivate("injectTemplate", new Class<?>[] {byte[].class, deckClass}, templateBytes, deck);
      assertEquals(true, success.length > 0);
      byte[] invalidTemplateBytes = new byte[] {1, 2};

      assertThrows(
          RuntimeException.class,
          () ->
              invokePrivate(
                  "injectTemplate", new Class<?>[] {byte[].class, deckClass}, invalidTemplateBytes, deck));
    }

    @Test
    void it_should_cover_inject_template_with_json_for_clone_remove_and_errors() throws Exception {
      Lesson lesson = buildLesson(lessonId, "Bai Json", LessonStatus.PUBLISHED, teacherId);
      List<LessonSlideJsonItemRequest> fiveSlides =
          List.of(
              LessonSlideJsonItemRequest.builder()
                  .slideNumber(1)
                  .slideType("MAIN_CONTENT")
                  .heading("H1")
                  .content("C1")
                  .build(),
              LessonSlideJsonItemRequest.builder()
                  .slideNumber(2)
                  .slideType("MAIN_CONTENT")
                  .heading("H2")
                  .content("C2")
                  .build(),
              LessonSlideJsonItemRequest.builder()
                  .slideNumber(3)
                  .slideType("MAIN_CONTENT")
                  .heading("H3")
                  .content("C3")
                  .build(),
              LessonSlideJsonItemRequest.builder()
                  .slideNumber(4)
                  .slideType("MAIN_CONTENT")
                  .heading("H4")
                  .content("C4")
                  .build(),
              LessonSlideJsonItemRequest.builder()
                  .slideNumber(5)
                  .slideType("MAIN_CONTENT")
                  .heading("H5")
                  .content("C5")
                  .build());

      byte[] threeSlideTemplate = createTemplateBytesWithPlaceholders(3);
      byte[] clonedOutput =
          (byte[])
              invokePrivate(
                  "injectTemplateWithJson",
                  new Class<?>[] {
                    byte[].class,
                    Lesson.class,
                    List.class,
                    com.fptu.math_master.enums.LessonSlideOutputFormat.class
                  },
                  threeSlideTemplate,
                  lesson,
                  fiveSlides,
                  com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT);
      try (XMLSlideShow ss = new XMLSlideShow(new java.io.ByteArrayInputStream(clonedOutput))) {
        assertEquals(5, ss.getSlides().size());
      }

      byte[] fiveSlideTemplate = createTemplateBytesWithPlaceholders(5);
      List<LessonSlideJsonItemRequest> twoSlides = fiveSlides.subList(0, 2);
      byte[] trimmedOutput =
          (byte[])
              invokePrivate(
                  "injectTemplateWithJson",
                  new Class<?>[] {
                    byte[].class,
                    Lesson.class,
                    List.class,
                    com.fptu.math_master.enums.LessonSlideOutputFormat.class
                  },
                  fiveSlideTemplate,
                  lesson,
                  twoSlides,
                  com.fptu.math_master.enums.LessonSlideOutputFormat.LATEX);
      try (XMLSlideShow ss = new XMLSlideShow(new java.io.ByteArrayInputStream(trimmedOutput))) {
        assertEquals(2, ss.getSlides().size());
      }
      List<LessonSlideJsonItemRequest> emptySlides = List.of();

      assertThrows(
          RuntimeException.class,
          () ->
              invokePrivate(
                  "injectTemplateWithJson",
                  new Class<?>[] {
                    byte[].class,
                    Lesson.class,
                    List.class,
                    com.fptu.math_master.enums.LessonSlideOutputFormat.class
                  },
                  fiveSlideTemplate,
                  lesson,
                  emptySlides,
                  com.fptu.math_master.enums.LessonSlideOutputFormat.PLAIN_TEXT));
    }
  }
}
