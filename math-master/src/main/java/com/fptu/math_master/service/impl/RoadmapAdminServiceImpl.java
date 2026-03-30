package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.CreateRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.CreateRoadmapTopicRequest;
import com.fptu.math_master.dto.request.RoadmapEntryTestAnswerRequest;
import com.fptu.math_master.dto.request.RoadmapEntryTestFlagRequest;
import com.fptu.math_master.dto.request.StartAssessmentRequest;
import com.fptu.math_master.dto.request.SubmitAssessmentRequest;
import com.fptu.math_master.dto.request.SubmitRoadmapEntryTestRequest;
import com.fptu.math_master.dto.request.UpdateAdminRoadmapRequest;
import com.fptu.math_master.dto.request.UpdateRoadmapTopicRequest;
import com.fptu.math_master.dto.response.AnswerAckResponse;
import com.fptu.math_master.dto.response.AttemptStartResponse;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestActiveAttemptResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestInfoResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestProgressResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestResultResponse;
import com.fptu.math_master.dto.response.RoadmapResourceOptionResponse;
import com.fptu.math_master.dto.response.RoadmapEntryTestSnapshotResponse;
import com.fptu.math_master.dto.response.RoadmapSummaryResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.dto.response.StudentAssessmentResponse;
import com.fptu.math_master.dto.response.TopicMaterialResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.AssessmentQuestion;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.LearningRoadmap;
import com.fptu.math_master.entity.LessonPlan;
import com.fptu.math_master.entity.Mindmap;
import com.fptu.math_master.entity.QuizAttempt;
import com.fptu.math_master.entity.RoadmapEntryQuestionMapping;
import com.fptu.math_master.entity.RoadmapTopic;
import com.fptu.math_master.entity.Subject;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.entity.TopicLearningMaterial;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.RoadmapGenerationType;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.LearningRoadmapRepository;
import com.fptu.math_master.repository.LessonPlanRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.MindmapRepository;
import com.fptu.math_master.repository.QuizAttemptRepository;
import com.fptu.math_master.repository.RoadmapEntryQuestionMappingRepository;
import com.fptu.math_master.repository.RoadmapTopicRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.repository.SubjectRepository;
import com.fptu.math_master.repository.TopicLearningMaterialRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GradingService;
import com.fptu.math_master.service.LearningRoadmapService;
import com.fptu.math_master.service.RoadmapAdminService;
import com.fptu.math_master.service.StudentAssessmentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
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
  LessonRepository lessonRepository;
  LessonPlanRepository lessonPlanRepository;
  TopicLearningMaterialRepository materialRepository;
  RoadmapEntryQuestionMappingRepository roadmapEntryQuestionMappingRepository;
  SubmissionRepository submissionRepository;
  QuizAttemptRepository quizAttemptRepository;
  SubjectRepository subjectRepository;
  CourseRepository courseRepository;
  UserRepository userRepository;
  LearningRoadmapService learningRoadmapService;
  StudentAssessmentService studentAssessmentService;
  GradingService gradingService;

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
    Set<Course> topicCourses = resolveTopicCourses(request.getCourseIds(), roadmap);

    RoadmapTopic topic =
        RoadmapTopic.builder()
            .roadmapId(roadmapId)
        .lessonId(null)
            .topicAssessmentId(request.getTopicAssessmentId())
        .courses(topicCourses)
            .title(request.getTitle())
            .description(request.getDescription())
            .status(request.getSequenceOrder() == 1 ? TopicStatus.NOT_STARTED : TopicStatus.LOCKED)
            .difficulty(request.getDifficulty())
            .sequenceOrder(request.getSequenceOrder())
        .mark(request.getMark())
            .progressPercentage(BigDecimal.ZERO)
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
    if (request.getMark() != null) {
      topic.setMark(request.getMark());
    }
    if (request.getTopicAssessmentId() != null) {
      topic.setTopicAssessmentId(request.getTopicAssessmentId());
    }
    if (request.getCourseIds() != null) {
      topic.setCourses(resolveTopicCourses(request.getCourseIds(), roadmap));
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
  @Transactional(readOnly = true)
  public List<RoadmapResourceOptionResponse> searchResourceOptions(
      String type, UUID chapterId, UUID lessonId, String name) {
    String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    String normalizedName = name == null ? null : name.trim();

    switch (normalizedType) {
      case "LESSON":
      case "TEMPLATE_SLIDE":
        return searchLessonBasedOptions(normalizedType, chapterId, lessonId, normalizedName);
      case "MINDMAP":
        return searchMindmapOptions(lessonId, normalizedName);
      case "LESSON_PLAN":
        return searchLessonPlanOptions(lessonId, normalizedName);
      case "ASSESSMENT":
        return searchAssessmentOptions(normalizedName);
      default:
        throw new AppException(ErrorCode.INVALID_KEY);
    }
  }

  @Override
  public void configureEntryTest(UUID roadmapId, CreateRoadmapEntryTestRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    if (roadmap.getGenerationType() != RoadmapGenerationType.ADMIN_TEMPLATE) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    assessmentRepository
        .findById(request.getAssessmentId())
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

    roadmapEntryQuestionMappingRepository.deleteByRoadmapId(roadmapId);

    List<AssessmentQuestion> assessmentQuestions =
        assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(request.getAssessmentId());

    if (assessmentQuestions.isEmpty()) {
      throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
    }

    List<RoadmapEntryQuestionMapping> mappings = new ArrayList<>();
    for (int i = 0; i < assessmentQuestions.size(); i++) {
      mappings.add(
          RoadmapEntryQuestionMapping.builder()
              .roadmapId(roadmapId)
              .assessmentId(request.getAssessmentId())
              .questionId(assessmentQuestions.get(i).getQuestionId())
              .orderIndex(i)
              .weight(BigDecimal.ONE)
              .build());
    }
    roadmapEntryQuestionMappingRepository.saveAll(mappings);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestInfoResponse getEntryTestForStudent(UUID studentId, UUID roadmapId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);

    StudentAssessmentResponse response = studentAssessmentService.getAssessmentDetails(assessmentId);
    UUID activeAttemptId = findActiveAttemptId(studentId, assessmentId);

    return RoadmapEntryTestInfoResponse.builder()
        .assessmentId(response.getId())
        .title(response.getTitle())
        .description(response.getDescription())
        .totalQuestions(response.getTotalQuestions())
        .totalPoints(response.getTotalPoints())
        .timeLimitMinutes(response.getTimeLimitMinutes())
        .startDate(response.getStartDate())
        .endDate(response.getEndDate())
        .studentStatus(response.getStudentStatus())
        .activeAttemptId(activeAttemptId)
        .attemptNumber(response.getAttemptNumber())
        .maxAttempts(response.getMaxAttempts())
        .allowMultipleAttempts(response.getAllowMultipleAttempts())
        .canStart(response.getCanStart())
        .cannotStartReason(response.getCannotStartReason())
        .build();
  }

  @Override
  public AnswerAckResponse saveEntryTestAnswer(
      UUID studentId,
      UUID roadmapId,
      UUID attemptId,
      RoadmapEntryTestAnswerRequest request) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    return studentAssessmentService.updateAnswer(
        com.fptu.math_master.dto.request.AnswerUpdateRequest.builder()
            .attemptId(attemptId)
            .questionId(request.getQuestionId())
            .answerValue(request.getAnswerValue())
            .clientTimestamp(request.getClientTimestamp())
            .sequenceNumber(request.getSequenceNumber())
            .build());
  }

  @Override
  public AnswerAckResponse updateEntryTestFlag(
      UUID studentId,
      UUID roadmapId,
      UUID attemptId,
      RoadmapEntryTestFlagRequest request) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    return studentAssessmentService.updateFlag(
        com.fptu.math_master.dto.request.FlagUpdateRequest.builder()
            .attemptId(attemptId)
            .questionId(request.getQuestionId())
            .flagged(request.getFlagged())
            .build());
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestSnapshotResponse getEntryTestSnapshot(
      UUID studentId, UUID roadmapId, UUID attemptId) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    com.fptu.math_master.dto.response.DraftSnapshotResponse snapshot =
        studentAssessmentService.getDraftSnapshot(attemptId);

    return RoadmapEntryTestSnapshotResponse.builder()
        .attemptId(snapshot.getAttemptId())
        .answers(snapshot.getAnswers())
        .flags(snapshot.getFlags())
        .startedAt(snapshot.getStartedAt())
        .expiresAt(snapshot.getExpiresAt())
        .timeRemainingSeconds(snapshot.getTimeRemainingSeconds())
        .progress(buildProgress(snapshot.getAnsweredCount(), snapshot.getTotalQuestions()))
        .build();
  }

  @Override
  public void saveEntryTestAndExit(UUID studentId, UUID roadmapId, UUID attemptId) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    studentAssessmentService.saveAndExit(attemptId);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestActiveAttemptResponse getActiveEntryTestAttempt(
      UUID studentId, UUID roadmapId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);
    StudentAssessmentResponse info = studentAssessmentService.getAssessmentDetails(assessmentId);
    UUID activeAttemptId = findActiveAttemptId(studentId, assessmentId);

    if (activeAttemptId == null) {
      return RoadmapEntryTestActiveAttemptResponse.builder()
          .assessmentId(assessmentId)
          .studentStatus(info.getStudentStatus())
          .attemptId(null)
          .build();
    }

    com.fptu.math_master.dto.response.DraftSnapshotResponse snapshot =
        studentAssessmentService.getDraftSnapshot(activeAttemptId);

    return RoadmapEntryTestActiveAttemptResponse.builder()
        .assessmentId(assessmentId)
        .studentStatus(info.getStudentStatus())
        .attemptId(activeAttemptId)
        .startedAt(snapshot.getStartedAt())
        .expiresAt(snapshot.getExpiresAt())
        .timeRemainingSeconds(snapshot.getTimeRemainingSeconds())
        .progress(buildProgress(snapshot.getAnsweredCount(), snapshot.getTotalQuestions()))
        .build();
  }

  @Override
  public AttemptStartResponse startEntryTest(UUID studentId, UUID roadmapId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);

    submissionRepository
        .findByAssessmentIdAndStudentId(assessmentId, studentId)
        .ifPresent(submission -> log.debug("Student {} has previous entry-test submission {}", studentId, submission.getId()));

    return studentAssessmentService.startAssessment(
      StartAssessmentRequest.builder().assessmentId(assessmentId).build());
  }

  @Override
  public RoadmapEntryTestResultResponse finishEntryTest(UUID studentId, UUID roadmapId, UUID attemptId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);

    QuizAttempt attempt =
        quizAttemptRepository
            .findById(attemptId)
            .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));

    if (!attempt.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ATTEMPT_ACCESS_DENIED);
    }

    if (!attempt.getAssessmentId().equals(assessmentId)) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    studentAssessmentService.submitAssessment(
        SubmitAssessmentRequest.builder().attemptId(attemptId).confirmed(true).build());

    Submission submission =
        submissionRepository
            .findByAssessmentIdAndStudentId(assessmentId, studentId)
            .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));

    if (submission.getStatus() == SubmissionStatus.SUBMITTED) {
      gradingService.autoGradeSubmission(submission.getId());
    }

    return evaluateEntryTestResult(studentId, roadmapId, submission.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestResultResponse submitEntryTest(
      UUID studentId, UUID roadmapId, SubmitRoadmapEntryTestRequest request) {
    return evaluateEntryTestResult(studentId, roadmapId, request.getSubmissionId());
    }

    private RoadmapEntryTestResultResponse evaluateEntryTestResult(
      UUID studentId, UUID roadmapId, UUID submissionId) {
    getActiveRoadmapOrThrow(roadmapId);

    Submission submission =
        submissionRepository
        .findById(submissionId)
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

    // Calculate score from submission
    double scoreOnTen = computeScoreOnTen(submission);

    // Find topic where score <= mark threshold
    List<RoadmapTopic> topics =
        topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId).stream()
            .filter(topic -> topic.getDeletedAt() == null)
            .toList();
    
    if (topics.isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    RoadmapTopic suggestedTopic = topics.get(topics.size() - 1);
    for (RoadmapTopic topic : topics) {
      if (topic.getMark() != null && scoreOnTen <= topic.getMark()) {
        suggestedTopic = topic;
        break;
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

  private double computeScoreOnTen(Submission submission) {
    double scoreOnTen = 0.0;

    if (submission.getFinalScore() != null && submission.getMaxScore() != null
        && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
      scoreOnTen =
          submission.getFinalScore().doubleValue() / submission.getMaxScore().doubleValue() * 10.0;
    } else if (submission.getScore() != null && submission.getMaxScore() != null
        && submission.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
      scoreOnTen = submission.getScore().doubleValue() / submission.getMaxScore().doubleValue() * 10.0;
    } else if (submission.getPercentage() != null) {
      scoreOnTen = submission.getPercentage().doubleValue() / 10.0;
    }

    if (scoreOnTen < 0.0) scoreOnTen = 0.0;
    if (scoreOnTen > 10.0) scoreOnTen = 10.0;

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

  private UUID getConfiguredEntryTestAssessmentId(UUID roadmapId) {
    getActiveRoadmapOrThrow(roadmapId);

    List<RoadmapEntryQuestionMapping> mappings =
        roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId);
    if (mappings.isEmpty()) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }

    return mappings.get(0).getAssessmentId();
  }

  private long countActiveTopics(UUID roadmapId) {
    return topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId).stream()
        .filter(topic -> topic.getDeletedAt() == null)
        .count();
  }

    private List<RoadmapResourceOptionResponse> searchLessonBasedOptions(
      String type, UUID chapterId, UUID lessonId, String name) {
    List<com.fptu.math_master.entity.Lesson> lessons;

    if (lessonId != null) {
      lessons =
        lessonRepository.findByIdAndNotDeleted(lessonId).stream()
          .filter(lesson -> chapterId == null || chapterId.equals(lesson.getChapterId()))
          .filter(
            lesson ->
              name == null
                || lesson.getTitle() == null
                || lesson.getTitle().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
          .toList();
    } else if (chapterId != null) {
      lessons =
        (name == null || name.isBlank())
          ? lessonRepository.findByChapterIdAndNotDeleted(chapterId)
          : lessonRepository.findByChapterIdAndTitleContainingAndNotDeleted(chapterId, name);
    } else {
      return List.of();
    }

    return lessons.stream()
      .map(
        lesson ->
          RoadmapResourceOptionResponse.builder()
            .id(lesson.getId())
            .name(lesson.getTitle())
            .type(type)
            .lessonId(lesson.getId())
            .chapterId(lesson.getChapterId())
            .build())
      .collect(Collectors.toList());
    }

    private List<RoadmapResourceOptionResponse> searchMindmapOptions(UUID lessonId, String name) {
    if (lessonId == null) {
      return List.of();
    }

    List<Mindmap> mindmaps =
      (name == null || name.isBlank())
        ? mindmapRepository.findByLessonIdAndNotDeleted(lessonId, Pageable.unpaged()).getContent()
        : mindmapRepository.findByLessonIdAndTitleContainingAndNotDeleted(lessonId, name);

    return mindmaps.stream()
      .map(
        mindmap ->
          RoadmapResourceOptionResponse.builder()
            .id(mindmap.getId())
            .name(mindmap.getTitle())
            .type("MINDMAP")
            .lessonId(mindmap.getLessonId())
            .build())
      .collect(Collectors.toList());
    }

    private List<RoadmapResourceOptionResponse> searchLessonPlanOptions(UUID lessonId, String name) {
    if (lessonId == null) {
      return List.of();
    }

    List<LessonPlan> lessonPlans =
      (name == null || name.isBlank())
        ? lessonPlanRepository.findByLessonIdAndNotDeleted(lessonId)
        : lessonPlanRepository.findByLessonIdAndLessonTitleContainingAndNotDeleted(lessonId, name);

    String lessonTitle =
      lessonRepository.findByIdAndNotDeleted(lessonId).map(com.fptu.math_master.entity.Lesson::getTitle).orElse("Lesson");

    return lessonPlans.stream()
      .map(
        lessonPlan ->
          RoadmapResourceOptionResponse.builder()
            .id(lessonPlan.getId())
            .name("Lesson plan - " + lessonTitle)
            .type("LESSON_PLAN")
            .lessonId(lessonPlan.getLessonId())
            .build())
      .collect(Collectors.toList());
    }

    private List<RoadmapResourceOptionResponse> searchAssessmentOptions(String name) {
    String keyword = (name == null || name.isBlank()) ? "" : name;

    return assessmentRepository.findByTitleContainingAndStatusAndNotDeleted(keyword, null).stream()
      .map(
        assessment ->
          RoadmapResourceOptionResponse.builder()
            .id(assessment.getId())
            .name(assessment.getTitle())
            .type("ASSESSMENT")
            .build())
      .collect(Collectors.toList());
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

  private UUID findActiveAttemptId(UUID studentId, UUID assessmentId) {
    List<QuizAttempt> inProgressAttempts =
        quizAttemptRepository.findByAssessmentIdAndStudentIdAndStatus(
            assessmentId, studentId, SubmissionStatus.IN_PROGRESS);

    if (inProgressAttempts.isEmpty()) {
      return null;
    }

    return inProgressAttempts.get(0).getId();
  }

  private void validateRoadmapAttempt(UUID studentId, UUID roadmapId, UUID attemptId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);

    QuizAttempt attempt =
        quizAttemptRepository
            .findById(attemptId)
            .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));

    if (!studentId.equals(attempt.getStudentId())) {
      throw new AppException(ErrorCode.ATTEMPT_ACCESS_DENIED);
    }

    if (!assessmentId.equals(attempt.getAssessmentId())) {
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    }
  }

  private RoadmapEntryTestProgressResponse buildProgress(
      Integer answeredCount, Integer totalQuestions) {
    int safeAnswered = answeredCount == null ? 0 : answeredCount;
    int safeTotal = totalQuestions == null ? 0 : totalQuestions;

    double completion =
        safeTotal == 0
            ? 0.0
            : BigDecimal.valueOf((safeAnswered * 100.0) / safeTotal)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

    return RoadmapEntryTestProgressResponse.builder()
        .answeredCount(safeAnswered)
        .totalQuestions(safeTotal)
        .completionPercentage(completion)
        .build();
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

  private Set<Course> resolveTopicCourses(List<UUID> courseIds, LearningRoadmap roadmap) {
    if (courseIds == null || courseIds.isEmpty()) {
      return Set.of();
    }

    Set<UUID> uniqueCourseIds =
        courseIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    if (uniqueCourseIds.isEmpty()) {
      return Set.of();
    }

    List<Course> courses =
        uniqueCourseIds.stream()
            .map(
                id ->
                    courseRepository
                        .findByIdAndDeletedAtIsNull(id)
                        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND)))
            .toList();

    for (Course course : courses) {
      if (!course.getSubjectId().equals(roadmap.getSubjectId())) {
        throw new AppException(ErrorCode.INVALID_SUBJECT);
      }
    }

    return new HashSet<>(courses);
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
