package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.enums.CurriculumCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, UUID> {

  @Query(
      "SELECT c FROM Curriculum c WHERE c.grade = :grade AND c.category = :category"
          + " AND c.deletedAt IS NULL ORDER BY c.name")
  List<Curriculum> findByGradeAndCategoryAndNotDeleted(
      @Param("grade") Integer grade, @Param("category") CurriculumCategory category);

  @Query(
      "SELECT c FROM Curriculum c WHERE c.grade = :grade AND c.deletedAt IS NULL"
          + " ORDER BY c.name")
  List<Curriculum> findByGradeAndNotDeleted(@Param("grade") Integer grade);

  @Query(
      "SELECT c FROM Curriculum c WHERE c.category = :category AND c.deletedAt IS NULL"
          + " ORDER BY c.grade, c.name")
  List<Curriculum> findByCategoryAndNotDeleted(@Param("category") CurriculumCategory category);

  @Query(
      "SELECT c FROM Curriculum c WHERE c.name = :name AND c.grade = :grade"
          + " AND c.category = :category AND c.deletedAt IS NULL")
  Optional<Curriculum> findByNameAndGradeAndCategoryAndNotDeleted(
      @Param("name") String name,
      @Param("grade") Integer grade,
      @Param("category") CurriculumCategory category);

  @Query("SELECT c FROM Curriculum c WHERE c.deletedAt IS NULL ORDER BY c.grade, c.name")
  List<Curriculum> findAllNotDeleted();

  @Query("SELECT c FROM Curriculum c WHERE c.id = :id AND c.deletedAt IS NULL")
  Optional<Curriculum> findByIdAndNotDeleted(@Param("id") UUID id);
}
