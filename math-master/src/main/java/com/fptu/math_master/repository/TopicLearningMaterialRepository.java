package com.fptu.math_master.repository;

import com.fptu.math_master.entity.TopicLearningMaterial;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicLearningMaterialRepository
    extends JpaRepository<TopicLearningMaterial, UUID> {

  /**
   * Find all materials for a topic
   */
  List<TopicLearningMaterial> findByTopicIdOrderBySequenceOrder(UUID topicId);

  /**
   * Find required materials only
   */
  List<TopicLearningMaterial> findByTopicIdAndIsRequiredTrueOrderBySequenceOrder(UUID topicId);

  /**
   * Find materials by type
   */
  List<TopicLearningMaterial> findByTopicIdAndResourceType(UUID topicId, String resourceType);

  /**
   * Find materials linked to specific lesson
   */
  List<TopicLearningMaterial> findByLessonIdAndTopicId(UUID lessonId, UUID topicId);

  /**
   * Find materials linked to specific question
   */
  List<TopicLearningMaterial> findByQuestionIdAndTopicId(UUID questionId, UUID topicId);
}
