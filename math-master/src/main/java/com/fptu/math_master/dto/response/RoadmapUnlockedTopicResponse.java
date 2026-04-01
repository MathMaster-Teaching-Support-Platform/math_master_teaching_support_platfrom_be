package com.fptu.math_master.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapUnlockedTopicResponse {

  private UUID id;
  private String name;
  private Double requiredPoint;
}
