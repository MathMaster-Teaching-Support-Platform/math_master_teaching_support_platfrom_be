package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Book;
import com.fptu.math_master.enums.BookStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository
    extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

  @EntityGraph(
      type = EntityGraph.EntityGraphType.LOAD,
      attributePaths = {"schoolGrade", "subject", "curriculum"})
  @Query("SELECT b FROM Book b WHERE b.id = :id AND b.deletedAt IS NULL")
  Optional<Book> findByIdAndNotDeleted(@Param("id") UUID id);

  @EntityGraph(
      type = EntityGraph.EntityGraphType.LOAD,
      attributePaths = {"schoolGrade", "subject", "curriculum"})
  @Query(
      "SELECT b FROM Book b WHERE b.deletedAt IS NULL"
          + " AND (:schoolGradeId IS NULL OR b.schoolGradeId = :schoolGradeId)"
          + " AND (:subjectId IS NULL OR b.subjectId = :subjectId)"
          + " AND (:curriculumId IS NULL OR b.curriculumId = :curriculumId)"
          + " AND (:chapterId IS NULL OR EXISTS ("
          + "   SELECT 1 FROM BookLessonPage blp"
          + "   JOIN Lesson l ON l.id = blp.lessonId"
          + "   WHERE blp.bookId = b.id"
          + "     AND l.chapterId = :chapterId"
          + "     AND l.deletedAt IS NULL"
          + " ))"
          + " AND (:lessonId IS NULL OR EXISTS ("
          + "   SELECT 1 FROM BookLessonPage blp"
          + "   WHERE blp.bookId = b.id"
          + "     AND blp.lessonId = :lessonId"
          + " ))"
          + " AND (:status IS NULL OR b.status = :status)"
          + " ORDER BY b.createdAt DESC")
  Page<Book> search(
      @Param("schoolGradeId") UUID schoolGradeId,
      @Param("subjectId") UUID subjectId,
      @Param("curriculumId") UUID curriculumId,
      @Param("chapterId") UUID chapterId,
      @Param("lessonId") UUID lessonId,
      @Param("status") BookStatus status,
      Pageable pageable);

  @Query(
      "SELECT b FROM Book b WHERE b.subjectId = :subjectId AND b.deletedAt IS NULL"
          + " ORDER BY b.createdAt DESC")
  List<Book> findBySubjectIdAndNotDeleted(@Param("subjectId") UUID subjectId);
}
