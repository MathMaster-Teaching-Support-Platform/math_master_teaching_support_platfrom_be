package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateLessonRequest;
import com.fptu.math_master.dto.request.UpdateLessonRequest;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@DisplayName("LessonServiceImpl - Tests")
class LessonServiceImplTest extends BaseUnitTest {

  @InjectMocks private LessonServiceImpl lessonService;

  @Mock private LessonRepository lessonRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private UserRepository userRepository;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  private static final UUID ADMIN_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
  private static final UUID CHAPTER_ID = UUID.fromString("70000000-0000-0000-0000-000000000002");
  private static final UUID LESSON_ID = UUID.fromString("70000000-0000-0000-0000-000000000003");

  @BeforeEach
  void setUp() {
    securityUtilsMock = Mockito.mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(ADMIN_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  private User buildAdminUser(UUID id) {
    Role role = new Role();
    role.setName("ADMIN");
    User user = new User();
    user.setId(id);
    user.setRoles(Set.of(role));
    return user;
  }

  private Lesson buildLesson(UUID id, UUID chapterId, String title) {
    Lesson lesson = new Lesson();
    lesson.setId(id);
    lesson.setChapterId(chapterId);
    lesson.setTitle(title);
    lesson.setLessonContent("Noi dung bai hoc");
    lesson.setOrderIndex(1);
    lesson.setStatus(LessonStatus.DRAFT);
    lesson.setDifficulty(LessonDifficulty.BEGINNER);
    lesson.setCreatedAt(Instant.parse("2026-04-26T04:00:00Z"));
    lesson.setUpdatedAt(Instant.parse("2026-04-26T04:00:00Z"));
    return lesson;
  }

  @Nested
  @DisplayName("createLesson()")
  class CreateLessonTests {

    @Test
    void it_should_create_lesson_with_calculated_order_index_when_request_order_is_null() {
      // ===== ARRANGE =====
      CreateLessonRequest request =
          CreateLessonRequest.builder()
              .chapterId(CHAPTER_ID)
              .title("Gioi han ham so")
              .lessonContent("Noi dung")
              .summary("Tong ket")
              .difficulty(LessonDifficulty.INTERMEDIATE)
              .durationMinutes(45)
              .orderIndex(null)
              .build();
      when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(buildAdminUser(ADMIN_ID)));
      Chapter chapter = new Chapter();
      chapter.setId(CHAPTER_ID);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
      when(lessonRepository.countByChapterIdAndNotDeleted(CHAPTER_ID)).thenReturn(2L);
      Lesson saved = buildLesson(LESSON_ID, CHAPTER_ID, "Gioi han ham so");
      saved.setOrderIndex(3);
      when(lessonRepository.save(any(Lesson.class))).thenReturn(saved);

      // ===== ACT =====
      var response = lessonService.createLesson(request);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(LESSON_ID, response.getId()), () -> assertEquals(3, response.getOrderIndex()));

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).countByChapterIdAndNotDeleted(CHAPTER_ID);
      verify(lessonRepository, times(1)).save(any(Lesson.class));
    }

    @Test
    void it_should_create_lesson_with_explicit_order_index_when_request_contains_order() {
      // ===== ARRANGE =====
      CreateLessonRequest request =
          CreateLessonRequest.builder()
              .chapterId(CHAPTER_ID)
              .title("Dao ham")
              .lessonContent("Noi dung dao ham")
              .orderIndex(8)
              .build();
      when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(buildAdminUser(ADMIN_ID)));
      Chapter chapter = new Chapter();
      chapter.setId(CHAPTER_ID);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
      Lesson saved = buildLesson(LESSON_ID, CHAPTER_ID, "Dao ham");
      saved.setOrderIndex(8);
      when(lessonRepository.save(any(Lesson.class))).thenReturn(saved);

      // ===== ACT =====
      var response = lessonService.createLesson(request);

      // ===== ASSERT =====
      assertEquals(8, response.getOrderIndex());

