package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Chapter;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

  @Query(
      "SELECT c FROM Chapter c WHERE c.lessonId = :lessonId AND c.deletedAt IS NULL ORDER BY c.orderIndex")
  List<Chapter> findByLessonIdAndNotDeleted(@Param("lessonId") UUID lessonId);

  @Query("SELECT COUNT(c) FROM Chapter c WHERE c.lessonId = :lessonId AND c.deletedAt IS NULL")
  Long countByLessonIdAndNotDeleted(@Param("lessonId") UUID lessonId);
}
