package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrix;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamMatrixRepository extends JpaRepository<ExamMatrix, UUID> {

  @Query("SELECT em FROM ExamMatrix em WHERE em.id = :id AND em.deletedAt IS NULL")
  Optional<ExamMatrix> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT em FROM ExamMatrix em WHERE em.assessmentId = :assessmentId AND em.deletedAt IS NULL")
  Optional<ExamMatrix> findByAssessmentIdAndNotDeleted(@Param("assessmentId") UUID assessmentId);

  @Query(
      "SELECT COUNT(em) > 0 FROM ExamMatrix em WHERE em.assessmentId = :assessmentId AND em.deletedAt IS NULL")
  boolean existsByAssessmentId(@Param("assessmentId") UUID assessmentId);

  @Query("SELECT COUNT(mc) FROM MatrixCell mc WHERE mc.matrixId = :matrixId")
  Long countCellsByMatrixId(@Param("matrixId") UUID matrixId);

  @Query(
      "SELECT COUNT(mc) FROM MatrixCell mc WHERE mc.matrixId = :matrixId AND mc.numQuestions > 0")
  Long countFilledCellsByMatrixId(@Param("matrixId") UUID matrixId);

  @Query(
      "SELECT COUNT(mqm) FROM MatrixQuestionMapping mqm "
          + "JOIN MatrixCell mc ON mqm.matrixCellId = mc.id "
          + "WHERE mc.matrixId = :matrixId AND mqm.isSelected = true")
  Long countSelectedQuestionsByMatrixId(@Param("matrixId") UUID matrixId);
}
