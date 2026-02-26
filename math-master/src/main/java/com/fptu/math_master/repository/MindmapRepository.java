package com.fptu.math_master.repository;

import com.fptu.math_master.entity.Mindmap;
import com.fptu.math_master.enums.MindmapStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MindmapRepository
    extends JpaRepository<Mindmap, UUID>, JpaSpecificationExecutor<Mindmap> {

  @Query("SELECT m FROM Mindmap m WHERE m.id = :id AND m.deletedAt IS NULL")
  Optional<Mindmap> findByIdAndNotDeleted(@Param("id") UUID id);

  @Query(
      "SELECT m FROM Mindmap m WHERE m.teacherId = :teacherId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
  Page<Mindmap> findByTeacherIdAndNotDeleted(@Param("teacherId") UUID teacherId, Pageable pageable);

  @Query(
      "SELECT m FROM Mindmap m WHERE m.lessonId = :lessonId AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
  Page<Mindmap> findByLessonIdAndNotDeleted(@Param("lessonId") UUID lessonId, Pageable pageable);

  @Query(
      "SELECT m FROM Mindmap m WHERE m.status = :status AND m.deletedAt IS NULL ORDER BY m.createdAt DESC")
  Page<Mindmap> findByStatusAndNotDeleted(@Param("status") MindmapStatus status, Pageable pageable);

  @Query(
      "SELECT m FROM Mindmap m WHERE m.teacherId = :teacherId AND m.lessonId = :lessonId AND m.deletedAt IS NULL")
  Page<Mindmap> findByTeacherIdAndLessonIdAndNotDeleted(
      @Param("teacherId") UUID teacherId, @Param("lessonId") UUID lessonId, Pageable pageable);

  @Query("SELECT COUNT(m) FROM Mindmap m WHERE m.teacherId = :teacherId AND m.deletedAt IS NULL")
  long countByTeacherId(@Param("teacherId") UUID teacherId);
}
