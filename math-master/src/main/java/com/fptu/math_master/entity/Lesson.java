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
      @Index(name = "idx_lessons_chapter_id", columnList = "chapter_id"),
      @Index(name = "idx_lessons_status", columnList = "status")
    })
public class Lesson {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "chapter_id", nullable = false)
  private UUID chapterId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Lob
  @Nationalized
  @Column(name = "learning_objectives")
  private String learningObjectives;

  @Lob
  @Nationalized
  @Column(name = "lesson_content", nullable = false)
  private String lessonContent;

  @Lob
  @Nationalized
  @Column(name = "summary")
  private String summary;

  @Column(name = "order_index")
  private Integer orderIndex;

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
  @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
  private Chapter chapter;

  @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<LessonPlan> lessonPlans;

  @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<QuestionTemplate> questionTemplates;

  @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentLesson> assessmentLessons;

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
