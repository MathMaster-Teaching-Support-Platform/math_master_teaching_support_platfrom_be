package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.SchoolRequest;
import com.fptu.math_master.dto.response.SchoolResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SchoolService {

  /**
   * Create new school (Admin only)
   */
  SchoolResponse createSchool(SchoolRequest request);

  /**
   * Update school information (Admin only)
   */
  SchoolResponse updateSchool(UUID schoolId, SchoolRequest request);

  /**
   * Get school by ID
   */
  SchoolResponse getSchoolById(UUID schoolId);

  /**
   * Get all schools with pagination
   */
  Page<SchoolResponse> getAllSchools(Pageable pageable);

  /**
   * Get all schools (no pagination)
   */
  List<SchoolResponse> getAllSchools();

  /**
   * Search schools by name
   */
  List<SchoolResponse> searchSchoolsByName(String name);

  /**
   * Delete school (Admin only)
   */
  void deleteSchool(UUID schoolId);
}
