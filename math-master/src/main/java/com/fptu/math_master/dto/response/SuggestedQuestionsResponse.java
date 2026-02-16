package com.fptu.math_master.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedQuestionsResponse {

  private List<QuestionResponse> suggestedQuestions;
  private Integer totalAvailable;
  private Integer requested;
  private Integer returned;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QuestionResponse {
    private java.util.UUID id;
    private String questionText;
    private com.fptu.math_master.enums.QuestionType questionType;
    private com.fptu.math_master.enums.QuestionDifficulty difficulty;
    private String[] bloomTaxonomyTags;
    private java.math.BigDecimal points;
  }
}
