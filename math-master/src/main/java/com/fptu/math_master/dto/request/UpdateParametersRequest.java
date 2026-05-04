package com.fptu.math_master.dto.request;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Request DTO for Feature 2: Teacher adjusts AI-generated parameter values
 * using a plain-text command. AI re-generates values satisfying all existing
 * constraints PLUS the new teacher requirement.
 */
@Data
@Builder
public class UpdateParametersRequest {

  /**
   * Current parameter values, e.g. {"a": 2, "b": -3, "c": 1}.
   */
  private Map<String, Object> currentParameters;

  /**
   * Per-parameter plain-text constraint explanation returned by the previous
   * generate-parameters call, e.g. {"a": "a = 2, positive integer, non-zero"}.
   */
  private Map<String, String> currentConstraintText;

  /**
   * Free-text command from the teacher, e.g.:
   *   "nghiệm phải là số nguyên"
   *   "tăng độ khó, dùng số lớn hơn"
   *   "b phải là số dương"
   */
  private String teacherCommand;

  /** Template text with placeholders (needed for AI context). */
  private String templateText;

  /** Answer formula (needed for AI to recheck constraints). */
  private String answerFormula;
}
