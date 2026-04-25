package com.fptu.math_master.dto.response;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StudentDashboardOverviewResponse {
  StudentDashboardSummaryResponse summary;
  List<StudentDashboardUpcomingTaskResponse> upcomingTasks;
  List<StudentDashboardRecentGradeResponse> recentGrades;
  List<StudentDashboardLearningProgressResponse> learningProgress;
  StudentDashboardWeeklyActivityResponse weeklyActivity;
  StudentDashboardStreakResponse streak;
}
