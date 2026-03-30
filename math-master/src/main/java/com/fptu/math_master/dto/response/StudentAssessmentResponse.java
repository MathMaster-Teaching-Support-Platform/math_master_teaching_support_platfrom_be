package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
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
public class StudentAssessmentResponse {

  private UUID id;
  private String title;
  private String description;
  private AssessmentType assessmentType;
  private Long totalQuestions;
  private BigDecimal totalPoints;
  private Integer timeLimitMinutes;
  private BigDecimal passingScore;
  private Instant dueDate;
  private Instant startDate;
  private Instant endDate;
  private AssessmentStatus status;

  private String studentStatus;
  private UUID currentAttemptId;
  private Integer attemptNumber;
  private Integer maxAttempts;
  private Boolean allowMultipleAttempts;
  private Boolean canStart;
  private String cannotStartReason;
}
