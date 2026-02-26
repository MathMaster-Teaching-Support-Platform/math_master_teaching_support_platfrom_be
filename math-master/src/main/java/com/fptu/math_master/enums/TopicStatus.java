package com.fptu.math_master.enums;

/**
 * Status of a topic in student's learning roadmap
 */
public enum TopicStatus {
  /** Topic has not been started yet */
  NOT_STARTED,
  /** Student is currently learning this topic */
  IN_PROGRESS,
  /** Student has completed this topic */
  COMPLETED,
  /** Topic is locked and requires prerequisites */
  LOCKED
}
