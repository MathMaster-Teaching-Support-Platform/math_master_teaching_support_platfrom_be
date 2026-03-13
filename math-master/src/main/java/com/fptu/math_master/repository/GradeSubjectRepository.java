package com.fptu.math_master.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fptu.math_master.entity.GradeSubject;

@Repository
public interface GradeSubjectRepository extends JpaRepository<GradeSubject, UUID> {

  List<GradeSubject> findByGradeLevelAndIsActiveTrue(Integer gradeLevel);

  List<GradeSubject> findBySubjectIdAndIsActiveTrue(UUID subjectId);

  Optional<GradeSubject> findByGradeLevelAndSubjectId(Integer gradeLevel, UUID subjectId);

  @Query("SELECT gs FROM GradeSubject gs WHERE gs.isActive = true ORDER BY gs.gradeLevel, gs.subjectId")
  List<GradeSubject> findAllActive();

  boolean existsByGradeLevelAndSubjectId(Integer gradeLevel, UUID subjectId);
}
