package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreateRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.SubmitRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.UpdateRoadmapTopicRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestResultResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.TopicMaterialResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import java.util.List;
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

  RoadmapTopicResponse updateTopic(UUID roadmapId, UUID topicId, UpdateRoadmapTopicRequest request);

  void softDeleteTopic(UUID roadmapId, UUID topicId);

  List<TopicMaterialResponse> getTopicMaterials(UUID topicId);

  List<TopicMaterialResponse> getMaterialsByType(UUID topicId, String resourceType);

  void configureEntryTest(UUID roadmapId, CreateRoadmapEntryTestRequest request);

  RoadmapEntryTestResultResponse submitEntryTest(
      UUID studentId, UUID roadmapId, SubmitRoadmapEntryTestRequest request);
}
