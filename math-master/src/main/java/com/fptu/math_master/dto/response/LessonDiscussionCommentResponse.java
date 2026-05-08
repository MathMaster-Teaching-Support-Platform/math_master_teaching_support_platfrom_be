package com.fptu.math_master.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDiscussionCommentResponse {
  private UUID id;
  private UUID courseId;
  private UUID courseLessonId;
  private UUID parentId;
  private Integer depth;
  private String content;
  private Integer likesCount;
  private Integer replyCount;
  private Boolean likedByCurrentUser;
  private Boolean deleted;
  private UUID authorId;
  private String authorName;
  private String authorAvatar;
  private String authorRole;
  private Boolean canEdit;
  private Boolean canDelete;
  private Instant createdAt;
  private Instant updatedAt;
}
