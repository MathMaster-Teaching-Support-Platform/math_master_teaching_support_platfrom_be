package com.fptu.math_master.service;

import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.entity.Order;
import com.fptu.math_master.entity.Transaction;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.OrderStatus;
import com.fptu.math_master.enums.TransactionStatus;
import com.fptu.math_master.enums.TransactionType;
import com.fptu.math_master.enums.UserSubscriptionStatus;
import com.fptu.math_master.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AdminFinancialService {

    TransactionRepository transactionRepository;
    UserSubscriptionRepository userSubscriptionRepository;
    UserRepository userRepository;
    OrderRepository orderRepository;
    CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public AdminFinancialOverviewResponse getFinancialOverview(String month) {
        log.info("Getting financial overview for month: {}", month);

        YearMonth currentMonth = month != null ? YearMonth.parse(month) : YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        Instant currentStart = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant currentEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();
        Instant prevStart = previousMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant prevEnd = previousMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

        // Total Revenue (all successful transactions)
        BigDecimal currentRevenue = transactionRepository.sumSuccessfulRevenue(currentStart, currentEnd);
        BigDecimal prevRevenue = transactionRepository.sumSuccessfulRevenue(prevStart, prevEnd);
        double revenueTrend = calculateTrend(currentRevenue, prevRevenue);

        // Platform Commission (from course sales)
        BigDecimal currentCommission = calculatePlatformCommission(currentStart, currentEnd);
        BigDecimal prevCommission = calculatePlatformCommission(prevStart, prevEnd);
        double commissionTrend = calculateTrend(currentCommission, prevCommission);

        // Active Subscriptions
        long currentSubs = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                UserSubscriptionStatus.ACTIVE, currentStart, currentEnd);
        long prevSubs = userSubscriptionRepository.countByStatusAndCreatedAtBetween(
                UserSubscriptionStatus.ACTIVE, prevStart, prevEnd);
        double subsTrend = calculateTrend(currentSubs, prevSubs);

        // Total Instructors (users with TEACHER role)
        long currentInstructors = userRepository.countByRoleAndCreatedAtBetween("TEACHER", currentStart, currentEnd);
        long prevInstructors = userRepository.countByRoleAndCreatedAtBetween("TEACHER", prevStart, prevEnd);
        double instructorsTrend = calculateTrend(currentInstructors, prevInstructors);

        // Average Order Value
        BigDecimal currentAOV = calculateAvgOrderValue(currentStart, currentEnd);
        BigDecimal prevAOV = calculateAvgOrderValue(prevStart, prevEnd);
        double aovTrend = calculateTrend(currentAOV, prevAOV);

        // Active Users (users with at least one transaction or subscription)
        long currentActiveUsers = countActiveUsers(currentStart, currentEnd);
        long prevActiveUsers = countActiveUsers(prevStart, prevEnd);
        double activeUsersTrend = calculateTrend(currentActiveUsers, prevActiveUsers);

        // Conversion Rate (paid users / total users)
        long totalUsers = userRepository.count();
        long paidUsers = userSubscriptionRepository.countDistinctUsersByStatus(UserSubscriptionStatus.ACTIVE);
        double conversionRate = totalUsers > 0 ? (paidUsers * 100.0 / totalUsers) : 0.0;
        
        // Previous month conversion for trend
        long prevTotalUsers = userRepository.countByCreatedAtBefore(prevEnd);
        long prevPaidUsers = userSubscriptionRepository.countDistinctUsersByStatusAndCreatedAtBefore(
                UserSubscriptionStatus.ACTIVE, prevEnd);
        double prevConversionRate = prevTotalUsers > 0 ? (prevPaidUsers * 100.0 / prevTotalUsers) : 0.0;
        double conversionTrend = conversionRate - prevConversionRate;

        // Churn Rate (cancelled/expired subscriptions / total subscriptions)
        long currentChurned = userSubscriptionRepository.countByStatusInAndUpdatedAtBetween(
                Arrays.asList(UserSubscriptionStatus.CANCELLED, UserSubscriptionStatus.EXPIRED),
                currentStart, currentEnd);
        long currentTotal = userSubscriptionRepository.countByCreatedAtBefore(currentEnd);
        double churnRate = currentTotal > 0 ? (currentChurned * 100.0 / currentTotal) : 0.0;
        
        long prevChurned = userSubscriptionRepository.countByStatusInAndUpdatedAtBetween(
                Arrays.asList(UserSubscriptionStatus.CANCELLED, UserSubscriptionStatus.EXPIRED),
                prevStart, prevEnd);
        long prevTotal = userSubscriptionRepository.countByCreatedAtBefore(prevEnd);
        double prevChurnRate = prevTotal > 0 ? (prevChurned * 100.0 / prevTotal) : 0.0;
        double churnTrend = churnRate - prevChurnRate;

        return AdminFinancialOverviewResponse.builder()
                .totalRevenue(currentRevenue)
                .totalRevenueTrend(revenueTrend)
                .platformCommission(currentCommission)
                .platformCommissionTrend(commissionTrend)
                .activeSubscriptions(currentSubs)
                .activeSubscriptionsTrend(subsTrend)
                .totalInstructors(currentInstructors)
                .totalInstructorsTrend(instructorsTrend)
                .avgOrderValue(currentAOV)
                .avgOrderValueTrend(aovTrend)
                .activeUsers(currentActiveUsers)
                .activeUsersTrend(activeUsersTrend)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .conversionRateTrend(Math.round(conversionTrend * 100.0) / 100.0)
                .churnRate(Math.round(churnRate * 100.0) / 100.0)
                .churnRateTrend(Math.round(churnTrend * 100.0) / 100.0)
                .period(currentMonth.toString())
                .build();
    }

    @Transactional(readOnly = true)
    public RevenueBreakdownResponse getRevenueBreakdown(String period) {
        log.info("Getting revenue breakdown for period: {}", period);

        int days = parsePeriod(period);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<RevenueBreakdownResponse.DailyRevenue> data = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant dayEnd = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

            // Deposits
            BigDecimal deposits = transactionRepository.sumByTypeAndStatusAndDateRange(
                    TransactionType.DEPOSIT, TransactionStatus.SUCCESS, dayStart, dayEnd);
            if (deposits == null) deposits = BigDecimal.ZERO;

            // Subscriptions
            BigDecimal subscriptions = transactionRepository.sumByTypeAndStatusAndDateRange(
                    TransactionType.PAYMENT, TransactionStatus.SUCCESS, dayStart, dayEnd);
            if (subscriptions == null) subscriptions = BigDecimal.ZERO;

            // Course sales (platform commission)
            BigDecimal courseSales = calculatePlatformCommission(dayStart, dayEnd);

            BigDecimal total = deposits.add(subscriptions).add(courseSales);

            data.add(RevenueBreakdownResponse.DailyRevenue.builder()
                    .date(date.toString())
                    .deposits(deposits)
                    .subscriptions(subscriptions)
                    .courseSales(courseSales)
                    .total(total)
                    .build());
        }

        return RevenueBreakdownResponse.builder()
                .period(period)
                .data(data)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MarketplaceTopCourseResponse> getTopCourses(int limit) {
        log.info("Getting top {} courses", limit);

        List<Order> completedOrders = orderRepository.findByStatusAndDeletedAtIsNull(OrderStatus.COMPLETED);

        Map<UUID, CourseStats> courseStatsMap = new HashMap<>();

        for (Order order : completedOrders) {
            UUID courseId = order.getCourseId();
            CourseStats stats = courseStatsMap.getOrDefault(courseId, new CourseStats());
            stats.salesCount++;
            stats.totalRevenue = stats.totalRevenue.add(order.getFinalPrice());
            stats.platformCommission = stats.platformCommission.add(order.getPlatformCommission());
            stats.instructorEarnings = stats.instructorEarnings.add(order.getInstructorEarnings());
            courseStatsMap.put(courseId, stats);
        }

        return courseStatsMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().totalRevenue.compareTo(a.getValue().totalRevenue))
                .limit(limit)
                .map(entry -> {
                    UUID courseId = entry.getKey();
                    CourseStats stats = entry.getValue();
                    
                    var course = courseRepository.findByIdAndDeletedAtIsNull(courseId).orElse(null);
                    if (course == null) return null;

                    var instructor = userRepository.findById(course.getTeacherId()).orElse(null);

                    return MarketplaceTopCourseResponse.builder()
                            .courseId(courseId)
                            .courseTitle(course.getTitle())
                            .thumbnailUrl(course.getThumbnailUrl())
                            .instructorId(course.getTeacherId())
                            .instructorName(instructor != null ? instructor.getFullName() : "Unknown")
                            .salesCount(stats.salesCount)
                            .totalRevenue(stats.totalRevenue)
                            .platformCommission(stats.platformCommission)
                            .instructorEarnings(stats.instructorEarnings)
                            .avgRating(BigDecimal.valueOf(4.5)) // TODO: Get from reviews
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarketplaceTopInstructorResponse> getTopInstructors(int limit) {
        log.info("Getting top {} instructors", limit);

        List<User> teachers = userRepository.findByRole("TEACHER");
        List<MarketplaceTopInstructorResponse> instructors = new ArrayList<>();

        for (User teacher : teachers) {
            var courses = courseRepository.findByTeacherIdAndDeletedAtIsNull(teacher.getId());
            
            long totalSales = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal totalEarnings = BigDecimal.ZERO;
            Set<UUID> uniqueStudents = new HashSet<>();

            for (var course : courses) {
                List<Order> orders = orderRepository.findByCourseIdAndStatusAndDeletedAtIsNull(
                        course.getId(), OrderStatus.COMPLETED);
                
                totalSales += orders.size();
                for (Order order : orders) {
                    totalRevenue = totalRevenue.add(order.getFinalPrice());
                    totalEarnings = totalEarnings.add(order.getInstructorEarnings());
                    uniqueStudents.add(order.getStudentId());
                }
            }

            if (totalSales > 0) {
                instructors.add(MarketplaceTopInstructorResponse.builder()
                        .instructorId(teacher.getId())
                        .instructorName(teacher.getFullName())
                        .avatarUrl(teacher.getAvatar())
                        .courseCount(courses.size())
                        .totalSales(totalSales)
                        .totalRevenue(totalRevenue)
                        .totalEarnings(totalEarnings)
                        .avgRating(BigDecimal.valueOf(4.5)) // TODO: Get from reviews
                        .totalStudents(uniqueStudents.size())
                        .build());
            }
        }

        return instructors.stream()
                .sorted((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SystemHealthResponse getSystemHealth() {
        log.info("Getting system health");

        Instant now = Instant.now();
        Instant last24h = now.minus(24, ChronoUnit.HOURS);

        // Metrics
        long totalTransactions = transactionRepository.countByCreatedAtBetween(last24h, now);
        long successfulTransactions = transactionRepository.countByStatusAndCreatedAtBetween(
                TransactionStatus.SUCCESS, last24h, now);
        long failedTransactions = transactionRepository.countByStatusInAndCreatedAtBetween(
                Arrays.asList(TransactionStatus.FAILED, TransactionStatus.CANCELLED), last24h, now);
        long pendingTransactions = transactionRepository.countByStatus(TransactionStatus.PENDING);

        double successRate = totalTransactions > 0 ? 
                (successfulTransactions * 100.0 / totalTransactions) : 100.0;

        // Calculate avg processing time (simplified - using transaction date - created date)
        double avgProcessingTime = 2300.0; // Placeholder - would need actual calculation

        SystemHealthResponse.Metrics metrics = SystemHealthResponse.Metrics.builder()
                .totalTransactions24h(totalTransactions)
                .successRate(Math.round(successRate * 100.0) / 100.0)
                .avgProcessingTimeMs(avgProcessingTime)
                .failedTransactions24h(failedTransactions)
                .pendingTransactions(pendingTransactions)
                .build();

        // Alerts
        List<SystemHealthResponse.Alert> alerts = new ArrayList<>();
        
        if (pendingTransactions > 10) {
            alerts.add(SystemHealthResponse.Alert.builder()
                    .severity("warning")
                    .message(pendingTransactions + " transactions pending > 10 minutes")
                    .timestamp(now.toString())
                    .build());
        }

        if (failedTransactions > 50) {
            alerts.add(SystemHealthResponse.Alert.builder()
                    .severity("warning")
                    .message(failedTransactions + " failed transactions in last 24 hours")
                    .timestamp(now.toString())
                    .build());
        }

        if (successRate < 95.0) {
            alerts.add(SystemHealthResponse.Alert.builder()
                    .severity("critical")
                    .message("Success rate below 95%: " + String.format("%.2f%%", successRate))
                    .timestamp(now.toString())
                    .build());
        }

        // Gateway Status
        SystemHealthResponse.GatewayStatus gatewayStatus = SystemHealthResponse.GatewayStatus.builder()
                .payosStatus("operational")
                .lastWebhook("2 seconds ago") // Would need actual tracking
                .webhookSuccessRate(99.8)
                .build();

        // Overall Status
        String status = "healthy";
        if (!alerts.isEmpty()) {
            boolean hasCritical = alerts.stream().anyMatch(a -> "critical".equals(a.getSeverity()));
            status = hasCritical ? "critical" : "warning";
        }

        return SystemHealthResponse.builder()
                .status(status)
                .alerts(alerts)
                .metrics(metrics)
                .gatewayStatus(gatewayStatus)
                .build();
    }

    // Helper methods

    private BigDecimal calculatePlatformCommission(Instant start, Instant end) {
        List<Order> orders = orderRepository.findByStatusAndConfirmedAtBetweenAndDeletedAtIsNull(
                OrderStatus.COMPLETED, start, end);
        return orders.stream()
                .map(Order::getPlatformCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateAvgOrderValue(Instant start, Instant end) {
        List<Order> orders = orderRepository.findByStatusAndConfirmedAtBetweenAndDeletedAtIsNull(
                OrderStatus.COMPLETED, start, end);
        if (orders.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal total = orders.stream()
                .map(Order::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
    }

    private long countActiveUsers(Instant start, Instant end) {
        Set<UUID> activeUserIds = new HashSet<>();
        
        // Users with transactions
        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(start, end);
        transactions.forEach(t -> {
            if (t.getWallet() != null && t.getWallet().getUser() != null) {
                activeUserIds.add(t.getWallet().getUser().getId());
            }
        });
        
        // Users with subscriptions
        var subscriptions = userSubscriptionRepository.findByCreatedAtBetween(start, end);
        subscriptions.forEach(s -> activeUserIds.add(s.getUserId()));
        
        return activeUserIds.size();
    }

    private double calculateTrend(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double calculateTrend(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) * 100.0) / previous;
    }

    private int parsePeriod(String period) {
        if (period == null) return 30;
        return switch (period) {
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            case "1y" -> 365;
            default -> 30;
        };
    }

    // Helper class for course statistics
    private static class CourseStats {
        long salesCount = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal platformCommission = BigDecimal.ZERO;
        BigDecimal instructorEarnings = BigDecimal.ZERO;
    }
}
