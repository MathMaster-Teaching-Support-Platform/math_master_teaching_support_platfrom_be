package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.QuestionType;
import java.math.BigDecimal;
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
public class AttemptQuestionResponse {

  private UUID questionId;
  private Integer orderIndex;
  private Integer partNumber;
  private QuestionType questionType;
  private String questionText;
  private String diagramData;
  private Map<String, Object> options;
  private BigDecimal points;
}
