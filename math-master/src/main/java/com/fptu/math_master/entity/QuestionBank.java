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
      @Index(name = "idx_question_banks_subject", columnList = "subject"),
      @Index(name = "idx_question_banks_public", columnList = "is_public")
    })
public class QuestionBank {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

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

  @OneToMany(mappedBy = "questionBank", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Question> questions;

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
