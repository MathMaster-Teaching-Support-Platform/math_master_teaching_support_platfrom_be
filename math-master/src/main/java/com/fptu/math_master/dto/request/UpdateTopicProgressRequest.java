package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request to update topic progress in a roadmap
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateTopicProgressRequest {

  @NotNull(message = "Topic ID is required")
  @Schema(description = "ID of the roadmap topic", example = "550e8400-e29b-41d4-a716-446655440000")
  private java.util.UUID topicId;

  @NotNull(message = "Status is required")
  @Schema(description = "Current status of the topic", example = "IN_PROGRESS")
  private TopicStatus status;

  @Min(value = 0, message = "Progress percentage must be >= 0")
  @Schema(description = "Progress percentage (0-100)", example = "50")
  private java.math.BigDecimal progressPercentage;

  @Schema(description = "Optional note about topic progress", example = "Completed first two practice sets")
  private String note;
}
