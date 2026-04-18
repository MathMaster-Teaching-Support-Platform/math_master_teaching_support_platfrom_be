package com.fptu.math_master.service.impl;

import com.fptu.math_master.constant.PredefinedRole;
import com.fptu.math_master.dto.response.TeachingResourceResponse;
import com.fptu.math_master.entity.TeachingResource;
import com.fptu.math_master.enums.TeachingResourceType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.TeachingResourceRepository;
import com.fptu.math_master.service.ResourceService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class ResourceServiceImpl implements ResourceService {

  static final long MAX_FILE_SIZE_BYTES = 25L * 1024L * 1024L;

  UploadService uploadService;
  TeachingResourceRepository teachingResourceRepository;

  @Override
  public TeachingResourceResponse uploadResource(String name, TeachingResourceType type, MultipartFile file) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    validateTeacherOrAdmin();
    validateUploadInput(name, type, file);

    String normalizedName = name.trim();
    String fileUrl = uploadService.uploadFile(file, "teaching-resources/" + type.name().toLowerCase(Locale.ROOT));

    TeachingResource resource =
        TeachingResource.builder().name(normalizedName).type(type).fileUrl(fileUrl).build();
    resource.setCreatedBy(currentUserId);

    TeachingResource saved = teachingResourceRepository.save(resource);
    return mapToResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public TeachingResourceResponse getResource(UUID resourceId) {
    TeachingResource resource = getActiveResource(resourceId);
    return mapToResponse(resource);
  }

  @Override
  public void deleteResource(UUID resourceId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId();
    TeachingResource resource = getActiveResource(resourceId);

    boolean isAdmin = SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE);
    boolean isOwner = currentUserId.equals(resource.getCreatedBy());
    if (!isAdmin && !isOwner) {
      throw new AppException(ErrorCode.TEACHING_RESOURCE_ACCESS_DENIED);
    }

    uploadService.deleteFile(resource.getFileUrl());
    resource.setDeletedAt(Instant.now());
    resource.setDeletedBy(currentUserId);
    teachingResourceRepository.save(resource);
  }

  private void validateUploadInput(String name, TeachingResourceType type, MultipartFile file) {
    if (name == null || name.trim().isEmpty()) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }
    if (type == null) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_KEY);
    }
    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new AppException(ErrorCode.RESOURCE_FILE_TOO_LARGE);
    }

    String fileName = file.getOriginalFilename();
    if (fileName == null || !fileName.contains(".")) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }

    String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    if (!isAllowedExtension(type, extension)) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }
  }

  private boolean isAllowedExtension(TeachingResourceType type, String extension) {
    return switch (type) {
      case SLIDE -> Set.of("ppt", "pptx").contains(extension);
      case MINDMAP -> Set.of("xmind", "mm", "json", "png", "jpg", "jpeg", "svg").contains(extension);
      case DOC -> Set.of("doc", "docx", "txt", "rtf", "odt").contains(extension);
      case PDF -> Set.of("pdf").contains(extension);
      case IMAGE -> Set.of("png", "jpg", "jpeg", "gif", "webp", "svg").contains(extension);
    };
  }

  private TeachingResource getActiveResource(UUID resourceId) {
    return teachingResourceRepository
        .findByIdAndNotDeleted(resourceId)
        .orElseThrow(() -> new AppException(ErrorCode.TEACHING_RESOURCE_NOT_FOUND));
  }

  private TeachingResourceResponse mapToResponse(TeachingResource resource) {
    return TeachingResourceResponse.builder()
        .id(resource.getId())
        .name(resource.getName())
        .type(resource.getType())
        .fileUrl(resource.getFileUrl())
        .createdBy(resource.getCreatedBy())
        .createdAt(resource.getCreatedAt())
        .build();
  }

  private void validateTeacherOrAdmin() {
    if (!SecurityUtils.hasRole(PredefinedRole.TEACHER_ROLE)
        && !SecurityUtils.hasRole(PredefinedRole.ADMIN_ROLE)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }
  }
}
