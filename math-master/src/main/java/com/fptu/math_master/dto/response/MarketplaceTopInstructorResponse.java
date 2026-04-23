package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceTopInstructorResponse {
    private UUID instructorId;
    private String instructorName;
    private String avatarUrl;
    private int courseCount;
    private long totalSales;
    private BigDecimal totalRevenue;
    private BigDecimal totalEarnings;
    private BigDecimal avgRating;
    private long totalStudents;
}
