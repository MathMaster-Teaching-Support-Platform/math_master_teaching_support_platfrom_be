package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CompleteSubtopicRequest;
import com.fptu.math_master.dto.request.GenerateRoadmapRequest;
import com.fptu.math_master.dto.request.UpdateTopicProgressRequest;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.LearningRoadmapService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
  GradeRepository gradeRepository;
  RoadmapSubtopicRepository subtopicRepository;
  TopicLearningMaterialRepository materialRepository;
  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;
  UserRepository userRepository;
  QuestionRepository questionRepository;

  // ============================================================================
  // ROADMAP GENERATION & RETRIEVAL
  // ============================================================================

  @Override
  @Transactional
  public RoadmapDetailResponse generateRoadmap(GenerateRoadmapRequest request) {
    // Extract studentId from JWT token
    String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID studentId = UUID.fromString(userId);
    
    log.info("Generating roadmap for student={}, subject={}, type={}",
        studentId, request.getSubject(), request.getGenerationType());

    // Check if active roadmap already exists
    if (existsActiveRoadmap(studentId, request.getSubject())) {
      log.warn("Roadmap already exists for student={}, subject={}",
          studentId, request.getSubject());
      return getActiveRoadmapBySubject(studentId, request.getSubject());
    }

    // Create roadmap entity
    LearningRoadmap roadmap = LearningRoadmap.builder()
        .studentId(studentId)
        .subject(request.getSubject())
        .gradeLevel(request.getGradeLevel())
        .generationType(request.getGenerationType())
        .description(request.getDescription())
        .status(RoadmapStatus.GENERATED)
        .progressPercentage(BigDecimal.ZERO)
        .completedTopicsCount(0)
        .totalTopicsCount(0)
        .build();

    // Generate topics based on type
    List<RoadmapTopic> topics;
    switch (request.getGenerationType()) {
      case PERSONALIZED:
        topics = generatePersonalizedTopics(roadmap, request);
        break;
      case DEFAULT:
        topics = generateDefaultTopics(roadmap, request);
        break;
      case TEACHER_ASSIGNED:
        topics = new ArrayList<>(); // Teacher will assign topics
        break;
      default:
        throw new IllegalArgumentException("Unknown generation type: " + request.getGenerationType());
    }

    // Calculate estimated completion days
    int totalEstimatedHours = topics.stream()
        .mapToInt(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : 1)
        .sum();
    roadmap.setEstimatedCompletionDays((totalEstimatedHours + 4) / 5); // Assuming 5 hours/day
    roadmap.setTotalTopicsCount(topics.size());

    // Save roadmap
    roadmap = roadmapRepository.save(roadmap);

    // Save topics with their subtopics and materials
    for (RoadmapTopic topic : topics) {
      topic.setRoadmapId(roadmap.getId());
      topic = topicRepository.save(topic);

      // Generate subtopics and materials
      generateSubtopicsForTopic(topic);
      linkMaterials(topic, request);
    }

    log.info("Roadmap generated successfully: roadmapId={}", roadmap.getId());
    return getRoadmapById(roadmap.getId());
  }

  /**
   * Generate personalized topics based on student performance
   */
  private List<RoadmapTopic> generatePersonalizedTopics(LearningRoadmap roadmap, GenerateRoadmapRequest request) {
    List<RoadmapTopic> topics = new ArrayList<>();

    // Analyze weak areas from performance data
    List<RoadmapTopic> weakTopics = analyzePerformanceDataForTopics(
        roadmap.getStudentId(), request.getSubject());

    // If insufficient performance data, fall back to default + weak areas
    if (weakTopics.isEmpty() && request.getCreateDefaultIfNeeded()) {
      return generateDefaultTopics(roadmap, request);
    }

    // Sort by priority (weak topics first)
    topics.addAll(weakTopics);

    // Add prerequisite and next-level topics
    List<RoadmapTopic> additionalTopics = generateDefaultTopics(roadmap, request);
    topics.addAll(additionalTopics.stream()
        .filter(t -> !topics.stream().anyMatch(existing -> existing.getTitle().equals(t.getTitle())))
        .collect(Collectors.toList()));

    return topics;
  }

  /**
   * Generate default grade-based topics from curriculum
   */
  private List<RoadmapTopic> generateDefaultTopics(LearningRoadmap roadmap, GenerateRoadmapRequest request) {
    log.info("Generating default topics for grade={}, subject={}",
        request.getGradeLevel(), request.getSubject());

    List<RoadmapTopic> topics = new ArrayList<>();

    // Get lessons for this grade and subject
    List<Lesson> lessons = lessonRepository.findByGradeLevelAndSubjectAndNotDeleted(
        request.getGradeLevel(), request.getSubject());

    int sequenceOrder = 1;
    for (Lesson lesson : lessons) {
      // Get chapters for this lesson
      List<Chapter> chapters = chapterRepository.findByLessonIdAndNotDeleted(lesson.getId());

      for (Chapter chapter : chapters) {
        RoadmapTopic topic = RoadmapTopic.builder()
            .roadmapId(roadmap.getId())
            .chapterId(chapter.getId())
            .title(chapter.getTitle())
            .description(chapter.getDescription())
            .status(TopicStatus.NOT_STARTED)
            .difficulty(determineDifficulty(sequenceOrder, lessons.size()))
            .sequenceOrder(sequenceOrder)
            .priority(0) // Default priority
            .progressPercentage(BigDecimal.ZERO)
            .completedSubTopics(0)
            .totalSubTopics(0)
            .estimatedHours(estimateHoursForTopic(lesson))
            .build();

        topics.add(topic);
        sequenceOrder++;
      }
    }

    log.info("Generated {} default topics", topics.size());
    return topics;
  }

  /**
   * Analyze performance data and identify weak topics
   */
  private List<RoadmapTopic> analyzePerformanceDataForTopics(UUID studentId, String subject) {
    log.info("Analyzing performance data for student={}, subject={}", studentId, subject);

    List<RoadmapTopic> weakTopics = new ArrayList<>();

    // Get grades for student in this subject
    List<Grade> grades = gradeRepository.findByStudentIdAndLessonSubject(studentId, subject);

    if (grades.isEmpty()) {
      log.warn("No performance data found for student={}, subject={}", studentId, subject);
      return weakTopics;
    }

    // Calculate average score per lesson
    Map<UUID, List<Grade>> gradesByLesson = grades.stream()
        .collect(Collectors.groupingBy(Grade::getLessonId));

    // Identify lessons with low average scores
    for (Map.Entry<UUID, List<Grade>> entry : gradesByLesson.entrySet()) {
      BigDecimal avgScore = entry.getValue().stream()
          .map(Grade::getPercentage)
          .reduce(BigDecimal.ZERO, BigDecimal::add)
          .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);

      // If average score is below 70%, mark as weak area
      if (avgScore.compareTo(BigDecimal.valueOf(70)) < 0) {
        Lesson lesson = lessonRepository.findById(entry.getKey()).orElse(null);
        if (lesson != null) {
          List<Chapter> chapters = chapterRepository.findByLessonIdAndNotDeleted(lesson.getId());

          int sequenceOrder = 1;
          for (Chapter chapter : chapters) {
            RoadmapTopic topic = RoadmapTopic.builder()
                .chapterId(chapter.getId())
                .title(chapter.getTitle())
                .description(chapter.getDescription())
                .status(TopicStatus.NOT_STARTED)
                .difficulty(QuestionDifficulty.MEDIUM)
                .sequenceOrder(sequenceOrder)
                .priority(-Math.round((100 - avgScore.intValue()))) // Negative for high priority
                .progressPercentage(BigDecimal.ZERO)
                .completedSubTopics(0)
                .totalSubTopics(0)
                .estimatedHours(2)
                .build();

            weakTopics.add(topic);
            sequenceOrder++;
          }
        }
      }
    }

    log.info("Found {} weak topics for student={}", weakTopics.size(), studentId);
    return weakTopics;
  }

  /**
   * Generate subtopics for a topic
   */
  private void generateSubtopicsForTopic(RoadmapTopic topic) {
    // Create 3-5 subtopics per topic
    for (int i = 1; i <= 3; i++) {
      RoadmapSubtopic subtopic = RoadmapSubtopic.builder()
          .topicId(topic.getId())
          .title(topic.getTitle() + " - Part " + i)
          .description("Learn " + topic.getTitle() + " concept " + i)
          .status(TopicStatus.NOT_STARTED)
          .sequenceOrder(i)
          .progressPercentage(BigDecimal.ZERO)
          .estimatedMinutes(30)
          .build();

      subtopicRepository.save(subtopic);
    }

    topic.setTotalSubTopics(3);
    topicRepository.save(topic);
  }

  /**
   * Link learning materials to a topic
   */
  private void linkMaterials(RoadmapTopic topic, GenerateRoadmapRequest request) {
    if (topic.getChapterId() == null) return;

    Chapter chapter = chapterRepository.findById(topic.getChapterId()).orElse(null);
    if (chapter == null) return;

    // Find and link lessons
    List<Lesson> relevantLessons = lessonRepository.findByChapterIdAndGradeLevel(
        chapter.getId(), request.getGradeLevel());

    int sequenceOrder = 1;
    for (Lesson lesson : relevantLessons.stream().limit(3).collect(Collectors.toList())) {
      TopicLearningMaterial material = TopicLearningMaterial.builder()
          .topicId(topic.getId())
          .lessonId(lesson.getId())
          .resourceTitle(lesson.getTitle())
          .resourceType("LESSON")
          .sequenceOrder(sequenceOrder++)
          .isRequired(true)
          .build();

      materialRepository.save(material);
    }

    // Find and link practice questions
    List<Question> questions = questionRepository.findByChapterIdAndDifficultyOrderByCreatedAt(
        topic.getChapterId(), topic.getDifficulty());

    for (Question question : questions.stream().limit(5).collect(Collectors.toList())) {
      TopicLearningMaterial material = TopicLearningMaterial.builder()
          .topicId(topic.getId())
          .questionId(question.getId())
          .resourceTitle("Question: " + question.getQuestionText())
          .resourceType("PRACTICE")
          .sequenceOrder(sequenceOrder++)
          .isRequired(true)
          .build();

      materialRepository.save(material);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getRoadmapById(UUID roadmapId) {
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToDetailResponse(roadmap);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RoadmapSummaryResponse> getStudentRoadmaps(UUID studentId, Pageable pageable) {
    Page<LearningRoadmap> roadmaps = roadmapRepository.findByStudentIdAndDeletedAtIsNullOrderByCreatedAtDesc(
        studentId, pageable);

    return roadmaps.map(this::mapToSummaryResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getActiveRoadmapBySubject(UUID studentId, String subject) {
    LearningRoadmap roadmap = roadmapRepository
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

  @Override
  @Transactional(readOnly = true)
  public List<RoadmapSummaryResponse> getTeacherAssignedRoadmaps(UUID teacherId) {
    return roadmapRepository.findByTeacherIdAndDeletedAtIsNull(teacherId).stream()
        .map(this::mapToSummaryResponse)
        .collect(Collectors.toList());
  }

  // ============================================================================
  // PROGRESS TRACKING
  // ============================================================================

  @Override
  @Transactional
  public RoadmapTopicResponse updateTopicProgress(UpdateTopicProgressRequest request) {
    log.info("Updating topic progress: topicId={}, status={}, progress={}%",
        request.getTopicId(), request.getStatus(), request.getProgressPercentage());

    RoadmapTopic topic = topicRepository.findById(request.getTopicId())
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    // Update topic status
    TopicStatus previousStatus = topic.getStatus();
    topic.setStatus(request.getStatus());
    topic.setProgressPercentage(request.getProgressPercentage());

    // Mark start time if transitioning to IN_PROGRESS
    if (previousStatus != TopicStatus.IN_PROGRESS && request.getStatus() == TopicStatus.IN_PROGRESS) {
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
  @Transactional
  public RoadmapSubtopicResponse completeSubtopic(CompleteSubtopicRequest request) {
    log.info("Completing subtopic: subtopicId={}", request.getSubtopicId());

    RoadmapSubtopic subtopic = subtopicRepository.findById(request.getSubtopicId())
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    subtopic.setStatus(TopicStatus.COMPLETED);
    subtopic.setProgressPercentage(BigDecimal.valueOf(100));
    subtopic.setCompletedAt(Instant.now());
    subtopic = subtopicRepository.save(subtopic);

    // Update parent topic's completed count
    RoadmapTopic topic = subtopic.getTopic();
    Long completedCount = subtopicRepository.countByTopicIdAndStatus(topic.getId(), TopicStatus.COMPLETED);
    topic.setCompletedSubTopics(completedCount.intValue());

    // Calculate topic progress
    BigDecimal topicProgress = BigDecimal.valueOf(completedCount.doubleValue() / topic.getTotalSubTopics() * 100)
        .setScale(2, RoundingMode.HALF_UP);
    topic.setProgressPercentage(topicProgress);

    // If all subtopics completed, mark topic as completed
    if (completedCount >= topic.getTotalSubTopics()) {
      topic.setStatus(TopicStatus.COMPLETED);
      topic.setProgressPercentage(BigDecimal.valueOf(100));
      topic.setCompletedAt(Instant.now());
    }

    topic = topicRepository.save(topic);

    // Update roadmap progress
    updateRoadmapProgress(topic.getRoadmapId());

    log.info("Subtopic completed successfully: subtopicId={}", subtopic.getId());
    return mapToSubtopicResponse(subtopic);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapTopicResponse getNextTopic(UUID roadmapId) {
    RoadmapTopic nextTopic = topicRepository.findNextTopic(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToTopicResponse(nextTopic);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapStatsResponse getRoadmapStats(UUID roadmapId) {
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId);

    Long easyCount = topics.stream()
        .filter(t -> t.getDifficulty() == QuestionDifficulty.EASY).count();
    Long mediumCount = topics.stream()
        .filter(t -> t.getDifficulty() == QuestionDifficulty.MEDIUM).count();
    Long hardCount = topics.stream()
        .filter(t -> t.getDifficulty() == QuestionDifficulty.HARD).count();
    Long lockedCount = topics.stream()
        .filter(t -> t.getStatus() == TopicStatus.LOCKED).count();

    int totalEstimatedHours = topics.stream()
        .mapToInt(t -> t.getEstimatedHours() != null ? t.getEstimatedHours() : 1)
        .sum();

    BigDecimal avgProgress = topics.stream()
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
    RoadmapTopic topic = topicRepository.findById(topicId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return mapToTopicResponse(topic);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getTopicMaterials(UUID topicId) {
    List<TopicLearningMaterial> materials = materialRepository.findByTopicIdOrderBySequenceOrder(topicId);
    return materials.stream().map(this::mapToMaterialResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getMaterialsByType(UUID topicId, String resourceType) {
    List<TopicLearningMaterial> materials = materialRepository.findByTopicIdAndResourceType(topicId, resourceType);
    return materials.stream().map(this::mapToMaterialResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public TopicMaterialResponse linkMaterialToTopic(UUID topicId, UUID lessonId, UUID questionId,
      String resourceType, Boolean isRequired) {
    log.info("Linking material to topic: topicId={}, type={}", topicId, resourceType);

    RoadmapTopic topic = topicRepository.findById(topicId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    TopicLearningMaterial material = TopicLearningMaterial.builder()
        .topicId(topicId)
        .lessonId(lessonId)
        .questionId(questionId)
        .resourceType(resourceType)
        .isRequired(isRequired != null ? isRequired : true)
        .sequenceOrder((int) materialRepository.findByTopicIdOrderBySequenceOrder(topicId).size() + 1)
        .build();

    material = materialRepository.save(material);
    log.info("Material linked successfully: materialId={}", material.getId());
    return mapToMaterialResponse(material);
  }

  @Override
  @Transactional
  public void removeMaterialFromTopic(UUID materialId) {
    log.info("Removing material from topic: materialId={}", materialId);
    materialRepository.deleteById(materialId);
  }

  // ============================================================================
  // WEAK AREA ANALYSIS
  // ============================================================================

  @Override
  @Transactional(readOnly = true)
  public List<RoadmapTopicResponse> analyzeWeakTopics(UUID studentId, String subject) {
    List<RoadmapTopic> weakTopics = analyzePerformanceDataForTopics(studentId, subject);
    return weakTopics.stream().map(this::mapToTopicResponse).collect(Collectors.toList());
  }

  @Override
  @Transactional
  public RoadmapDetailResponse refreshRoadmapWithPerformanceData(UUID roadmapId) {
    log.info("Refreshing roadmap with performance data: roadmapId={}", roadmapId);

    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    // Re-analyze weak topics
    List<RoadmapTopic> weakTopics = analyzePerformanceDataForTopics(
        roadmap.getStudentId(), roadmap.getSubject());

    // Update priorities for existing topics
    List<RoadmapTopic> existingTopics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId);
    for (RoadmapTopic existingTopic : existingTopics) {
      for (RoadmapTopic weakTopic : weakTopics) {
        if (existingTopic.getTitle().equals(weakTopic.getTitle())) {
          existingTopic.setPriority(weakTopic.getPriority());
          break;
        }
      }
      topicRepository.save(existingTopic);
    }

    log.info("Roadmap refreshed with updated priorities");
    return getRoadmapById(roadmapId);
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

    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    roadmap.setDeletedAt(Instant.now());
    roadmap.setStatus(RoadmapStatus.ARCHIVED);
    roadmapRepository.save(roadmap);

    log.info("Roadmap archived successfully");
  }

  @Override
  @Transactional(readOnly = true)
  public Integer estimateCompletionDays(UUID roadmapId) {
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    return roadmap.getEstimatedCompletionDays();
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal calculateRoadmapProgress(UUID roadmapId) {
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId)
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
      BigDecimal totalProgress = topics.stream()
          .map(RoadmapTopic::getProgressPercentage)
          .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal avgProgress = totalProgress.divide(
          BigDecimal.valueOf(topics.size()), 2, RoundingMode.HALF_UP);

      roadmap.setProgressPercentage(avgProgress);
    }

    // Update completed topics count
    Long completedCount = topics.stream()
        .filter(t -> t.getStatus() == TopicStatus.COMPLETED)
        .count();

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
    BigDecimal estimatedDays = remainingPercent
        .multiply(BigDecimal.valueOf(roadmap.getEstimatedCompletionDays() != null
            ? roadmap.getEstimatedCompletionDays() : 30))
        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

    return estimatedDays.intValue();
  }

  // ============================================================================
  // MAPPING METHODS
  // ============================================================================

  private RoadmapDetailResponse mapToDetailResponse(LearningRoadmap roadmap) {
    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmap.getId());
    List<RoadmapTopicResponse> topicResponses = topics.stream()
        .map(this::mapToTopicResponse)
        .collect(Collectors.toList());

    return RoadmapDetailResponse.builder()
        .id(roadmap.getId())
        .studentId(roadmap.getStudentId())
        .teacherId(roadmap.getTeacherId())
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
    List<RoadmapSubtopic> subtopics = subtopicRepository.findByTopicIdOrderBySequenceOrder(topic.getId());
    List<RoadmapSubtopicResponse> subtopicResponses = subtopics.stream()
        .map(this::mapToSubtopicResponse)
        .collect(Collectors.toList());

    List<TopicLearningMaterial> materials = materialRepository.findByTopicIdOrderBySequenceOrder(topic.getId());
    List<TopicMaterialResponse> materialResponses = materials.stream()
        .map(this::mapToMaterialResponse)
        .collect(Collectors.toList());

    return RoadmapTopicResponse.builder()
        .id(topic.getId())
        .title(topic.getTitle())
        .description(topic.getDescription())
        .status(topic.getStatus())
        .difficulty(topic.getDifficulty())
        .sequenceOrder(topic.getSequenceOrder())
        .priority(topic.getPriority())
        .progressPercentage(topic.getProgressPercentage())
        .completedSubTopics(topic.getCompletedSubTopics())
        .totalSubTopics(topic.getTotalSubTopics())
        .estimatedHours(topic.getEstimatedHours())
        .startedAt(topic.getStartedAt())
        .completedAt(topic.getCompletedAt())
        .subtopics(subtopicResponses)
        .materials(materialResponses)
        .build();
  }

  private RoadmapSubtopicResponse mapToSubtopicResponse(RoadmapSubtopic subtopic) {
    return RoadmapSubtopicResponse.builder()
        .id(subtopic.getId())
        .title(subtopic.getTitle())
        .description(subtopic.getDescription())
        .status(subtopic.getStatus())
        .sequenceOrder(subtopic.getSequenceOrder())
        .progressPercentage(subtopic.getProgressPercentage())
        .estimatedMinutes(subtopic.getEstimatedMinutes())
        .startedAt(subtopic.getStartedAt())
        .completedAt(subtopic.getCompletedAt())
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
        .chapterId(material.getChapterId())
        .build();
  }
}
