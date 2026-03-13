package com.fptu.math_master.entity;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.util.UuidV7Generator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Entity
@Table(
    name = "subjects",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_subjects_code", columnNames = {"code"})
    },
    indexes = {
      @Index(name = "idx_subjects_code", columnList = "code"),
      @Index(name = "idx_subjects_active", columnList = "is_active")
    })
public class Subject {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

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

  @Lob
  @Nationalized
  @Column(name = "description")
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

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  // ── Relationships ────────────────────────────────────────────────────────

  @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
  private Set<GradeSubject> gradeSubjects;

  @OneToMany(mappedBy = "subject", fetch = FetchType.LAZY)
  private Set<Curriculum> curricula;

  // ── Lifecycle hooks ──────────────────────────────────────────────────────

  @PrePersist
  public void prePersist() {
    if (isActive == null) isActive = true;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
