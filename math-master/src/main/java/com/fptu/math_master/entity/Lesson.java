package com.fptu.math_master.entity;

import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
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
    name = "lessons",
    indexes = {
      @Index(name = "idx_lessons_teacher_id", columnList = "teacher_id"),
      @Index(name = "idx_lessons_status", columnList = "status"),
      @Index(name = "idx_lessons_subject", columnList = "subject"),
      @Index(name = "idx_lessons_grade_level", columnList = "grade_level"),
      @Index(name = "idx_lessons_teacher_status", columnList = "teacher_id, status")
    })
public class Lesson {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Size(max = 100)
  @Column(name = "subject", length = 100)
  private String subject;

  @Size(max = 50)
  @Column(name = "grade_level", length = 50)
  private String gradeLevel;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "difficulty")
  @Enumerated(EnumType.STRING)
  private LessonDifficulty difficulty;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private LessonStatus status;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Chapter> chapters;

  @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private LessonPlan lessonPlan;

  @PrePersist
  public void prePersist() {
    if (status == null) status = LessonStatus.DRAFT;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
