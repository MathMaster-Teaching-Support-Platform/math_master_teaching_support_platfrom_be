package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixRequest {

  @NotBlank(message = "Name is required")
  @Size(max = 255, message = "Name must not exceed 255 characters")
  private String name;

  private String description;

  private Boolean isReusable;

  @Positive(message = "Total questions target must be greater than 0")
  private Integer totalQuestionsTarget;

  @DecimalMin(value = "0.01", message = "Total points target must be greater than 0")
  private BigDecimal totalPointsTarget;

  /**
   * Optional bank mappings to create together with the matrix in one API call.
   */
  @Valid
  private List<AddBankMappingRequest> bankMappings;
}
