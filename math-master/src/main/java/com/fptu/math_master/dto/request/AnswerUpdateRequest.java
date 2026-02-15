package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class AnswerUpdateRequest {

  @NotNull(message = "Attempt ID is required")
  private UUID attemptId;

  @NotNull(message = "Question ID is required")
  private UUID questionId;

  private Object answerValue;

  private Instant clientTimestamp;

  private Long sequenceNumber;
}
