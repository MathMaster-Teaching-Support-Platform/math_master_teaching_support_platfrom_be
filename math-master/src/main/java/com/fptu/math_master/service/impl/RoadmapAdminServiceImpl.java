package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreateRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.LinkTopicMaterialsRequest;
import com.fptu.math_master.dto.request.RoadmapEntryQuestionMappingRequest;
import com.fptu.math_master.dto.request.SubmitRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestResultResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.dto.response.TopicMaterialResponse;
import com.fptu.math_master.entity.Answer;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.Mindmap;
import com.fptu.math_master.entity.Question;
import com.fptu.math_master.entity.QuestionTemplate;
import com.fptu.math_master.entity.RoadmapEntryQuestionMapping;
import com.fptu.math_master.entity.RoadmapTopic;
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
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.MindmapRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuestionTemplateRepository;
import com.fptu.math_master.repository.RoadmapEntryQuestionMappingRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.SubmissionRepository;
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
import java.util.UUID;
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
  QuestionRepository questionRepository;
  QuestionTemplateRepository questionTemplateRepository;
  LessonRepository lessonRepository;
  MindmapRepository mindmapRepository;
  TopicLearningMaterialRepository materialRepository;
  RoadmapEntryQuestionMappingRepository roadmapEntryQuestionMappingRepository;
  SubmissionRepository submissionRepository;
  AnswerRepository answerRepository;
  UserRepository userRepository;
  LearningRoadmapService learningRoadmapService;

  @Override
  public RoadmapDetailResponse createRoadmap(CreateAdminRoadmapRequest request) {
    LearningRoadmap roadmap =
        LearningRoadmap.builder()
            .name(request.getName())
            .subject(request.getSubject())
            .gradeLevel(request.getGradeLevel())
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
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    if (request.getSubject() != null && !request.getSubject().isBlank()) {
      roadmap.setSubject(request.getSubject().trim());
    }
    if (request.getGradeLevel() != null && !request.getGradeLevel().isBlank()) {
      roadmap.setGradeLevel(request.getGradeLevel().trim());
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
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null) {
      return;
    }

    roadmap.setDeletedAt(Instant.now());
    roadmap.setStatus(RoadmapStatus.ARCHIVED);
    roadmapRepository.save(roadmap);
  }

  @Override
  public RoadmapTopicResponse addTopic(UUID roadmapId, CreateRoadmapTopicRequest request) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

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
  public List<TopicMaterialResponse> linkTopicMaterials(
      UUID roadmapId, UUID topicId, LinkTopicMaterialsRequest request) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null || roadmap.getGenerationType() != RoadmapGenerationType.ADMIN_TEMPLATE) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    RoadmapTopic topic =
        topicRepository
            .findById(topicId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    if (!topic.getRoadmapId().equals(roadmapId)) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    Question question =
        questionRepository
            .findByIdAndNotDeleted(request.getQuestionId())
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

    UUID lessonId = topic.getLessonId();
    if (lessonId == null && question.getTemplateId() != null) {
      QuestionTemplate template =
          questionTemplateRepository
              .findById(question.getTemplateId())
              .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
      lessonId = template.getLessonId();
    }

    UUID chapterId =
        lessonId == null
            ? null
            : lessonRepository
                .findByIdAndNotDeleted(lessonId)
                .map(l -> l.getChapterId())
                .orElse(null);

    List<TopicLearningMaterial> existing =
        materialRepository.findByQuestionIdAndTopicId(request.getQuestionId(), topicId);
    if (!existing.isEmpty()) {
      materialRepository.deleteAll(existing);
    }

    int sequence =
        request.getStartSequenceOrder() != null
            ? request.getStartSequenceOrder()
            : materialRepository.findByTopicIdOrderBySequenceOrder(topicId).size() + 1;

    List<TopicLearningMaterial> created = new ArrayList<>();

    if (Boolean.TRUE.equals(request.getIncludeQuestions())) {
      created.add(
          materialRepository.save(
              TopicLearningMaterial.builder()
                  .topicId(topicId)
                  .questionId(question.getId())
                  .resourceType("QUESTION")
                  .resourceTitle("Question: " + question.getQuestionText())
                  .sequenceOrder(sequence++)
                  .isRequired(true)
                  .build()));
    }

    if (lessonId != null && Boolean.TRUE.equals(request.getIncludeSlides())) {
      created.add(
          materialRepository.save(
              TopicLearningMaterial.builder()
                  .topicId(topicId)
                  .lessonId(lessonId)
                  .chapterId(chapterId)
                  .resourceType("SLIDE")
                  .resourceTitle("Slides for lesson")
                  .sequenceOrder(sequence++)
                  .isRequired(true)
                  .build()));
    }

    if (lessonId != null && Boolean.TRUE.equals(request.getIncludeDocuments())) {
      created.add(
          materialRepository.save(
              TopicLearningMaterial.builder()
                  .topicId(topicId)
                  .lessonId(lessonId)
                  .chapterId(chapterId)
                  .resourceType("DOCUMENT")
                  .resourceTitle("Document for lesson")
                  .sequenceOrder(sequence++)
                  .isRequired(true)
                  .build()));
    }

    if (lessonId != null && Boolean.TRUE.equals(request.getIncludeMindmaps())) {
      List<Mindmap> mindmaps =
          mindmapRepository.findByLessonIdAndNotDeleted(lessonId, Pageable.ofSize(20)).getContent();
      for (Mindmap mindmap : mindmaps) {
        created.add(
            materialRepository.save(
                TopicLearningMaterial.builder()
                    .topicId(topicId)
                    .mindmapId(mindmap.getId())
                    .lessonId(lessonId)
                    .chapterId(chapterId)
                    .resourceType("MINDMAP")
                    .resourceTitle(mindmap.getTitle())
                    .sequenceOrder(sequence++)
                    .isRequired(false)
                    .build()));
      }
    }

    return created.stream().map(this::mapToMaterialResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TopicMaterialResponse> getTopicMaterialsByQuestion(UUID topicId, UUID questionId) {
    return materialRepository.findByQuestionIdAndTopicId(questionId, topicId).stream()
        .map(this::mapToMaterialResponse)
        .toList();
  }

  @Override
  public void configureEntryTest(UUID roadmapId, CreateRoadmapEntryTestRequest request) {
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null || roadmap.getGenerationType() != RoadmapGenerationType.ADMIN_TEMPLATE) {
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
    LearningRoadmap roadmap =
        roadmapRepository
            .findById(roadmapId)
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    if (roadmap.getDeletedAt() != null) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

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
      totalByTopic.merge(topicId, 1, Integer::sum);

      Answer answer = answerByQuestionId.get(mapping.getQuestionId());
      if (answer != null && Boolean.TRUE.equals(answer.getIsCorrect())) {
        correctByTopic.merge(topicId, 1, Integer::sum);
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
}
