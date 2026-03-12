package com.fptu.math_master.entity;

import com.fptu.math_master.enums.AssessmentMode;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.AttemptScoringPolicy;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "assessments",
    indexes = {
      @Index(name = "idx_assessments_teacher", columnList = "teacher_id"),
      @Index(name = "idx_assessments_lesson", columnList = "lesson_id"),
      @Index(name = "idx_assessments_exam_matrix", columnList = "exam_matrix_id"),
      @Index(name = "idx_assessments_mode", columnList = "assessment_mode"),
      @Index(name = "idx_assessments_status", columnList = "status"),
      @Index(name = "idx_assessments_start_date", columnList = "start_date"),
      @Index(name = "idx_assessments_end_date", columnList = "end_date")
    })
public class Assessment {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Column(name = "assessment_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private AssessmentType assessmentType;

  @Column(name = "time_limit_minutes")
  private Integer timeLimitMinutes;

  @Column(name = "passing_score", precision = 5, scale = 2)
  private BigDecimal passingScore;

  @Column(name = "start_date")
  private Instant startDate;

  @Column(name = "end_date")
  private Instant endDate;

  @Column(name = "randomize_questions")
  private Boolean randomizeQuestions;

  @Column(name = "show_correct_answers")
  private Boolean showCorrectAnswers;

  @Column(name = "assessment_mode")
  @Enumerated(EnumType.STRING)
  private AssessmentMode assessmentMode;

  @Column(name = "exam_matrix_id")
  private UUID examMatrixId;

  @Column(name = "allow_multiple_attempts")
  private Boolean allowMultipleAttempts;

  @Column(name = "max_attempts")
  private Integer maxAttempts;

  @Column(name = "attempt_scoring_policy")
  @Enumerated(EnumType.STRING)
  private AttemptScoringPolicy attemptScoringPolicy;

  @Column(name = "show_score_immediately")
  private Boolean showScoreImmediately;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private AssessmentStatus status;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentQuestion> assessmentQuestions;

  @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Submission> submissions;

  @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<QuizAttempt> quizAttempts;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exam_matrix_id", insertable = false, updatable = false)
  private ExamMatrix examMatrix;

  @PrePersist
  public void prePersist() {
    if (randomizeQuestions == null) randomizeQuestions = false;
    if (showCorrectAnswers == null) showCorrectAnswers = false;
    if (allowMultipleAttempts == null) allowMultipleAttempts = false;
    if (showScoreImmediately == null) showScoreImmediately = true;
    if (assessmentMode == null) assessmentMode = AssessmentMode.DIRECT;
    if (status == null) status = AssessmentStatus.DRAFT;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
