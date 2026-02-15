package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.SubmissionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingSubmissionResponse {

  private UUID submissionId;
  private UUID assessmentId;
  private String assessmentTitle;
  private UUID studentId;
  private String studentName;
  private String studentEmail;
  private SubmissionStatus status;
  private Instant submittedAt;
  private BigDecimal score;
  private BigDecimal maxScore;
  private BigDecimal percentage;
  private BigDecimal manualAdjustment;
  private String manualAdjustmentReason;
  private BigDecimal finalScore;
  private Integer timeSpentSeconds;
  private Integer attemptNumber;
  private Long pendingQuestionsCount;
  private Long autoGradedQuestionsCount;
  private List<AnswerGradeResponse> answers;
  private Boolean gradesReleased;
  private Instant gradedAt;
}
