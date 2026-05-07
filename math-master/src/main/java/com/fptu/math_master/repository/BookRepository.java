package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Book;
import com.fptu.math_master.enums.BookStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository
    extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

  @Query("SELECT b FROM Book b WHERE b.id = :id AND b.deletedAt IS NULL")
  Optional<Book> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT b FROM Book b WHERE b.deletedAt IS NULL"
          + " AND (:schoolGradeId IS NULL OR b.schoolGradeId = :schoolGradeId)"
          + " AND (:subjectId IS NULL OR b.subjectId = :subjectId)"
          + " AND (:curriculumId IS NULL OR b.curriculumId = :curriculumId)"
          + " AND (:status IS NULL OR b.status = :status)"
          + " ORDER BY b.createdAt DESC")
  Page<Book> search(
      @Param("schoolGradeId") UUID schoolGradeId,
      @Param("subjectId") UUID subjectId,
      @Param("curriculumId") UUID curriculumId,
      @Param("status") BookStatus status,
      Pageable pageable);

  @Query(
      "SELECT b FROM Book b WHERE b.subjectId = :subjectId AND b.deletedAt IS NULL"
          + " ORDER BY b.createdAt DESC")
  List<Book> findBySubjectIdAndNotDeleted(@Param("subjectId") UUID subjectId);
}
