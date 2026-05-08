package com.fptu.math_master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "lesson_discussion_comment_likes",
    indexes = {
      @Index(name = "idx_lesson_discussion_comment_likes_comment", columnList = "comment_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_lesson_discussion_comment_likes_comment_user",
          columnNames = {"comment_id", "user_id"})
    })
public class LessonDiscussionCommentLike extends BaseEntity {

  @Column(name = "comment_id", nullable = false)
  private UUID commentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id", insertable = false, updatable = false)
  private LessonDiscussionComment comment;
}
