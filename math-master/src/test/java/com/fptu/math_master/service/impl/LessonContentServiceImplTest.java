package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.GenerateLessonContentRequest;
import com.fptu.math_master.dto.response.ChapterResponse;
import com.fptu.math_master.dto.response.GenerateLessonContentResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GeminiService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@DisplayName("LessonContentServiceImpl - Tests")
class LessonContentServiceImplTest extends BaseUnitTest {

  @InjectMocks private LessonContentServiceImpl lessonContentService;

  @Mock private LessonRepository lessonRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private CurriculumRepository curriculumRepository;
  @Mock private UserRepository userRepository;
  @Mock private GeminiService geminiService;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  private User adminUser;
  private UUID adminId;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void mockJwt(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("lesson-content-token")
            .header("alg", "none")
            .subject(userId.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }

  private User buildMockUser(UUID id, String fullName, String roleName) {
    Role role = new Role();
    role.setName(roleName);
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    user.setEmail("pham.ngoc.anh@fptu.edu.vn");
    user.setRoles(Set.of(role));
    return user;
  }

  private Curriculum buildCurriculum(UUID id, String name, Integer grade, CurriculumCategory category) {
    Curriculum curriculum = new Curriculum();
    curriculum.setId(id);
    curriculum.setName(name);
    curriculum.setGrade(grade);
    curriculum.setCategory(category);
    curriculum.setDescription("Curriculum for Vietnamese high-school mathematics");
    curriculum.setChapters(Set.of());
    return curriculum;
  }

  private Chapter buildChapter(UUID id, UUID curriculumId, String title, boolean deleted) {
    Chapter chapter = new Chapter();
    chapter.setId(id);
    chapter.setCurriculumId(curriculumId);
    chapter.setTitle(title);
    chapter.setDescription("Chapter details for lesson progression");
    chapter.setOrderIndex(1);
    chapter.setDeletedAt(deleted ? Instant.now() : null);
    chapter.setLessons(Set.of());
    return chapter;
  }

  private Lesson buildLesson(
      UUID id, UUID chapterId, String title, String content, LessonDifficulty difficulty, boolean deleted) {
    Lesson lesson = new Lesson();
    lesson.setId(id);
    lesson.setChapterId(chapterId);
    lesson.setTitle(title);
    lesson.setLearningObjectives("Students apply theorem and solve exercises");
    lesson.setLessonContent(content);
    lesson.setSummary("Lesson summary");
    lesson.setOrderIndex(1);
    lesson.setDurationMinutes(45);
    lesson.setDifficulty(difficulty);
    lesson.setStatus(LessonStatus.PUBLISHED);
    lesson.setDeletedAt(deleted ? Instant.now() : null);
    return lesson;
  }

  private GenerateLessonContentRequest buildGenerateRequest(boolean skipIfExists) {
    return GenerateLessonContentRequest.builder()
        .gradeLevel("Lớp 10")
        .subject("Hình học")
        .additionalContext("focus on vector and coordinate techniques")
        .lessonCount(1)
        .chaptersPerLesson(2)
        .skipIfExists(skipIfExists)
        .build();
  }

  @SuppressWarnings("unchecked")
  private <T> T invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args)
      throws Exception {
    Method method = LessonContentServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
    method.setAccessible(true);
    return (T) method.invoke(lessonContentService, args);
  }

  @Nested
  @DisplayName("generateAndSaveContent()")
  class GenerateAndSaveContentTests {

    /**
     * Normal case: Tạo curriculum mới và lưu lesson/chapter từ kết quả AI hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>request: gradeLevel=Lớp 10, subject=Hình học, skipIfExists=false</li>
     *   <li>aiResponse: JSON array chứa 1 lesson và 2 chapter</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByNameAndGradeAndCategoryAndNotDeleted -> Optional.empty (nhánh tạo curriculum mới)</li>
     *   <li>request.isSkipIfExists() -> FALSE branch</li>
     *   <li>aiLesson.getChapters() != null && !isEmpty -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về response có totalLessonsCreated=2 và totalChaptersCreated=2</li>
     *   <li>Các repository save được gọi đúng số lần</li>
     * </ul>
     */
    @Test
    void it_should_create_curriculum_and_save_lessons_when_ai_response_is_valid() {
      // ===== ARRANGE =====
      adminId = UUID.fromString("7a000000-0000-0000-0000-000000000001");
      adminUser = buildMockUser(adminId, "Phạm Ngọc Anh", "ADMIN");
      mockJwt(adminId);

      GenerateLessonContentRequest request = buildGenerateRequest(false);
      Curriculum savedCurriculum =
          buildCurriculum(
              UUID.fromString("7a000000-0000-0000-0000-000000000010"),
              "Mathematics Lớp 10 - Hình học",
              10,
              CurriculumCategory.GEOMETRY);
      Chapter chapter1 =
          buildChapter(
              UUID.fromString("7a000000-0000-0000-0000-000000000020"),
              savedCurriculum.getId(),
              "Vector cơ bản",
              false);
      Chapter chapter2 =
          buildChapter(
              UUID.fromString("7a000000-0000-0000-0000-000000000021"),
              savedCurriculum.getId(),
              "Tọa độ trong mặt phẳng",
              false);
      Lesson savedLesson =
          buildLesson(
              UUID.fromString("7a000000-0000-0000-0000-000000000030"),
              chapter1.getId(),
              "Vector và tọa độ",
              "Nội dung bài học về vector và hệ tọa độ.",
              LessonDifficulty.BEGINNER,
              false);
      String aiResponse =
          """
          [
            {
              "title":"Vector và tọa độ",
              "description":"Nội dung bài học về vector và hệ tọa độ.",
              "durationMinutes":45,
              "difficulty":"BEGINNER",
              "chapters":[
                {"title":"Vector cơ bản","description":"Khái niệm và phép toán vector."},
                {"title":"Tọa độ trong mặt phẳng","description":"Biểu diễn điểm và đường thẳng."}
              ]
            }
          ]
          """;

      when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(
              "Mathematics Lớp 10 - Hình học", 10, CurriculumCategory.GEOMETRY))
          .thenReturn(Optional.empty());
      when(curriculumRepository.save(any(Curriculum.class))).thenReturn(savedCurriculum);
      when(geminiService.sendMessage(any(String.class))).thenReturn(aiResponse);
      when(chapterRepository.save(any(Chapter.class))).thenReturn(chapter1, chapter2);
      when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);

