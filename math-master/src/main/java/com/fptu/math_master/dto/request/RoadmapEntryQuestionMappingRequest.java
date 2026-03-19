package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapEntryQuestionMappingRequest {

  @NotNull(message = "Question ID is required")
  private UUID questionId;

  @NotNull(message = "Roadmap topic ID is required")
  private UUID roadmapTopicId;

  @Builder.Default
  private Integer orderIndex = 1;

  @Builder.Default
  private BigDecimal weight = BigDecimal.ONE;
}
