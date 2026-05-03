package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAnalyticsResponse {
    private List<MonthlyUserStats> userStats;
    private List<MonthlyRevenueStats> revenueStats;
    private List<MonthlyEngagementStats> engagementStats;
    private List<MonthlyTeacherStats> teacherStats;
    private List<PlanDistribution> planDistribution;
    private List<SubjectEngagement> subjectEngagement;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyUserStats {
        private String month;
        private long students;
        private long teachers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenueStats {
        private String month;
        private BigDecimal revenue;
        private long transactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyEngagementStats {
        private String month;
        private long enrollments;
        private long videoViews;
        private long assessmentsCompleted;
        private long coursesCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTeacherStats {
        private String month;
        private long newTeachers;
        private long approvedTeachers;
        private long contentCreated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanDistribution {
        private String name;
        private long value;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectEngagement {
        private String subject;
        private long enrolled;
        private long videoViews;
        private long completed;
    }
}
