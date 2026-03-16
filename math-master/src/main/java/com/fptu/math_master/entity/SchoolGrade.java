package com.fptu.math_master.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
    name = "school_grades",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_school_grades_level",
          columnNames = {"grade_level"})
    },
    indexes = {
      @Index(name = "idx_school_grades_level", columnList = "grade_level"),
      @Index(name = "idx_school_grades_active", columnList = "is_active")
    })
public class SchoolGrade extends BaseEntity {

  @Min(1)
  @Max(12)
  @Column(name = "grade_level", nullable = false)
  private Integer gradeLevel;

  @Size(max = 100)
  @Nationalized
  @Column(name = "name", length = 100, nullable = false)
  private String name;

  /**
   * description
   */
  @Lob
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;
}
