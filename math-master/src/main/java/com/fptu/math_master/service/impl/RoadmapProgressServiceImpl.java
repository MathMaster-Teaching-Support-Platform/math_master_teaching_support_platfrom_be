package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.SubmitTopicAssessmentRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.StudentRoadmapProgressResponse;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.StudentRoadmapProgress;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.enums.StudentRoadmapProgressStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.StudentRoadmapProgressRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.service.LearningRoadmapService;
import com.fptu.math_master.service.RoadmapProgressService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoadmapProgressServiceImpl implements RoadmapProgressService {

  LearningRoadmapRepository roadmapRepository;
  RoadmapTopicRepository topicRepository;
  SubmissionRepository submissionRepository;
  StudentRoadmapProgressRepository progressRepository;
  LearningRoadmapService learningRoadmapService;

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getRoadmapForStudent(UUID studentId, UUID roadmapId) {
    var roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (!roadmap.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    return learningRoadmapService.getRoadmapById(roadmapId);
  }

  @Override
  @Transactional(readOnly = true)
  public StudentRoadmapProgressResponse getRoadmapProgress(UUID studentId, UUID roadmapId) {
    StudentRoadmapProgress progress =
        progressRepository
            .findByStudentIdAndRoadmapId(studentId, roadmapId)
            .orElseGet(
                () ->
                    StudentRoadmapProgress.builder()
                        .studentId(studentId)
                        .roadmapId(roadmapId)
                        .status(StudentRoadmapProgressStatus.NOT_STARTED)
                        .build());

    return mapProgress(progress);
  }

  @Override
  public StudentRoadmapProgressResponse submitTopicAssessment(
      UUID studentId, SubmitTopicAssessmentRequest request) {
    RoadmapTopic topic =
        topicRepository
            .findById(request.getTopicId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    Submission submission =
        submissionRepository
            .findById(request.getSubmissionId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (!submission.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    if (topic.getTopicAssessmentId() == null
        || !topic.getTopicAssessmentId().equals(submission.getAssessmentId())) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    BigDecimal threshold =
        topic.getPassThresholdPercentage() != null
            ? topic.getPassThresholdPercentage()
            : BigDecimal.valueOf(70);
    BigDecimal percentage =
        submission.getPercentage() != null ? submission.getPercentage() : BigDecimal.ZERO;

    StudentRoadmapProgress progress =
        progressRepository
            .findByStudentIdAndRoadmapId(studentId, request.getRoadmapId())
            .orElseGet(
                () ->
                    StudentRoadmapProgress.builder()
                        .studentId(studentId)
                        .roadmapId(request.getRoadmapId())
                        .startedAt(Instant.now())
                        .status(StudentRoadmapProgressStatus.IN_PROGRESS)
                        .build());

    if (percentage.compareTo(threshold) >= 0) {
      topic.setStatus(TopicStatus.COMPLETED);
      topic.setCompletedAt(Instant.now());
      topic.setProgressPercentage(BigDecimal.valueOf(100));
      topicRepository.save(topic);

      var nextTopicOpt =
          topicRepository.findFirstByRoadmapIdAndSequenceOrderGreaterThanOrderBySequenceOrderAsc(
              topic.getRoadmapId(), topic.getSequenceOrder());

      if (nextTopicOpt.isPresent()) {
        RoadmapTopic nextTopic = nextTopicOpt.get();
        if (nextTopic.getStatus() == TopicStatus.LOCKED) {
          nextTopic.setStatus(TopicStatus.NOT_STARTED);
          topicRepository.save(nextTopic);
        }
        progress.setCurrentTopicId(nextTopic.getId());
        progress.setStatus(StudentRoadmapProgressStatus.IN_PROGRESS);
      } else {
        progress.setCurrentTopicId(null);
        progress.setStatus(StudentRoadmapProgressStatus.COMPLETED);
        progress.setCompletedAt(Instant.now());
      }
    } else {
      topic.setStatus(TopicStatus.IN_PROGRESS);
      topicRepository.save(topic);
      progress.setCurrentTopicId(topic.getId());
      progress.setStatus(StudentRoadmapProgressStatus.IN_PROGRESS);
    }

    progress = progressRepository.save(progress);
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
