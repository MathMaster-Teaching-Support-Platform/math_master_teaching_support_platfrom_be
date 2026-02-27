package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.response.RoadmapDetailResponse;
import com.fptu.math_master.dto.response.RoadmapTopicResponse;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.RoadmapStatus;
import com.fptu.math_master.enums.TopicStatus;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.RoadmapAIPlannerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of RoadmapAIPlannerService
 *
 * Generates personalized learning roadmaps using Gemini AI based on:
 * - Student learning goals and wishes
 * - Performance data (accuracy, weak areas)
 * - Available curriculum resources
 * - Daily study time constraints
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoadmapAIPlannerServiceImpl implements RoadmapAIPlannerService {

  private final GeminiService geminiService;
  private final LearningRoadmapRepository roadmapRepository;
  private final RoadmapTopicRepository topicRepository;
  private final RoadmapSubtopicRepository subtopicRepository;
  private final TopicLearningMaterialRepository materialRepository;
  private final LessonRepository lessonRepository;
  private final ChapterRepository chapterRepository;
  private final QuestionRepository questionRepository;
  private final GradeRepository gradeRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public RoadmapDetailResponse generateRoadmapFromWish(UUID studentId, StudentWish wish, String subject) {
    log.info("Generating AI-powered roadmap for student={}, subject={}", studentId, subject);

    // Get student performance stats
    PerformanceStats performanceStats = getPerformanceStats(studentId, subject);

    // Generate AI prompt
    String prompt = generateRoadmapPrompt(wish, performanceStats, wish.getGradeLevel());
    log.debug("Generated AI prompt for student={}", studentId);

    // Get AI response
    String aiResponse = geminiService.sendMessage(prompt);
    log.debug("Received AI response from Gemini");

    // Parse AI response
    AIPlannerResponse plannerResponse = parseAIResponse(aiResponse);
    log.info("Parsed roadmap plan: {} topics, {} estimated days",
        plannerResponse.topics.size(), plannerResponse.estimatedDays);

    // Create roadmap entity
    LearningRoadmap roadmap = LearningRoadmap.builder()
        .studentId(studentId)
        .subject(subject)
        .gradeLevel(wish.getGradeLevel())
        .generationType(com.fptu.math_master.enums.RoadmapGenerationType.PERSONALIZED)
        .description("AI-generated roadmap based on student wishes: " + wish.getLearningGoals())
        .status(RoadmapStatus.GENERATED)
        .progressPercentage(BigDecimal.ZERO)
        .completedTopicsCount(0)
        .totalTopicsCount(plannerResponse.topics.size())
        .estimatedCompletionDays(plannerResponse.estimatedDays)
        .build();

    roadmap = roadmapRepository.save(roadmap);
    log.info("Created roadmap: {}", roadmap.getId());

    // Create topics from AI recommendations
    List<RoadmapTopic> topics = createTopicsFromAIPlan(roadmap, plannerResponse);
    log.info("Created {} topics from AI plan", topics.size());

    return getRoadmapDetailResponse(roadmap);
  }

  @Override
  public String generateRoadmapPrompt(StudentWish wish, PerformanceStats stats, String gradeLevel) {
    StringBuilder prompt = new StringBuilder();

    prompt.append("You are an expert mathematics learning planner. ")
        .append("Generate a personalized learning roadmap for a student based on their wishes and performance data.\n\n")
        .append("STUDENT PROFILE:\n")
        .append("- Subject: ").append(wish.getSubject()).append("\n")
        .append("- Grade Level: ").append(gradeLevel).append("\n")
        .append("- Learning Goals: ").append(wish.getLearningGoals()).append("\n")
        .append("- Preferred Topics: ").append(wish.getPreferredTopics()).append("\n")
        .append("- Areas to Improve: ").append(wish.getWeakAreasToImprove()).append("\n")
        .append("- Daily Study Time: ").append(wish.getDailyStudyMinutes()).append(" minutes\n")
        .append("- Target Accuracy: ").append(wish.getTargetAccuracyPercentage()).append("%\n")
        .append("- Learning Style: ").append(wish.getLearningStylePreference()).append("\n\n");

    if (stats.totalAssessmentsCompleted > 0) {
      prompt.append("PERFORMANCE DATA:\n")
          .append("- Overall Accuracy: ").append(String.format("%.1f", stats.averageAccuracy)).append("%\n")
          .append("- Assessments Completed: ").append(stats.totalAssessmentsCompleted).append("\n");

      if (!stats.topicStats.isEmpty()) {
        prompt.append("- Topic Performance:\n");
        for (PerformanceStats.TopicPerformance topic : stats.topicStats) {
          prompt.append("  * ").append(topic.topicName).append(": ")
              .append(String.format("%.1f", topic.accuracy)).append("% (")
              .append(topic.questionsAttempted).append(" questions)\n");
        }
      }
      prompt.append("\n");
    }

    prompt.append("PRIORITY LOGIC:\n")
        .append("Assign priority for each topic:\n")
        .append("- Accuracy < 60% → Priority -2 (WEAK - highest priority)\n")
        .append("- Accuracy 60-80% → Priority -1\n")
        .append("- In student preferred topics → Priority +1\n")
        .append("- Accuracy > 85% → Priority +2 (review only)\n")
        .append("- If topic is both weak and preferred → Keep negative priority\n\n")

        .append("ROADMAP GENERATION RULES:\n")
        .append("1. INCLUDE weak topics (< 60% accuracy) - these are highest priority\n")
        .append("2. INCLUDE topics related to student goals\n")
        .append("3. INCLUDE preferred topics with increasing depth\n")
        .append("4. Include only EASY/MEDIUM materials for strong topics (> 85%)\n")
        .append("5. Each topic must have: Easy foundation → Medium practice → Hard application\n")
        .append("6. Allocate more time to weak topics\n\n")

        .append("RESPONSE FORMAT (JSON):\n")
        .append("{\n")
        .append("  \"topics\": [\n")
        .append("    {\n")
        .append("      \"title\": \"Topic Name\",\n")
        .append("      \"priority\": -2,  // -2 to +2\n")
        .append("      \"difficulty\": \"EASY|MEDIUM|HARD\",\n")
        .append("      \"estimatedHours\": 3,\n")
        .append("      \"rationale\": \"Why this topic at this difficulty\"\n")
        .append("    }\n")
        .append("  ],\n")
        .append("  \"estimatedDays\": 20,\n")
        .append("  \"totalHours\": 30,\n")
        .append("  \"recommendations\": [\"recommendation 1\", \"recommendation 2\"],\n")
        .append("  \"focusAreas\": \"Top 3 areas student should focus on\"\n")
        .append("}\n\n")

        .append("Generate a comprehensive roadmap that balances weak areas with student interests,\n")
        .append("respects daily study time constraints, and provides a clear progression path.");

    return prompt.toString();
  }

  @Override
  public AIPlannerResponse parseAIResponse(String aiResponse) {
    AIPlannerResponse response = new AIPlannerResponse();
    response.topics = new ArrayList<>();
    response.recommendations = new ArrayList<>();

    try {
      // Extract JSON from response (handle markdown code blocks)
      String jsonStr = extractJsonFromResponse(aiResponse);

      // Parse JSON with support for leading plus signs in numbers (e.g., +2, +1)
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS.mappedFeature());
      Map<String, Object> parsed = mapper.readValue(jsonStr, Map.class);

      // Parse topics
      if (parsed.containsKey("topics")) {
        List<Map<String, Object>> topicsData = (List<Map<String, Object>>) parsed.get("topics");
        for (Map<String, Object> topicData : topicsData) {
          AIPlannerResponse.PrioritizedTopic topic = new AIPlannerResponse.PrioritizedTopic();
          topic.title = (String) topicData.get("title");
          topic.priority = ((Number) topicData.get("priority")).intValue();
          topic.difficulty = (String) topicData.get("difficulty");
          topic.estimatedHours = ((Number) topicData.get("estimatedHours")).intValue();
          topic.rationale = (String) topicData.get("rationale");
          response.topics.add(topic);
        }
      }

      // Parse other fields
      response.estimatedDays = parsed.containsKey("estimatedDays")
          ? ((Number) parsed.get("estimatedDays")).intValue() : 30;
      response.totalHours = parsed.containsKey("totalHours")
          ? ((Number) parsed.get("totalHours")).intValue() : 60;
      response.focusAreas = (String) parsed.getOrDefault("focusAreas", "General improvement");

      if (parsed.containsKey("recommendations")) {
        response.recommendations = (List<String>) parsed.get("recommendations");
      }

      log.info("Successfully parsed AI response: {} topics, {} estimated days",
          response.topics.size(), response.estimatedDays);

    } catch (Exception e) {
      log.error("Error parsing AI response: {}", e.getMessage(), e);
      log.debug("AI Response was: {}", aiResponse);
      // Return empty response to prevent crash
      response.estimatedDays = 30;
      response.totalHours = 60;
    }

    return response;
  }

  private String extractJsonFromResponse(String response) {
    // Try to find JSON block in markdown code block
    Pattern pattern = Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(response);

    if (matcher.find()) {
      return matcher.group(1);
    }

    // If no markdown block, assume entire response or part of it is JSON
    int braceStart = response.indexOf('{');
    int braceEnd = response.lastIndexOf('}');

    if (braceStart >= 0 && braceEnd > braceStart) {
      return response.substring(braceStart, braceEnd + 1);
    }

    throw new RuntimeException("Could not find JSON in AI response");
  }

  /**
   * Get student's performance statistics
   */
  private PerformanceStats getPerformanceStats(UUID studentId, String subject) {
    PerformanceStats stats = new PerformanceStats();
    stats.studentId = studentId;
    stats.subject = subject;
    stats.topicStats = new ArrayList<>();

    try {
      // Get grades for this student in this subject
      List<Grade> grades = gradeRepository.findByStudentIdAndLessonSubject(studentId, subject);

      if (!grades.isEmpty()) {
        stats.totalAssessmentsCompleted = grades.size();

        BigDecimal totalAccuracy = grades.stream()
            .map(Grade::getPercentage)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        stats.averageAccuracy = totalAccuracy.divide(
            BigDecimal.valueOf(grades.size()), 2, RoundingMode.HALF_UP).doubleValue();

        // Group by lesson to get topic-level stats
        Map<UUID, List<Grade>> byLesson = grades.stream()
            .collect(Collectors.groupingBy(Grade::getLessonId));

        for (Map.Entry<UUID, List<Grade>> entry : byLesson.entrySet()) {
          Lesson lesson = lessonRepository.findById(entry.getKey()).orElse(null);
          if (lesson != null) {
            BigDecimal avg = entry.getValue().stream()
                .map(Grade::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(entry.getValue().size()), 2, RoundingMode.HALF_UP);

            PerformanceStats.TopicPerformance topicPerf = new PerformanceStats.TopicPerformance();
            topicPerf.topicName = lesson.getTitle();
            topicPerf.accuracy = avg.doubleValue();
            topicPerf.questionsAttempted = entry.getValue().size();
            topicPerf.difficulty = determineDifficultyFromAccuracy(avg.doubleValue());
            stats.topicStats.add(topicPerf);
          }
        }
      } else {
        stats.averageAccuracy = 0.0;
        stats.totalAssessmentsCompleted = 0;
      }
    } catch (Exception e) {
      log.warn("Error fetching performance stats for student={}: {}", studentId, e.getMessage());
      stats.averageAccuracy = 0.0;
      stats.totalAssessmentsCompleted = 0;
    }

    return stats;
  }

  private String determineDifficultyFromAccuracy(double accuracy) {
    if (accuracy < 60) return "HARD";
    if (accuracy < 80) return "MEDIUM";
    return "EASY";
  }

  /**
   * Create RoadmapTopic entities from AI plan
   */
  private List<RoadmapTopic> createTopicsFromAIPlan(LearningRoadmap roadmap, AIPlannerResponse plan) {
    List<RoadmapTopic> createdTopics = new ArrayList<>();

    // Sort topics by priority (prioritize weak areas)
    List<AIPlannerResponse.PrioritizedTopic> sortedTopics = plan.topics.stream()
        .sorted(Comparator.comparingInt((AIPlannerResponse.PrioritizedTopic t) -> t.priority)
            .thenComparingInt(t -> -t.estimatedHours))
        .collect(Collectors.toList());

    int sequenceOrder = 1;
    for (AIPlannerResponse.PrioritizedTopic aiTopic : sortedTopics) {
      // Find matching chapter
      Chapter chapter = findChapterByTitle(aiTopic.title, roadmap.getGradeLevel());

      QuestionDifficulty difficulty = QuestionDifficulty.valueOf(aiTopic.difficulty);

      RoadmapTopic topic = RoadmapTopic.builder()
          .roadmapId(roadmap.getId())
          .chapterId(chapter != null ? chapter.getId() : null)
          .title(aiTopic.title)
          .description("AI-recommended: " + aiTopic.rationale)
          .status(TopicStatus.NOT_STARTED)
          .difficulty(difficulty)
          .sequenceOrder(sequenceOrder++)
          .priority(aiTopic.priority)
          .progressPercentage(BigDecimal.ZERO)
          .completedSubTopics(0)
          .totalSubTopics(0)
          .estimatedHours(aiTopic.estimatedHours)
          .build();

      topic = topicRepository.save(topic);
      createdTopics.add(topic);

      // Generate subtopics and link materials
      if (chapter != null) {
        generateSubtopicsForTopic(topic);
        linkMaterialsForTopic(topic, chapter, difficulty);
      }
    }

    return createdTopics;
  }

  /**
   * Find chapter by title (best effort matching)
   */
  private Chapter findChapterByTitle(String title, String gradeLevel) {
    try {
      // Convert to lowercase for case-insensitive search
      String searchTitle = title.toLowerCase();

      // Get lessons for grade level
      List<Lesson> lessons = lessonRepository.findByGradeLevelAndNotDeleted(gradeLevel);

      for (Lesson lesson : lessons) {
        List<Chapter> chapters = chapterRepository.findByLessonIdAndNotDeleted(lesson.getId());
        for (Chapter chapter : chapters) {
          if (chapter.getTitle().toLowerCase().contains(searchTitle)
              || searchTitle.contains(chapter.getTitle().toLowerCase())) {
            return chapter;
          }
        }
      }
    } catch (Exception e) {
      log.warn("Error finding chapter by title '{}': {}", title, e.getMessage());
    }
    return null;
  }

  /**
   * Generate subtopics for a topic
   */
  private void generateSubtopicsForTopic(RoadmapTopic topic) {
    for (int i = 1; i <= 3; i++) {
      RoadmapSubtopic subtopic = RoadmapSubtopic.builder()
          .topicId(topic.getId())
          .title(topic.getTitle() + " - Part " + i)
          .description("Subtopic " + i + " of " + topic.getTitle())
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
   * Link learning materials to topic
   */
  private void linkMaterialsForTopic(RoadmapTopic topic, Chapter chapter, QuestionDifficulty difficulty) {
    int sequenceOrder = 1;

    // Link lessons
    List<Lesson> lessons = lessonRepository.findByChapterIdAndNotDeleted(chapter.getId());
    for (Lesson lesson : lessons.stream().limit(2).collect(Collectors.toList())) {
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

    // Link practice questions
    List<Question> questions = questionRepository.findByChapterIdAndDifficultyOrderByCreatedAt(
        chapter.getId(), difficulty);

    for (Question question : questions.stream().limit(5).collect(Collectors.toList())) {
      TopicLearningMaterial material = TopicLearningMaterial.builder()
          .topicId(topic.getId())
          .questionId(question.getId())
          .resourceTitle("Practice: " + question.getQuestionText().substring(0, Math.min(50, question.getQuestionText().length())))
          .resourceType("PRACTICE")
          .sequenceOrder(sequenceOrder++)
          .isRequired(true)
          .build();

      materialRepository.save(material);
    }
  }

  /**
   * Get roadmap detail response with topics fetched from database
   */
  private RoadmapDetailResponse getRoadmapDetailResponse(LearningRoadmap roadmap) {
    // Fetch topics from database
    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmap.getId());
    
    // Map topics to response DTOs
    List<RoadmapTopicResponse> topicResponses = topics.stream()
        .map(topic -> RoadmapTopicResponse.builder()
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
            .subtopics(new ArrayList<>()) // Subtopics not loaded for performance
            .materials(new ArrayList<>()) // Materials not loaded for performance
            .build())
        .collect(Collectors.toList());
    
    return RoadmapDetailResponse.builder()
        .id(roadmap.getId())
        .studentId(roadmap.getStudentId())
        .subject(roadmap.getSubject())
        .gradeLevel(roadmap.getGradeLevel())
        .description(roadmap.getDescription())
        .status(roadmap.getStatus())
        .progressPercentage(roadmap.getProgressPercentage())
        .completedTopicsCount(roadmap.getCompletedTopicsCount())
        .totalTopicsCount(roadmap.getTotalTopicsCount())
        .estimatedCompletionDays(roadmap.getEstimatedCompletionDays())
        .topics(topicResponses)
        .build();
  }
}
