package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.AddAssessmentToCourseRequest;
import com.fptu.math_master.dto.request.CreateCourseRequest;
import com.fptu.math_master.dto.request.UpdateCourseAssessmentRequest;
import com.fptu.math_master.dto.request.UpdateCourseRequest;
import com.fptu.math_master.dto.response.CoursePreviewResponse;
import com.fptu.math_master.dto.response.CourseResponse;
import com.fptu.math_master.dto.response.StudentInCourseResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentLesson;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseAssessment;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.CourseLevel;
import com.fptu.math_master.enums.CourseProvider;
import com.fptu.math_master.enums.CourseStatus;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentLessonRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.CourseAssessmentRepository;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.CourseReviewRepository;
import com.fptu.math_master.repository.CustomCourseSectionRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.UploadService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("CourseServiceImpl - Tests")
class CourseServiceImplTest extends BaseUnitTest {

  @InjectMocks private CourseServiceImpl courseService;

  @Mock private CourseRepository courseRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private SchoolGradeRepository schoolGradeRepository;
  @Mock private EnrollmentRepository enrollmentRepository;
  @Mock private CourseLessonRepository courseLessonRepository;
  @Mock private LessonProgressRepository lessonProgressRepository;
  @Mock private UserRepository userRepository;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @Mock private CourseAssessmentRepository courseAssessmentRepository;
  @Mock private AssessmentRepository assessmentRepository;
  @Mock private AssessmentLessonRepository assessmentLessonRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private UploadService uploadService;
  @Mock private MinioProperties minioProperties;
  @Mock private CourseReviewRepository courseReviewRepository;
  @Mock private TeacherProfileRepository teacherProfileRepository;
  @Mock private CustomCourseSectionRepository customCourseSectionRepository;
  @Mock private StreamPublisher streamPublisher;

  private static final UUID TEACHER_ID = UUID.fromString("6a91f5ba-7f10-4f08-8419-c0f329584610");
  private static final UUID ADMIN_ID = UUID.fromString("7b02f6cb-8f20-5f19-9520-d1d43a695721");
  private static final UUID STUDENT_ID = UUID.fromString("8c13f7dc-9a31-6f2a-a631-e2e54b7a6832");
  private static final UUID COURSE_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
  private static final UUID SUBJECT_ID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
  private static final UUID SCHOOL_GRADE_ID = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
  private static final UUID ASSESSMENT_ID = UUID.fromString("d4e5f6a7-b8c9-0123-def0-234567890123");
  private static final UUID LESSON_ID = UUID.fromString("e5f6a7b8-c9d0-1234-ef01-345678901234");

