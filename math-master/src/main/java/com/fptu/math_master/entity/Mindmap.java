package com.fptu.math_master.entity;

import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;

import com.fptu.math_master.enums.MindmapStatus;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "mindmaps",
    indexes = {
      @Index(name = "idx_mindmaps_teacher", columnList = "teacher_id"),
      @Index(name = "idx_mindmaps_lesson", columnList = "lesson_id"),
      @Index(name = "idx_mindmaps_status", columnList = "status"),
      @Index(
          name = "idx_mindmaps_teacher_deleted_created",
          columnList = "teacher_id, deleted_at, created_at"),
      @Index(
          name = "idx_mindmaps_lesson_deleted_created",
          columnList = "lesson_id, deleted_at, created_at"),
      @Index(
          name = "idx_mindmaps_teacher_lesson_deleted_created",
          columnList = "teacher_id, lesson_id, deleted_at, created_at")
    })
/**
 * The entity of 'Mindmap'.
 */
public class Mindmap extends BaseEntity {

  /**
   * teacher_id
   */
  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * lesson_id
   */
  @Column(name = "lesson_id")
  private UUID lessonId;

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
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  /**
   * ai_generated
   */
  @Column(name = "ai_generated")
  private Boolean aiGenerated;

  /**
   * generation_prompt
   */
  @Nationalized
  @Column(name = "generation_prompt")
  private String generationPrompt;

  /**
   * status
   */
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private MindmapStatus status;

  /**
   * Relationships
   * - Many-to-One with User (teacher)
   * - Many-to-One with Lesson
   * - One-to-Many with MindmapNode
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private User teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Lesson lesson;

  @OneToMany(mappedBy = "mindmap", cascade = CascadeType.ALL, orphanRemoval = true)
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  private Set<MindmapNode> nodes;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (aiGenerated == null) aiGenerated = false;
    if (status == null) status = MindmapStatus.DRAFT;
  }
}
