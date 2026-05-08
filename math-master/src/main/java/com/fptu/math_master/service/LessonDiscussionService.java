package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateLessonDiscussionCommentRequest;
import com.fptu.math_master.dto.request.UpdateLessonDiscussionCommentRequest;
import com.fptu.math_master.dto.response.LessonDiscussionCommentResponse;
import com.fptu.math_master.dto.response.LessonDiscussionLikeResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LessonDiscussionService {

  Page<LessonDiscussionCommentResponse> getRootComments(UUID courseId, UUID courseLessonId, Pageable pageable);

  Page<LessonDiscussionCommentResponse> getReplies(
      UUID courseId, UUID courseLessonId, UUID parentCommentId, Pageable pageable);

  LessonDiscussionCommentResponse createComment(
      UUID courseId, UUID courseLessonId, CreateLessonDiscussionCommentRequest request);

  LessonDiscussionCommentResponse updateComment(
      UUID courseId,
      UUID courseLessonId,
      UUID commentId,
      UpdateLessonDiscussionCommentRequest request);

  void deleteComment(UUID courseId, UUID courseLessonId, UUID commentId);

  LessonDiscussionLikeResponse toggleLike(UUID courseId, UUID courseLessonId, UUID commentId);
}
