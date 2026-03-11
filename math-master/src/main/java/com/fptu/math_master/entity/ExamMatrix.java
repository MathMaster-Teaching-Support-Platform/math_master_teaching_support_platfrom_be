package com.fptu.math_master.entity;

import com.fptu.math_master.enums.MatrixStatus;
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
    name = "exam_matrices",
    indexes = {
      @Index(name = "idx_exam_matrices_teacher", columnList = "teacher_id"),
      @Index(name = "idx_exam_matrices_status", columnList = "status"),
      @Index(name = "idx_exam_matrices_is_reusable", columnList = "is_reusable")
    })
public class ExamMatrix {

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

  @Column(name = "is_reusable", nullable = false)
  private Boolean isReusable;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private MatrixStatus status;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixTemplateMapping> templateMappings;

  @PrePersist
  public void prePersist() {
    if (isReusable == null) isReusable = false;
    if (status == null) status = MatrixStatus.DRAFT;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
