package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.StudentRoadmapProgressStatus;
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
public class StudentRoadmapProgressResponse {

  private UUID roadmapId;
  private UUID studentId;
  private UUID currentTopicId;
  private UUID suggestedStartTopicId;
  private UUID placementSubmissionId;
  private StudentRoadmapProgressStatus status;
  private Instant startedAt;
  private Instant completedAt;
}
