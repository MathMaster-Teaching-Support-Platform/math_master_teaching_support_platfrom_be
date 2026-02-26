package com.fptu.math_master.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Request to generate a roadmap based on student wishes using AI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GenerateRoadmapFromWishRequest {

  @NotBlank(message = "Subject is required")
  @Schema(description = "Subject name (e.g., 'Algebra', 'Geometry')", example = "Algebra")
  private String subject;

  @NotNull(message = "Wish ID is required")
  @Schema(description = "ID of the student wish to use for roadmap generation")
  private UUID wishId;

  @Schema(description = "Optional additional context or notes for AI planning")
  private String additionalContext;

  @Builder.Default
  @Schema(description = "Whether to use performance data in decision making", example = "true")
  private Boolean usePerformanceData = true;

  @Builder.Default
  @Schema(description = "Whether to include advanced material for strong areas", example = "false")
  private Boolean includeAdvancedMaterial = false;
}
