package com.fptu.math_master.enums;

public enum AssessmentStatus {
  DRAFT,
  PUBLISHED,
  CLOSED;

  public boolean isTerminal() {
    return this == CLOSED;
  }
}
