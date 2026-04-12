package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to generate assessment from exam matrix using percentage-based cognitive level distribution.
 * 
 * <p>Example usage:
 * <pre>
 * {
 *   "examMatrixId": "uuid",
 *   "totalQuestions": 40,
 *   "cognitiveLevelPercentages": {
 *     "NHAN_BIET": 25.0,      // 25% = 10 questions
 *     "THONG_HIEU": 35.0,     // 35% = 14 questions
 *     "VAN_DUNG": 30.0,       // 30% = 12 questions
 *     "VAN_DUNG_CAO": 10.0    // 10% = 4 questions
 *   },
 *   "assessmentTitle": "Kiểm tra giữa kỳ",
 *   "assessmentDescription": "Đề thi toán lớp 10",
 *   "randomizeQuestions": true
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAssessmentByPercentageRequest {

  @NotNull(message = "Exam matrix ID is required")
  private UUID examMatrixId;

  @NotNull(message = "Total questions is required")
  @Min(value = 1, message = "Total questions must be at least 1")
  @Max(value = 200, message = "Total questions cannot exceed 200")
  private Integer totalQuestions;

  /**
   * Percentage distribution for each cognitive level.
   * Keys: CognitiveLevel enum values (NHAN_BIET, THONG_HIEU, VAN_DUNG, VAN_DUNG_CAO)
   * Values: Percentage (0-100), total must equal 100
   * 
   * <p>Example:
   * <pre>
   * {
   *   "NHAN_BIET": 25.0,
   *   "THONG_HIEU": 35.0,
   *   "VAN_DUNG": 30.0,
   *   "VAN_DUNG_CAO": 10.0
   * }
   * </pre>
   */
  @NotNull(message = "Cognitive level percentages are required")
  private Map<CognitiveLevel, Double> cognitiveLevelPercentages;

  /**
   * Optional custom title for the assessment.
   * If not provided, will use matrix name.
   */
  private String assessmentTitle;

  /**
   * Optional description for the assessment.
   */
  private String assessmentDescription;

  /**
   * Whether to randomize question order for students.
   * Default: false
   */
  @Builder.Default
  private Boolean randomizeQuestions = false;

  /**
   * Whether to reuse previously approved questions from the same matrix.
   * Default: true
   */
  @Builder.Default
  private Boolean reuseApprovedQuestions = true;

  /**
   * Time limit in minutes (optional).
   */
  private Integer timeLimitMinutes;

  /**
   * Passing score percentage (0-100, optional).
   */
  @Min(value = 0, message = "Passing score must be at least 0")
  @Max(value = 100, message = "Passing score cannot exceed 100")
  private Integer passingScore;
}
