package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * Links learning materials (lessons, questions, resources) to roadmap topics
 *
 * <p>This entity acts as a junction between:
 * - RoadmapTopic
 * - Related resources (Questions, Lessons, etc.)
 *
 * <p>Tracks which materials students should study for each topic.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "topic_learning_materials",
    indexes = {
      @Index(name = "idx_materials_topic", columnList = "topic_id"),
      @Index(name = "idx_materials_lesson", columnList = "lesson_id"),
      @Index(name = "idx_materials_question", columnList = "question_id"),
      @Index(name = "idx_materials_sequence", columnList = "sequence_order")
    })
public class TopicLearningMaterial {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "topic_id", nullable = false)
  private UUID topicId;

  @Column(name = "lesson_id")
  private UUID lessonId; // Reference to learning lesson

  @Column(name = "question_id")
  private UUID questionId; // Reference to practice question

  @Column(name = "chapter_id")
  private UUID chapterId; // Reference to chapter for context

  @Size(max = 255)
  @Nationalized
  @Column(name = "resource_title", length = 255)
  private String resourceTitle;

  @Size(max = 50)
  @Column(name = "resource_type", length = 50)
  private String resourceType; // LESSON, QUESTION, EXAMPLE, PRACTICE, ASSESSMENT

  @Column(name = "sequence_order", nullable = false)
  private Integer sequenceOrder;

  @Column(name = "is_required", nullable = false)
  private Boolean isRequired = true; // Whether this material is mandatory

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

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
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
