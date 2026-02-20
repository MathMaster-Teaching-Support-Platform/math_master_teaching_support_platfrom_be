package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AIEnhancementRequest {

  /** The raw question text generated from template */
  String rawQuestionText;

  /** Question type (MCQ, TRUE_FALSE, SHORT_ANSWER, etc.) */
  QuestionType questionType;

  /** The correct answer */
  String correctAnswer;

  /** Generated options (for MCQ) */
  Map<String, String> rawOptions;

  /** Parameters used to generate the question */
  Map<String, Object> parameters;

  /** Answer formula used */
  String answerFormula;

  /** Calculated difficulty */
  QuestionDifficulty difficulty;

  /** Subject context (always "Math" for this system) */
  String subject = "Mathematics";

  /** Additional context for AI (chapter, topic, etc.) */
  String context;
}
