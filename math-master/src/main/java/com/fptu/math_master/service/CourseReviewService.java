package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CourseReviewRequest;
import com.fptu.math_master.dto.response.CourseReviewResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fptu.math_master.dto.request.InstructorReplyRequest;
import com.fptu.math_master.dto.response.CourseReviewSummaryResponse;

public interface CourseReviewService {
  CourseReviewResponse submitReview(UUID courseId, CourseReviewRequest request, UUID studentId);
  
  CourseReviewResponse updateReview(UUID reviewId, CourseReviewRequest request, UUID studentId);
  
  Page<CourseReviewResponse> getCourseReviews(UUID courseId, Integer rating, Pageable pageable);

  CourseReviewSummaryResponse getReviewSummary(UUID courseId);

  CourseReviewResponse replyToReview(UUID reviewId, InstructorReplyRequest request, UUID teacherId);
  
  void deleteReview(UUID reviewId, UUID studentId);
  
  CourseReviewResponse getMyReview(UUID courseId, UUID studentId);
}
