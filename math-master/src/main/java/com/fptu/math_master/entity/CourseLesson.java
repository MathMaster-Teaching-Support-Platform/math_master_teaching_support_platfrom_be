package com.fptu.math_master.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
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
import org.hibernate.annotations.Type;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
    name = "course_lessons",
    indexes = {
      @Index(name = "idx_course_lessons_course_id", columnList = "course_id"),
      @Index(name = "idx_course_lessons_lesson_id", columnList = "lesson_id")
    })
public class CourseLesson extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  /**
   * FK to Ministry {@link Lesson}. Required for MINISTRY courses.
   * NULL for CUSTOM courses (lesson is teacher-defined).
   */
  @Column(name = "lesson_id")
  private UUID lessonId;

  /**
   * FK to {@link CustomCourseSection}. Required for CUSTOM courses. NULL for MINISTRY.
   */
  @Column(name = "section_id")
  private UUID sectionId;

  /**
   * Teacher-defined title for CUSTOM-course lessons.
   * For MINISTRY courses, the title is fetched from the referenced {@link Lesson}.
   */
  @Column(name = "custom_title", length = 255)
  private String customTitle;

  @Column(name = "custom_description", columnDefinition = "TEXT")
  private String customDescription;

  @Column(name = "video_url")
  private String videoUrl;

  @Size(max = 255)
  @Nationalized
  @Column(name = "video_title", length = 255)
  private String videoTitle;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "order_index")
  private Integer orderIndex;

  @Column(name = "is_free_preview", nullable = false)
  @Builder.Default
  private boolean isFreePreview = false;

  @Type(JsonType.class)
  @Column(name = "materials", columnDefinition = "jsonb")
  private String materials;

  // ─── Relationships ────────────────────────────────────────────────────────

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lesson_id", insertable = false, updatable = false)
  private Lesson lesson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "section_id", insertable = false, updatable = false)
  private CustomCourseSection section;

  @OneToMany(mappedBy = "courseLesson", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<LessonProgress> lessonProgresses;
}
