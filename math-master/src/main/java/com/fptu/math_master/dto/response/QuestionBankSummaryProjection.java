package com.fptu.math_master.dto.response;
import java.time.Instant;
import java.util.UUID;

public interface QuestionBankSummaryProjection {
  UUID getId();
  UUID getTeacherId();
  String getTeacherName();
  String getName();
  String getDescription();
  String getSubject();
  String getGradeLevel();
  Boolean getIsPublic();
  Long getQuestionCount();
  Instant getCreatedAt();
  Instant getUpdatedAt();
}
