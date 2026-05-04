package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.TeacherEarningsStatsResponse;
import com.fptu.math_master.dto.response.TeacherMonthlyRevenueResponse;
import com.fptu.math_master.dto.response.TeacherTopCourseResponse;
import com.fptu.math_master.dto.response.TransactionResponse;
import com.fptu.math_master.entity.Course;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.TransactionRepository;
import com.fptu.math_master.repository.WalletRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TeacherEarningsService {

    TransactionRepository transactionRepository;
    WalletRepository walletRepository;
    CourseRepository courseRepository;
    EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public TeacherEarningsStatsResponse getEarningsStats(UUID teacherId) {
        log.info("Getting earnings stats for teacher: {}", teacherId);

        // Get teacher's wallet
        var wallet = walletRepository.findByUserId(teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        // Total earnings (all time)
        BigDecimal totalEarnings = transactionRepository.sumByWalletIdAndTypeAndStatus(
                wallet.getId(), TransactionType.INSTRUCTOR_REVENUE, TransactionStatus.SUCCESS);
        if (totalEarnings == null) totalEarnings = BigDecimal.ZERO;

        // This month earnings
        Instant startOfMonth = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfMonth = YearMonth.now().atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        BigDecimal thisMonthEarnings = transactionRepository.sumByWalletIdAndTypeAndStatusAndDateRange(
                wallet.getId(), TransactionType.INSTRUCTOR_REVENUE, TransactionStatus.SUCCESS, startOfMonth, endOfMonth);
        if (thisMonthEarnings == null) thisMonthEarnings = BigDecimal.ZERO;

        // Previous month earnings for growth calculation
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        Instant startOfPrevMonth = previousMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfPrevMonth = previousMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        
        BigDecimal prevMonthEarnings = transactionRepository.sumByWalletIdAndTypeAndStatusAndDateRange(
                wallet.getId(), TransactionType.INSTRUCTOR_REVENUE, TransactionStatus.SUCCESS, startOfPrevMonth, endOfPrevMonth);
        if (prevMonthEarnings == null) prevMonthEarnings = BigDecimal.ZERO;

        // Calculate growth percent
        double growthPercent = 0.0;
        if (prevMonthEarnings.compareTo(BigDecimal.ZERO) > 0) {
            growthPercent = thisMonthEarnings.subtract(prevMonthEarnings)
                    .divide(prevMonthEarnings, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        } else if (thisMonthEarnings.compareTo(BigDecimal.ZERO) > 0) {
            growthPercent = 100.0;
        }

        // Pending earnings
        BigDecimal pendingEarnings = transactionRepository.sumByWalletIdAndTypeAndStatus(
                wallet.getId(), TransactionType.INSTRUCTOR_REVENUE, TransactionStatus.PENDING);
        if (pendingEarnings == null) pendingEarnings = BigDecimal.ZERO;

        // Total students (unique across all courses)
        List<Course> teacherCourses = courseRepository.findByTeacherIdAndDeletedAtIsNull(teacherId);
        Set<UUID> uniqueStudents = new HashSet<>();
        for (Course course : teacherCourses) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourseIdAndDeletedAtIsNull(course.getId());
            enrollments.forEach(e -> uniqueStudents.add(e.getStudentId()));
        }

        // Active courses (published)
        long activeCourses = teacherCourses.stream()
                .filter(Course::isPublished)
                .count();

        // Avg revenue per course
        BigDecimal avgRevenuePerCourse = BigDecimal.ZERO;
        if (activeCourses > 0) {
            avgRevenuePerCourse = totalEarnings.divide(BigDecimal.valueOf(activeCourses), 2, RoundingMode.HALF_UP);
        }

        return TeacherEarningsStatsResponse.builder()
                .totalEarnings(totalEarnings)
                .thisMonthEarnings(thisMonthEarnings)
                .pendingEarnings(pendingEarnings)
                .totalStudents(uniqueStudents.size())
                .activeCourses(activeCourses)
                .avgRevenuePerCourse(avgRevenuePerCourse)
                .growthPercent(Math.round(growthPercent * 100.0) / 100.0)
                .build();
    }

    @Transactional(readOnly = true)
    public TeacherMonthlyRevenueResponse getMonthlyRevenue(UUID teacherId, Integer year) {
        log.info("Getting monthly revenue for teacher: {}, year: {}", teacherId, year);

        int targetYear = (year != null) ? year : Year.now().getValue();

        var wallet = walletRepository.findByUserId(teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        List<TeacherMonthlyRevenueResponse.MonthRevenue> months = new ArrayList<>();
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(targetYear, month);
            Instant startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            BigDecimal revenue = transactionRepository.sumByWalletIdAndTypeAndStatusAndDateRange(
                    wallet.getId(), TransactionType.INSTRUCTOR_REVENUE, TransactionStatus.SUCCESS, startOfMonth, endOfMonth);
            if (revenue == null) revenue = BigDecimal.ZERO;

            months.add(TeacherMonthlyRevenueResponse.MonthRevenue.builder()
                    .month(month)
                    .monthName(monthNames[month - 1])
                    .revenue(revenue)
                    .build());
        }

        return TeacherMonthlyRevenueResponse.builder()
                .year(targetYear)
                .months(months)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeacherTopCourseResponse> getTopCourses(UUID teacherId, int limit) {
        log.info("Getting top {} courses for teacher: {}", limit, teacherId);

        var wallet = walletRepository.findByUserId(teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        List<Course> teacherCourses = courseRepository.findByTeacherIdAndDeletedAtIsNull(teacherId);
        
        List<TeacherTopCourseResponse> topCourses = new ArrayList<>();

        for (Course course : teacherCourses) {
            // Get student count
            long studentCount = enrollmentRepository.countByCourseIdAndDeletedAtIsNull(course.getId());

            // Get total revenue for this course
            BigDecimal totalRevenue = transactionRepository.sumInstructorRevenueForCourse(
                    wallet.getId(), course.getId(), TransactionStatus.SUCCESS);
            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

            // TODO: Get avg rating from feedback/reviews (placeholder for now)
            BigDecimal avgRating = BigDecimal.valueOf(4.5);

            topCourses.add(TeacherTopCourseResponse.builder()
                    .courseId(course.getId())
                    .courseTitle(course.getTitle())
                    .thumbnailUrl(course.getThumbnailUrl())
                    .studentCount(studentCount)
                    .totalRevenue(totalRevenue)
                    .avgRating(avgRating)
                    .build());
        }

        // Sort by revenue descending and limit
        return topCourses.stream()
                .sorted((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactions(UUID teacherId, Pageable pageable) {
        log.info("Getting transactions for teacher: {}", teacherId);

        var wallet = walletRepository.findByUserId(teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        return transactionRepository
                .findByWalletIdAndType(wallet.getId(), TransactionType.INSTRUCTOR_REVENUE, pageable)
                .map(transaction -> mapToTransactionResponse(transaction, wallet.getBalance()));
    }

    private TransactionResponse mapToTransactionResponse(Transaction transaction, BigDecimal currentWalletBalance) {
        Instant effectiveTime = transaction.getCreatedAt() != null
                ? transaction.getCreatedAt()
                : transaction.getTransactionDate();
        BigDecimal balanceAfterTransaction;

        if (effectiveTime != null) {
            BigDecimal deltaAfter = transactionRepository.sumSuccessfulBalanceDeltaAfter(
                    transaction.getWallet().getId(), effectiveTime);
            balanceAfterTransaction = currentWalletBalance.subtract(deltaAfter);
        } else {
            balanceAfterTransaction = currentWalletBalance;
        }

        return TransactionResponse.builder()
                .transactionId(transaction.getId())
                .walletId(transaction.getWallet().getId())
                .orderCode(transaction.getOrderCode())
                .amount(transaction.getAmount())
                .balanceAfterTransaction(balanceAfterTransaction)
                .balance(balanceAfterTransaction)
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .paymentLinkId(transaction.getPaymentLinkId())
                .referenceCode(transaction.getReferenceCode())
                .transactionDate(transaction.getTransactionDate())
                .expiresAt(transaction.getExpiresAt())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
