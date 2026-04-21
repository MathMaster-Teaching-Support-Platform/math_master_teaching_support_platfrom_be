package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CourseReviewRequest;
import com.fptu.math_master.dto.request.InstructorReplyRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.CourseReviewResponse;
import com.fptu.math_master.dto.response.CourseReviewSummaryResponse;
import com.fptu.math_master.service.CourseReviewService;
import com.fptu.math_master.util.SecurityUtils;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseReviewController {

  CourseReviewService reviewService;

  @PostMapping("/{courseId}/reviews")
  public ApiResponse<CourseReviewResponse> submitReview(
      @PathVariable UUID courseId, @RequestBody @Valid CourseReviewRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<CourseReviewResponse>builder()
        .result(reviewService.submitReview(courseId, request, studentId))
        .build();
  }

  @PutMapping("/reviews/{reviewId}")
  public ApiResponse<CourseReviewResponse> updateReview(
      @PathVariable UUID reviewId, @RequestBody @Valid CourseReviewRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<CourseReviewResponse>builder()
        .result(reviewService.updateReview(reviewId, request, studentId))
        .build();
  }

  @GetMapping("/{courseId}/reviews")
  public ApiResponse<Page<CourseReviewResponse>> getCourseReviews(
      @PathVariable UUID courseId, 
      @RequestParam(required = false) Integer rating,
      Pageable pageable) {
    return ApiResponse.<Page<CourseReviewResponse>>builder()
        .result(reviewService.getCourseReviews(courseId, rating, pageable))
        .build();
  }

  @GetMapping("/{courseId}/reviews/summary")
  public ApiResponse<CourseReviewSummaryResponse> getReviewSummary(@PathVariable UUID courseId) {
    return ApiResponse.<CourseReviewSummaryResponse>builder()
        .result(reviewService.getReviewSummary(courseId))
        .build();
  }

  @PostMapping("/reviews/{reviewId}/reply")
  public ApiResponse<CourseReviewResponse> replyToReview(
      @PathVariable UUID reviewId, @RequestBody @Valid InstructorReplyRequest request) {
    UUID teacherId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<CourseReviewResponse>builder()
        .result(reviewService.replyToReview(reviewId, request, teacherId))
        .build();
  }

  @DeleteMapping("/reviews/{reviewId}")
  public ApiResponse<String> deleteReview(@PathVariable UUID reviewId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    reviewService.deleteReview(reviewId, studentId);
    return ApiResponse.<String>builder().result("Review deleted successfully").build();
  }

  @GetMapping("/{courseId}/my-review")
  public ApiResponse<CourseReviewResponse> getMyReview(@PathVariable UUID courseId) {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return ApiResponse.<CourseReviewResponse>builder()
        .result(reviewService.getMyReview(courseId, studentId))
        .build();
  }
}
