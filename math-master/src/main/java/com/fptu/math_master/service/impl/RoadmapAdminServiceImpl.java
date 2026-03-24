package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreateRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.RoadmapEntryQuestionMappingRequest;
import com.fptu.math_master.dto.request.SubmitRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.UpdateRoadmapTopicRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestResultResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.dto.response.TopicMaterialResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.Answer;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.LessonPlan;
import com.fptu.math_master.entity.Mindmap;
import com.fptu.math_master.entity.RoadmapEntryQuestionMapping;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.entity.TopicLearningMaterial;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AnswerRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.LessonPlanRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.MindmapRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.RoadmapEntryQuestionMappingRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.TopicLearningMaterialRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.LearningRoadmapService;
import com.fptu.math_master.service.RoadmapAdminService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoadmapAdminServiceImpl implements RoadmapAdminService {

  LearningRoadmapRepository roadmapRepository;
  AssessmentRepository assessmentRepository;
  AssessmentQuestionRepository assessmentQuestionRepository;
  RoadmapTopicRepository topicRepository;
  MindmapRepository mindmapRepository;
  QuestionRepository questionRepository;
  LessonRepository lessonRepository;
  LessonPlanRepository lessonPlanRepository;
  TopicLearningMaterialRepository materialRepository;
  RoadmapEntryQuestionMappingRepository roadmapEntryQuestionMappingRepository;
  SubmissionRepository submissionRepository;
  SubjectRepository subjectRepository;
  AnswerRepository answerRepository;
  UserRepository userRepository;
  LearningRoadmapService learningRoadmapService;

  @Override
  public RoadmapDetailResponse createRoadmap(CreateAdminRoadmapRequest request) {
    Subject subject = resolveSubject(request.getSubjectId());

    LearningRoadmap roadmap =
        LearningRoadmap.builder()
            .name(request.getName())
            .subjectId(subject.getId())
            .subject(subject.getName())
            .gradeLevel(resolveGradeLevel(subject))
            .description(request.getDescription())
            .estimatedCompletionDays(request.getEstimatedDays())
            .generationType(RoadmapGenerationType.ADMIN_TEMPLATE)
            .status(RoadmapStatus.GENERATED)
            .progressPercentage(BigDecimal.ZERO)
            .completedTopicsCount(0)
            .totalTopicsCount(0)
            .build();

    roadmap = roadmapRepository.save(roadmap);
    return learningRoadmapService.getRoadmapById(roadmap.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RoadmapSummaryResponse> getAllRoadmaps(String name, Pageable pageable) {
    String normalizedName = name == null ? null : name.trim();

    return roadmapRepository.findAdminTemplates(normalizedName, pageable).map(this::mapToSummaryResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getRoadmap(UUID roadmapId) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    return learningRoadmapService.getRoadmapById(roadmapId);
  }

  @Override
  public RoadmapDetailResponse updateRoadmap(UUID roadmapId, UpdateAdminRoadmapRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    if (request.getSubjectId() != null) {
      Subject subject = resolveSubject(request.getSubjectId());
      roadmap.setSubjectId(subject.getId());
      roadmap.setSubject(subject.getName());
      roadmap.setGradeLevel(resolveGradeLevel(subject));
    }
    if (request.getDescription() != null) {
      roadmap.setDescription(request.getDescription());
    }
    if (request.getEstimatedCompletionDays() != null) {
      roadmap.setEstimatedCompletionDays(request.getEstimatedCompletionDays());
    }
    if (request.getStatus() != null) {
      roadmap.setStatus(request.getStatus());
    }

    roadmapRepository.save(roadmap);
    return learningRoadmapService.getRoadmapById(roadmapId);
  }

  @Override
  public void softDeleteRoadmap(UUID roadmapId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    roadmap.setDeletedAt(Instant.now());
    roadmap.setStatus(RoadmapStatus.ARCHIVED);
    roadmapRepository.save(roadmap);
  }

  @Override
  public RoadmapTopicResponse addTopic(UUID roadmapId, CreateRoadmapTopicRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    RoadmapTopic topic =
        RoadmapTopic.builder()
            .roadmapId(roadmapId)
        .lessonId(null)
            .topicAssessmentId(request.getTopicAssessmentId())
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getSequenceOrder() == 1 ? TopicStatus.NOT_STARTED : TopicStatus.LOCKED)
            .difficulty(request.getDifficulty())
            .sequenceOrder(request.getSequenceOrder())
            .priority(request.getPriority())
            .estimatedHours(request.getEstimatedHours())
        .mark(request.getMark())
            .progressPercentage(BigDecimal.ZERO)
            .passThresholdPercentage(
                request.getPassThresholdPercentage() != null
                    ? request.getPassThresholdPercentage()
                    : BigDecimal.valueOf(70))
            .build();

    topic = topicRepository.save(topic);

    upsertTopicLessonMaterials(topic.getId(), request.getLessonIds());
  upsertTopicSlideMaterials(topic.getId(), request.getSlideLessonIds());
  upsertTopicAssessmentMaterials(topic.getId(), request.getAssessmentIds());
  upsertTopicLessonPlanMaterials(topic.getId(), request.getLessonPlanIds());
  upsertTopicMindmapMaterials(topic.getId(), request.getMindmapIds());

    long totalTopics = countActiveTopics(roadmapId);
    roadmap.setTotalTopicsCount((int) totalTopics);
    roadmapRepository.save(roadmap);

    return learningRoadmapService.getTopicDetails(topic.getId());
  }

  @Override
  public RoadmapTopicResponse updateTopic(UUID roadmapId, UUID topicId, UpdateRoadmapTopicRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    RoadmapTopic topic =
        topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (topic.getDeletedAt() != null || !topic.getRoadmapId().equals(roadmapId)) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    if (request.getTitle() != null) {
      topic.setTitle(request.getTitle());
    }
    if (request.getDescription() != null) {
      topic.setDescription(request.getDescription());
    }
    if (request.getSequenceOrder() != null) {
      topic.setSequenceOrder(request.getSequenceOrder());
    }
    if (request.getPriority() != null) {
      topic.setPriority(request.getPriority());
    }
    if (request.getEstimatedHours() != null) {
      topic.setEstimatedHours(request.getEstimatedHours());
    }
    if (request.getMark() != null) {
      topic.setMark(request.getMark());
    }
    if (request.getTopicAssessmentId() != null) {
      topic.setTopicAssessmentId(request.getTopicAssessmentId());
    }
    if (request.getPassThresholdPercentage() != null) {
      topic.setPassThresholdPercentage(request.getPassThresholdPercentage());
    }
    if (request.getDifficulty() != null) {
      topic.setDifficulty(request.getDifficulty());
    }
    if (request.getStatus() != null) {
      topic.setStatus(request.getStatus());
    }

    topicRepository.save(topic);

    if (request.getLessonIds() != null) {
      upsertTopicLessonMaterials(topicId, request.getLessonIds());
    }
    if (request.getSlideLessonIds() != null) {
      upsertTopicSlideMaterials(topicId, request.getSlideLessonIds());
    }
    if (request.getAssessmentIds() != null) {
      upsertTopicAssessmentMaterials(topicId, request.getAssessmentIds());
    }
    if (request.getLessonPlanIds() != null) {
      upsertTopicLessonPlanMaterials(topicId, request.getLessonPlanIds());
    }
    if (request.getMindmapIds() != null) {
      upsertTopicMindmapMaterials(topicId, request.getMindmapIds());
    }

    return learningRoadmapService.getTopicDetails(topicId);
  }

  @Override
  public void softDeleteTopic(UUID roadmapId, UUID topicId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    RoadmapTopic topic =
        topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (!topic.getRoadmapId().equals(roadmapId)) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    if (topic.getDeletedAt() != null) {
      return;
    }

    topic.setDeletedAt(Instant.now());
    topicRepository.save(topic);

    roadmapEntryQuestionMappingRepository.deleteByRoadmapTopicId(topicId);

    long totalTopics = countActiveTopics(roadmapId);
    roadmap.setTotalTopicsCount((int) totalTopics);
    roadmapRepository.save(roadmap);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getTopicMaterials(UUID topicId) {
    return learningRoadmapService.getTopicMaterials(topicId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getMaterialsByType(UUID topicId, String resourceType) {
    return learningRoadmapService.getMaterialsByType(topicId, resourceType);
  }

  @Override
  public void configureEntryTest(UUID roadmapId, CreateRoadmapEntryTestRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    if (roadmap.getGenerationType() != RoadmapGenerationType.ADMIN_TEMPLATE) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    Assessment assessment =
        assessmentRepository
            .findById(request.getAssessmentId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    // Clear existing mappings for this roadmap
    roadmapEntryQuestionMappingRepository.deleteByRoadmapId(roadmapId);

    // Get all questions from the assessment, ordered by index
    List<AssessmentQuestion> assessmentQuestions =
        assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(request.getAssessmentId());

    if (assessmentQuestions.isEmpty()) {
      throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
    }

    // Get all topics for this roadmap
    List<RoadmapTopic> topics =
        topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId).stream()
            .filter(topic -> topic.getDeletedAt() == null)
            .toList();

    if (topics.isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    // Create mappings: each assessment question maps to each roadmap topic
    // This allows the entry test to assess readiness for all topics
    int orderIndex = 0;
    for (AssessmentQuestion assessmentQuestion : assessmentQuestions) {
      for (RoadmapTopic topic : topics) {
        roadmapEntryQuestionMappingRepository.save(
            RoadmapEntryQuestionMapping.builder()
                .roadmapId(roadmapId)
                .assessmentId(request.getAssessmentId())
                .questionId(assessmentQuestion.getQuestionId())
                .roadmapTopicId(topic.getId())
                .orderIndex(orderIndex)
                .weight(BigDecimal.ONE)
                .build());
      }
      orderIndex++;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestResultResponse submitEntryTest(
      UUID studentId, UUID roadmapId, SubmitRoadmapEntryTestRequest request) {
    getActiveRoadmapOrThrow(roadmapId);

    Submission submission =
        submissionRepository
            .findById(request.getSubmissionId())
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (!submission.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    List<RoadmapEntryQuestionMapping> mappings =
        roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId);
    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    UUID configuredAssessmentId = mappings.get(0).getAssessmentId();
    if (!configuredAssessmentId.equals(submission.getAssessmentId())) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    List<Answer> answers = answerRepository.findBySubmissionId(submission.getId());
    Map<UUID, Answer> answerByQuestionId = new HashMap<>();
    for (Answer answer : answers) {
      answerByQuestionId.put(answer.getQuestionId(), answer);
    }

    Map<UUID, Integer> totalByTopic = new HashMap<>();
    Map<UUID, Integer> correctByTopic = new HashMap<>();

    for (RoadmapEntryQuestionMapping mapping : mappings) {
      UUID topicId = mapping.getRoadmapTopicId();
      totalByTopic.merge(
          topicId,
          1,
          (left, right) -> Integer.valueOf((left == null ? 0 : left) + (right == null ? 0 : right)));

      Answer answer = answerByQuestionId.get(mapping.getQuestionId());
      if (answer != null && Boolean.TRUE.equals(answer.getIsCorrect())) {
        correctByTopic.merge(
            topicId,
            1,
            (left, right) ->
                Integer.valueOf((left == null ? 0 : left) + (right == null ? 0 : right)));
      }
    }

    List<RoadmapTopic> topics =
        topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId).stream()
            .filter(topic -> topic.getDeletedAt() == null)
            .toList();
    if (topics.isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    double scoreOnTen = computeScoreOnTen(submission, mappings, answerByQuestionId);

    RoadmapTopic suggestedTopic = topics.get(topics.size() - 1);
    boolean hasMarkConfig = topics.stream().anyMatch(topic -> topic.getMark() != null);

    if (hasMarkConfig) {
      for (RoadmapTopic topic : topics) {
        if (topic.getMark() != null && scoreOnTen <= topic.getMark()) {
          suggestedTopic = topic;
          break;
        }
      }
    } else {
      for (RoadmapTopic topic : topics) {
        int total = totalByTopic.getOrDefault(topic.getId(), 0);
        int correct = correctByTopic.getOrDefault(topic.getId(), 0);
        double mastery = total == 0 ? 0 : (correct * 100.0 / total);

        if (mastery < 70.0) {
          suggestedTopic = topic;
          break;
        }
      }
    }

    return RoadmapEntryTestResultResponse.builder()
        .roadmapId(roadmapId)
        .submissionId(submission.getId())
        .suggestedTopicId(suggestedTopic.getId())
        .scoreOnTen(scoreOnTen)
        .evaluatedQuestions(mappings.size())
        .thresholdPercentage(70)
        .evaluatedAt(Instant.now())
        .build();
  }

  private double computeScoreOnTen(
      Submission submission,
      List<RoadmapEntryQuestionMapping> mappings,
      Map<UUID, Answer> answerByQuestionId) {
    double scoreOnTen;

    if (submission.getFinalScore() != null
        && submission.getMaxScore() != null
        && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
      scoreOnTen =
          submission.getFinalScore().doubleValue() / submission.getMaxScore().doubleValue() * 10.0;
    } else if (submission.getScore() != null
        && submission.getMaxScore() != null
        && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
      scoreOnTen = submission.getScore().doubleValue() / submission.getMaxScore().doubleValue() * 10.0;
    } else if (submission.getPercentage() != null) {
      scoreOnTen = submission.getPercentage().doubleValue() / 10.0;
    } else {
      int correct = 0;
      for (RoadmapEntryQuestionMapping mapping : mappings) {
        Answer answer = answerByQuestionId.get(mapping.getQuestionId());
        if (answer != null && Boolean.TRUE.equals(answer.getIsCorrect())) {
          correct++;
        }
      }
      scoreOnTen = mappings.isEmpty() ? 0.0 : (correct * 10.0 / mappings.size());
    }

    if (scoreOnTen < 0.0) {
      scoreOnTen = 0.0;
    }
    if (scoreOnTen > 10.0) {
      scoreOnTen = 10.0;
    }

    return BigDecimal.valueOf(scoreOnTen).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
  }

  private RoadmapSummaryResponse mapToSummaryResponse(LearningRoadmap roadmap) {
    String studentName =
      roadmap.getStudentId() == null
        ? "ALL_STUDENTS"
        : userRepository.findById(roadmap.getStudentId()).map(User::getFullName).orElse("Unknown");

    return RoadmapSummaryResponse.builder()
        .id(roadmap.getId())
      .name(roadmap.getName())
        .studentId(roadmap.getStudentId())
        .subjectId(roadmap.getSubjectId())
        .studentName(studentName)
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

  private LearningRoadmap getActiveRoadmapOrThrow(UUID roadmapId) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    return roadmap;
  }

  private long countActiveTopics(UUID roadmapId) {
    return topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId).stream()
        .filter(topic -> topic.getDeletedAt() == null)
        .count();
  }

  private void upsertTopicLessonMaterials(UUID topicId, List<UUID> lessonIds) {
    materialRepository.deleteByTopicIdAndResourceType(topicId, "LESSON");

    if (lessonIds == null || lessonIds.isEmpty()) {
      return;
    }

    Set<UUID> uniqueLessonIds = lessonIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());

    long existingLessonCount =
        lessonRepository.findAllById(uniqueLessonIds).stream()
            .filter(lesson -> lesson.getDeletedAt() == null)
            .count();
    if (existingLessonCount != uniqueLessonIds.size()) {
      throw new AppException(ErrorCode.LESSON_NOT_FOUND);
    }

    int sequenceOrder = nextSequenceOrder(topicId);
    List<TopicLearningMaterial> materials = new ArrayList<>();
    for (UUID lessonId : lessonIds) {
      if (lessonId == null) {
        continue;
      }

      UUID chapterId =
          lessonRepository
              .findByIdAndNotDeleted(lessonId)
              .map(com.fptu.math_master.entity.Lesson::getChapterId)
              .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

      materials.add(
          TopicLearningMaterial.builder()
              .topicId(topicId)
              .lessonId(lessonId)
              .chapterId(chapterId)
              .resourceType("LESSON")
              .resourceTitle("Lesson")
              .sequenceOrder(sequenceOrder++)
              .isRequired(true)
              .build());
    }

    if (!materials.isEmpty()) {
      materialRepository.saveAll(materials);
    }
  }

  private void upsertTopicSlideMaterials(UUID topicId, List<UUID> slideLessonIds) {
    materialRepository.deleteByTopicIdAndResourceType(topicId, "SLIDE");

    if (slideLessonIds == null || slideLessonIds.isEmpty()) {
      return;
    }

    int sequenceOrder = nextSequenceOrder(topicId);
    List<TopicLearningMaterial> materials = new ArrayList<>();
    for (UUID lessonId : slideLessonIds) {
      if (lessonId == null) {
        continue;
      }

      UUID chapterId =
          lessonRepository
              .findByIdAndNotDeleted(lessonId)
              .map(com.fptu.math_master.entity.Lesson::getChapterId)
              .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

      materials.add(
          TopicLearningMaterial.builder()
              .topicId(topicId)
              .lessonId(lessonId)
              .chapterId(chapterId)
              .resourceType("SLIDE")
              .resourceTitle("Slide")
              .sequenceOrder(sequenceOrder++)
              .isRequired(true)
              .build());
    }

    if (!materials.isEmpty()) {
      materialRepository.saveAll(materials);
    }
  }

  private void upsertTopicAssessmentMaterials(UUID topicId, List<UUID> assessmentIds) {
    materialRepository.deleteByTopicIdAndResourceType(topicId, "ASSESSMENT");

    if (assessmentIds == null || assessmentIds.isEmpty()) {
      return;
    }

    int sequenceOrder = nextSequenceOrder(topicId);
    List<TopicLearningMaterial> materials = new ArrayList<>();
    for (UUID assessmentId : assessmentIds) {
      if (assessmentId == null) {
        continue;
      }

      Assessment assessment =
          assessmentRepository
              .findByIdAndNotDeleted(assessmentId)
              .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

      materials.add(
          TopicLearningMaterial.builder()
              .topicId(topicId)
              .assessmentId(assessmentId)
              .resourceType("ASSESSMENT")
              .resourceTitle("Assessment")
              .sequenceOrder(sequenceOrder++)
              .isRequired(true)
              .build());
    }

    if (!materials.isEmpty()) {
      materialRepository.saveAll(materials);
    }
  }

  private void upsertTopicLessonPlanMaterials(UUID topicId, List<UUID> lessonPlanIds) {
    materialRepository.deleteByTopicIdAndResourceType(topicId, "LESSON_PLAN");

    if (lessonPlanIds == null || lessonPlanIds.isEmpty()) {
      return;
    }

    int sequenceOrder = nextSequenceOrder(topicId);
    List<TopicLearningMaterial> materials = new ArrayList<>();
    for (UUID lessonPlanId : lessonPlanIds) {
      if (lessonPlanId == null) {
        continue;
      }

      LessonPlan lessonPlan =
          lessonPlanRepository
              .findByIdAndNotDeleted(lessonPlanId)
              .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

      UUID chapterId =
          lessonRepository
              .findByIdAndNotDeleted(lessonPlan.getLessonId())
              .map(com.fptu.math_master.entity.Lesson::getChapterId)
              .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

      materials.add(
          TopicLearningMaterial.builder()
              .topicId(topicId)
              .lessonId(lessonPlan.getLessonId())
              .chapterId(chapterId)
              .resourceType("LESSON_PLAN")
              .resourceTitle("Lesson Plan")
              .sequenceOrder(sequenceOrder++)
              .isRequired(true)
              .build());
    }

    if (!materials.isEmpty()) {
      materialRepository.saveAll(materials);
    }
  }

  private void upsertTopicMindmapMaterials(UUID topicId, List<UUID> mindmapIds) {
    materialRepository.deleteByTopicIdAndResourceType(topicId, "MINDMAP");

    if (mindmapIds == null || mindmapIds.isEmpty()) {
      return;
    }

    int sequenceOrder = nextSequenceOrder(topicId);
    List<TopicLearningMaterial> materials = new ArrayList<>();
    for (UUID mindmapId : mindmapIds) {
      if (mindmapId == null) {
        continue;
      }

      Mindmap mindmap =
          mindmapRepository
              .findByIdAndNotDeleted(mindmapId)
              .orElseThrow(() -> new AppException(ErrorCode.MINDMAP_NOT_FOUND));

      materials.add(
          TopicLearningMaterial.builder()
              .topicId(topicId)
              .mindmapId(mindmapId)
              .lessonId(mindmap.getLessonId())
              .resourceType("MINDMAP")
              .resourceTitle("Mindmap")
              .sequenceOrder(sequenceOrder++)
              .isRequired(true)
              .build());
    }

    if (!materials.isEmpty()) {
      materialRepository.saveAll(materials);
    }
  }

  private int nextSequenceOrder(UUID topicId) {
    Integer maxSequence = materialRepository.findMaxSequenceOrderByTopicId(topicId);
    return (maxSequence == null ? 0 : maxSequence) + 1;
  }

  private Subject resolveSubject(UUID subjectId) {
    Subject subject =
        subjectRepository
            .findById(subjectId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));

    if (subject.getDeletedAt() != null) {
      throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
    }

    return subject;
  }

  private String resolveGradeLevel(Subject subject) {
    if (subject.getSchoolGrade() == null
        || subject.getSchoolGrade().getDeletedAt() != null
        || subject.getSchoolGrade().getName() == null
        || subject.getSchoolGrade().getName().isBlank()) {
      throw new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND);
    }

    return subject.getSchoolGrade().getName().trim();
  }
}
