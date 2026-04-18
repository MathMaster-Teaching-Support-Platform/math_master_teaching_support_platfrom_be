package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.UpdateTopicProgressRequest;
import com.fptu.math_master.dto.response.*;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing student learning roadmaps.
 *
 * <p>Roadmaps are course-centric: each topic links to exactly one Course.
 * There is no topic locking — students can click any topic at any time.
 */
public interface LearningRoadmapService {

  // ============================================================================
  // ROADMAP RETRIEVAL
  // ============================================================================

  /** Get roadmap by ID with all topic details. */
  RoadmapDetailResponse getRoadmapById(UUID roadmapId);

  /** Get all roadmaps for a student (paginated). */
  Page<RoadmapSummaryResponse> getStudentRoadmaps(UUID studentId, Pageable pageable);

  /** Get active roadmap for specific student and subject. */
  RoadmapDetailResponse getActiveRoadmapBySubject(UUID studentId, String subject);

  /** List all roadmaps for a student. */
  List<RoadmapSummaryResponse> getStudentRoadmapsList(UUID studentId);

  // ============================================================================
  // PROGRESS TRACKING (non-blocking)
  // ============================================================================

  /**
   * Update a topic's progress (non-blocking — does NOT gate other topics).
   *
   * @param request topic progress update
   * @return updated topic response
   */
  RoadmapTopicResponse updateTopicProgress(UpdateTopicProgressRequest request);

  /** Get next recommended topic to learn (first NOT_STARTED or IN_PROGRESS). */
  RoadmapTopicResponse getNextTopic(UUID roadmapId);

  /** Get current progress stats for roadmap. */
  RoadmapStatsResponse getRoadmapStats(UUID roadmapId);

  // ============================================================================
  // TOPIC MANAGEMENT
  // ============================================================================

  /** Get detailed information for a specific topic including its linked course. */
  RoadmapTopicResponse getTopicDetails(UUID topicId);

  // ============================================================================
  // UTILITY & ADMINISTRATION
  // ============================================================================

  /** Check if roadmap exists for student and subject. */
  boolean existsActiveRoadmap(UUID studentId, String subject);

  /** Soft delete a roadmap (archive it). */
  void archiveRoadmap(UUID roadmapId);

  /** Get roadmap completion estimate in days. */
  Integer estimateCompletionDays(UUID roadmapId);

  /** Calculate overall progress percentage for roadmap. */
  java.math.BigDecimal calculateRoadmapProgress(UUID roadmapId);
}
