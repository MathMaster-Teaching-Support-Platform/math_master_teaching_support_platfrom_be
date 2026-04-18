package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for batch topic operations (create/update/delete multiple topics in one transaction).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTopicRequest {

  @NotNull(message = "Roadmap ID is required")
  private UUID roadmapId;

  @NotEmpty(message = "Topics list cannot be empty")
  @Valid
  private List<TopicBatchItem> topics;
}
