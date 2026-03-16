package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Chapter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

  @Query(
      "SELECT c FROM Chapter c WHERE c.curriculumId = :curriculumId AND c.deletedAt IS NULL ORDER BY c.orderIndex")
  List<Chapter> findByCurriculumIdAndNotDeleted(@Param("curriculumId") UUID curriculumId);

  @Query(
      "SELECT c FROM Chapter c WHERE c.subjectId = :subjectId AND c.deletedAt IS NULL ORDER BY c.orderIndex")
  List<Chapter> findBySubjectIdAndNotDeleted(@Param("subjectId") UUID subjectId);

  @Query(
      "SELECT COUNT(c) FROM Chapter c WHERE c.curriculumId = :curriculumId AND c.deletedAt IS NULL")
  Long countByCurriculumIdAndNotDeleted(@Param("curriculumId") UUID curriculumId);

    @Query("SELECT COUNT(c) FROM Chapter c WHERE c.subjectId = :subjectId AND c.deletedAt IS NULL")
    Long countBySubjectIdAndNotDeleted(@Param("subjectId") UUID subjectId);

  @Query(
      "SELECT c FROM Chapter c WHERE c.curriculumId = :curriculumId AND c.title = :title"
          + " AND c.deletedAt IS NULL")
  Optional<Chapter> findByCurriculumIdAndTitleAndNotDeleted(
      @Param("curriculumId") UUID curriculumId, @Param("title") String title);

  // Query to find chapter that contains a specific lesson (backward compatibility)
  @Query(
      "SELECT c FROM Chapter c WHERE EXISTS (SELECT 1 FROM Lesson l WHERE l.chapterId = c.id"
          + " AND l.id = :lessonId)")
  List<Chapter> findByLessonIdAndNotDeleted(@Param("lessonId") UUID lessonId);

  // Count lessons in chapters (for validation - checks if lesson belongs to a chapter)
  @Query("SELECT COUNT(l) FROM Lesson l WHERE l.id = :lessonId AND l.deletedAt IS NULL")
  Long countByLessonIdAndNotDeleted(@Param("lessonId") UUID lessonId);
}
