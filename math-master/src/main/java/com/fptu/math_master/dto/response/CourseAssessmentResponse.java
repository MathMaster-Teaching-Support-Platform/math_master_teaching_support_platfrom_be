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
public class CourseAssessmentResponse {

  // CourseAssessment fields
  private UUID id;
  private UUID courseId;
  private UUID assessmentId;
  private Integer orderIndex;
  private boolean isRequired;
  private Instant createdAt;
  private Instant updatedAt;

  // Assessment details (denormalized for convenience)
  private String assessmentTitle;
  private String assessmentDescription;
  private AssessmentType assessmentType;
  private AssessmentStatus assessmentStatus;
  private Integer timeLimitMinutes;
  private BigDecimal passingScore;
  private Instant startDate;
  private Instant endDate;
  private Long totalQuestions;
  private BigDecimal totalPoints;
  private Long submissionCount;
}
