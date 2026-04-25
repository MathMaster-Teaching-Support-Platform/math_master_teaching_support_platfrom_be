package com.fptu.math_master.service;

import java.util.List;

import com.fptu.math_master.dto.response.StudentDashboardLearningProgressResponse;
import com.fptu.math_master.dto.response.StudentDashboardOverviewResponse;
import com.fptu.math_master.dto.response.StudentDashboardRecentGradeResponse;
import com.fptu.math_master.dto.response.StudentDashboardStreakResponse;
import com.fptu.math_master.dto.response.StudentDashboardSummaryResponse;
import com.fptu.math_master.dto.response.StudentDashboardUpcomingTaskResponse;
import com.fptu.math_master.dto.response.StudentDashboardWeeklyActivityResponse;

public interface StudentDashboardService {

  StudentDashboardSummaryResponse getSummary();

  List<StudentDashboardUpcomingTaskResponse> getUpcomingTasks();

  List<StudentDashboardRecentGradeResponse> getRecentGrades();

  List<StudentDashboardLearningProgressResponse> getLearningProgress();

  StudentDashboardWeeklyActivityResponse getWeeklyActivity();

  StudentDashboardStreakResponse getStreak();

  StudentDashboardOverviewResponse getOverview();
}
