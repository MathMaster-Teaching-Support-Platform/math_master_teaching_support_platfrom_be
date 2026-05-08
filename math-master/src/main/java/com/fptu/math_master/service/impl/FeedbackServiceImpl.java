package com.fptu.math_master.service.impl;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.configuration.properties.MinioProperties;
import com.fptu.math_master.dto.request.CreateFeedbackRequest;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.request.RespondFeedbackRequest;
import com.fptu.math_master.dto.response.FeedbackAttachmentResponse;
import com.fptu.math_master.dto.response.FeedbackResponse;
import com.fptu.math_master.entity.Feedback;
import com.fptu.math_master.entity.FeedbackAttachment;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.FeedbackStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.FeedbackAttachmentRepository;
import com.fptu.math_master.repository.FeedbackRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.FeedbackService;
import com.fptu.math_master.service.UploadService;
import com.fptu.math_master.util.SecurityUtils;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
@Transactional
public class FeedbackServiceImpl implements FeedbackService {

  static final String ROLE_STUDENT = "STUDENT";
  static final String ROLE_TEACHER = "TEACHER";
  static final String ROLE_ADMIN = "ADMIN";
  static final String FEEDBACK_ATTACHMENT_DIR = "feedbacks";
  static final int MAX_ATTACHMENT_FILES = 10;
  static final long MAX_ATTACHMENT_SIZE_BYTES = 2L * 1024L * 1024L;
  static final long MAX_TOTAL_ATTACHMENT_SIZE_BYTES = 10L * 1024L * 1024L;
  static final Set<String> ALLOWED_SENDER_ROLES = Set.of(ROLE_STUDENT, ROLE_TEACHER);
  static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS =
      Set.of("pdf", "png", "jpg", "jpeg", "gif", "webp", "doc", "docx", "txt", "ppt", "pptx");

  FeedbackRepository feedbackRepository;
  FeedbackAttachmentRepository feedbackAttachmentRepository;
  UserRepository userRepository;
  UploadService uploadService;
  StreamPublisher streamPublisher;
  MinioProperties minioProperties;

