package com.fptu.math_master.constant;

/**
 * Constants for Learning Roadmap feature
 */
public final class RoadmapConstant {

  private RoadmapConstant() {}

  // ============================================================================
  // THRESHOLD CONSTANTS
  // ============================================================================

  /** Minimum score percentage to consider a topic as weak area */
  public static final int WEAK_TOPIC_THRESHOLD = 70;

  /** Minimum performance data points required for personalized roadmap */
  public static final int MIN_GRADES_FOR_PERSONALIZATION = 3;

  /** Number of subtopics to generate per topic by default */
  public static final int DEFAULT_SUBTOPICS_PER_TOPIC = 3;

  /** Estimated learning hours per day for students */
  public static final int ESTIMATED_HOURS_PER_DAY = 5;

  // ============================================================================
  // TOPIC PRIORITY CONSTANTS
  // ============================================================================

  /** Priority multiplier for weak areas (negative number for high priority) */
  public static final int WEAK_AREA_PRIORITY_BASE = -100;

  /** Default priority for normal topics */
  public static final int DEFAULT_PRIORITY = 0;

  /** Priority offset added per percentage point below threshold */
  public static final int PRIORITY_OFFSET_PER_PERCENT = 1;

  // ============================================================================
  // MATERIAL LIMITS
  // ============================================================================

  /** Maximum number of lessons to link per topic */
  public static final int MAX_LESSONS_PER_TOPIC = 3;

  /** Maximum number of practice questions to link per topic */
  public static final int MAX_QUESTIONS_PER_TOPIC = 5;

  // ============================================================================
  // RESOURCE TYPES
  // ============================================================================

  public static final String RESOURCE_TYPE_LESSON = "LESSON";
  public static final String RESOURCE_TYPE_QUESTION = "QUESTION";
  public static final String RESOURCE_TYPE_EXAMPLE = "EXAMPLE";
  public static final String RESOURCE_TYPE_PRACTICE = "PRACTICE";
  public static final String RESOURCE_TYPE_ASSESSMENT = "ASSESSMENT";

  // ============================================================================
  // ERROR MESSAGES
  // ============================================================================

  public static final String ROADMAP_NOT_FOUND = "Roadmap not found";
  public static final String TOPIC_NOT_FOUND = "Topic not found";
  public static final String SUBTOPIC_NOT_FOUND = "Subtopic not found";
  public static final String MATERIAL_NOT_FOUND = "Learning material not found";
  public static final String STUDENT_NOT_FOUND = "Student not found";
  public static final String INSUFFICIENT_PERFORMANCE_DATA = "Insufficient performance data for personalization";

  // ============================================================================
  // SUCCESS MESSAGES
  // ============================================================================

  public static final String ROADMAP_GENERATED = "Roadmap generated successfully";
  public static final String PROGRESS_UPDATED = "Progress updated successfully";
  public static final String SUBTOPIC_COMPLETED = "Subtopic completed successfully";
  public static final String MATERIAL_LINKED = "Material linked successfully";
}
