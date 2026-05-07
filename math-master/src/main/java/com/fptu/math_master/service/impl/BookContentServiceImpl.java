package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.UpdateLessonPageRequest;
import com.fptu.math_master.dto.response.LessonContentResponse;
import com.fptu.math_master.dto.response.LessonPageResponse;
import com.fptu.math_master.entity.Book;
import com.fptu.math_master.entity.BookLessonPage;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.BookLessonPageRepository;
import com.fptu.math_master.repository.BookRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.service.BookContentService;
import com.fptu.math_master.service.BookService;
import com.fptu.math_master.service.PythonCrawlerClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookContentServiceImpl implements BookContentService {

  BookRepository bookRepository;
  BookLessonPageRepository bookLessonPageRepository;
  LessonRepository lessonRepository;
  PythonCrawlerClient crawlerClient;
  BookService bookService;

  @Override
  public LessonContentResponse getLessonContentForBook(UUID bookId, UUID lessonId) {
    Book book = findActiveBook(bookId);
    Lesson lesson = findActiveLesson(lessonId);

    List<LessonPageResponse> pages = crawlerClient.getPagesByBookAndLesson(bookId, lessonId);
    pages = sortByPageNumber(pages);

    return LessonContentResponse.builder()
        .lessonId(lessonId)
        .lessonTitle(lesson.getTitle())
        .bookId(bookId)
        .bookTitle(book.getTitle())
        .pages(pages)
        .lessonVerified(allVerified(pages))
        .build();
  }

  @Override
  public LessonContentResponse getLessonContent(UUID lessonId) {
    Lesson lesson = findActiveLesson(lessonId);

    List<LessonPageResponse> pages = crawlerClient.getPagesByLesson(lessonId, null);
    pages = sortByPageNumber(pages);

    return LessonContentResponse.builder()
        .lessonId(lessonId)
        .lessonTitle(lesson.getTitle())
        .bookId(null)
        .bookTitle(null)
        .pages(pages)
        .lessonVerified(allVerified(pages))
        .build();
  }

  @Override
  public LessonPageResponse getPage(UUID bookId, UUID lessonId, int pageNumber) {
    findActiveBook(bookId);
    findActiveLesson(lessonId);
    return crawlerClient
        .getPage(bookId, lessonId, pageNumber)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_PAGE_NOT_FOUND));
  }

  @Override
  public List<LessonContentResponse> getAllLessonsForBook(UUID bookId) {
    Book book = findActiveBook(bookId);
    List<BookLessonPage> mappings = bookLessonPageRepository.findByBookIdOrdered(bookId);
    if (mappings.isEmpty()) return List.of();

    List<LessonContentResponse> out = new ArrayList<>(mappings.size());
    for (BookLessonPage m : mappings) {
      Lesson lesson =
          lessonRepository
              .findByIdAndNotDeleted(m.getLessonId())
              .orElse(null); // soft-deleted lesson — keep the row but title null

      List<LessonPageResponse> pages =
          sortByPageNumber(crawlerClient.getPagesByBookAndLesson(bookId, m.getLessonId()));

      out.add(
          LessonContentResponse.builder()
              .lessonId(m.getLessonId())
              .lessonTitle(lesson == null ? null : lesson.getTitle())
              .bookId(bookId)
              .bookTitle(book.getTitle())
              .pages(pages)
              .lessonVerified(allVerified(pages))
              .build());
    }
    return out;
  }

  @Override
  @Transactional
  public LessonPageResponse updatePage(
      UUID bookId,
      UUID lessonId,
      int pageNumber,
      UpdateLessonPageRequest request,
      UUID actorId) {
    findActiveBook(bookId);
    findActiveLesson(lessonId);

    boolean verifiedTouched = request.getVerified() != null;
    LessonPageResponse updated =
        crawlerClient.updatePage(bookId, lessonId, pageNumber, request, actorId);

    // Toggling verified at the page level may flip the book-level rollup either way; refresh the
    // cached flag in Postgres so listings stay in sync without forcing a getProgress() round-trip.
    if (verifiedTouched) {
      try {
        bookService.refreshVerificationStatus(bookId, actorId);
      } catch (AppException ex) {
        log.warn("Failed to refresh book verification status after page update: {}", ex.getMessage());
      }
    }

    return updated;
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private Book findActiveBook(UUID id) {
    return bookRepository
        .findByIdAndNotDeleted(id)
        .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
  }

  private Lesson findActiveLesson(UUID id) {
    return lessonRepository
        .findByIdAndNotDeleted(id)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
  }

  private List<LessonPageResponse> sortByPageNumber(List<LessonPageResponse> pages) {
    return pages.stream()
        .sorted(
            Comparator.comparingInt(
                p -> p.getPageNumber() == null ? Integer.MAX_VALUE : p.getPageNumber()))
        .toList();
  }

  private boolean allVerified(List<LessonPageResponse> pages) {
    return !pages.isEmpty() && pages.stream().allMatch(LessonPageResponse::isVerified);
  }
}
