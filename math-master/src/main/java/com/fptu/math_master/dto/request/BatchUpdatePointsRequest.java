package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdatePointsRequest {

  @NotEmpty(message = "Danh sách câu hỏi không được để trống")
  @Valid
  private List<QuestionPointItem> questions;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QuestionPointItem {

    @NotNull(message = "ID câu hỏi không được để trống")
    private UUID id;

    @NotNull(message = "Điểm không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Điểm phải >= 0")
    private BigDecimal point;
  }
}
