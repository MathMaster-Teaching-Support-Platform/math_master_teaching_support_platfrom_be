package com.fptu.math_master.util;

import java.util.Map;

/**
 * Thread-local context for storing scoring detail during grading operations.
 * Used to pass clause-level scoring information from grading methods to callers.
 */
public class ScoringContext {
  private static final ThreadLocal<Map<String, Object>> scoringDetail = new ThreadLocal<>();

  /**
   * Set the scoring detail for the current thread.
   *
   * @param detail the scoring detail map
   */
  public static void setScoringDetail(Map<String, Object> detail) {
    scoringDetail.set(detail);
  }

  /**
   * Get the scoring detail for the current thread.
   *
   * @return the scoring detail map, or null if not set
   */
  public static Map<String, Object> getScoringDetail() {
    return scoringDetail.get();
  }

  /**
   * Clear the scoring detail for the current thread.
   */
  public static void clear() {
    scoringDetail.remove();
  }
}
