package com.fptu.math_master.service.impl;

import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.ProfileReviewRequest;
import com.fptu.math_master.dto.request.TeacherProfileRequest;
import com.fptu.math_master.dto.response.TeacherProfileResponse;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.ProfileStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.FileStorageService;
import com.fptu.math_master.service.TeacherProfileService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TeacherProfileServiceImpl implements TeacherProfileService {

  TeacherProfileRepository teacherProfileRepository;
  UserRepository userRepository;
  RoleRepository roleRepository;
  FileStorageService fileStorageService;

  @Override
  @Transactional
  public TeacherProfileResponse submitProfile(
      TeacherProfileRequest request, MultipartFile file, UUID userId) {
    log.info(
        "User {} submitting teacher profile with file: {}", userId, file.getOriginalFilename());

    // Check if user exists
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // Check if profile already exists
    if (teacherProfileRepository.existsByUserId(userId)) {
      throw new AppException(ErrorCode.PROFILE_ALREADY_EXISTS);
    }

    // Save physical file
    String documentUrl = fileStorageService.storeFile(file, "verification");

    // Create new profile
    TeacherProfile profile =
        TeacherProfile.builder()
            .user(user)
            .schoolName(request.getSchoolName())
            .schoolAddress(request.getSchoolAddress())
            .schoolWebsite(request.getSchoolWebsite())
            .position(request.getPosition())
            .documentUrl(documentUrl)
            .documentType(request.getDocumentType())
            .description(request.getDescription())
            .status(ProfileStatus.PENDING)
            .build();

    profile = teacherProfileRepository.save(profile);
    log.info("Teacher profile submitted successfully for user {}", userId);

    return mapToResponse(profile);
  }

  @Override
  @Transactional
  public TeacherProfileResponse updateProfile(TeacherProfileRequest request, UUID userId) {
    log.info("User {} updating teacher profile", userId);

    TeacherProfile profile =
        teacherProfileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    // Only PENDING or REJECTED profiles can be updated
    if (profile.getStatus() == ProfileStatus.APPROVED) {
      throw new AppException(ErrorCode.PROFILE_CANNOT_BE_MODIFIED);
    }

    // Update profile fields
    profile.setSchoolName(request.getSchoolName());
    profile.setSchoolAddress(request.getSchoolAddress());
    profile.setSchoolWebsite(request.getSchoolWebsite());
    profile.setPosition(request.getPosition());
    profile.setDocumentType(request.getDocumentType());
    profile.setDescription(request.getDescription());

    // If updating a rejected profile, reset status to PENDING
    if (profile.getStatus() == ProfileStatus.REJECTED) {
      profile.setStatus(ProfileStatus.PENDING);
      profile.setAdminComment(null);
      profile.setReviewedBy(null);
      profile.setReviewedAt(null);
    }

    profile = teacherProfileRepository.save(profile);
    log.info("Teacher profile updated successfully for user {}", userId);

    return mapToResponse(profile);
  }

  @Override
  public TeacherProfileResponse getMyProfile(UUID userId) {
    TeacherProfile profile =
        teacherProfileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    return mapToResponse(profile);
  }

  @Override
  public TeacherProfileResponse getProfileById(UUID profileId) {
    TeacherProfile profile =
        teacherProfileRepository
            .findById(profileId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    return mapToResponse(profile);
  }

  @Override
  public Page<TeacherProfileResponse> getProfilesByStatus(ProfileStatus status, Pageable pageable) {
    Page<TeacherProfile> profiles =
        teacherProfileRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

    return profiles.map(this::mapToResponse);
  }

  @Override
  @Transactional
  public TeacherProfileResponse reviewProfile(
      UUID profileId, ProfileReviewRequest request, UUID adminId) {
    log.info("Admin {} reviewing profile {}", adminId, profileId);

    TeacherProfile profile =
        teacherProfileRepository
            .findById(profileId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    // Check if profile is in PENDING status
    if (profile.getStatus() != ProfileStatus.PENDING) {
      throw new AppException(ErrorCode.INVALID_PROFILE_STATUS);
    }

    // Validate review status
    if (request.getStatus() != ProfileStatus.APPROVED
        && request.getStatus() != ProfileStatus.REJECTED) {
      throw new AppException(ErrorCode.INVALID_PROFILE_STATUS);
    }

    // Update profile status
    profile.setStatus(request.getStatus());
    profile.setAdminComment(request.getAdminComment());
    profile.setReviewedBy(adminId);
    profile.setReviewedAt(LocalDateTime.now());

    // If approved, upgrade user role to TEACHER
    if (request.getStatus() == ProfileStatus.APPROVED) {
      User user = profile.getUser();
      Role teacherRole =
          roleRepository
              .findByName(PredefinedRole.TEACHER_ROLE)
              .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));

      Set<Role> roles = new HashSet<>(user.getRoles());
      roles.add(teacherRole);
      user.setRoles(roles);
      userRepository.save(user);

      log.info("User {} upgraded to TEACHER role", user.getId());
    }

    profile = teacherProfileRepository.save(profile);
    log.info("Profile {} reviewed with status {}", profileId, request.getStatus());

    return mapToResponse(profile);
  }

  @Override
  public long countPendingProfiles() {
    return teacherProfileRepository.countPendingProfiles();
  }

  @Override
  @Transactional
  public void deleteMyProfile(UUID userId) {
    TeacherProfile profile =
        teacherProfileRepository
            .findByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    // Can only delete PENDING or REJECTED profiles
    if (profile.getStatus() == ProfileStatus.APPROVED) {
      throw new AppException(ErrorCode.PROFILE_CANNOT_BE_MODIFIED);
    }

    teacherProfileRepository.delete(profile);
    log.info("Profile deleted for user {}", userId);
  }

  private TeacherProfileResponse mapToResponse(TeacherProfile profile) {
    TeacherProfileResponse response =
        TeacherProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUser().getId())
            .userName(profile.getUser().getUserName())
            .fullName(profile.getUser().getFullName())
            .schoolName(profile.getSchoolName())
            .schoolAddress(profile.getSchoolAddress())
            .schoolWebsite(profile.getSchoolWebsite())
            .position(profile.getPosition())
            .documentUrl(profile.getDocumentUrl())
            .documentType(profile.getDocumentType())
            .description(profile.getDescription())
            .status(profile.getStatus())
            .adminComment(profile.getAdminComment())
            .reviewedBy(profile.getReviewedBy())
            .reviewedAt(profile.getReviewedAt())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();

    // Get reviewer name if available
    if (profile.getReviewedBy() != null) {
      userRepository
          .findById(profile.getReviewedBy())
          .ifPresent(reviewer -> response.setReviewedByName(reviewer.getFullName()));
    }

    return response;
  }
}
