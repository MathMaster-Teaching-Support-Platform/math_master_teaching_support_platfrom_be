package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.AssessmentMode;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.AttemptScoringPolicy;
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
public class AssessmentResponse {

  private UUID id;
  private UUID teacherId;
  private String teacherName;
  private List<UUID> lessonIds;
  private List<String> lessonTitles;
  private String title;
  private String description;
  private AssessmentType assessmentType;
  private Integer timeLimitMinutes;
  private BigDecimal passingScore;
  private Instant startDate;
  private Instant endDate;
  private Boolean randomizeQuestions;
  private Boolean showCorrectAnswers;
  private AssessmentMode assessmentMode;
  private UUID examMatrixId;
  private String examMatrixName;
  private Integer examMatrixGradeLevel;
  private Boolean allowMultipleAttempts;
  private Integer maxAttempts;
  private AttemptScoringPolicy attemptScoringPolicy;
  private Boolean showScoreImmediately;
  private AssessmentStatus status;
  private Long totalQuestions;
  private BigDecimal totalPoints;
  private Long submissionCount;
  private Instant createdAt;
  private Instant updatedAt;
}
