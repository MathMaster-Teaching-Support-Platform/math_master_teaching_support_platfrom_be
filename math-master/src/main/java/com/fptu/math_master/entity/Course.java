package com.fptu.math_master.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "courses",
    indexes = {
      @Index(name = "idx_courses_teacher_id", columnList = "teacher_id"),
      @Index(name = "idx_courses_subject_id", columnList = "subject_id"),
      @Index(name = "idx_courses_school_grade_id", columnList = "school_grade_id"),
      @Index(name = "idx_courses_is_published", columnList = "is_published")
    })
public class Course extends BaseEntity {

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /** Subject (môn học) mà course này dạy, e.g. "Đại Số lớp 10" */
  @Column(name = "subject_id", nullable = false)
  private UUID subjectId;

  /** SchoolGrade để biết lớp mấy, cache lại cho tiện query/filter */
  @Column(name = "school_grade_id", nullable = false)
  private UUID schoolGradeId;

  @Size(max = 255)
  @Column(name = "title", length = 255, nullable = false, columnDefinition = "VARCHAR(255)")
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "thumbnail_url")
  private String thumbnailUrl;

  @Column(name = "is_published", nullable = false)
  @Builder.Default
  private boolean isPublished = false;

  @Column(name = "rating", precision = 3, scale = 2)
  @Builder.Default
  private BigDecimal rating = BigDecimal.ZERO;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", insertable = false, updatable = false)
  private Subject subject;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "school_grade_id", insertable = false, updatable = false)
  private SchoolGrade schoolGrade;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CourseLesson> courseLessons;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Enrollment> enrollments;
}
