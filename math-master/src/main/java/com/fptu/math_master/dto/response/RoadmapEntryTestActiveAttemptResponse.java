package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapEntryTestActiveAttemptResponse {

  private UUID assessmentId;
  private String studentStatus;
  private UUID attemptId;
  private Instant startedAt;
  private Instant expiresAt;
  private Integer timeRemainingSeconds;
  private RoadmapEntryTestProgressResponse progress;
}
