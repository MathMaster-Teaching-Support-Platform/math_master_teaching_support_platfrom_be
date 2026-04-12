package com.fptu.math_master.entity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.MatrixStatus;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(
    name = "exam_matrices",
    indexes = {
      @Index(name = "idx_exam_matrices_teacher", columnList = "teacher_id"),
      @Index(name = "idx_exam_matrices_status", columnList = "status"),
      @Index(name = "idx_exam_matrices_is_reusable", columnList = "is_reusable"),
      @Index(name = "idx_exam_matrices_curriculum", columnList = "curriculum_id"),
      @Index(name = "idx_exam_matrices_grade", columnList = "grade_level")
    })
/**
 * The entity of 'ExamMatrix'.
 */
public class ExamMatrix extends BaseEntity {

  @Column(name = "teacher_id", nullable = false)
  private UUID teacherId;

  /**
   * Optional link to the {@link Curriculum} (chương trình) this matrix covers.
   * Set automatically when using the structured builder API.
   */
  @Column(name = "curriculum_id")
  private UUID curriculumId;

  /**
   * Cache of the target school-grade level (lớp) for this matrix (e.g. 10, 11, 12).
   * A matrix may reference content from multiple grades; this is the primary grade.
   */
  @Column(name = "grade_level")
  private Integer gradeLevel;

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", length = 255, nullable = false)
  private String name;

  /**
   * description
   */
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "is_reusable", nullable = false)
  private Boolean isReusable;

  @Column(name = "total_questions_target")
  private Integer totalQuestionsTarget;

  @Column(name = "total_points_target", precision = 6, scale = 2)
  private BigDecimal totalPointsTarget;

  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private MatrixStatus status;

  /**
   * Percentage distribution for cognitive levels (for percentage-based matrices).
   * Stored as JSON: {"NHAN_BIET": 25.0, "THONG_HIEU": 35.0, "VAN_DUNG": 30.0, "VAN_DUNG_CAO": 10.0}
   * Only used when matrix type is percentage-based.
   */
  @Type(JsonBinaryType.class)
  @Column(name = "cognitive_level_percentages", columnDefinition = "jsonb")
  private Map<CognitiveLevel, Double> cognitiveLevelPercentages;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
  private User teacher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "curriculum_id", insertable = false, updatable = false)
  private Curriculum curriculum;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixTemplateMapping> templateMappings;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixBankMapping> bankMappings;

  @OneToMany(mappedBy = "examMatrix", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<ExamMatrixRow> matrixRows;

  @PrePersist
  public void prePersist() {
    super.prePersist();
    if (isReusable == null) isReusable = false;
    if (status == null) status = MatrixStatus.DRAFT;
  }
}
