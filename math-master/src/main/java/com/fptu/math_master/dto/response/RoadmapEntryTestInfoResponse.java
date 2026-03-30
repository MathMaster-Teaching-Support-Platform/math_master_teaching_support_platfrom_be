package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
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
public class RoadmapEntryTestInfoResponse {

  private UUID assessmentId;
  private String title;
  private String description;
  private Long totalQuestions;
  private BigDecimal totalPoints;
  private Integer timeLimitMinutes;
  private Instant startDate;
  private Instant endDate;

  private String studentStatus;
  private UUID activeAttemptId;
  private Integer attemptNumber;
  private Integer maxAttempts;
  private Boolean allowMultipleAttempts;
  private Boolean canStart;
  private String cannotStartReason;
}
