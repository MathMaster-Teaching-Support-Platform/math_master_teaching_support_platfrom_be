package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.RoadmapStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAdminRoadmapRequest {

  private String subject;

  private String gradeLevel;

  private String description;

  private Integer estimatedCompletionDays;

  private RoadmapStatus status;
}
