package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Subject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

  Optional<Subject> findByCode(String code);

  @Query("SELECT s FROM Subject s WHERE s.isActive = true ORDER BY s.name")
  List<Subject> findAllActive();

  @Query(
      """
      SELECT s FROM Subject s
      JOIN s.schoolGrade sg
      WHERE s.isActive = true
        AND sg.gradeLevel = :grade
        AND sg.isActive = true
        AND sg.deletedAt IS NULL
      ORDER BY s.name
      """)
  List<Subject> findActiveByGradeLevel(@Param("grade") Integer grade);

  List<Subject> findBySchoolGradeIdAndIsActiveTrueOrderByName(UUID schoolGradeId);

  boolean existsByCode(String code);
}
