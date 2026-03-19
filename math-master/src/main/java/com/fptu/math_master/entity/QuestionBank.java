package com.fptu.math_master.entity;

import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
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
    name = "question_banks",
    indexes = {
      @Index(name = "idx_question_banks_teacher", columnList = "teacher_id"),
      @Index(name = "idx_question_banks_curriculum", columnList = "curriculum_id"),
      @Index(name = "idx_question_banks_public", columnList = "is_public")
    })
/**
 * The entity of 'QuestionBank'.
 */
public class QuestionBank extends BaseEntity {

  /**
   * teacher_id
   */
  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * curriculum_id
   */
  @Column(name = "curriculum_id")
  private UUID curriculumId;

  /**
   * name
   */
  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * is_public
   */
  @Column(name = "is_public")
  private Boolean isPublic;

  /**
   * Relationships
   * - Many-to-One with User (teacher)
   * - Many-to-One with Curriculum
   * - One-to-Many with QuestionTemplate
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "curriculum_id", insertable = false, updatable = false)
  private Curriculum curriculum;

  @OneToMany(mappedBy = "questionBank", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<QuestionTemplate> questionTemplates;

  @PrePersist
  public void prePersist() {
    if (isPublic == null) isPublic = false;
  }
}
