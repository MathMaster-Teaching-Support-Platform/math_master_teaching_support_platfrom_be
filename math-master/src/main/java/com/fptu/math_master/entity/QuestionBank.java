package com.fptu.math_master.entity;

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
    name = "question_banks",
    indexes = {
      @Index(name = "idx_question_banks_teacher", columnList = "teacher_id"),
      @Index(name = "idx_question_banks_curriculum", columnList = "curriculum_id"),
      @Index(name = "idx_question_banks_public", columnList = "is_public")
    })
public class QuestionBank {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Column(name = "curriculum_id")
  private UUID curriculumId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  @Lob
  @Nationalized
  @Column(name = "description")
  private String description;

  @Column(name = "is_public")
  private Boolean isPublic;

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
  @JoinColumn(name = "curriculum_id", insertable = false, updatable = false)
  private Curriculum curriculum;

  @OneToMany(mappedBy = "questionBank", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<QuestionTemplate> questionTemplates;

  @PrePersist
  public void prePersist() {
    if (isPublic == null) isPublic = false;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
