package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateChapterRequest;
import com.fptu.math_master.dto.request.UpdateChapterRequest;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.SubjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("ChapterServiceImpl - Tests")
class ChapterServiceImplTest extends BaseUnitTest {

  @InjectMocks private ChapterServiceImpl chapterService;

  @Mock private ChapterRepository chapterRepository;
  @Mock private SubjectRepository subjectRepository;

  private static final UUID CHAPTER_ID = UUID.fromString("94000000-0000-0000-0000-000000000001");
  private static final UUID SUBJECT_ID = UUID.fromString("94000000-0000-0000-0000-000000000002");
  private static final UUID CURRICULUM_ID = UUID.fromString("94000000-0000-0000-0000-000000000003");

  private Chapter buildChapter(UUID id, UUID subjectId, String title, int orderIndex) {
    Chapter chapter = new Chapter();
    chapter.setId(id);
    chapter.setSubjectId(subjectId);
    chapter.setCurriculumId(CURRICULUM_ID);
    chapter.setTitle(title);
    chapter.setDescription("Mo ta");
    chapter.setOrderIndex(orderIndex);
    chapter.setCreatedAt(Instant.parse("2026-04-26T07:30:00Z"));
    chapter.setUpdatedAt(Instant.parse("2026-04-26T07:30:00Z"));
    return chapter;
  }

  private Subject buildSubject(UUID id, boolean active, boolean deleted) {
    Subject subject = new Subject();
    subject.setId(id);
    subject.setIsActive(active);
    if (deleted) {
      subject.setDeletedAt(Instant.now());
    }
    return subject;
  }

  @Nested
  @DisplayName("createChapter()")
  class CreateChapterTests {

    @Test
    void it_should_create_chapter_with_explicit_order_index_when_order_is_provided() {
      // ===== ARRANGE =====
      CreateChapterRequest request =
          CreateChapterRequest.builder()
              .subjectId(SUBJECT_ID)
              .title("Ham so")
              .description("Noi dung")
              .orderIndex(7)
              .build();
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(buildSubject(SUBJECT_ID, true, false)));
      Chapter saved = buildChapter(CHAPTER_ID, SUBJECT_ID, "Ham so", 7);
      when(chapterRepository.save(any(Chapter.class))).thenReturn(saved);

      // ===== ACT =====
      var response = chapterService.createChapter(request);

      // ===== ASSERT =====
      assertEquals(7, response.getOrderIndex());
      verify(chapterRepository, never()).countBySubjectIdAndNotDeleted(SUBJECT_ID);
    }

    @Test
    void it_should_create_chapter_with_auto_increment_order_when_order_is_null() {
      // ===== ARRANGE =====
      CreateChapterRequest request =
          CreateChapterRequest.builder()
              .subjectId(SUBJECT_ID)
              .title("Dao ham")
              .description("Noi dung")
              .orderIndex(null)
              .build();
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(buildSubject(SUBJECT_ID, true, false)));
      when(chapterRepository.countBySubjectIdAndNotDeleted(SUBJECT_ID)).thenReturn(2L);
      Chapter saved = buildChapter(CHAPTER_ID, SUBJECT_ID, "Dao ham", 3);
      when(chapterRepository.save(any(Chapter.class))).thenReturn(saved);

      // ===== ACT =====
      var response = chapterService.createChapter(request);

