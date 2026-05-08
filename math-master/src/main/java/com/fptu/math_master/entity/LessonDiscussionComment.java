package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
    name = "lesson_discussion_comments",
    indexes = {
      @Index(name = "idx_lesson_discussion_comments_course_lesson", columnList = "course_lesson_id,created_at"),
      @Index(name = "idx_lesson_discussion_comments_parent", columnList = "parent_id,created_at")
    })
public class LessonDiscussionComment extends BaseEntity {

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "course_lesson_id", nullable = false)
  private UUID courseLessonId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "parent_id")
  private UUID parentId;

  @Column(name = "depth", nullable = false)
  @Builder.Default
  private Integer depth = 0;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "likes_count", nullable = false)
  @Builder.Default
  private Integer likesCount = 0;

  @Column(name = "reply_count", nullable = false)
  @Builder.Default
  private Integer replyCount = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;
}
