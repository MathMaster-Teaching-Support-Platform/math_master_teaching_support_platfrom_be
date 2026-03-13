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
      SELECT DISTINCT s FROM Subject s
      JOIN s.gradeSubjects gs
      WHERE gs.gradeLevel = :grade AND gs.isActive = true AND s.isActive = true
      ORDER BY s.name
      """)
  List<Subject> findActiveByGradeLevel(@Param("grade") Integer grade);

  boolean existsByCode(String code);
}