      // ===== VERIFY =====
      verify(lessonRepository, never()).countByChapterIdAndNotDeleted(CHAPTER_ID);
      verify(lessonRepository, times(1)).save(any(Lesson.class));
    }

    @Test
    void it_should_throw_unauthorized_when_current_user_is_not_admin() {
      // ===== ARRANGE =====
      CreateLessonRequest request =
          CreateLessonRequest.builder()
              .chapterId(CHAPTER_ID)
              .title("Bat dang thuc")
              .lessonContent("Noi dung")
              .build();
      Role teacherRole = new Role();
      teacherRole.setName("TEACHER");
      User teacher = new User();
      teacher.setId(ADMIN_ID);
      teacher.setRoles(Set.of(teacherRole));
      when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(teacher));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> lessonService.createLesson(request));
      assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void it_should_throw_user_not_existed_when_admin_user_not_found() {
      // ===== ARRANGE =====
      CreateLessonRequest request =
          CreateLessonRequest.builder()
              .chapterId(CHAPTER_ID)
              .title("Tich phan")
              .lessonContent("Noi dung")
              .build();
      when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> lessonService.createLesson(request));
      assertEquals(ErrorCode.USER_NOT_EXISTED, exception.getErrorCode());
    }

    @Test
    void it_should_throw_chapter_not_found_when_chapter_missing_or_deleted() {
      // ===== ARRANGE =====
      CreateLessonRequest request =
          CreateLessonRequest.builder()
              .chapterId(CHAPTER_ID)
              .title("Ham so bac hai")
              .lessonContent("Noi dung")
              .build();
      when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(buildAdminUser(ADMIN_ID)));
      Chapter deleted = new Chapter();
      deleted.setId(CHAPTER_ID);
      deleted.setDeletedAt(Instant.now());
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(deleted));

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> lessonService.createLesson(request));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("search and get methods")
  class SearchAndGetTests {

    @Test
    void it_should_get_lesson_by_id_when_lesson_exists() {
      // ===== ARRANGE =====
      Lesson lesson = buildLesson(LESSON_ID, CHAPTER_ID, "He truc toa do");
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(lesson));

      // ===== ACT =====
      var response = lessonService.getLessonById(LESSON_ID);

      // ===== ASSERT =====
      assertEquals("He truc toa do", response.getTitle());
    }

    @Test
    void it_should_throw_lesson_not_found_when_get_lesson_by_id_missing() {
      // ===== ARRANGE =====
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception = assertThrows(AppException.class, () -> lessonService.getLessonById(LESSON_ID));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void it_should_search_lessons_by_chapter_with_blank_name_using_find_all() {
      // ===== ARRANGE =====
      Chapter chapter = new Chapter();
      chapter.setId(CHAPTER_ID);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
      Lesson l1 = buildLesson(LESSON_ID, CHAPTER_ID, "Bai 1");
      when(lessonRepository.findByChapterIdAndNotDeleted(CHAPTER_ID)).thenReturn(List.of(l1));

      // ===== ACT =====
      var responses = lessonService.searchLessonsByChapterId(CHAPTER_ID, "   ");

      // ===== ASSERT =====
      assertEquals(1, responses.size());
      verify(lessonRepository, times(1)).findByChapterIdAndNotDeleted(CHAPTER_ID);
      verify(lessonRepository, never()).findByChapterIdAndTitleContainingAndNotDeleted(any(), any());
    }

    @Test
    void it_should_search_lessons_by_chapter_with_keyword_using_trimmed_name() {
      // ===== ARRANGE =====
      Chapter chapter = new Chapter();
      chapter.setId(CHAPTER_ID);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
      Lesson l1 = buildLesson(LESSON_ID, CHAPTER_ID, "Ung dung dao ham");
      when(lessonRepository.findByChapterIdAndTitleContainingAndNotDeleted(CHAPTER_ID, "dao ham"))
          .thenReturn(List.of(l1));

      // ===== ACT =====
      var responses = lessonService.searchLessonsByChapterId(CHAPTER_ID, "  dao ham  ");

      // ===== ASSERT =====
      assertEquals(1, responses.size());
      verify(lessonRepository, times(1)).findByChapterIdAndTitleContainingAndNotDeleted(CHAPTER_ID, "dao ham");
      verify(lessonRepository, never()).findByChapterIdAndNotDeleted(CHAPTER_ID);
    }

    @Test
    void it_should_throw_chapter_not_found_when_search_uses_deleted_chapter() {
      // ===== ARRANGE =====
      Chapter deleted = new Chapter();
      deleted.setId(CHAPTER_ID);
      deleted.setDeletedAt(Instant.now());
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(deleted));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> lessonService.searchLessonsByChapterId(CHAPTER_ID, null));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, exception.getErrorCode());
    }
  }

  @Nested
  @DisplayName("updateLesson() and deleteLesson()")
  class UpdateAndDeleteTests {

    @Test
    void it_should_update_all_mutable_fields_when_request_has_non_null_values() {
      // ===== ARRANGE =====
      when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(buildAdminUser(ADMIN_ID)));
      Lesson existing = buildLesson(LESSON_ID, CHAPTER_ID, "Ten cu");
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(existing));
      when(lessonRepository.save(existing)).thenReturn(existing);
      UpdateLessonRequest request =
          UpdateLessonRequest.builder()
              .title("Ten moi")
              .learningObjectives("Muc tieu moi")
              .lessonContent("Noi dung moi")
              .summary("Tom tat moi")
              .orderIndex(9)
              .durationMinutes(90)
              .difficulty(LessonDifficulty.ADVANCED)
              .status(LessonStatus.PUBLISHED)
              .build();

      // ===== ACT =====
      var response = lessonService.updateLesson(LESSON_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Ten moi", response.getTitle()),
          () -> assertEquals("Muc tieu moi", response.getLearningObjectives()),
          () -> assertEquals("Noi dung moi", response.getLessonContent()),
          () -> assertEquals("Tom tat moi", response.getSummary()),
          () -> assertEquals(9, response.getOrderIndex()),
          () -> assertEquals(90, response.getDurationMinutes()),
          () -> assertEquals(LessonDifficulty.ADVANCED, response.getDifficulty()),
          () -> assertEquals(LessonStatus.PUBLISHED, response.getStatus()));
    }

    @Test
    void it_should_keep_original_values_when_update_request_fields_are_all_null() {
      // ===== ARRANGE =====
      when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(buildAdminUser(ADMIN_ID)));
      Lesson existing = buildLesson(LESSON_ID, CHAPTER_ID, "Gia tri goc");
      existing.setLearningObjectives("Objectives goc");
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(existing));
      when(lessonRepository.save(existing)).thenReturn(existing);
      UpdateLessonRequest request = UpdateLessonRequest.builder().build();

      // ===== ACT =====
      var response = lessonService.updateLesson(LESSON_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Gia tri goc", response.getTitle()),
          () -> assertEquals("Objectives goc", response.getLearningObjectives()));
    }

    @Test
    void it_should_soft_delete_lesson_when_delete_called_with_existing_lesson() {
      // ===== ARRANGE =====
      Lesson existing = buildLesson(LESSON_ID, CHAPTER_ID, "Can xoa");
      when(lessonRepository.findByIdAndNotDeleted(LESSON_ID)).thenReturn(Optional.of(existing));
      when(lessonRepository.save(existing)).thenReturn(existing);

      // ===== ACT =====
      lessonService.deleteLesson(LESSON_ID);

      // ===== ASSERT =====
      assertNotNull(existing.getDeletedAt());
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(LESSON_ID);
      verify(lessonRepository, times(1)).save(existing);
      verifyNoMoreInteractions(lessonRepository);
    }
  }
}
