package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CourseReviewRequest;
import com.fptu.math_master.dto.request.InstructorReplyRequest;
import com.fptu.math_master.dto.response.CourseReviewResponse;
import com.fptu.math_master.dto.response.CourseReviewSummaryResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.CourseReview;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.CourseReviewRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.service.CourseReviewService;
import com.fptu.math_master.service.UploadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class CourseReviewServiceImpl implements CourseReviewService {

  CourseReviewRepository reviewRepository;
  CourseRepository courseRepository;
  EnrollmentRepository enrollmentRepository;
  UserRepository userRepository;
  UploadService uploadService;
  MinioProperties minioProperties;

  @Override
  @Transactional
  public CourseReviewResponse submitReview(UUID courseId, CourseReviewRequest request, UUID studentId) {
    // 1. Check if course exists
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    // 2. Check if student is enrolled
    enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_ENROLLED));

    // 3. Check if already reviewed (including soft-deleted)
    CourseReview review = reviewRepository.findByCourseIdAndStudentId(courseId, studentId).orElse(null);
    if (review != null && review.getDeletedAt() == null) {
      throw new AppException(ErrorCode.ALREADY_REVIEWED);
    }

    // 4. Create or restore review
    if (review == null) {
      review = CourseReview.builder()
          .courseId(courseId)
          .studentId(studentId)
          .rating(request.getRating())
          .comment(request.getComment())
          .build();
    } else {
      review.setDeletedAt(null);
      review.setDeletedBy(null);
      review.setRating(request.getRating());
      review.setComment(request.getComment());
    }

    review = reviewRepository.save(review);

    // 5. Update course average rating
    updateCourseRating(course);

    return mapToResponse(review);
  }

  @Override
  @Transactional
  public CourseReviewResponse updateReview(UUID reviewId, CourseReviewRequest request, UUID studentId) {
    CourseReview review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
        .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    review.setRating(request.getRating());
    review.setComment(request.getComment());
    review = reviewRepository.save(review);

    // Update course average rating
    Course course = courseRepository.findByIdAndDeletedAtIsNull(review.getCourseId())
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
    updateCourseRating(course);

    return mapToResponse(review);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CourseReviewResponse> getCourseReviews(UUID courseId, Integer rating, Pageable pageable) {
    log.info("Fetching course reviews for courseId: {}, rating filter: {}, pageable: {}", courseId, rating, pageable);
    
    Page<CourseReview> reviewPage;
    if (rating != null) {
      reviewPage = reviewRepository.findByCourseIdAndRatingAndDeletedAtIsNull(courseId, rating, pageable);
    } else {
      reviewPage = reviewRepository.findByCourseIdAndDeletedAtIsNull(courseId, pageable);
    }
    
    log.info("Found {} reviews for courseId: {}", reviewPage.getTotalElements(), courseId);
    System.out.println("DEBUG: Found " + reviewPage.getTotalElements() + " reviews for courseId: " + courseId);
    return reviewPage.map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public CourseReviewSummaryResponse getReviewSummary(UUID courseId) {
    long totalReviews = reviewRepository.countByCourseIdAndDeletedAtIsNull(courseId);
    Double avg = reviewRepository.calculateAverageRating(courseId);
    BigDecimal averageRating = avg != null ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
        : BigDecimal.ZERO;

    Map<Integer, Long> distribution = new HashMap<>();
    for (int i = 1; i <= 5; i++)
      distribution.put(i, 0L);

    List<Object[]> stats = reviewRepository.getRatingDistribution(courseId);
    for (Object[] row : stats) {
      distribution.put((Integer) row[0], (Long) row[1]);
    }

    return CourseReviewSummaryResponse.builder()
        .totalReviews(totalReviews)
        .averageRating(averageRating)
        .ratingDistribution(distribution)
        .build();
  }

  @Override
  @Transactional
  public CourseReviewResponse replyToReview(UUID reviewId, InstructorReplyRequest request, UUID teacherId) {
    CourseReview review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
        .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

    Course course = courseRepository.findByIdAndDeletedAtIsNull(review.getCourseId())
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    if (!course.getTeacherId().equals(teacherId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    review.setInstructorReply(request.getReply());
    review.setRepliedAt(Instant.now());

    return mapToResponse(reviewRepository.save(review));
  }

  @Override
  @Transactional
  public void deleteReview(UUID reviewId, UUID studentId) {
    CourseReview review = reviewRepository.findByIdAndDeletedAtIsNull(reviewId)
        .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

    if (!review.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    review.setDeletedAt(Instant.now());
    review.setDeletedBy(studentId);
    reviewRepository.save(review);

    Course course = courseRepository.findByIdAndDeletedAtIsNull(review.getCourseId()).orElse(null);
    if (course != null) {
      updateCourseRating(course);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public CourseReviewResponse getMyReview(UUID courseId, UUID studentId) {
    return reviewRepository.findByCourseIdAndStudentIdAndDeletedAtIsNull(courseId, studentId)
        .map(this::mapToResponse)
        .orElse(null);
  }

  private void updateCourseRating(Course course) {
    Double avg = reviewRepository.calculateAverageRating(course.getId());
    if (avg == null) {
      course.setRating(BigDecimal.ZERO);
    } else {
      course.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
    }
    courseRepository.save(course);
  }

  private CourseReviewResponse mapToResponse(CourseReview review) {
    return CourseReviewResponse.builder()
        .id(review.getId())
        .courseId(review.getCourseId())
        .studentId(review.getStudentId())
        .studentName(review.getStudent() != null ? review.getStudent().getFullName() : "Anonymous")
        .studentAvatar(resolveAvatarUrl(review.getStudent()))
        .rating(review.getRating())
        .comment(review.getComment())
        .instructorReply(review.getInstructorReply())
        .repliedAt(review.getRepliedAt())
        .createdAt(review.getCreatedAt())
        .updatedAt(review.getUpdatedAt())
        .build();
  }

  private String resolveAvatarUrl(User student) {
    if (student == null || student.getAvatar() == null) {
      return null;
    }
    String avatar = student.getAvatar();
    if (avatar.startsWith("http")) {
      return avatar;
    }
    try {
      return uploadService.getPresignedUrl(avatar, "avatars");
    } catch (Exception e) {
      log.error("Failed to generate presigned URL for student avatar: {}", avatar, e);
      return null;
    }
  }
}
