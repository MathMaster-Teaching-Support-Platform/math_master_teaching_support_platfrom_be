package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
    name = "placement_question_mappings",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_placement_question_mappings",
          columnNames = {"placement_assessment_id", "question_id"})
    },
    indexes = {
      @Index(name = "idx_placement_mappings_assessment", columnList = "placement_assessment_id"),
      @Index(name = "idx_placement_mappings_question", columnList = "question_id"),
      @Index(name = "idx_placement_mappings_topic", columnList = "roadmap_topic_id"),
      @Index(name = "idx_placement_mappings_order", columnList = "order_index")
    })
public class PlacementQuestionMapping extends BaseEntity {

  @Column(name = "placement_assessment_id", nullable = false)
  private UUID placementAssessmentId;

  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @Column(name = "roadmap_topic_id", nullable = false)
  private UUID roadmapTopicId;

  @Column(name = "weight", precision = 5, scale = 2, nullable = false)
  private BigDecimal weight;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_assessment_id", insertable = false, updatable = false)
  private Assessment placementAssessment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id", insertable = false, updatable = false)
  private Question question;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_topic_id", insertable = false, updatable = false)
  private RoadmapTopic roadmapTopic;

  @PrePersist
  public void prePersist() {
    if (weight == null) weight = BigDecimal.ONE;
    if (orderIndex == null) orderIndex = 1;
  }
}
