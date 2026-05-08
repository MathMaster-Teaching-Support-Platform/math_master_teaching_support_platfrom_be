package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateFeedbackRequest;
import com.fptu.math_master.dto.request.RespondFeedbackRequest;
import com.fptu.math_master.dto.response.FeedbackResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface FeedbackService {
  FeedbackResponse createFeedback(CreateFeedbackRequest request, List<MultipartFile> files);

  Page<FeedbackResponse> getMyFeedbacks(Pageable pageable);

  Page<FeedbackResponse> getAllFeedbacks(Pageable pageable);

  FeedbackResponse respondToFeedback(java.util.UUID feedbackId, RespondFeedbackRequest request);

  FeedbackResponse markAsRead(java.util.UUID feedbackId);
}
