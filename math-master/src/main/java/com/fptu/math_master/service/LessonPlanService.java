package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateLessonPlanRequest;
import com.fptu.math_master.dto.request.UpdateLessonPlanRequest;
import com.fptu.math_master.dto.response.LessonPlanResponse;
import java.util.List;
import java.util.UUID;

public interface LessonPlanService {

  LessonPlanResponse createLessonPlan(CreateLessonPlanRequest request);

  LessonPlanResponse updateLessonPlan(UUID lessonPlanId, UpdateLessonPlanRequest request);

  LessonPlanResponse getLessonPlanById(UUID lessonPlanId);

  LessonPlanResponse getMyLessonPlanByLesson(UUID lessonId);

  List<LessonPlanResponse> getMyLessonPlans();

  List<LessonPlanResponse> getLessonPlansByLesson(UUID lessonId);

  void deleteLessonPlan(UUID lessonPlanId);
}
