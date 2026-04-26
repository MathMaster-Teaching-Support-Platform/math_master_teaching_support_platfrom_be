package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateCustomCourseSectionRequest;
import com.fptu.math_master.dto.request.UpdateCustomCourseSectionRequest;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CustomCourseSection;
import com.fptu.math_master.enums.CourseProvider;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.CustomCourseSectionRepository;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@DisplayName("CustomCourseSectionServiceImpl - Tests")
class CustomCourseSectionServiceImplTest extends BaseUnitTest {

  @InjectMocks private CustomCourseSectionServiceImpl customCourseSectionService;

  @Mock private CustomCourseSectionRepository sectionRepository;
  @Mock private CourseRepository courseRepository;

  private static final UUID COURSE_ID = UUID.fromString("95000000-0000-0000-0000-000000000001");
  private static final UUID SECTION_ID = UUID.fromString("95000000-0000-0000-0000-000000000002");
  private static final UUID TEACHER_ID = UUID.fromString("95000000-0000-0000-0000-000000000003");

  private Course buildCourse(UUID id, UUID teacherId, CourseProvider provider) {
    Course course = new Course();
    course.setId(id);
    course.setTeacherId(teacherId);
    course.setProvider(provider);
    return course;
  }

  private CustomCourseSection buildSection(UUID id, UUID courseId, String title, int orderIndex) {
    CustomCourseSection section = new CustomCourseSection();
    section.setId(id);
    section.setCourseId(courseId);
    section.setTitle(title);
    section.setDescription("Mo ta");
    section.setOrderIndex(orderIndex);
    section.setCreatedAt(Instant.parse("2026-04-26T08:00:00Z"));
    section.setUpdatedAt(Instant.parse("2026-04-26T08:00:00Z"));
    return section;
  }

  @Nested
  @DisplayName("createSection()")
  class CreateSectionTests {

    @Test
    void it_should_create_section_when_teacher_owns_custom_course() {
      // ===== ARRANGE =====
      CreateCustomCourseSectionRequest request =
          CreateCustomCourseSectionRequest.builder()
              .title("Phan 1")
              .description("Mo ta")
              .orderIndex(1)
              .build();
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CustomCourseSection saved = buildSection(SECTION_ID, COURSE_ID, "Phan 1", 1);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(sectionRepository.save(any(CustomCourseSection.class))).thenReturn(saved);

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);

        // ===== ACT =====
        var response = customCourseSectionService.createSection(COURSE_ID, request);

