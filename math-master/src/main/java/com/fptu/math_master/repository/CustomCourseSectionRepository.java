package com.fptu.math_master.repository;

import com.fptu.math_master.entity.CustomCourseSection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomCourseSectionRepository extends JpaRepository<CustomCourseSection, UUID> {

  List<CustomCourseSection> findByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID courseId);
  
  long countByCourseIdAndDeletedAtIsNull(UUID courseId);

  Optional<CustomCourseSection> findByIdAndDeletedAtIsNull(UUID id);

  /** Count active (non-deleted) lessons belonging to a section. */
  @Query(
      value =
          "SELECT COUNT(*) FROM course_lessons cl "
              + "WHERE cl.section_id = :sectionId AND cl.deleted_at IS NULL",
      nativeQuery = true)
  long countActiveLessonsBySectionId(@Param("sectionId") UUID sectionId);
}
