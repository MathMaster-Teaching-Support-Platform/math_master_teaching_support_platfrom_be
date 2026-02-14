package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptStartResponse {

    private UUID attemptId;
    private UUID assessmentId;
    private Integer attemptNumber;
    private Instant startedAt;
    private Instant expiresAt;
    private Integer timeLimitMinutes;
    private Long totalQuestions;
    private String instructions;
    private String connectionToken;
    private String channelName;
    private List<AttemptQuestionResponse> questions;
}

