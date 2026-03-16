package com.fptu.math_master.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "chapters",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_chapters_subject_order",
          columnNames = {"subject_id", "order_index"})
    },
    indexes = {
      @Index(name = "idx_chapters_curriculum_id", columnList = "curriculum_id"),
      @Index(name = "idx_chapters_subject_id", columnList = "subject_id"),
      @Index(name = "idx_chapters_order", columnList = "order_index")
    })
/**
 * The entity of 'Chapter'.
 */
public class Chapter extends BaseEntity {

  /**
   * curriculum_id
   */
  @Column(name = "curriculum_id")
  private UUID curriculumId;

  /** Direct ERD relationship: each Chapter belongs to one Subject. */
  @Column(name = "subject_id")
  private UUID subjectId;

  /**
   * title
   */
  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  /**
   * description
   */
  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  /**
   * order_index
   */
  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  /**
   * Relationships
   * - Many-to-One with Curriculum
   * - One-to-Many with Lesson
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "curriculum_id", insertable = false, updatable = false)
  private Curriculum curriculum;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_id", insertable = false, updatable = false)
  private Subject subject;

  @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Lesson> lessons;
}