        // ===== ASSERT =====
        assertAll(() -> assertEquals(SECTION_ID, response.getId()), () -> assertEquals(0, response.getLessonCount()));
      }
    }

    @Test
    void it_should_throw_course_not_found_when_create_for_missing_course() {
      // ===== ARRANGE =====
      CreateCustomCourseSectionRequest request =
          CreateCustomCourseSectionRequest.builder().title("x").orderIndex(1).build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.empty());

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(AppException.class, () -> customCourseSectionService.createSection(COURSE_ID, request));
        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getErrorCode());
      }
    }

    @Test
    void it_should_throw_access_denied_when_create_by_non_owner() {
      // ===== ARRANGE =====
      CreateCustomCourseSectionRequest request =
          CreateCustomCourseSectionRequest.builder().title("x").orderIndex(1).build();
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(UUID.fromString("95000000-0000-0000-0000-000000000099"));

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(AppException.class, () -> customCourseSectionService.createSection(COURSE_ID, request));
        assertEquals(ErrorCode.COURSE_ACCESS_DENIED, exception.getErrorCode());
      }
    }

    @Test
    void it_should_throw_operation_not_supported_when_provider_not_custom() {
      // ===== ARRANGE =====
      CreateCustomCourseSectionRequest request =
          CreateCustomCourseSectionRequest.builder().title("x").orderIndex(1).build();
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.MINISTRY);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);

        // ===== ACT & ASSERT =====
        AppException exception =
            assertThrows(AppException.class, () -> customCourseSectionService.createSection(COURSE_ID, request));
        assertEquals(ErrorCode.OPERATION_NOT_SUPPORTED_FOR_PROVIDER, exception.getErrorCode());
      }
    }
  }

  @Nested
  @DisplayName("listSections()")
  class ListSectionsTests {

    @Test
    void it_should_list_sections_and_map_lesson_count() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CustomCourseSection s1 = buildSection(SECTION_ID, COURSE_ID, "Phan 1", 1);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(sectionRepository.findByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(COURSE_ID))
          .thenReturn(List.of(s1));
      when(sectionRepository.countActiveLessonsBySectionId(SECTION_ID)).thenReturn(3L);

      // ===== ACT =====
      var response = customCourseSectionService.listSections(COURSE_ID);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, response.size()), () -> assertEquals(3, response.get(0).getLessonCount()));
    }
  }

  @Nested
  @DisplayName("updateSection() and deleteSection()")
  class UpdateAndDeleteTests {

    @Test
    void it_should_update_section_when_valid_course_owner_and_section_in_course() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CustomCourseSection section = buildSection(SECTION_ID, COURSE_ID, "Cu", 1);
      UpdateCustomCourseSectionRequest request =
          UpdateCustomCourseSectionRequest.builder().title("Moi").description(null).orderIndex(5).build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(sectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID)).thenReturn(Optional.of(section));
      when(sectionRepository.save(section)).thenReturn(section);
      when(sectionRepository.countActiveLessonsBySectionId(SECTION_ID)).thenReturn(2L);

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);

        // ===== ACT =====
        var response = customCourseSectionService.updateSection(COURSE_ID, SECTION_ID, request);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals("Moi", response.getTitle()),
            () -> assertEquals("Mo ta", response.getDescription()),
            () -> assertEquals(5, response.getOrderIndex()),
            () -> assertEquals(2, response.getLessonCount()));
      }
    }

    @Test
    void it_should_not_update_title_when_update_title_is_blank() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CustomCourseSection section = buildSection(SECTION_ID, COURSE_ID, "Original", 1);
      UpdateCustomCourseSectionRequest request =
          UpdateCustomCourseSectionRequest.builder().title("   ").description("Desc moi").build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(sectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID)).thenReturn(Optional.of(section));
      when(sectionRepository.save(section)).thenReturn(section);
      when(sectionRepository.countActiveLessonsBySectionId(SECTION_ID)).thenReturn(0L);

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);

        // ===== ACT =====
        var response = customCourseSectionService.updateSection(COURSE_ID, SECTION_ID, request);

        // ===== ASSERT =====
        assertAll(
            () -> assertEquals("Original", response.getTitle()),
            () -> assertEquals("Desc moi", response.getDescription()));
      }
    }

    @Test
    void it_should_throw_not_found_or_not_in_course_when_update_invalid_section() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      UpdateCustomCourseSectionRequest request = UpdateCustomCourseSectionRequest.builder().build();
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(sectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID)).thenReturn(Optional.empty());

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        AppException notFound =
            assertThrows(
                AppException.class,
                () -> customCourseSectionService.updateSection(COURSE_ID, SECTION_ID, request));
        assertEquals(ErrorCode.CUSTOM_COURSE_SECTION_NOT_FOUND, notFound.getErrorCode());
      }

      CustomCourseSection otherCourseSection =
          buildSection(SECTION_ID, UUID.fromString("95000000-0000-0000-0000-000000000088"), "x", 1);
      when(sectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID)).thenReturn(Optional.of(otherCourseSection));
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        AppException notInCourse =
            assertThrows(
                AppException.class,
                () -> customCourseSectionService.updateSection(COURSE_ID, SECTION_ID, request));
        assertEquals(ErrorCode.SECTION_NOT_IN_COURSE, notInCourse.getErrorCode());
      }
    }

    @Test
    void it_should_soft_delete_section_when_no_active_lessons() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CustomCourseSection section = buildSection(SECTION_ID, COURSE_ID, "x", 1);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(sectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID)).thenReturn(Optional.of(section));
      when(sectionRepository.countActiveLessonsBySectionId(SECTION_ID)).thenReturn(0L);
      when(sectionRepository.save(section)).thenReturn(section);

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);

        // ===== ACT =====
        customCourseSectionService.deleteSection(COURSE_ID, SECTION_ID);

        // ===== ASSERT =====
        assertNotNull(section.getDeletedAt());
        assertEquals(TEACHER_ID, section.getDeletedBy());
      }
    }

    @Test
    void it_should_throw_when_delete_has_active_lessons_or_section_not_in_course() {
      // ===== ARRANGE =====
      Course course = buildCourse(COURSE_ID, TEACHER_ID, CourseProvider.CUSTOM);
      CustomCourseSection section = buildSection(SECTION_ID, COURSE_ID, "x", 1);
      when(courseRepository.findByIdAndDeletedAtIsNull(COURSE_ID)).thenReturn(Optional.of(course));
      when(sectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID)).thenReturn(Optional.of(section));
      when(sectionRepository.countActiveLessonsBySectionId(SECTION_ID)).thenReturn(1L);

      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        AppException hasLessons =
            assertThrows(
                AppException.class,
                () -> customCourseSectionService.deleteSection(COURSE_ID, SECTION_ID));
        assertEquals(ErrorCode.CUSTOM_COURSE_SECTION_HAS_LESSONS, hasLessons.getErrorCode());
      }

      CustomCourseSection sectionOtherCourse =
          buildSection(SECTION_ID, UUID.fromString("95000000-0000-0000-0000-000000000077"), "x", 1);
      when(sectionRepository.findByIdAndDeletedAtIsNull(SECTION_ID)).thenReturn(Optional.of(sectionOtherCourse));
      try (MockedStatic<SecurityUtils> mocked = Mockito.mockStatic(SecurityUtils.class)) {
        mocked.when(SecurityUtils::getCurrentUserId).thenReturn(TEACHER_ID);
        AppException notInCourse =
            assertThrows(
                AppException.class,
                () -> customCourseSectionService.deleteSection(COURSE_ID, SECTION_ID));
        assertEquals(ErrorCode.SECTION_NOT_IN_COURSE, notInCourse.getErrorCode());
      }

      verify(sectionRepository, never()).countByCourseIdAndDeletedAtIsNull(any());
    }
  }
}
