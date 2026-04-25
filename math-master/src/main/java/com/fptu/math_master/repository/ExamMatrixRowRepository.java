package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrixRow;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamMatrixRowRepository extends JpaRepository<ExamMatrixRow, UUID> {

  List<ExamMatrixRow> findByExamMatrixIdOrderByOrderIndex(UUID examMatrixId);

  List<ExamMatrixRow> findByExamMatrixId(UUID examMatrixId);

  @Query(
      """
      SELECT r FROM ExamMatrixRow r
      WHERE r.examMatrixId = :matrixId AND r.chapterId = :chapterId
      ORDER BY r.orderIndex
      """)
  List<ExamMatrixRow> findByMatrixAndChapter(
      @Param("matrixId") UUID matrixId, @Param("chapterId") UUID chapterId);

  @Query(
      "SELECT DISTINCT r.lessonId FROM ExamMatrixRow r "
          + "WHERE r.examMatrixId = :matrixId "
          + "AND r.lessonId IS NOT NULL")
  List<UUID> findDistinctLessonIdsByExamMatrixId(@Param("matrixId") UUID matrixId);

  void deleteAllByExamMatrixId(UUID examMatrixId);
}
