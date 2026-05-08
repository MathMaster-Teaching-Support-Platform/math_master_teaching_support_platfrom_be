package com.fptu.math_master.repository;

import com.fptu.math_master.entity.LessonDiscussionCommentLike;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonDiscussionCommentLikeRepository extends JpaRepository<LessonDiscussionCommentLike, UUID> {

  Optional<LessonDiscussionCommentLike> findByCommentIdAndUserIdAndDeletedAtIsNull(
      UUID commentId, UUID userId);

  long countByCommentIdAndDeletedAtIsNull(UUID commentId);

  List<LessonDiscussionCommentLike> findByCommentIdInAndUserIdAndDeletedAtIsNull(
      List<UUID> commentIds, UUID userId);
}
