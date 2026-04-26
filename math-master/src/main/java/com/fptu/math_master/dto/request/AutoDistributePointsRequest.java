package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /assessments/{id}/auto-distribute.
 * <p>
 * {@code distribution} maps cognitive-level name (e.g. "NHAN_BIET", "THONG_HIEU") to the
 * percentage (0-100) of {@code totalPoints} to allocate to questions at that level.
 * Percentages should sum to 100; remaining questions (unmatched level) receive proportional share.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoDistributePointsRequest {

  @NotNull(message = "Tổng điểm không được để trống")
  @DecimalMin(value = "0.01", message = "Tổng điểm phải lớn hơn 0")
  private BigDecimal totalPoints;

  /**
   * Map of cognitiveLevel.name() → percentage (integer 0-100). E.g. {"NHAN_BIET": 30,
   * "THONG_HIEU": 40, "VAN_DUNG": 30}
   */
  private Map<String, Integer> distribution;
}
