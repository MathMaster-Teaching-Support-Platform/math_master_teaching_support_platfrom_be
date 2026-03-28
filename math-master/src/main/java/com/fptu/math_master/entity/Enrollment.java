package com.fptu.math_master.entity;

import com.fptu.math_master.enums.EnrollmentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_enrollments_student_course",
          columnNames = {"student_id", "course_id"})
    },
    indexes = {
      @Index(name = "idx_enrollments_student_id", columnList = "student_id"),
      @Index(name = "idx_enrollments_course_id", columnList = "course_id"),
      @Index(name = "idx_enrollments_student_course", columnList = "student_id, course_id")
    })
public class Enrollment extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  private EnrollmentStatus status;

  @Column(name = "enrolled_at", nullable = false)
  private Instant enrolledAt;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;

  @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<LessonProgress> lessonProgresses;
}
