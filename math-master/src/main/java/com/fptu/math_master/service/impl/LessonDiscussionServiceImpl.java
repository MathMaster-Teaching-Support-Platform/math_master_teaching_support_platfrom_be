package com.fptu.math_master.service.impl;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.request.CreateLessonDiscussionCommentRequest;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.request.UpdateLessonDiscussionCommentRequest;
import com.fptu.math_master.dto.response.LessonDiscussionCommentResponse;
import com.fptu.math_master.dto.response.LessonDiscussionLikeResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseLesson;
import com.fptu.math_master.entity.LessonDiscussionComment;
import com.fptu.math_master.entity.LessonDiscussionCommentLike;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonDiscussionCommentLikeRepository;
import com.fptu.math_master.repository.LessonDiscussionCommentRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.LessonDiscussionService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class LessonDiscussionServiceImpl implements LessonDiscussionService {

  static final int MAX_REPLY_DEPTH = 2;
  static final String ROLE_TEACHER = "TEACHER";
  static final String ROLE_STUDENT = "STUDENT";
  static final String ROLE_ADMIN = "ADMIN";
  static final String LESSON_TAB_SUFFIX = "?tab=lessons";

  CourseRepository courseRepository;
  CourseLessonRepository courseLessonRepository;
  EnrollmentRepository enrollmentRepository;
  LessonDiscussionCommentRepository commentRepository;
  LessonDiscussionCommentLikeRepository likeRepository;
  UserRepository userRepository;
  UploadService uploadService;
  StreamPublisher streamPublisher;

  @Override
  @Transactional(readOnly = true)
  public Page<LessonDiscussionCommentResponse> getRootComments(
      UUID courseId, UUID courseLessonId, Pageable pageable) {
    AccessContext access = requireMemberAccess(courseId, courseLessonId);
    Page<LessonDiscussionComment> page =
        commentRepository.findByCourseLessonIdAndParentIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(
            courseLessonId, pageable);
    Set<UUID> likedIds = findLikedCommentIds(access.currentUserId(), page.getContent());
    return page.map(comment -> mapToResponse(comment, access, likedIds.contains(comment.getId())));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<LessonDiscussionCommentResponse> getReplies(
      UUID courseId, UUID courseLessonId, UUID parentCommentId, Pageable pageable) {
    AccessContext access = requireMemberAccess(courseId, courseLessonId);
    LessonDiscussionComment parent = findCommentOrThrow(parentCommentId);
    ensureCommentBelongsToLesson(parent, courseId, courseLessonId);
    Page<LessonDiscussionComment> page =
        commentRepository.findByCourseLessonIdAndParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            courseLessonId, parentCommentId, pageable);
    Set<UUID> likedIds = findLikedCommentIds(access.currentUserId(), page.getContent());
    return page.map(comment -> mapToResponse(comment, access, likedIds.contains(comment.getId())));
  }

  @Override
  public LessonDiscussionCommentResponse createComment(
      UUID courseId, UUID courseLessonId, CreateLessonDiscussionCommentRequest request) {
    AccessContext access = requireMemberAccess(courseId, courseLessonId);
    String content = normalizeContent(request.getContent());

    LessonDiscussionComment parent = null;
    int depth = 0;
    if (request.getParentId() != null) {
      parent = findCommentOrThrow(request.getParentId());
      ensureCommentBelongsToLesson(parent, courseId, courseLessonId);
      if (parent.getDepth() >= MAX_REPLY_DEPTH) {
        throw new AppException(
            ErrorCode.INVALID_REQUEST, "Reply depth exceeds maximum level " + MAX_REPLY_DEPTH);
      }
      depth = parent.getDepth() + 1;
    }

    LessonDiscussionComment comment =
        LessonDiscussionComment.builder()
            .courseId(courseId)
            .courseLessonId(courseLessonId)
            .userId(access.currentUserId())
            .parentId(parent != null ? parent.getId() : null)
            .depth(depth)
            .content(content)
            .build();
    comment = commentRepository.save(comment);

    if (parent != null) {
      parent.setReplyCount((parent.getReplyCount() == null ? 0 : parent.getReplyCount()) + 1);
      commentRepository.save(parent);
    }

    notifyOnCommentActivity(comment, access, parent);
    return mapToResponse(comment, access, false);
  }

  @Override
  public LessonDiscussionCommentResponse updateComment(
      UUID courseId,
      UUID courseLessonId,
      UUID commentId,
      UpdateLessonDiscussionCommentRequest request) {
    AccessContext access = requireMemberAccess(courseId, courseLessonId);
    LessonDiscussionComment comment = findCommentOrThrow(commentId);
    ensureCommentBelongsToLesson(comment, courseId, courseLessonId);
    if (!comment.getUserId().equals(access.currentUserId())) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    comment.setContent(normalizeContent(request.getContent()));
    comment = commentRepository.save(comment);
    boolean liked =
        likeRepository.findByCommentIdAndUserIdAndDeletedAtIsNull(commentId, access.currentUserId())
            .isPresent();
    return mapToResponse(comment, access, liked);
  }

  @Override
  public void deleteComment(UUID courseId, UUID courseLessonId, UUID commentId) {
    AccessContext access = requireMemberAccess(courseId, courseLessonId);
    LessonDiscussionComment comment = findCommentOrThrow(commentId);
    ensureCommentBelongsToLesson(comment, courseId, courseLessonId);
    boolean canDelete = comment.getUserId().equals(access.currentUserId()) || access.ownerTeacher();
    if (!canDelete) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
    comment.setDeletedAt(Instant.now());
    comment.setDeletedBy(access.currentUserId());
    commentRepository.save(comment);
  }

  @Override
  public LessonDiscussionLikeResponse toggleLike(UUID courseId, UUID courseLessonId, UUID commentId) {
    AccessContext access = requireMemberAccess(courseId, courseLessonId);
    LessonDiscussionComment comment = findCommentOrThrow(commentId);
    ensureCommentBelongsToLesson(comment, courseId, courseLessonId);

    Optional<LessonDiscussionCommentLike> existing =
        likeRepository.findByCommentIdAndUserIdAndDeletedAtIsNull(commentId, access.currentUserId());

    boolean liked;
    if (existing.isPresent()) {
      LessonDiscussionCommentLike like = existing.get();
      like.setDeletedAt(Instant.now());
      like.setDeletedBy(access.currentUserId());
      likeRepository.save(like);
      liked = false;
    } else {
      LessonDiscussionCommentLike like =
          LessonDiscussionCommentLike.builder()
              .commentId(commentId)
              .userId(access.currentUserId())
              .build();
      likeRepository.save(like);
      liked = true;
    }

    int likesCount = (int) likeRepository.countByCommentIdAndDeletedAtIsNull(commentId);
    comment.setLikesCount(likesCount);
    commentRepository.save(comment);

    return LessonDiscussionLikeResponse.builder()
        .commentId(commentId)
        .liked(liked)
        .likesCount(likesCount)
        .build();
  }

  private void notifyOnCommentActivity(
      LessonDiscussionComment comment, AccessContext actor, LessonDiscussionComment parent) {
    try {
      if (parent == null && actor.studentMember()) {
        NotificationRequest request =
            NotificationRequest.builder()
                .id(UUID.randomUUID().toString())
                .type("LESSON_DISCUSSION")
                .title("Có bình luận mới trong bài học")
                .content(actor.currentUser().getFullName() + " vừa bình luận vào bài học của bạn.")
                .recipientId(actor.course().getTeacherId().toString())
                .senderId(actor.currentUserId().toString())
                .actionUrl("/teacher/courses/" + actor.course().getId() + LESSON_TAB_SUFFIX)
                .metadata(buildMetadata(comment, parent))
                .timestamp(LocalDateTime.now())
                .build();
        streamPublisher.publish(request);
      }

      if (parent != null && !parent.getUserId().equals(actor.currentUserId())) {
        User parentOwner =
            userRepository.findByIdWithRoles(parent.getUserId()).orElse(null);
        String actionUrl =
            parentOwner != null && hasRole(parentOwner, ROLE_TEACHER)
                ? "/teacher/courses/" + actor.course().getId() + LESSON_TAB_SUFFIX
                : "/student/course/" + actor.course().getId() + LESSON_TAB_SUFFIX;
        NotificationRequest request =
            NotificationRequest.builder()
                .id(UUID.randomUUID().toString())
                .type("LESSON_DISCUSSION")
                .title("Có phản hồi trong thảo luận bài học")
                .content(actor.currentUser().getFullName() + " đã trả lời bình luận của bạn.")
                .recipientId(parent.getUserId().toString())
                .senderId(actor.currentUserId().toString())
                .actionUrl(actionUrl)
                .metadata(buildMetadata(comment, parent))
                .timestamp(LocalDateTime.now())
                .build();
        streamPublisher.publish(request);
      }
    } catch (Exception e) {
      log.warn("Failed to send lesson discussion notification", e);
    }
  }

  private Map<String, Object> buildMetadata(
      LessonDiscussionComment comment, LessonDiscussionComment parent) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("courseId", comment.getCourseId().toString());
    metadata.put("courseLessonId", comment.getCourseLessonId().toString());
    metadata.put("commentId", comment.getId().toString());
    if (parent != null) {
      metadata.put("parentCommentId", parent.getId().toString());
    }
    return metadata;
  }

  private LessonDiscussionCommentResponse mapToResponse(
      LessonDiscussionComment comment, AccessContext access, boolean likedByCurrentUser) {
    User author = userRepository.findByIdWithRoles(comment.getUserId()).orElse(null);
    boolean isDeleted = comment.getDeletedAt() != null;
    boolean canEdit = !isDeleted && comment.getUserId().equals(access.currentUserId());
    boolean canDelete = !isDeleted && (canEdit || access.ownerTeacher());

    return LessonDiscussionCommentResponse.builder()
        .id(comment.getId())
        .courseId(comment.getCourseId())
        .courseLessonId(comment.getCourseLessonId())
        .parentId(comment.getParentId())
        .depth(comment.getDepth())
        .content(isDeleted ? "[Đã xóa bình luận]" : comment.getContent())
        .likesCount(comment.getLikesCount() == null ? 0 : comment.getLikesCount())
        .replyCount(comment.getReplyCount() == null ? 0 : comment.getReplyCount())
        .likedByCurrentUser(likedByCurrentUser)
        .deleted(isDeleted)
        .authorId(author != null ? author.getId() : comment.getUserId())
        .authorName(author != null ? author.getFullName() : "Unknown user")
        .authorAvatar(resolveAvatarUrl(author))
        .authorRole(resolvePrimaryRole(author))
        .canEdit(canEdit)
        .canDelete(canDelete)
        .createdAt(comment.getCreatedAt())
        .updatedAt(comment.getUpdatedAt())
        .build();
  }

  private Set<UUID> findLikedCommentIds(UUID currentUserId, List<LessonDiscussionComment> comments) {
    if (currentUserId == null || comments.isEmpty()) {
      return Set.of();
    }
    var commentIds = comments.stream().map(LessonDiscussionComment::getId).toList();
    return likeRepository.findByCommentIdInAndUserIdAndDeletedAtIsNull(commentIds, currentUserId).stream()
        .map(LessonDiscussionCommentLike::getCommentId)
        .collect(Collectors.toSet());
  }

  private AccessContext requireMemberAccess(UUID courseId, UUID courseLessonId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    User currentUser =
        userRepository.findByIdWithRoles(currentUserId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    Course course =
        courseRepository
            .findByIdAndDeletedAtIsNull(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
    CourseLesson lesson =
        courseLessonRepository
            .findByIdAndDeletedAtIsNull(courseLessonId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND));
    if (!lesson.getCourseId().equals(courseId)) {
      throw new AppException(ErrorCode.COURSE_LESSON_NOT_FOUND);
    }

    boolean isOwnerTeacher = course.getTeacherId().equals(currentUserId);
    boolean activeStudent =
        enrollmentRepository
            .findByStudentIdAndCourseIdAndDeletedAtIsNull(currentUserId, courseId)
            .map(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
            .orElse(false);

    if (!isOwnerTeacher && !activeStudent) {
      throw new AppException(ErrorCode.COURSE_ACCESS_DENIED);
    }

    return new AccessContext(currentUserId, currentUser, course, isOwnerTeacher, activeStudent);
  }

  private LessonDiscussionComment findCommentOrThrow(UUID commentId) {
    return commentRepository
        .findByIdAndDeletedAtIsNull(commentId)
        .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "Comment not found"));
  }

  private void ensureCommentBelongsToLesson(
      LessonDiscussionComment comment, UUID courseId, UUID courseLessonId) {
    if (!comment.getCourseId().equals(courseId) || !comment.getCourseLessonId().equals(courseLessonId)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Comment does not belong to this lesson");
    }
  }

  private String normalizeContent(String content) {
    if (content == null || content.trim().isEmpty()) {
      throw new AppException(ErrorCode.COMMENT_REQUIRED);
    }
    return content.trim();
  }

  private String resolvePrimaryRole(User user) {
    if (user == null || user.getRoles() == null) {
      return ROLE_STUDENT;
    }
    if (hasRole(user, ROLE_ADMIN)) return "admin";
    if (hasRole(user, ROLE_TEACHER)) return "teacher";
    return "student";
  }

  private boolean hasRole(User user, String roleName) {
    return user.getRoles().stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
  }

  private String resolveAvatarUrl(User user) {
    if (user == null || user.getAvatar() == null || user.getAvatar().isBlank()) {
      return null;
    }
    String avatar = user.getAvatar();
    if (avatar.startsWith("http")) {
      return avatar;
    }
    try {
      return uploadService.getPresignedUrl(avatar, "avatars");
    } catch (Exception e) {
      return null;
    }
  }

  private record AccessContext(
      UUID currentUserId, User currentUser, Course course, boolean ownerTeacher, boolean studentMember) {}
}
