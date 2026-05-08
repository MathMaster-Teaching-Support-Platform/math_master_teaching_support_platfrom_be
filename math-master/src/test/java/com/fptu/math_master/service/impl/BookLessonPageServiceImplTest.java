package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.BulkPageMappingRequest;
import com.fptu.math_master.dto.request.PageMappingItem;
import com.fptu.math_master.dto.response.LessonPageResponse;
import com.fptu.math_master.entity.Book;
import com.fptu.math_master.entity.BookLessonPage;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.enums.BookStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.BookLessonPageRepository;
import com.fptu.math_master.repository.BookRepository;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.service.PythonCrawlerClient;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("BookLessonPageServiceImpl - Tests")
class BookLessonPageServiceImplTest extends BaseUnitTest {

  @InjectMocks private BookLessonPageServiceImpl service;

  @Mock private BookRepository bookRepository;
  @Mock private BookLessonPageRepository bookLessonPageRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private ChapterRepository chapterRepository;
  @Mock private PythonCrawlerClient crawlerClient;

  private static final UUID BOOK_ID = UUID.fromString("11111111-0000-0000-0000-000000000001");
  private static final UUID CURRICULUM_ID = UUID.fromString("11111111-0000-0000-0000-000000000002");
  private static final UUID OTHER_CURRICULUM_ID =
      UUID.fromString("11111111-0000-0000-0000-000000000099");
  private static final UUID ACTOR = UUID.fromString("11111111-0000-0000-0000-0000000000aa");

  private static final UUID CHAPTER_1 = UUID.fromString("22222222-0000-0000-0000-000000000001");
  private static final UUID CHAPTER_2 = UUID.fromString("22222222-0000-0000-0000-000000000002");

  private static final UUID LESSON_1A = UUID.fromString("33333333-0000-0000-0000-000000000001");
  private static final UUID LESSON_1B = UUID.fromString("33333333-0000-0000-0000-000000000002");
  private static final UUID LESSON_2A = UUID.fromString("33333333-0000-0000-0000-000000000003");

  private Book book;
  private Chapter chapter1;
  private Chapter chapter2;
  private Lesson lesson1A;
  private Lesson lesson1B;
  private Lesson lesson2A;

  @BeforeEach
  void setUp() {
    book = new Book();
    book.setId(BOOK_ID);
    book.setCurriculumId(CURRICULUM_ID);
    book.setStatus(BookStatus.DRAFT);
    book.setOcrPageFrom(1);
    book.setOcrPageTo(100);
    book.setTotalPages(120);

    chapter1 = chapter(CHAPTER_1, CURRICULUM_ID, 1);
    chapter2 = chapter(CHAPTER_2, CURRICULUM_ID, 2);

    lesson1A = lesson(LESSON_1A, CHAPTER_1, 1);
    lesson1B = lesson(LESSON_1B, CHAPTER_1, 2);
    lesson2A = lesson(LESSON_2A, CHAPTER_2, 1);
  }

  // ---------------------------------------------------------------------------
  // bulkUpsert()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("bulkUpsert()")
  class BulkUpsertTests {

    @Test
    void it_should_persist_mapping_and_advance_status_to_ready_when_input_is_valid() {
      BulkPageMappingRequest request = req(
          item(LESSON_1A, 1, 5),
          item(LESSON_1B, 5, 10),
          item(LESSON_2A, 11, 20));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A, lesson1B, lesson2A));
      when(chapterRepository.findAllById(anyCollection()))
          .thenReturn(List.of(chapter1, chapter2));
      when(bookLessonPageRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
      when(crawlerClient.getPagesByBookAndLesson(any(), any())).thenReturn(List.of());

      service.bulkUpsert(BOOK_ID, request, ACTOR);

      verify(bookLessonPageRepository).hardDeleteAllByBookId(BOOK_ID);
      verify(bookLessonPageRepository).saveAll(any());
      assertEquals(BookStatus.READY, book.getStatus());
    }

