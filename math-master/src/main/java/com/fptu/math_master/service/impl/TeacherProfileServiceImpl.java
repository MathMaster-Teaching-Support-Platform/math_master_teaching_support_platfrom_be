package com.fptu.math_master.service.impl;

import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.request.ProfileReviewRequest;
import com.fptu.math_master.dto.request.TeacherProfileRequest;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.response.TeacherProfileResponse;
import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.entity.Role;
import com.fptu.math_master.entity.TeacherProfile;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.ProfileStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.RoleRepository;
import com.fptu.math_master.repository.TeacherProfileRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.TeacherProfileService;
import com.fptu.math_master.service.UploadService;
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
  UploadService uploadService;
  MinioProperties minioProperties;
  com.fptu.math_master.service.EmailService emailService;
  StreamPublisher streamPublisher;
  com.fptu.math_master.service.async.OcrJobProducer ocrJobProducer; // Add OCR job producer

  @Override
  @Transactional
  public TeacherProfileResponse submitProfile(
      TeacherProfileRequest request, java.util.List<MultipartFile> files, UUID userId) {
    log.info(
        "User {} submitting teacher profile with {} files", userId, files.size());

    // Check if user exists
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    // Check if profile already exists
    if (teacherProfileRepository.existsByUserId(userId)) {
      throw new AppException(ErrorCode.PROFILE_ALREADY_EXISTS);
    }

    // Save physical files as a zip
    String documentUrl = uploadService.uploadFilesAsZip(files, "verification", "verification_docs");

    // Create new profile
    TeacherProfile profile =
        TeacherProfile.builder()
            .user(user)
            .fullName(request.getFullName()) // Use fullName from request for OCR verification
            .schoolName(request.getSchoolName())
            .schoolAddress(request.getSchoolAddress())
            .schoolWebsite(request.getSchoolWebsite())
            .position(request.getPosition())
            .verificationDocumentKey(documentUrl)
            .verificationDocumentPath(documentUrl)
            .description(request.getDescription())
            .status(ProfileStatus.PENDING)
            .build();

    profile = teacherProfileRepository.save(profile);
    log.info("Teacher profile submitted successfully for user {} with document: {}", userId, documentUrl);

    // AUTO-TRIGGER OCR verification job
    try {
      String jobId = ocrJobProducer.createOcrJob(profile.getId(), userId);
      log.info("Auto-triggered OCR verification job {} for profile {}", jobId, profile.getId());
    } catch (Exception e) {
      log.error("Failed to auto-trigger OCR job for profile {}", profile.getId(), e);
      // Don't fail the submission if OCR job creation fails
    }

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
    profile.setFullName(request.getFullName()); // Update fullName for OCR verification
    profile.setSchoolName(request.getSchoolName());
    profile.setSchoolAddress(request.getSchoolAddress());
    profile.setSchoolWebsite(request.getSchoolWebsite());
    profile.setPosition(request.getPosition());
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

    // AUTO-TRIGGER OCR verification job if profile was rejected and now updated
    if (profile.getStatus() == ProfileStatus.PENDING && !profile.getOcrVerified()) {
      try {
        String jobId = ocrJobProducer.createOcrJob(profile.getId(), userId);
        log.info("Auto-triggered OCR verification job {} for updated profile {}", jobId, profile.getId());
      } catch (Exception e) {
        log.error("Failed to auto-trigger OCR job for updated profile {}", profile.getId(), e);
      }
    }

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

    // Send email notification to teacher
    String teacherEmail = profile.getUser().getEmail();
    String teacherName = profile.getUser().getFullName();
    
    NotificationRequest streamNotif = NotificationRequest.builder()
        .id(UUID.randomUUID().toString())
        .recipientId(profile.getUser().getId().toString())
        .senderId(adminId.toString())
        .timestamp(LocalDateTime.now())
        .build();

    if (request.getStatus() == ProfileStatus.APPROVED) {
        emailService.sendTeacherApprovalEmail(teacherEmail, teacherName);
        streamNotif.setType("PROFILE_VERIFICATION");
        streamNotif.setTitle("Hồ sơ Giáo viên được phê duyệt");
        streamNotif.setContent("Chúc mừng " + teacherName + ", yêu cầu nâng cấp tài khoản Giáo viên của bạn đã được quản trị viên phê duyệt thành công!");
    } else if (request.getStatus() == ProfileStatus.REJECTED) {
        emailService.sendTeacherRejectionEmail(teacherEmail, teacherName, request.getAdminComment());
        streamNotif.setType("PROFILE_VERIFICATION");
        streamNotif.setTitle("Hồ sơ Giáo viên bị từ chối");
        streamNotif.setContent("Hồ sơ Giáo viên của bạn đã bị từ chối với lý do: " + request.getAdminComment());
    }

    try {
        if (streamNotif.getType() != null) {
            streamPublisher.publish(streamNotif);
            log.info("In-app notification published to stream for user {}", profile.getUser().getId());
        }
    } catch (Exception e) {
        log.error("Failed to publish stream notification", e);
    }

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

  @Override
  public String getDownloadUrl(UUID profileId) {
    TeacherProfile profile =
        teacherProfileRepository
            .findById(profileId)
            .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));

    if (profile.getVerificationDocumentKey() == null) {
      throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
    }

    return uploadService.getPresignedUrl(
        profile.getVerificationDocumentKey(), minioProperties.getVerificationBucket());
  }

  private TeacherProfileResponse mapToResponse(TeacherProfile profile) {
    TeacherProfileResponse response =
        TeacherProfileResponse.builder()
            .id(profile.getId())
            .userId(profile.getUser().getId())
            .userName(profile.getUser().getUserName())
            .fullName(profile.getFullName()) // Use fullName from profile (for OCR verification)
            .schoolName(profile.getSchoolName())
            .schoolAddress(profile.getSchoolAddress())
            .schoolWebsite(profile.getSchoolWebsite())
            .position(profile.getPosition())
            .verificationDocumentKey(profile.getVerificationDocumentKey())
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