      // ===== ASSERT =====
      assertEquals(3, response.getOrderIndex());
    }

    @Test
    void it_should_throw_subject_not_found_when_subject_missing_or_deleted_or_inactive() {
      // ===== ARRANGE =====
      CreateChapterRequest request =
          CreateChapterRequest.builder().subjectId(SUBJECT_ID).title("x").build();
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException missing = assertThrows(AppException.class, () -> chapterService.createChapter(request));
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, missing.getErrorCode());

      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(buildSubject(SUBJECT_ID, false, false)));
      AppException inactive = assertThrows(AppException.class, () -> chapterService.createChapter(request));
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, inactive.getErrorCode());

      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(buildSubject(SUBJECT_ID, true, true)));
      AppException deleted = assertThrows(AppException.class, () -> chapterService.createChapter(request));
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, deleted.getErrorCode());
    }
  }

  @Nested
  @DisplayName("get methods")
  class GetMethodsTests {

    @Test
    void it_should_get_chapter_by_id_when_chapter_exists_and_not_deleted() {
      // ===== ARRANGE =====
      Chapter chapter = buildChapter(CHAPTER_ID, SUBJECT_ID, "Bai 1", 1);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));

      // ===== ACT =====
      var response = chapterService.getChapterById(CHAPTER_ID);

      // ===== ASSERT =====
      assertEquals("Bai 1", response.getTitle());
    }

    @Test
    void it_should_throw_chapter_not_found_when_get_by_id_missing_or_deleted() {
      // ===== ARRANGE =====
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.empty());

      // ===== ACT & ASSERT =====
      AppException missing = assertThrows(AppException.class, () -> chapterService.getChapterById(CHAPTER_ID));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, missing.getErrorCode());

      Chapter deleted = buildChapter(CHAPTER_ID, SUBJECT_ID, "x", 1);
      deleted.setDeletedAt(Instant.now());
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(deleted));
      AppException deletedEx = assertThrows(AppException.class, () -> chapterService.getChapterById(CHAPTER_ID));
      assertEquals(ErrorCode.CHAPTER_NOT_FOUND, deletedEx.getErrorCode());
    }

    @Test
    void it_should_get_chapters_by_curriculum_and_subject() {
      // ===== ARRANGE =====
      Chapter c1 = buildChapter(CHAPTER_ID, SUBJECT_ID, "Bai 1", 1);
      when(chapterRepository.findByCurriculumIdAndNotDeleted(CURRICULUM_ID)).thenReturn(List.of(c1));
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(buildSubject(SUBJECT_ID, true, false)));
      when(chapterRepository.findBySubjectIdAndNotDeleted(SUBJECT_ID)).thenReturn(List.of(c1));

      // ===== ACT =====
      var byCurriculum = chapterService.getChaptersByCurriculumId(CURRICULUM_ID);
      var bySubject = chapterService.getChaptersBySubjectId(SUBJECT_ID);

      // ===== ASSERT =====
      assertAll(() -> assertEquals(1, byCurriculum.size()), () -> assertEquals(1, bySubject.size()));
    }

    @Test
    void it_should_throw_subject_not_found_when_get_chapters_by_subject_with_inactive_or_deleted_subject() {
      // ===== ARRANGE =====
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(buildSubject(SUBJECT_ID, false, false)));

      // ===== ACT & ASSERT =====
      AppException inactive =
          assertThrows(AppException.class, () -> chapterService.getChaptersBySubjectId(SUBJECT_ID));
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, inactive.getErrorCode());

      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(buildSubject(SUBJECT_ID, true, true)));
      AppException deleted =
          assertThrows(AppException.class, () -> chapterService.getChaptersBySubjectId(SUBJECT_ID));
      assertEquals(ErrorCode.SUBJECT_NOT_FOUND, deleted.getErrorCode());
    }
  }

  @Nested
  @DisplayName("updateChapter() and deleteChapter()")
  class UpdateAndDeleteTests {

    @Test
    void it_should_update_only_non_null_fields() {
      // ===== ARRANGE =====
      Chapter chapter = buildChapter(CHAPTER_ID, SUBJECT_ID, "Ten cu", 2);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
      when(chapterRepository.save(chapter)).thenReturn(chapter);
      UpdateChapterRequest request =
          UpdateChapterRequest.builder().title("Ten moi").description(null).orderIndex(5).build();

      // ===== ACT =====
      var response = chapterService.updateChapter(CHAPTER_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Ten moi", response.getTitle()),
          () -> assertEquals("Mo ta", response.getDescription()),
          () -> assertEquals(5, response.getOrderIndex()));
    }

    @Test
    void it_should_keep_all_values_when_update_request_all_fields_null() {
      // ===== ARRANGE =====
      Chapter chapter = buildChapter(CHAPTER_ID, SUBJECT_ID, "Ten goc", 3);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
      when(chapterRepository.save(chapter)).thenReturn(chapter);
      UpdateChapterRequest request = UpdateChapterRequest.builder().build();

      // ===== ACT =====
      var response = chapterService.updateChapter(CHAPTER_ID, request);

      // ===== ASSERT =====
      assertAll(
          () -> assertEquals("Ten goc", response.getTitle()),
          () -> assertEquals("Mo ta", response.getDescription()),
          () -> assertEquals(3, response.getOrderIndex()));
    }

    @Test
    void it_should_soft_delete_chapter_when_exists() {
      // ===== ARRANGE =====
      Chapter chapter = buildChapter(CHAPTER_ID, SUBJECT_ID, "Can xoa", 1);
      when(chapterRepository.findById(CHAPTER_ID)).thenReturn(Optional.of(chapter));
      when(chapterRepository.save(chapter)).thenReturn(chapter);

      // ===== ACT =====
      chapterService.deleteChapter(CHAPTER_ID);

      // ===== ASSERT =====
      assertNotNull(chapter.getDeletedAt());
      verify(chapterRepository, times(1)).save(chapter);
    }
  }
}