      // ===== ACT =====
      GenerateLessonContentResponse result = lessonContentService.generateAndSaveContent(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertNotNull(result),
          () -> assertEquals("Lớp 10", result.getGradeLevel()),
          () -> assertEquals("Hình học", result.getSubject()),
          () -> assertEquals(2, result.getTotalLessonsCreated()),
          () -> assertEquals(2, result.getTotalChaptersCreated()),
          () -> assertEquals(0, result.getSkippedLessons()),
          () -> assertEquals(1, result.getLessons().size()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(adminId);
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              "Mathematics Lớp 10 - Hình học", 10, CurriculumCategory.GEOMETRY);
      verify(curriculumRepository, times(1)).save(any(Curriculum.class));
      verify(geminiService, times(1)).sendMessage(any(String.class));
      verify(chapterRepository, times(2)).save(any(Chapter.class));
      verify(lessonRepository, times(2)).save(any(Lesson.class));
      verifyNoMoreInteractions(
          userRepository, curriculumRepository, geminiService, chapterRepository, lessonRepository);
    }

    /**
     * Normal case: Bỏ qua lesson đã tồn tại khi skipIfExists=true.
     *
     * <p>Input:
     * <ul>
     *   <li>request: skipIfExists=true</li>
     *   <li>curriculum: có chapter chứa lesson cùng title và chưa bị xóa</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>request.isSkipIfExists() -> TRUE branch</li>
     *   <li>exists -> TRUE branch (continue, không save chapter/lesson)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>skippedLessons=1, totalLessonsCreated=0, totalChaptersCreated=0</li>
     * </ul>
     */
    @Test
    void it_should_skip_existing_lesson_when_skip_if_exists_is_true() {
      // ===== ARRANGE =====
      adminId = UUID.fromString("7a000000-0000-0000-0000-000000000002");
      adminUser = buildMockUser(adminId, "Nguyen Quang Huy", "ADMIN");
      mockJwt(adminId);

      GenerateLessonContentRequest request = buildGenerateRequest(true);
      Curriculum curriculum =
          buildCurriculum(
              UUID.fromString("7a000000-0000-0000-0000-000000000011"),
              "Mathematics Lớp 10 - Hình học",
              10,
              CurriculumCategory.GEOMETRY);
      Chapter chapter =
          buildChapter(
              UUID.fromString("7a000000-0000-0000-0000-000000000022"),
              curriculum.getId(),
              "Vector cơ bản",
              false);
      Lesson existingLesson =
          buildLesson(
              UUID.fromString("7a000000-0000-0000-0000-000000000031"),
              chapter.getId(),
              "Vector và tọa độ",
              "Nội dung đã tồn tại",
              LessonDifficulty.INTERMEDIATE,
              false);
      chapter.setLessons(Set.of(existingLesson));
      curriculum.setChapters(Set.of(chapter));

      String aiResponse =
          """
          [{"title":"Vector và tọa độ","description":"Mô tả mới","durationMinutes":45,"difficulty":"ADVANCED",
          "chapters":[{"title":"Vector cơ bản","description":"Mô tả chương"}]}]
          """;

      when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(
              "Mathematics Lớp 10 - Hình học", 10, CurriculumCategory.GEOMETRY))
          .thenReturn(Optional.of(curriculum));
      when(geminiService.sendMessage(any(String.class))).thenReturn(aiResponse);

