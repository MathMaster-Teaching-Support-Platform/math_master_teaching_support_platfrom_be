package com.fptu.math_master.entity;

import com.fptu.math_master.enums.CurriculumCategory;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "curricula",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_curricula_name_grade_category",
          columnNames = {"name", "grade", "category"})
    },
    indexes = {
      @Index(name = "idx_curricula_grade", columnList = "grade"),
      @Index(name = "idx_curricula_category", columnList = "category"),
      @Index(name = "idx_curricula_grade_category", columnList = "grade, category"),
      @Index(name = "idx_curricula_subject", columnList = "subject_id")
    })
/**
 * The entity of 'Curriculum'.
 */
public class Curriculum extends BaseEntity {

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @Min(1)
  @Max(12)
  @Column(name = "grade", nullable = false)
  private Integer grade;

  @Column(name = "category", nullable = false)
  @Enumerated(EnumType.STRING)
  private CurriculumCategory category;

  /**
   * FK to the {@link Subject} entity — the structured subject (môn học) this
   * curriculum belongs to.  Supersedes the {@code category} enum for new data.
   * Nullable for backward-compat with rows created before Subject was introduced.
   */
  @Column(name = "subject_id")
  private UUID subjectId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", insertable = false, updatable = false)
  private Subject subject;

  /**
   * description
   */
  @Lob
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @OneToMany(mappedBy = "curriculum", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Chapter> chapters;
}
