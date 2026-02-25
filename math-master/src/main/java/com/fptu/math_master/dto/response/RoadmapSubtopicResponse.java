package com.fptu.math_master.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fptu.math_master.enums.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response for a subtopic within a roadmap topic
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoadmapSubtopicResponse {

  @Schema(description = "Subtopic ID")
  private UUID id;

  @Schema(description = "Subtopic title")
  private String title;

  @Schema(description = "Subtopic description")
  private String description;

  @Schema(description = "Current status")
  private TopicStatus status;

  @Schema(description = "Order in sequence")
  private Integer sequenceOrder;

  @Schema(description = "Progress percentage")
  private BigDecimal progressPercentage;

  @Schema(description = "Estimated time in minutes")
  private Integer estimatedMinutes;

  @Schema(description = "When started")
  private Instant startedAt;

  @Schema(description = "When completed")
  private Instant completedAt;
}
