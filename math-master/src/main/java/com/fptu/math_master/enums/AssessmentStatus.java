package com.fptu.math_master.enums;

/**
 * The enum of 'AssessmentStatus'.
 */
public enum AssessmentStatus {
  DRAFT,
  PUBLISHED,
  CLOSED;

  public boolean isTerminal() {
    return this == CLOSED;
  }
}
