package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * Represents a branch/subject of Mathematics (môn học).
 * <p>
 * Examples: Đại Số (Algebra), Hình Học (Geometry), Giải Tích (Calculus/Analysis),
 * Tổ Hợp - Xác Suất (Combinatorics & Probability).
 * <p>
 * A Subject can be taught across multiple grade levels and a Grade can cover
 * multiple Subjects — modelled via the {@link GradeSubject} join entity.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "subjects",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_subjects_code",
          columnNames = {"code"})
    },
    indexes = {
      @Index(name = "idx_subjects_code", columnList = "code"),
      @Index(name = "idx_subjects_active", columnList = "is_active"),
      @Index(name = "idx_subjects_school_grade", columnList = "school_grade_id")
    })
public class Subject extends BaseEntity {

  /**
   * Full Vietnamese name, e.g. "Đại Số", "Hình Học Giải Tích",
   * "Tổ Hợp - Xác Suất"
   */
  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  /**
   * Unique short-code used programmatically,
   * e.g. "DAI_SO", "HINH_HOC", "GIAI_TICH", "TO_HOP_XAC_SUAT"
   */
  @Size(max = 50)
  @Column(name = "code", length = 50, nullable = false)
  private String code;

  /**
   * description
   */
  @Lob
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /** Lowest grade level this subject appears in (1–12). Null = unrestricted. */
  @Min(1)
  @Max(12)
  @Column(name = "grade_min")
  private Integer gradeMin;

  /** Highest grade level this subject appears in (1–12). Null = unrestricted. */
  @Min(1)
  @Max(12)
  @Column(name = "grade_max")
  private Integer gradeMax;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  /** Direct ERD relationship: each Subject belongs to one SchoolGrade (lop). */
  @Column(name = "school_grade_id")
  private UUID schoolGradeId;

  // ── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "school_grade_id", insertable = false, updatable = false)
  private SchoolGrade schoolGrade;

  @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
  private Set<GradeSubject> gradeSubjects;

  @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
  private Set<Chapter> chapters;

  @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
  private Set<Curriculum> curricula;

  // ── Lifecycle hooks ──────────────────────────────────────────────────────

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (isActive == null) isActive = true;
  }
}
