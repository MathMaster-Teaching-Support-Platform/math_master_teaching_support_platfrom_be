package com.fptu.math_master.repository;

import com.fptu.math_master.entity.ExamMatrix;
import java.util.List;
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

  @Query("SELECT em FROM ExamMatrix em WHERE em.id = (SELECT a.examMatrixId FROM Assessment a WHERE a.id = :assessmentId AND a.deletedAt IS NULL) AND em.deletedAt IS NULL")
  Optional<ExamMatrix> findByAssessmentIdAndNotDeleted(@Param("assessmentId") UUID assessmentId);

  @Query("SELECT em FROM ExamMatrix em WHERE em.teacherId = :teacherId AND em.deletedAt IS NULL")
  List<ExamMatrix> findByTeacherIdAndNotDeleted(@Param("teacherId") UUID teacherId);
}
