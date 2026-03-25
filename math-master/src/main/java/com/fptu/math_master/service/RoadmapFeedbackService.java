package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateRoadmapFeedbackRequest;
import com.fptu.math_master.dto.response.RoadmapFeedbackResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoadmapFeedbackService {

  RoadmapFeedbackResponse submitFeedback(
      UUID roadmapId, UUID studentId, CreateRoadmapFeedbackRequest request);

  RoadmapFeedbackResponse getMyFeedback(UUID roadmapId, UUID studentId);

  Page<RoadmapFeedbackResponse> getRoadmapFeedbacks(UUID roadmapId, Pageable pageable);
}
