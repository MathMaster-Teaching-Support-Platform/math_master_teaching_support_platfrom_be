package com.fptu.math_master.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * A teacher-defined section (chapter) used by CUSTOM (Udemy-style) courses.
 * Ministry-backed courses use Ministry {@link Chapter} entities instead.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "custom_course_sections",
    indexes = {
      @Index(name = "idx_custom_sections_course_id", columnList = "course_id"),
      @Index(name = "idx_custom_sections_order", columnList = "order_index"),
      @Index(name = "idx_custom_sections_deleted_at", columnList = "deleted_at")
    })
public class CustomCourseSection extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Size(max = 255)
  @Nationalized
  @Column(name = "title", length = 255, nullable = false)
  private String title;

  @Nationalized
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "order_index", nullable = false)
  private Integer orderIndex;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;

  @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<CourseLesson> courseLessons;
}
