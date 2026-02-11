package com.fptu.math_master.repository;

import com.fptu.math_master.entity.MatrixCell;
import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatrixCellRepository extends JpaRepository<MatrixCell, UUID> {

  @Query("SELECT mc FROM MatrixCell mc WHERE mc.matrixId = :matrixId ORDER BY mc.createdAt")
  List<MatrixCell> findByMatrixIdOrderByCreatedAt(@Param("matrixId") UUID matrixId);

  @Query("SELECT SUM(mc.numQuestions) FROM MatrixCell mc WHERE mc.matrixId = :matrixId")
  Integer sumQuestionsByMatrixId(@Param("matrixId") UUID matrixId);

  @Query("SELECT SUM(mc.numQuestions * mc.pointsPerQuestion) FROM MatrixCell mc WHERE mc.matrixId = :matrixId")
  Double sumPointsByMatrixId(@Param("matrixId") UUID matrixId);

  @Query("SELECT mc.difficulty, SUM(mc.numQuestions) FROM MatrixCell mc " +
         "WHERE mc.matrixId = :matrixId GROUP BY mc.difficulty")
  List<Object[]> getDifficultyDistribution(@Param("matrixId") UUID matrixId);

  @Query("SELECT mc.cognitiveLevel, SUM(mc.numQuestions) FROM MatrixCell mc " +
         "WHERE mc.matrixId = :matrixId GROUP BY mc.cognitiveLevel")
  List<Object[]> getCognitiveLevelDistribution(@Param("matrixId") UUID matrixId);

  @Query("SELECT c.title, SUM(mc.numQuestions) FROM MatrixCell mc " +
         "JOIN Chapter c ON mc.chapterId = c.id " +
         "WHERE mc.matrixId = :matrixId GROUP BY c.id, c.title")
  List<Object[]> getChapterDistribution(@Param("matrixId") UUID matrixId);

  @Query("SELECT COUNT(DISTINCT mc.cognitiveLevel) FROM MatrixCell mc WHERE mc.matrixId = :matrixId")
  Long countDistinctCognitiveLevels(@Param("matrixId") UUID matrixId);
}

