package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
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
public class MatrixCellResponse {

  private UUID id;
  private UUID matrixId;
  private UUID chapterId;
  private String chapterTitle;
  private String topic;
  private CognitiveLevel cognitiveLevel;
  private QuestionDifficulty difficulty;
  private QuestionType questionType;
  private Integer numQuestions;
  private BigDecimal pointsPerQuestion;
  private BigDecimal totalPoints;
  private Integer selectedQuestionCount;
  private String notes;
  private Instant createdAt;
  private Instant updatedAt;
}
