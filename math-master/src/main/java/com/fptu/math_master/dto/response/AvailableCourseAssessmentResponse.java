package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
import java.math.BigDecimal;
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
public class AvailableCourseAssessmentResponse {

  private UUID assessmentId;
  private String title;
  private String description;
  private AssessmentType assessmentType;
  private AssessmentStatus status;
  private Integer timeLimitMinutes;
  private Long totalQuestions;
  private BigDecimal totalPoints;
  private Integer matchedLessonCount;
  private List<UUID> matchedLessonIds;
  private List<String> matchedLessonTitles;
}
