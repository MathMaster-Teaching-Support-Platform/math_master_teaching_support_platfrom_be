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
public class AnswerAckResponse {

  private String type; // "ack" or "flag_ack"
  private UUID questionId;
  private Instant serverTimestamp;
  private Long sequenceNumber;
  private Boolean success;
  private String message;
}
