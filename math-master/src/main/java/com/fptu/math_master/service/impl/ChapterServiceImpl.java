package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.request.CreateChapterRequest;
import com.fptu.math_master.dto.request.UpdateChapterRequest;
import com.fptu.math_master.dto.response.ChapterResponse;
import com.fptu.math_master.entity.Chapter;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.ChapterRepository;
import com.fptu.math_master.repository.CurriculumRepository;
import com.fptu.math_master.service.ChapterService;
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
public class ChapterServiceImpl implements ChapterService {

  ChapterRepository chapterRepository;
  CurriculumRepository curriculumRepository;

  @Override
  @Transactional
  public ChapterResponse createChapter(CreateChapterRequest request) {
    // Validate curriculum exists
    curriculumRepository
        .findById(request.getCurriculumId())
        .filter(c -> c.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

    int orderIndex =
        request.getOrderIndex() != null
            ? request.getOrderIndex()
            : chapterRepository
                    .countByCurriculumIdAndNotDeleted(request.getCurriculumId())
                    .intValue()
                + 1;

    Chapter chapter =
        Chapter.builder()
            .curriculumId(request.getCurriculumId())
            .title(request.getTitle())
            .description(request.getDescription())
            .orderIndex(orderIndex)
            .build();

    return toResponse(chapterRepository.save(chapter));
  }

  @Override
  public ChapterResponse getChapterById(UUID id) {
    return toResponse(findActiveChapter(id));
  }

  @Override
  public List<ChapterResponse> getChaptersByCurriculumId(UUID curriculumId) {
    curriculumRepository
        .findById(curriculumId)
        .filter(c -> c.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.CURRICULUM_NOT_FOUND));

    return chapterRepository.findByCurriculumIdAndNotDeleted(curriculumId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public ChapterResponse updateChapter(UUID id, UpdateChapterRequest request) {
    Chapter chapter = findActiveChapter(id);

    if (request.getTitle() != null) chapter.setTitle(request.getTitle());
    if (request.getDescription() != null) chapter.setDescription(request.getDescription());
    if (request.getOrderIndex() != null) chapter.setOrderIndex(request.getOrderIndex());

    return toResponse(chapterRepository.save(chapter));
  }

  @Override
  @Transactional
  public void deleteChapter(UUID id) {
    Chapter chapter = findActiveChapter(id);
    chapter.setDeletedAt(Instant.now());
    chapterRepository.save(chapter);
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  private Chapter findActiveChapter(UUID id) {
    return chapterRepository
        .findById(id)
        .filter(c -> c.getDeletedAt() == null)
        .orElseThrow(() -> new AppException(ErrorCode.CHAPTER_NOT_FOUND));
  }

  private ChapterResponse toResponse(Chapter c) {
    return ChapterResponse.builder()
        .id(c.getId())
        .curriculumId(c.getCurriculumId())
        .title(c.getTitle())
        .description(c.getDescription())
        .orderIndex(c.getOrderIndex())
        .createdAt(c.getCreatedAt())
        .updatedAt(c.getUpdatedAt())
        .build();
  }
}
