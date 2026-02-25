package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.dto.request.GenerateLessonContentRequest;
import com.fptu.math_master.dto.response.ChapterResponse;
import com.fptu.math_master.dto.response.GenerateLessonContentResponse;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.LessonDifficulty;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
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
  UserRepository userRepository;
  GeminiService geminiService;
  ObjectMapper objectMapper;

  // -------------------------------------------------------------------------
  // AI Generation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public GenerateLessonContentResponse generateAndSaveContent(GenerateLessonContentRequest request) {
    log.info(
        "Generating lesson content for gradeLevel={}, subject={}", request.getGradeLevel(), request.getSubject());

    UUID currentUserId = getCurrentUserId();
    validateTeacherRole(currentUserId);

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
      // Idempotency: skip if lesson with same title already exists for this grade+subject
      if (request.isSkipIfExists()) {
        long count = lessonRepository.countByGradeLevelAndSubjectAndTitle(
            request.getGradeLevel(), request.getSubject(), aiLesson.getTitle());
        if (count > 0) {
          log.info("Skipping existing lesson: {}", aiLesson.getTitle());
          skippedLessons++;
          continue;
        }
      }

      Lesson lesson = Lesson.builder()
          .teacherId(currentUserId)
          .title(aiLesson.getTitle())
          .description(aiLesson.getDescription())
          .subject(request.getSubject())
          .gradeLevel(request.getGradeLevel())
          .durationMinutes(aiLesson.getDurationMinutes() != null ? aiLesson.getDurationMinutes() : 45)
          .difficulty(parseDifficulty(aiLesson.getDifficulty()))
          .status(LessonStatus.PUBLISHED)
          .build();

      lesson = lessonRepository.save(lesson);
      log.info("Saved lesson: {} (id={})", lesson.getTitle(), lesson.getId());
      totalLessonsCreated++;

      List<Chapter> savedChapters = new ArrayList<>();
      if (aiLesson.getChapters() != null) {
        for (int i = 0; i < aiLesson.getChapters().size(); i++) {
          AiChapterData aiChapter = aiLesson.getChapters().get(i);
          Chapter chapter = Chapter.builder()
              .lessonId(lesson.getId())
              .title(aiChapter.getTitle())
              .description(aiChapter.getDescription())
              .orderIndex(i + 1)
              .build();
          savedChapters.add(chapterRepository.save(chapter));
          totalChaptersCreated++;
        }
      }

      savedLessons.add(mapToLessonResponse(lesson, savedChapters));
    }

    log.info(
        "Generation complete: {} lessons created, {} chapters created, {} skipped",
        totalLessonsCreated, totalChaptersCreated, skippedLessons);

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
    List<Lesson> lessons = lessonRepository.findByGradeLevelAndSubjectAndNotDeleted(gradeLevel, subject);
    return lessons.stream()
        .map(l -> mapToLessonResponse(l, chapterRepository.findByLessonIdAndNotDeleted(l.getId())))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public LessonResponse getLessonById(UUID lessonId) {
    Lesson lesson = lessonRepository.findById(lessonId)
        .filter(l -> l.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    List<Chapter> chapters = chapterRepository.findByLessonIdAndNotDeleted(lessonId);
    return mapToLessonResponse(lesson, chapters);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ChapterResponse> getChaptersByLessonId(UUID lessonId) {
    lessonRepository.findById(lessonId)
        .filter(l -> l.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
    return chapterRepository.findByLessonIdAndNotDeleted(lessonId)
        .stream()
        .map(this::mapToChapterResponse)
        .collect(Collectors.toList());
  }

  // -------------------------------------------------------------------------
  // Delete operations (soft-delete)
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void deleteLesson(UUID lessonId) {
    UUID currentUserId = getCurrentUserId();
    Lesson lesson = lessonRepository.findById(lessonId)
        .filter(l -> l.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    validateLessonOwner(lesson.getTeacherId(), currentUserId);

    lesson.setDeletedAt(Instant.now());
    lessonRepository.save(lesson);

    // soft-delete all chapters of this lesson
    List<Chapter> chapters = chapterRepository.findByLessonIdAndNotDeleted(lessonId);
    Instant now = Instant.now();
    chapters.forEach(c -> c.setDeletedAt(now));
    chapterRepository.saveAll(chapters);

    log.info("Soft-deleted lesson {} and {} chapters", lessonId, chapters.size());
  }

  @Override
  @Transactional
  public void deleteChapter(UUID chapterId) {
    UUID currentUserId = getCurrentUserId();
    Chapter chapter = chapterRepository.findById(chapterId)
        .filter(c -> c.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

    Lesson lesson = lessonRepository.findById(chapter.getLessonId())
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

    validateLessonOwner(lesson.getTeacherId(), currentUserId);

    chapter.setDeletedAt(Instant.now());
    chapterRepository.save(chapter);
    log.info("Soft-deleted chapter {}", chapterId);
  }

  // -------------------------------------------------------------------------
  // Prompt builder
  // -------------------------------------------------------------------------

  private String buildGenerationPrompt(GenerateLessonContentRequest req) {
    String extra = (req.getAdditionalContext() != null && !req.getAdditionalContext().isBlank())
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
        """.formatted(
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

  private LessonResponse mapToLessonResponse(Lesson lesson, List<Chapter> chapters) {
    return LessonResponse.builder()
        .id(lesson.getId())
        .teacherId(lesson.getTeacherId())
        .title(lesson.getTitle())
        .description(lesson.getDescription())
        .subject(lesson.getSubject())
        .gradeLevel(lesson.getGradeLevel())
        .durationMinutes(lesson.getDurationMinutes())
        .difficulty(lesson.getDifficulty())
        .status(lesson.getStatus())
        .chapters(chapters.stream().map(this::mapToChapterResponse).collect(Collectors.toList()))
        .build();
  }

  private ChapterResponse mapToChapterResponse(Chapter chapter) {
    return ChapterResponse.builder()
        .id(chapter.getId())
        .lessonId(chapter.getLessonId())
        .title(chapter.getTitle())
        .description(chapter.getDescription())
        .orderIndex(chapter.getOrderIndex())
        .build();
  }

  private UUID getCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      String sub = jwtAuth.getToken().getSubject();
      return UUID.fromString(sub);
    }

    throw new IllegalStateException("Authentication is not JwtAuthenticationToken");
  }

  private void validateTeacherRole(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    boolean isTeacherOrAdmin = user.getRoles().stream()
        .anyMatch(r -> r.getName().equals("TEACHER") || r.getName().equals("ADMIN"));
    if (!isTeacherOrAdmin) {
      throw new AppException(ErrorCode.NOT_A_TEACHER);
    }
  }

  private void validateLessonOwner(UUID ownerId, UUID currentUserId) {
    User current = userRepository.findById(currentUserId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    boolean isAdmin = current.getRoles().stream().anyMatch(r -> r.getName().equals("ADMIN"));
    if (!isAdmin && !ownerId.equals(currentUserId)) {
      throw new AppException(ErrorCode.LESSON_ACCESS_DENIED);
    }
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

