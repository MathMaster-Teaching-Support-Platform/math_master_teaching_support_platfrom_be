package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateSubjectRequest;
import com.fptu.math_master.dto.request.LinkGradeSubjectRequest;
import com.fptu.math_master.dto.response.SubjectResponse;
import java.util.List;
import java.util.UUID;

public interface SubjectService {

  /** Create a new subject (ADMIN only). */
  SubjectResponse createSubject(CreateSubjectRequest request);

  /** Get a subject by its id. */
  SubjectResponse getSubjectById(UUID subjectId);

  /** List all active subjects. */
  List<SubjectResponse> getAllSubjects();

  /** List subjects available for a given grade level. */
  List<SubjectResponse> getSubjectsByGrade(Integer gradeLevel);

  /** Assign a subject to a grade level. */
  SubjectResponse linkToGrade(UUID subjectId, LinkGradeSubjectRequest request);

  /** Remove grade assignment from a subject. */
  void unlinkFromGrade(UUID subjectId, Integer gradeLevel);

  /** Deactivate (soft-delete) a subject. */
  void deactivateSubject(UUID subjectId);
}
