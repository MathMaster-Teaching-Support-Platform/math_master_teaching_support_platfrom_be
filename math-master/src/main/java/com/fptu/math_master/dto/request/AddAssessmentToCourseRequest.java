package com.fptu.math_master.dto.request;

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
public class AddAssessmentToCourseRequest {

  @NotNull(message = "Assessment ID is required")
  private UUID assessmentId;

  private Integer orderIndex;

  @Builder.Default
  private Boolean isRequired = true;
}
