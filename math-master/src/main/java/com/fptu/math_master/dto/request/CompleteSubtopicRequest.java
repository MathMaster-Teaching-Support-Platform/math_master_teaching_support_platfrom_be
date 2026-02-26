package com.fptu.math_master.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request to mark a subtopic as completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteSubtopicRequest {

  @NotNull(message = "Subtopic ID is required")
  @Schema(description = "ID of the roadmap subtopic", example = "550e8400-e29b-41d4-a716-446655440000")
  private java.util.UUID subtopicId;

  @Schema(description = "Time spent on this subtopic in minutes", example = "45")
  private Integer timeSpentMinutes;

  @Schema(description = "Score or performance rating (0-100)", example = "85")
  private java.math.BigDecimal performanceScore;

  @Schema(description = "Student feedback on the subtopic", example = "Understood the concept well")
  private String feedback;
}
