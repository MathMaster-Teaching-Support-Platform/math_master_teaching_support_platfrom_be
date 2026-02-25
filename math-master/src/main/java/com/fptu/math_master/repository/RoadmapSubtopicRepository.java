package com.fptu.math_master.repository;

import com.fptu.math_master.entity.RoadmapSubtopic;
import com.fptu.math_master.enums.TopicStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoadmapSubtopicRepository extends JpaRepository<RoadmapSubtopic, UUID> {

  /**
   * Find all subtopics for a topic
   */
  List<RoadmapSubtopic> findByTopicIdOrderBySequenceOrder(UUID topicId);

  /**
   * Find subtopics by status
   */
  List<RoadmapSubtopic> findByTopicIdAndStatus(UUID topicId, TopicStatus status);

  /**
   * Count subtopics by status
   */
  Long countByTopicIdAndStatus(UUID topicId, TopicStatus status);
}
