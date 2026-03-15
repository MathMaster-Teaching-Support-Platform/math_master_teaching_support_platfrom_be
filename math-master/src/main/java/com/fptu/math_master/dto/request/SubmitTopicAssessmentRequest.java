package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitTopicAssessmentRequest {

  @NotNull(message = "Roadmap ID is required")
  private UUID roadmapId;

  @NotNull(message = "Topic ID is required")
  private UUID topicId;

  @NotNull(message = "Submission ID is required")
  private UUID submissionId;
}
