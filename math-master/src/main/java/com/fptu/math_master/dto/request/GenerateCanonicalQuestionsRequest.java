package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateCanonicalQuestionsRequest {

  @NotNull(message = "templateId is required")
  private UUID templateId;

  @NotNull(message = "count is required")
  @Min(value = 1, message = "count must be at least 1")
  private Integer count;
}
