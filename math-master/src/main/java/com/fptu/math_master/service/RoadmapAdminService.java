package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreatePlacementTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoadmapAdminService {

  RoadmapDetailResponse createRoadmap(CreateAdminRoadmapRequest request);

  Page<RoadmapSummaryResponse> getAllRoadmaps(String name, Pageable pageable);

  RoadmapDetailResponse getRoadmap(UUID roadmapId);

  RoadmapDetailResponse updateRoadmap(UUID roadmapId, UpdateAdminRoadmapRequest request);

  void softDeleteRoadmap(UUID roadmapId);

  RoadmapTopicResponse addTopic(UUID roadmapId, CreateRoadmapTopicRequest request);

  void createPlacementTest(CreatePlacementTestRequest request);
}
