package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.SubmitPlacementTestRequest;
import com.fptu.math_master.dto.response.StudentRoadmapProgressResponse;
import com.fptu.math_master.entity.Answer;
import com.fptu.math_master.entity.PlacementQuestionMapping;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.StudentRoadmapProgress;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.enums.StudentRoadmapProgressStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AnswerRepository;
import com.fptu.math_master.repository.PlacementQuestionMappingRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.StudentRoadmapProgressRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.service.PlacementTestService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class PlacementTestServiceImpl implements PlacementTestService {

  PlacementQuestionMappingRepository mappingRepository;
  SubmissionRepository submissionRepository;
  AnswerRepository answerRepository;
  RoadmapTopicRepository topicRepository;
  StudentRoadmapProgressRepository progressRepository;

  @Override
  public StudentRoadmapProgressResponse submitPlacementTest(
      UUID studentId, SubmitPlacementTestRequest request) {
    Submission submission =
        submissionRepository
            .findById(request.getSubmissionId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (!submission.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    List<PlacementQuestionMapping> mappings =
        mappingRepository.findByPlacementAssessmentIdOrderByOrderIndex(submission.getAssessmentId());

    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    List<Answer> answers = answerRepository.findBySubmissionId(submission.getId());
    Map<UUID, Answer> answerByQuestionId = new HashMap<>();
    for (Answer answer : answers) {
      answerByQuestionId.put(answer.getQuestionId(), answer);
    }

    Map<UUID, Integer> totalByTopic = new HashMap<>();
    Map<UUID, Integer> correctByTopic = new HashMap<>();

    for (PlacementQuestionMapping mapping : mappings) {
      UUID topicId = mapping.getRoadmapTopicId();
      totalByTopic.merge(topicId, 1, Integer::sum);

      Answer answer = answerByQuestionId.get(mapping.getQuestionId());
      if (answer != null && Boolean.TRUE.equals(answer.getIsCorrect())) {
        correctByTopic.merge(topicId, 1, Integer::sum);
      }
    }

    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(request.getRoadmapId());
    if (topics.isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    RoadmapTopic suggestedTopic = topics.get(topics.size() - 1);
    for (RoadmapTopic topic : topics) {
      int total = totalByTopic.getOrDefault(topic.getId(), 0);
      int correct = correctByTopic.getOrDefault(topic.getId(), 0);

      double mastery = total == 0 ? 0 : (correct * 100.0 / total);
      if (mastery < 70.0) {
        suggestedTopic = topic;
        break;
      }
    }

    StudentRoadmapProgress progress =
        progressRepository
            .findByStudentIdAndRoadmapId(studentId, request.getRoadmapId())
            .orElseGet(
                () ->
                    StudentRoadmapProgress.builder()
                        .studentId(studentId)
                        .roadmapId(request.getRoadmapId())
                        .build());

    progress.setSuggestedStartTopicId(suggestedTopic.getId());
    progress.setCurrentTopicId(suggestedTopic.getId());
    progress.setPlacementSubmissionId(submission.getId());
    progress.setStatus(StudentRoadmapProgressStatus.IN_PROGRESS);
    if (progress.getStartedAt() == null) {
      progress.setStartedAt(Instant.now());
    }

    progress = progressRepository.save(progress);

    log.info(
        "Placement submitted: student={}, roadmap={}, suggestedTopic={}",
        studentId,
        request.getRoadmapId(),
        suggestedTopic.getId());

    return mapProgress(progress);
  }

  private StudentRoadmapProgressResponse mapProgress(StudentRoadmapProgress progress) {
    return StudentRoadmapProgressResponse.builder()
        .roadmapId(progress.getRoadmapId())
        .studentId(progress.getStudentId())
        .currentTopicId(progress.getCurrentTopicId())
        .suggestedStartTopicId(progress.getSuggestedStartTopicId())
        .placementSubmissionId(progress.getPlacementSubmissionId())
        .status(progress.getStatus())
        .startedAt(progress.getStartedAt())
        .completedAt(progress.getCompletedAt())
        .build();
  }
}
