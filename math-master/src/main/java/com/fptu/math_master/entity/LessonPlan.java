package com.fptu.math_master.entity;

import com.fptu.math_master.util.UuidV7Generator;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
  name = "lesson_plans",
  indexes = {
    @Index(name = "idx_lesson_plans_lesson_id", columnList = "lesson_id"),
    @Index(name = "idx_lesson_plans_teacher_id", columnList = "teacher_id")
  }
)
public class LessonPlan {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "lesson_id", nullable = false, unique = true)
  private UUID lessonId;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Type(StringArrayType.class)
  @Column(name = "objectives", columnDefinition = "TEXT[]")
  private String[] objectives;

  @Type(StringArrayType.class)
  @Column(name = "materials_needed", columnDefinition = "TEXT[]")
  private String[] materialsNeeded;

  @Lob
  @Nationalized
  @Column(name = "teaching_strategy")
  private String teachingStrategy;

  @Lob
  @Nationalized
  @Column(name = "assessment_methods")
  private String assessmentMethods;

  @Lob
  @Nationalized
  @Column(name = "notes")
  private String notes;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

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

