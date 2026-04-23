package com.fptu.math_master.service.impl;

import com.fptu.math_master.component.StreamPublisher;
import com.fptu.math_master.dto.request.NotificationRequest;
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
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
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
  CourseLessonRepository courseLessonRepository;
  LessonProgressRepository lessonProgressRepository;
  StreamPublisher streamPublisher;

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

    publishEnrollmentNotifications(course, studentId);

    log.info("Student {} enrollment finalized for course {}", studentId, courseId);
    return mapToResponse(enrollment, course.getTitle(), studentId);
  }

  private void publishEnrollmentNotifications(Course course, UUID studentId) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("courseId", course.getId().toString());
    metadata.put("event", "COURSE_ENROLLED");

    try {
      NotificationRequest studentNotification =
          NotificationRequest.builder()
              .id(UUID.randomUUID().toString())
              .type("COURSE")
              .title("Dang ky khoa hoc thanh cong")
              .content("Ban da dang ky thanh cong khoa hoc '" + course.getTitle() + "'.")
              .recipientId(studentId.toString())
              .senderId("SYSTEM")
              .timestamp(LocalDateTime.now())
              .metadata(metadata)
              .actionUrl("/student/courses")
              .build();
      streamPublisher.publish(studentNotification);
    } catch (Exception e) {
      log.error("Failed to publish enrollment notification for student {}", studentId, e);
    }

    try {
      NotificationRequest teacherNotification =
          NotificationRequest.builder()
              .id(UUID.randomUUID().toString())
              .type("COURSE")
              .title("Hoc vien moi dang ky")
              .content("Khoa hoc '" + course.getTitle() + "' vua co mot hoc vien moi dang ky.")
              .recipientId(course.getTeacherId().toString())
              .senderId("SYSTEM")
              .timestamp(LocalDateTime.now())
              .metadata(metadata)
              .actionUrl("/teacher/courses/" + course.getId())
              .build();
      streamPublisher.publish(teacherNotification);
    } catch (Exception e) {
      log.error(
          "Failed to publish enrollment notification for teacher {}", course.getTeacherId(), e);
    }
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

    Course course = courseRepository
        .findByIdAndDeletedAtIsNull(enrollment.getCourseId())
        .orElse(null);

    log.info("Student {} dropped enrollment {}", studentId, enrollmentId);
    return mapToResponse(enrollment, course != null ? course.getTitle() : null, studentId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentResponse> getMyEnrollments() {
    UUID studentId = SecurityUtils.getCurrentUserId();
    List<Enrollment> enrollments = enrollmentRepository
        .findByStudentIdAndDeletedAtIsNullOrderByEnrolledAtDesc(studentId);

    if (enrollments.isEmpty()) {
      return Collections.emptyList();
    }

    Set<UUID> courseIds = enrollments.stream().map(Enrollment::getCourseId).collect(Collectors.toSet());
    Map<UUID, Course> courseMap = courseRepository.findAllById(courseIds).stream()
        .collect(Collectors.toMap(Course::getId, c -> c));

    return enrollments.stream()
        .map(
            e -> {
              Course course = courseMap.get(e.getCourseId());
              String courseTitle = course != null ? course.getTitle() : null;
              String thumbnailUrl = course != null ? course.getThumbnailUrl() : null;
              
              EnrollmentResponse res = mapToResponse(e, courseTitle, studentId);
              res.setCourseThumbnailUrl(thumbnailUrl);

              // Embed progress summary to avoid N+1 from frontend
              int totalLessons = (int) courseLessonRepository.countByCourseIdAndNotDeleted(e.getCourseId());
              int completedLessons = (int) lessonProgressRepository.countCompletedByEnrollmentId(e.getId());
              double completionRate = totalLessons == 0 ? 0.0 : Math.min(100.0, (completedLessons * 100.0) / totalLessons);
              
              res.setTotalLessons(totalLessons);
              res.setCompletedLessons(completedLessons);
              res.setCompletionRate(completionRate);
              
              return res;
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
