package com.fptu.math_master.service.impl;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.request.CreateBookRequest;
import com.fptu.math_master.dto.request.UpdateBookRequest;
import com.fptu.math_master.dto.response.BookPdfPreviewUrlResponse;
import com.fptu.math_master.dto.response.BookProgressResponse;
import com.fptu.math_master.dto.response.BookProgressResponse.LessonProgress;
import com.fptu.math_master.dto.response.BookResponse;
import com.fptu.math_master.dto.response.BookSeriesResponse;
import com.fptu.math_master.dto.response.LessonPageResponse;
import com.fptu.math_master.dto.response.OcrTriggerResponse;
import com.fptu.math_master.entity.Book;
import com.fptu.math_master.entity.BookLessonPage;
import com.fptu.math_master.entity.BookSeries;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.enums.BookStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.BookLessonPageRepository;
import com.fptu.math_master.repository.BookRepository;
import com.fptu.math_master.repository.BookSeriesRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.SchoolGradeRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.service.BookService;
import com.fptu.math_master.service.PythonCrawlerClient;
import com.fptu.math_master.service.PythonCrawlerClient.OcrTriggerRequest;
import com.fptu.math_master.service.UploadService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookServiceImpl implements BookService {

  BookRepository bookRepository;
  BookSeriesRepository bookSeriesRepository;
  BookLessonPageRepository bookLessonPageRepository;
  SchoolGradeRepository schoolGradeRepository;
  SubjectRepository subjectRepository;
  CurriculumRepository curriculumRepository;
  LessonRepository lessonRepository;
  PythonCrawlerClient crawlerClient;
  UploadService uploadService;
  MinioProperties minioProperties;

  // ---------------------------------------------------------------------------
  // CRUD
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public BookResponse create(CreateBookRequest request, UUID actorId) {
    schoolGradeRepository
        .findByIdAndNotDeleted(request.getSchoolGradeId())
        .filter(sg -> Boolean.TRUE.equals(sg.getIsActive()))
        .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND));

    Subject subject =
        subjectRepository
            .findById(request.getSubjectId())
            .filter(s -> s.getDeletedAt() == null && Boolean.TRUE.equals(s.getIsActive()))
            .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

    // Curriculum is optional — when supplied it must belong to the same subject so the lesson
    // tree stays consistent; admins can leave it blank and attach one later.
    if (request.getCurriculumId() != null) {
      Curriculum curriculum =
          curriculumRepository
              .findByIdAndNotDeleted(request.getCurriculumId())
              .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));
      if (curriculum.getSubjectId() == null
          || !curriculum.getSubjectId().equals(subject.getId())) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }
    }

    validateOcrWindow(
        request.getOcrPageFrom(), request.getOcrPageTo(), request.getTotalPages());

    Book book =
        Book.builder()
            .schoolGradeId(request.getSchoolGradeId())
            .subjectId(request.getSubjectId())
            .bookSeriesId(resolveBookSeriesId(request))
            .curriculumId(request.getCurriculumId())
            .title(request.getTitle())
            .publisher(request.getPublisher())
            .academicYear(request.getAcademicYear())
            .totalPages(request.getTotalPages())
            .ocrPageFrom(request.getOcrPageFrom())
            .ocrPageTo(request.getOcrPageTo())
            .status(BookStatus.DRAFT)
            .verified(false)
            .build();
    // BaseEntity audit fields aren't on the @Builder; set via inherited setters.
    book.setCreatedBy(actorId);
    book.setUpdatedBy(actorId);

    return toResponse(bookRepository.save(book));
  }

  @Override
  @Transactional(readOnly = true)
  public BookResponse getById(UUID id) {
    return toResponse(findActiveBook(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BookResponse> search(
      UUID schoolGradeId,
      UUID subjectId,
      UUID bookSeriesId,
      UUID curriculumId,
    UUID chapterId,
    UUID lessonId,
      BookStatus status,
      Pageable pageable) {
    return bookRepository
      .search(
          schoolGradeId,
          subjectId,
          bookSeriesId,
          curriculumId,
          chapterId,
          lessonId,
          status,
          pageable)
        .map(this::toResponse);
  }

  @Override
  @Transactional
  public BookResponse update(UUID id, UpdateBookRequest request, UUID actorId) {
    Book book = findActiveBook(id);
    if (book.getStatus() == BookStatus.OCR_RUNNING) {
      // Don't let users tweak the OCR window while a job is in flight — the running Python task
      // captured the values it needs at trigger time.
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    if (request.getTitle() != null) book.setTitle(request.getTitle());
    if (request.getPublisher() != null) book.setPublisher(request.getPublisher());
    if (request.getAcademicYear() != null) book.setAcademicYear(request.getAcademicYear());
    if (request.getTotalPages() != null) book.setTotalPages(request.getTotalPages());
    if (request.getOcrPageFrom() != null) book.setOcrPageFrom(request.getOcrPageFrom());
    if (request.getOcrPageTo() != null) book.setOcrPageTo(request.getOcrPageTo());

    validateOcrWindow(book.getOcrPageFrom(), book.getOcrPageTo(), book.getTotalPages());

    book.setUpdatedBy(actorId);
    return toResponse(bookRepository.save(book));
  }

  @Override
  @Transactional
  public BookSeriesResponse updateSeriesName(UUID seriesId, String name, UUID actorId) {
    String normalizedName = name == null ? "" : name.trim();
    if (normalizedName.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    Optional<BookSeries> maybeSeries = bookSeriesRepository.findByIdAndNotDeleted(seriesId);
    if (maybeSeries.isPresent()) {
      BookSeries series = maybeSeries.get();
      series.setName(normalizedName);
      series.setUpdatedBy(actorId);
      BookSeries saved = bookSeriesRepository.save(series);
      return BookSeriesResponse.builder().id(saved.getId()).name(saved.getName()).build();
    }

    // Legacy-safe path: FE may pass an anchor bookId when that book has no real bookSeriesId yet.
    Book anchorBook =
        bookRepository
            .findByIdAndNotDeleted(seriesId)
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
    BookSeries createdSeries =
        bookSeriesRepository.save(
            BookSeries.builder()
                .name(normalizedName)
                .schoolGradeId(anchorBook.getSchoolGradeId())
                .subjectId(anchorBook.getSubjectId())
                .academicYear(anchorBook.getAcademicYear())
                .build());
    createdSeries.setUpdatedBy(actorId);

    anchorBook.setBookSeriesId(createdSeries.getId());
    anchorBook.setUpdatedBy(actorId);
    bookRepository.save(anchorBook);

    return BookSeriesResponse.builder()
        .id(createdSeries.getId())
        .name(createdSeries.getName())
        .build();
  }

  @Override
  @Transactional
  public BookResponse setPdfPath(UUID id, String pdfPath, UUID actorId) {
    Book book = findActiveBook(id);
    book.setPdfPath(pdfPath);
    book.setUpdatedBy(actorId);
    return toResponse(bookRepository.save(book));
  }

  @Override
  @Transactional(readOnly = true)
  public BookPdfPreviewUrlResponse getPdfPreviewUrl(UUID id) {
    Book book = findActiveBook(id);
    String key = book.getPdfPath();
    if (!StringUtils.hasText(key)) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }
    String url =
        uploadService.getPresignedUrl(key.trim(), minioProperties.getTemplateBucket());
    return new BookPdfPreviewUrlResponse(url);
  }

  @Override
  @Transactional
  public void delete(UUID id, UUID actorId) {
    Book book = findActiveBook(id);
    Instant now = Instant.now();

    bookLessonPageRepository.softDeleteAllByBookId(id, actorId);
    book.setDeletedAt(now);
    book.setDeletedBy(actorId);
    bookRepository.save(book);

    // Best-effort — if the crawler is down, we still want the soft-delete to land.
    try {
      crawlerClient.deleteAllPagesForBook(id);
    } catch (AppException ex) {
      log.warn("Crawler unavailable while deleting pages for book {}; will require cleanup", id);
    }
  }

  // ---------------------------------------------------------------------------
  // OCR orchestration
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public OcrTriggerResponse triggerOcr(UUID id, UUID actorId) {
    Book book = findActiveBook(id);

    if (book.getOcrPageFrom() == null || book.getOcrPageTo() == null) {
      throw new AppException(ErrorCode.BOOK_OCR_WINDOW_REQUIRED);
    }
    if (book.getPdfPath() == null || book.getPdfPath().isBlank()) {
      // Without a PDF reference, Python has nothing to OCR.
      throw new AppException(ErrorCode.BOOK_NOT_READY_FOR_OCR);
    }

    List<BookLessonPage> mappings = bookLessonPageRepository.findByBookIdOrdered(id);
    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.BOOK_NOT_READY_FOR_OCR);
    }

    OcrTriggerRequest payload =
        new OcrTriggerRequest(
            id,
            book.getPdfPath(),
            book.getOcrPageFrom(),
            book.getOcrPageTo(),
            mappings.stream()
                .map(
                    m ->
                        new OcrTriggerRequest.MappingItem(
                            m.getLessonId(), m.getPageStart(), m.getPageEnd()))
                .toList());

    var result = crawlerClient.triggerOcrWithMapping(payload);

    clearOcrSnapshotFields(book);
    book.setStatus(BookStatus.OCR_RUNNING);
    book.setOcrError(null);
    book.setUpdatedBy(actorId);
    bookRepository.save(book);

    int totalPages =
        mappings.stream()
            .mapToInt(m -> Math.max(0, m.getPageEnd() - m.getPageStart() + 1))
            .sum();

    return OcrTriggerResponse.builder()
        .bookId(id)
        .status(BookStatus.OCR_RUNNING)
        .mappingCount(mappings.size())
        .totalPagesQueued(totalPages)
        .message(result.message())
        .build();
  }

  @Override
  @Transactional
  public BookResponse cancelOcr(UUID id, UUID actorId) {
    Book book = findActiveBook(id);
    if (book.getStatus() != BookStatus.OCR_RUNNING) {
      throw new AppException(ErrorCode.INVALID_REQUEST);
    }

    try {
      crawlerClient.cancelOcr(id);
    } catch (AppException ex) {
      log.warn("Crawler unavailable while cancelling OCR for book {}; Postgres status still reset", id);
    }

    clearOcrSnapshotFields(book);
    book.setStatus(BookStatus.READY);
    book.setOcrError(null);
    book.setUpdatedBy(actorId);
    bookRepository.save(book);

    return toResponse(book);
  }

  // ---------------------------------------------------------------------------
  // Verification rollup
  // ---------------------------------------------------------------------------

  @Override
  @Transactional
  public BookProgressResponse getProgress(UUID id) {
    Book book = findActiveBook(id);
    List<BookLessonPage> mappings = bookLessonPageRepository.findByBookIdOrdered(id);
    Map<UUID, String> lessonTitles = lessonTitlesFor(mappings);

    int totalLessons = mappings.size();
    int verifiedLessons = 0;
    int totalPages = 0;
    int verifiedPages = 0;
    var perLesson = new java.util.ArrayList<LessonProgress>(mappings.size());

    for (BookLessonPage m : mappings) {
      List<LessonPageResponse> pages =
          crawlerClient.getPagesByBookAndLesson(id, m.getLessonId());
      int pageCount = pages.size();
      int verified = (int) pages.stream().filter(LessonPageResponse::isVerified).count();
      boolean lessonVerified = pageCount > 0 && verified == pageCount;

      totalPages += pageCount;
      verifiedPages += verified;
      if (lessonVerified) verifiedLessons++;

      perLesson.add(
          LessonProgress.builder()
              .lessonId(m.getLessonId())
              .lessonTitle(lessonTitles.getOrDefault(m.getLessonId(), ""))
              .pageStart(m.getPageStart())
              .pageEnd(m.getPageEnd())
              .totalPages(pageCount)
              .verifiedPages(verified)
              .lessonVerified(lessonVerified)
              .build());
    }

    boolean bookVerified = totalLessons > 0 && verifiedLessons == totalLessons;

    var builder =
        BookProgressResponse.builder()
            .bookId(id)
            .bookVerified(bookVerified)
            .totalLessons(totalLessons)
            .verifiedLessons(verifiedLessons)
            .totalPages(totalPages)
            .verifiedPages(verifiedPages)
            .lessons(perLesson);

    Boolean ocrCrawlerReachable = null;
    if (book.getStatus() == BookStatus.OCR_RUNNING) {
      ocrCrawlerReachable = Boolean.TRUE;
      try {
        PythonCrawlerClient.OcrStatus ocr = crawlerClient.getBookOcrStatus(id);
        boolean dirty = applyMongoTerminalStatus(book, ocr);
        if (book.getStatus() == BookStatus.OCR_RUNNING) {
          applyOcrSnapshotFromLive(book, ocr);
          dirty = true;
        }
        if (dirty) {
          bookRepository.save(book);
        }
        builder
            .ocrRunnerStatus(ocr.status())
            .ocrJobProgressPercent(ocr.progressPercent())
            .ocrJobPhase(ocr.currentPhase())
            .ocrJobProcessedPages(ocr.processedPages())
            .ocrJobTotalPages(ocr.totalPages())
            .ocrJobErrorMessage(ocr.errorMessage())
            .ocrProgressFromCache(Boolean.FALSE)
            .ocrProgressCachedAt(book.getOcrCachedAt());
      } catch (AppException ex) {
        ocrCrawlerReachable = Boolean.FALSE;
        log.debug("Crawler unavailable while reading OCR progress for book {}", id);
        applyOcrProgressFallbackFromBookCache(book, builder);
      }
    }

    return builder
        .status(book.getStatus())
        .ocrCrawlerReachable(ocrCrawlerReachable)
        .build();
  }

  /**
   * Align Postgres with terminal Mongo runner states. Mutates {@code book} only — caller saves.
   *
   * @return whether {@code book} was modified and should be persisted
   */
  private boolean applyMongoTerminalStatus(Book book, PythonCrawlerClient.OcrStatus ocr) {
    String mongoStatus = ocr.status() != null ? ocr.status().toLowerCase() : "";
    if ("done".equals(mongoStatus)) {
      if (book.getStatus() != BookStatus.OCR_DONE) {
        book.setStatus(BookStatus.OCR_DONE);
        book.setOcrError(null);
        clearOcrSnapshotFields(book);
        return true;
      }
      return false;
    }
    if ("error".equals(mongoStatus)) {
      if (book.getStatus() != BookStatus.OCR_FAILED) {
        book.setStatus(BookStatus.OCR_FAILED);
        String msg =
            Optional.ofNullable(ocr.errorMessage()).filter(s -> !s.isBlank()).orElse("OCR thất bại");
        book.setOcrError(msg);
        clearOcrSnapshotFields(book);
        return true;
      }
      return false;
    }
    return false;
  }

  private void applyOcrSnapshotFromLive(Book book, PythonCrawlerClient.OcrStatus ocr) {
    book.setOcrCachedRunnerStatus(truncate(ocr.status(), 32));
    book.setOcrCachedPhase(truncate(ocr.currentPhase(), 80));
    book.setOcrCachedProgressPercent(ocr.progressPercent());
    book.setOcrCachedProcessedPages(ocr.processedPages());
    book.setOcrCachedTotalPages(ocr.totalPages());
    book.setOcrCachedAt(Instant.now());
  }

  private void applyOcrProgressFallbackFromBookCache(
      Book book, BookProgressResponse.BookProgressResponseBuilder builder) {
    if (book.getOcrCachedAt() == null) {
      return;
    }
    builder
        .ocrRunnerStatus(book.getOcrCachedRunnerStatus())
        .ocrJobPhase(book.getOcrCachedPhase())
        .ocrJobProgressPercent(book.getOcrCachedProgressPercent())
        .ocrJobProcessedPages(book.getOcrCachedProcessedPages())
        .ocrJobTotalPages(book.getOcrCachedTotalPages())
        .ocrProgressFromCache(Boolean.TRUE)
        .ocrProgressCachedAt(book.getOcrCachedAt());
  }

  private static void clearOcrSnapshotFields(Book book) {
    book.setOcrCachedRunnerStatus(null);
    book.setOcrCachedPhase(null);
    book.setOcrCachedProgressPercent(null);
    book.setOcrCachedProcessedPages(null);
    book.setOcrCachedTotalPages(null);
    book.setOcrCachedAt(null);
  }

  private static String truncate(String s, int maxLen) {
    if (s == null) {
      return null;
    }
    return s.length() <= maxLen ? s : s.substring(0, maxLen);
  }

  @Override
  @Transactional
  public void refreshVerificationStatus(UUID id, UUID actorId) {
    Book book = findActiveBook(id);
    boolean fullyVerified = crawlerClient.isBookFullyVerified(id);

    if (fullyVerified == Boolean.TRUE.equals(book.getVerified())) {
      return; // already in sync
    }

    book.setVerified(fullyVerified);
    book.setVerifiedAt(fullyVerified ? Instant.now() : null);
    book.setUpdatedBy(actorId);
    bookRepository.save(book);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private Book findActiveBook(UUID id) {
    return bookRepository
        .findByIdAndNotDeleted(id)
        .orElseThrow(() -> new AppException(ErrorCode.BOOK_NOT_FOUND));
  }

  private void validateOcrWindow(Integer from, Integer to, Integer totalPages) {
    if (from == null && to == null) return;
    if (from == null || to == null || from > to || from < 1) {
      throw new AppException(ErrorCode.BOOK_INVALID_OCR_WINDOW);
    }
    if (totalPages != null && to > totalPages) {
      throw new AppException(ErrorCode.BOOK_INVALID_OCR_WINDOW);
    }
  }

  private Map<UUID, String> lessonTitlesFor(List<BookLessonPage> mappings) {
    if (mappings.isEmpty()) return Map.of();
    Set<UUID> ids = mappings.stream().map(BookLessonPage::getLessonId).collect(Collectors.toSet());
    Map<UUID, String> result = new HashMap<>();
    for (Lesson l : lessonRepository.findByIdInAndNotDeleted(ids)) {
      result.put(l.getId(), l.getTitle());
    }
    return result;
  }

  private BookResponse toResponse(Book book) {
    long mappedCount = bookLessonPageRepository.countByBookId(book.getId());
    String schoolGradeName =
      schoolGradeRepository
        .findByIdAndNotDeleted(book.getSchoolGradeId())
        .map(sg -> sg.getName())
        .orElse(null);
    String subjectName =
      subjectRepository
        .findById(book.getSubjectId())
        .filter(s -> s.getDeletedAt() == null)
        .map(s -> s.getName())
        .orElse(null);
    String bookSeriesName =
        book.getBookSeriesId() == null
            ? null
            : bookSeriesRepository
                .findByIdAndNotDeleted(book.getBookSeriesId())
                .map(BookSeries::getName)
                .orElse(null);
    String curriculumName =
      book.getCurriculumId() == null
        ? null
        : curriculumRepository
          .findByIdAndNotDeleted(book.getCurriculumId())
          .map(c -> c.getName())
          .orElse(null);

    return BookResponse.builder()
        .id(book.getId())
        .schoolGradeId(book.getSchoolGradeId())
      .schoolGradeName(schoolGradeName)
        .subjectId(book.getSubjectId())
      .subjectName(subjectName)
        .bookSeriesId(book.getBookSeriesId())
        .bookSeriesName(bookSeriesName)
        .curriculumId(book.getCurriculumId())
      .curriculumName(curriculumName)
        .title(book.getTitle())
        .publisher(book.getPublisher())
        .academicYear(book.getAcademicYear())
        .pdfPath(book.getPdfPath())
        .thumbnailPath(book.getThumbnailPath())
        .totalPages(book.getTotalPages())
        .ocrPageFrom(book.getOcrPageFrom())
        .ocrPageTo(book.getOcrPageTo())
        .status(book.getStatus())
        .ocrError(book.getOcrError())
        .verified(Boolean.TRUE.equals(book.getVerified()))
        .verifiedAt(book.getVerifiedAt())
        .mappedLessonCount(mappedCount)
        .createdAt(book.getCreatedAt())
        .updatedAt(book.getUpdatedAt())
        .build();
  }

  private UUID resolveBookSeriesId(CreateBookRequest request) {
    if (request.getBookSeriesId() != null) {
      BookSeries existing =
          bookSeriesRepository
              .findByIdAndNotDeleted(request.getBookSeriesId())
              .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST));
      if (!existing.getSchoolGradeId().equals(request.getSchoolGradeId())
          || !existing.getSubjectId().equals(request.getSubjectId())) {
        throw new AppException(ErrorCode.INVALID_REQUEST);
      }
      return existing.getId();
    }

    BookSeries created =
        bookSeriesRepository.save(
            BookSeries.builder()
                .name(request.getTitle())
                .schoolGradeId(request.getSchoolGradeId())
                .subjectId(request.getSubjectId())
                .academicYear(request.getAcademicYear())
                .build());
    return created.getId();
  }
}
