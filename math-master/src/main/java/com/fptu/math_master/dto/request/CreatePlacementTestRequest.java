package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlacementTestRequest {

  @NotNull(message = "Roadmap ID is required")
  private UUID roadmapId;

  @NotNull(message = "Placement assessment ID is required")
  private UUID placementAssessmentId;

  @Valid
  @NotEmpty(message = "At least one question mapping is required")
  private List<PlacementQuestionMappingRequest> mappings;
}
