package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.dto.request.CreateBookRequest;
import com.fptu.math_master.dto.response.BookProgressResponse;
import com.fptu.math_master.dto.response.LessonPageResponse;
import com.fptu.math_master.dto.response.OcrTriggerResponse;
import com.fptu.math_master.entity.Book;
import com.fptu.math_master.entity.BookLessonPage;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.enums.BookStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.BookLessonPageRepository;
import com.fptu.math_master.repository.BookRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.PythonCrawlerClient;
import com.fptu.math_master.service.PythonCrawlerClient.OcrTriggerResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@DisplayName("BookServiceImpl - Tests")
class BookServiceImplTest extends BaseUnitTest {

  @InjectMocks private BookServiceImpl service;

  @Mock private BookRepository bookRepository;
  @Mock private BookLessonPageRepository bookLessonPageRepository;
  @Mock private SchoolGradeRepository schoolGradeRepository;
  @Mock private SubjectRepository subjectRepository;
  @Mock private CurriculumRepository curriculumRepository;
  @Mock private LessonRepository lessonRepository;
  @Mock private PythonCrawlerClient crawlerClient;

  private static final UUID BOOK_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
  private static final UUID GRADE_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
  private static final UUID SUBJECT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
  private static final UUID CURRICULUM_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004");
  private static final UUID LESSON_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000005");
  private static final UUID ACTOR = UUID.fromString("aaaaaaaa-0000-0000-0000-0000000000aa");

  private SchoolGrade schoolGrade;
  private Subject subject;
  private Curriculum curriculum;

  @BeforeEach
  void setUp() {
    schoolGrade = new SchoolGrade();
    schoolGrade.setId(GRADE_ID);
    schoolGrade.setIsActive(true);

    subject = new Subject();
    subject.setId(SUBJECT_ID);
    subject.setIsActive(true);

    curriculum = new Curriculum();
    curriculum.setId(CURRICULUM_ID);
    curriculum.setSubjectId(SUBJECT_ID);
    curriculum.setName("Cánh diều");
  }

  // ---------------------------------------------------------------------------
  // create()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("create()")
  class CreateTests {

    @Test
    void it_should_create_book_in_draft_status_when_inputs_are_valid() {
      CreateBookRequest request = baseRequest().build();

      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID))
          .thenReturn(Optional.of(schoolGrade));
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      when(curriculumRepository.findByIdAndNotDeleted(CURRICULUM_ID))
          .thenReturn(Optional.of(curriculum));
      when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(bookLessonPageRepository.countByBookId(any())).thenReturn(0L);

