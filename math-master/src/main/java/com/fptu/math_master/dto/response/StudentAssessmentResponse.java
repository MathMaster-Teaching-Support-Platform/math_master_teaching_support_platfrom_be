package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
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

  // Course context metadata (used by course-scoped student assessment listing)
  private Boolean isRequired;
  private Integer courseOrderIndex;

  /** Khóa học liên kết (một trong các khóa học học sinh đang học có gắn đề này). */
  private UUID courseId;
  /** CT: khối / lớp SGK (từ khóa học liên kết). */
  private UUID schoolGradeId;
  /** CT: môn học (từ khóa học liên kết). */
  private UUID subjectId;
  /** Bài học gắn với đề (assessment_lessons), phục vụ lọc Chương / Bài trên FE. */
  private List<UUID> lessonIds;
}
