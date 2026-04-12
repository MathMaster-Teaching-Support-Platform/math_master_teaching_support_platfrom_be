package com.fptu.math_master.service;

import com.fptu.math_master.dto.request.ProfileReviewRequest;
import com.fptu.math_master.dto.request.TeacherProfileRequest;
import com.fptu.math_master.dto.response.TeacherProfileResponse;
import com.fptu.math_master.enums.ProfileStatus;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface TeacherProfileService {

  /**
   * Submit teacher profile for approval
   *
   * @param request Profile information
   * @param file Verification document file
   * @param userId Current user ID
   * @return Created profile response
   */
  TeacherProfileResponse submitProfile(
    TeacherProfileRequest request, List<MultipartFile> file, UUID userId);

  /**
   * Update pending teacher profile
   *
   * @param request Updated profile information
   * @param files Verification document files (optional for update)
   * @param userId Current user ID
   * @return Updated profile response
   */
  TeacherProfileResponse updateProfile(TeacherProfileRequest request, List<MultipartFile> files, UUID userId);

  /**
   * Get user's teacher profile
   *
   * @param userId User ID
   * @return Profile response
   */
  TeacherProfileResponse getMyProfile(UUID userId);

  /**
   * Get teacher profile by ID (Admin only)
   *
   * @param profileId Profile ID
   * @return Profile response
   */
  TeacherProfileResponse getProfileById(UUID profileId);

  /**
   * Get all profiles by status
   *
   * @param status Profile status
   * @param pageable Pagination info
   * @return Page of profiles
   */
  Page<TeacherProfileResponse> getProfilesByStatus(ProfileStatus status, Pageable pageable);

  /**
   * Admin review (approve/reject) teacher profile
   *
   * @param profileId Profile ID to review
   * @param request Review decision and comment
   * @param adminId Admin user ID
   * @return Updated profile response
   */
  TeacherProfileResponse reviewProfile(UUID profileId, ProfileReviewRequest request, UUID adminId);

  /**
   * Count pending profiles
   *
   * @return Number of pending profiles
   */
  long countPendingProfiles();

  /**
   * Get a pre-signed download URL for the teacher's verification document
   *
   * @param profileId Profile ID
   * @return Pre-signed URL string
   */
  String getDownloadUrl(UUID profileId);

  /**
   * Delete profile (only if PENDING or REJECTED)
   *
   * @param userId Current user ID
   */
  void deleteMyProfile(UUID userId);
}
