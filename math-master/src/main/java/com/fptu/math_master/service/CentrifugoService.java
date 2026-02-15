package com.fptu.math_master.service;

import java.util.Map;
import java.util.UUID;

public interface CentrifugoService {

  String generateConnectionToken(UUID userId, UUID attemptId);

  void publishToChannel(String channel, Map<String, Object> data);

  void publishAnswerAck(UUID attemptId, UUID questionId, Long sequenceNumber);

  void publishFlagAck(UUID attemptId, UUID questionId, Boolean flagged);

  void publishSubmitted(UUID attemptId);

  String getAttemptChannel(UUID attemptId);
}
