package com.fptu.math_master.controller;

import com.fptu.math_master.dto.request.CreateFeedbackRequest;
import com.fptu.math_master.dto.request.RespondFeedbackRequest;
import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.FeedbackResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.service.FeedbackService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FeedbackController {

  private final FeedbackService feedbackService;
  private final ObjectMapper objectMapper;

  @PostMapping(value = "/v1/feedbacks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<FeedbackResponse> createFeedback(
      @Valid @RequestPart("request") CreateFeedbackRequest request,
      @RequestPart(value = "files", required = false) java.util.List<MultipartFile> files) {
    return ApiResponse.<FeedbackResponse>builder().result(feedbackService.createFeedback(request, files)).build();
  }

  @GetMapping("/v1/feedbacks/my")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<Page<FeedbackResponse>> getMyFeedbacks(Pageable pageable) {
    return ApiResponse.<Page<FeedbackResponse>>builder().result(feedbackService.getMyFeedbacks(pageable)).build();
  }

  @GetMapping("/admin/feedbacks")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Page<FeedbackResponse>> getAllFeedbacks(Pageable pageable) {
    return ApiResponse.<Page<FeedbackResponse>>builder().result(feedbackService.getAllFeedbacks(pageable)).build();
  }

  @PatchMapping("/v1/feedbacks/{feedbackId}/read")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<FeedbackResponse> markFeedbackAsRead(@PathVariable UUID feedbackId) {
    return ApiResponse.<FeedbackResponse>builder().result(feedbackService.markAsRead(feedbackId)).build();
  }

  @PatchMapping(value = "/admin/feedbacks/{feedbackId}/respond", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<FeedbackResponse> respondFeedback(
      @PathVariable UUID feedbackId, @Valid @RequestBody RespondFeedbackRequest request) {
    return ApiResponse.<FeedbackResponse>builder()
        .result(feedbackService.respondToFeedback(feedbackId, request))
        .build();
  }

  @PatchMapping(value = "/admin/feedbacks/{feedbackId}/respond", consumes = MediaType.TEXT_PLAIN_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<FeedbackResponse> respondFeedbackTextPlain(
      @PathVariable UUID feedbackId, @RequestBody String rawBody) {
    try {
      RespondFeedbackRequest request = objectMapper.readValue(rawBody, RespondFeedbackRequest.class);
      return ApiResponse.<FeedbackResponse>builder()
          .result(feedbackService.respondToFeedback(feedbackId, request))
          .build();
    } catch (Exception ex) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Invalid response payload");
    }
  }
}
