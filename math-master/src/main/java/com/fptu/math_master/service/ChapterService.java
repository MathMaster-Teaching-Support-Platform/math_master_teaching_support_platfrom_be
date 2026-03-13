package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateChapterRequest;
import com.fptu.math_master.dto.request.UpdateChapterRequest;
import com.fptu.math_master.dto.response.ChapterResponse;
import java.util.List;
import java.util.UUID;

public interface ChapterService {

  ChapterResponse createChapter(CreateChapterRequest request);

  ChapterResponse getChapterById(UUID id);

  List<ChapterResponse> getChaptersByCurriculumId(UUID curriculumId);

  ChapterResponse updateChapter(UUID id, UpdateChapterRequest request);

  void deleteChapter(UUID id);
}
