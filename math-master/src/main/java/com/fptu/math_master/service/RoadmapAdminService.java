package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreateRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.RoadmapEntryTestAnswerRequest;
import com.fptu.math_master.dto.request.RoadmapEntryTestFlagRequest;
import com.fptu.math_master.dto.request.SubmitRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.UpdateRoadmapTopicRequest;
import com.fptu.math_master.dto.response.AnswerAckResponse;
import com.fptu.math_master.dto.response.AttemptStartResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestActiveAttemptResponse;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestInfoResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestResultResponse;
import com.fptu.math_master.dto.response.RoadmapResourceOptionResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestSnapshotResponse;
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

  List<RoadmapResourceOptionResponse> searchResourceOptions(
      String type, UUID chapterId, UUID lessonId, String name);

  void configureEntryTest(UUID roadmapId, CreateRoadmapEntryTestRequest request);

    RoadmapEntryTestInfoResponse getEntryTestForStudent(UUID studentId, UUID roadmapId);

    AnswerAckResponse saveEntryTestAnswer(
      UUID studentId,
      UUID roadmapId,
      UUID attemptId,
      RoadmapEntryTestAnswerRequest request);

    AnswerAckResponse updateEntryTestFlag(
      UUID studentId,
      UUID roadmapId,
      UUID attemptId,
      RoadmapEntryTestFlagRequest request);

    RoadmapEntryTestSnapshotResponse getEntryTestSnapshot(
      UUID studentId, UUID roadmapId, UUID attemptId);

    void saveEntryTestAndExit(UUID studentId, UUID roadmapId, UUID attemptId);

    RoadmapEntryTestActiveAttemptResponse getActiveEntryTestAttempt(UUID studentId, UUID roadmapId);

    AttemptStartResponse startEntryTest(UUID studentId, UUID roadmapId);

    RoadmapEntryTestResultResponse finishEntryTest(UUID studentId, UUID roadmapId, UUID attemptId);

  RoadmapEntryTestResultResponse submitEntryTest(
      UUID studentId, UUID roadmapId, SubmitRoadmapEntryTestRequest request);
}
