package com.fptu.math_master.service.impl;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.request.NotificationRequest;
import com.fptu.math_master.dto.request.RefundRequestRequest;
import com.fptu.math_master.dto.response.RefundRequestResponse;
import com.fptu.math_master.entity.*;
import com.fptu.math_master.enums.*;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.RefundService;
import com.fptu.math_master.service.WalletService;
import com.fptu.math_master.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RefundServiceImpl implements RefundService {

  RefundRequestRepository refundRequestRepository;
  OrderRepository orderRepository;
  EnrollmentRepository enrollmentRepository;
  CourseRepository courseRepository;
  CourseLessonRepository courseLessonRepository;
  LessonProgressRepository lessonProgressRepository;
  WalletRepository walletRepository;
  TransactionRepository transactionRepository;
  UserRepository userRepository;
  WalletService walletService;
  StreamPublisher streamPublisher;
  com.fptu.math_master.service.EmailService emailService;

  private static final Duration AUTO_APPROVE_TIME_WINDOW = Duration.ofHours(24);
  private static final double AUTO_APPROVE_PROGRESS_THRESHOLD = 10.0;

  @Override
  @Transactional
  public RefundRequestResponse createRefundRequest(UUID orderId, RefundRequestRequest request) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    // 1. Validate order
    Order order = orderRepository
        .findByIdAndStudentId(orderId, studentId)
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    if (order.getStatus() != OrderStatus.COMPLETED) {
      throw new AppException(ErrorCode.ORDER_NOT_COMPLETED);
    }

    if (order.getEnrollmentId() == null) {
      throw new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }

    // 2. Check if refund already requested
    Optional<RefundRequest> existingRefund = refundRequestRepository
        .findByOrderIdAndDeletedAtIsNull(orderId);

    if (existingRefund.isPresent()) {
      RefundRequest existing = existingRefund.get();
      if (existing.getStatus() == RefundStatus.PENDING) {
        throw new AppException(ErrorCode.REFUND_ALREADY_REQUESTED);
      }
      if (existing.getStatus() == RefundStatus.APPROVED || existing.getStatus() == RefundStatus.COMPLETED) {
        throw new AppException(ErrorCode.REFUND_ALREADY_PROCESSED);
      }
    }

    // 3. Get enrollment
    Enrollment enrollment = enrollmentRepository
        .findByIdAndDeletedAtIsNull(order.getEnrollmentId())
        .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

    if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
      throw new AppException(ErrorCode.ENROLLMENT_NOT_ACTIVE);
    }

    // 4. Get course
    Course course = courseRepository
        .findByIdAndDeletedAtIsNull(order.getCourseId())
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    // 5. Check eligibility and determine auto-approval
    boolean isAutoApproved = checkAutoApprovalEligibility(enrollment, course);

    // 6. Create refund request
    RefundRequest refundRequest = RefundRequest.builder()
        .orderId(orderId)
        .enrollmentId(enrollment.getId())
        .studentId(studentId)
        .status(isAutoApproved ? RefundStatus.APPROVED : RefundStatus.PENDING)
        .reason(request.getReason())
        .refundAmount(order.getFinalPrice())
        .instructorDeduction(order.getInstructorEarnings())
        .platformDeduction(order.getPlatformCommission())
        .requestedAt(Instant.now())
        .isAutoApproved(isAutoApproved)
        .build();

    if (isAutoApproved) {
      refundRequest.setProcessedAt(Instant.now());
      refundRequest.setAdminNotes("Auto-approved: Eligible for automatic refund");
    }

    refundRequest = refundRequestRepository.save(refundRequest);

    // 7. If auto-approved, process refund immediately
    if (isAutoApproved) {
      processRefund(refundRequest, order, enrollment, course);
    }

    // 8. Send notifications
    sendRefundRequestNotifications(refundRequest, order, course, isAutoApproved);

    log.info("Refund request {} created for order {} (auto-approved: {})",
        refundRequest.getId(), orderId, isAutoApproved);

    return mapToResponse(refundRequest, order, course);
  }

  @Override
  @Transactional(readOnly = true)
  public RefundRequestResponse getRefundRequest(UUID refundRequestId) {
    UUID userId = SecurityUtils.getCurrentUserId();

    RefundRequest refundRequest = refundRequestRepository
        .findByIdAndDeletedAtIsNull(refundRequestId)
        .orElseThrow(() -> new AppException(ErrorCode.REFUND_REQUEST_NOT_FOUND));

    // Check access: student or admin
    if (!refundRequest.getStudentId().equals(userId) && !SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.REFUND_REQUEST_ACCESS_DENIED);
    }

    Order order = orderRepository.findById(refundRequest.getOrderId()).orElse(null);
    Course course = order != null
        ? courseRepository.findByIdAndDeletedAtIsNull(order.getCourseId()).orElse(null)
        : null;

    return mapToResponse(refundRequest, order, course);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RefundRequestResponse> getMyRefundRequests(Pageable pageable) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    Page<RefundRequest> refundRequests = refundRequestRepository
        .findByStudentIdAndDeletedAtIsNullOrderByRequestedAtDesc(studentId, pageable);

    return mapPageToResponse(refundRequests);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<RefundRequestResponse> getPendingRefundRequests(Pageable pageable) {
    // Admin only
    if (!SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    Page<RefundRequest> refundRequests = refundRequestRepository
        .findByStatusAndDeletedAtIsNullOrderByRequestedAtAsc(RefundStatus.PENDING, pageable);

    return mapPageToResponse(refundRequests);
  }

  @Override
  @Transactional
  public RefundRequestResponse approveRefundRequest(UUID refundRequestId, String adminNotes) {
    // Admin only
    if (!SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    UUID adminId = SecurityUtils.getCurrentUserId();

    RefundRequest refundRequest = refundRequestRepository
        .findByIdAndDeletedAtIsNull(refundRequestId)
        .orElseThrow(() -> new AppException(ErrorCode.REFUND_REQUEST_NOT_FOUND));

    if (refundRequest.getStatus() != RefundStatus.PENDING) {
      throw new AppException(ErrorCode.REFUND_REQUEST_NOT_PENDING);
    }

    // Get related entities
    Order order = orderRepository
        .findById(refundRequest.getOrderId())
        .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

    Enrollment enrollment = enrollmentRepository
        .findById(refundRequest.getEnrollmentId())
        .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

    Course course = courseRepository
        .findByIdAndDeletedAtIsNull(order.getCourseId())
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    // Update refund request
    refundRequest.setStatus(RefundStatus.APPROVED);
    refundRequest.setProcessedAt(Instant.now());
    refundRequest.setProcessedBy(adminId);
    refundRequest.setAdminNotes(adminNotes);
    refundRequest = refundRequestRepository.save(refundRequest);

    // Process refund
    processRefund(refundRequest, order, enrollment, course);

    // Send notifications
    sendRefundApprovalNotifications(refundRequest, order, course);

    log.info("Refund request {} approved by admin {}", refundRequestId, adminId);

    return mapToResponse(refundRequest, order, course);
  }

  @Override
  @Transactional
  public RefundRequestResponse rejectRefundRequest(UUID refundRequestId, String adminNotes) {
    // Admin only
    if (!SecurityUtils.hasRole("ADMIN")) {
      throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    UUID adminId = SecurityUtils.getCurrentUserId();

    RefundRequest refundRequest = refundRequestRepository
        .findByIdAndDeletedAtIsNull(refundRequestId)
        .orElseThrow(() -> new AppException(ErrorCode.REFUND_REQUEST_NOT_FOUND));

    if (refundRequest.getStatus() != RefundStatus.PENDING) {
      throw new AppException(ErrorCode.REFUND_REQUEST_NOT_PENDING);
    }

    if (adminNotes == null || adminNotes.trim().isEmpty()) {
      throw new AppException(ErrorCode.REFUND_REJECTION_REASON_REQUIRED);
    }

    // Update refund request
    refundRequest.setStatus(RefundStatus.REJECTED);
    refundRequest.setProcessedAt(Instant.now());
    refundRequest.setProcessedBy(adminId);
    refundRequest.setAdminNotes(adminNotes);
    refundRequest = refundRequestRepository.save(refundRequest);

    // Get related entities for response
    Order order = orderRepository.findById(refundRequest.getOrderId()).orElse(null);
    Course course = order != null
        ? courseRepository.findByIdAndDeletedAtIsNull(order.getCourseId()).orElse(null)
        : null;

    // Send notification
    sendRefundRejectionNotification(refundRequest, order, course);

    log.info("Refund request {} rejected by admin {}", refundRequestId, adminId);

    return mapToResponse(refundRequest, order, course);
  }

  @Override
  @Transactional
  public RefundRequestResponse cancelRefundRequest(UUID refundRequestId) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    RefundRequest refundRequest = refundRequestRepository
        .findByIdAndStudentIdAndDeletedAtIsNull(refundRequestId, studentId)
        .orElseThrow(() -> new AppException(ErrorCode.REFUND_REQUEST_NOT_FOUND));

    if (refundRequest.getStatus() != RefundStatus.PENDING) {
      throw new AppException(ErrorCode.REFUND_REQUEST_CANNOT_BE_CANCELLED);
    }

    refundRequest.setStatus(RefundStatus.REJECTED);
    refundRequest.setProcessedAt(Instant.now());
    refundRequest.setAdminNotes("Cancelled by student");
    refundRequest = refundRequestRepository.save(refundRequest);

    Order order = orderRepository.findById(refundRequest.getOrderId()).orElse(null);
    Course course = order != null
        ? courseRepository.findByIdAndDeletedAtIsNull(order.getCourseId()).orElse(null)
        : null;

    log.info("Refund request {} cancelled by student {}", refundRequestId, studentId);

    return mapToResponse(refundRequest, order, course);
  }

  // ─── Private Helper Methods ──────────────────────────────────────────────

  private boolean checkAutoApprovalEligibility(Enrollment enrollment, Course course) {
    // Check 1: Within 24 hours of enrollment
    if (enrollment.getEnrolledAt() != null) {
      Duration timeSinceEnrollment = Duration.between(enrollment.getEnrolledAt(), Instant.now());
      if (timeSinceEnrollment.compareTo(AUTO_APPROVE_TIME_WINDOW) <= 0) {
        log.info("Auto-approval eligible: Within 24 hours ({} hours)", timeSinceEnrollment.toHours());
        return true;
      }
    }

    // Check 2: Less than 10% progress
    int totalLessons = (int) courseLessonRepository.countByCourseIdAndNotDeleted(course.getId());
    if (totalLessons > 0) {
      int completedLessons = (int) lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId());
      double completionRate = (completedLessons * 100.0) / totalLessons;

      if (completionRate < AUTO_APPROVE_PROGRESS_THRESHOLD) {
        log.info("Auto-approval eligible: Less than 10% progress ({}%)", completionRate);
        return true;
      }
    }

    log.info("Auto-approval not eligible: Beyond time window and progress threshold");
    return false;
  }

  private void processRefund(RefundRequest refundRequest, Order order, Enrollment enrollment, Course course) {
    // 1. Update order status
    order.setStatus(OrderStatus.REFUNDED);
    orderRepository.save(order);

    // 2. Update enrollment status
    enrollment.setStatus(EnrollmentStatus.DROPPED);
    enrollmentRepository.save(enrollment);

    // 3. Process wallet transactions
    if (refundRequest.getRefundAmount().compareTo(BigDecimal.ZERO) > 0) {
      // Refund to student
      Wallet studentWallet = walletRepository
          .findByUserId(refundRequest.getStudentId())
          .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

      walletService.addBalance(studentWallet.getId(), refundRequest.getRefundAmount());

      Transaction studentRefundTx = Transaction.builder()
          .wallet(studentWallet)
          .order(order)
          .orderNumber(order.getOrderNumber())
          .amount(refundRequest.getRefundAmount())
          .type(TransactionType.REFUND)
          .status(TransactionStatus.SUCCESS)
          .description("Refund for Course: " + course.getTitle())
          .transactionDate(Instant.now())
          .orderCode(System.currentTimeMillis())
          .build();
      transactionRepository.save(studentRefundTx);

      // Deduct from instructor
      if (refundRequest.getInstructorDeduction() != null &&
          refundRequest.getInstructorDeduction().compareTo(BigDecimal.ZERO) > 0) {

        Wallet instructorWallet = walletRepository
            .findByUserId(course.getTeacherId())
            .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        walletService.deductBalance(instructorWallet.getId(), refundRequest.getInstructorDeduction());

        Transaction instructorDeductionTx = Transaction.builder()
            .wallet(instructorWallet)
            .order(order)
            .orderNumber(order.getOrderNumber())
            .amount(refundRequest.getInstructorDeduction())
            .type(TransactionType.REFUND)
            .status(TransactionStatus.SUCCESS)
            .description("Refund Deduction for Course: " + course.getTitle())
            .transactionDate(Instant.now())
            .orderCode(System.currentTimeMillis() + 1)
            .build();
        transactionRepository.save(instructorDeductionTx);
      }
    }

    // 4. Update refund request status
    refundRequest.setStatus(RefundStatus.COMPLETED);
    refundRequestRepository.save(refundRequest);

    log.info("Refund processed for order {}: {} refunded to student, {} deducted from instructor",
        order.getOrderNumber(), refundRequest.getRefundAmount(), refundRequest.getInstructorDeduction());
  }

  private void sendRefundRequestNotifications(RefundRequest refundRequest, Order order, Course course, boolean isAutoApproved) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("refundRequestId", refundRequest.getId().toString());
    metadata.put("orderId", order.getId().toString());
    metadata.put("orderNumber", order.getOrderNumber());
    metadata.put("courseId", course.getId().toString());
    metadata.put("amount", refundRequest.getRefundAmount().toString());
    metadata.put("event", isAutoApproved ? "REFUND_AUTO_APPROVED" : "REFUND_REQUESTED");

    // Student notification
    try {
      String title = isAutoApproved ? "Yêu cầu hoàn tiền đã được chấp nhận" : "Yêu cầu hoàn tiền đã được gửi";
      String content = isAutoApproved
          ? "Yêu cầu hoàn tiền cho khóa học '" + course.getTitle() + "' đã được tự động chấp nhận. Số tiền " +
            refundRequest.getRefundAmount() + " VND đã được hoàn vào ví của bạn."
          : "Yêu cầu hoàn tiền cho khóa học '" + course.getTitle() + "' đã được gửi. Chúng tôi sẽ xem xét và phản hồi trong thời gian sớm nhất.";

      NotificationRequest studentNotification = NotificationRequest.builder()
          .id(UUID.randomUUID().toString())
          .type("REFUND")
          .title(title)
          .content(content)
          .recipientId(refundRequest.getStudentId().toString())
          .senderId("SYSTEM")
          .timestamp(LocalDateTime.now())
          .metadata(metadata)
          .actionUrl("/student/orders/" + order.getId())
          .build();
      streamPublisher.publish(studentNotification);
    } catch (Exception e) {
      log.error("Failed to send refund request notification to student {}", refundRequest.getStudentId(), e);
    }

    // Instructor notification (if not auto-approved)
    if (!isAutoApproved) {
      try {
        NotificationRequest instructorNotification = NotificationRequest.builder()
            .id(UUID.randomUUID().toString())
            .type("REFUND")
            .title("Yêu cầu hoàn tiền mới")
            .content("Một học viên đã yêu cầu hoàn tiền cho khóa học '" + course.getTitle() + "'.")
            .recipientId(course.getTeacherId().toString())
            .senderId("SYSTEM")
            .timestamp(LocalDateTime.now())
            .metadata(metadata)
            .actionUrl("/teacher/courses/" + course.getId())
            .build();
        streamPublisher.publish(instructorNotification);
      } catch (Exception e) {
        log.error("Failed to send refund request notification to instructor {}", course.getTeacherId(), e);
      }
    }
  }

  private void sendRefundApprovalNotifications(RefundRequest refundRequest, Order order, Course course) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("refundRequestId", refundRequest.getId().toString());
    metadata.put("orderId", order.getId().toString());
    metadata.put("orderNumber", order.getOrderNumber());
    metadata.put("courseId", course.getId().toString());
    metadata.put("amount", refundRequest.getRefundAmount().toString());
    metadata.put("event", "REFUND_APPROVED");

    // Get user details
    User student = userRepository.findById(refundRequest.getStudentId()).orElse(null);
    User instructor = userRepository.findById(course.getTeacherId()).orElse(null);

    // Student notification
    try {
      NotificationRequest studentNotification = NotificationRequest.builder()
          .id(UUID.randomUUID().toString())
          .type("REFUND")
          .title("Yêu cầu hoàn tiền đã được chấp nhận")
          .content("Yêu cầu hoàn tiền cho khóa học '" + course.getTitle() + "' đã được chấp nhận. Số tiền " +
              refundRequest.getRefundAmount() + " VND đã được hoàn vào ví của bạn.")
          .recipientId(refundRequest.getStudentId().toString())
          .senderId("SYSTEM")
          .timestamp(LocalDateTime.now())
          .metadata(metadata)
          .actionUrl("/student/wallet")
          .build();
      streamPublisher.publish(studentNotification);
    } catch (Exception e) {
      log.error("Failed to send refund approval notification to student {}", refundRequest.getStudentId(), e);
    }

    // Student email
    if (student != null && student.getEmail() != null) {
      try {
        String formattedAmount = String.format("%,.0f VND", refundRequest.getRefundAmount());
        emailService.sendRefundConfirmationEmail(
            student.getEmail(),
            student.getFullName(),
            course.getTitle(),
            formattedAmount,
            refundRequest.getReason() != null ? refundRequest.getReason() : "Yêu cầu hoàn tiền"
        );
        log.info("Refund confirmation email sent to student {}", student.getEmail());
      } catch (Exception e) {
        log.error("Failed to send refund confirmation email to student {}", student.getEmail(), e);
      }
    }

    // Instructor notification
    try {
      NotificationRequest instructorNotification = NotificationRequest.builder()
          .id(UUID.randomUUID().toString())
          .type("REFUND")
          .title("Yêu cầu hoàn tiền đã được xử lý")
          .content("Yêu cầu hoàn tiền cho khóa học '" + course.getTitle() + "' đã được chấp nhận. Số tiền " +
              refundRequest.getInstructorDeduction() + " VND đã được trừ từ ví của bạn.")
          .recipientId(course.getTeacherId().toString())
          .senderId("SYSTEM")
          .timestamp(LocalDateTime.now())
          .metadata(metadata)
          .actionUrl("/teacher/wallet")
          .build();
      streamPublisher.publish(instructorNotification);
    } catch (Exception e) {
      log.error("Failed to send refund approval notification to instructor {}", course.getTeacherId(), e);
    }

    // Instructor email
    if (instructor != null && instructor.getEmail() != null && student != null) {
      try {
        String formattedDeduction = String.format("%,.0f VND", refundRequest.getInstructorDeduction());
        emailService.sendRefundNotificationEmail(
            instructor.getEmail(),
            instructor.getFullName(),
            student.getFullName(),
            course.getTitle(),
            formattedDeduction
        );
        log.info("Refund notification email sent to instructor {}", instructor.getEmail());
      } catch (Exception e) {
        log.error("Failed to send refund notification email to instructor {}", instructor.getEmail(), e);
      }
    }
  }

  private void sendRefundRejectionNotification(RefundRequest refundRequest, Order order, Course course) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("refundRequestId", refundRequest.getId().toString());
    metadata.put("orderId", order.getId().toString());
    metadata.put("orderNumber", order.getOrderNumber());
    metadata.put("courseId", course.getId().toString());
    metadata.put("event", "REFUND_REJECTED");

    try {
      NotificationRequest studentNotification = NotificationRequest.builder()
          .id(UUID.randomUUID().toString())
          .type("REFUND")
          .title("Yêu cầu hoàn tiền đã bị từ chối")
          .content("Yêu cầu hoàn tiền cho khóa học '" + course.getTitle() + "' đã bị từ chối. Lý do: " +
              refundRequest.getAdminNotes())
          .recipientId(refundRequest.getStudentId().toString())
          .senderId("SYSTEM")
          .timestamp(LocalDateTime.now())
          .metadata(metadata)
          .actionUrl("/student/orders/" + order.getId())
          .build();
      streamPublisher.publish(studentNotification);
    } catch (Exception e) {
      log.error("Failed to send refund rejection notification to student {}", refundRequest.getStudentId(), e);
    }
  }

  private RefundRequestResponse mapToResponse(RefundRequest refundRequest, Order order, Course course) {
    String studentName = userRepository.findById(refundRequest.getStudentId())
        .map(User::getFullName)
        .orElse(null);

    String processorName = refundRequest.getProcessedBy() != null
        ? userRepository.findById(refundRequest.getProcessedBy()).map(User::getFullName).orElse(null)
        : null;

    return RefundRequestResponse.builder()
        .id(refundRequest.getId())
        .orderId(refundRequest.getOrderId())
        .orderNumber(order != null ? order.getOrderNumber() : null)
        .enrollmentId(refundRequest.getEnrollmentId())
        .studentId(refundRequest.getStudentId())
        .studentName(studentName)
        .courseId(order != null ? order.getCourseId() : null)
        .courseTitle(course != null ? course.getTitle() : null)
        .status(refundRequest.getStatus())
        .reason(refundRequest.getReason())
        .refundAmount(refundRequest.getRefundAmount())
        .instructorDeduction(refundRequest.getInstructorDeduction())
        .platformDeduction(refundRequest.getPlatformDeduction())
        .requestedAt(refundRequest.getRequestedAt())
        .processedAt(refundRequest.getProcessedAt())
        .processedBy(refundRequest.getProcessedBy())
        .processorName(processorName)
        .adminNotes(refundRequest.getAdminNotes())
        .isAutoApproved(refundRequest.isAutoApproved())
        .createdAt(refundRequest.getCreatedAt())
        .updatedAt(refundRequest.getUpdatedAt())
        .build();
  }

  private Page<RefundRequestResponse> mapPageToResponse(Page<RefundRequest> refundRequests) {
    Set<UUID> orderIds = new HashSet<>();
    Set<UUID> courseIds = new HashSet<>();

    refundRequests.forEach(r -> orderIds.add(r.getOrderId()));

    Map<UUID, Order> orderMap = new HashMap<>();
    if (!orderIds.isEmpty()) {
      orderRepository.findAllById(orderIds).forEach(o -> {
        orderMap.put(o.getId(), o);
        courseIds.add(o.getCourseId());
      });
    }

    Map<UUID, Course> courseMap = new HashMap<>();
    if (!courseIds.isEmpty()) {
      courseRepository.findAllById(courseIds).forEach(c -> courseMap.put(c.getId(), c));
    }

    return refundRequests.map(refundRequest -> {
      Order order = orderMap.get(refundRequest.getOrderId());
      Course course = order != null ? courseMap.get(order.getCourseId()) : null;
      return mapToResponse(refundRequest, order, course);
    });
  }
}
