package com.fptu.math_master.repository;

import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.TopicStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadmapTopicRepository extends JpaRepository<RoadmapTopic, UUID> {

  /**
   * Find all topics in a roadmap ordered by sequence
   */
  List<RoadmapTopic> findByRoadmapIdOrderBySequenceOrder(UUID roadmapId);

  /**
   * Find topics by status
   */
  List<RoadmapTopic> findByRoadmapIdAndStatus(UUID roadmapId, TopicStatus status);

  /**
   * Find topics by difficulty
   */
  List<RoadmapTopic> findByRoadmapIdAndDifficulty(UUID roadmapId, QuestionDifficulty difficulty);

  /**
   * Find topics by roadmap and priority (for weak areas)
   */
  @Query("SELECT t FROM RoadmapTopic t WHERE t.roadmapId = :roadmapId "
      + "ORDER BY t.priority ASC, t.sequenceOrder ASC")
  List<RoadmapTopic> findTopicsByPriority(@Param("roadmapId") UUID roadmapId);

  /**
   * Find next topic to learn (first NOT_STARTED or in progress)
   */
  @Query("SELECT t FROM RoadmapTopic t WHERE t.roadmapId = :roadmapId "
      + "AND (t.status = 'NOT_STARTED' OR t.status = 'IN_PROGRESS') "
      + "ORDER BY t.sequenceOrder ASC LIMIT 1")
  Optional<RoadmapTopic> findNextTopic(@Param("roadmapId") UUID roadmapId);

  /**
   * Count completed topics in roadmap
   */
  Long countByRoadmapIdAndStatus(UUID roadmapId, TopicStatus status);

  /**
   * Check if all prerequisites for a topic are completed
   */
  @Query("SELECT CASE WHEN COUNT(t) = 0 THEN true ELSE false END "
      + "FROM RoadmapTopic t WHERE t.roadmapId = :roadmapId "
      + "AND t.sequenceOrder < :sequenceOrder AND t.status != 'COMPLETED'")
  boolean arePrerequisitesCompleted(@Param("roadmapId") UUID roadmapId,
      @Param("sequenceOrder") Integer sequenceOrder);
}
