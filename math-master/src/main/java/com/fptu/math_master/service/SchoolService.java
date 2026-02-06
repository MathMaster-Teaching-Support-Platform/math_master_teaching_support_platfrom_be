package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.SchoolRequest;
import com.fptu.math_master.dto.response.SchoolResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SchoolService {

    /**
     * Create new school (Admin only)
     */
    SchoolResponse createSchool(SchoolRequest request);

    /**
     * Update school information (Admin only)
     */
    SchoolResponse updateSchool(Long schoolId, SchoolRequest request);

    /**
     * Get school by ID
     */
    SchoolResponse getSchoolById(Long schoolId);

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
    void deleteSchool(Long schoolId);
}
