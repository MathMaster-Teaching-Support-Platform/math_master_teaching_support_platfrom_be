package com.fptu.math_master.entity;

import com.fptu.math_master.enums.MatrixStatus;
import com.fptu.math_master.util.UuidV7Generator;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(
    name = "exam_matrices",
    indexes = {
      @Index(name = "idx_exam_matrices_assessment", columnList = "assessment_id"),
      @Index(name = "idx_exam_matrices_lesson", columnList = "lesson_id"),
      @Index(name = "idx_exam_matrices_teacher", columnList = "teacher_id"),
      @Index(name = "idx_exam_matrices_status", columnList = "status")
    })
public class ExamMatrix {

  @Id
  @UuidV7Generator.UuidV7
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "assessment_id", nullable = false, unique = true)
  private UUID assessmentId;

  @Column(name = "lesson_id")
  private UUID lessonId;

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

  @Column(name = "total_questions", nullable = false)
  private Integer totalQuestions;

  @Column(name = "total_points", nullable = false, precision = 6, scale = 2)
  private BigDecimal totalPoints;

  @Column(name = "time_limit_minutes")
  private Integer timeLimitMinutes;

  @Type(JsonBinaryType.class)
  @Column(name = "matrix_config", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> matrixConfig;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private MatrixStatus status;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MatrixCell> matrixCells;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<PointDistribution> pointDistributions;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<MatrixUsageStats> matrixUsageStats;

  @PrePersist
  public void prePersist() {
    if (totalQuestions == null) totalQuestions = 0;
    if (totalPoints == null) totalPoints = BigDecimal.ZERO;
    if (status == null) status = MatrixStatus.DRAFT;
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }
}
