package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "chapters",
  uniqueConstraints = {
    @UniqueConstraint(name = "uq_chapters_lesson_order", columnNames = {"lesson_id", "order_index"})
  },
  indexes = {
    @Index(name = "idx_chapters_lesson_id", columnList = "lesson_id"),
    @Index(name = "idx_chapters_order", columnList = "order_index")
  }
)
public class Chapter {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}

