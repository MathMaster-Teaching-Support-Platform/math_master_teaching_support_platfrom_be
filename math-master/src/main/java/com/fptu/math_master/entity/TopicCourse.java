package com.fptu.math_master.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Join table for many-to-many relationship between RoadmapTopic and Course.
 * Allows a topic to link to multiple courses.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
    name = "topic_courses",
    indexes = {
      @Index(name = "idx_topic_courses_topic", columnList = "topic_id"),
      @Index(name = "idx_topic_courses_course", columnList = "course_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(name = "uk_topic_course", columnNames = {"topic_id", "course_id"})
    })
public class TopicCourse extends BaseEntity {

  @EqualsAndHashCode.Include
  private UUID entityIdForEquality() {
    return getId();
  }

  @Column(name = "topic_id", nullable = false)
  private UUID topicId;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "topic_id", insertable = false, updatable = false)
  private RoadmapTopic topic;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "course_id", insertable = false, updatable = false)
  private Course course;
}