    @Test
    void it_should_keep_status_when_book_already_past_mapping() {
      book.setStatus(BookStatus.OCR_DONE);
      BulkPageMappingRequest request = req(item(LESSON_1A, 1, 5));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A));
      when(chapterRepository.findAllById(anyCollection())).thenReturn(List.of(chapter1));
      when(bookLessonPageRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
      when(crawlerClient.getPagesByBookAndLesson(any(), any())).thenReturn(List.of());

      service.bulkUpsert(BOOK_ID, request, ACTOR);

      // OCR_DONE → re-mapping is allowed, but we don't roll back to MAPPING/READY.
      assertEquals(BookStatus.OCR_DONE, book.getStatus());
      verify(bookRepository, never()).save(any());
    }

    @Test
    void it_should_reject_when_book_is_currently_ocr_running() {
      book.setStatus(BookStatus.OCR_RUNNING);
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));

      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  service.bulkUpsert(
                      BOOK_ID, req(item(LESSON_1A, 1, 5)), ACTOR));
      assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
      verify(bookLessonPageRepository, never()).hardDeleteAllByBookId(any());
    }

    @Test
    void it_should_reject_when_duplicate_lessons_in_request() {
      BulkPageMappingRequest request =
          req(item(LESSON_1A, 1, 5), item(LESSON_1A, 6, 10));
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));

      AppException ex = assertThrows(
          AppException.class, () -> service.bulkUpsert(BOOK_ID, request, ACTOR));
      assertEquals(ErrorCode.PAGE_MAPPING_DUPLICATE_LESSON, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_page_start_greater_than_page_end() {
      BulkPageMappingRequest request = req(item(LESSON_1A, 10, 5));
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));

      AppException ex = assertThrows(
          AppException.class, () -> service.bulkUpsert(BOOK_ID, request, ACTOR));
      assertEquals(ErrorCode.PAGE_MAPPING_OUT_OF_RANGE, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_range_falls_outside_ocr_window() {
      // Window is [1, 100] — page 150 is out of bounds.
      BulkPageMappingRequest request = req(item(LESSON_1A, 95, 150));
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));

      AppException ex = assertThrows(
          AppException.class, () -> service.bulkUpsert(BOOK_ID, request, ACTOR));
      assertEquals(ErrorCode.PAGE_MAPPING_OUT_OF_RANGE, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_lesson_belongs_to_other_curriculum() {
      chapter1.setCurriculumId(OTHER_CURRICULUM_ID); // poison
      BulkPageMappingRequest request = req(item(LESSON_1A, 1, 5));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A));
      when(chapterRepository.findAllById(anyCollection())).thenReturn(List.of(chapter1));

      AppException ex = assertThrows(
          AppException.class, () -> service.bulkUpsert(BOOK_ID, request, ACTOR));
      assertEquals(ErrorCode.PAGE_MAPPING_LESSON_FOREIGN, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_lesson_not_found() {
      // findByIdIn returns 0 lessons but request carries 1.
      BulkPageMappingRequest request = req(item(LESSON_1A, 1, 5));
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection())).thenReturn(List.of());

      AppException ex = assertThrows(
          AppException.class, () -> service.bulkUpsert(BOOK_ID, request, ACTOR));
      assertEquals(ErrorCode.LESSON_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_consecutive_lessons_overlap_by_more_than_one_page() {
      // Lesson 1A: pages 1–10. Lesson 1B starts at page 5 → overlap of 6 pages.
      BulkPageMappingRequest request = req(
          item(LESSON_1A, 1, 10),
          item(LESSON_1B, 5, 15));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A, lesson1B));
      when(chapterRepository.findAllById(anyCollection())).thenReturn(List.of(chapter1));

      AppException ex = assertThrows(
          AppException.class, () -> service.bulkUpsert(BOOK_ID, request, ACTOR));
      assertEquals(ErrorCode.PAGE_MAPPING_OVERLAP_INVALID, ex.getErrorCode());
    }

    @Test
    void it_should_allow_single_shared_page_between_consecutive_lessons() {
      // pageEnd of L1A == pageStart of L1B → exactly one shared page is OK.
      BulkPageMappingRequest request = req(
          item(LESSON_1A, 1, 10),
          item(LESSON_1B, 10, 15));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A, lesson1B));
      when(chapterRepository.findAllById(anyCollection())).thenReturn(List.of(chapter1));
      when(bookLessonPageRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
      when(crawlerClient.getPagesByBookAndLesson(any(), any())).thenReturn(List.of());

      service.bulkUpsert(BOOK_ID, request, ACTOR);

      verify(bookLessonPageRepository).saveAll(any());
    }

    @Test
    void it_should_sort_input_by_chapter_and_lesson_order_index_before_validation() {
      // User input is in random order: lesson 2A first, then 1B, then 1A. After sorting, the
      // pages must be 1A(1-5) → 1B(5-10) → 2A(11-20), which is valid.
      BulkPageMappingRequest request = req(
          item(LESSON_2A, 11, 20),
          item(LESSON_1B, 5, 10),
          item(LESSON_1A, 1, 5));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A, lesson1B, lesson2A));
      when(chapterRepository.findAllById(anyCollection()))
          .thenReturn(List.of(chapter1, chapter2));
      when(bookLessonPageRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
      when(crawlerClient.getPagesByBookAndLesson(any(), any())).thenReturn(List.of());

      service.bulkUpsert(BOOK_ID, request, ACTOR);

      // No overlap exception → sorting worked.
      verify(bookLessonPageRepository).saveAll(any());
    }

    @Test
    void it_should_throw_when_book_not_found() {
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.empty());
      AppException ex =
          assertThrows(
              AppException.class,
              () ->
                  service.bulkUpsert(
                      BOOK_ID, req(item(LESSON_1A, 1, 5)), ACTOR));
      assertEquals(ErrorCode.BOOK_NOT_FOUND, ex.getErrorCode());
    }
  }

  // ---------------------------------------------------------------------------
  // listForBook()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("listForBook()")
  class ListForBookTests {

    @Test
    void it_should_return_empty_list_when_book_has_no_mapping_yet() {
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(bookLessonPageRepository.findByBookIdOrdered(BOOK_ID)).thenReturn(List.of());

      assertEquals(0, service.listForBook(BOOK_ID).size());
    }

    @Test
    void it_should_include_per_lesson_ocr_and_verified_counts_when_mapping_exists() {
      BookLessonPage row =
          BookLessonPage.builder()
              .bookId(BOOK_ID)
              .lessonId(LESSON_1A)
              .pageStart(1)
              .pageEnd(5)
              .orderIndex(0)
              .build();
      row.setId(UUID.randomUUID());

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(bookLessonPageRepository.findByBookIdOrdered(BOOK_ID)).thenReturn(List.of(row));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A));
      when(chapterRepository.findAllById(anyCollection())).thenReturn(List.of(chapter1));
      when(crawlerClient.getPagesByBookAndLesson(BOOK_ID, LESSON_1A))
          .thenReturn(
              List.of(
                  page(LESSON_1A, 1, true),
                  page(LESSON_1A, 2, false),
                  page(LESSON_1A, 3, true)));

      var responses = service.listForBook(BOOK_ID);

      assertEquals(1, responses.size());
      assertEquals(3, responses.get(0).getOcrPageCount());
      assertEquals(2, responses.get(0).getVerifiedPageCount());
    }

    @Test
    void it_should_swallow_crawler_outage_and_return_zero_counts() {
      BookLessonPage row =
          BookLessonPage.builder()
              .bookId(BOOK_ID)
              .lessonId(LESSON_1A)
              .pageStart(1)
              .pageEnd(5)
              .orderIndex(0)
              .build();
      row.setId(UUID.randomUUID());

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(bookLessonPageRepository.findByBookIdOrdered(BOOK_ID)).thenReturn(List.of(row));
      when(lessonRepository.findByIdInAndNotDeleted(anyCollection()))
          .thenReturn(List.of(lesson1A));
      when(chapterRepository.findAllById(anyCollection())).thenReturn(List.of(chapter1));
      when(crawlerClient.getPagesByBookAndLesson(BOOK_ID, LESSON_1A))
          .thenThrow(new AppException(ErrorCode.CRAWLER_UNAVAILABLE));

      var responses = service.listForBook(BOOK_ID);

      // We still return the row — listing the mapping shouldn't fail just because Python is down.
      assertEquals(1, responses.size());
      assertEquals(0, responses.get(0).getOcrPageCount());
      assertEquals(0, responses.get(0).getVerifiedPageCount());
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static PageMappingItem item(UUID lessonId, int from, int to) {
    return PageMappingItem.builder().lessonId(lessonId).pageStart(from).pageEnd(to).build();
  }

  private static BulkPageMappingRequest req(PageMappingItem... items) {
    return BulkPageMappingRequest.builder().mappings(List.of(items)).build();
  }

  private static Lesson lesson(UUID id, UUID chapterId, int orderIndex) {
    Lesson l = new Lesson();
    l.setId(id);
    l.setChapterId(chapterId);
    l.setOrderIndex(orderIndex);
    l.setTitle("L-" + orderIndex);
    return l;
  }

  private static Chapter chapter(UUID id, UUID curriculumId, int orderIndex) {
    Chapter c = new Chapter();
    c.setId(id);
    c.setCurriculumId(curriculumId);
    c.setOrderIndex(orderIndex);
    c.setTitle("C-" + orderIndex);
    return c;
  }

  private static LessonPageResponse page(UUID lessonId, int pageNumber, boolean verified) {
    return LessonPageResponse.builder()
        .id(UUID.randomUUID().toString())
        .bookId(BOOK_ID)
        .lessonId(lessonId)
        .pageNumber(pageNumber)
        .verified(verified)
        .build();
  }

  // Suppress unused-import warning for Collection.
  private static Collection<UUID> ignored;
}
