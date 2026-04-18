package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.UpdateTopicProgressRequest;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.LearningRoadmapService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of Learning Roadmap Service.
 *
 * <p>Course-centric: each RoadmapTopic links to exactly one Course.
 * There is NO topic locking — students can explore any topic at any time.
 * Progress tracking is optional and non-blocking.
 */
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LearningRoadmapServiceImpl implements LearningRoadmapService {

  // Repositories
  LearningRoadmapRepository roadmapRepository;
  RoadmapTopicRepository topicRepository;
  TopicCourseRepository topicCourseRepository;
  CourseRepository courseRepository;
  CourseLessonRepository courseLessonRepository;

  // ============================================================================
  // ROADMAP RETRIEVAL
  // ============================================================================

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getRoadmapById(UUID roadmapId) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToDetailResponse(roadmap);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RoadmapSummaryResponse> getStudentRoadmaps(UUID studentId, Pageable pageable) {
    Page<LearningRoadmap> roadmaps =
        roadmapRepository.findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            studentId, pageable);

    return roadmaps.map(this::mapToSummaryResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getActiveRoadmapBySubject(UUID studentId, String subject) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findTopByStudentIdAndSubjectAndDeletedAtIsNullOrderByCreatedAtDesc(studentId, subject)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToDetailResponse(roadmap);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoadmapSummaryResponse> getStudentRoadmapsList(UUID studentId) {
    return roadmapRepository.findByStudentIdAndDeletedAtIsNull(studentId).stream()
        .map(this::mapToSummaryResponse)
        .collect(Collectors.toList());
  }

  // ============================================================================
  // PROGRESS TRACKING (non-blocking)
  // ============================================================================

  @Override
  @Transactional
  public RoadmapTopicResponse updateTopicProgress(UpdateTopicProgressRequest request) {
    log.info(
        "Updating topic progress: topicId={}, status={}, progress={}%",
        request.getTopicId(), request.getStatus(), request.getProgressPercentage());

    RoadmapTopic topic =
        topicRepository
            .findById(request.getTopicId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    TopicStatus previousStatus = topic.getStatus();
    topic.setStatus(request.getStatus());
    topic.setProgressPercentage(request.getProgressPercentage());

    if (previousStatus != TopicStatus.IN_PROGRESS
        && request.getStatus() == TopicStatus.IN_PROGRESS) {
      topic.setStartedAt(Instant.now());
    }

    if (previousStatus != TopicStatus.COMPLETED && request.getStatus() == TopicStatus.COMPLETED) {
      topic.setCompletedAt(Instant.now());
      topic.setProgressPercentage(BigDecimal.valueOf(100));
    }

    topic = topicRepository.save(topic);
    updateRoadmapProgress(topic.getRoadmapId());

    log.info("Topic progress updated successfully: topicId={}", topic.getId());
    return mapToTopicResponse(topic);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapTopicResponse getNextTopic(UUID roadmapId) {
    RoadmapTopic nextTopic =
        topicRepository
            .findNextTopic(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToTopicResponse(nextTopic);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapStatsResponse getRoadmapStats(UUID roadmapId) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId);

    Long easyCount =
        topics.stream().filter(t -> t.getDifficulty() == QuestionDifficulty.EASY).count();
    Long mediumCount =
        topics.stream().filter(t -> t.getDifficulty() == QuestionDifficulty.MEDIUM).count();
    Long hardCount =
        topics.stream().filter(t -> t.getDifficulty() == QuestionDifficulty.HARD).count();

    int totalEstimatedHours =
        topics.stream()
            .mapToInt(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : 1)
            .sum();

    BigDecimal avgProgress =
        topics.stream()
            .map(RoadmapTopic::getProgressPercentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(Math.max(topics.size(), 1)), 2, RoundingMode.HALF_UP);

    Integer daysRemaining = calculateDaysRemaining(roadmap);

    return RoadmapStatsResponse.builder()
        .totalEstimatedHours(totalEstimatedHours)
        .easyTopicsCount(easyCount)
        .mediumTopicsCount(mediumCount)
        .hardTopicsCount(hardCount)
        .lockedTopicsCount(0L) // no locking — always 0
        .averageProgress(avgProgress)
        .daysRemaining(daysRemaining)
        .build();
  }

  // ============================================================================
  // TOPIC MANAGEMENT
  // ============================================================================

  @Override
  @Transactional(readOnly = true)
  public RoadmapTopicResponse getTopicDetails(UUID topicId) {
    RoadmapTopic topic =
        topicRepository
            .findById(topicId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToTopicResponse(topic);
  }

  // ============================================================================
  // UTILITY & ADMINISTRATION
  // ============================================================================

  @Override
  @Transactional(readOnly = true)
  public boolean existsActiveRoadmap(UUID studentId, String subject) {
    return roadmapRepository.existsActiveRoadmapForStudentAndSubject(studentId, subject);
  }

  @Override
  @Transactional
  public void archiveRoadmap(UUID roadmapId) {
    log.info("Archiving roadmap: roadmapId={}", roadmapId);

    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    roadmap.setDeletedAt(Instant.now());
    roadmap.setStatus(RoadmapStatus.ARCHIVED);
    roadmapRepository.save(roadmap);

    log.info("Roadmap archived successfully");
  }

  @Override
  @Transactional(readOnly = true)
  public Integer estimateCompletionDays(UUID roadmapId) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return roadmap.getEstimatedCompletionDays();
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal calculateRoadmapProgress(UUID roadmapId) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return roadmap.getProgressPercentage();
  }

  // ============================================================================
  // PRIVATE HELPER METHODS
  // ============================================================================

  private void updateRoadmapProgress(UUID roadmapId) {
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId).orElse(null);
    if (roadmap == null) return;

    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId);
    if (topics.isEmpty()) {
      roadmap.setProgressPercentage(BigDecimal.ZERO);
    } else {
      BigDecimal totalProgress =
          topics.stream()
              .map(RoadmapTopic::getProgressPercentage)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal avgProgress =
          totalProgress.divide(BigDecimal.valueOf(topics.size()), 2, RoundingMode.HALF_UP);

      roadmap.setProgressPercentage(avgProgress);
    }

    Long completedCount =
        topics.stream().filter(t -> t.getStatus() == TopicStatus.COMPLETED).count();

    roadmap.setCompletedTopicsCount(completedCount.intValue());

    if (completedCount >= topics.size() && !topics.isEmpty()) {
      roadmap.setStatus(RoadmapStatus.COMPLETED);
      roadmap.setCompletedAt(Instant.now());
      roadmap.setProgressPercentage(BigDecimal.valueOf(100));
    } else if (roadmap.getStatus() == RoadmapStatus.GENERATED) {
      roadmap.setStatus(RoadmapStatus.IN_PROGRESS);
      roadmap.setStartedAt(Instant.now());
    }

    roadmapRepository.save(roadmap);
  }

  private Integer calculateDaysRemaining(LearningRoadmap roadmap) {
    if (roadmap.getProgressPercentage().compareTo(BigDecimal.valueOf(100)) >= 0) {
      return 0;
    }

    BigDecimal remainingPercent = BigDecimal.valueOf(100).subtract(roadmap.getProgressPercentage());
    BigDecimal estimatedDays =
        remainingPercent
            .multiply(
                BigDecimal.valueOf(
                    roadmap.getEstimatedCompletionDays() != null
                        ? roadmap.getEstimatedCompletionDays()
                        : 30))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

    return estimatedDays.intValue();
  }

  // ============================================================================
  // MAPPING METHODS
  // ============================================================================

  private RoadmapDetailResponse mapToDetailResponse(LearningRoadmap roadmap) {
    List<RoadmapTopic> topics =
        topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmap.getId());
    List<RoadmapTopicResponse> topicResponses =
        topics.stream()
            .filter(topic -> topic.getDeletedAt() == null)
            .map(this::mapToTopicResponse)
            .collect(Collectors.toList());

    return RoadmapDetailResponse.builder()
        .id(roadmap.getId())
        .name(roadmap.getName())
        .studentId(roadmap.getStudentId())
        .teacherId(roadmap.getTeacherId())
        .subjectId(roadmap.getSubjectId())
        .subject(roadmap.getSubject())
        .gradeLevel(roadmap.getGradeLevel())
        .generationType(roadmap.getGenerationType())
        .status(roadmap.getStatus())
        .progressPercentage(roadmap.getProgressPercentage())
        .completedTopicsCount(roadmap.getCompletedTopicsCount())
        .totalTopicsCount(roadmap.getTotalTopicsCount())
        .estimatedCompletionDays(roadmap.getEstimatedCompletionDays())
        .description(roadmap.getDescription())
        .startedAt(roadmap.getStartedAt())
        .completedAt(roadmap.getCompletedAt())
        .createdAt(roadmap.getCreatedAt())
        .updatedAt(roadmap.getUpdatedAt())
        .topics(topicResponses)
        .stats(getRoadmapStats(roadmap.getId()))
        .build();
  }

  private RoadmapSummaryResponse mapToSummaryResponse(LearningRoadmap roadmap) {
    return RoadmapSummaryResponse.builder()
        .id(roadmap.getId())
        .name(roadmap.getName())
        .studentId(roadmap.getStudentId())
        .subjectId(roadmap.getSubjectId())
        .subject(roadmap.getSubject())
        .gradeLevel(roadmap.getGradeLevel())
        .status(roadmap.getStatus())
        .progressPercentage(roadmap.getProgressPercentage())
        .completedTopicsCount(roadmap.getCompletedTopicsCount())
        .totalTopicsCount(roadmap.getTotalTopicsCount())
        .createdAt(roadmap.getCreatedAt())
        .updatedAt(roadmap.getUpdatedAt())
        .build();
  }

  /**
   * Maps a RoadmapTopic to its response, resolving the linked courses info.
   * If a course has been deleted, returns a placeholder with title "Unavailable".
   */
  private RoadmapTopicResponse mapToTopicResponse(RoadmapTopic topic) {
    // Load courses from TopicCourse join table
    List<TopicCourse> topicCourses = topicCourseRepository.findByTopicId(topic.getId());
    
    List<RoadmapTopicCourseResponse> courseResponses = new ArrayList<>();
    
    for (TopicCourse topicCourse : topicCourses) {
      Course course = courseRepository.findByIdAndDeletedAtIsNull(topicCourse.getCourseId()).orElse(null);
      
      RoadmapTopicCourseResponse courseResponse;
      if (course != null) {
        long totalLessons = courseLessonRepository.countByCourseIdAndNotDeleted(course.getId());
        courseResponse = RoadmapTopicCourseResponse.builder()
            .id(course.getId())
            .title(course.getTitle())
            .thumbnail(course.getThumbnailUrl())
            .totalLessons((int) totalLessons)
            .isEnrolled(false) // FE overlays real-time enrollment state
            .build();
      } else {
        // Course was deleted — return a placeholder so FE can show "Unavailable"
        courseResponse = RoadmapTopicCourseResponse.builder()
            .id(topicCourse.getCourseId())
            .title("Unavailable")
            .totalLessons(0)
            .isEnrolled(false)
            .build();
      }
      
      courseResponses.add(courseResponse);
    }

    return RoadmapTopicResponse.builder()
        .id(topic.getId())
        .title(topic.getTitle())
        .description(topic.getDescription())
        .status(topic.getStatus())
        .difficulty(topic.getDifficulty())
        .sequenceOrder(topic.getSequenceOrder())
        .courses(courseResponses)
        .startedAt(topic.getStartedAt())
        .build();
  }
}
