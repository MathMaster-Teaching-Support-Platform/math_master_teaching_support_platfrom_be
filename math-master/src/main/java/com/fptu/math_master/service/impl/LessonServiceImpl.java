package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateLessonRequest;
import com.fptu.math_master.dto.request.UpdateLessonRequest;
import com.fptu.math_master.dto.response.LessonResponse;
import com.fptu.math_master.entity.Lesson;
import com.fptu.math_master.enums.LessonStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.LessonRepository;
import com.fptu.math_master.service.LessonService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonServiceImpl implements LessonService {

  LessonRepository lessonRepository;
  ChapterRepository chapterRepository;

  @Override
  @Transactional
  public LessonResponse createLesson(CreateLessonRequest request) {
    // Validate chapter exists
    chapterRepository
        .findById(request.getChapterId())
        .filter(c -> c.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

    int orderIndex =
        request.getOrderIndex() != null
            ? request.getOrderIndex()
            : lessonRepository.countByChapterIdAndNotDeleted(request.getChapterId()).intValue() + 1;

    Lesson lesson =
        Lesson.builder()
            .chapterId(request.getChapterId())
            .title(request.getTitle())
            .learningObjectives(request.getLearningObjectives())
            .lessonContent(request.getLessonContent())
            .summary(request.getSummary())
            .orderIndex(orderIndex)
            .durationMinutes(request.getDurationMinutes())
            .difficulty(request.getDifficulty())
            .status(LessonStatus.DRAFT)
            .build();

    return toResponse(lessonRepository.save(lesson));
  }

  @Override
  public LessonResponse getLessonById(UUID id) {
    return toResponse(findActiveLesson(id));
  }

  @Override
  public List<LessonResponse> getLessonsByChapterId(UUID chapterId) {
    chapterRepository
        .findById(chapterId)
        .filter(c -> c.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));

    return lessonRepository.findByChapterIdAndNotDeleted(chapterId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public LessonResponse updateLesson(UUID id, UpdateLessonRequest request) {
    Lesson lesson = findActiveLesson(id);

    if (request.getTitle() != null) lesson.setTitle(request.getTitle());
    if (request.getLearningObjectives() != null)
      lesson.setLearningObjectives(request.getLearningObjectives());
    if (request.getLessonContent() != null) lesson.setLessonContent(request.getLessonContent());
    if (request.getSummary() != null) lesson.setSummary(request.getSummary());
    if (request.getOrderIndex() != null) lesson.setOrderIndex(request.getOrderIndex());
    if (request.getDurationMinutes() != null)
      lesson.setDurationMinutes(request.getDurationMinutes());
    if (request.getDifficulty() != null) lesson.setDifficulty(request.getDifficulty());
    if (request.getStatus() != null) lesson.setStatus(request.getStatus());

    return toResponse(lessonRepository.save(lesson));
  }

  @Override
  @Transactional
  public void deleteLesson(UUID id) {
    Lesson lesson = findActiveLesson(id);
    lesson.setDeletedAt(Instant.now());
    lessonRepository.save(lesson);
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private Lesson findActiveLesson(UUID id) {
    return lessonRepository
        .findByIdAndNotDeleted(id)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
  }

  private LessonResponse toResponse(Lesson l) {
    return LessonResponse.builder()
        .id(l.getId())
        .chapterId(l.getChapterId())
        .title(l.getTitle())
        .learningObjectives(l.getLearningObjectives())
        .lessonContent(l.getLessonContent())
        .summary(l.getSummary())
        .orderIndex(l.getOrderIndex())
        .durationMinutes(l.getDurationMinutes())
        .difficulty(l.getDifficulty())
        .status(l.getStatus())
        .createdAt(l.getCreatedAt())
        .updatedAt(l.getUpdatedAt())
        .build();
  }
}
