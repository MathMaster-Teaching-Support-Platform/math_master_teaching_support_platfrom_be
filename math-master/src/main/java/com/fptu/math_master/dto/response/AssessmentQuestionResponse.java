package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.QuestionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentQuestionResponse {

  private UUID questionId;
  private Integer orderIndex;
  private BigDecimal points;
  private BigDecimal pointsOverride;
  private QuestionType questionType;
  private String questionText;
  private Map<String, Object> options;
  private String correctAnswer;
  private String explanation;
  private Instant createdAt;
}
