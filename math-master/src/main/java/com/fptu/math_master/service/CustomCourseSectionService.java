package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.CreateCustomCourseSectionRequest;
import com.fptu.math_master.dto.request.UpdateCustomCourseSectionRequest;
import com.fptu.math_master.dto.response.CustomCourseSectionResponse;
import java.util.List;
import java.util.UUID;

/** CRUD service for {@link com.fptu.math_master.entity.CustomCourseSection}. */
public interface CustomCourseSectionService {

  CustomCourseSectionResponse createSection(UUID courseId, CreateCustomCourseSectionRequest request);

  List<CustomCourseSectionResponse> listSections(UUID courseId);

  CustomCourseSectionResponse updateSection(
      UUID courseId, UUID sectionId, UpdateCustomCourseSectionRequest request);

  void deleteSection(UUID courseId, UUID sectionId);
}
