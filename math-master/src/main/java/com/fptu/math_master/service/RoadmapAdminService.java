package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreatePlacementTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import java.util.UUID;

public interface RoadmapAdminService {

  RoadmapDetailResponse createRoadmap(CreateAdminRoadmapRequest request);

  RoadmapTopicResponse addTopic(UUID roadmapId, CreateRoadmapTopicRequest request);

  void createPlacementTest(CreatePlacementTestRequest request);
}
