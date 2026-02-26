package com.fptu.math_master.enums;

/**
 * Status of a student's learning roadmap
 */
public enum RoadmapStatus {
  /** Roadmap has been generated but not yet started */
  GENERATED,
  /** Student has started the roadmap */
  IN_PROGRESS,
  /** Roadmap has been completed */
  COMPLETED,
  /** Roadmap has been archived or is no longer active */
  ARCHIVED
}
