package com.fptu.math_master.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.request.BulkPageMappingRequest;
import com.fptu.math_master.dto.request.PageMappingItem;
import com.fptu.math_master.dto.response.BookLessonPageResponse;
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
import com.fptu.math_master.service.BookLessonPageService;
import com.fptu.math_master.service.PythonCrawlerClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookLessonPageServiceImpl implements BookLessonPageService {

  BookRepository bookRepository;
  BookLessonPageRepository bookLessonPageRepository;
  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;
  PythonCrawlerClient crawlerClient;

  // ---------------------------------------------------------------------------
  // Bulk upsert
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public List<BookLessonPageResponse> bulkUpsert(
      UUID bookId, BulkPageMappingRequest request, UUID actorId) {
    Book book = findActiveBook(bookId);
    if (book.getStatus() == BookStatus.OCR_RUNNING) {
      // Python is mid-flight on the previous mapping; refuse to clobber it.
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    List<PageMappingItem> items = request.getMappings();
    validateNoDuplicateLessons(items);
    validatePageRangesShape(items);
    validateRangesWithinOcrWindow(items, book);

    Map<UUID, Lesson> lessonsById = loadLessons(items);
    validateLessonsBelongToCurriculum(
      lessonsById.values(), book.getCurriculumId(), book.getSubjectId());

    List<PageMappingItem> sorted = sortByCurriculumOrder(items, lessonsById);
    validateNonOverlapping(sorted);

    // Hard-replace existing mapping. Soft-delete won't work because the UNIQUE(book_id, lesson_id)
    // constraint isn't partial — re-adding the same lesson would collide with stale tombstones.
    bookLessonPageRepository.hardDeleteAllByBookId(bookId);
    bookLessonPageRepository.flush();

    List<BookLessonPage> rows = new ArrayList<>(sorted.size());
    for (int i = 0; i < sorted.size(); i++) {
      PageMappingItem it = sorted.get(i);
      BookLessonPage row =
          BookLessonPage.builder()
              .bookId(bookId)
              .lessonId(it.getLessonId())
              .pageStart(it.getPageStart())
              .pageEnd(it.getPageEnd())
              .orderIndex(i)
              .build();
      row.setCreatedBy(actorId);
      row.setUpdatedBy(actorId);
      rows.add(row);
    }
    List<BookLessonPage> saved = bookLessonPageRepository.saveAll(rows);

    // Mapping is now complete; reflect that on the book so the FE can enable the "trigger OCR"
    // button. We don't touch READY-or-later statuses — re-mapping after OCR_DONE is allowed but
    // shouldn't reset the status to MAPPING.
    if (book.getStatus() == BookStatus.DRAFT || book.getStatus() == BookStatus.MAPPING) {
      book.setStatus(BookStatus.READY);
      book.setUpdatedBy(actorId);
      bookRepository.save(book);
    }

    return toResponses(saved, lessonsById);
  }

  // ---------------------------------------------------------------------------
  // Listing
  // ---------------------------------------------------------------------------

  @Override
  public List<BookLessonPageResponse> listForBook(UUID bookId) {
    findActiveBook(bookId); // existence check
    List<BookLessonPage> rows = bookLessonPageRepository.findByBookIdOrdered(bookId);
    if (rows.isEmpty()) return List.of();

    Set<UUID> lessonIds = rows.stream().map(BookLessonPage::getLessonId).collect(Collectors.toSet());
    Map<UUID, Lesson> lessonsById =
        lessonRepository.findByIdInAndNotDeleted(lessonIds).stream()
            .collect(Collectors.toMap(Lesson::getId, l -> l));
    return toResponses(rows, lessonsById);
  }

  // ---------------------------------------------------------------------------
  // Validation
  // ---------------------------------------------------------------------------

  private void validateNoDuplicateLessons(List<PageMappingItem> items) {
    Set<UUID> seen = new HashSet<>();
    for (PageMappingItem it : items) {
      if (!seen.add(it.getLessonId())) {
        throw new AppException(ErrorCode.PAGE_MAPPING_DUPLICATE_LESSON);
      }
    }
  }

  private void validatePageRangesShape(List<PageMappingItem> items) {
    for (PageMappingItem it : items) {
      if (it.getPageStart() == null
          || it.getPageEnd() == null
          || it.getPageStart() < 1
          || it.getPageEnd() < it.getPageStart()) {
        throw new AppException(ErrorCode.PAGE_MAPPING_OUT_OF_RANGE);
      }
    }
  }

  private void validateRangesWithinOcrWindow(List<PageMappingItem> items, Book book) {
    Integer lo = book.getOcrPageFrom();
    Integer hi = book.getOcrPageTo();
    Integer total = book.getTotalPages();

    int loBound = lo != null ? lo : 1;
    Integer hiBound =
        hi != null ? hi : total; // when neither is set, only the lower bound is enforced

    for (PageMappingItem it : items) {
      if (it.getPageStart() < loBound) {
        throw new AppException(ErrorCode.PAGE_MAPPING_OUT_OF_RANGE);
      }
      if (hiBound != null && it.getPageEnd() > hiBound) {
        throw new AppException(ErrorCode.PAGE_MAPPING_OUT_OF_RANGE);
      }
    }
  }

  private Map<UUID, Lesson> loadLessons(List<PageMappingItem> items) {
    Set<UUID> ids = items.stream().map(PageMappingItem::getLessonId).collect(Collectors.toSet());
    Map<UUID, Lesson> byId =
        lessonRepository.findByIdInAndNotDeleted(ids).stream()
            .collect(Collectors.toMap(Lesson::getId, l -> l));
    if (byId.size() != ids.size()) {
      throw new AppException(ErrorCode.LESSON_NOT_FOUND);
    }
    return byId;
  }

    private void validateLessonsBelongToCurriculum(
      java.util.Collection<Lesson> lessons, UUID curriculumId, UUID subjectId) {
    Set<UUID> chapterIds = lessons.stream().map(Lesson::getChapterId).collect(Collectors.toSet());
    Map<UUID, Chapter> chaptersById =
        chapterRepository.findAllById(chapterIds).stream()
            .filter(c -> c.getDeletedAt() == null)
            .collect(Collectors.toMap(Chapter::getId, c -> c));
    if (chaptersById.size() != chapterIds.size()) {
      // Some chapter is missing or soft-deleted — refuse to map a lesson against a dangling parent.
      throw new AppException(ErrorCode.PAGE_MAPPING_LESSON_FOREIGN);
    }

    if (curriculumId == null) {
      // Curriculum is optional for books; in that case constrain lessons by subject only.
      for (Chapter ch : chaptersById.values()) {
        if (ch.getSubjectId() == null || !ch.getSubjectId().equals(subjectId)) {
          throw new AppException(ErrorCode.PAGE_MAPPING_LESSON_FOREIGN);
        }
      }
      return;
    }

    for (Chapter ch : chaptersById.values()) {
      if (ch.getCurriculumId() == null || !ch.getCurriculumId().equals(curriculumId)) {
        throw new AppException(ErrorCode.PAGE_MAPPING_LESSON_FOREIGN);
      }
    }
  }

  /**
   * Sort by curriculum reading order: chapter.orderIndex first, then lesson.orderIndex. This is
   * the order users expect when paging through a textbook.
   */
  private List<PageMappingItem> sortByCurriculumOrder(
      List<PageMappingItem> items, Map<UUID, Lesson> lessonsById) {
    Set<UUID> chapterIds =
        lessonsById.values().stream().map(Lesson::getChapterId).collect(Collectors.toSet());
    Map<UUID, Integer> chapterOrder = new HashMap<>();
    for (Chapter c : chapterRepository.findAllById(chapterIds)) {
      chapterOrder.put(c.getId(), c.getOrderIndex() == null ? Integer.MAX_VALUE : c.getOrderIndex());
    }

    Comparator<PageMappingItem> byCurriculum =
        Comparator.comparingInt(
                (PageMappingItem it) -> {
                  Lesson l = lessonsById.get(it.getLessonId());
                  return chapterOrder.getOrDefault(l.getChapterId(), Integer.MAX_VALUE);
                })
            .thenComparingInt(
                it -> {
                  Lesson l = lessonsById.get(it.getLessonId());
                  return l.getOrderIndex() == null ? Integer.MAX_VALUE : l.getOrderIndex();
                });

    return items.stream().sorted(byCurriculum).toList();
  }

  /**
   * Once sorted by curriculum order, page ranges may not jump backwards. A single shared page
   * between consecutive lessons is allowed (lessons[i].pageEnd == lessons[i+1].pageStart) since
   * one PDF page can contain the tail of one lesson and the start of the next.
   */
  private void validateNonOverlapping(List<PageMappingItem> sorted) {
    for (int i = 1; i < sorted.size(); i++) {
      PageMappingItem prev = sorted.get(i - 1);
      PageMappingItem cur = sorted.get(i);
      if (cur.getPageStart() < prev.getPageEnd()) {
        throw new AppException(ErrorCode.PAGE_MAPPING_OVERLAP_INVALID);
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private Book findActiveBook(UUID id) {
    return bookRepository
        .findByIdAndNotDeleted(id)
        .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
  }

  private List<BookLessonPageResponse> toResponses(
      List<BookLessonPage> rows, Map<UUID, Lesson> lessonsById) {
    Set<UUID> chapterIds =
        rows.stream()
            .map(r -> {
              Lesson l = lessonsById.get(r.getLessonId());
              return l == null ? null : l.getChapterId();
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    Map<UUID, Chapter> chaptersById =
        chapterRepository.findAllById(chapterIds).stream()
            .collect(Collectors.toMap(Chapter::getId, c -> c));

    List<BookLessonPageResponse> out = new ArrayList<>(rows.size());
    for (BookLessonPage r : rows) {
      Lesson lesson = lessonsById.get(r.getLessonId());
      Chapter chapter = lesson == null ? null : chaptersById.get(lesson.getChapterId());

      // Crawler may be down or have nothing for this lesson yet — treat as zero, don't fail the
      // listing.
      int ocrPageCount = 0;
      int verifiedPageCount = 0;
      try {
        List<LessonPageResponse> pages =
            crawlerClient.getPagesByBookAndLesson(r.getBookId(), r.getLessonId());
        ocrPageCount = pages.size();
        verifiedPageCount = (int) pages.stream().filter(LessonPageResponse::isVerified).count();
      } catch (AppException ex) {
        log.debug("Skipping per-lesson OCR counts; crawler unavailable for lesson {}", r.getLessonId());
      }

      out.add(
          BookLessonPageResponse.builder()
              .id(r.getId())
              .bookId(r.getBookId())
              .lessonId(r.getLessonId())
              .lessonTitle(lesson == null ? null : lesson.getTitle())
              .lessonOrderIndex(lesson == null ? null : lesson.getOrderIndex())
              .chapterId(lesson == null ? null : lesson.getChapterId())
              .chapterTitle(chapter == null ? null : chapter.getTitle())
              .chapterOrderIndex(chapter == null ? null : chapter.getOrderIndex())
              .pageStart(r.getPageStart())
              .pageEnd(r.getPageEnd())
              .ocrPageCount(ocrPageCount)
              .verifiedPageCount(verifiedPageCount)
              .build());
    }
    return out;
  }
}
