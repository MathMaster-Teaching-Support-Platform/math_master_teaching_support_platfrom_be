package com.fptu.math_master.repository;

import com.fptu.math_master.entity.BookLessonPage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookLessonPageRepository extends JpaRepository<BookLessonPage, UUID> {

  @Query(
      "SELECT m FROM BookLessonPage m WHERE m.bookId = :bookId AND m.deletedAt IS NULL"
          + " ORDER BY m.orderIndex, m.pageStart")
  List<BookLessonPage> findByBookIdOrdered(@Param("bookId") UUID bookId);

  @Query(
      "SELECT m FROM BookLessonPage m WHERE m.bookId = :bookId AND m.lessonId = :lessonId"
          + " AND m.deletedAt IS NULL")
  Optional<BookLessonPage> findByBookIdAndLessonId(
      @Param("bookId") UUID bookId, @Param("lessonId") UUID lessonId);

  @Query(
      "SELECT m FROM BookLessonPage m WHERE m.lessonId = :lessonId AND m.deletedAt IS NULL"
          + " ORDER BY m.createdAt DESC")
  List<BookLessonPage> findByLessonId(@Param("lessonId") UUID lessonId);

  @Query("SELECT COUNT(m) FROM BookLessonPage m WHERE m.bookId = :bookId AND m.deletedAt IS NULL")
  long countByBookId(@Param("bookId") UUID bookId);

  @Modifying
  @Query(
      "UPDATE BookLessonPage m SET m.deletedAt = CURRENT_TIMESTAMP, m.deletedBy = :userId"
          + " WHERE m.bookId = :bookId AND m.deletedAt IS NULL")
  int softDeleteAllByBookId(@Param("bookId") UUID bookId, @Param("userId") UUID userId);

  @Query(
      "SELECT m FROM BookLessonPage m WHERE m.bookId = :bookId"
          + " AND m.lessonId IN :lessonIds AND m.deletedAt IS NULL")
  List<BookLessonPage> findByBookIdAndLessonIds(
      @Param("bookId") UUID bookId, @Param("lessonIds") Collection<UUID> lessonIds);

  /**
   * Hard-delete every mapping (live or soft-deleted) for a book. Used by bulk-replace upsert so the
   * UNIQUE(book_id, lesson_id) constraint doesn't collide with stale soft-deleted rows when the
   * same lesson is re-added.
   */
  @Modifying
  @Query("DELETE FROM BookLessonPage m WHERE m.bookId = :bookId")
  int hardDeleteAllByBookId(@Param("bookId") UUID bookId);
}
