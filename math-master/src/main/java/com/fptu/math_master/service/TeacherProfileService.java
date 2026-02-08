package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.ProfileReviewRequest;
import com.fptu.math_master.dto.request.TeacherProfileRequest;
import com.fptu.math_master.dto.response.TeacherProfileResponse;
import com.fptu.math_master.enums.ProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeacherProfileService {

  /**
   * Submit teacher profile for approval
   *
   * @param request Profile information
   * @param userId  Current user ID
   * @return Created profile response
   */
  TeacherProfileResponse submitProfile(TeacherProfileRequest request, Integer userId);

  /**
   * Update pending teacher profile
   *
   * @param request Updated profile information
   * @param userId  Current user ID
   * @return Updated profile response
   */
  TeacherProfileResponse updateProfile(TeacherProfileRequest request, Integer userId);

  /**
   * Get user's teacher profile
   *
   * @param userId User ID
   * @return Profile response
   */
  TeacherProfileResponse getMyProfile(Integer userId);

  /**
   * Get teacher profile by ID (Admin only)
   *
   * @param profileId Profile ID
   * @return Profile response
   */
  TeacherProfileResponse getProfileById(Long profileId);

  /**
   * Get all profiles by status
   *
   * @param status   Profile status
   * @param pageable Pagination info
   * @return Page of profiles
   */
  Page<TeacherProfileResponse> getProfilesByStatus(ProfileStatus status, Pageable pageable);

  /**
   * Admin review (approve/reject) teacher profile
   *
   * @param profileId Profile ID to review
   * @param request   Review decision and comment
   * @param adminId   Admin user ID
   * @return Updated profile response
   */
  TeacherProfileResponse reviewProfile(Long profileId, ProfileReviewRequest request, Integer adminId);

  /**
   * Count pending profiles
   *
   * @return Number of pending profiles
   */
  long countPendingProfiles();

  /**
   * Delete profile (only if PENDING or REJECTED)
   *
   * @param userId Current user ID
   */
  void deleteMyProfile(Integer userId);
}