  @Override
  public FeedbackResponse createFeedback(CreateFeedbackRequest request, List<MultipartFile> files) {
    User sender = getCurrentUser();
    String senderRole = resolvePrimaryRole(sender);
    if (!ALLOWED_SENDER_ROLES.contains(senderRole)) {
      throw new AppException(ErrorCode.FEEDBACK_SUBMIT_ROLE_NOT_ALLOWED);
    }

    Feedback feedback =
        Feedback.builder()
            .sender(sender)
            .title(request.getTitle().trim())
            .description(request.getDescription().trim())
            .category(trimToNull(request.getCategory()))
            .relatedUrl(trimToNull(request.getRelatedUrl()))
            .status(FeedbackStatus.OPEN)
            .build();

    feedback = feedbackRepository.save(feedback);
    saveAttachments(feedback, files);
    notifyAdminsNewFeedback(feedback, senderRole);
    return mapToResponse(feedback);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<FeedbackResponse> getMyFeedbacks(Pageable pageable) {
    UUID userId = SecurityUtils.getCurrentUserId();
    return feedbackRepository.findAllBySender_IdOrderByCreatedAtDesc(userId, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<FeedbackResponse> getAllFeedbacks(Pageable pageable) {
    return feedbackRepository.findAll(pageable).map(this::mapToResponse);
  }

  @Override
  public FeedbackResponse respondToFeedback(UUID feedbackId, RespondFeedbackRequest request) {
    User admin = getCurrentUser();
    if (!hasRole(admin, ROLE_ADMIN)) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Feedback feedback =
        feedbackRepository
            .findById(feedbackId)
            .orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));

    feedback.setResponseMessage(request.getResponseMessage().trim());
    feedback.setRespondedBy(admin);
    feedback.setStatus(FeedbackStatus.RESPONDED);
    feedback = feedbackRepository.save(feedback);

    notifyFeedbackSenderResponse(feedback, admin);
    return mapToResponse(feedback);
  }

  @Override
  public FeedbackResponse markAsRead(UUID feedbackId) {
    User currentUser = getCurrentUser();
    Feedback feedback =
        feedbackRepository.findById(feedbackId).orElseThrow(() -> new AppException(ErrorCode.FEEDBACK_NOT_FOUND));

    boolean isSender =
        feedback.getSender() != null
            && feedback.getSender().getId() != null
            && feedback.getSender().getId().equals(currentUser.getId());
    boolean isAdmin = hasRole(currentUser, ROLE_ADMIN);
    if (!isSender && !isAdmin) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Instant now = Instant.now();
    if (isSender && feedback.getSenderReadAt() == null) {
      feedback.setSenderReadAt(now);
    }
    if (isAdmin && feedback.getAdminReadAt() == null) {
      feedback.setAdminReadAt(now);
    }
    feedback = feedbackRepository.save(feedback);
    return mapToResponse(feedback);
  }

  private User getCurrentUser() {
    UUID userId = SecurityUtils.getCurrentUserId();
    return userRepository.findByIdWithRoles(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
  }

  private String resolvePrimaryRole(User user) {
    if (hasRole(user, ROLE_ADMIN)) return ROLE_ADMIN;
    if (hasRole(user, ROLE_TEACHER)) return ROLE_TEACHER;
    if (hasRole(user, ROLE_STUDENT)) return ROLE_STUDENT;
    return "UNKNOWN";
  }

  private boolean hasRole(User user, String roleName) {
    return user.getRoles() != null && user.getRoles().stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void notifyAdminsNewFeedback(Feedback feedback, String senderRole) {
    try {
      List<UUID> adminIds = userRepository.findUserIdsByRoleName(ROLE_ADMIN);
      for (UUID adminId : adminIds) {
        NotificationRequest notification =
            NotificationRequest.builder()
                .id(UUID.randomUUID().toString())
                .type("FEEDBACK")
                .title("Có góp ý mới từ " + (ROLE_TEACHER.equals(senderRole) ? "Giáo viên" : "Học sinh"))
                .content(feedback.getTitle())
                .recipientId(adminId.toString())
                .senderId(feedback.getSender().getId().toString())
                .actionUrl("/admin/feedbacks?feedbackId=" + feedback.getId())
                .metadata(buildFeedbackMetadata(feedback))
                .timestamp(LocalDateTime.now())
                .build();
        streamPublisher.publish(notification);
      }
    } catch (Exception e) {
      log.error("Failed to notify admins for feedback {}", feedback.getId(), e);
    }
  }

  private void notifyFeedbackSenderResponse(Feedback feedback, User admin) {
    try {
      NotificationRequest notification =
          NotificationRequest.builder()
              .id(UUID.randomUUID().toString())
              .type("FEEDBACK")
              .title("Góp ý của bạn đã được phản hồi")
              .content(feedback.getTitle())
              .recipientId(feedback.getSender().getId().toString())
              .senderId(admin.getId().toString())
              .actionUrl("/notifications")
              .metadata(buildFeedbackMetadata(feedback))
              .timestamp(LocalDateTime.now())
              .build();
      streamPublisher.publish(notification);
    } catch (Exception e) {
      log.error("Failed to notify sender {} for feedback response {}", feedback.getSender().getId(), feedback.getId(), e);
    }
  }

  private Map<String, Object> buildFeedbackMetadata(Feedback feedback) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("feedbackId", feedback.getId().toString());
    metadata.put("category", feedback.getCategory());
    metadata.put("status", feedback.getStatus().name());
    return metadata;
  }

  private void saveAttachments(Feedback feedback, List<MultipartFile> files) {
    if (files == null || files.isEmpty()) return;
    if (files.size() > MAX_ATTACHMENT_FILES) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "You can upload at most " + MAX_ATTACHMENT_FILES + " files per feedback.");
    }
    long totalSize = files.stream().filter(file -> file != null && !file.isEmpty()).mapToLong(MultipartFile::getSize).sum();
    if (totalSize > MAX_TOTAL_ATTACHMENT_SIZE_BYTES) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST,
          "Total attachment size must not exceed " + (MAX_TOTAL_ATTACHMENT_SIZE_BYTES / (1024 * 1024)) + " MB.");
    }

    for (MultipartFile file : files) {
      if (file == null || file.isEmpty()) continue;
      validateAttachment(file);
      String uploadDirectory = String.join("/", FEEDBACK_ATTACHMENT_DIR, feedback.getId().toString());
      String filePath =
          uploadService.uploadFile(file, uploadDirectory, minioProperties.getVerificationBucket());
      FeedbackAttachment attachment =
          FeedbackAttachment.builder()
              .feedback(feedback)
              .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment")
              .contentType(file.getContentType())
              .fileSize(file.getSize())
              .filePath(filePath)
              .build();
      feedbackAttachmentRepository.save(attachment);
    }
  }

  private void validateAttachment(MultipartFile file) {
    if (file.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
      throw new AppException(ErrorCode.RESOURCE_FILE_TOO_LARGE);
    }
    String originalName = file.getOriginalFilename();
    if (originalName == null || !originalName.contains(".")) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }
    String ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(ext)) {
      throw new AppException(ErrorCode.INVALID_FILE_FORMAT);
    }
  }

  private FeedbackResponse mapToResponse(Feedback feedback) {
    User sender = feedback.getSender();
    User respondedBy = feedback.getRespondedBy();
    UUID currentUserId = SecurityUtils.getOptionalCurrentUserId();
    boolean isCurrentUserSender =
        currentUserId != null && sender != null && sender.getId() != null && sender.getId().equals(currentUserId);
    boolean isCurrentUserAdmin = SecurityUtils.hasRole(ROLE_ADMIN);
    boolean senderRead = feedback.getSenderReadAt() != null;
    boolean adminRead = feedback.getAdminReadAt() != null;
    boolean readByCurrentUser = isCurrentUserAdmin ? adminRead : (isCurrentUserSender && senderRead);
    List<FeedbackAttachmentResponse> attachments =
        feedbackAttachmentRepository.findAllByFeedback_IdOrderByCreatedAtAsc(feedback.getId()).stream()
            .map(
                attachment ->
                    FeedbackAttachmentResponse.builder()
                        .id(attachment.getId().toString())
                        .fileName(attachment.getFileName())
                        .contentType(attachment.getContentType())
                        .fileSize(attachment.getFileSize())
                        .fileUrl(
                            uploadService.getPresignedUrl(
                                attachment.getFilePath(), minioProperties.getVerificationBucket()))
                        .build())
            .toList();
    return FeedbackResponse.builder()
        .id(feedback.getId())
        .title(feedback.getTitle())
        .description(feedback.getDescription())
        .category(feedback.getCategory())
        .relatedUrl(feedback.getRelatedUrl())
        .status(feedback.getStatus())
        .senderId(sender != null ? sender.getId().toString() : null)
        .senderName(sender != null ? sender.getFullName() : null)
        .senderEmail(sender != null ? sender.getEmail() : null)
        .senderRole(sender != null ? resolvePrimaryRole(sender) : null)
        .responseMessage(feedback.getResponseMessage())
        .respondedById(respondedBy != null ? respondedBy.getId().toString() : null)
        .respondedByName(respondedBy != null ? respondedBy.getFullName() : null)
        .attachments(attachments)
        .senderRead(senderRead)
        .adminRead(adminRead)
        .readByCurrentUser(readByCurrentUser)
        .createdAt(feedback.getCreatedAt())
        .updatedAt(feedback.getUpdatedAt())
        .build();
  }
}
