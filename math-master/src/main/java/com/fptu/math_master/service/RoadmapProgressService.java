package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.SubmitTopicAssessmentRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.StudentRoadmapProgressResponse;
import java.util.UUID;

public interface RoadmapProgressService {

  RoadmapDetailResponse getRoadmapForStudent(UUID studentId, UUID roadmapId);

  StudentRoadmapProgressResponse getRoadmapProgress(UUID studentId, UUID roadmapId);

  StudentRoadmapProgressResponse submitTopicAssessment(
      UUID studentId, SubmitTopicAssessmentRequest request);
}
