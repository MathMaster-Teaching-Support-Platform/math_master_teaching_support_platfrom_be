package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.*;
import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.*;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.*;
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
  TopicCourseRepository topicCourseRepository;
  RoadmapEntryQuestionMappingRepository roadmapEntryQuestionMappingRepository;
  SubmissionRepository submissionRepository;
  QuizAttemptRepository quizAttemptRepository;
  SubjectRepository subjectRepository;
  CourseRepository courseRepository;
  UserRepository userRepository;
  LearningRoadmapService learningRoadmapService;
  StudentAssessmentService studentAssessmentService;
  GradingService gradingService;

  // ── Roadmap CRUD ─────────────────────────────────────────────────────────────

  @Override
  public RoadmapDetailResponse createRoadmap(CreateAdminRoadmapRequest request) {
    Subject subject = resolveSubject(request.getSubjectId());
    LearningRoadmap roadmap = LearningRoadmap.builder()
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
    String n = name == null ? null : name.trim();
    return roadmapRepository.findAdminTemplates(n, pageable).map(this::mapToSummaryResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getRoadmap(UUID roadmapId) {
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    if (roadmap.getDeletedAt() != null) throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    return learningRoadmapService.getRoadmapById(roadmapId);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapDetailResponse getRoadmapForStudent(UUID studentId, UUID roadmapId) {
    RoadmapDetailResponse response = getRoadmap(roadmapId);
    double bestScore = computeStudentBestScoreOnTenForRoadmap(studentId, roadmapId, null);
    response.setStudentBestScore(toPointScaleInt(bestScore));
    return response;
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
    if (request.getDescription() != null) roadmap.setDescription(request.getDescription());
    if (request.getEstimatedCompletionDays() != null)
      roadmap.setEstimatedCompletionDays(request.getEstimatedCompletionDays());
    if (request.getStatus() != null) roadmap.setStatus(request.getStatus());
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

  // ── Roadmap Entry Test ───────────────────────────────────────────────────────

  @Override
  public void setRoadmapEntryTest(UUID roadmapId, UUID entryTestId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    
    // Validate that the assessment exists and is not deleted
    assessmentRepository.findByIdAndNotDeleted(entryTestId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    
    roadmap.setEntryTestId(entryTestId);
    roadmapRepository.save(roadmap);
    
    log.info("Set entry test {} for roadmap {}", entryTestId, roadmapId);
  }

  @Override
  public void removeRoadmapEntryTest(UUID roadmapId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    roadmap.setEntryTestId(null);
    roadmapRepository.save(roadmap);
    
    log.info("Removed entry test from roadmap {}", roadmapId);
  }

  // ── Topic CRUD ───────────────────────────────────────────────────────────────

  @Override
  public RoadmapTopicResponse addTopic(UUID roadmapId, CreateRoadmapTopicRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);

    RoadmapTopic topic = RoadmapTopic.builder()
        .roadmapId(roadmapId)
        .title(request.getTitle())
        .description(request.getDescription())
        .status(TopicStatus.NOT_STARTED)
        .difficulty(request.getDifficulty())
        .sequenceOrder(request.getSequenceOrder())
        .progressPercentage(BigDecimal.ZERO)
        .build();

    topic = topicRepository.save(topic);
    
    // Handle course if provided
    if (request.getCourseId() != null) {
      Course course = validateCourse(request.getCourseId(), roadmap);
      TopicCourse topicCourse = TopicCourse.builder()
          .topicId(topic.getId())
          .courseId(course.getId())
          .build();
      topicCourseRepository.save(topicCourse);
    }
    
    roadmap.setTotalTopicsCount((int) countActiveTopics(roadmapId));
    roadmapRepository.save(roadmap);
    return learningRoadmapService.getTopicDetails(topic.getId());
  }

  @Override
  public RoadmapTopicResponse updateTopic(UUID roadmapId, UUID topicId,
      UpdateRoadmapTopicRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    RoadmapTopic topic = topicRepository.findById(topicId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    if (topic.getDeletedAt() != null || !topic.getRoadmapId().equals(roadmapId))
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);

    if (request.getTitle() != null) topic.setTitle(request.getTitle());
    if (request.getDescription() != null) topic.setDescription(request.getDescription());
    if (request.getSequenceOrder() != null) topic.setSequenceOrder(request.getSequenceOrder());
    if (request.getDifficulty() != null) topic.setDifficulty(request.getDifficulty());
    if (request.getStatus() != null) topic.setStatus(request.getStatus());

    topicRepository.save(topic);
    return learningRoadmapService.getTopicDetails(topicId);
  }

  @Override
  public void softDeleteTopic(UUID roadmapId, UUID topicId) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    RoadmapTopic topic = topicRepository.findById(topicId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    if (!topic.getRoadmapId().equals(roadmapId))
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    if (topic.getDeletedAt() != null) return;
    topic.setDeletedAt(Instant.now());
    topicRepository.save(topic);
    roadmap.setTotalTopicsCount((int) countActiveTopics(roadmapId));
    roadmapRepository.save(roadmap);
  }

  @Override
  public List<RoadmapTopicResponse> batchSaveTopics(BatchTopicRequest request) {
    UUID roadmapId = request.getRoadmapId();
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    
    log.info("Batch saving {} topics for roadmap {}", request.getTopics().size(), roadmapId);
    
    List<RoadmapTopicResponse> savedTopics = new ArrayList<>();
    
    // Process each topic in the batch
    for (TopicBatchItem item : request.getTopics()) {
      log.debug("Processing topic: id={}, title={}, courseIds={}", 
          item.getId(), item.getTitle(), item.getCourseIds());
      
      if (item.getId() == null) {
        // CREATE new topic
        RoadmapTopic topic = RoadmapTopic.builder()
            .roadmapId(roadmapId)
            .title(item.getTitle())
            .description(item.getDescription())
            .status(item.getStatus() != null ? item.getStatus() : TopicStatus.NOT_STARTED)
            .difficulty(item.getDifficulty())
            .sequenceOrder(item.getSequenceOrder())
            .mark(item.getMark())
            .progressPercentage(BigDecimal.ZERO)
            .build();
        
        topic = topicRepository.save(topic);
        
        // Handle courses via TopicCourse join table
        if (item.getCourseIds() != null && !item.getCourseIds().isEmpty()) {
          for (UUID courseId : item.getCourseIds()) {
            Course course = validateCourse(courseId, roadmap);
            if (!topicCourseRepository.existsByTopicIdAndCourseId(topic.getId(), course.getId())) {
              TopicCourse topicCourse = TopicCourse.builder()
                  .topicId(topic.getId())
                  .courseId(course.getId())
                  .build();
              topicCourseRepository.save(topicCourse);
            }
          }
        }
        
        savedTopics.add(learningRoadmapService.getTopicDetails(topic.getId()));
        
      } else if (item.getStatus() == TopicStatus.INACTIVE || 
                 (item.getStatus() != null && item.getStatus().name().equals("INACTIVE"))) {
        // SOFT DELETE existing topic
        RoadmapTopic topic = topicRepository.findById(item.getId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
        
        if (!topic.getRoadmapId().equals(roadmapId)) {
          throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        
        if (topic.getDeletedAt() == null) {
          topic.setDeletedAt(Instant.now());
          topicRepository.save(topic);
          // Also soft delete all topic-course associations
          topicCourseRepository.softDeleteByTopicId(topic.getId());
        }
        // Don't add deleted topics to response
        
      } else {
        // UPDATE existing topic
        RoadmapTopic topic = topicRepository.findById(item.getId())
            .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
        
        if (topic.getDeletedAt() != null || !topic.getRoadmapId().equals(roadmapId)) {
          throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
        }
        
        // Update fields
        topic.setTitle(item.getTitle());
        topic.setDescription(item.getDescription());
        topic.setSequenceOrder(item.getSequenceOrder());
        topic.setDifficulty(item.getDifficulty());
        topic.setStatus(item.getStatus());
        if (item.getMark() != null) {
          topic.setMark(item.getMark());
        }
        
        topicRepository.save(topic);
        
        // Handle courses - replace all via TopicCourse join table
        if (item.getCourseIds() != null) {
          // Remove all existing courses
          topicCourseRepository.softDeleteByTopicId(topic.getId());
          
          // Add new courses
          for (UUID courseId : item.getCourseIds()) {
            Course course = validateCourse(courseId, roadmap);
            if (!topicCourseRepository.existsByTopicIdAndCourseId(topic.getId(), course.getId())) {
              TopicCourse topicCourse = TopicCourse.builder()
                  .topicId(topic.getId())
                  .courseId(course.getId())
                  .build();
              topicCourseRepository.save(topicCourse);
            }
          }
        }
        
        savedTopics.add(learningRoadmapService.getTopicDetails(topic.getId()));
      }
    }
    
    // Update roadmap total topics count
    roadmap.setTotalTopicsCount((int) countActiveTopics(roadmapId));
    roadmapRepository.save(roadmap);
    
    log.info("Batch saved {} topics for roadmap {}", savedTopics.size(), roadmapId);
    return savedTopics;
  }

  // ── Entry Test ───────────────────────────────────────────────────────────────

  @Override
  public void configureEntryTest(UUID roadmapId, CreateRoadmapEntryTestRequest request) {
    LearningRoadmap roadmap = getActiveRoadmapOrThrow(roadmapId);
    if (roadmap.getGenerationType() != RoadmapGenerationType.ADMIN_TEMPLATE)
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);

    assessmentRepository.findById(request.getAssessmentId())
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    roadmapEntryQuestionMappingRepository.deleteByRoadmapId(roadmapId);

    List<AssessmentQuestion> questions =
        assessmentQuestionRepository.findByAssessmentIdOrderByOrderIndex(request.getAssessmentId());
    if (questions.isEmpty()) throw new AppException(ErrorCode.QUESTION_NOT_FOUND);

    List<RoadmapEntryQuestionMapping> mappings = new ArrayList<>();
    for (int i = 0; i < questions.size(); i++) {
      mappings.add(RoadmapEntryQuestionMapping.builder()
          .roadmapId(roadmapId)
          .assessmentId(request.getAssessmentId())
          .questionId(questions.get(i).getQuestionId())
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
    StudentAssessmentResponse r = studentAssessmentService.getAssessmentDetails(assessmentId);
    UUID activeAttemptId = findActiveAttemptId(studentId, assessmentId);
    return RoadmapEntryTestInfoResponse.builder()
        .assessmentId(r.getId()).title(r.getTitle()).description(r.getDescription())
        .totalQuestions(r.getTotalQuestions()).totalPoints(r.getTotalPoints())
        .timeLimitMinutes(r.getTimeLimitMinutes()).startDate(r.getStartDate())
        .endDate(r.getEndDate()).studentStatus(r.getStudentStatus())
        .activeAttemptId(activeAttemptId).attemptNumber(r.getAttemptNumber())
        .maxAttempts(r.getMaxAttempts()).allowMultipleAttempts(r.getAllowMultipleAttempts())
        .canStart(r.getCanStart()).cannotStartReason(r.getCannotStartReason())
        .build();
  }

  @Override
  public AnswerAckResponse saveEntryTestAnswer(UUID studentId, UUID roadmapId, UUID attemptId,
      RoadmapEntryTestAnswerRequest request) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    return studentAssessmentService.updateAnswer(
        com.fptu.math_master.dto.request.AnswerUpdateRequest.builder()
            .attemptId(attemptId).questionId(request.getQuestionId())
            .answerValue(request.getAnswerValue()).clientTimestamp(request.getClientTimestamp())
            .sequenceNumber(request.getSequenceNumber()).build());
  }

  @Override
  public AnswerAckResponse updateEntryTestFlag(UUID studentId, UUID roadmapId, UUID attemptId,
      RoadmapEntryTestFlagRequest request) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    return studentAssessmentService.updateFlag(
        com.fptu.math_master.dto.request.FlagUpdateRequest.builder()
            .attemptId(attemptId).questionId(request.getQuestionId())
            .flagged(request.getFlagged()).build());
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestSnapshotResponse getEntryTestSnapshot(UUID studentId, UUID roadmapId,
      UUID attemptId) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    com.fptu.math_master.dto.response.DraftSnapshotResponse s =
        studentAssessmentService.getDraftSnapshot(attemptId);
    return RoadmapEntryTestSnapshotResponse.builder()
        .attemptId(s.getAttemptId()).answers(s.getAnswers()).flags(s.getFlags())
        .startedAt(s.getStartedAt()).expiresAt(s.getExpiresAt())
        .timeRemainingSeconds(s.getTimeRemainingSeconds())
        .progress(buildProgress(s.getAnsweredCount(), s.getTotalQuestions())).build();
  }

  @Override
  public void saveEntryTestAndExit(UUID studentId, UUID roadmapId, UUID attemptId) {
    validateRoadmapAttempt(studentId, roadmapId, attemptId);
    studentAssessmentService.saveAndExit(attemptId);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestActiveAttemptResponse getActiveEntryTestAttempt(UUID studentId,
      UUID roadmapId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);
    StudentAssessmentResponse info = studentAssessmentService.getAssessmentDetails(assessmentId);
    UUID activeAttemptId = findActiveAttemptId(studentId, assessmentId);
    if (activeAttemptId == null) {
      return RoadmapEntryTestActiveAttemptResponse.builder()
          .assessmentId(assessmentId).studentStatus(info.getStudentStatus()).attemptId(null).build();
    }
    com.fptu.math_master.dto.response.DraftSnapshotResponse s =
        studentAssessmentService.getDraftSnapshot(activeAttemptId);
    return RoadmapEntryTestActiveAttemptResponse.builder()
        .assessmentId(assessmentId).studentStatus(info.getStudentStatus())
        .attemptId(activeAttemptId).startedAt(s.getStartedAt()).expiresAt(s.getExpiresAt())
        .timeRemainingSeconds(s.getTimeRemainingSeconds())
        .progress(buildProgress(s.getAnsweredCount(), s.getTotalQuestions())).build();
  }

  @Override
  public AttemptStartResponse startEntryTest(UUID studentId, UUID roadmapId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);
    return studentAssessmentService.startAssessment(
        StartAssessmentRequest.builder().assessmentId(assessmentId).build());
  }

  @Override
  public RoadmapEntryTestResultResponse finishEntryTest(UUID studentId, UUID roadmapId,
      UUID attemptId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);
    QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));
    if (!attempt.getStudentId().equals(studentId)) throw new AppException(ErrorCode.ATTEMPT_ACCESS_DENIED);
    if (!attempt.getAssessmentId().equals(assessmentId)) throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);

    Submission prev = submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId).orElse(null);
    double prevBest = computeStudentBestScoreOnTenForRoadmap(studentId, roadmapId,
        prev == null ? null : prev.getId());

    studentAssessmentService.submitAssessment(
        SubmitAssessmentRequest.builder().attemptId(attemptId).confirmed(true).build());

    Submission submission = submissionRepository.findByAssessmentIdAndStudentId(assessmentId, studentId)
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    if (submission.getStatus() == SubmissionStatus.SUBMITTED)
      gradingService.autoGradeSubmission(submission.getId());

    return evaluateEntryTestResult(studentId, roadmapId, submission.getId(), prevBest);
  }

  @Override
  @Transactional(readOnly = true)
  public RoadmapEntryTestResultResponse submitEntryTest(UUID studentId, UUID roadmapId,
      SubmitRoadmapEntryTestRequest request) {
    double prevBest = computeStudentBestScoreOnTenForRoadmap(studentId, roadmapId,
        request.getSubmissionId());
    return evaluateEntryTestResult(studentId, roadmapId, request.getSubmissionId(), prevBest);
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private RoadmapEntryTestResultResponse evaluateEntryTestResult(UUID studentId, UUID roadmapId,
      UUID submissionId, double previousBestScoreOnTen) {
    getActiveRoadmapOrThrow(roadmapId);
    Submission submission = submissionRepository.findById(submissionId)
        .orElseThrow(() -> new AppException(ErrorCode.SUBMISSION_NOT_FOUND));
    if (!submission.getStudentId().equals(studentId)) throw new AppException(ErrorCode.UNAUTHORIZED);

    List<RoadmapEntryQuestionMapping> mappings =
        roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId);
    if (mappings.isEmpty()) throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    if (!mappings.get(0).getAssessmentId().equals(submission.getAssessmentId()))
      throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);

    double scoreOnTen = computeScoreOnTen(submission);
    double bestScoreOnTen = computeStudentBestScoreOnTenForRoadmap(studentId, roadmapId, null);

    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)
        .stream().filter(t -> t.getDeletedAt() == null).toList();
    if (topics.isEmpty()) throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);

    // Suggest starting topic based on mark thresholds (informational only — no locking)
    RoadmapTopic suggestedTopic = topics.get(topics.size() - 1);
    for (RoadmapTopic topic : topics) {
      if (topic.getMark() != null && bestScoreOnTen <= topic.getMark()) {
        suggestedTopic = topic;
        break;
      }
    }

    List<RoadmapUnlockedTopicResponse> unlockedTopics = mapUnlockedTopics(topics, bestScoreOnTen);
    List<RoadmapUnlockedTopicResponse> newlyUnlocked =
        mapNewlyUnlockedTopics(topics, previousBestScoreOnTen, bestScoreOnTen);

    return RoadmapEntryTestResultResponse.builder()
        .roadmapId(roadmapId).submissionId(submission.getId())
        .suggestedTopicId(suggestedTopic.getId())
        .score(toPointScaleInt(scoreOnTen)).studentBestScore(toPointScaleInt(bestScoreOnTen))
        .unlockedTopics(unlockedTopics).newlyUnlockedTopics(newlyUnlocked)
        .scoreOnTen(scoreOnTen).evaluatedQuestions(mappings.size())
        .thresholdPercentage(70).evaluatedAt(Instant.now()).build();
  }

  private Course validateCourse(UUID courseId, LearningRoadmap roadmap) {
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
    
    log.debug("Validating course: courseId={}, courseSubjectId={}, roadmapSubjectId={}", 
        courseId, course.getSubjectId(), roadmap.getSubjectId());
    
    // TODO: Consider making this validation optional for multi-subject roadmaps
    if (!course.getSubjectId().equals(roadmap.getSubjectId())) {
      log.warn("Course subject mismatch: course {} has subjectId {}, but roadmap {} has subjectId {}. Allowing for flexibility.", 
          courseId, course.getSubjectId(), roadmap.getId(), roadmap.getSubjectId());
      // For now, allow cross-subject courses for flexibility
      // throw new AppException(ErrorCode.INVALID_SUBJECT);
    }
    return course;
  }

  private LearningRoadmap getActiveRoadmapOrThrow(UUID roadmapId) {
    LearningRoadmap r = roadmapRepository.findById(roadmapId)
        .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));
    if (r.getDeletedAt() != null) throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    return r;
  }

  private UUID getConfiguredEntryTestAssessmentId(UUID roadmapId) {
    getActiveRoadmapOrThrow(roadmapId);
    List<RoadmapEntryQuestionMapping> mappings =
        roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId);
    if (mappings.isEmpty()) throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
    return mappings.get(0).getAssessmentId();
  }

  private long countActiveTopics(UUID roadmapId) {
    return topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId).stream()
        .filter(t -> t.getDeletedAt() == null).count();
  }

  private UUID findActiveAttemptId(UUID studentId, UUID assessmentId) {
    List<QuizAttempt> attempts = quizAttemptRepository
        .findByAssessmentIdAndStudentIdAndStatus(assessmentId, studentId, SubmissionStatus.IN_PROGRESS);
    return attempts.isEmpty() ? null : attempts.get(0).getId();
  }

  private void validateRoadmapAttempt(UUID studentId, UUID roadmapId, UUID attemptId) {
    UUID assessmentId = getConfiguredEntryTestAssessmentId(roadmapId);
    QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));
    if (!studentId.equals(attempt.getStudentId())) throw new AppException(ErrorCode.ATTEMPT_ACCESS_DENIED);
    if (!assessmentId.equals(attempt.getAssessmentId())) throw new AppException(ErrorCode.ASSESSMENT_NOT_FOUND);
  }

  private RoadmapEntryTestProgressResponse buildProgress(Integer answered, Integer total) {
    int a = answered == null ? 0 : answered;
    int t = total == null ? 0 : total;
    double pct = t == 0 ? 0.0
        : BigDecimal.valueOf((a * 100.0) / t).setScale(2, RoundingMode.HALF_UP).doubleValue();
    return RoadmapEntryTestProgressResponse.builder()
        .answeredCount(a).totalQuestions(t).completionPercentage(pct).build();
  }

  private double computeScoreOnTen(Submission s) {
    double v = 0.0;
    if (s.getFinalScore() != null && s.getMaxScore() != null
        && s.getMaxScore().compareTo(BigDecimal.ZERO) > 0)
      v = s.getFinalScore().doubleValue() / s.getMaxScore().doubleValue() * 10.0;
    else if (s.getScore() != null && s.getMaxScore() != null
        && s.getMaxScore().compareTo(BigDecimal.ZERO) > 0)
      v = s.getScore().doubleValue() / s.getMaxScore().doubleValue() * 10.0;
    else if (s.getPercentage() != null)
      v = s.getPercentage().doubleValue() / 10.0;
    v = Math.min(10.0, Math.max(0.0, v));
    return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private double computeStudentBestScoreOnTenForRoadmap(UUID studentId, UUID roadmapId,
      UUID excludeSubmissionId) {
    List<RoadmapEntryQuestionMapping> mappings =
        roadmapEntryQuestionMappingRepository.findByRoadmapIdOrderByOrderIndex(roadmapId);
    if (mappings.isEmpty()) return 0.0;
    UUID assessmentId = mappings.get(0).getAssessmentId();
    List<Submission> submissions = submissionRepository.findByStudentIdAndStatuses(
        studentId, java.util.EnumSet.of(SubmissionStatus.SUBMITTED, SubmissionStatus.GRADED));
    return submissions.stream()
        .filter(s -> assessmentId.equals(s.getAssessmentId()))
        .filter(s -> excludeSubmissionId == null || !excludeSubmissionId.equals(s.getId()))
        .mapToDouble(this::computeScoreOnTen).max().orElse(0.0);
  }

  private int toPointScaleInt(double v) {
    return BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP).intValue();
  }

  private boolean isTopicUnlocked(RoadmapTopic topic, double bestScore) {
    double required = topic.getMark() == null ? 0.0 : topic.getMark();
    return required <= 0.0 || bestScore >= required;
  }

  private RoadmapUnlockedTopicResponse mapUnlockedTopic(RoadmapTopic topic) {
    return RoadmapUnlockedTopicResponse.builder().id(topic.getId()).name(topic.getTitle())
        .requiredPoint(topic.getMark() == null ? 0.0 : topic.getMark()).build();
  }

  private List<RoadmapUnlockedTopicResponse> mapUnlockedTopics(List<RoadmapTopic> topics,
      double best) {
    return topics.stream().filter(t -> isTopicUnlocked(t, best))
        .map(this::mapUnlockedTopic).collect(Collectors.toList());
  }

  private List<RoadmapUnlockedTopicResponse> mapNewlyUnlockedTopics(List<RoadmapTopic> topics,
      double prev, double curr) {
    return topics.stream().filter(t -> !isTopicUnlocked(t, prev))
        .filter(t -> isTopicUnlocked(t, curr))
        .map(this::mapUnlockedTopic).collect(Collectors.toList());
  }

  private RoadmapSummaryResponse mapToSummaryResponse(LearningRoadmap r) {
    String studentName = r.getStudentId() == null ? "ALL_STUDENTS"
        : userRepository.findById(r.getStudentId())
            .map(User::getFullName).orElse("Unknown");
    return RoadmapSummaryResponse.builder()
        .id(r.getId()).name(r.getName()).studentId(r.getStudentId())
        .subjectId(r.getSubjectId()).studentName(studentName).subject(r.getSubject())
        .gradeLevel(r.getGradeLevel()).status(r.getStatus())
        .progressPercentage(r.getProgressPercentage())
        .completedTopicsCount(r.getCompletedTopicsCount())
        .totalTopicsCount(r.getTotalTopicsCount())
        .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt()).build();
  }

  private Subject resolveSubject(UUID subjectId) {
    Subject s = subjectRepository.findById(subjectId)
        .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    if (s.getDeletedAt() != null) throw new AppException(ErrorCode.SUBJECT_NOT_FOUND);
    return s;
  }

  private String resolveGradeLevel(Subject subject) {
    if (subject.getSchoolGrade() == null || subject.getSchoolGrade().getDeletedAt() != null
        || subject.getSchoolGrade().getName() == null
        || subject.getSchoolGrade().getName().isBlank())
      throw new AppException(ErrorCode.SCHOOL_GRADE_NOT_FOUND);
    return subject.getSchoolGrade().getName().trim();
  }
}
