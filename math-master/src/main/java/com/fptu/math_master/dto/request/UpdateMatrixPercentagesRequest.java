package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.CognitiveLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to update exam matrix with percentage-based configuration.
 * Used when teacher wants to set total questions and cognitive level percentages
 * for a matrix that has question banks but no fixed question counts.
 * 
 * <p>Example usage:
 * <pre>
 * {
 *   "totalQuestionsTarget": 40,
 *   "cognitiveLevelPercentages": {
 *     "NHAN_BIET": 25.0,
 *     "THONG_HIEU": 35.0,
 *     "VAN_DUNG": 30.0,
 *     "VAN_DUNG_CAO": 10.0
 *   }
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMatrixPercentagesRequest {

  @NotNull(message = "Total questions target is required")
  @Min(value = 1, message = "Total questions must be at least 1")
  @Max(value = 200, message = "Total questions cannot exceed 200")
  private Integer totalQuestionsTarget;

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
}
