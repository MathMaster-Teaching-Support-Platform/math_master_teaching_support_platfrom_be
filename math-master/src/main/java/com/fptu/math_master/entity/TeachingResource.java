package com.fptu.math_master.entity;

import com.fptu.math_master.enums.TeachingResourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.Set;
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
    name = "teaching_resources",
    indexes = {
      @Index(name = "idx_teaching_resources_type", columnList = "type"),
      @Index(name = "idx_teaching_resources_created_by", columnList = "created_by"),
      @Index(name = "idx_teaching_resources_created_at", columnList = "created_at")
    })
public class TeachingResource extends BaseEntity {

  @Size(max = 255)
  @Nationalized
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private TeachingResourceType type;

  @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
  private String fileUrl;

  @ManyToMany(mappedBy = "teachingResources", fetch = FetchType.LAZY)
  private Set<RoadmapTopic> roadmapTopics;
}
