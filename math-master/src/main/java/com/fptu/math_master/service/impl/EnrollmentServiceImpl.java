package com.fptu.math_master.service.impl;

import com.fptu.math_master.dto.response.EnrollmentResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.repository.WalletRepository;
import com.fptu.math_master.service.EnrollmentService;
import com.fptu.math_master.service.WalletService;
import com.fptu.math_master.util.SecurityUtils;
import com.fptu.math_master.entity.Wallet;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

  EnrollmentRepository enrollmentRepository;
  CourseRepository courseRepository;
  UserRepository userRepository;
  WalletService walletService;
  WalletRepository walletRepository;
  TransactionRepository transactionRepository;

  @Override
  public EnrollmentResponse enroll(UUID courseId) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    Course course =
        courseRepository
            .findByIdAndDeletedAtIsNull(courseId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    if (!course.isPublished()) {
      throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
    }

    // --- Start Payment Logic ---
    BigDecimal activePrice = course.getOriginalPrice();
    // If discount is active and not expired
    if (course.getDiscountedPrice() != null) {
      if (course.getDiscountExpiryDate() == null || course.getDiscountExpiryDate().isAfter(Instant.now())) {
        activePrice = course.getDiscountedPrice();
      }
    }

    if (activePrice != null && activePrice.compareTo(BigDecimal.ZERO) > 0) {
      Wallet wallet = walletRepository.findByUserId(studentId)
          .orElseGet(() -> {
            walletService.createWallet(studentId);
            return walletRepository.findByUserId(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
          });

      // This will throw INSUFFICIENT_BALANCE if not enough
      walletService.deductBalance(wallet.getId(), activePrice);

      // Record transaction
      Transaction transaction = Transaction.builder()
          .wallet(wallet)
          .amount(activePrice)
          .type(TransactionType.PAYMENT)
          .status(TransactionStatus.SUCCESS)
          .description("Purchase Course: " + course.getTitle())
          .transactionDate(Instant.now())
          .orderCode(System.currentTimeMillis())
          .build();
      transactionRepository.save(transaction);
      log.info("Payment successful: Student {} paid {} for course {}", studentId, activePrice, courseId);
    }
    // --- End Payment Logic ---

    var existing =
        enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId);

    if (existing.isPresent()) {
      Enrollment e = existing.get();
      if (e.getStatus() == EnrollmentStatus.ACTIVE) {
        throw new AppException(ErrorCode.ALREADY_ENROLLED);
      }
      e.setStatus(EnrollmentStatus.ACTIVE);
      e.setEnrolledAt(Instant.now());
      e = enrollmentRepository.save(e);
      log.info("Student {} re-enrolled in course {}", studentId, courseId);
      return mapToResponse(e, course.getTitle(), studentId);
    }

    Enrollment enrollment =
        Enrollment.builder()
            .courseId(courseId)
            .studentId(studentId)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .build();

    enrollment = enrollmentRepository.save(enrollment);
    log.info("Student {} enrolled in course {}", studentId, courseId);
    return mapToResponse(enrollment, course.getTitle(), studentId);
  }

  @Override
  public EnrollmentResponse drop(UUID enrollmentId) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    Enrollment enrollment =
        enrollmentRepository
            .findByIdAndDeletedAtIsNull(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

    if (!enrollment.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
    }

    enrollment.setStatus(EnrollmentStatus.DROPPED);
    enrollment = enrollmentRepository.save(enrollment);

    String courseTitle =
        courseRepository
            .findByIdAndDeletedAtIsNull(enrollment.getCourseId())
            .map(Course::getTitle)
            .orElse(null);

    log.info("Student {} dropped enrollment {}", studentId, enrollmentId);
    return mapToResponse(enrollment, courseTitle, studentId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> getMyEnrollments() {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return enrollmentRepository
        .findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(studentId)
        .stream()
        .map(
            e -> {
              String courseTitle =
                  courseRepository
                      .findByIdAndDeletedAtIsNull(e.getCourseId())
                      .map(Course::getTitle)
                      .orElse(null);
              return mapToResponse(e, courseTitle, studentId);
            })
        .collect(Collectors.toList());
  }

  private EnrollmentResponse mapToResponse(Enrollment e, String courseTitle, UUID studentId) {
    String studentName = userRepository.findById(studentId).map(User::getFullName).orElse(null);
    return EnrollmentResponse.builder()
        .id(e.getId())
        .courseId(e.getCourseId())
        .courseTitle(courseTitle)
        .studentId(e.getStudentId())
        .studentName(studentName)
        .status(e.getStatus())
        .enrolledAt(e.getEnrolledAt())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }
}
