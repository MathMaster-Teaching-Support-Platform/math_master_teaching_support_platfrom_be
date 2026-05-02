package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrixTemplateMapping;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamMatrixTemplateMappingRepository
    extends JpaRepository<ExamMatrixTemplateMapping, UUID> {

  @Query(
      "SELECT m FROM ExamMatrixTemplateMapping m WHERE m.examMatrixId = :matrixId ORDER BY m.createdAt")
  List<ExamMatrixTemplateMapping> findByExamMatrixIdOrderByCreatedAt(
      @Param("matrixId") UUID matrixId);

  @Query(
      "SELECT m FROM ExamMatrixTemplateMapping m WHERE m.id = :id AND m.examMatrixId = :matrixId")
  Optional<ExamMatrixTemplateMapping> findByIdAndExamMatrixId(
      @Param("id") UUID id, @Param("matrixId") UUID matrixId);

  @Query(
      "SELECT SUM(m.questionCount) FROM ExamMatrixTemplateMapping m WHERE m.examMatrixId = :matrixId")
  Integer sumQuestionCountByMatrixId(@Param("matrixId") UUID matrixId);

  @Query(
      "SELECT SUM(m.questionCount * m.pointsPerQuestion) FROM ExamMatrixTemplateMapping m WHERE m.examMatrixId = :matrixId")
  Double sumPointsByMatrixId(@Param("matrixId") UUID matrixId);

  @Query("SELECT COUNT(m) FROM ExamMatrixTemplateMapping m WHERE m.examMatrixId = :matrixId")
  Long countByExamMatrixId(@Param("matrixId") UUID matrixId);

  @Query(
      "SELECT DISTINCT qt.lessonId FROM ExamMatrixTemplateMapping m "
          + "JOIN m.questionTemplate qt "
          + "WHERE m.examMatrixId = :matrixId "
          + "AND qt.lessonId IS NOT NULL "
          + "AND qt.deletedAt IS NULL")
  List<UUID> findDistinctLessonIdsByExamMatrixId(@Param("matrixId") UUID matrixId);

  // Delete all template mappings for a specific row (used when deleting a row)
  @org.springframework.data.jpa.repository.Modifying
  @Query("DELETE FROM ExamMatrixTemplateMapping m WHERE m.matrixRowId = :rowId")
  void deleteByMatrixRowId(@Param("rowId") UUID rowId);
}
