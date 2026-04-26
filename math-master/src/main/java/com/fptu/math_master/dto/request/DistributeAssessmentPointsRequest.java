package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributeAssessmentPointsRequest {

  public enum Strategy {
    EQUAL
  }

  @NotNull(message = "Tổng điểm không được để trống")
  @DecimalMin(value = "0.0", inclusive = true, message = "Tổng điểm phải >= 0")
  private BigDecimal totalPoints;

  @NotNull(message = "Chiến lược phân bổ không được để trống")
  private Strategy strategy = Strategy.EQUAL;

  @Min(value = 0, message = "Số chữ số thập phân phải >= 0")
  @Max(value = 6, message = "Số chữ số thập phân phải <= 6")
  private Integer scale = 2;
}
