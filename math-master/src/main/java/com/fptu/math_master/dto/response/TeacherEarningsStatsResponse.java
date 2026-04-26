package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherEarningsStatsResponse {
    private BigDecimal totalEarnings;
    private BigDecimal thisMonthEarnings;
    private BigDecimal pendingEarnings;
    private long totalStudents;
    private long activeCourses;
    private BigDecimal avgRevenuePerCourse;
    private double growthPercent; // vs previous month
}
