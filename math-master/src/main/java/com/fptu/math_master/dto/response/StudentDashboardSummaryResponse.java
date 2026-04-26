package com.fptu.math_master.dto.response;

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
public class StudentDashboardSummaryResponse {

  StudentInfo student;
  long notificationCount;
  Stats stats;
  Motivation motivation;
  int todayTaskCount;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class StudentInfo {
    String id;
    String name;
    String avatar;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Stats {
    long enrolledCourses;
    long completedAssignments;
    double averageScore;
    long pendingAssignments;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class Motivation {
    int goalAssignments;
    long completedAssignments;
    long remainingAssignments;
    double progressPercent;
  }
}
