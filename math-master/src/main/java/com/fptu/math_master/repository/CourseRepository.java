package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Course;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

  Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

  List<Course> findByTeacherIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID teacherId);

  // Public listing — JOIN subject + schoolGrade để đảm bảo còn active
  @Query(
      "SELECT c FROM Course c "
          + "JOIN c.subject s "
          + "JOIN c.schoolGrade sg "
          + "WHERE c.isPublished = true AND c.deletedAt IS NULL "
          + "AND s.isActive = true AND sg.isActive = true "
          + "AND (:schoolGradeId IS NULL OR c.schoolGradeId = :schoolGradeId) "
          + "AND (:subjectId IS NULL OR c.subjectId = :subjectId) "
          + "AND (:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<Course> findPublishedCoursesWithFilter(
      @Param("schoolGradeId") UUID schoolGradeId,
      @Param("subjectId") UUID subjectId,
      @Param("keyword") String keyword,
      Pageable pageable);
}
