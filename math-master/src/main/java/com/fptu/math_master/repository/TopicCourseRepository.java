package com.fptu.math_master.repository;

import com.fptu.math_master.entity.TopicCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicCourseRepository extends JpaRepository<TopicCourse, UUID> {

  /**
   * Find all course associations for a topic
   */
  @Query("SELECT tc FROM TopicCourse tc WHERE tc.topicId = :topicId AND tc.deletedAt IS NULL")
  List<TopicCourse> findByTopicId(@Param("topicId") UUID topicId);

  /**
   * Delete all course associations for a topic
   */
  @Modifying
  @Query("UPDATE TopicCourse tc SET tc.deletedAt = CURRENT_TIMESTAMP WHERE tc.topicId = :topicId AND tc.deletedAt IS NULL")
  void softDeleteByTopicId(@Param("topicId") UUID topicId);

  /**
   * Delete specific course association
   */
  @Modifying
  @Query("UPDATE TopicCourse tc SET tc.deletedAt = CURRENT_TIMESTAMP WHERE tc.topicId = :topicId AND tc.courseId = :courseId AND tc.deletedAt IS NULL")
  void softDeleteByTopicIdAndCourseId(@Param("topicId") UUID topicId, @Param("courseId") UUID courseId);

  /**
   * Check if topic-course association exists
   */
  @Query("SELECT COUNT(tc) > 0 FROM TopicCourse tc WHERE tc.topicId = :topicId AND tc.courseId = :courseId AND tc.deletedAt IS NULL")
  boolean existsByTopicIdAndCourseId(@Param("topicId") UUID topicId, @Param("courseId") UUID courseId);
}
