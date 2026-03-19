package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.GenerateLessonContentRequest;
import com.fptu.math_master.dto.response.ChapterResponse;
import com.fptu.math_master.dto.response.GenerateLessonContentResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Curriculum;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.CurriculumCategory;
import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GeminiService;
import com.fptu.math_master.service.LessonContentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonContentServiceImpl implements LessonContentService {

  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;
  CurriculumRepository curriculumRepository;
  UserRepository userRepository;
  GeminiService geminiService;
  ObjectMapper objectMapper;

  // -------------------------------------------------------------------------
  // AI Generation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public GenerateLessonContentResponse generateAndSaveContent(
      GenerateLessonContentRequest request) {
    log.info(
        "Generating lesson content for gradeLevel={}, category={}",
        request.getGradeLevel(),
        request.getSubject());

    UUID currentUserId = getCurrentUserId();
    validateAdminRole(currentUserId);

    // Parse grade from gradeLevel if needed and determine category
    Integer grade = parseGrade(request.getGradeLevel());
    CurriculumCategory category = parseCategory(request.getSubject());

    // Create or get curriculum
    String curriculumName = buildCurriculumName(request.getGradeLevel(), request.getSubject());
    Curriculum curriculum =
        curriculumRepository
            .findByNameAndGradeAndCategoryAndNotDeleted(curriculumName, grade, category)
            .orElseGet(
                () -> {
                  Curriculum newCurriculum =
                      Curriculum.builder()
                          .name(curriculumName)
                          .grade(grade)
                          .category(category)
                          .description(
                              "Auto-generated curriculum for "
                                  + request.getGradeLevel()
                                  + " - "
                                  + request.getSubject())
                          .build();
                  return curriculumRepository.save(newCurriculum);
                });

    log.info("Using curriculum: {} (id={})", curriculum.getName(), curriculum.getId());

    String prompt = buildGenerationPrompt(request);

    String aiResponse;
    try {
      aiResponse = geminiService.sendMessage(prompt);
      log.info("Received AI response for lesson content generation");
    } catch (Exception e) {
      log.error("Failed to call Gemini API for lesson generation", e);
      throw new AppException(ErrorCode.LESSON_GENERATION_FAILED);
    }

    List<AiLessonData> aiLessons;
    try {
      aiLessons = parseAiResponse(aiResponse);
    } catch (Exception e) {
      log.error("Failed to parse Gemini AI response: {}", aiResponse, e);
      throw new AppException(ErrorCode.LESSON_GENERATION_FAILED);
    }

    int totalLessonsCreated = 0;
    int totalChaptersCreated = 0;
    int skippedLessons = 0;
    List<LessonResponse> savedLessons = new ArrayList<>();

    for (AiLessonData aiLesson : aiLessons) {
      // Idempotency: skip if lesson with same title already exists for this curriculum
      if (request.isSkipIfExists()) {
        // Check if a lesson with this title exists in any chapter of the curriculum
        boolean exists =
            curriculum.getChapters() != null
                && curriculum.getChapters().stream()
                    .anyMatch(
                        c ->
                            c.getLessons() != null
                                && c.getLessons().stream()
                                    .anyMatch(
                                        l ->
                                            l.getTitle().equals(aiLesson.getTitle())
                                                && l.getDeletedAt() == null));
        if (exists) {
          log.info("Skipping existing lesson: {}", aiLesson.getTitle());
          skippedLessons++;
          continue;
        }
      }

      List<Chapter> savedChapters = new ArrayList<>();
      if (aiLesson.getChapters() != null && !aiLesson.getChapters().isEmpty()) {
        for (int i = 0; i < aiLesson.getChapters().size(); i++) {
          AiChapterData aiChapter = aiLesson.getChapters().get(i);
          Chapter chapter =
              Chapter.builder()
                  .curriculumId(curriculum.getId())
                  .title(aiChapter.getTitle())
                  .description(aiChapter.getDescription())
                  .orderIndex(i + 1)
                  .build();
          Chapter savedChapter = chapterRepository.save(chapter);
          log.info("Saved chapter: {} (id={})", chapter.getTitle(), chapter.getId());

          // Create lesson under this chapter
          Lesson lesson =
              Lesson.builder()
                  .chapterId(savedChapter.getId())
                  .title(aiLesson.getTitle())
                  .learningObjectives(buildLearningObjectives(aiLesson.getTitle()))
                  .lessonContent(aiLesson.getDescription())
                  .summary(buildSummary(aiLesson.getDescription()))
                  .orderIndex(i + 1)
                  .durationMinutes(
                      aiLesson.getDurationMinutes() != null ? aiLesson.getDurationMinutes() : 45)
                  .difficulty(parseDifficulty(aiLesson.getDifficulty()))
                  .status(LessonStatus.PUBLISHED)
                  .build();

          lesson = lessonRepository.save(lesson);
          log.info("Saved lesson: {} (id={})", lesson.getTitle(), lesson.getId());
          totalLessonsCreated++;
          totalChaptersCreated++;
          savedChapters.add(savedChapter);
        }
      }

      if (!savedChapters.isEmpty()) {
        savedLessons.add(mapToLessonResponse(savedChapters));
      }
    }

    log.info(
        "Generation complete: {} lessons created, {} chapters created, {} skipped",
        totalLessonsCreated,
        totalChaptersCreated,
        skippedLessons);

    return GenerateLessonContentResponse.builder()
        .gradeLevel(request.getGradeLevel())
        .subject(request.getSubject())
        .totalLessonsCreated(totalLessonsCreated)
        .totalChaptersCreated(totalChaptersCreated)
        .skippedLessons(skippedLessons)
        .lessons(savedLessons)
        .build();
  }

  // -------------------------------------------------------------------------
  // Read operations
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public List<LessonResponse> getLessonsByGradeAndSubject(String gradeLevel, String subject) {
    log.info("Fetching lessons for gradeLevel={}, subject={}", gradeLevel, subject);
    Integer grade = parseGrade(gradeLevel);
    CurriculumCategory category = parseCategory(subject);
    List<Curriculum> curricula =
        curriculumRepository.findByGradeAndCategoryAndNotDeleted(grade, category);
    List<LessonResponse> lessons = new ArrayList<>();
    for (Curriculum curriculum : curricula) {
      for (Chapter chapter : curriculum.getChapters()) {
        if (chapter.getDeletedAt() == null) {
          lessons.addAll(mapToLessonResponseList(chapter.getLessons()));
        }
      }
    }
    return lessons;
  }

  @Override
  @Transactional(readOnly = true)
  public LessonResponse getLessonById(UUID lessonId) {
    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    return mapToLessonResponse(lesson);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChapterResponse> getChaptersByLessonId(UUID lessonId) {
    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    Chapter chapter =
        chapterRepository
            .findById(lesson.getChapterId())
            .filter(c -> c.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));
    // Return the chapter that contains this lesson
    return List.of(mapToChapterResponse(chapter));
  }

  // -------------------------------------------------------------------------
  // Delete operations (soft-delete)
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void deleteLesson(UUID lessonId) {
    UUID currentUserId = getCurrentUserId();
    Lesson lesson =
        lessonRepository
            .findByIdAndNotDeleted(lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    validateAdminRole(currentUserId);

    lesson.setDeletedAt(Instant.now());
    lessonRepository.save(lesson);

    log.info("Soft-deleted lesson {}", lessonId);
  }

  @Override
  @Transactional
  public void deleteChapter(UUID chapterId) {
    UUID currentUserId = getCurrentUserId();
    Chapter chapter =
        chapterRepository
            .findById(chapterId)
            .filter(c -> c.getDeletedAt() == null)
            .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

    validateAdminRole(currentUserId);

    chapter.setDeletedAt(Instant.now());
    chapterRepository.save(chapter);

    // soft-delete all lessons of this chapter
    if (chapter.getLessons() != null) {
      Instant now = Instant.now();
      chapter.getLessons().stream()
          .filter(l -> l.getDeletedAt() == null)
          .forEach(l -> l.setDeletedAt(now));
      lessonRepository.saveAll(
          chapter.getLessons().stream()
              .filter(l -> l.getDeletedAt() != null)
              .collect(Collectors.toList()));
    }

    log.info("Soft-deleted chapter {} and associated lessons", chapterId);
  }

  // -------------------------------------------------------------------------
  // Prompt builder
  // -------------------------------------------------------------------------

  private String buildLearningObjectives(String lessonTitle) {
    return "Students will understand and be able to apply concepts related to " + lessonTitle;
  }

  private String buildSummary(String description) {
    if (description == null || description.length() <= 100) {
      return description;
    }
    return description.substring(0, 100) + "...";
  }

  private String buildGenerationPrompt(GenerateLessonContentRequest req) {
    String extra =
        (req.getAdditionalContext() != null && !req.getAdditionalContext().isBlank())
            ? "\nAdditional context: " + req.getAdditionalContext()
            : "";

    return """
        Bạn là chuyên gia xây dựng chương trình Toán học cấp THPT Việt Nam.
        Hãy tạo %d bài học (lesson) cho môn "%s" – %s.%s

        Với mỗi bài học, hãy tạo đúng %d chương (chapter).

        Trả về CHÍNH XÁC một JSON hợp lệ (không có markdown, không có ```json, không có văn bản thừa):
        [
          {
            "title": "Tên bài học",
            "description": "Mô tả ngắn gọn nội dung bài học (2-3 câu)",
            "durationMinutes": 45,
            "difficulty": "BEGINNER|INTERMEDIATE|ADVANCED",
            "chapters": [
              {
                "title": "Tên chương/mục",
                "description": "Mô tả nội dung chương (1-2 câu)"
              }
            ]
          }
        ]

        Yêu cầu:
        - Tên bài học phải rõ ràng, đúng chương trình Toán %s
        - difficulty: chọn BEGINNER cho kiến thức cơ bản, INTERMEDIATE cho trung bình, ADVANCED cho nâng cao
        - durationMinutes: thường 45 hoặc 90
        - Các chương phải có thứ tự logic (từ lý thuyết → ví dụ → bài tập)
        - Chỉ trả về JSON array, không thêm bất kỳ nội dung nào khác
        """
        .formatted(
            req.getLessonCount(),
            req.getSubject(),
            req.getGradeLevel(),
            extra,
            req.getChaptersPerLesson(),
            req.getGradeLevel());
  }

  // -------------------------------------------------------------------------
  // AI response parser
  // -------------------------------------------------------------------------

  private List<AiLessonData> parseAiResponse(String raw) throws Exception {
    String cleaned = raw.trim();
    // Strip markdown fences if Gemini wraps in ```json ... ```
    if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
    else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
    if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
    cleaned = cleaned.trim();

    // Find first '[' to handle any leading text
    int start = cleaned.indexOf('[');
    int end = cleaned.lastIndexOf(']');
    if (start >= 0 && end > start) {
      cleaned = cleaned.substring(start, end + 1);
    }

    return objectMapper.readValue(cleaned, new TypeReference<>() {});
  }

  // -------------------------------------------------------------------------
  // Mappers
  // -------------------------------------------------------------------------

  private LessonResponse mapToLessonResponse(Lesson lesson) {
    return LessonResponse.builder()
        .id(lesson.getId())
        .chapterId(lesson.getChapterId())
        .title(lesson.getTitle())
        .learningObjectives(lesson.getLearningObjectives())
        .lessonContent(lesson.getLessonContent())
        .summary(lesson.getSummary())
        .orderIndex(lesson.getOrderIndex())
        .durationMinutes(lesson.getDurationMinutes())
        .difficulty(lesson.getDifficulty())
        .status(lesson.getStatus())
        .createdAt(lesson.getCreatedAt())
        .updatedAt(lesson.getUpdatedAt())
        .build();
  }

  private List<LessonResponse> mapToLessonResponseList(java.util.Collection<Lesson> lessons) {
    if (lessons == null) {
      return List.of();
    }
    return lessons.stream()
        .filter(l -> l.getDeletedAt() == null)
        .map(this::mapToLessonResponse)
        .collect(Collectors.toList());
  }

  private LessonResponse mapToLessonResponse(List<Chapter> chapters) {
    if (chapters == null || chapters.isEmpty()) {
      return LessonResponse.builder().build();
    }
    Chapter first = chapters.get(0);
    if (first.getLessons() == null || first.getLessons().isEmpty()) {
      return LessonResponse.builder().build();
    }
    Lesson firstLesson =
        first.getLessons().stream().filter(l -> l.getDeletedAt() == null).findFirst().orElse(null);
    if (firstLesson == null) {
      return LessonResponse.builder().build();
    }
    return mapToLessonResponse(firstLesson);
  }

  private ChapterResponse mapToChapterResponse(Chapter chapter) {
    return ChapterResponse.builder()
        .id(chapter.getId())
        .title(chapter.getTitle())
        .description(chapter.getDescription())
        .orderIndex(chapter.getOrderIndex())
        .build();
  }

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth
        instanceof
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
                jwtAuth) {
      String sub = jwtAuth.getToken().getSubject();
      return UUID.fromString(sub);
    }

    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private void validateAdminRole(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
    if (!isAdmin) {
      throw new AppException(ErrorCode.NOT_A_TEACHER);
    }
  }

  private Integer parseGrade(String gradeLevelStr) {
    if (gradeLevelStr == null) return 10;
    try {
      // Try to extract number from strings like "Grade 10", "10", etc.
      String numStr = gradeLevelStr.replaceAll("[^0-9]", "");
      if (numStr.isEmpty()) return 10;
      Integer grade = Integer.parseInt(numStr);
      if (grade < 1) return 1;
      if (grade > 12) return 12;
      return grade;
    } catch (Exception e) {
      return 10;
    }
  }

  private CurriculumCategory parseCategory(String subject) {
    if (subject == null) return CurriculumCategory.NUMERICAL;
    String lower = subject.toLowerCase();
    if (lower.contains("geometry") || lower.contains("hình")) {
      return CurriculumCategory.GEOMETRY;
    }
    return CurriculumCategory.NUMERICAL;
  }

  private String buildCurriculumName(String gradeLevel, String subject) {
    return "Mathematics " + gradeLevel + " - " + subject;
  }

  // -------------------------------------------------------------------------
  // Helper
  // -------------------------------------------------------------------------

  private LessonDifficulty parseDifficulty(String raw) {
    if (raw == null) return LessonDifficulty.INTERMEDIATE;
    try {
      return LessonDifficulty.valueOf(raw.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return LessonDifficulty.INTERMEDIATE;
    }
  }

  // -------------------------------------------------------------------------
  // Inner DTOs for Gemini JSON parsing
  // -------------------------------------------------------------------------

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class AiLessonData {
    private String title;
    private String description;
    private Integer durationMinutes;
    private String difficulty;
    private List<AiChapterData> chapters;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class AiChapterData {
    private String title;
    private String description;
  }
}
