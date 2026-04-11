package com.fptu.math_master.repository;

import com.fptu.math_master.entity.RoadmapFeedback;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadmapFeedbackRepository extends JpaRepository<RoadmapFeedback, UUID> {

  @Query(
      "SELECT rf FROM RoadmapFeedback rf "
          + "WHERE rf.roadmapId = :roadmapId "
          + "AND rf.studentId = :studentId "
          + "AND rf.deletedAt IS NULL")
  Optional<RoadmapFeedback> findByRoadmapIdAndStudentIdAndNotDeleted(
      @Param("roadmapId") UUID roadmapId, @Param("studentId") UUID studentId);

  @Query(
      "SELECT rf FROM RoadmapFeedback rf "
          + "WHERE rf.roadmapId = :roadmapId "
          + "AND rf.deletedAt IS NULL "
          + "ORDER BY rf.createdAt DESC")
  Page<RoadmapFeedback> findByRoadmapIdAndNotDeleted(
      @Param("roadmapId") UUID roadmapId, Pageable pageable);
}
