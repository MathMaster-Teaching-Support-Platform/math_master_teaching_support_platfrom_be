package com.fptu.math_master.repository;

import com.fptu.math_master.entity.LessonDiscussionComment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonDiscussionCommentRepository extends JpaRepository<LessonDiscussionComment, UUID> {

  Optional<LessonDiscussionComment> findByIdAndDeletedAtIsNull(UUID id);

  Page<LessonDiscussionComment> findByCourseLessonIdAndParentIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(
      UUID courseLessonId, Pageable pageable);

  Page<LessonDiscussionComment> findByCourseLessonIdAndParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(
      UUID courseLessonId, UUID parentId, Pageable pageable);

  long countByParentIdAndDeletedAtIsNull(UUID parentId);

  List<LessonDiscussionComment> findByParentIdAndDeletedAtIsNull(UUID parentId);
}
