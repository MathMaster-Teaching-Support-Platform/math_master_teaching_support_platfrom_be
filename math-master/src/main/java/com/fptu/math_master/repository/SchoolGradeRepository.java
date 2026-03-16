package com.fptu.math_master.repository;

import com.fptu.math_master.entity.SchoolGrade;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolGradeRepository extends JpaRepository<SchoolGrade, UUID> {

  @Query("SELECT sg FROM SchoolGrade sg WHERE sg.id = :id AND sg.deletedAt IS NULL")
  Optional<SchoolGrade> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT sg FROM SchoolGrade sg WHERE sg.deletedAt IS NULL AND sg.isActive = true ORDER BY sg.gradeLevel")
  List<SchoolGrade> findAllActiveNotDeleted();

  @Query("SELECT sg FROM SchoolGrade sg WHERE sg.deletedAt IS NULL ORDER BY sg.gradeLevel")
  List<SchoolGrade> findAllNotDeleted();

  Optional<SchoolGrade> findByGradeLevel(Integer gradeLevel);

  boolean existsByGradeLevel(Integer gradeLevel);

  boolean existsByGradeLevelAndIsActiveTrue(Integer gradeLevel);
}
