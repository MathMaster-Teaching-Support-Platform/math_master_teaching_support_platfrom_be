package com.fptu.math_master.entity;

import com.fptu.math_master.enums.MindmapStatus;
import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "mindmaps",
    indexes = {
      @Index(name = "idx_mindmaps_teacher", columnList = "teacher_id"),
      @Index(name = "idx_mindmaps_lesson", columnList = "lesson_id"),
      @Index(name = "idx_mindmaps_status", columnList = "status")
    })
public class Mindmap {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "lesson_id")
  private UUID lessonId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Column(name = "ai_generated")
  private Boolean aiGenerated;

  @Lob
  @Nationalized
  @Column(name = "generation_prompt")
  private String generationPrompt;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private MindmapStatus status;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @OneToMany(mappedBy = "mindmap", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MindmapNode> nodes;

  @PrePersist
  public void prePersist() {
    if (aiGenerated == null) aiGenerated = false;
    if (status == null) status = MindmapStatus.DRAFT;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
