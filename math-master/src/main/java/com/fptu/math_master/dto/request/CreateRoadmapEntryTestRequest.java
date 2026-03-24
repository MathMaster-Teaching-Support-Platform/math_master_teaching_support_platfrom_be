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
public class CreateRoadmapEntryTestRequest {

  @NotNull(message = "Assessment ID is required")
  private UUID assessmentId;
}
