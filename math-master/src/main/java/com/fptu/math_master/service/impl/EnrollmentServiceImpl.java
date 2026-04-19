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
import java.math.RoundingMode;
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

    Course course = courseRepository
        .findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

    if (!course.isPublished()) {
      throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
    }

    // 1. Check early for idempotency
    var existingEnrollment = enrollmentRepository.findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, courseId);

    if (existingEnrollment.isPresent()) {
      Enrollment e = existingEnrollment.get();
      if (e.getStatus() == EnrollmentStatus.ACTIVE) {
        log.info("Student {} already active in course {}. Returning existing record.", studentId, courseId);
        return mapToResponse(e, course.getTitle(), studentId);
      }
    }

    // 2. Initialize or Update to PENDING
    // This phase ensures we have a record to lock against concurrent requests
    Enrollment enrollment;
    if (existingEnrollment.isPresent()) {
      enrollment = existingEnrollment.get();
      enrollment.setStatus(EnrollmentStatus.PENDING);
    } else {
      enrollment = Enrollment.builder()
          .courseId(courseId)
          .studentId(studentId)
          .status(EnrollmentStatus.PENDING)
          .enrolledAt(Instant.now())
          .build();
    }
    enrollment = enrollmentRepository.saveAndFlush(enrollment);

    // 3. Process Payment and Split
    BigDecimal activePrice = course.getOriginalPrice();
    if (course.getDiscountedPrice() != null) {
      if (course.getDiscountExpiryDate() == null || course.getDiscountExpiryDate().isAfter(Instant.now())) {
        activePrice = course.getDiscountedPrice();
      }
    }

    if (activePrice != null && activePrice.compareTo(BigDecimal.ZERO) > 0) {
      Wallet studentWallet = walletRepository.findByUserId(studentId)
          .orElseGet(() -> {
            walletService.createWallet(studentId);
            return walletRepository.findByUserId(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
          });

      // Deduct full amount from student
      walletService.deductBalance(studentWallet.getId(), activePrice);

      // Calculate Split
      BigDecimal instructorEarnings = activePrice.multiply(new BigDecimal("0.9"))
          .setScale(2, RoundingMode.HALF_UP);
      BigDecimal platformCommission = activePrice.subtract(instructorEarnings);

      // Record Student Transaction (COURSE_PURCHASE)
      Transaction studentTx = Transaction.builder()
          .wallet(studentWallet)
          .amount(activePrice)
          .type(TransactionType.COURSE_PURCHASE)
          .status(TransactionStatus.SUCCESS)
          .description("Purchased Course: " + course.getTitle())
          .transactionDate(Instant.now())
          .orderCode(System.currentTimeMillis())
          .instructorEarnings(instructorEarnings)
          .platformCommission(platformCommission)
          .build();
      transactionRepository.save(studentTx);

      // Deposit to Instructor
      Wallet instructorWallet = walletRepository.findByUserId(course.getTeacherId())
          .orElseGet(() -> {
            walletService.createWallet(course.getTeacherId());
            return walletRepository.findByUserId(course.getTeacherId())
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
          });

      walletService.addBalance(instructorWallet.getId(), instructorEarnings);

      // Record Instructor Transaction (INSTRUCTOR_REVENUE)
      Transaction instructorTx = Transaction.builder()
          .wallet(instructorWallet)
          .amount(instructorEarnings)
          .type(TransactionType.INSTRUCTOR_REVENUE)
          .status(TransactionStatus.SUCCESS)
          .description("Revenue from Course: " + course.getTitle())
          .transactionDate(Instant.now())
          .orderCode(System.currentTimeMillis() + 1)
          .build();
      transactionRepository.save(instructorTx);

      log.info("Enrollment payment success: Student {} paid {}, Teacher {} earned {}",
          studentId, activePrice, instructorEarnings);
    }

    // 4. Finalize Enrollment
    enrollment.setStatus(EnrollmentStatus.ACTIVE);
    enrollment.setEnrolledAt(Instant.now());
    enrollment = enrollmentRepository.save(enrollment);

    log.info("Student {} enrollment finalized for course {}", studentId, courseId);
    return mapToResponse(enrollment, course.getTitle(), studentId);
  }

  @Override
  public EnrollmentResponse drop(UUID enrollmentId) {
    UUID studentId = SecurityUtils.getCurrentUserId();

    Enrollment enrollment = enrollmentRepository
        .findByIdAndDeletedAtIsNull(enrollmentId)
        .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

    if (!enrollment.getStudentId().equals(studentId)) {
      throw new AppException(ErrorCode.ENROLLMENT_ACCESS_DENIED);
    }

    enrollment.setStatus(EnrollmentStatus.DROPPED);
    enrollment = enrollmentRepository.save(enrollment);

    String courseTitle = courseRepository
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
              String courseTitle = courseRepository
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
