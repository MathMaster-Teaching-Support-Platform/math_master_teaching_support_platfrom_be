package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.UpdateProgressRequest;
import com.fptu.math_master.dto.response.LessonProgressItem;
import com.fptu.math_master.dto.response.StudentProgressResponse;
import java.util.UUID;

public interface ProgressService {

  LessonProgressItem markComplete(UUID enrollmentId, UUID courseLessonId);

  LessonProgressItem updateProgress(UUID enrollmentId, UUID courseLessonId, UpdateProgressRequest request);

  StudentProgressResponse getProgress(UUID enrollmentId);
}
