package com.fptu.math_master.dto/response;

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
public class TeacherTopCourseResponse {
    private UUID courseId;
    private String courseTitle;
    private String thumbnailUrl;
    private long studentCount;
    private BigDecimal totalRevenue;
    private BigDecimal avgRating;
}