      // ===== ACT =====
      GenerateLessonContentResponse result = lessonContentService.generateAndSaveContent(request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(0, result.getTotalLessonsCreated()),
          () -> assertEquals(0, result.getTotalChaptersCreated()),
          () -> assertEquals(1, result.getSkippedLessons()),
          () -> assertTrue(result.getLessons().isEmpty()));

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(adminId);
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              "Mathematics Lớp 10 - Hình học", 10, CurriculumCategory.GEOMETRY);
      verify(curriculumRepository, never()).save(any(Curriculum.class));
      verify(geminiService, times(1)).sendMessage(any(String.class));
      verify(chapterRepository, never()).save(any(Chapter.class));
      verify(lessonRepository, never()).save(any(Lesson.class));
      verifyNoMoreInteractions(
          userRepository, curriculumRepository, geminiService, chapterRepository, lessonRepository);
    }

    /**
     * Abnormal case: Lỗi khi gọi Gemini API.
     *
     * <p>Input:
     * <ul>
     *   <li>geminiService.sendMessage() ném RuntimeException</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>try-catch gọi Gemini -> catch branch được cover</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code LESSON_GENERATION_FAILED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_lesson_generation_failed_when_gemini_call_throws_exception() {
      // ===== ARRANGE =====
      adminId = UUID.fromString("7a000000-0000-0000-0000-000000000003");
      adminUser = buildMockUser(adminId, "Tran Minh Quan", "ADMIN");
      mockJwt(adminId);
      GenerateLessonContentRequest request = buildGenerateRequest(false);
      Curriculum curriculum =
          buildCurriculum(
              UUID.fromString("7a000000-0000-0000-0000-000000000012"),
              "Mathematics Lớp 10 - Hình học",
              10,
              CurriculumCategory.GEOMETRY);

      when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(any(), any(), any()))
          .thenReturn(Optional.of(curriculum));
      when(geminiService.sendMessage(any(String.class)))
          .thenThrow(new RuntimeException("Gemini timeout"));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> lessonContentService.generateAndSaveContent(request));
      assertEquals(ErrorCode.LESSON_GENERATION_FAILED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(adminId);
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              "Mathematics Lớp 10 - Hình học", 10, CurriculumCategory.GEOMETRY);
      verify(geminiService, times(1)).sendMessage(any(String.class));
      verifyNoMoreInteractions(
          userRepository, curriculumRepository, geminiService, chapterRepository, lessonRepository);
    }

    /**
     * Abnormal case: Parse AI response thất bại do JSON không hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>aiResponse: chuỗi không parse được thành JSON array</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>parseAiResponse(...) -> catch branch ở generateAndSaveContent</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code LESSON_GENERATION_FAILED}</li>
     * </ul>
     */
    @Test
    void it_should_throw_lesson_generation_failed_when_ai_response_is_invalid_json() {
      // ===== ARRANGE =====
      adminId = UUID.fromString("7a000000-0000-0000-0000-000000000004");
      adminUser = buildMockUser(adminId, "Le Thanh Nam", "ADMIN");
      mockJwt(adminId);
      GenerateLessonContentRequest request = buildGenerateRequest(false);
      Curriculum curriculum =
          buildCurriculum(
              UUID.fromString("7a000000-0000-0000-0000-000000000013"),
              "Mathematics Lớp 10 - Hình học",
              10,
              CurriculumCategory.GEOMETRY);

      when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
      when(curriculumRepository.findByNameAndGradeAndCategoryAndNotDeleted(any(), any(), any()))
          .thenReturn(Optional.of(curriculum));
      when(geminiService.sendMessage(any(String.class))).thenReturn("khong-phai-json");

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> lessonContentService.generateAndSaveContent(request));
      assertEquals(ErrorCode.LESSON_GENERATION_FAILED, exception.getErrorCode());

      // ===== VERIFY =====
      verify(userRepository, times(1)).findById(adminId);
      verify(curriculumRepository, times(1))
          .findByNameAndGradeAndCategoryAndNotDeleted(
              "Mathematics Lớp 10 - Hình học", 10, CurriculumCategory.GEOMETRY);
      verify(geminiService, times(1)).sendMessage(any(String.class));
      verifyNoMoreInteractions(
          userRepository, curriculumRepository, geminiService, chapterRepository, lessonRepository);
    }
  }

  @Nested
  @DisplayName("read operations")
  class ReadOperationTests {

    /**
     * Normal case: Lấy lessons theo grade và subject, bỏ qua chapter/lesson đã soft-delete.
     *
     * <p>Input:
     * <ul>
     *   <li>curriculum: gồm 1 chapter chưa xóa và 1 chapter đã xóa</li>
     *   <li>lessons: chapter hợp lệ gồm 1 lesson chưa xóa và 1 lesson đã xóa</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>chapter.getDeletedAt() == null -> TRUE/FALSE đều được cover</li>
     *   <li>mapToLessonResponseList -> filter deleted lesson TRUE/FALSE đều được cover</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về đúng 1 lesson chưa xóa</li>
     * </ul>
     */
    @Test
    void it_should_return_non_deleted_lessons_when_fetching_by_grade_and_subject() {
      // ===== ARRANGE =====
      UUID curriculumId = UUID.fromString("7a000000-0000-0000-0000-000000000040");
      UUID activeChapterId = UUID.fromString("7a000000-0000-0000-0000-000000000041");
      UUID deletedChapterId = UUID.fromString("7a000000-0000-0000-0000-000000000042");
      Lesson activeLesson =
          buildLesson(
              UUID.fromString("7a000000-0000-0000-0000-000000000043"),
              activeChapterId,
              "Phương trình đường thẳng",
              "Nội dung về phương trình tổng quát",
              LessonDifficulty.INTERMEDIATE,
              false);
      Lesson deletedLesson =
          buildLesson(
              UUID.fromString("7a000000-0000-0000-0000-000000000044"),
              activeChapterId,
              "Phương trình tham số",
              "Nội dung tham khảo",
              LessonDifficulty.INTERMEDIATE,
              true);
      Chapter activeChapter = buildChapter(activeChapterId, curriculumId, "Đường thẳng", false);
      activeChapter.setLessons(Set.of(activeLesson, deletedLesson));
      Chapter deletedChapter = buildChapter(deletedChapterId, curriculumId, "Đường tròn", true);
      deletedChapter.setLessons(Set.of(activeLesson));
      Curriculum curriculum =
          buildCurriculum(curriculumId, "Mathematics Lớp 10 - Hình học", 10, CurriculumCategory.GEOMETRY);
      curriculum.setChapters(Set.of(activeChapter, deletedChapter));

      when(curriculumRepository.findByGradeAndCategoryAndNotDeleted(10, CurriculumCategory.GEOMETRY))
          .thenReturn(List.of(curriculum));

      // ===== ACT =====
      List<LessonResponse> result =
          lessonContentService.getLessonsByGradeAndSubject("Lớp 10", "Hình học không gian");

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(activeLesson.getId(), result.get(0).getId()),
          () -> assertEquals("Phương trình đường thẳng", result.get(0).getTitle()));

      // ===== VERIFY =====
      verify(curriculumRepository, times(1))
          .findByGradeAndCategoryAndNotDeleted(10, CurriculumCategory.GEOMETRY);
      verifyNoMoreInteractions(curriculumRepository, lessonRepository, chapterRepository);
    }

    /**
     * Abnormal case: Lesson không tồn tại theo id.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonId: UUID hợp lệ nhưng repository trả Optional.empty()</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted -> empty (nhánh throw LESSON_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code LESSON_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_lesson_not_found_when_get_lesson_by_id_not_found() {
      // ===== ARRANGE =====
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000045");
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> lessonContentService.getLessonById(lessonId));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verifyNoMoreInteractions(lessonRepository, chapterRepository);
    }

    /**
     * Abnormal case: Lesson tồn tại nhưng chapter đã xóa hoặc không tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>lesson: tồn tại và hợp lệ</li>
     *   <li>chapterRepository.findById(...) -> Optional.of(chapter deletedAt != null)</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>filter(c -> c.getDeletedAt() == null) -> FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code CHAPTER_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_chapter_not_found_when_get_chapters_by_lesson_id_with_deleted_chapter() {
      // ===== ARRANGE =====
      UUID chapterId = UUID.fromString("7a000000-0000-0000-0000-000000000046");
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000047");
      Lesson lesson =
          buildLesson(
              lessonId,
              chapterId,
              "Hàm số bậc hai",
              "Nội dung hàm số bậc hai",
              LessonDifficulty.BEGINNER,
              false);
      Chapter deletedChapter = buildChapter(chapterId, UUID.randomUUID(), "Chương hàm số", true);

      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(deletedChapter));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> lessonContentService.getChaptersByLessonId(lessonId));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verify(chapterRepository, times(1)).findById(chapterId);
      verifyNoMoreInteractions(lessonRepository, chapterRepository);
    }

    /**
     * Normal case: Trả về chapter chứa lesson khi lesson và chapter đều hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonId/chapterId: dữ liệu tồn tại và chưa xóa</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted -> present (TRUE branch)</li>
     *   <li>chapter filter deletedAt == null -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về list 1 phần tử đúng chapter id/title</li>
     * </ul>
     */
    @Test
    void it_should_return_chapter_response_when_get_chapters_by_lesson_id_successfully() {
      // ===== ARRANGE =====
      UUID chapterId = UUID.fromString("7a000000-0000-0000-0000-000000000048");
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000049");
      Lesson lesson =
          buildLesson(
              lessonId,
              chapterId,
              "Tích vô hướng",
              "Nội dung tích vô hướng",
              LessonDifficulty.INTERMEDIATE,
              false);
      Chapter chapter = buildChapter(chapterId, UUID.randomUUID(), "Tích vô hướng", false);

      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));

      // ===== ACT =====
      List<ChapterResponse> result = lessonContentService.getChaptersByLessonId(lessonId);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(1, result.size()),
          () -> assertEquals(chapterId, result.get(0).getId()),
          () -> assertEquals("Tích vô hướng", result.get(0).getTitle()));

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verify(chapterRepository, times(1)).findById(chapterId);
      verifyNoMoreInteractions(lessonRepository, chapterRepository);
    }

    /**
     * Abnormal case: Không tìm thấy lesson khi lấy chapter theo lessonId.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonRepository.findByIdAndNotDeleted(...) trả Optional.empty()</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted -> empty (nhánh throw LESSON_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code LESSON_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_lesson_not_found_when_get_chapters_by_lesson_id_not_found() {
      // ===== ARRANGE =====
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000059");
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(
              AppException.class, () -> lessonContentService.getChaptersByLessonId(lessonId));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verifyNoMoreInteractions(lessonRepository, chapterRepository);
    }

    /**
     * Normal case: Trả về lesson response khi lesson id tồn tại.
     *
     * <p>Input:
     * <ul>
     *   <li>lesson có đầy đủ thông tin và chưa bị xóa</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted -> present branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Response có id/title đúng với lesson</li>
     * </ul>
     */
    @Test
    void it_should_return_lesson_response_when_get_lesson_by_id_successfully() {
      // ===== ARRANGE =====
      UUID chapterId = UUID.fromString("7a000000-0000-0000-0000-000000000060");
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000061");
      Lesson lesson =
          buildLesson(
              lessonId,
              chapterId,
              "Ứng dụng đạo hàm",
              "Nội dung ứng dụng đạo hàm trong tối ưu.",
              LessonDifficulty.ADVANCED,
              false);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));

      // ===== ACT =====
      LessonResponse result = lessonContentService.getLessonById(lessonId);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(lessonId, result.getId()), () -> assertEquals("Ứng dụng đạo hàm", result.getTitle()));

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verifyNoMoreInteractions(lessonRepository);
    }
  }

  @Nested
  @DisplayName("delete operations")
  class DeleteOperationTests {

    /**
     * Normal case: Soft-delete lesson thành công bởi admin.
     *
     * <p>Input:
     * <ul>
     *   <li>auth: Jwt của user có role ADMIN</li>
     *   <li>lesson: tồn tại và chưa xóa</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateAdminRole -> isAdmin TRUE branch</li>
     *   <li>findByIdAndNotDeleted -> present branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>lesson.deletedAt được gán và lessonRepository.save được gọi 1 lần</li>
     * </ul>
     */
    @Test
    void it_should_soft_delete_lesson_when_admin_deletes_existing_lesson() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("7a000000-0000-0000-0000-000000000050");
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000051");
      mockJwt(userId);

      User admin = buildMockUser(userId, "Nguyen Thi Mai", "ADMIN");
      Lesson lesson =
          buildLesson(
              lessonId,
              UUID.randomUUID(),
              "Giới hạn hàm số",
              "Nội dung giới hạn",
              LessonDifficulty.BEGINNER,
              false);

      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

      // ===== ACT =====
      lessonContentService.deleteLesson(lessonId);

      // ===== ASSERT =====
      assertNotNull(lesson.getDeletedAt());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verify(userRepository, times(1)).findById(userId);
      verify(lessonRepository, times(1)).save(lesson);
      verifyNoMoreInteractions(lessonRepository, userRepository);
    }

    /**
     * Abnormal case: Authentication không phải JwtAuthenticationToken.
     *
     * <p>Input:
     * <ul>
     *   <li>authentication: TestingAuthenticationToken</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>getCurrentUserId() -> nhánh throw IllegalStateException</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link IllegalStateException}</li>
     * </ul>
     */
    @Test
    void it_should_throw_illegal_state_when_delete_lesson_with_non_jwt_authentication() {
      // ===== ARRANGE =====
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000052");
      SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u", "p"));

      // ===== ACT & ASSERT =====
      assertThrows(IllegalStateException.class, () -> lessonContentService.deleteLesson(lessonId));

      // ===== VERIFY =====
      verifyNoMoreInteractions(lessonRepository, chapterRepository, userRepository);
    }

    /**
     * Abnormal case: User không có role ADMIN.
     *
     * <p>Input:
     * <ul>
     *   <li>user.roles: chỉ có TEACHER</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>validateAdminRole -> isAdmin FALSE branch, throw NOT_A_TEACHER</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code NOT_A_TEACHER}</li>
     * </ul>
     */
    @Test
    void it_should_throw_not_a_teacher_when_delete_lesson_by_non_admin_user() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("7a000000-0000-0000-0000-000000000053");
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000054");
      mockJwt(userId);

      User teacherOnlyUser = buildMockUser(userId, "Pham Trung Kien", "TEACHER");
      Lesson lesson =
          buildLesson(
              lessonId,
              UUID.randomUUID(),
              "Hệ phương trình tuyến tính",
              "Nội dung hệ phương trình",
              LessonDifficulty.INTERMEDIATE,
              false);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.of(lesson));
      when(userRepository.findById(userId)).thenReturn(Optional.of(teacherOnlyUser));

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> lessonContentService.deleteLesson(lessonId));
      assertEquals(ErrorCode.NOT_A_TEACHER, exception.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verify(userRepository, times(1)).findById(userId);
      verify(lessonRepository, never()).save(any(Lesson.class));
      verifyNoMoreInteractions(lessonRepository, userRepository);
    }

    /**
     * Normal case: Soft-delete chapter và soft-delete các lesson chưa xóa.
     *
     * <p>Input:
     * <ul>
     *   <li>chapter gồm 1 lesson chưa xóa và 1 lesson đã xóa</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>chapter.getLessons() != null -> TRUE branch</li>
     *   <li>filter lesson deletedAt == null -> TRUE/FALSE đều được cover</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>chapterRepository.save và lessonRepository.saveAll được gọi</li>
     * </ul>
     */
    @Test
    void it_should_soft_delete_chapter_and_non_deleted_lessons_when_admin_deletes_chapter() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("7a000000-0000-0000-0000-000000000055");
      UUID chapterId = UUID.fromString("7a000000-0000-0000-0000-000000000056");
      mockJwt(userId);
      User admin = buildMockUser(userId, "Do Quynh Nhu", "ADMIN");

      Lesson activeLesson =
          buildLesson(
              UUID.fromString("7a000000-0000-0000-0000-000000000057"),
              chapterId,
              "Đạo hàm cơ bản",
              "Nội dung đạo hàm",
              LessonDifficulty.BEGINNER,
              false);
      Lesson deletedLesson =
          buildLesson(
              UUID.fromString("7a000000-0000-0000-0000-000000000058"),
              chapterId,
              "Quy tắc đạo hàm",
              "Nội dung nâng cao",
              LessonDifficulty.ADVANCED,
              true);
      Chapter chapter = buildChapter(chapterId, UUID.randomUUID(), "Đạo hàm", false);
      chapter.setLessons(Set.of(activeLesson, deletedLesson));

      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(userRepository.findById(userId)).thenReturn(Optional.of(admin));
      when(chapterRepository.save(chapter)).thenReturn(chapter);
      when(lessonRepository.saveAll(any())).thenReturn(new ArrayList<>());

      // ===== ACT =====
      lessonContentService.deleteChapter(chapterId);

      // ===== ASSERT =====
      assertAll(() -> assertNotNull(chapter.getDeletedAt()), () -> assertNotNull(activeLesson.getDeletedAt()));

      // ===== VERIFY =====
      verify(chapterRepository, times(1)).findById(chapterId);
      verify(userRepository, times(1)).findById(userId);
      verify(chapterRepository, times(1)).save(chapter);
      verify(lessonRepository, times(1)).saveAll(any());
      verifyNoMoreInteractions(chapterRepository, lessonRepository, userRepository);
    }

    /**
     * Abnormal case: Không tìm thấy lesson khi xóa lesson.
     *
     * <p>Input:
     * <ul>
     *   <li>lessonRepository.findByIdAndNotDeleted(...) trả Optional.empty()</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findByIdAndNotDeleted -> empty (throw LESSON_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code LESSON_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_lesson_not_found_when_delete_lesson_with_missing_id() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("7a000000-0000-0000-0000-000000000062");
      UUID lessonId = UUID.fromString("7a000000-0000-0000-0000-000000000063");
      mockJwt(userId);
      when(lessonRepository.findByIdAndNotDeleted(lessonId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> lessonContentService.deleteLesson(lessonId));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(lessonRepository, times(1)).findByIdAndNotDeleted(lessonId);
      verifyNoMoreInteractions(lessonRepository, userRepository);
    }

    /**
     * Abnormal case: Không tìm thấy chapter hoặc chapter đã xóa khi xóa chapter.
     *
     * <p>Input:
     * <ul>
     *   <li>chapterRepository.findById(...) trả Optional.empty()</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findById(...).filter(...) -> empty (throw CHAPTER_NOT_FOUND)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Throw {@link AppException} với error code {@code CHAPTER_NOT_FOUND}</li>
     * </ul>
     */
    @Test
    void it_should_throw_chapter_not_found_when_delete_chapter_with_missing_id() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("7a000000-0000-0000-0000-000000000064");
      UUID chapterId = UUID.fromString("7a000000-0000-0000-0000-000000000065");
      mockJwt(userId);
      when(chapterRepository.findById(chapterId)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException exception =
          assertThrows(AppException.class, () -> lessonContentService.deleteChapter(chapterId));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, exception.getErrorCode());

      // ===== VERIFY =====
      verify(chapterRepository, times(1)).findById(chapterId);
      verifyNoMoreInteractions(chapterRepository, lessonRepository, userRepository);
    }

    /**
     * Normal case: Xóa chapter khi chapter không có lessons (lessons = null).
     *
     * <p>Input:
     * <ul>
     *   <li>chapter tồn tại, lessons = null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>if (chapter.getLessons() != null) -> FALSE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>lessonRepository.saveAll(...) không được gọi</li>
     * </ul>
     */
    @Test
    void it_should_skip_lesson_save_all_when_delete_chapter_has_null_lessons_collection() {
      // ===== ARRANGE =====
      UUID userId = UUID.fromString("7a000000-0000-0000-0000-000000000066");
      UUID chapterId = UUID.fromString("7a000000-0000-0000-0000-000000000067");
      mockJwt(userId);
      User admin = buildMockUser(userId, "Ha Bao Tran", "ADMIN");
      Chapter chapter = buildChapter(chapterId, UUID.randomUUID(), "Xác suất", false);
      chapter.setLessons(null);

      when(chapterRepository.findById(chapterId)).thenReturn(Optional.of(chapter));
      when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

      // ===== ACT =====
      lessonContentService.deleteChapter(chapterId);

      // ===== ASSERT =====
      assertNotNull(chapter.getDeletedAt());

      // ===== VERIFY =====
      verify(chapterRepository, times(1)).findById(chapterId);
      verify(userRepository, times(1)).findById(userId);
      verify(chapterRepository, times(1)).save(chapter);
      verify(lessonRepository, never()).saveAll(any());
      verifyNoMoreInteractions(chapterRepository, lessonRepository, userRepository);
    }
  }

  @Nested
  @DisplayName("private helper methods")
  class PrivateHelperMethodTests {

    /**
     * Normal case: parseGrade chuẩn hóa dữ liệu grade hợp lệ trong miền [1..12].
     *
     * <p>Input:
     * <ul>
     *   <li>"Lớp 11", "0", "Lớp 20"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>numStr non-empty -> parse integer branch</li>
     *   <li>grade &lt; 1, grade &gt; 12 và grade hợp lệ đều được cover</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả lần lượt là 11, 1, 12</li>
     * </ul>
     */
    @Test
    void it_should_parse_grade_with_clamp_when_input_contains_valid_or_out_of_range_numbers()
        throws Exception {
      // ===== ARRANGE =====
      String grade11 = "Lớp 11";
      String gradeTooLow = "0";
      String gradeTooHigh = "Lớp 20";

      // ===== ACT =====
      Integer result11 = invokePrivateMethod("parseGrade", new Class<?>[] {String.class}, grade11);
      Integer resultLow =
          invokePrivateMethod("parseGrade", new Class<?>[] {String.class}, gradeTooLow);
      Integer resultHigh =
          invokePrivateMethod("parseGrade", new Class<?>[] {String.class}, gradeTooHigh);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(11, result11), () -> assertEquals(1, resultLow), () -> assertEquals(12, resultHigh));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Abnormal case: parseGrade nhận null hoặc chuỗi không có số.
     *
     * <p>Input:
     * <ul>
     *   <li>null, "Lớp Mười"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>gradeLevelStr == null -> TRUE branch</li>
     *   <li>numStr.isEmpty() -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về grade mặc định 10</li>
     * </ul>
     */
    @Test
    void it_should_return_default_grade_when_parse_grade_receives_null_or_non_numeric_input()
        throws Exception {
      // ===== ARRANGE =====
      String nonNumeric = "Lớp Mười";

      // ===== ACT =====
      Integer resultNull = invokePrivateMethod("parseGrade", new Class<?>[] {String.class}, (Object) null);
      Integer resultNonNumeric =
          invokePrivateMethod("parseGrade", new Class<?>[] {String.class}, nonNumeric);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(10, resultNull), () -> assertEquals(10, resultNonNumeric));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: parseCategory nhận diện môn hình học và môn mặc định.
     *
     * <p>Input:
     * <ul>
     *   <li>"Hình học tọa độ", "Đại số"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>contains("hình") -> TRUE branch</li>
     *   <li>else -> NUMERICAL branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả lần lượt GEOMETRY và NUMERICAL</li>
     * </ul>
     */
    @Test
    void it_should_parse_geometry_or_default_category_when_subject_varies() throws Exception {
      // ===== ARRANGE =====
      String geometrySubject = "Hình học tọa độ";
      String numericalSubject = "Đại số";

      // ===== ACT =====
      CurriculumCategory geometry =
          invokePrivateMethod("parseCategory", new Class<?>[] {String.class}, geometrySubject);
      CurriculumCategory numerical =
          invokePrivateMethod("parseCategory", new Class<?>[] {String.class}, numericalSubject);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(CurriculumCategory.GEOMETRY, geometry),
          () -> assertEquals(CurriculumCategory.NUMERICAL, numerical));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: parseDifficulty map được giá trị hợp lệ và fallback khi invalid/null.
     *
     * <p>Input:
     * <ul>
     *   <li>"advanced", "invalid-level", null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>raw == null branch</li>
     *   <li>valueOf success branch</li>
     *   <li>catch IllegalArgumentException branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả ADVANCED, INTERMEDIATE, INTERMEDIATE</li>
     * </ul>
     */
    @Test
    void it_should_parse_or_fallback_difficulty_when_raw_value_is_valid_invalid_or_null()
        throws Exception {
      // ===== ARRANGE =====
      String valid = "advanced";
      String invalid = "invalid-level";

      // ===== ACT =====
      LessonDifficulty validResult =
          invokePrivateMethod("parseDifficulty", new Class<?>[] {String.class}, valid);
      LessonDifficulty invalidResult =
          invokePrivateMethod("parseDifficulty", new Class<?>[] {String.class}, invalid);
      LessonDifficulty nullResult =
          invokePrivateMethod("parseDifficulty", new Class<?>[] {String.class}, (Object) null);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(LessonDifficulty.ADVANCED, validResult),
          () -> assertEquals(LessonDifficulty.INTERMEDIATE, invalidResult),
          () -> assertEquals(LessonDifficulty.INTERMEDIATE, nullResult));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: parseCategory trả về NUMERICAL khi subject null.
     *
     * <p>Input:
     * <ul>
     *   <li>subject: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>subject == null -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả là {@code CurriculumCategory.NUMERICAL}</li>
     * </ul>
     */
    @Test
    void it_should_return_numerical_when_parse_category_receives_null_subject() throws Exception {
      // ===== ARRANGE =====

      // ===== ACT =====
      CurriculumCategory category =
          invokePrivateMethod("parseCategory", new Class<?>[] {String.class}, (Object) null);

      // ===== ASSERT =====
      assertEquals(CurriculumCategory.NUMERICAL, category);

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: buildSummary trả nguyên văn hoặc cắt ngắn theo độ dài description.
     *
     * <p>Input:
     * <ul>
     *   <li>descriptionShort: độ dài &lt;= 100</li>
     *   <li>descriptionLong: độ dài &gt; 100</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>description.length() &lt;= 100 -> TRUE branch</li>
     *   <li>description.length() &gt; 100 -> FALSE branch (cắt + "...")</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Chuỗi ngắn giữ nguyên, chuỗi dài được cắt còn 103 ký tự</li>
     * </ul>
     */
    @Test
    void it_should_keep_or_truncate_summary_when_build_summary_receives_short_or_long_text()
        throws Exception {
      // ===== ARRANGE =====
      String shortDescription = "Giới thiệu khái niệm vector và các phép toán cơ bản.";
      String longDescription =
          "Đây là bài học mở rộng về đạo hàm, bao gồm định nghĩa giới hạn, quy tắc đạo hàm, "
              + "các bài toán ứng dụng thực tế trong tối ưu và chuyển động.";

      // ===== ACT =====
      String shortSummary =
          invokePrivateMethod("buildSummary", new Class<?>[] {String.class}, shortDescription);
      String longSummary =
          invokePrivateMethod("buildSummary", new Class<?>[] {String.class}, longDescription);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals(shortDescription, shortSummary),
          () -> assertTrue(longSummary.endsWith("...")),
          () -> assertEquals(103, longSummary.length()));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: parseAiResponse loại bỏ markdown fence và text thừa trước/sau mảng JSON.
     *
     * <p>Input:
     * <ul>
     *   <li>raw: "```json ... ```" kèm tiền tố/hậu tố văn bản</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>startsWith("```json") -> TRUE branch</li>
     *   <li>endsWith("```") -> TRUE branch</li>
     *   <li>start/end index of [ ] hợp lệ -> substring branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Parse thành list 1 phần tử với title đúng</li>
     * </ul>
     */
    @Test
    void it_should_parse_ai_response_when_raw_contains_markdown_fence_and_extra_text()
        throws Exception {
      // ===== ARRANGE =====
      String raw =
          """
          Dưới đây là kết quả bạn cần:
          ```json
          [{"title":"Giới hạn dãy số","description":"Mô tả","durationMinutes":45,"difficulty":"BEGINNER","chapters":[]}]
          ```
          Kết thúc phản hồi.
          """;

      // ===== ACT =====
      List<?> parsed = invokePrivateMethod("parseAiResponse", new Class<?>[] {String.class}, raw);

      // ===== ASSERT =====
      assertEquals(1, parsed.size());

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Abnormal case: parseAiResponse nhận JSON không hợp lệ.
     *
     * <p>Input:
     * <ul>
     *   <li>raw: "[{invalid-json}]"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>objectMapper.readValue(...) ném exception (nhánh throw)</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Gọi private method ném exception</li>
     * </ul>
     */
    @Test
    void it_should_throw_exception_when_parse_ai_response_receives_invalid_json() {
      // ===== ARRANGE =====
      String invalidRaw = "[{invalid-json}]";

      // ===== ACT & ASSERT =====
      assertThrows(
          Exception.class,
          () -> invokePrivateMethod("parseAiResponse", new Class<?>[] {String.class}, invalidRaw));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: parseGrade đi vào catch branch khi số quá lớn gây NumberFormatException.
     *
     * <p>Input:
     * <ul>
     *   <li>gradeLevel: chuỗi số rất lớn vượt phạm vi Integer</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>catch (Exception e) -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Fallback về grade mặc định 10</li>
     * </ul>
     */
    @Test
    void it_should_return_default_grade_when_parse_grade_hits_number_format_exception()
        throws Exception {
      // ===== ARRANGE =====
      String hugeNumberInput = "Grade 999999999999999999999999";

      // ===== ACT =====
      Integer result =
          invokePrivateMethod("parseGrade", new Class<?>[] {String.class}, hugeNumberInput);

      // ===== ASSERT =====
      assertEquals(10, result);

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: mapToLessonResponseList trả list rỗng khi input null.
     *
     * <p>Input:
     * <ul>
     *   <li>lessons: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>if (lessons == null) -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả là list rỗng</li>
     * </ul>
     */
    @Test
    void it_should_return_empty_list_when_map_to_lesson_response_list_receives_null_collection()
        throws Exception {
      // ===== ARRANGE =====

      // ===== ACT =====
      List<?> result =
          invokePrivateMethod(
              "mapToLessonResponseList",
              new Class<?>[] {java.util.Collection.class},
              (Object) null);

      // ===== ASSERT =====
      assertTrue(result.isEmpty());

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: mapToLessonResponse(List&lt;Chapter&gt;) trả object rỗng khi list chapter rỗng.
     *
     * <p>Input:
     * <ul>
     *   <li>chapters: List.of()</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>chapters == null || chapters.isEmpty() -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>LessonResponse không có id/title</li>
     * </ul>
     */
    @Test
    void it_should_return_empty_response_when_map_to_lesson_response_receives_empty_chapter_list()
        throws Exception {
      // ===== ARRANGE =====

      // ===== ACT =====
      LessonResponse result =
          invokePrivateMethod("mapToLessonResponse", new Class<?>[] {List.class}, List.of());

      // ===== ASSERT =====
      assertEquals(null, result.getId());

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: mapToLessonResponse(List&lt;Chapter&gt;) trả object rỗng khi chapter không có lesson.
     *
     * <p>Input:
     * <ul>
     *   <li>chapters: list chứa chapter có lessons = null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>first.getLessons() == null || isEmpty() -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>LessonResponse rỗng</li>
     * </ul>
     */
    @Test
    void it_should_return_empty_response_when_map_to_lesson_response_first_chapter_has_no_lessons()
        throws Exception {
      // ===== ARRANGE =====
      Chapter chapter = buildChapter(UUID.randomUUID(), UUID.randomUUID(), "Giới hạn", false);
      chapter.setLessons(null);

      // ===== ACT =====
      LessonResponse result =
          invokePrivateMethod(
              "mapToLessonResponse", new Class<?>[] {List.class}, List.of(chapter));

      // ===== ASSERT =====
      assertEquals(null, result.getId());

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: mapToLessonResponse(List&lt;Chapter&gt;) trả object rỗng khi tất cả lesson đã xóa.
     *
     * <p>Input:
     * <ul>
     *   <li>chapter có lesson nhưng deletedAt != null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>findFirst().orElse(null) -> firstLesson == null branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>LessonResponse rỗng</li>
     * </ul>
     */
    @Test
    void it_should_return_empty_response_when_map_to_lesson_response_all_lessons_are_deleted()
        throws Exception {
      // ===== ARRANGE =====
      UUID chapterId = UUID.randomUUID();
      Lesson deletedLesson =
          buildLesson(
              UUID.randomUUID(),
              chapterId,
              "Tích phân nâng cao",
              "Nội dung",
              LessonDifficulty.ADVANCED,
              true);
      Chapter chapter = buildChapter(chapterId, UUID.randomUUID(), "Tích phân", false);
      chapter.setLessons(Set.of(deletedLesson));

      // ===== ACT =====
      LessonResponse result =
          invokePrivateMethod(
              "mapToLessonResponse", new Class<?>[] {List.class}, List.of(chapter));

      // ===== ASSERT =====
      assertEquals(null, result.getId());

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: mapToLessonResponse(List&lt;Chapter&gt;) map thành công khi có lesson chưa xóa.
     *
     * <p>Input:
     * <ul>
     *   <li>chapter có lesson deletedAt = null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>firstLesson != null branch</li>
     *   <li>lambda filter deletedAt == null -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Trả về LessonResponse có id/title đúng</li>
     * </ul>
     */
    @Test
    void it_should_return_mapped_response_when_map_to_lesson_response_has_active_lesson()
        throws Exception {
      // ===== ARRANGE =====
      UUID chapterId = UUID.randomUUID();
      UUID lessonId = UUID.randomUUID();
      Lesson lesson =
          buildLesson(
              lessonId,
              chapterId,
              "Tích phân xác định",
              "Nội dung tích phân",
              LessonDifficulty.INTERMEDIATE,
              false);
      Chapter chapter = buildChapter(chapterId, UUID.randomUUID(), "Tích phân", false);
      chapter.setLessons(Set.of(lesson));

      // ===== ACT =====
      LessonResponse result =
          invokePrivateMethod(
              "mapToLessonResponse", new Class<?>[] {List.class}, List.of(chapter));

      // ===== ASSERT =====
      assertAll(() -> assertEquals(lessonId, result.getId()), () -> assertEquals("Tích phân xác định", result.getTitle()));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: buildSummary trả null khi description là null.
     *
     * <p>Input:
     * <ul>
     *   <li>description: null</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>description == null -> TRUE branch</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả summary là null</li>
     * </ul>
     */
    @Test
    void it_should_return_null_when_build_summary_receives_null_description() throws Exception {
      // ===== ARRANGE =====

      // ===== ACT =====
      String result = invokePrivateMethod("buildSummary", new Class<?>[] {String.class}, (Object) null);

      // ===== ASSERT =====
      assertEquals(null, result);

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: buildGenerationPrompt không thêm additional context khi context rỗng.
     *
     * <p>Input:
     * <ul>
     *   <li>request.additionalContext: blank string</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>additionalContext != null && isBlank -> FALSE branch của toán tử ba ngôi</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Prompt không chứa cụm "Additional context:"</li>
     * </ul>
     */
    @Test
    void it_should_build_prompt_without_additional_context_when_context_is_blank()
        throws Exception {
      // ===== ARRANGE =====
      GenerateLessonContentRequest request =
          GenerateLessonContentRequest.builder()
              .gradeLevel("Lớp 11")
              .subject("Đại số")
              .additionalContext("   ")
              .lessonCount(2)
              .chaptersPerLesson(3)
              .skipIfExists(true)
              .build();

      // ===== ACT =====
      String prompt =
          invokePrivateMethod(
              "buildGenerationPrompt",
              new Class<?>[] {GenerateLessonContentRequest.class},
              request);

      // ===== ASSERT =====
      assertTrue(!prompt.contains("Additional context:"));

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: parseAiResponse parse được khi dùng markdown fence "```" không có chữ json.
     *
     * <p>Input:
     * <ul>
     *   <li>raw: chuỗi bắt đầu bằng "```" và kết thúc bằng "```"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>startsWith("```") -> TRUE branch của nhánh else-if</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả parse thành list có 1 phần tử</li>
     * </ul>
     */
    @Test
    void it_should_parse_ai_response_when_raw_uses_plain_markdown_fence() throws Exception {
      // ===== ARRANGE =====
      String raw =
          """
          ```
          [{"title":"Hệ bất phương trình","description":"Mô tả","durationMinutes":90,"difficulty":"INTERMEDIATE","chapters":[]}]
          ```
          """;

      // ===== ACT =====
      List<?> parsed = invokePrivateMethod("parseAiResponse", new Class<?>[] {String.class}, raw);

      // ===== ASSERT =====
      assertEquals(1, parsed.size());

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }

    /**
     * Normal case: parseCategory nhận diện GEOMETRY khi subject chứa từ khóa "geometry".
     *
     * <p>Input:
     * <ul>
     *   <li>subject: "plane geometry fundamentals"</li>
     * </ul>
     *
     * <p>Branch coverage:
     * <ul>
     *   <li>lower.contains("geometry") -> TRUE branch (không cần chứa "hình")</li>
     * </ul>
     *
     * <p>Expectation:
     * <ul>
     *   <li>Kết quả là {@code CurriculumCategory.GEOMETRY}</li>
     * </ul>
     */
    @Test
    void it_should_return_geometry_when_parse_category_receives_geometry_keyword()
        throws Exception {
      // ===== ARRANGE =====

      // ===== ACT =====
      CurriculumCategory category =
          invokePrivateMethod(
              "parseCategory", new Class<?>[] {String.class}, "plane geometry fundamentals");

      // ===== ASSERT =====
      assertEquals(CurriculumCategory.GEOMETRY, category);

      // ===== VERIFY =====
      verifyNoMoreInteractions(
          lessonRepository, chapterRepository, curriculumRepository, userRepository, geminiService);
    }
  }
}
