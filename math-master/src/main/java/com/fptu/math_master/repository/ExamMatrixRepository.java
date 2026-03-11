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

  @Query("SELECT DISTINCT em FROM ExamMatrix em JOIN em.assessments a WHERE a.id = :assessmentId AND em.deletedAt IS NULL")
  Optional<ExamMatrix> findByAssessmentIdAndNotDeleted(@Param("assessmentId") UUID assessmentId);
}
