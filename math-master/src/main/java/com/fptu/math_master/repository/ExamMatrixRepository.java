package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrix;
import com.fptu.math_master.enums.MatrixStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamMatrixRepository extends JpaRepository<ExamMatrix, UUID> {

  @Query("SELECT em FROM ExamMatrix em WHERE em.id = :id AND em.deletedAt IS NULL")
  Optional<ExamMatrix> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT em FROM ExamMatrix em WHERE em.id = (SELECT a.examMatrixId FROM Assessment a WHERE a.id = :assessmentId AND a.deletedAt IS NULL) AND em.deletedAt IS NULL")
  Optional<ExamMatrix> findByAssessmentIdAndNotDeleted(@Param("assessmentId") UUID assessmentId);

  @Query("SELECT em FROM ExamMatrix em WHERE em.teacherId = :teacherId AND em.deletedAt IS NULL")
  List<ExamMatrix> findByTeacherIdAndNotDeleted(@Param("teacherId") UUID teacherId);

  @Query(
      "SELECT em FROM ExamMatrix em "
          + "WHERE em.teacherId = :teacherId AND em.deletedAt IS NULL "
          + "AND (:status IS NULL OR em.status = :status) "
          + "AND (CAST(:search AS string) IS NULL OR LOWER(em.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) "
          + "     OR (CAST(:search AS string) IS NOT NULL AND LOWER(em.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))))")
  Page<ExamMatrix> findByTeacherIdWithFilters(
      @Param("teacherId") UUID teacherId,
      @Param("status") MatrixStatus status,
      @Param("search") String search,
      Pageable pageable);
}
