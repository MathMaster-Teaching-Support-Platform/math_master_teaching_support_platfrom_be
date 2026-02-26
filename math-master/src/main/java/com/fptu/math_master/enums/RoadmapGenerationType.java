package com.fptu.math_master.enums;

/**
 * Type of roadmap generation strategy
 */
public enum RoadmapGenerationType {
  /** Generated based on student's performance data (personalized) */
  PERSONALIZED,
  /** Generated based on grade level and curriculum (default) */
  DEFAULT,
  /** Manually assigned by teacher */
  TEACHER_ASSIGNED
}
