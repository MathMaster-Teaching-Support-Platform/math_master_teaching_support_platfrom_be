package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-question grading result returned by the teacher preview-submit endpoint. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewAnswerResult {

  private UUID questionId;
  private int orderIndex;
  private String questionType;
  private String questionText;
  private Map<String, Object> options;
  private String explanation;
  private String[] tags;

  /** Echo of what the teacher submitted (string form). */
  private String studentAnswer;

  /** The stored correct answer string (key for MCQ, true-keys for TF, value for SA). */
  private String correctAnswer;

  /** Use boxed Boolean so Jackson serializes the field as "isCorrect" (Lombok generates getIsCorrect with boxed type). */
  private Boolean isCorrect;
  private BigDecimal pointsEarned;
  private BigDecimal maxPoints;

  /** TF clause-level breakdown when applicable; null otherwise. */
  private Map<String, Object> scoringDetail;
}
