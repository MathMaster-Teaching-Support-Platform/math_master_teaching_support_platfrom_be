package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "grades",
  indexes = {
    @Index(name = "idx_grades_student", columnList = "student_id"),
    @Index(name = "idx_grades_lesson", columnList = "lesson_id"),
    @Index(name = "idx_grades_teacher", columnList = "teacher_id"),
    @Index(name = "idx_grades_student_lesson", columnList = "student_id, lesson_id")
  }
)
public class Grade {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "assessment_id")
  private UUID assessmentId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "score", nullable = false, precision = 5, scale = 2)
  private BigDecimal score;

  @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
  private BigDecimal maxScore;

  @Column(name = "percentage", precision = 5, scale = 2, insertable = false, updatable = false)
  private BigDecimal percentage;

  @Column(name = "grade_letter", length = 5)
  private String gradeLetter;

  @Lob
  @Nationalized
  @Column(name = "feedback")
  private String feedback;

  @Column(name = "graded_at")
  private Instant gradedAt;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @PrePersist
  public void prePersist() {
    if (gradedAt == null) gradedAt = Instant.now();
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}

