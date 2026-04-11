package com.fptu.math_master.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapResourceOptionResponse {

  @Schema(description = "Resource ID")
  private UUID id;

  @Schema(description = "Display name")
  private String name;

  @Schema(description = "Resource type")
  private String type;

  @Schema(description = "Related lesson ID when available")
  private UUID lessonId;

  @Schema(description = "Related chapter ID when available")
  private UUID chapterId;
}
