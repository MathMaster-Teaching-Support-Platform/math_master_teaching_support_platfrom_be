package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.*;
import com.fptu.math_master.service.AdminFinancialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(name = "Admin Financial Management", description = "APIs for admin financial dashboard and analytics")
@SecurityRequirement(name = "bearerAuth")
public class AdminFinancialController {

    AdminFinancialService adminFinancialService;

    @GetMapping("/dashboard/financial-overview")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get financial overview",
        description = "Get comprehensive financial overview with metrics and trends for admin dashboard"
    )
    public ResponseEntity<ApiResponse<AdminFinancialOverviewResponse>> getFinancialOverview(
            @Parameter(description = "Month in YYYY-MM format (default: current month)")
            @RequestParam(required = false) String month) {
        log.info("Admin requesting financial overview for month: {}", month);
        
        AdminFinancialOverviewResponse overview = adminFinancialService.getFinancialOverview(month);
        
        return ResponseEntity.ok(ApiResponse.<AdminFinancialOverviewResponse>builder()
                .code(200)
                .message("Financial overview retrieved successfully")
                .result(overview)
                .build());
    }

    @GetMapping("/dashboard/revenue-breakdown")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get revenue breakdown",
        description = "Get revenue breakdown by source (deposits, subscriptions, course sales)"
    )
    public ResponseEntity<ApiResponse<RevenueBreakdownResponse>> getRevenueBreakdown(
            @Parameter(description = "Period: 7d, 30d, 90d, 1y (default: 30d)")
            @RequestParam(required = false, defaultValue = "30d") String period,
            @Parameter(description = "Grouping: hour | day | month")
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.info("Admin requesting revenue breakdown period={} groupBy={} from={} to={}", period, groupBy, from, to);
        
        RevenueBreakdownResponse breakdown = adminFinancialService.getRevenueBreakdown(period, groupBy, from, to);
        
        return ResponseEntity.ok(ApiResponse.<RevenueBreakdownResponse>builder()
                .code(200)
                .message("Revenue breakdown retrieved successfully")
                .result(breakdown)
                .build());
    }

    @GetMapping("/marketplace/top-courses")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get top selling courses",
        description = "Get top selling courses with revenue and commission breakdown"
    )
    public ResponseEntity<ApiResponse<List<MarketplaceTopCourseResponse>>> getTopCourses(
            @Parameter(description = "Number of courses to return (default: 10)")
            @RequestParam(required = false, defaultValue = "10") int limit) {
        log.info("Admin requesting top {} courses", limit);
        
        List<MarketplaceTopCourseResponse> topCourses = adminFinancialService.getTopCourses(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<MarketplaceTopCourseResponse>>builder()
                .code(200)
                .message("Top courses retrieved successfully")
                .result(topCourses)
                .build());
    }

    @GetMapping("/marketplace/top-instructors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get top instructors",
        description = "Get top earning instructors with sales and revenue statistics"
    )
    public ResponseEntity<ApiResponse<List<MarketplaceTopInstructorResponse>>> getTopInstructors(
            @Parameter(description = "Number of instructors to return (default: 10)")
            @RequestParam(required = false, defaultValue = "10") int limit) {
        log.info("Admin requesting top {} instructors", limit);
        
        List<MarketplaceTopInstructorResponse> topInstructors = adminFinancialService.getTopInstructors(limit);
        
        return ResponseEntity.ok(ApiResponse.<List<MarketplaceTopInstructorResponse>>builder()
                .code(200)
                .message("Top instructors retrieved successfully")
                .result(topInstructors)
                .build());
    }

    @GetMapping("/system/health/financial")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get financial system health",
        description = "Get system health status, metrics, and alerts for financial operations"
    )
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getSystemHealth() {
        log.info("Admin requesting system health");
        
        SystemHealthResponse health = adminFinancialService.getSystemHealth();
        
        return ResponseEntity.ok(ApiResponse.<SystemHealthResponse>builder()
                .code(200)
                .message("System health retrieved successfully")
                .result(health)
                .build());
    }

    @GetMapping("/dashboard/full-analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Get full system analytics",
        description = "Get comprehensive analytics including user, engagement, teacher and revenue stats"
    )
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getFullAnalytics(
            @Parameter(description = "Year (default: current year)")
            @RequestParam(required = false) Integer year) {
        log.info("Admin requesting full analytics for year: {}", year);
        
        AdminAnalyticsResponse analytics = adminFinancialService.getFullAnalytics(year != null ? year : LocalDate.now().getYear());
        
        return ResponseEntity.ok(ApiResponse.<AdminAnalyticsResponse>builder()
                .code(200)
                .message("Full analytics retrieved successfully")
                .result(analytics)
                .build());
    }
}