  @BeforeEach
  void setUp() {
    objectMapper.findAndRegisterModules();
    when(minioProperties.getTemplateBucket()).thenReturn("slide-templates");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAsTeacher(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("course-teacher-token")
            .header("alg", "none")
            .subject(userId.toString())
            .claim("scope", "course:write")
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.TEACHER_ROLE)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void authenticateAsAdmin(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("course-admin-token")
            .header("alg", "none")
            .subject(userId.toString())
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.ADMIN_ROLE)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void authenticateAsStudent(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("course-student-token")
            .header("alg", "none")
            .subject(userId.toString())
            .build();
    JwtAuthenticationToken authentication =
        new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + PredefinedRole.STUDENT_ROLE)));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private User buildUser(UUID id, String fullName, String email) {
    User u = User.builder().fullName(fullName).email(email).build();
    u.setId(id);
    return u;
  }

  private Subject buildSubject() {
    return Subject.builder().name("Đại số lớp 10").code("DAI_SO_10").build();
  }

  private SchoolGrade buildSchoolGrade() {
    return SchoolGrade.builder().gradeLevel(10).name("Lớp 10").build();
  }

  private Subject subjectWithId() {
    Subject s = buildSubject();
    s.setId(SUBJECT_ID);
    return s;
  }

  private SchoolGrade schoolGradeWithId() {
    SchoolGrade g = buildSchoolGrade();
    g.setId(SCHOOL_GRADE_ID);
    return g;
  }

  private Course buildCourse(
      CourseStatus status, boolean published, CourseProvider provider) {
    Instant now = Instant.now();
    Course c =
        Course.builder()
            .teacherId(TEACHER_ID)
            .provider(provider)
            .subjectId(SUBJECT_ID)
            .schoolGradeId(SCHOOL_GRADE_ID)
            .title("Lý thuyết tích phân hàm một biến")
            .description("Nội dung ôn tập tích phân theo chương trình Bộ Giáo dục")
            .isPublished(published)
            .status(status)
            .language("Tiếng Việt")
            .level(CourseLevel.ALL_LEVELS)
            .build();
    c.setId(COURSE_ID);
    c.setCreatedAt(now);
    c.setUpdatedAt(now);
    return c;
  }

  private void stubMapResponseData(UUID teacherId) {
    User teacher = buildUser(teacherId, "Giảng viên Phạm Minh Tuấn", "tuan.pham@fptu.edu.vn");
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(teacherProfileRepository.findByUserId(teacherId)).thenReturn(Optional.empty());
    when(enrollmentRepository.countActiveEnrollmentsByCourseId(COURSE_ID)).thenReturn(0L);
    when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(0L);
    when(customCourseSectionRepository.countByCourseIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(0L);
    when(courseReviewRepository.countByCourseIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(0L);
    when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(any(), eq(COURSE_ID)))
        .thenReturn(Optional.empty());
  }

  private void stubSubjectGradeLookup() {
    Subject s = buildSubject();
    s.setId(SUBJECT_ID);
    SchoolGrade g = buildSchoolGrade();
    g.setId(SCHOOL_GRADE_ID);
    when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(s));
    when(schoolGradeRepository.findById(SCHOOL_GRADE_ID)).thenReturn(Optional.of(g));
  }

  private void answerSaveCourse() {
    when(courseRepository.save(any(Course.class)))
        .thenAnswer(
            inv -> {
              Course c = inv.getArgument(0);
              if (c.getId() == null) {
                c.setId(COURSE_ID);
              }
              return c;
            });
  }

  // ─── createCourse ─────────────────────────────────────────────────────────

  @Nested
  @DisplayName("createCourse()")
  class CreateCourseTests {

    /**
     * Normal case: Tạo khóa Bộ với môn và khối hợp lệ, không lỗi giá.
     *
     * <p>Input:
     * <ul>
     *   <li>provider: MINISTRY, subjectId và schoolGradeId có trong DB</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>MINISTRY: subject và grade tồn tại</li>
     *   <li>originalPrice hoặc discountedPrice null: bỏ qua so sánh giảm giá</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về {@link CourseResponse} với id khóa</li>
     * </ul>
     */
    @Test
    void it_should_create_ministry_course_when_subject_and_grade_exist() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.MINISTRY)
              .subjectId(SUBJECT_ID)
              .schoolGradeId(SCHOOL_GRADE_ID)
              .title("Ôn thi cuối kỳ Giải tích 11")
              .build();
      when(subjectRepository.findById(SUBJECT_ID))
          .thenReturn(Optional.of(subjectWithId()));
      when(schoolGradeRepository.findById(SCHOOL_GRADE_ID))
          .thenReturn(Optional.of(schoolGradeWithId()));
      answerSaveCourse();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.createCourse(request, null);

      assertNotNull(res);
      assertEquals(COURSE_ID, res.getId());
      verify(courseRepository, times(1)).save(any(Course.class));
    }

    /**
     * Abnormal case: Thiếu subjectId với khóa Bộ.
     */
    @Test
    void it_should_throw_exception_when_ministry_course_missing_subject() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.MINISTRY)
              .schoolGradeId(SCHOOL_GRADE_ID)
              .title("Khóa môn Số học nâng cao")
              .build();

      AppException ex =
          assertThrows(AppException.class, () -> courseService.createCourse(request, null));
      assertEquals(ErrorCode.SUBJECT_REQUIRED_FOR_MINISTRY_COURSE, ex.getErrorCode());
      verify(courseRepository, never()).save(any());
    }

    /**
     * Abnormal case: Thiếu schoolGradeId với khóa Bộ.
     */
    @Test
    void it_should_throw_exception_when_ministry_course_missing_grade() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.MINISTRY)
              .subjectId(SUBJECT_ID)
              .title("Khóa bồi dưỡng HSG")
              .build();

      AppException ex =
          assertThrows(AppException.class, () -> courseService.createCourse(request, null));
      assertEquals(ErrorCode.GRADE_REQUIRED_FOR_MINISTRY_COURSE, ex.getErrorCode());
    }

    /**
     * Abnormal case: subjectId trỏ tới bản ghi không tồn tại.
     */
    @Test
    void it_should_throw_exception_when_ministry_subject_not_found() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.MINISTRY)
              .subjectId(SUBJECT_ID)
              .schoolGradeId(SCHOOL_GRADE_ID)
              .title("Ôn luyện Hình học không gian")
              .build();
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(AppException.class, () -> courseService.createCourse(request, null));
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, ex.getErrorCode());
    }

    /**
     * Abnormal case: schoolGradeId không tồn tại.
     */
    @Test
    void it_should_throw_exception_when_school_grade_not_found() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.MINISTRY)
              .subjectId(SUBJECT_ID)
              .schoolGradeId(SCHOOL_GRADE_ID)
              .title("Ôn thi tốt nghiệp môn Toán")
              .build();
      when(subjectRepository.findById(SUBJECT_ID))
          .thenReturn(Optional.of(subjectWithId()));
      when(schoolGradeRepository.findById(SCHOOL_GRADE_ID)).thenReturn(Optional.empty());

      AppException ex =
          assertThrows(AppException.class, () -> courseService.createCourse(request, null));
      assertEquals(ErrorCode.SCHOOL_GRADE_NOT_FOUND, ex.getErrorCode());
    }

    /**
     * Normal case: Khóa CUSTOM tùy chọn môn/khối, có cấu hình ngôn ngữ rỗng dùng mặc định.
     */
    @Test
    void it_should_create_custom_course_with_default_language_when_empty() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.CUSTOM)
              .title("Học Toán ứng dụng qua bài tập thực tế")
              .language("   ")
              .build();
      answerSaveCourse();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.createCourse(request, null);

      assertNotNull(res);
      verify(courseRepository, times(1)).save(any(Course.class));
    }

    /**
     * Abnormal case: Giá giảm không thấp hơn giá gốc.
     */
    @Test
    void it_should_throw_exception_when_discounted_price_not_lower_than_original() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.CUSTOM)
              .title("Khóa luyện thi môn Toán cấp tốc")
              .originalPrice(new BigDecimal("500000"))
              .discountedPrice(new BigDecimal("500000"))
              .build();

      AppException ex =
          assertThrows(AppException.class, () -> courseService.createCourse(request, null));
      assertEquals(ErrorCode.INVALID_DISCOUNT_PRICE, ex.getErrorCode());
    }

    /**
     * Normal case: Tải ảnh bìa lên kho lưu trữ.
     */
    @Test
    void it_should_upload_thumbnail_when_file_provided() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.CUSTOM)
              .title("Giải tích ứng dụng trong kinh tế")
              .build();
      MultipartFile file =
          new MockMultipartFile("thumbnail", "bia-khoa-hoc.png", "image/png", "binary".getBytes());
      when(uploadService.uploadFile(any(), eq("course-thumbnails")))
          .thenReturn("courses/thumb/bia-khoa-hoc.png");
      answerSaveCourse();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.createCourse(request, file);

      assertNotNull(res);
      verify(uploadService, times(1)).uploadFile(any(), eq("course-thumbnails"));
    }

    @Test
    void it_should_resolve_optional_subject_for_custom_when_id_present() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.CUSTOM)
              .subjectId(SUBJECT_ID)
              .title("Khóa nâng cao qua bài tập tự luyện")
              .build();
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subjectWithId()));
      answerSaveCourse();
      stubMapResponseData(TEACHER_ID);

      courseService.createCourse(request, null);

      verify(subjectRepository, times(1)).findById(SUBJECT_ID);
    }

    @Test
    void it_should_tolerate_missing_optional_subject_for_custom() {
      authenticateAsTeacher(TEACHER_ID);
      CreateCourseRequest request =
          CreateCourseRequest.builder()
              .provider(CourseProvider.CUSTOM)
              .subjectId(SUBJECT_ID)
              .title("Lộ trình ôn thi tốt nghiệp môn Toán mở rộng")
              .build();
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());
      answerSaveCourse();
      stubMapResponseData(TEACHER_ID);

      assertNotNull(courseService.createCourse(request, null));
    }
  }

  // ─── update / delete / publish / submit / admin ───────────────────────

  @Nested
  @DisplayName("updateCourse()")
  class UpdateCourseTests {

    @Test
    void it_should_update_fields_when_request_provides_values() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      UpdateCourseRequest request =
          UpdateCourseRequest.builder()
              .title("Tiêu đề cập nhật sau biên tập nội dung")
              .description("Mô tả đã điều chỉnh theo góp ý hội đồng")
              .build();
      when(courseRepository.save(any(Course.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.updateCourse(COURSE_ID, request, null);

      assertEquals("Tiêu đề cập nhật sau biên tập nội dung", res.getTitle());
      verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void it_should_apply_null_discount_when_original_updated_and_discount_cleared() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      course.setOriginalPrice(new BigDecimal("300000"));
      course.setDiscountedPrice(new BigDecimal("250000"));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      UpdateCourseRequest request =
          UpdateCourseRequest.builder()
              .originalPrice(new BigDecimal("400000"))
              .discountedPrice(null)
              .build();
      when(courseRepository.save(any(Course.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      courseService.updateCourse(COURSE_ID, request, null);

      assertNull(course.getDiscountedPrice());
    }

    @Test
    void it_should_delete_old_thumbnail_when_replacing_with_new_file() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      course.setThumbnailUrl("relative/key/old-thumb.png");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      MultipartFile file =
          new MockMultipartFile("thumbnail", "new-bia.png", "image/png", "data".getBytes());
      when(uploadService.uploadFile(any(), eq("course-thumbnails")))
          .thenReturn("relative/key/new-thumb.png");
      when(courseRepository.save(any(Course.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      courseService.updateCourse(COURSE_ID, new UpdateCourseRequest(), file);

      verify(uploadService, times(1)).deleteFile(eq("relative/key/old-thumb.png"), anyString());
    }

    @Test
    void it_should_not_touch_thumbnail_when_file_is_empty() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      MultipartFile empty = new MockMultipartFile("thumbnail", "empty", "image/png", new byte[0]);
      when(courseRepository.save(any(Course.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      courseService.updateCourse(COURSE_ID, new UpdateCourseRequest(), empty);

      verify(uploadService, never()).uploadFile(any(), anyString());
    }

    @Test
    void it_should_set_default_language_on_update_when_blank() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);
      UpdateCourseRequest req = UpdateCourseRequest.builder().language("  ").build();

      courseService.updateCourse(COURSE_ID, req, null);

      assertEquals("Tiếng Việt", course.getLanguage());
    }

    @Test
    void it_should_update_discounted_price_when_original_not_sent() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      course.setOriginalPrice(new BigDecimal("900000"));
      course.setDiscountedPrice(new BigDecimal("800000"));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      courseService.updateCourse(
          COURSE_ID,
          UpdateCourseRequest.builder().discountedPrice(new BigDecimal("750000")).build(),
          null);

      assertEquals(new BigDecimal("750000"), course.getDiscountedPrice());
    }

    @Test
    void it_should_reject_update_when_stored_prices_create_invalid_discount() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      course.setOriginalPrice(new BigDecimal("100000"));
      course.setDiscountedPrice(new BigDecimal("50000"));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      UpdateCourseRequest req = UpdateCourseRequest.builder().discountedPrice(new BigDecimal("150000")).build();

      assertThrows(AppException.class, () -> courseService.updateCourse(COURSE_ID, req, null));
    }

    @Test
    void it_should_update_extended_metadata_when_all_sections_provided() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);
      Instant future = Instant.parse("2027-06-15T00:00:00Z");
      UpdateCourseRequest req =
          UpdateCourseRequest.builder()
              .whatYouWillLearn("Nắm định nghĩa giới hạn và tính liên tục")
              .requirements("Có nền tảng đại số lớp 10")
              .targetAudience("Sinh viên năm nhất chuyên ngành CNTT")
              .subtitle("Phần mở rộng cho môn Giải tích 1A")
              .level(CourseLevel.BEGINNER)
              .discountExpiryDate(future)
              .build();

      courseService.updateCourse(COURSE_ID, req, null);

      assertEquals("Nắm định nghĩa giới hạn và tính liên tục", course.getWhatYouWillLearn());
      assertEquals(CourseLevel.BEGINNER, course.getLevel());
      assertEquals(future, course.getDiscountExpiryDate());
    }

    @Test
    void it_should_throw_exception_when_user_not_owner() {
      authenticateAsTeacher(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(Optional.of(buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));

      assertThrows(
          AppException.class,
          () -> courseService.updateCourse(COURSE_ID, new UpdateCourseRequest(), null));
    }
  }

  @Nested
  @DisplayName("deleteCourse()")
  class DeleteCourseTests {

    @Test
    void it_should_throw_exception_when_deleting_published_status_course() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      AppException ex = assertThrows(AppException.class, () -> courseService.deleteCourse(COURSE_ID));
      assertEquals(ErrorCode.INVALID_COURSE_STATUS, ex.getErrorCode());
    }

    @Test
    void it_should_convert_to_draft_when_active_enrollments_exist() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, true, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(enrollmentRepository.countActiveEnrollmentsByCourseId(COURSE_ID)).thenReturn(3L);
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

      courseService.deleteCourse(COURSE_ID);

      assertEquals(CourseStatus.DRAFT, course.getStatus());
      assertFalse(course.isPublished());
      verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void it_should_soft_delete_when_no_active_enrollments() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(enrollmentRepository.countActiveEnrollmentsByCourseId(COURSE_ID)).thenReturn(0L);
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

      courseService.deleteCourse(COURSE_ID);

      assertNotNull(course.getDeletedAt());
      assertEquals(TEACHER_ID, course.getDeletedBy());
    }
  }

  @Nested
  @DisplayName("publishCourse()")
  class PublishCourseTests {

    @Test
    void it_should_publish_when_course_approved() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.publishCourse(COURSE_ID, true);

      assertTrue(res.isPublished());
    }

    @Test
    void it_should_throw_exception_when_course_not_approved() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      AppException ex =
          assertThrows(AppException.class, () -> courseService.publishCourse(COURSE_ID, true));
      assertEquals(ErrorCode.COURSE_NOT_APPROVED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_unpublish_requested() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      AppException ex =
          assertThrows(AppException.class, () -> courseService.publishCourse(COURSE_ID, false));
      assertEquals(ErrorCode.INVALID_COURSE_STATUS, ex.getErrorCode());
    }
  }

  @Nested
  @DisplayName("submitForReview()")
  class SubmitForReviewTests {

    @Test
    void it_should_reject_submission_from_published_course() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      assertThrows(
          AppException.class,
          () -> courseService.submitForReview(COURSE_ID));
    }

    @Test
    void it_should_throw_exception_when_rejected_and_not_revised_after() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.REJECTED, false, CourseProvider.CUSTOM);
      Instant rejectedAt = Instant.parse("2025-10-10T10:00:00Z");
      course.setRejectedAt(rejectedAt);
      course.setUpdatedAt(rejectedAt);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(2L);

      AppException ex =
          assertThrows(AppException.class, () -> courseService.submitForReview(COURSE_ID));
      assertEquals(ErrorCode.COURSE_ALREADY_SUBMITTED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_no_lessons_in_course() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(0L);

      AppException ex =
          assertThrows(AppException.class, () -> courseService.submitForReview(COURSE_ID));
      assertEquals(ErrorCode.COURSE_MUST_HAVE_LESSONS, ex.getErrorCode());
    }

    @Test
    void it_should_allow_resubmit_after_rejection_when_course_edited() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.REJECTED, false, CourseProvider.CUSTOM);
      course.setRejectedAt(Instant.parse("2026-01-10T08:00:00Z"));
      course.setUpdatedAt(Instant.parse("2026-01-20T10:00:00Z"));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(1L);
      when(userRepository.findUserIdsByRoleName(PredefinedRole.ADMIN_ROLE))
          .thenReturn(Collections.emptyList());
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(1L);

      courseService.submitForReview(COURSE_ID);

      assertEquals(CourseStatus.PENDING_REVIEW, course.getStatus());
    }

    @Test
    void it_should_submit_and_notify_admins_when_lessons_present() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(1L);
      when(userRepository.findUserIdsByRoleName(PredefinedRole.ADMIN_ROLE))
          .thenReturn(List.of(ADMIN_ID));
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(1L);

      CourseResponse res = courseService.submitForReview(COURSE_ID);

      assertEquals(CourseStatus.PENDING_REVIEW, res.getStatus());
      verify(streamPublisher, times(1)).publish(any());
    }

    @Test
    void it_should_skip_stream_when_no_admins() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(1L);
      when(userRepository.findUserIdsByRoleName(PredefinedRole.ADMIN_ROLE))
          .thenReturn(Collections.emptyList());
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(1L);

      courseService.submitForReview(COURSE_ID);

      verify(streamPublisher, never()).publish(any());
    }
  }

  @Nested
  @DisplayName("getCourseReviewsForAdmin()")
  class AdminListTests {

    @Test
    void it_should_filter_by_status_enum_when_valid() {
      Pageable pageable = PageRequest.of(0, 5);
      when(courseRepository.findByStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
              eq(CourseStatus.PENDING_REVIEW), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      courseService.getCourseReviewsForAdmin("PENDING_REVIEW", pageable);

      verify(courseRepository, times(1))
          .findByStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(
              eq(CourseStatus.PENDING_REVIEW), any(Pageable.class));
    }

    @Test
    void it_should_return_all_when_status_invalid() {
      Pageable pageable = PageRequest.of(0, 5);
      when(courseRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc(any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      courseService.getCourseReviewsForAdmin("NOT_A_REAL_STATUS", pageable);

      verify(courseRepository, times(1)).findByDeletedAtIsNullOrderByUpdatedAtDesc(any(Pageable.class));
    }

    @Test
    void it_should_return_all_when_status_all() {
      Pageable pageable = PageRequest.of(0, 5);
      when(courseRepository.findByDeletedAtIsNullOrderByUpdatedAtDesc(any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      courseService.getCourseReviewsForAdmin("ALL", pageable);

      verify(courseRepository, times(1)).findByDeletedAtIsNullOrderByUpdatedAtDesc(any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("Admin approve / reject")
  class ApproveRejectTests {

    @Test
    void it_should_approve_and_notify_teacher() {
      authenticateAsAdmin(ADMIN_ID);
      Course course = buildCourse(CourseStatus.PENDING_REVIEW, false, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.approveCourse(COURSE_ID);

      assertEquals(CourseStatus.PUBLISHED, res.getStatus());
      assertTrue(res.isPublished());
      verify(streamPublisher, times(1)).publish(any());
    }

    @Test
    void it_should_reject_with_reason() {
      authenticateAsAdmin(ADMIN_ID);
      Course course = buildCourse(CourseStatus.PENDING_REVIEW, false, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.rejectCourse(COURSE_ID, "Cần bổ sung bài tập ở chương II");

      assertEquals(CourseStatus.REJECTED, res.getStatus());
      assertFalse(res.isPublished());
      verify(streamPublisher, times(1)).publish(any());
    }

    @Test
    void it_should_reject_without_verbose_reason() {
      authenticateAsAdmin(ADMIN_ID);
      Course course = buildCourse(CourseStatus.PENDING_REVIEW, false, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.rejectCourse(COURSE_ID, null);

      assertEquals(CourseStatus.REJECTED, res.getStatus());
      verify(streamPublisher, times(1)).publish(any());
    }

    @Test
    void it_should_not_approve_non_pending_course() {
      authenticateAsAdmin(ADMIN_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      assertThrows(AppException.class, () -> courseService.approveCourse(COURSE_ID));
    }
  }

  @Nested
  @DisplayName("getCourseById() / getCoursePreview()")
  class ReadCourseTests {

    @Test
    void it_should_return_public_course_to_anonymous() {
      SecurityContextHolder.clearContext();
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      stubSubjectGradeLookup();
      when(userRepository.findById(TEACHER_ID))
          .thenReturn(
              Optional.of(
                  buildUser(TEACHER_ID, "Giảng viên Lê Hồng Hạnh", "hanh.lh@fptu.edu.vn")));

      CourseResponse res = courseService.getCourseById(COURSE_ID);

      assertNotNull(res);
    }

    @Test
    void it_should_allow_admin_to_view_draft() {
      authenticateAsAdmin(ADMIN_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      assertNotNull(courseService.getCourseById(COURSE_ID));
    }

    @Test
    void it_should_deny_unauthenticated_on_private_course() {
      SecurityContextHolder.clearContext();
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      AppException ex =
          assertThrows(AppException.class, () -> courseService.getCourseById(COURSE_ID));
      assertEquals(ErrorCode.COURSE_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    void it_should_allow_teacher_to_view_own_draft_course() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);
      when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.empty());

      CourseResponse res = courseService.getCourseById(COURSE_ID);

      assertEquals("Anonymous", res.getTeacherName());
    }

    @Test
    void it_should_hide_discount_in_response_when_expiry_in_past() {
      authenticateAsStudent(STUDENT_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      course.setOriginalPrice(new BigDecimal("600000"));
      course.setDiscountedPrice(new BigDecimal("450000"));
      course.setDiscountExpiryDate(Instant.parse("2020-01-01T00:00:00Z"));
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CourseResponse res = courseService.getCourseById(COURSE_ID);

      assertNull(res.getDiscountedPrice());
    }

    @Test
    void it_should_use_custom_title_for_preview_when_no_lesson_reference() {
      authenticateAsStudent(STUDENT_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      CourseLesson cl =
          CourseLesson.builder()
              .customTitle("Phần mở đầu về ứng dụng tích phân")
              .orderIndex(1)
              .build();
      cl.setId(UUID.fromString("beebeebe-ebee-4bee-8bee-beebeebeebee"));
      cl.setCourseId(COURSE_ID);
      cl.setLessonId(null);
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CoursePreviewResponse preview = courseService.getCoursePreview(COURSE_ID);

      assertEquals("Phần mở đầu về ứng dụng tích phân", preview.getLessons().get(0).getLessonTitle());
    }

    @Test
    void it_should_map_lesson_titles_in_preview() {
      authenticateAsStudent(STUDENT_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      CourseLesson cl = CourseLesson.builder().customTitle("Tựa tùy chỉnh gốc").build();
      cl.setId(UUID.fromString("f1a2b3c4-d5e6-7890-abcd-ef1234567890"));
      cl.setCourseId(COURSE_ID);
      cl.setLessonId(LESSON_ID);
      cl.setOrderIndex(1);
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl));
      Lesson l = Lesson.builder().title("Bài giảng hàm số liên tục").build();
      l.setId(LESSON_ID);
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(l));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CoursePreviewResponse preview = courseService.getCoursePreview(COURSE_ID);

      assertEquals("Bài giảng hàm số liên tục", preview.getLessons().get(0).getLessonTitle());
    }
  }

  @Nested
  @DisplayName("getPublicCourses() / getRelatedCourses() / search")
  class SearchAndRelatedTests {

    @Test
    void it_should_call_published_filter_with_unsorted_pageable() {
      Pageable pageable = PageRequest.of(0, 10);
      when(courseRepository.findPublishedCoursesWithFilter(
              any(), any(), any(), any(), any(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      courseService.getPublicCourses(
          SCHOOL_GRADE_ID, SUBJECT_ID, null, null, "từ khóa tìm kiếm", pageable);

      verify(courseRepository, times(1))
          .findPublishedCoursesWithFilter(
              eq(SCHOOL_GRADE_ID),
              eq(SUBJECT_ID),
              isNull(),
              isNull(),
              eq("từ khóa tìm kiếm"),
              any(Pageable.class));
    }

    @Test
    void it_should_trims_keyword_for_admin_search() {
      Pageable pageable = PageRequest.of(0, 20);
      when(courseRepository.searchAllCoursesForAdmin(eq("giải tích"), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      courseService.searchCoursesForAdmin("  giải tích  ", pageable);

      verify(courseRepository, times(1))
          .searchAllCoursesForAdmin(eq("giải tích"), any(Pageable.class));
    }

    @Test
    void it_should_send_null_keyword_to_repository_when_search_blank() {
      Pageable pageable = PageRequest.of(0, 10);
      when(courseRepository.searchAllCoursesForAdmin(isNull(), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      courseService.searchCoursesForAdmin("   ", pageable);

      verify(courseRepository, times(1)).searchAllCoursesForAdmin(isNull(), any(Pageable.class));
    }

    @Test
    void it_should_return_related_courses_page() {
      Course base = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(base));
      when(courseRepository.findRelatedCourses(
              eq(SUBJECT_ID), eq(SCHOOL_GRADE_ID), eq(COURSE_ID), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      Page<CourseResponse> page =
          courseService.getRelatedCourses(COURSE_ID, PageRequest.of(0, 4));

      assertNotNull(page);
    }
  }

  @Nested
  @DisplayName("getStudentsInCourse()")
  class StudentsInCourseTests {

    @Test
    void it_should_map_enrollments_to_responses() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(5L);
      UUID enrollmentId = UUID.fromString("9d24f8ed-0b42-7f3a-b742-f3e65c8a7941");
      Enrollment e =
          Enrollment.builder()
              .courseId(COURSE_ID)
              .studentId(STUDENT_ID)
              .status(EnrollmentStatus.ACTIVE)
              .enrolledAt(Instant.parse("2026-01-15T02:00:00Z"))
              .build();
      e.setId(enrollmentId);
      Page<Enrollment> page = new PageImpl<>(List.of(e));
      when(enrollmentRepository.findByCourseIdAndStatusAndDeletedAtIsNull(
              eq(COURSE_ID), eq(EnrollmentStatus.ACTIVE), any(Pageable.class)))
          .thenReturn(page);
      when(userRepository.findById(STUDENT_ID))
          .thenReturn(
              Optional.of(
                  buildUser(
                      STUDENT_ID,
                      "Học sinh Vũ Khánh An",
                      "khanh.an@student.fptu.edu.vn")));
      when(lessonProgressRepository.countCompletedByEnrollmentId(enrollmentId)).thenReturn(2L);

      Page<StudentInCourseResponse> res =
          courseService.getStudentsInCourse(COURSE_ID, PageRequest.of(0, 10));

      assertEquals(1, res.getContent().size());
      assertEquals(2, res.getContent().get(0).getCompletedLessons());
      assertEquals(5, res.getContent().get(0).getTotalLessons());
    }

    @Test
    void it_should_map_student_name_null_when_user_record_absent() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(3L);
      UUID enrId = UUID.fromString("cedecede-cede-cede-cede-cedecedecede");
      Enrollment e =
          Enrollment.builder()
              .courseId(COURSE_ID)
              .studentId(STUDENT_ID)
              .status(EnrollmentStatus.ACTIVE)
              .build();
      e.setId(enrId);
      when(enrollmentRepository.findByCourseIdAndStatusAndDeletedAtIsNull(
              eq(COURSE_ID), eq(EnrollmentStatus.ACTIVE), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(e)));
      when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());
      when(lessonProgressRepository.countCompletedByEnrollmentId(enrId)).thenReturn(0L);

      var row = courseService.getStudentsInCourse(COURSE_ID, PageRequest.of(0, 5)).getContent().get(0);

      assertNull(row.getStudentName());
    }
  }

  @Nested
  @DisplayName("getTeacherProfile() / getTeacherCourses() / getMyCourses()")
  class TeacherInfoTests {

    @Test
    void it_should_build_teacher_profile_response() {
      when(userRepository.findById(TEACHER_ID))
          .thenReturn(
              Optional.of(
                  buildUser(TEACHER_ID, "Thầy Hoàng Mạnh Dũng", "dung.hm@fptu.edu.vn")));
      when(teacherProfileRepository.findByUserId(TEACHER_ID))
          .thenReturn(
              Optional.of(
                  TeacherProfile.builder()
                      .description("Hơn 15 năm giảng dạy bậc trung học phổ thông")
                      .position("Thạc sĩ Toán học")
                      .build()));
      when(courseRepository.findByTeacherIdAndIsPublishedTrueAndDeletedAtIsNull(TEACHER_ID))
          .thenReturn(List.of(buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.CUSTOM)));
      when(enrollmentRepository.countStudentsByTeacherId(TEACHER_ID)).thenReturn(48);
      when(courseReviewRepository.countByTeacherId(TEACHER_ID)).thenReturn(12L);
      when(courseReviewRepository.calculateTeacherAverageRating(TEACHER_ID))
          .thenReturn(4.65);

      var profile = courseService.getTeacherProfile(TEACHER_ID);

      assertEquals("Thầy Hoàng Mạnh Dũng", profile.getFullName());
      assertTrue(profile.getTotalCourses() >= 1);
    }

    @Test
    void it_should_default_average_rating_to_zero_when_no_reviews() {
      when(userRepository.findById(TEACHER_ID))
          .thenReturn(
              Optional.of(
                  buildUser(TEACHER_ID, "Thầy Trần Quốc Khải", "khai.tq@fptu.edu.vn")));
      when(teacherProfileRepository.findByUserId(TEACHER_ID)).thenReturn(Optional.empty());
      when(courseRepository.findByTeacherIdAndIsPublishedTrueAndDeletedAtIsNull(TEACHER_ID))
          .thenReturn(Collections.emptyList());
      when(enrollmentRepository.countStudentsByTeacherId(TEACHER_ID)).thenReturn(0);
      when(courseReviewRepository.countByTeacherId(TEACHER_ID)).thenReturn(0L);
      when(courseReviewRepository.calculateTeacherAverageRating(TEACHER_ID)).thenReturn(null);

      var profile = courseService.getTeacherProfile(TEACHER_ID);

      assertEquals(0, BigDecimal.ZERO.compareTo(profile.getAverageRating()));
    }

    @Test
    void it_should_list_published_courses_by_teacher() {
      when(courseRepository.findByTeacherIdAndIsPublishedTrueAndDeletedAtIsNull(TEACHER_ID))
          .thenReturn(
              List.of(
                  buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY)));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      List<CourseResponse> list = courseService.getTeacherCourses(TEACHER_ID);

      assertFalse(list.isEmpty());
    }

    @Test
    void it_should_list_teacher_courses_for_current_user() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByTeacherIdAndDeletedAtIsNullOrderByCreatedAtDesc(TEACHER_ID))
          .thenReturn(
              List.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      List<CourseResponse> my = courseService.getMyCourses();

      assertEquals(1, my.size());
    }
  }

  @Nested
  @DisplayName("getPendingReviewCourses() / getAdminCoursePreview()")
  class AdminReadTests {

    @Test
    void it_should_page_pending_review() {
      when(courseRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
              eq(CourseStatus.PENDING_REVIEW), any(Pageable.class)))
          .thenReturn(new PageImpl<>(Collections.emptyList()));

      Page<CourseResponse> p =
          courseService.getPendingReviewCourses(PageRequest.of(0, 3));

      assertNotNull(p);
    }

    @Test
    void it_should_return_admin_preview_with_sorted_lessons() {
      authenticateAsAdmin(ADMIN_ID);
      Course course = buildCourse(CourseStatus.PENDING_REVIEW, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      CourseLesson cl = CourseLesson.builder().customTitle("Bài 2").orderIndex(2).build();
      cl.setId(UUID.fromString("aa11bb22-cc33-4455-6677-889900aabbcc"));
      cl.setCourseId(COURSE_ID);
      cl.setLessonId(LESSON_ID);
      CourseLesson cl2 = CourseLesson.builder().customTitle("Bài 1").orderIndex(1).build();
      cl2.setId(UUID.fromString("bb22cc33-dd44-5566-7788-9900aabbccdd"));
      cl2.setCourseId(COURSE_ID);
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(cl, cl2));
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.empty());
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      CoursePreviewResponse r = courseService.getAdminCoursePreview(COURSE_ID);

      assertEquals(1, (int) r.getLessons().get(0).getOrderIndex());
    }
  }

  // ─── Assessments ───────────────────────────────────────────────────────

  @Nested
  @DisplayName("addAssessmentToCourse()")
  class AddAssessmentTests {

    private Assessment publishedAssessment() {
      Assessment a =
          Assessment.builder()
              .teacherId(TEACHER_ID)
              .title("Bài kiểm tra 45 phút chương lũy thừa")
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.PUBLISHED)
              .build();
      a.setId(ASSESSMENT_ID);
      a.setCreatedAt(Instant.now());
      return a;
    }

    @Test
    void it_should_add_assessment_to_ministry_course_with_matching_lessons() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      Assessment a = publishedAssessment();
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(a));
      when(courseAssessmentRepository.existsByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(false);
      when(assessmentRepository.countQuestionsByAssessmentId(ASSESSMENT_ID)).thenReturn(5L);
      CourseLesson clRow =
          CourseLesson.builder()
              .courseId(COURSE_ID)
              .lessonId(LESSON_ID)
              .customTitle("Tựa tại chỗ")
              .build();
      clRow.setId(UUID.fromString("10101010-1010-1010-1010-101010101010"));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(clRow));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(ASSESSMENT_ID))
          .thenReturn(List.of(LESSON_ID));
      Lesson lessonEntity = Lesson.builder().title("Bài tập ứng dụng").build();
      lessonEntity.setId(LESSON_ID);
      when(lessonRepository.findByIdInAndNotDeleted(any()))
          .thenReturn(List.of(lessonEntity));
      when(courseAssessmentRepository.save(any(CourseAssessment.class)))
          .thenAnswer(
              inv -> {
                CourseAssessment ca = inv.getArgument(0);
                ca.setId(UUID.fromString("20202020-2020-2020-2020-202020202020"));
                return ca;
              });
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(10.0);

      AddAssessmentToCourseRequest req =
          AddAssessmentToCourseRequest.builder()
              .assessmentId(ASSESSMENT_ID)
              .orderIndex(1)
              .allowOutOfCourseLessons(false)
              .build();

      var res = courseService.addAssessmentToCourse(COURSE_ID, req);

      assertNotNull(res.getId());
    }

    @Test
    void it_should_allow_ministry_out_of_course_when_flag_true() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      Assessment a = publishedAssessment();
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(a));
      when(courseAssessmentRepository.existsByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(false);
      when(assessmentRepository.countQuestionsByAssessmentId(ASSESSMENT_ID)).thenReturn(1L);
      CourseLesson clMin =
          CourseLesson.builder().courseId(COURSE_ID).lessonId(LESSON_ID).build();
      clMin.setId(UUID.fromString("abababab-abab-abab-abab-abababababab"));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(clMin));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(ASSESSMENT_ID))
          .thenReturn(List.of());
      when(lessonRepository.findByIdInAndNotDeleted(any())).thenReturn(Collections.emptyList());
      when(courseAssessmentRepository.save(any(CourseAssessment.class)))
          .thenAnswer(
              inv -> {
                CourseAssessment ca = inv.getArgument(0);
                ca.setId(UUID.fromString("20202020-2020-2020-2020-202020202021"));
                return ca;
              });
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(5.0);

      AddAssessmentToCourseRequest req =
          AddAssessmentToCourseRequest.builder()
              .assessmentId(ASSESSMENT_ID)
              .allowOutOfCourseLessons(true)
              .build();

      assertNotNull(courseService.addAssessmentToCourse(COURSE_ID, req));
    }

    @Test
    void it_should_add_assessment_to_custom_provider_without_lesson_match() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      Assessment a = publishedAssessment();
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(a));
      when(courseAssessmentRepository.existsByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(false);
      when(assessmentRepository.countQuestionsByAssessmentId(ASSESSMENT_ID)).thenReturn(3L);
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of());
      when(courseAssessmentRepository.save(any(CourseAssessment.class)))
          .thenAnswer(
              inv -> {
                CourseAssessment ca = inv.getArgument(0);
                ca.setId(UUID.fromString("20202020-2020-2020-2020-202020202022"));
                return ca;
              });
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(8.0);

      AddAssessmentToCourseRequest req =
          AddAssessmentToCourseRequest.builder().assessmentId(ASSESSMENT_ID).build();

      var res = courseService.addAssessmentToCourse(COURSE_ID, req);
      assertTrue(res.isLessonMatched() == false || res.getMatchedLessonCount() == 0);
    }

    @Test
    void it_should_throw_exception_when_assessment_id_unknown() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID)).thenReturn(Optional.empty());

      var req = AddAssessmentToCourseRequest.builder().assessmentId(ASSESSMENT_ID).build();
      assertThrows(
          AppException.class, () -> courseService.addAssessmentToCourse(COURSE_ID, req));
    }

    @Test
    void it_should_throw_exception_when_assessment_owned_by_another_teacher() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));
      Assessment a = publishedAssessment();
      a.setTeacherId(UUID.fromString("fedcfedc-fedc-fedc-fedc-fedcfedcfedc"));
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID)).thenReturn(Optional.of(a));

      var req = AddAssessmentToCourseRequest.builder().assessmentId(ASSESSMENT_ID).build();
      assertThrows(
          AppException.class, () -> courseService.addAssessmentToCourse(COURSE_ID, req));
    }

    @Test
    void it_should_throw_exception_when_assessment_not_published() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));
      Assessment a = publishedAssessment();
      a.setStatus(AssessmentStatus.DRAFT);
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID)).thenReturn(Optional.of(a));

      var req = AddAssessmentToCourseRequest.builder().assessmentId(ASSESSMENT_ID).build();
      AppException ex =
          assertThrows(
              AppException.class, () -> courseService.addAssessmentToCourse(COURSE_ID, req));
      assertEquals(ErrorCode.ASSESSMENT_NOT_PUBLISHED, ex.getErrorCode());
    }

    @Test
    void it_should_throw_exception_when_ministry_has_no_intersecting_lessons() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(publishedAssessment()));
      when(courseAssessmentRepository.existsByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(false);
      when(assessmentRepository.countQuestionsByAssessmentId(ASSESSMENT_ID)).thenReturn(2L);
      CourseLesson cl =
          CourseLesson.builder()
              .courseId(COURSE_ID)
              .lessonId(LESSON_ID)
              .build();
      cl.setId(UUID.fromString("dededede-dede-dede-dede-dededededede"));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(List.of(cl));
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(ASSESSMENT_ID))
          .thenReturn(List.of());

      var req =
          AddAssessmentToCourseRequest.builder()
              .assessmentId(ASSESSMENT_ID)
              .allowOutOfCourseLessons(false)
              .build();
      assertThrows(
          AppException.class, () -> courseService.addAssessmentToCourse(COURSE_ID, req));
    }

    @Test
    void it_should_throw_exception_when_assessment_already_in_course() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(publishedAssessment()));
      when(courseAssessmentRepository.existsByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(true);

      var req = AddAssessmentToCourseRequest.builder().assessmentId(ASSESSMENT_ID).build();
      assertThrows(
          AppException.class, () -> courseService.addAssessmentToCourse(COURSE_ID, req));
    }

    @Test
    void it_should_throw_exception_when_assessment_has_zero_questions() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(publishedAssessment()));
      when(courseAssessmentRepository.existsByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(false);
      when(assessmentRepository.countQuestionsByAssessmentId(ASSESSMENT_ID)).thenReturn(0L);

      var req = AddAssessmentToCourseRequest.builder().assessmentId(ASSESSMENT_ID).build();
      assertThrows(
          AppException.class, () -> courseService.addAssessmentToCourse(COURSE_ID, req));
    }
  }

  @Nested
  @DisplayName("getCourseAssessments() / getAvailableAssessments()")
  class ListAssessmentTests {

    @Test
    void it_should_filter_get_course_assessments_by_status_type_required() {
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      UUID caId = UUID.fromString("30303030-3030-3030-3030-303030303030");
      CourseAssessment ca =
          CourseAssessment.builder()
              .courseId(COURSE_ID)
              .assessmentId(ASSESSMENT_ID)
              .orderIndex(0)
              .isRequired(true)
              .build();
      ca.setId(caId);
      when(courseAssessmentRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(ca));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(Collections.emptyList());
      Assessment draftForList =
          Assessment.builder()
              .title("Kiểm tra nhanh")
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.DRAFT)
              .build();
      draftForList.setId(ASSESSMENT_ID);
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(draftForList));
      when(assessmentRepository.countQuestionsByAssessmentId(any())).thenReturn(2L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(3.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentLessonRepository.findByAssessmentIdIn(any()))
          .thenReturn(Collections.emptyList());

      var list = courseService.getCourseAssessments(COURSE_ID, "DRAFT", "QUIZ", true);

      assertEquals(1, list.size());
    }

    @Test
    void it_should_ignore_invalid_status_token_in_filter() {
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      CourseAssessment ca =
          CourseAssessment.builder()
              .courseId(COURSE_ID)
              .assessmentId(ASSESSMENT_ID)
              .orderIndex(0)
              .isRequired(true)
              .build();
      ca.setId(UUID.fromString("30303030-3030-3030-3030-303030303031"));
      when(courseAssessmentRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(ca));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(Collections.emptyList());
      Assessment linked =
          Assessment.builder()
              .title("Kiểm tra giữa kỳ")
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.DRAFT)
              .build();
      linked.setId(ASSESSMENT_ID);
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(linked));
      when(assessmentRepository.countQuestionsByAssessmentId(any())).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(2.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentLessonRepository.findByAssessmentIdIn(any()))
          .thenReturn(Collections.emptyList());

      var list = courseService.getCourseAssessments(COURSE_ID, "NOT_A_STATUS", "QUIZ", true);

      assertEquals(1, list.size());
    }

    @Test
    void it_should_ignore_invalid_assessment_type_token_in_filter() {
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      CourseAssessment ca =
          CourseAssessment.builder()
              .courseId(COURSE_ID)
              .assessmentId(ASSESSMENT_ID)
              .orderIndex(0)
              .isRequired(true)
              .build();
      ca.setId(UUID.fromString("31313131-3131-3131-3131-313131313131"));
      when(courseAssessmentRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(ca));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(Collections.emptyList());
      Assessment linked =
          Assessment.builder()
              .title("Bài kiểm tra định kỳ")
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.DRAFT)
              .build();
      linked.setId(ASSESSMENT_ID);
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(linked));
      when(assessmentRepository.countQuestionsByAssessmentId(any())).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(1.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentLessonRepository.findByAssessmentIdIn(any()))
          .thenReturn(Collections.emptyList());

      var list = courseService.getCourseAssessments(COURSE_ID, "DRAFT", "NOT_A_TYPE", true);

      assertEquals(1, list.size());
    }

    @Test
    void it_should_filter_out_assessment_when_status_param_not_matching() {
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      CourseAssessment ca =
          CourseAssessment.builder()
              .courseId(COURSE_ID)
              .assessmentId(ASSESSMENT_ID)
              .orderIndex(0)
              .isRequired(true)
              .build();
      ca.setId(UUID.fromString("32323232-3232-3232-3232-323232323232"));
      when(courseAssessmentRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(ca));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(Collections.emptyList());
      Assessment linked =
          Assessment.builder()
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.DRAFT)
              .build();
      linked.setId(ASSESSMENT_ID);
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(linked));
      when(assessmentRepository.countQuestionsByAssessmentId(any())).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(1.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentLessonRepository.findByAssessmentIdIn(any()))
          .thenReturn(Collections.emptyList());

      var list = courseService.getCourseAssessments(COURSE_ID, "PUBLISHED", "QUIZ", true);

      assertTrue(list.isEmpty());
    }

    @Test
    void it_should_exclude_by_required_filter_when_mismatch() {
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      CourseAssessment ca =
          CourseAssessment.builder()
              .courseId(COURSE_ID)
              .assessmentId(ASSESSMENT_ID)
              .orderIndex(0)
              .isRequired(true)
              .build();
      ca.setId(UUID.fromString("30303030-3030-3030-3030-303030303032"));
      when(courseAssessmentRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(ca));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(Collections.emptyList());
      Assessment linked =
          Assessment.builder()
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.DRAFT)
              .build();
      linked.setId(ASSESSMENT_ID);
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(linked));
      when(assessmentRepository.countQuestionsByAssessmentId(any())).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(1.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(assessmentLessonRepository.findByAssessmentIdIn(any()))
          .thenReturn(Collections.emptyList());

      var list = courseService.getCourseAssessments(COURSE_ID, "DRAFT", "QUIZ", false);

      assertTrue(list.isEmpty());
    }

    @Test
    void it_should_return_empty_available_without_lessons_and_flag_false() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(Collections.emptyList());

      var list = courseService.getAvailableAssessmentsForCourse(COURSE_ID, false);

      assertTrue(list.isEmpty());
    }

    @Test
    void it_should_list_all_published_of_teacher_when_include_out_of_course() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM)));
      CourseLesson clOut =
          CourseLesson.builder().courseId(COURSE_ID).lessonId(LESSON_ID).build();
      clOut.setId(UUID.fromString("acacacac-acac-acac-acac-acacacacacac"));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(clOut));
      Assessment a =
          Assessment.builder()
              .teacherId(TEACHER_ID)
              .title("Đề tổng hợp")
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.PUBLISHED)
              .build();
      a.setId(ASSESSMENT_ID);
      a.setCreatedAt(Instant.now());
      when(assessmentRepository.findByTeacherIdAndStatusAndNotDeleted(
              eq(TEACHER_ID), eq(AssessmentStatus.PUBLISHED)))
          .thenReturn(List.of(a));
      when(lessonRepository.findByIdInAndNotDeleted(any())).thenReturn(Collections.emptyList());
      when(assessmentRepository.findBulkSummaryByIds(any()))
          .thenReturn(
              List.<Object[]>of(
                  new Object[] {ASSESSMENT_ID, 4L, new BigDecimal("7.5")}));
      when(assessmentLessonRepository.findByAssessmentIdIn(any()))
          .thenReturn(Collections.emptyList());

      var list = courseService.getAvailableAssessmentsForCourse(COURSE_ID, true);

      assertEquals(1, list.size());
      assertEquals(4, list.get(0).getTotalQuestions().longValue());
    }

    @Test
    void it_should_use_candidate_list_when_not_include_out_of_course() {
      authenticateAsTeacher(TEACHER_ID);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(
              Optional.of(
                  buildCourse(CourseStatus.DRAFT, false, CourseProvider.MINISTRY)));
      CourseLesson clCand =
          CourseLesson.builder().courseId(COURSE_ID).lessonId(LESSON_ID).build();
      clCand.setId(UUID.fromString("adadadad-adad-adad-adad-adadadadadad"));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(clCand));
      when(assessmentLessonRepository.findAssessmentIdsByLessonIds(any()))
          .thenReturn(List.of(ASSESSMENT_ID));
      Assessment a =
          Assessment.builder()
              .teacherId(TEACHER_ID)
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.PUBLISHED)
              .build();
      a.setId(ASSESSMENT_ID);
      a.setCreatedAt(Instant.now());
      when(assessmentRepository.findByIdInAndNotDeleted(any())).thenReturn(List.of(a));
      Lesson l1 = Lesson.builder().title("Bài 1").build();
      l1.setId(LESSON_ID);
      when(lessonRepository.findByIdInAndNotDeleted(any())).thenReturn(List.of(l1));
      when(assessmentRepository.findBulkSummaryByIds(any()))
          .thenReturn(
              List.<Object[]>of(
                  new Object[] {ASSESSMENT_ID, 2L, new BigDecimal("1.0")}));
      AssessmentLesson alink =
          AssessmentLesson.builder()
              .assessmentId(ASSESSMENT_ID)
              .lessonId(LESSON_ID)
              .build();
      alink.setId(UUID.fromString("40404040-4040-4040-4040-404040404040"));
      when(assessmentLessonRepository.findByAssessmentIdIn(any()))
          .thenReturn(List.of(alink));

      var list = courseService.getAvailableAssessmentsForCourse(COURSE_ID, false);

      assertEquals(1, list.size());
    }
  }

  @Nested
  @DisplayName("updateCourseAssessment() / removeAssessmentFromCourse()")
  class UpdateRemoveAssessmentTests {

    @Test
    void it_should_update_order_and_requirement() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      CourseAssessment ca = CourseAssessment.builder().courseId(COURSE_ID).assessmentId(ASSESSMENT_ID).orderIndex(0).isRequired(true).build();
      ca.setId(UUID.fromString("50505050-5050-5050-5050-505050505050"));
      when(courseAssessmentRepository.findByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(Optional.of(ca));
      when(courseAssessmentRepository.save(any(CourseAssessment.class)))
          .thenAnswer(inv -> inv.getArgument(0));
      Assessment updateView =
          Assessment.builder()
              .assessmentType(AssessmentType.QUIZ)
              .status(AssessmentStatus.PUBLISHED)
              .build();
      updateView.setId(ASSESSMENT_ID);
      when(assessmentRepository.findByIdAndNotDeleted(ASSESSMENT_ID))
          .thenReturn(Optional.of(updateView));
      when(assessmentRepository.countQuestionsByAssessmentId(any())).thenReturn(1L);
      when(assessmentRepository.calculateTotalPoints(any())).thenReturn(1.0);
      when(assessmentRepository.countSubmissionsByAssessmentId(any())).thenReturn(0L);
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(Collections.emptyList());
      when(assessmentLessonRepository.findLessonIdsByAssessmentId(any()))
          .thenReturn(Collections.emptyList());

      var res =
          courseService.updateCourseAssessment(
              COURSE_ID,
              ASSESSMENT_ID,
              UpdateCourseAssessmentRequest.builder().orderIndex(3).isRequired(false).build());

      assertEquals(3, res.getOrderIndex());
    }

    @Test
    void it_should_throw_exception_when_remove_has_submissions() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      CourseAssessment caSub =
          CourseAssessment.builder()
              .courseId(COURSE_ID)
              .assessmentId(ASSESSMENT_ID)
              .build();
      caSub.setId(UUID.fromString("60606060-6060-6060-6060-606060606060"));
      when(courseAssessmentRepository.findByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(Optional.of(caSub));
      when(assessmentRepository.countSubmissionsByAssessmentId(ASSESSMENT_ID)).thenReturn(1L);

      assertThrows(
          AppException.class,
          () -> courseService.removeAssessmentFromCourse(COURSE_ID, ASSESSMENT_ID));
    }

    @Test
    void it_should_soft_delete_course_assessment_when_no_submissions() {
      authenticateAsTeacher(TEACHER_ID);
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      CourseAssessment ca = CourseAssessment.builder().courseId(COURSE_ID).assessmentId(ASSESSMENT_ID).build();
      ca.setId(UUID.fromString("70707070-7070-7070-7070-707070707070"));
      when(courseAssessmentRepository.findByCourseIdAndAssessmentIdAndNotDeleted(
              COURSE_ID, ASSESSMENT_ID))
          .thenReturn(Optional.of(ca));
      when(assessmentRepository.countSubmissionsByAssessmentId(ASSESSMENT_ID)).thenReturn(0L);
      when(courseAssessmentRepository.save(any(CourseAssessment.class)))
          .thenAnswer(inv -> inv.getArgument(0));

      courseService.removeAssessmentFromCourse(COURSE_ID, ASSESSMENT_ID);

      assertNotNull(ca.getDeletedAt());
    }
  }

  @Nested
  @DisplayName("syncCourseMetrics() + notification + thumbnail + map branches")
  class MiscCoverageTests {

    @Test
    void it_should_aggregate_metrics_and_save_course() throws Exception {
      Course course = buildCourse(CourseStatus.DRAFT, false, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      CourseLesson cl1 = CourseLesson.builder()
          .courseId(COURSE_ID)
          .durationSeconds(3600)
          .videoUrl("https://video.example.com/lesson-a")
          .materials("[{\"k\":\"v\"}]")
          .build();
      cl1.setId(UUID.fromString("80808080-8080-8080-8080-808080808080"));
      CourseLesson cl2 = CourseLesson.builder()
          .courseId(COURSE_ID)
          .videoUrl("   ")
          .materials("not json")
          .build();
      cl2.setId(UUID.fromString("90909090-9090-9090-9090-909090909090"));
      when(courseLessonRepository.findByCourseIdAndNotDeleted(COURSE_ID))
          .thenReturn(List.of(cl1, cl2));
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

      courseService.syncCourseMetrics(COURSE_ID);

      assertNotNull(course.getTotalVideoHours());
    }

    @Test
    void it_should_resilience_when_notification_publish_fails() {
      authenticateAsAdmin(ADMIN_ID);
      Course course = buildCourse(CourseStatus.PENDING_REVIEW, false, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
      doThrow(new RuntimeException("NATS down")).when(streamPublisher).publish(any());
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      assertDoesNotThrow(() -> courseService.approveCourse(COURSE_ID));
    }

    @Test
    void it_should_return_presigned_or_fallback_on_thumbnail() {
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.CUSTOM);
      course.setThumbnailUrl("relative/minio-key.png");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(uploadService.getPresignedUrl("relative/minio-key.png", "slide-templates"))
          .thenReturn("https://cdn.example.com/presigned.png");
      stubSubjectGradeLookup();
      when(userRepository.findById(TEACHER_ID))
          .thenReturn(
              Optional.of(
                  buildUser(TEACHER_ID, "Giảng viên Đoàn Hải", "hai.d@fptu.edu.vn")));

      var res = courseService.getCourseById(COURSE_ID);

      assertNotNull(res.getThumbnailUrl());
    }

    @Test
    void it_should_fallback_to_raw_key_when_presigned_fails() {
      authenticateAsStudent(STUDENT_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      course.setThumbnailUrl("khoa-hoc/anh-bia-local.png");
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(uploadService.getPresignedUrl("khoa-hoc/anh-bia-local.png", "slide-templates"))
          .thenThrow(new RuntimeException("MinIO unavailable"));
      stubSubjectGradeLookup();
      stubMapResponseData(TEACHER_ID);

      var res = courseService.getCourseById(COURSE_ID);

      assertEquals("khoa-hoc/anh-bia-local.png", res.getThumbnailUrl());
    }

    @Test
    void it_should_map_progress_when_student_enrolled() {
      authenticateAsStudent(STUDENT_ID);
      Course course = buildCourse(CourseStatus.PUBLISHED, true, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      stubSubjectGradeLookup();
      User teacher =
          buildUser(TEACHER_ID, "Giảng viên Nguyễn Sơn Nam", "nam.ns@fptu.edu.vn");
      when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
      when(teacherProfileRepository.findByUserId(TEACHER_ID)).thenReturn(Optional.empty());
      when(enrollmentRepository.countActiveEnrollmentsByCourseId(COURSE_ID)).thenReturn(1L);
      when(courseLessonRepository.countByCourseIdAndNotDeleted(COURSE_ID)).thenReturn(4L);
      when(customCourseSectionRepository.countByCourseIdAndDeletedAtIsNull(COURSE_ID))
          .thenReturn(0L);
      when(courseReviewRepository.countByCourseIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(0L);
      UUID eid = UUID.fromString("aabbccdd-eeff-0011-2233-445566770011");
      Enrollment e =
          Enrollment.builder()
              .courseId(COURSE_ID)
              .studentId(STUDENT_ID)
              .status(EnrollmentStatus.ACTIVE)
              .build();
      e.setId(eid);
      when(enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(STUDENT_ID, COURSE_ID))
          .thenReturn(Optional.of(e));
      when(lessonProgressRepository.countCompletedByEnrollmentId(eid)).thenReturn(2L);

      CourseResponse res = courseService.getCourseById(COURSE_ID);

      assertTrue(Boolean.TRUE.equals(res.getIsEnrolled()));
      assertEquals(50.0, res.getProgress(), 1e-9);
    }
  }

  @Nested
  @DisplayName("getMaterialListStatic (reflection)")
  class PrivateHelperTests {

    @Test
    void it_should_parse_and_handle_invalid_json_via_reflection() throws ReflectiveOperationException {
      Method m =
          CourseServiceImpl.class.getDeclaredMethod("getMaterialListStatic", String.class);
      m.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<?> empty = (List<?>) m.invoke(courseService, (Object) null);
      assertTrue(empty.isEmpty());
      List<?> invalid = (List<?>) m.invoke(courseService, "{ not json");
      assertTrue(invalid.isEmpty());
      List<?> valid =
          (List<?>)
              m.invoke(
                  courseService,
                  "[{\"name\":\"Tài liệu tham khảo\",\"key\":\"materials/tai-lieu.pdf\"}]");
      assertEquals(1, valid.size());
    }
  }
}
