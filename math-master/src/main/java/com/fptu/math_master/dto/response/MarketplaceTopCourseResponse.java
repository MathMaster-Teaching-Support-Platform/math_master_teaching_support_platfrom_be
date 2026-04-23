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
public class MarketplaceTopCourseResponse {
    private UUID courseId;
    private String courseTitle;
    private String thumbnailUrl;
    private UUID instructorId;
    private String instructorName;
    private long salesCount;
    private BigDecimal totalRevenue;
    private BigDecimal platformCommission;
    private BigDecimal instructorEarnings;
    private BigDecimal avgRating;
}
