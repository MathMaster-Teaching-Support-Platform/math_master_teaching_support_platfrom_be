package com.fptu.math_master.repository;

import com.fptu.math_master.entity.MatrixQuestionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatrixQuestionMappingRepository extends JpaRepository<MatrixQuestionMapping, UUID> {

  @Query("SELECT mqm FROM MatrixQuestionMapping mqm WHERE mqm.matrixCellId = :matrixCellId ORDER BY mqm.selectionPriority")
  List<MatrixQuestionMapping> findByMatrixCellIdOrderByPriority(@Param("matrixCellId") UUID matrixCellId);

  @Query("SELECT mqm.questionId FROM MatrixQuestionMapping mqm " +
         "JOIN MatrixCell mc ON mqm.matrixCellId = mc.id " +
         "WHERE mc.matrixId = :matrixId AND mqm.isSelected = true")
  List<UUID> findSelectedQuestionIdsByMatrixId(@Param("matrixId") UUID matrixId);

  @Query("SELECT COUNT(mqm) FROM MatrixQuestionMapping mqm " +
         "WHERE mqm.matrixCellId = :matrixCellId AND mqm.isSelected = true")
  Long countSelectedByMatrixCellId(@Param("matrixCellId") UUID matrixCellId);

  @Modifying
  @Query("DELETE FROM MatrixQuestionMapping mqm WHERE mqm.matrixCellId = :matrixCellId")
  void deleteByMatrixCellId(@Param("matrixCellId") UUID matrixCellId);
}

