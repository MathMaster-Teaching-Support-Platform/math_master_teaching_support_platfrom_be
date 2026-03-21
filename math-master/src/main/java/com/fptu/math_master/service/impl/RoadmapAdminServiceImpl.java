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
import com.fptu.math_master.util.SecurityUtils;
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
    UUID teacherId = SecurityUtils.getCurrentUserId();

    LearningRoadmap roadmap =
        LearningRoadmap.builder()
            .name(request.getName())
            .subjectId(subject.getId())
            .subject(subject.getName())
            .gradeLevel(resolveGradeLevel(subject))
            .description(request.getDescription())
            .estimatedCompletionDays(request.getEstimatedDays())
            .teacherId(teacherId)
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
    UUID teacherId = SecurityUtils.getCurrentUserId();

    return roadmapRepository
        .findAdminTemplates(teacherId, normalizedName, pageable)
        .map(this::mapToSummaryResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RoadmapSummaryResponse> getPublishedRoadmaps(String name, Pageable pageable) {
    String normalizedName = name == null ? null : name.trim();

    return roadmapRepository.findPublishedTemplates(normalizedName, pageable).map(this::mapToSummaryResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getRoadmap(UUID roadmapId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

    return learningRoadmapService.getRoadmapById(roadmapId);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getPublishedRoadmap(UUID roadmapId) {
    getPublishedRoadmapOrThrow(roadmapId);

    return learningRoadmapService.getRoadmapById(roadmapId);
  }

  @Override
  public RoadmapDetailResponse updateRoadmap(UUID roadmapId, UpdateAdminRoadmapRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

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
  public RoadmapDetailResponse publishRoadmap(UUID roadmapId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

    if (roadmap.getStatus() != RoadmapStatus.PUBLISHED) {
      roadmap.setStatus(RoadmapStatus.PUBLISHED);
      roadmapRepository.save(roadmap);
    }

    return learningRoadmapService.getRoadmapById(roadmapId);
  }

  @Override
  public void softDeleteRoadmap(UUID roadmapId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

    roadmap.setDeletedAt(Instant.now());
    roadmap.setStatus(RoadmapStatus.ARCHIVED);
    roadmapRepository.save(roadmap);
  }

  @Override
  public RoadmapTopicResponse addTopic(UUID roadmapId, CreateRoadmapTopicRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

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
            .progressPercentage(BigDecimal.ZERO)
            .passThresholdPercentage(
                request.getPassThresholdPercentage() != null
                    ? request.getPassThresholdPercentage()
                    : BigDecimal.valueOf(70))
            .build();

    topic = topicRepository.save(topic);

    UUID teacherId = SecurityUtils.getCurrentUserId();
    upsertTopicLessonMaterials(topic.getId(), request.getLessonIds());
    upsertTopicSlideMaterials(topic.getId(), request.getSlideLessonIds(), teacherId);
    upsertTopicAssessmentMaterials(topic.getId(), request.getAssessmentIds(), teacherId);
    upsertTopicLessonPlanMaterials(topic.getId(), request.getLessonPlanIds(), teacherId);
    upsertTopicMindmapMaterials(topic.getId(), request.getMindmapIds(), teacherId);

    long totalTopics = countActiveTopics(roadmapId);
    roadmap.setTotalTopicsCount((int) totalTopics);
    roadmapRepository.save(roadmap);

    return learningRoadmapService.getTopicDetails(topic.getId());
  }

  @Override
  public RoadmapTopicResponse updateTopic(UUID roadmapId, UUID topicId, UpdateRoadmapTopicRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

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

    UUID teacherId = SecurityUtils.getCurrentUserId();
    if (request.getLessonIds() != null) {
      upsertTopicLessonMaterials(topicId, request.getLessonIds());
    }
    if (request.getSlideLessonIds() != null) {
      upsertTopicSlideMaterials(topicId, request.getSlideLessonIds(), teacherId);
    }
    if (request.getAssessmentIds() != null) {
      upsertTopicAssessmentMaterials(topicId, request.getAssessmentIds(), teacherId);
    }
    if (request.getLessonPlanIds() != null) {
      upsertTopicLessonPlanMaterials(topicId, request.getLessonPlanIds(), teacherId);
    }
    if (request.getMindmapIds() != null) {
      upsertTopicMindmapMaterials(topicId, request.getMindmapIds(), teacherId);
    }

    return learningRoadmapService.getTopicDetails(topicId);
  }

  @Override
  public void softDeleteTopic(UUID roadmapId, UUID topicId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

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
    getActiveTopicInPublishedRoadmapOrThrow(topicId);
    return learningRoadmapService.getTopicMaterials(topicId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getMaterialsByType(UUID topicId, String resourceType) {
    getActiveTopicInPublishedRoadmapOrThrow(topicId);
    return learningRoadmapService.getMaterialsByType(topicId, resourceType);
  }

  @Override
  public void configureEntryTest(UUID roadmapId, CreateRoadmapEntryTestRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    validateTeacherOwnership(roadmap);

    if (roadmap.getGenerationType() != RoadmapGenerationType.ADMIN_TEMPLATE) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    if (assessmentRepository.findById(request.getAssessmentId()).isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    roadmapEntryQuestionMappingRepository.deleteByRoadmapId(roadmapId);

    for (RoadmapEntryQuestionMappingRequest mappingRequest : request.getMappings()) {
      if (questionRepository.findByIdAndNotDeleted(mappingRequest.getQuestionId()).isEmpty()) {
        throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
      }

      RoadmapTopic topic =
          topicRepository
              .findById(mappingRequest.getRoadmapTopicId())
              .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

      if (!topic.getRoadmapId().equals(roadmapId)) {
        throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
      }

      roadmapEntryQuestionMappingRepository.save(
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(request.getAssessmentId())
              .questionId(mappingRequest.getQuestionId())
              .roadmapTopicId(mappingRequest.getRoadmapTopicId())
              .orderIndex(mappingRequest.getOrderIndex())
              .weight(mappingRequest.getWeight())
              .build());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestResultResponse submitEntryTest(
      UUID studentId, UUID roadmapId, SubmitRoadmapEntryTestRequest request) {
    getPublishedRoadmapOrThrow(roadmapId);

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

    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId);
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

    return RoadmapEntryTestResultResponse.builder()
        .roadmapId(roadmapId)
        .submissionId(submission.getId())
        .suggestedTopicId(suggestedTopic.getId())
        .evaluatedQuestions(mappings.size())
        .thresholdPercentage(70)
        .evaluatedAt(Instant.now())
        .build();
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

  private LearningRoadmap getPublishedRoadmapOrThrow(UUID roadmapId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    if (roadmap.getStatus() != RoadmapStatus.PUBLISHED) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }
    return roadmap;
  }

  private RoadmapTopic getActiveTopicInPublishedRoadmapOrThrow(UUID topicId) {
    RoadmapTopic topic =
        topicRepository.findById(topicId).orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (topic.getDeletedAt() != null) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    getPublishedRoadmapOrThrow(topic.getRoadmapId());
    return topic;
  }

  private void validateTeacherOwnership(LearningRoadmap roadmap) {
    UUID teacherId = SecurityUtils.getCurrentUserId();
    if (roadmap.getTeacherId() == null || !roadmap.getTeacherId().equals(teacherId)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
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

  private void upsertTopicSlideMaterials(UUID topicId, List<UUID> slideLessonIds, UUID teacherId) {
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

      boolean hasTeacherPlan =
          lessonPlanRepository.existsByLessonIdAndTeacherIdAndNotDeleted(lessonId, teacherId);
      if (!hasTeacherPlan) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }

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

  private void upsertTopicAssessmentMaterials(
      UUID topicId, List<UUID> assessmentIds, UUID teacherId) {
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

      if (!teacherId.equals(assessment.getTeacherId())) {
        throw new AppException(ErrorCode.ASSESSMENT_ACCESS_DENIED);
      }

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

  private void upsertTopicLessonPlanMaterials(
      UUID topicId, List<UUID> lessonPlanIds, UUID teacherId) {
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

      if (!teacherId.equals(lessonPlan.getTeacherId())) {
        throw new AppException(ErrorCode.UNAUTHORIZED);
      }

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

  private void upsertTopicMindmapMaterials(UUID topicId, List<UUID> mindmapIds, UUID teacherId) {
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

      if (!teacherId.equals(mindmap.getTeacherId())) {
        throw new AppException(ErrorCode.MINDMAP_ACCESS_DENIED);
      }

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
