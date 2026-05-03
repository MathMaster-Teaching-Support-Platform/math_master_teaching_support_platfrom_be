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
  private String correctAnswer;
  private Boolean isCorrect;
  private BigDecimal pointsEarned;
  private BigDecimal maxPoints;
  private String feedback;
  private Boolean isManuallyAdjusted;
  private Instant gradedAt;
  private String explanation;
  private String solutionSteps;
  
  // Fields needed by UI to render correctly
  private com.fptu.math_master.enums.QuestionType questionType;
  private java.util.Map<String, Object> options;
  private Boolean needsManualGrading;
  private java.util.Map<String, Object> scoringDetail;
}
