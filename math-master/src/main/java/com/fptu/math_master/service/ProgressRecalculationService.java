package com.fptu.math_master.service;

import java.util.UUID;

/**
 * Service for recalculating and fixing progress data
 */
public interface ProgressRecalculationService {

  /**
   * Recalculate progress for a specific roadmap
   */
  void recalculateRoadmapProgress(UUID roadmapId);

  /**
   * Recalculate progress for all roadmaps
   */
  void recalculateAllRoadmapProgress();

  /**
   * Clean up invalid entry test references
   */
  void cleanupInvalidEntryTests();

  /**
   * Diagnose progress calculation issues for a roadmap
   */
  String diagnoseProgressIssues(UUID roadmapId);
}