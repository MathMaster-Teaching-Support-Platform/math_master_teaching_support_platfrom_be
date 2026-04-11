package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.LessonProgressItem;
import com.fptu.math_master.dto.response.StudentProgressResponse;
import java.util.UUID;

public interface ProgressService {

  LessonProgressItem markComplete(UUID enrollmentId, UUID courseLessonId);

  StudentProgressResponse getProgress(UUID enrollmentId);
}
