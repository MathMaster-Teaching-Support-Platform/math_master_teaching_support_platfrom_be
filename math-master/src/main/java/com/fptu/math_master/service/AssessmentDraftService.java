package com.fptu.math_master.service;

import java.util.Map;
import java.util.UUID;

public interface AssessmentDraftService {

  void initDraft(UUID attemptId, UUID assessmentId, Integer timeLimitMinutes);

  void saveAnswer(UUID attemptId, UUID questionId, Object answerValue);

  void saveFlag(UUID attemptId, UUID questionId, Boolean flagged);

  Map<String, Object> getDraftSnapshot(UUID attemptId);

  void flushDraftToDatabase(UUID attemptId);

  void deleteDraft(UUID attemptId);

  Integer getAnsweredCount(UUID attemptId);
}
