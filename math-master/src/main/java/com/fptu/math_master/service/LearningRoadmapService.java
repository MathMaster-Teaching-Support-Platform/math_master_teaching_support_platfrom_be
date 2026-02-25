package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CompleteSubtopicRequest;
import com.fptu.math_master.dto.request.GenerateRoadmapRequest;
import com.fptu.math_master.dto.request.UpdateTopicProgressRequest;
import com.fptu.math_master.dto.response.*;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing student learning roadmaps
 *
 * <p>Responsibilities:
 * - Generate personalized learning roadmaps based on student performance
 * - Create default grade-based roadmaps when needed
 * - Track student progress through topics and subtopics
 * - Link learning materials (lessons, questions) to roadmap topics
 * - Calculate roadmap completion statistics
 *
 * <p>Flow:
 * 1. Student requests roadmap generation
 * 2. Service analyzes performance data (if available)
 * 3. Identifies weak topics for prioritization
 * 4. Maps topics to curriculum (lessons/chapters)
 * 5. Generates learning materials links
 * 6. Returns personalized learning path
 */
public interface LearningRoadmapService {

  // ============================================================================
  // ROADMAP GENERATION & RETRIEVAL
  // ============================================================================

  /**
   * Generate a personalized learning roadmap for a student
   *
   * <p>Logic:
   * - If PERSONALIZED: analyze student grades → identify weak topics
   * → prioritize them
   * - If DEFAULT: use curriculum for grade level
   * - If TEACHER_ASSIGNED: use teacher's custom configuration
   * - Link lessons and questions based on difficulty progression
   * - Estimate completion time
   *
   * @param request generation request with student, subject, grade, type
   * @return detailed roadmap response with topics and materials
   */
  RoadmapDetailResponse generateRoadmap(GenerateRoadmapRequest request);

  /**
   * Get existing roadmap by ID with all details
   */
  RoadmapDetailResponse getRoadmapById(UUID roadmapId);

  /**
   * Get all roadmaps for a student (paginated)
   */
  Page<RoadmapSummaryResponse> getStudentRoadmaps(UUID studentId, Pageable pageable);

  /**
   * Get active roadmap for specific student and subject
   */
  RoadmapDetailResponse getActiveRoadmapBySubject(UUID studentId, String subject);

  /**
   * List all roadmaps for a student
   */
  List<RoadmapSummaryResponse> getStudentRoadmapsList(UUID studentId);

  /**
   * Get roadmaps assigned by a teacher
   */
  List<RoadmapSummaryResponse> getTeacherAssignedRoadmaps(UUID teacherId);

  // ============================================================================
  // PROGRESS TRACKING
  // ============================================================================

  /**
   * Update a topic's progress
   *
   * <p>Logic:
   * - Mark topic as started/in progress/completed
   * - Update overall roadmap progress
   * - Check if prerequisites are met (for LOCKED topics)
   * - Trigger notifications if milestone reached
   *
   * @param request topic progress update
   * @return updated topic response
   */
  RoadmapTopicResponse updateTopicProgress(UpdateTopicProgressRequest request);

  /**
   * Mark a subtopic as completed
   *
   * <p>Logic:
   * - Update subtopic status to COMPLETED
   * - Update parent topic completion count
   * - Recalculate parent topic progress percentage
   * - Check if topic should transition to COMPLETED
   *
   * @param request completion details
   * @return updated subtopic response
   */
  RoadmapSubtopicResponse completeSubtopic(CompleteSubtopicRequest request);

  /**
   * Get next recommended topic to learn
   *
   * <p>Logic:
   * - Find first NOT_STARTED topic
   * - Check prerequisites are completed
   * - Return with learning materials
   *
   * @param roadmapId the roadmap ID
   * @return next topic to study
   */
  RoadmapTopicResponse getNextTopic(UUID roadmapId);

  /**
   * Get current progress stats for roadmap
   */
  RoadmapStatsResponse getRoadmapStats(UUID roadmapId);

  // ============================================================================
  // TOPIC & MATERIALS MANAGEMENT
  // ============================================================================

  /**
   * Get detailed information for a specific topic
   */
  RoadmapTopicResponse getTopicDetails(UUID topicId);

  /**
   * Get all materials for a topic
   */
  List<TopicMaterialResponse> getTopicMaterials(UUID topicId);

  /**
   * Get learning materials by resource type
   */
  List<TopicMaterialResponse> getMaterialsByType(UUID topicId, String resourceType);

  /**
   * Link learning material (lesson/question) to a topic
   *
   * <p>Used when:
   * - Generating roadmap and matching resources
   * - Teacher customizing roadmap with additional materials
   *
   * @param topicId target topic
   * @param lessonId lesson to link (optional)
   * @param questionId question to link (optional)
   * @param resourceType type of resource
   * @param isRequired whether material is mandatory
   * @return created material response
   */
  TopicMaterialResponse linkMaterialToTopic(UUID topicId, UUID lessonId, UUID questionId,
      String resourceType, Boolean isRequired);

  /**
   * Remove material from topic
   */
  void removeMaterialFromTopic(UUID materialId);

  // ============================================================================
  // WEAK AREA ANALYSIS
  // ============================================================================

  /**
   * Analyze student's weak topics based on performance data
   *
   * <p>Logic:
   * - Query grades for subject
   * - Find topics with low scores
   * - Identify patterns in performance
   * - Return prioritized weak areas
   *
   * @param studentId student to analyze
   * @param subject subject area
   * @return list of weak topics with priority
   */
  List<RoadmapTopicResponse> analyzeWeakTopics(UUID studentId, String subject);

  /**
   * Re-generate roadmap with updated performance data
   *
   * <p>Useful when:
   * - Student completes assessments
   * - New performance data available
   * - Teacher wants to update roadmap priorities
   *
   * @param roadmapId existing roadmap
   * @return regenerated roadmap with updated priorities
   */
  RoadmapDetailResponse refreshRoadmapWithPerformanceData(UUID roadmapId);

  // ============================================================================
  // UTILITY & ADMINISTRATION
  // ============================================================================

  /**
   * Check if roadmap exists for student and subject
   */
  boolean existsActiveRoadmap(UUID studentId, String subject);

  /**
   * Soft delete a roadmap (archive it)
   */
  void archiveRoadmap(UUID roadmapId);

  /**
   * Get roadmap completion estimate in days
   */
  Integer estimateCompletionDays(UUID roadmapId);

  /**
   * Calculate overall progress percentage for roadmap
   *
   * @param roadmapId roadmap ID
   * @return progress 0-100
   */
  java.math.BigDecimal calculateRoadmapProgress(UUID roadmapId);
}
