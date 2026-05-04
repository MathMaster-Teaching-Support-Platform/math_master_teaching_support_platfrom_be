package com.fptu.math_master.dto.response;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for Feature 2: AI-generated parameter values.
 * Returned by POST /api/question-templates/{id}/generate-parameters
 * and POST /api/question-templates/{id}/update-parameters.
 */
@Data
@Builder
public class GenerateParametersResponse {

  /**
   * The generated parameter values, e.g. {"a": 2, "b": -3, "c": 1}.
   * Values are numeric (Integer or Double).
   */
  private Map<String, Object> parameters;

  /**
   * Per-parameter plain-text explanation of why each value was chosen
   * and what constraints it satisfies. Displayed to teacher as plain text.
   * e.g. {
   *   "a": "a = 2, số nguyên dương, khác 0 để giữ bậc 2",
   *   "b": "b = -3, đảm bảo delta = 9 - 8 = 1 ≥ 0",
   *   "c": "c = 1, cho nghiệm thực x = 1 và x = 0.5"
   * }
   */
  private Map<String, String> constraintText;

  /**
   * Combined constraint statements explaining how all parameters work together.
   * e.g. ["b² - 4ac = 1 ≥ 0: phương trình có nghiệm thực",
   *        "Bộ {a:2, b:-3, c:1} chưa tồn tại trong hệ thống"]
   */
  private List<String> combinedConstraints;

  /**
   * The template text with parameters filled in (preview).
   * e.g. "Cho phương trình 2x² + (-3)x + 1 = 0..."
   */
  private String filledTextPreview;
}
