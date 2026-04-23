package com.fptu.math_master.controller;

import com.fptu.math_master.dto.response.ApiResponse;
import com.fptu.math_master.dto.response.TeacherEarningsStatsResponse;
import com.fptu.math_master.dto.response.TeacherMonthlyRevenueResponse;
import com.fptu.math_master.dto.response.TeacherTopCourseResponse;
import com.fptu.math_master.dto.response.TransactionResponse;
import com.fptu.math_master.service.TeacherEarningsService;
import com.fptu.math_master.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/teacher/earnings")
@RequiredArgsConstructor
@Tag(name = "Teacher Earnings", description = "APIs for teacher earnings and revenue tracking")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('TEACHER')")
public class TeacherEarningsController {

    private final TeacherEarningsService teacherEarningsService;

    @GetMapping("/stats")
    @Operation(
        summary = "Get earnings statistics",
        description = "Returns total earnings, monthly earnings, pending earnings, student count, and growth metrics for the authenticated teacher")
    public ApiResponse<TeacherEarningsStatsResponse> getEarningsStats() {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return ApiResponse.<TeacherEarningsStatsResponse>builder()
                .result(teacherEarningsService.getEarningsStats(teacherId))
                .build();
    }

    @GetMapping("/monthly-revenue")
    @Operation(
        summary = "Get monthly revenue breakdown",
        description = "Returns revenue for each month of the specified year (defaults to current year)")
    public ApiResponse<TeacherMonthlyRevenueResponse> getMonthlyRevenue(
            @RequestParam(required = false) Integer year) {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return ApiResponse.<TeacherMonthlyRevenueResponse>builder()
                .result(teacherEarningsService.getMonthlyRevenue(teacherId, year))
                .build();
    }

    @GetMapping("/top-courses")
    @Operation(
        summary = "Get top performing courses",
        description = "Returns the teacher's courses sorted by revenue, with student count and ratings")
    public ApiResponse<List<TeacherTopCourseResponse>> getTopCourses(
            @RequestParam(defaultValue = "5") int limit) {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        return ApiResponse.<List<TeacherTopCourseResponse>>builder()
                .result(teacherEarningsService.getTopCourses(teacherId, limit))
                .build();
    }

    @GetMapping("/transactions")
    @Operation(
        summary = "Get earnings transactions",
        description = "Returns paginated list of instructor revenue transactions")
    public ApiResponse<Page<TransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String order) {
        UUID teacherId = SecurityUtils.getCurrentUserId();
        Sort.Direction direction = order.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        return ApiResponse.<Page<TransactionResponse>>builder()
                .result(teacherEarningsService.getMyTransactions(teacherId, pageable))
                .build();
    }
}
