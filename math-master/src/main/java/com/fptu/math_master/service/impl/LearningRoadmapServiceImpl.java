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
 * Implementation of Learning Roadmap Service
 *
 * <p>Handles:
 * - Roadmap generation (personalized, default, teacher-assigned)
 * - Progress tracking for topics and subtopics
 * - Performance analysis and weak area identification
 * - Material linking and progression
 * - Statistics calculation
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
  QuestionTemplateRepository questionTemplateRepository;
  MindmapRepository mindmapRepository;
  TopicLearningMaterialRepository materialRepository;
  UserRepository userRepository;

  // ============================================================================
  // ROADMAP GENERATION & RETRIEVAL
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
  // PROGRESS TRACKING
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

    // Update topic status
    TopicStatus previousStatus = topic.getStatus();
    topic.setStatus(request.getStatus());
    topic.setProgressPercentage(request.getProgressPercentage());

    // Mark start time if transitioning to IN_PROGRESS
    if (previousStatus != TopicStatus.IN_PROGRESS
        && request.getStatus() == TopicStatus.IN_PROGRESS) {
      topic.setStartedAt(Instant.now());
    }

    // Mark completion time if transitioning to COMPLETED
    if (previousStatus != TopicStatus.COMPLETED && request.getStatus() == TopicStatus.COMPLETED) {
      topic.setCompletedAt(Instant.now());
      topic.setProgressPercentage(BigDecimal.valueOf(100));
    }

    topic = topicRepository.save(topic);

    // Update roadmap progress
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
    Long lockedCount = topics.stream().filter(t -> t.getStatus() == TopicStatus.LOCKED).count();

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
        .lockedTopicsCount(lockedCount)
        .averageProgress(avgProgress)
        .daysRemaining(daysRemaining)
        .build();
  }

  // ============================================================================
  // TOPIC & MATERIALS MANAGEMENT
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

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getTopicMaterials(UUID topicId) {
    List<TopicLearningMaterial> materials =
        materialRepository.findByTopicIdOrderBySequenceOrder(topicId);
    return materials.stream().map(this::mapToMaterialResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getMaterialsByType(UUID topicId, String resourceType) {
    List<TopicLearningMaterial> materials =
        materialRepository.findByTopicIdAndResourceType(topicId, resourceType);
    return materials.stream().map(this::mapToMaterialResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void removeMaterialFromTopic(UUID materialId) {
    log.info("Removing material from topic: materialId={}", materialId);
    materialRepository.deleteById(materialId);
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

  /**
   * Update roadmap progress based on topics
   */
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

    // Update completed topics count
    Long completedCount =
        topics.stream().filter(t -> t.getStatus() == TopicStatus.COMPLETED).count();

    roadmap.setCompletedTopicsCount(completedCount.intValue());

    // If all topics completed, mark roadmap as completed
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

  /**
   * Determine difficulty based on sequence order
   */
  private QuestionDifficulty determineDifficulty(int sequenceOrder, int totalTopics) {
    double ratioTopic = (double) sequenceOrder / totalTopics;
    if (ratioTopic <= 0.33) return QuestionDifficulty.EASY;
    if (ratioTopic <= 0.66) return QuestionDifficulty.MEDIUM;
    return QuestionDifficulty.HARD;
  }

  /**
   * Estimate hours for a topic based on lesson duration
   */
  private Integer estimateHoursForTopic(Lesson lesson) {
    if (lesson.getDurationMinutes() == null) return 2;
    return Math.max(1, lesson.getDurationMinutes() / 60);
  }

  /**
   * Calculate remaining days to complete roadmap
   */
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
        topics.stream().map(this::mapToTopicResponse).collect(Collectors.toList());

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

  private RoadmapTopicResponse mapToTopicResponse(RoadmapTopic topic) {
    List<QuestionTemplateResponse> questionTemplateResponses = new ArrayList<>();
    List<MindmapResponse> mindmapResponses = new ArrayList<>();

    // Load question templates and mindmaps from the linked lesson
    if (topic.getLessonId() != null) {
      // Fetch question templates for the lesson
      List<QuestionTemplate> questionTemplates =
          questionTemplateRepository.findByLessonIdAndNotDeleted(topic.getLessonId());
      questionTemplateResponses =
          questionTemplates.stream()
              .map(this::mapToQuestionTemplateResponse)
              .collect(Collectors.toList());

      // Fetch mindmaps for the lesson
      Page<Mindmap> mindmapsPage =
          mindmapRepository.findByLessonIdAndNotDeleted(topic.getLessonId(), Pageable.unpaged());
      mindmapResponses =
          mindmapsPage.getContent().stream()
              .map(this::mapToMindmapResponse)
              .collect(Collectors.toList());
    }

    return RoadmapTopicResponse.builder()
        .id(topic.getId())
        .title(topic.getTitle())
        .description(topic.getDescription())
        .status(topic.getStatus())
        .difficulty(topic.getDifficulty())
        .sequenceOrder(topic.getSequenceOrder())
        .priority(topic.getPriority())
        .progressPercentage(topic.getProgressPercentage())
        .estimatedHours(topic.getEstimatedHours())
        .topicAssessmentId(topic.getTopicAssessmentId())
        .passThresholdPercentage(topic.getPassThresholdPercentage())
        .startedAt(topic.getStartedAt())
        .completedAt(topic.getCompletedAt())
        .questionTemplates(questionTemplateResponses)
        .mindmaps(mindmapResponses)
        .build();
  }

  private AssessmentResponse mapToAssessmentResponse(Assessment assessment) {
    return AssessmentResponse.builder()
        .id(assessment.getId())
        .teacherId(assessment.getTeacherId())
        .title(assessment.getTitle())
        .description(assessment.getDescription())
        .assessmentType(assessment.getAssessmentType())
        .timeLimitMinutes(assessment.getTimeLimitMinutes())
        .passingScore(assessment.getPassingScore())
        .startDate(assessment.getStartDate())
        .endDate(assessment.getEndDate())
        .randomizeQuestions(assessment.getRandomizeQuestions())
        .showCorrectAnswers(assessment.getShowCorrectAnswers())
        .assessmentMode(assessment.getAssessmentMode())
        .examMatrixId(assessment.getExamMatrixId())
        .allowMultipleAttempts(assessment.getAllowMultipleAttempts())
        .maxAttempts(assessment.getMaxAttempts())
        .attemptScoringPolicy(assessment.getAttemptScoringPolicy())
        .showScoreImmediately(assessment.getShowScoreImmediately())
        .status(assessment.getStatus())
        .createdAt(assessment.getCreatedAt())
        .updatedAt(assessment.getUpdatedAt())
        .build();
  }

  private MindmapResponse mapToMindmapResponse(Mindmap mindmap) {
    return MindmapResponse.builder()
        .id(mindmap.getId())
        .teacherId(mindmap.getTeacherId())
        .lessonId(mindmap.getLessonId())
        .title(mindmap.getTitle())
        .description(mindmap.getDescription())
        .aiGenerated(mindmap.getAiGenerated())
        .generationPrompt(mindmap.getGenerationPrompt())
        .status(mindmap.getStatus())
        .createdAt(mindmap.getCreatedAt())
        .updatedAt(mindmap.getUpdatedAt())
        .build();
  }

  private TopicMaterialResponse mapToMaterialResponse(TopicLearningMaterial material) {
    return TopicMaterialResponse.builder()
        .id(material.getId())
        .resourceTitle(material.getResourceTitle())
        .resourceType(material.getResourceType())
        .sequenceOrder(material.getSequenceOrder())
        .isRequired(material.getIsRequired())
        .lessonId(material.getLessonId())
        .questionId(material.getQuestionId())
        .assessmentId(material.getAssessmentId())
        .mindmapId(material.getMindmapId())
        .chapterId(material.getChapterId())
        .build();
  }

  private QuestionTemplateResponse mapToQuestionTemplateResponse(QuestionTemplate template) {
    return QuestionTemplateResponse.builder()
        .id(template.getId())
        .createdBy(template.getCreatedBy())
        .creatorName(
            userRepository
                .findById(template.getCreatedBy())
                .map(User::getFullName)
                .orElse("Unknown"))
        .name(template.getName())
        .description(template.getDescription())
        .templateType(template.getTemplateType())
        .templateVariant(template.getTemplateVariant())
        .templateText(template.getTemplateText())
        .parameters(template.getParameters())
        .answerFormula(template.getAnswerFormula())
        .optionsGenerator(template.getOptionsGenerator())
        .difficultyRules(template.getDifficultyRules())
        .constraints(template.getConstraints())
        .cognitiveLevel(template.getCognitiveLevel())
        .tags(template.getTags())
        .isPublic(template.getIsPublic())
        .status(template.getStatus())
        .usageCount(template.getUsageCount())
        .avgSuccessRate(template.getAvgSuccessRate())
        .createdAt(template.getCreatedAt())
        .updatedAt(template.getUpdatedAt())
        .build();
  }
}
