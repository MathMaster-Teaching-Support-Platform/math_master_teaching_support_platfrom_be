package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "roadmap_feedbacks",
    indexes = {
      @Index(name = "idx_roadmap_feedbacks_roadmap", columnList = "roadmap_id"),
      @Index(name = "idx_roadmap_feedbacks_student", columnList = "student_id"),
      @Index(name = "idx_roadmap_feedbacks_rating", columnList = "rating"),
      @Index(name = "idx_roadmap_feedbacks_created_at", columnList = "created_at")
    })
public class RoadmapFeedback extends BaseEntity {

  @Column(name = "roadmap_id", nullable = false)
  private UUID roadmapId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Min(1)
  @Max(5)
  @Column(name = "rating", nullable = false)
  private Integer rating;

  @Size(max = 2000)
  @Nationalized
  @Column(name = "content", length = 2000)
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "roadmap_id", insertable = false, updatable = false)
  private LearningRoadmap roadmap;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id", insertable = false, updatable = false)
  private User student;
}
