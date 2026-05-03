package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.AssessmentMode;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.AttemptScoringPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UpdateAssessmentRequest {

  @Size(max = 255, message = "Title must not exceed 255 characters")
  private String title;

  private String description;

  private AssessmentType assessmentType;

  @Min(value = 1, message = "Time limit must be greater than 0")
  private Integer timeLimitMinutes;

  @DecimalMin(value = "0.0", message = "Passing score must be at least 0")
  @DecimalMax(value = "100.0", message = "Passing score must not exceed 100")
  private BigDecimal passingScore;

  private Instant startDate;
  private Instant endDate;
  private Boolean randomizeQuestions;
  private Boolean showCorrectAnswers;
  private AssessmentMode assessmentMode;
  private UUID examMatrixId;
  private Boolean allowMultipleAttempts;
  private Integer maxAttempts;
  private AttemptScoringPolicy attemptScoringPolicy;
  private Boolean showScoreImmediately;
}
