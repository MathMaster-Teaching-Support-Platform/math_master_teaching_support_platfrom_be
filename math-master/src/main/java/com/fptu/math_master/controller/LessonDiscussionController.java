package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateLessonDiscussionCommentRequest;
import com.fptu.math_master.dto.request.UpdateLessonDiscussionCommentRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.LessonDiscussionCommentResponse;
import com.fptu.math_master.dto.response.LessonDiscussionLikeResponse;
import com.fptu.math_master.service.LessonDiscussionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses/{courseId}/lessons/{courseLessonId}/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonDiscussionController {

  LessonDiscussionService lessonDiscussionService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<Page<LessonDiscussionCommentResponse>> getRootComments(
      @PathVariable UUID courseId, @PathVariable UUID courseLessonId, Pageable pageable) {
    return ApiResponse.<Page<LessonDiscussionCommentResponse>>builder()
        .result(lessonDiscussionService.getRootComments(courseId, courseLessonId, pageable))
        .build();
  }

  @GetMapping("/{commentId}/replies")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<Page<LessonDiscussionCommentResponse>> getReplies(
      @PathVariable UUID courseId,
      @PathVariable UUID courseLessonId,
      @PathVariable UUID commentId,
      Pageable pageable) {
    return ApiResponse.<Page<LessonDiscussionCommentResponse>>builder()
        .result(lessonDiscussionService.getReplies(courseId, courseLessonId, commentId, pageable))
        .build();
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<LessonDiscussionCommentResponse> createComment(
      @PathVariable UUID courseId,
      @PathVariable UUID courseLessonId,
      @RequestBody @Valid CreateLessonDiscussionCommentRequest request) {
    return ApiResponse.<LessonDiscussionCommentResponse>builder()
        .result(lessonDiscussionService.createComment(courseId, courseLessonId, request))
        .build();
  }

  @PatchMapping("/{commentId}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<LessonDiscussionCommentResponse> updateComment(
      @PathVariable UUID courseId,
      @PathVariable UUID courseLessonId,
      @PathVariable UUID commentId,
      @RequestBody @Valid UpdateLessonDiscussionCommentRequest request) {
    return ApiResponse.<LessonDiscussionCommentResponse>builder()
        .result(lessonDiscussionService.updateComment(courseId, courseLessonId, commentId, request))
        .build();
  }

  @DeleteMapping("/{commentId}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<String> deleteComment(
      @PathVariable UUID courseId,
      @PathVariable UUID courseLessonId,
      @PathVariable UUID commentId) {
    lessonDiscussionService.deleteComment(courseId, courseLessonId, commentId);
    return ApiResponse.<String>builder().result("Deleted").build();
  }

  @PostMapping("/{commentId}/like-toggle")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<LessonDiscussionLikeResponse> toggleLike(
      @PathVariable UUID courseId,
      @PathVariable UUID courseLessonId,
      @PathVariable UUID commentId) {
    return ApiResponse.<LessonDiscussionLikeResponse>builder()
        .result(lessonDiscussionService.toggleLike(courseId, courseLessonId, commentId))
        .build();
  }
}
