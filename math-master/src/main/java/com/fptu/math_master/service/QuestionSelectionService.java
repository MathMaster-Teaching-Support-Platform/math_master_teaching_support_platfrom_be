package com.fptu.math_master.service;

import com.fptu.math_master.entity.AssessmentQuestion;
import java.util.List;
import java.util.UUID;

public interface QuestionSelectionService {

  void validateAvailability(UUID examMatrixId);

  SelectionPlan buildSelectionPlan(UUID assessmentId, UUID examMatrixId, int startOrderIndex);

  record SelectionPlan(List<AssessmentQuestion> assessmentQuestions, int totalPoints) {}
}
