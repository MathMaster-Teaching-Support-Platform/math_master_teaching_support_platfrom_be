package com.fptu.math_master.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fptu.math_master.entity.Book;
import com.fptu.math_master.entity.BookLessonPage;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.SchoolGrade;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.enums.BookStatus;
import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.enums.LessonStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Phase 1 verification — confirms Hibernate can generate schema for the new
 * {@link Book} and {@link BookLessonPage} entities and that JPA-level
 * constraints (FK, UNIQUE, CHECK) behave as designed.
 *
 * <p>Runs against H2 (test profile) using {@code ddl-auto: create-drop} so this
 * test transitively exercises the schema generation that Hibernate would
 * perform against Postgres in dev.
 */
@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class BookSchemaTest {

  @Autowired private BookRepository bookRepository;
  @Autowired private BookLessonPageRepository bookLessonPageRepository;
  @Autowired private EntityManager em;

  /**
   * Persist the parent hierarchy (SchoolGrade → Subject → Curriculum → Chapter → Lesson)
   * needed for FK references and return the final Lesson id.
   */
  private LessonContext seedHierarchy() {
    SchoolGrade grade = new SchoolGrade();
    grade.setName("Lớp 10");
    grade.setGradeLevel(10);
    grade.setIsActive(true);
    em.persist(grade);

    Subject subject = new Subject();
    subject.setCode("MATH-10-" + UUID.randomUUID().toString().substring(0, 6));
    subject.setName("Toán 10");
    subject.setGradeMin(10);
    subject.setGradeMax(10);
    subject.setSchoolGradeId(grade.getId());
    subject.setIsActive(true);
    em.persist(subject);

    Curriculum curriculum = new Curriculum();
    curriculum.setSubjectId(subject.getId());
    curriculum.setName("Kết nối tri thức");
    curriculum.setGrade(10);
    curriculum.setCategory(CurriculumCategory.NUMERICAL);
    em.persist(curriculum);

    Chapter chapter = Chapter.builder()
        .curriculumId(curriculum.getId())
        .subjectId(subject.getId())
        .title("Chương I — Mệnh đề và tập hợp")
        .orderIndex(1)
        .build();
    em.persist(chapter);

    Lesson lesson1 = Lesson.builder()
        .chapterId(chapter.getId())
        .title("Bài 1 — Mệnh đề")
        .lessonContent("placeholder")
        .orderIndex(1)
        .status(LessonStatus.DRAFT)
        .build();
    em.persist(lesson1);

    Lesson lesson2 = Lesson.builder()
        .chapterId(chapter.getId())
        .title("Bài 2 — Tập hợp")
        .lessonContent("placeholder")
        .orderIndex(2)
        .status(LessonStatus.DRAFT)
        .build();
    em.persist(lesson2);

    em.flush();
    return new LessonContext(grade.getId(), subject.getId(), curriculum.getId(),
        lesson1.getId(), lesson2.getId());
  }

  @Test
  void it_should_persist_book_and_apply_default_status_and_verified() {
    LessonContext ctx = seedHierarchy();

    Book book = Book.builder()
        .schoolGradeId(ctx.gradeId)
        .subjectId(ctx.subjectId)
        .curriculumId(ctx.curriculumId)
        .title("Sách Toán 10 — Kết nối tri thức")
        .totalPages(120)
        .ocrPageFrom(5)
        .ocrPageTo(110)
        .build();

    Book saved = bookRepository.saveAndFlush(book);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(BookStatus.DRAFT);
    assertThat(saved.getVerified()).isFalse();
    assertThat(saved.getCreatedAt()).isNotNull();
  }

  @Test
  void it_should_persist_book_lesson_pages_and_enforce_unique_book_lesson() {
    LessonContext ctx = seedHierarchy();

    Book book = bookRepository.saveAndFlush(Book.builder()
        .schoolGradeId(ctx.gradeId)
        .subjectId(ctx.subjectId)
        .curriculumId(ctx.curriculumId)
        .title("Sách thử")
        .totalPages(120)
        .ocrPageFrom(1)
        .ocrPageTo(120)
        .build());

    BookLessonPage map1 = BookLessonPage.builder()
        .bookId(book.getId())
        .lessonId(ctx.lesson1Id)
        .pageStart(5)
        .pageEnd(9)
        .orderIndex(1)
        .build();
    BookLessonPage map2 = BookLessonPage.builder()
        .bookId(book.getId())
        .lessonId(ctx.lesson2Id)
        .pageStart(9)  // overlap allowed (page 9 contains end of lesson1 + start of lesson2)
        .pageEnd(15)
        .orderIndex(2)
        .build();

    bookLessonPageRepository.saveAllAndFlush(List.of(map1, map2));

    List<BookLessonPage> ordered = bookLessonPageRepository.findByBookIdOrdered(book.getId());
    assertThat(ordered).hasSize(2);
    assertThat(ordered.get(0).getLessonId()).isEqualTo(ctx.lesson1Id);
    assertThat(ordered.get(1).getLessonId()).isEqualTo(ctx.lesson2Id);

    // Duplicate (book_id, lesson_id) must violate UNIQUE constraint.
    BookLessonPage duplicate = BookLessonPage.builder()
        .bookId(book.getId())
        .lessonId(ctx.lesson1Id)
        .pageStart(50)
        .pageEnd(55)
        .orderIndex(99)
        .build();

    assertThatThrownBy(() -> bookLessonPageRepository.saveAndFlush(duplicate))
        .isInstanceOfAny(PersistenceException.class,
            org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void it_should_soft_delete_all_mappings_for_a_book() {
    LessonContext ctx = seedHierarchy();
    Book book = bookRepository.saveAndFlush(Book.builder()
        .schoolGradeId(ctx.gradeId)
        .subjectId(ctx.subjectId)
        .curriculumId(ctx.curriculumId)
        .title("Soft delete test")
        .ocrPageFrom(1)
        .ocrPageTo(120)
        .build());

    bookLessonPageRepository.saveAllAndFlush(List.of(
        BookLessonPage.builder().bookId(book.getId()).lessonId(ctx.lesson1Id)
            .pageStart(1).pageEnd(2).orderIndex(1).build(),
        BookLessonPage.builder().bookId(book.getId()).lessonId(ctx.lesson2Id)
            .pageStart(3).pageEnd(4).orderIndex(2).build()));

    int affected = bookLessonPageRepository.softDeleteAllByBookId(book.getId(), UUID.randomUUID());
    em.flush();
    em.clear();

    assertThat(affected).isEqualTo(2);
    assertThat(bookLessonPageRepository.findByBookIdOrdered(book.getId())).isEmpty();
    assertThat(bookLessonPageRepository.countByBookId(book.getId())).isZero();
  }

  private record LessonContext(
      UUID gradeId, UUID subjectId, UUID curriculumId, UUID lesson1Id, UUID lesson2Id) {}
}
