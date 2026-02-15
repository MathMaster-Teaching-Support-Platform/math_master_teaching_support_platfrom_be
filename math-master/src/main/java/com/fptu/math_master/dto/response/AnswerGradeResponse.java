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
public class AnswerGradeResponse {

  private UUID answerId;
  private UUID questionId;
  private String questionText;
  private String answerText;
  private Boolean isCorrect;
  private BigDecimal pointsEarned;
  private BigDecimal maxPoints;
  private String feedback;
  private Boolean isManuallyAdjusted;
  private Instant gradedAt;
}
