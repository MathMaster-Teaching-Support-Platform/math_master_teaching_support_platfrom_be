package com.fptu.math_master.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Quick statistics for a roadmap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoadmapStatsResponse {

  @Schema(description = "Total estimated hours for complete roadmap")
  private Integer totalEstimatedHours;

  @Schema(description = "Number of easy topics")
  private Long easyTopicsCount;

  @Schema(description = "Number of medium topics")
  private Long mediumTopicsCount;

  @Schema(description = "Number of hard topics")
  private Long hardTopicsCount;

  @Schema(description = "Average progress across all topics")
  private BigDecimal averageProgress;

  @Schema(description = "Number of locked topics")
  private Long lockedTopicsCount;

  @Schema(description = "Estimated days remaining to complete")
  private Integer daysRemaining;
}
