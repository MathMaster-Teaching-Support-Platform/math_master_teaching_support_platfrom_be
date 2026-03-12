package com.fptu.math_master.repository;

import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.enums.RoadmapStatus;
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
public interface LearningRoadmapRepository extends JpaRepository<LearningRoadmap, UUID> {

  /**
   * Find all roadmaps for a specific student
   */
  List<LearningRoadmap> findByStudentIdAndDeletedAtIsNull(UUID studentId);

  /**
   * Find roadmap by student and subject (most recent)
   */
  Optional<LearningRoadmap> findTopByStudentIdAndSubjectAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID studentId, String subject);

  /**
   * Find all active roadmaps for a student with pagination
   */
  Page<LearningRoadmap> findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID studentId, Pageable pageable);

  /**
   * Find roadmaps by status
   */
  List<LearningRoadmap> findByStatusAndDeletedAtIsNull(RoadmapStatus status);

  /**
   * Find roadmaps by student and status
   */
  List<LearningRoadmap> findByStudentIdAndStatusAndDeletedAtIsNull(
      UUID studentId, RoadmapStatus status);

  /**
   * Find roadmaps assigned by a specific teacher
   */
  List<LearningRoadmap> findByTeacherIdAndDeletedAtIsNull(UUID teacherId);

  /**
   * Check if roadmap exists for student and subject
   */
  @Query(
      "SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END "
          + "FROM LearningRoadmap r WHERE r.studentId = :studentId "
          + "AND r.subject = :subject AND r.deletedAt IS NULL AND r.status = 'ACTIVE'")
  boolean existsActiveRoadmapForStudentAndSubject(
      @Param("studentId") UUID studentId, @Param("subject") String subject);

  /**
   * Get completed roadmaps count for a student
   */
  Long countByStudentIdAndStatusAndDeletedAtIsNull(UUID studentId, RoadmapStatus status);

  /**
   * Find recently created roadmaps (last N days)
   */
  @Query(
      "SELECT r FROM LearningRoadmap r WHERE r.studentId = :studentId "
          + "AND r.deletedAt IS NULL AND r.createdAt >= :fromDate ORDER BY r.createdAt DESC")
  List<LearningRoadmap> findRecentRoadmaps(
      @Param("studentId") UUID studentId, @Param("fromDate") java.time.Instant fromDate);
}
