package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapEntryTestSnapshotResponse {

  private UUID attemptId;
  private Map<UUID, Object> answers;
  private Map<UUID, Boolean> flags;
  private Instant startedAt;
  private Instant expiresAt;
  private Integer timeRemainingSeconds;
  private RoadmapEntryTestProgressResponse progress;
}
