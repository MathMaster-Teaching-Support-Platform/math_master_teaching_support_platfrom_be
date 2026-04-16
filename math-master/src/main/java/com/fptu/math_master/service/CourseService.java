package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.AddAssessmentToCourseRequest;
import com.fptu.math_master.dto.request.CreateCourseRequest;
import com.fptu.math_master.dto.request.UpdateCourseAssessmentRequest;
import com.fptu.math_master.dto.request.UpdateCourseRequest;
import com.fptu.math_master.dto.response.CourseAssessmentResponse;
import com.fptu.math_master.dto.response.CourseResponse;
import com.fptu.math_master.dto.response.StudentInCourseResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

  CourseResponse createCourse(CreateCourseRequest request);

  CourseResponse updateCourse(UUID courseId, UpdateCourseRequest request);

  void deleteCourse(UUID courseId);

  CourseResponse publishCourse(UUID courseId, boolean publish);

  List<CourseResponse> getMyCourses();

  CourseResponse getCourseById(UUID courseId);

  /** Filter theo schoolGradeId, subjectId, keyword */
  Page<CourseResponse> getPublicCourses(UUID schoolGradeId, UUID subjectId, String keyword, Pageable pageable);

  Page<StudentInCourseResponse> getStudentsInCourse(UUID courseId, Pageable pageable);

  // ─── Course Assessment Management ─────────────────────────────────────────

  /**
   * Add an assessment to a course
   * @param courseId Course ID
   * @param request Request containing assessment ID, order index, and required flag
   * @return CourseAssessmentResponse with assessment details
   */
  CourseAssessmentResponse addAssessmentToCourse(UUID courseId, AddAssessmentToCourseRequest request);

  /**
   * Get all assessments in a course with optional filtering
   * @param courseId Course ID
   * @param status Filter by assessment status (DRAFT, PUBLISHED, CLOSED)
   * @param type Filter by assessment type (QUIZ, TEST, EXAM, HOMEWORK)
   * @param isRequired Filter by required flag
   * @return List of assessments ordered by orderIndex
   */
  List<CourseAssessmentResponse> getCourseAssessments(UUID courseId, String status, String type, Boolean isRequired);

  /**
   * Update course assessment settings (order, required flag)
   * @param courseId Course ID
   * @param assessmentId Assessment ID
   * @param request Update request
   * @return Updated CourseAssessmentResponse
   */
  CourseAssessmentResponse updateCourseAssessment(
      UUID courseId, UUID assessmentId, UpdateCourseAssessmentRequest request);

  /**
   * Remove an assessment from a course (soft delete)
   * @param courseId Course ID
   * @param assessmentId Assessment ID
   */
  void removeAssessmentFromCourse(UUID courseId, UUID assessmentId);
}
