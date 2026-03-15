package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * The entity of 'TopicLearningMaterial'.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "topic_learning_materials",
    indexes = {
      @Index(name = "idx_materials_topic", columnList = "topic_id"),
      @Index(name = "idx_materials_lesson", columnList = "lesson_id"),
      @Index(name = "idx_materials_question", columnList = "question_id"),
      @Index(name = "idx_materials_sequence", columnList = "sequence_order")
    })
public class TopicLearningMaterial extends BaseEntity {

  @Column(name = "topic_id", nullable = false)
  private UUID topicId;

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Column(name = "question_id")
  private UUID questionId;

  @Column(name = "chapter_id")
  private UUID chapterId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "resource_title", length = 255)
  private String resourceTitle;

  /**
   * resource_type
   */
  @Size(max = 50)
  @Column(name = "resource_type", length = 50)
  private String resourceType;

  /**
   * sequence_order
   */
  @Column(name = "sequence_order", nullable = false)
  private Integer sequenceOrder;

  /**
   * is_required
   */
  @Builder.Default
  @Column(name = "is_required", nullable = false)
  private Boolean isRequired = true;

  /**
   * Relationships
   * - Many-to-One with RoadmapTopic
   * - Many-to-One with Lesson
   * - Many-to-One with Question
   * - Many-to-One with Chapter
   */
  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "topic_id", insertable = false, updatable = false)
  private RoadmapTopic topic;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
  private Chapter chapter;

  @PrePersist
  public void prePersist() {
    if (isRequired == null) isRequired = true;
  }
}
