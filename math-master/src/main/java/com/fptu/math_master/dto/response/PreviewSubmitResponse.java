package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aggregated response from the teacher preview-submit endpoint. Stateless — nothing persisted. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewSubmitResponse {

  private BigDecimal totalScore;
  private BigDecimal maxScore;

  private int totalQuestions;
  private int correctCount;

  private List<PreviewAnswerResult> answers;
}
