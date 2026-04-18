package com.fptu.math_master.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseReviewSummaryResponse {
    private long totalReviews;
    private BigDecimal averageRating;
    private Map<Integer, Long> ratingDistribution; // Star level -> Count
}
