package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreatePlacementTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.PlacementQuestionMappingRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.PlacementQuestionMapping;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.PlacementQuestionMappingRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.service.LearningRoadmapService;
import com.fptu.math_master.service.RoadmapAdminService;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoadmapAdminServiceImpl implements RoadmapAdminService {

  LearningRoadmapRepository roadmapRepository;
  RoadmapTopicRepository topicRepository;
  AssessmentRepository assessmentRepository;
  QuestionRepository questionRepository;
  PlacementQuestionMappingRepository placementQuestionMappingRepository;
  LearningRoadmapService learningRoadmapService;

  @Override
  public RoadmapDetailResponse createRoadmap(CreateAdminRoadmapRequest request) {
    LearningRoadmap roadmap =
        LearningRoadmap.builder()
            .studentId(request.getStudentId())
            .subject(request.getSubject())
            .gradeLevel(request.getGradeLevel())
            .description(request.getDescription())
            .generationType(RoadmapGenerationType.TEACHER_ASSIGNED)
            .status(RoadmapStatus.GENERATED)
            .progressPercentage(BigDecimal.ZERO)
            .completedTopicsCount(0)
            .totalTopicsCount(0)
            .estimatedCompletionDays(request.getEstimatedCompletionDays())
            .build();

    roadmap = roadmapRepository.save(roadmap);
    return learningRoadmapService.getRoadmapById(roadmap.getId());
  }

  @Override
  public RoadmapTopicResponse addTopic(java.util.UUID roadmapId, CreateRoadmapTopicRequest request) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (request.getTopicAssessmentId() != null
        && assessmentRepository.findById(request.getTopicAssessmentId()).isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    RoadmapTopic topic =
        RoadmapTopic.builder()
            .roadmapId(roadmapId)
            .lessonId(request.getLessonId())
            .topicAssessmentId(request.getTopicAssessmentId())
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getSequenceOrder() == 1 ? TopicStatus.NOT_STARTED : TopicStatus.LOCKED)
            .difficulty(request.getDifficulty())
            .sequenceOrder(request.getSequenceOrder())
            .priority(request.getPriority())
            .estimatedHours(request.getEstimatedHours())
            .progressPercentage(BigDecimal.ZERO)
            .passThresholdPercentage(
                request.getPassThresholdPercentage() != null
                    ? request.getPassThresholdPercentage()
                    : BigDecimal.valueOf(70))
            .build();

    topic = topicRepository.save(topic);

    long totalTopics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId).size();
    roadmap.setTotalTopicsCount((int) totalTopics);
    roadmapRepository.save(roadmap);

    return learningRoadmapService.getTopicDetails(topic.getId());
  }

  @Override
  public void createPlacementTest(CreatePlacementTestRequest request) {
    if (roadmapRepository.findById(request.getRoadmapId()).isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    if (assessmentRepository.findById(request.getPlacementAssessmentId()).isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    placementQuestionMappingRepository.deleteByPlacementAssessmentId(request.getPlacementAssessmentId());

    for (PlacementQuestionMappingRequest mappingRequest : request.getMappings()) {
      if (questionRepository.findById(mappingRequest.getQuestionId()).isEmpty()) {
        throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
      }

      if (topicRepository.findById(mappingRequest.getRoadmapTopicId()).isEmpty()) {
        throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
      }

      PlacementQuestionMapping mapping =
          PlacementQuestionMapping.builder()
              .placementAssessmentId(request.getPlacementAssessmentId())
              .questionId(mappingRequest.getQuestionId())
              .roadmapTopicId(mappingRequest.getRoadmapTopicId())
              .orderIndex(mappingRequest.getOrderIndex())
              .weight(mappingRequest.getWeight())
              .build();

      placementQuestionMappingRepository.save(mapping);
    }

    log.info(
        "Placement mappings configured for assessment={} with {} mappings",
        request.getPlacementAssessmentId(),
        request.getMappings().size());
  }
}