      var response = service.create(request, ACTOR);

      ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
      verify(bookRepository).save(captor.capture());
      Book saved = captor.getValue();
      assertEquals(BookStatus.DRAFT, saved.getStatus());
      assertEquals(false, saved.getVerified());
      assertEquals(ACTOR, saved.getCreatedBy());
      assertEquals(ACTOR, saved.getUpdatedBy());
      assertNotNull(response);
    }

    @Test
    void it_should_reject_when_curriculum_belongs_to_a_different_subject() {
      curriculum.setSubjectId(UUID.randomUUID()); // poison
      CreateBookRequest request = baseRequest().build();

      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID))
          .thenReturn(Optional.of(schoolGrade));
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      when(curriculumRepository.findByIdAndNotDeleted(CURRICULUM_ID))
          .thenReturn(Optional.of(curriculum));

      AppException ex =
          assertThrows(AppException.class, () -> service.create(request, ACTOR));
      assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
      verify(bookRepository, never()).save(any());
    }

    @Test
    void it_should_reject_when_ocr_window_is_inverted() {
      CreateBookRequest request = baseRequest().ocrPageFrom(50).ocrPageTo(10).build();

      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID))
          .thenReturn(Optional.of(schoolGrade));
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      when(curriculumRepository.findByIdAndNotDeleted(CURRICULUM_ID))
          .thenReturn(Optional.of(curriculum));

      AppException ex =
          assertThrows(AppException.class, () -> service.create(request, ACTOR));
      assertEquals(ErrorCode.BOOK_INVALID_OCR_WINDOW, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_ocr_window_exceeds_total_pages() {
      CreateBookRequest request =
          baseRequest().totalPages(80).ocrPageFrom(1).ocrPageTo(100).build();

      when(schoolGradeRepository.findByIdAndNotDeleted(GRADE_ID))
          .thenReturn(Optional.of(schoolGrade));
      when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
      when(curriculumRepository.findByIdAndNotDeleted(CURRICULUM_ID))
          .thenReturn(Optional.of(curriculum));

      AppException ex =
          assertThrows(AppException.class, () -> service.create(request, ACTOR));
      assertEquals(ErrorCode.BOOK_INVALID_OCR_WINDOW, ex.getErrorCode());
    }
  }

  // ---------------------------------------------------------------------------
  // triggerOcr()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("triggerOcr()")
  class TriggerOcrTests {

    @Test
    void it_should_dispatch_to_python_and_set_status_to_running_when_all_preconditions_met() {
      Book book = readyForOcr();
      List<BookLessonPage> mappings =
          List.of(
              mapping(LESSON_ID, 1, 5),
              mapping(UUID.randomUUID(), 5, 10));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(bookLessonPageRepository.findByBookIdOrdered(BOOK_ID)).thenReturn(mappings);
      when(crawlerClient.triggerOcrWithMapping(any()))
          .thenReturn(new OcrTriggerResult("ACCEPTED", "queued", 11));
      when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(bookLessonPageRepository.countByBookId(any())).thenReturn(2L);

      OcrTriggerResponse response = service.triggerOcr(BOOK_ID, ACTOR);

      assertEquals(BookStatus.OCR_RUNNING, book.getStatus());
      assertEquals(2, response.getMappingCount());
      // pages 1-5 (5 pages) + 5-10 (6 pages) = 11
      assertEquals(11, response.getTotalPagesQueued());
    }

    @Test
    void it_should_reject_when_pdf_not_uploaded_yet() {
      Book book = readyForOcr();
      book.setPdfPath(null);
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));

      AppException ex = assertThrows(AppException.class, () -> service.triggerOcr(BOOK_ID, ACTOR));
      assertEquals(ErrorCode.BOOK_NOT_READY_FOR_OCR, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_no_page_mapping_present() {
      Book book = readyForOcr();
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(bookLessonPageRepository.findByBookIdOrdered(BOOK_ID)).thenReturn(List.of());

      AppException ex = assertThrows(AppException.class, () -> service.triggerOcr(BOOK_ID, ACTOR));
      assertEquals(ErrorCode.BOOK_NOT_READY_FOR_OCR, ex.getErrorCode());
    }

    @Test
    void it_should_reject_when_ocr_window_not_set() {
      Book book = readyForOcr();
      book.setOcrPageFrom(null);
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));

      AppException ex = assertThrows(AppException.class, () -> service.triggerOcr(BOOK_ID, ACTOR));
      assertEquals(ErrorCode.BOOK_OCR_WINDOW_REQUIRED, ex.getErrorCode());
    }
  }

  // ---------------------------------------------------------------------------
  // getProgress()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("getProgress()")
  class GetProgressTests {

    @Test
    void it_should_roll_up_page_verifications_into_lesson_and_book_levels() {
      Book book = readyForOcr();
      UUID lesson2 = UUID.randomUUID();
      List<BookLessonPage> mappings =
          List.of(mapping(LESSON_ID, 1, 5), mapping(lesson2, 6, 10));

      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(bookLessonPageRepository.findByBookIdOrdered(BOOK_ID)).thenReturn(mappings);
      when(lessonRepository.findByIdInAndNotDeleted(any())).thenReturn(List.of());
      // Lesson 1: 2 pages, both verified.
      when(crawlerClient.getPagesByBookAndLesson(BOOK_ID, LESSON_ID))
          .thenReturn(List.of(page(true), page(true)));
      // Lesson 2: 2 pages, only one verified.
      when(crawlerClient.getPagesByBookAndLesson(BOOK_ID, lesson2))
          .thenReturn(List.of(page(true), page(false)));

      BookProgressResponse progress = service.getProgress(BOOK_ID);

      assertEquals(2, progress.getTotalLessons());
      assertEquals(1, progress.getVerifiedLessons());
      assertEquals(4, progress.getTotalPages());
      assertEquals(3, progress.getVerifiedPages());
      assertEquals(false, progress.isBookVerified());
    }
  }

  // ---------------------------------------------------------------------------
  // refreshVerificationStatus()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("refreshVerificationStatus()")
  class RefreshVerificationTests {

    @Test
    void it_should_persist_verified_true_and_stamp_verified_at_when_python_says_fully_verified() {
      Book book = readyForOcr();
      book.setVerified(false);
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(crawlerClient.isBookFullyVerified(BOOK_ID)).thenReturn(true);
      when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.refreshVerificationStatus(BOOK_ID, ACTOR);

      assertTrue(book.getVerified());
      assertNotNull(book.getVerifiedAt());
    }

    @Test
    void it_should_clear_verified_at_when_python_now_reports_unverified() {
      Book book = readyForOcr();
      book.setVerified(true);
      book.setVerifiedAt(java.time.Instant.now());
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(crawlerClient.isBookFullyVerified(BOOK_ID)).thenReturn(false);
      when(bookRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.refreshVerificationStatus(BOOK_ID, ACTOR);

      assertEquals(false, book.getVerified());
      assertNull(book.getVerifiedAt());
    }

    @Test
    void it_should_skip_save_when_status_already_in_sync() {
      Book book = readyForOcr();
      book.setVerified(true);
      when(bookRepository.findByIdAndNotDeleted(BOOK_ID)).thenReturn(Optional.of(book));
      when(crawlerClient.isBookFullyVerified(BOOK_ID)).thenReturn(true);

      service.refreshVerificationStatus(BOOK_ID, ACTOR);

      verify(bookRepository, never()).save(any());
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private CreateBookRequest.CreateBookRequestBuilder baseRequest() {
    return CreateBookRequest.builder()
        .schoolGradeId(GRADE_ID)
        .subjectId(SUBJECT_ID)
        .curriculumId(CURRICULUM_ID)
        .title("Toán 10 — Cánh Diều")
        .totalPages(120)
        .ocrPageFrom(5)
        .ocrPageTo(115);
  }

  private Book readyForOcr() {
    Book b = new Book();
    b.setId(BOOK_ID);
    b.setSchoolGradeId(GRADE_ID);
    b.setSubjectId(SUBJECT_ID);
    b.setCurriculumId(CURRICULUM_ID);
    b.setTitle("T");
    b.setStatus(BookStatus.READY);
    b.setPdfPath("books/abc.pdf");
    b.setTotalPages(120);
    b.setOcrPageFrom(1);
    b.setOcrPageTo(100);
    return b;
  }

  private BookLessonPage mapping(UUID lessonId, int from, int to) {
    return BookLessonPage.builder()
        .bookId(BOOK_ID)
        .lessonId(lessonId)
        .pageStart(from)
        .pageEnd(to)
        .orderIndex(0)
        .build();
  }

  private LessonPageResponse page(boolean verified) {
    return LessonPageResponse.builder()
        .bookId(BOOK_ID)
        .lessonId(LESSON_ID)
        .pageNumber(1)
        .verified(verified)
        .build();
  }

  // Dummy reference so the unused-import linter doesn't complain.
  @SuppressWarnings("unused")
  private static final Class<Lesson> LESSON_CLASS = Lesson.class;
}
