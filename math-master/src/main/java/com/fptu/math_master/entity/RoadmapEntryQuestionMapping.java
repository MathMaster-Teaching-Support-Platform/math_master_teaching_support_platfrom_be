package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "roadmap_entry_question_mappings",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_roadmap_entry_question",
          columnNames = {"roadmap_id", "question_id"})
    },
    indexes = {
      @Index(name = "idx_roadmap_entry_roadmap", columnList = "roadmap_id"),
      @Index(name = "idx_roadmap_entry_assessment", columnList = "assessment_id"),
      @Index(name = "idx_roadmap_entry_question", columnList = "question_id")
    })
public class RoadmapEntryQuestionMapping extends BaseEntity {

  @Column(name = "roadmap_id", nullable = false)
  private UUID roadmapId;

  @Column(name = "assessment_id", nullable = false)
  private UUID assessmentId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "order_index")
  private Integer orderIndex;

  @Column(name = "weight", precision = 8, scale = 4)
  private BigDecimal weight;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_id", insertable = false, updatable = false)
  private LearningRoadmap roadmap;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id", insertable = false, updatable = false)
  private Assessment assessment;
}
