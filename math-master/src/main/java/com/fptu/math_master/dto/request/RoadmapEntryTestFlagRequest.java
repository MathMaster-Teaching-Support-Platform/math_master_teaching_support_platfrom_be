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
public class RoadmapEntryTestFlagRequest {

  @NotNull(message = "Question ID is required")
  private UUID questionId;

  @NotNull(message = "Flag status is required")
  private Boolean flagged;
}
