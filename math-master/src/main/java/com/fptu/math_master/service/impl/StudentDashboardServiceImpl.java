package com.fptu.math_master.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fptu.math_master.dto.response.StudentDashboardLearningProgressResponse;
import com.fptu.math_master.dto.response.StudentDashboardOverviewResponse;
import com.fptu.math_master.dto.response.StudentDashboardRecentGradeResponse;
import com.fptu.math_master.dto.response.StudentDashboardStreakResponse;
import com.fptu.math_master.dto.response.StudentDashboardSummaryResponse;
import com.fptu.math_master.dto.response.StudentDashboardUpcomingTaskResponse;
import com.fptu.math_master.dto.response.StudentDashboardWeeklyActivityResponse;
import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.Enrollment;
import com.fptu.math_master.entity.Submission;
import com.fptu.math_master.entity.User;
import com.fptu.math_master.enums.AssessmentType;
import com.fptu.math_master.enums.EnrollmentStatus;
import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.CourseAssessmentRepository;
import com.fptu.math_master.repository.CourseLessonRepository;
import com.fptu.math_master.repository.EnrollmentRepository;
import com.fptu.math_master.repository.LessonProgressRepository;
import com.fptu.math_master.repository.NotificationRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.StudentDashboardService;
import com.fptu.math_master.util.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentDashboardServiceImpl implements StudentDashboardService {

  private static final ZoneId DASHBOARD_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final int WEEK_DAYS = 7;

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final SubmissionRepository submissionRepository;
  private final CourseAssessmentRepository courseAssessmentRepository;
  private final CourseLessonRepository courseLessonRepository;
  private final LessonProgressRepository lessonProgressRepository;

  @Override
  @Cacheable(cacheNames = "studentDashboardSummary", key = "T(com.fptu.math_master.util.SecurityUtils).getCurrentUserId()")
  public StudentDashboardSummaryResponse getSummary() {
    UUID studentId = SecurityUtils.getCurrentUserId();
    User student =
        userRepository
            .findById(studentId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

    long notificationCount = notificationRepository.countByRecipient_IdAndIsReadFalse(studentId);
    long enrolledCourses =
        enrollmentRepository.countByStudentIdAndStatusAndDeletedAtIsNull(
            studentId, EnrollmentStatus.ACTIVE);

    long completedAssignments =
        submissionRepository.countByStudentIdAndStatusInAndDeletedAtIsNull(
            studentId, List.of(SubmissionStatus.SUBMITTED, SubmissionStatus.GRADED));

    double averageScore =
        round2(Optional.ofNullable(submissionRepository.averageScoreOfGradedByStudentId(studentId)).orElse(0.0));

    List<StudentDashboardUpcomingTaskResponse> pendingTasks = buildPendingTasks(studentId, 500);
    long pendingAssignments = pendingTasks.size();

    LocalDate today = LocalDate.now(DASHBOARD_ZONE);
    long todayTaskCount =
        pendingTasks.stream()
            .filter(task -> isSameLocalDate(task.getDueDate(), today))
            .count();

    int goalAssignments = safeToInt(completedAssignments + pendingAssignments);
    long remainingAssignments = Math.max(0, pendingAssignments);
    double progressPercent =
        goalAssignments > 0 ? round2((completedAssignments * 100.0) / goalAssignments) : 0.0;

    return StudentDashboardSummaryResponse.builder()
        .student(
            StudentDashboardSummaryResponse.StudentInfo.builder()
                .id(student.getId().toString())
                .name(student.getFullName())
                .avatar(student.getAvatar())
                .build())
        .notificationCount(notificationCount)
        .stats(
            StudentDashboardSummaryResponse.Stats.builder()
                .enrolledCourses(enrolledCourses)
                .completedAssignments(completedAssignments)
                .averageScore(averageScore)
                .pendingAssignments(pendingAssignments)
                .build())
        .motivation(
            StudentDashboardSummaryResponse.Motivation.builder()
                .goalAssignments(goalAssignments)
                .completedAssignments(completedAssignments)
                .remainingAssignments(remainingAssignments)
                .progressPercent(progressPercent)
                .build())
        .todayTaskCount(safeToInt(todayTaskCount))
        .build();
  }

  @Override
  @Cacheable(cacheNames = "studentDashboardUpcomingTasks", key = "T(com.fptu.math_master.util.SecurityUtils).getCurrentUserId()")
  public List<StudentDashboardUpcomingTaskResponse> getUpcomingTasks() {
    UUID studentId = SecurityUtils.getCurrentUserId();
    return buildPendingTasks(studentId, 10);
  }

  @Override
  @Cacheable(cacheNames = "studentDashboardRecentGrades", key = "T(com.fptu.math_master.util.SecurityUtils).getCurrentUserId()")
  public List<StudentDashboardRecentGradeResponse> getRecentGrades() {
    UUID studentId = SecurityUtils.getCurrentUserId();

    List<Submission> recentGraded =
        submissionRepository.findTop10ByStudentIdAndStatusAndDeletedAtIsNullOrderByGradedAtDesc(
            studentId, SubmissionStatus.GRADED);

    if (recentGraded.isEmpty()) {
      return List.of();
    }

    Set<UUID> assessmentIds = new HashSet<>();
    for (Submission submission : recentGraded) {
      if (submission.getAssessmentId() != null) {
        assessmentIds.add(submission.getAssessmentId());
      }
    }

    Map<UUID, String> subjectByAssessmentId = new HashMap<>();
    if (!assessmentIds.isEmpty()) {
      List<Object[]> subjectRows = courseAssessmentRepository.findSubjectNameByAssessmentIds(assessmentIds);
      for (Object[] row : subjectRows) {
        UUID assessmentId = (UUID) row[0];
        String subjectName = row[1] != null ? row[1].toString() : "General";
        subjectByAssessmentId.putIfAbsent(assessmentId, subjectName);
      }
    }

    List<StudentDashboardRecentGradeResponse> result = new ArrayList<>();
    for (Submission submission : recentGraded) {
      Assessment assessment = submission.getAssessment();
      String title = assessment != null ? assessment.getTitle() : "Assessment";
      String subject =
          subjectByAssessmentId.getOrDefault(submission.getAssessmentId(), "General");

      result.add(
          StudentDashboardRecentGradeResponse.builder()
              .id(submission.getId().toString())
              .title(title)
              .subject(subject)
              .score(readSubmissionScore(submission))
              .gradedAt(
                  firstNonNull(
                      submission.getGradedAt(), submission.getUpdatedAt(), submission.getCreatedAt()))
              .build());
    }

    return result;
  }

  @Override
  @Cacheable(cacheNames = "studentDashboardLearningProgress", key = "T(com.fptu.math_master.util.SecurityUtils).getCurrentUserId()")
  public List<StudentDashboardLearningProgressResponse> getLearningProgress() {
    UUID studentId = SecurityUtils.getCurrentUserId();
    List<Enrollment> enrollments =
        enrollmentRepository.findByStudentIdAndStatusWithCourseAndSubject(
            studentId, EnrollmentStatus.ACTIVE);

    if (enrollments.isEmpty()) {
      return List.of();
    }

    List<UUID> courseIds =
        enrollments.stream().map(Enrollment::getCourseId).distinct().collect(Collectors.toList());
    List<UUID> enrollmentIds =
        enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

    Map<UUID, Long> totalLessonsByCourseId = new HashMap<>();
    if (!courseIds.isEmpty()) {
      List<Object[]> totalRows = courseLessonRepository.countByCourseIdsAndNotDeleted(courseIds);
      for (Object[] row : totalRows) {
        totalLessonsByCourseId.put((UUID) row[0], ((Number) row[1]).longValue());
      }
    }

    Map<UUID, Long> completedLessonsByEnrollmentId = new HashMap<>();
    if (!enrollmentIds.isEmpty()) {
      List<Object[]> completedRows =
          lessonProgressRepository.countCompletedByEnrollmentIds(enrollmentIds);
      for (Object[] row : completedRows) {
        completedLessonsByEnrollmentId.put((UUID) row[0], ((Number) row[1]).longValue());
      }
    }

    Map<String, long[]> totalsBySubject = new HashMap<>();

    for (Enrollment enrollment : enrollments) {
      String subjectName =
          enrollment.getCourse() != null && enrollment.getCourse().getSubject() != null
              ? enrollment.getCourse().getSubject().getName()
              : (enrollment.getCourse() != null
                  ? enrollment.getCourse().getTitle()
                  : "General");

      long totalLessons = totalLessonsByCourseId.getOrDefault(enrollment.getCourseId(), 0L);
      long doneLessons = completedLessonsByEnrollmentId.getOrDefault(enrollment.getId(), 0L);

      long[] accumulator = totalsBySubject.computeIfAbsent(subjectName, k -> new long[] {0L, 0L});
      accumulator[0] += doneLessons;
      accumulator[1] += totalLessons;
    }

    List<StudentDashboardLearningProgressResponse> responses = new ArrayList<>();
    for (Map.Entry<String, long[]> entry : totalsBySubject.entrySet()) {
      long done = entry.getValue()[0];
      long total = entry.getValue()[1];
      double percent = total > 0 ? round2((done * 100.0) / total) : 0.0;

      responses.add(
          StudentDashboardLearningProgressResponse.builder()
              .subject(entry.getKey())
              .doneLessons(done)
              .totalLessons(total)
              .percent(percent)
              .build());
    }

    responses.sort(Comparator.comparing(StudentDashboardLearningProgressResponse::getSubject));
    return responses;
  }

  @Override
  @Cacheable(cacheNames = "studentDashboardWeeklyActivity", key = "T(com.fptu.math_master.util.SecurityUtils).getCurrentUserId()")
  public StudentDashboardWeeklyActivityResponse getWeeklyActivity() {
    UUID studentId = SecurityUtils.getCurrentUserId();

    LocalDate today = LocalDate.now(DASHBOARD_ZONE);
    Instant currentStart = today.minusDays(WEEK_DAYS - 1L).atStartOfDay(DASHBOARD_ZONE).toInstant();
    Instant currentEnd = today.plusDays(1L).atStartOfDay(DASHBOARD_ZONE).toInstant();

    Instant previousStart = today.minusDays((WEEK_DAYS * 2L) - 1L).atStartOfDay(DASHBOARD_ZONE).toInstant();
    Instant previousEnd = today.minusDays(WEEK_DAYS - 1L).atStartOfDay(DASHBOARD_ZONE).toInstant();

    Map<LocalDate, Long> currentSecondsByDay = collectActivitySecondsByDay(studentId, currentStart, currentEnd);
    Map<LocalDate, Long> previousSecondsByDay = collectActivitySecondsByDay(studentId, previousStart, previousEnd);

    long currentTotalSeconds = currentSecondsByDay.values().stream().mapToLong(Long::longValue).sum();
    long previousTotalSeconds = previousSecondsByDay.values().stream().mapToLong(Long::longValue).sum();

    double currentHours = round2(currentTotalSeconds / 3600.0);
    double deltaPercent =
        previousTotalSeconds == 0
            ? (currentTotalSeconds > 0 ? 100.0 : 0.0)
            : round2(((currentTotalSeconds - previousTotalSeconds) * 100.0) / previousTotalSeconds);

    List<StudentDashboardWeeklyActivityResponse.DayHours> days = new ArrayList<>();
    for (int i = WEEK_DAYS - 1; i >= 0; i--) {
      LocalDate day = today.minusDays(i);
      long seconds = currentSecondsByDay.getOrDefault(day, 0L);
      days.add(
          StudentDashboardWeeklyActivityResponse.DayHours.builder()
              .dayLabel(toDayLabel(day.getDayOfWeek()))
              .hours(round2(seconds / 3600.0))
              .build());
    }

    return StudentDashboardWeeklyActivityResponse.builder()
        .range(
            StudentDashboardWeeklyActivityResponse.DateRange.builder()
                .from(currentStart)
                .to(currentEnd)
                .build())
        .totalHours(currentHours)
        .deltaPercentVsPreviousWeek(deltaPercent)
        .days(days)
        .build();
  }

  @Override
  @Cacheable(cacheNames = "studentDashboardStreak", key = "T(com.fptu.math_master.util.SecurityUtils).getCurrentUserId()")
  public StudentDashboardStreakResponse getStreak() {
    UUID studentId = SecurityUtils.getCurrentUserId();
    LocalDate today = LocalDate.now(DASHBOARD_ZONE);

    Instant lookback = today.minusDays(60L).atStartOfDay(DASHBOARD_ZONE).toInstant();
    Set<LocalDate> activeDays = collectActiveDays(studentId, lookback);

    int streak = 0;
    LocalDate cursor = today;
    while (activeDays.contains(cursor)) {
      streak++;
      cursor = cursor.minusDays(1L);
    }

    List<StudentDashboardStreakResponse.StreakDay> days = new ArrayList<>();
    for (int i = WEEK_DAYS - 1; i >= 0; i--) {
      LocalDate day = today.minusDays(i);
      days.add(
          StudentDashboardStreakResponse.StreakDay.builder()
              .dayLabel(toDayLabel(day.getDayOfWeek()))
              .active(activeDays.contains(day))
              .build());
    }

    String message =
        streak > 0
            ? "You are on a " + streak + "-day study streak."
            : "Start your streak by finishing a lesson or assignment today.";

    return StudentDashboardStreakResponse.builder()
        .currentStreakDays(streak)
        .days(days)
        .message(message)
        .build();
  }

  @Override
  @Cacheable(cacheNames = "studentDashboardOverview", key = "T(com.fptu.math_master.util.SecurityUtils).getCurrentUserId()")
  public StudentDashboardOverviewResponse getOverview() {
    return StudentDashboardOverviewResponse.builder()
        .summary(getSummary())
        .upcomingTasks(getUpcomingTasks())
        .recentGrades(getRecentGrades())
        .learningProgress(getLearningProgress())
        .weeklyActivity(getWeeklyActivity())
        .streak(getStreak())
        .build();
  }

  private List<StudentDashboardUpcomingTaskResponse> buildPendingTasks(UUID studentId, int limit) {
    Instant now = Instant.now();
    int pageSize = Math.max(limit, 200);

    List<Object[]> rows =
        courseAssessmentRepository.findUpcomingAssessmentsByStudentId(
            studentId, now, PageRequest.of(0, pageSize));

    if (rows.isEmpty()) {
      return List.of();
    }

    List<Assessment> assessments = new ArrayList<>();
    Map<UUID, String> subjectByAssessmentId = new HashMap<>();
    for (Object[] row : rows) {
      Assessment assessment = (Assessment) row[0];
      String subject = row[1] != null ? row[1].toString() : "General";
      assessments.add(assessment);
      subjectByAssessmentId.putIfAbsent(assessment.getId(), subject);
    }

    Set<UUID> assessmentIds = new HashSet<>();
    for (Assessment assessment : assessments) {
      assessmentIds.add(assessment.getId());
    }

    Map<UUID, Submission> latestSubmissionByAssessmentId = new HashMap<>();
    if (!assessmentIds.isEmpty()) {
      List<Submission> submissions =
          submissionRepository.findByStudentIdAndAssessmentIdInOrderByCreatedAtDesc(
              studentId, assessmentIds);
      for (Submission submission : submissions) {
        latestSubmissionByAssessmentId.putIfAbsent(submission.getAssessmentId(), submission);
      }
    }

    List<StudentDashboardUpcomingTaskResponse> tasks = new ArrayList<>();
    for (Assessment assessment : assessments) {
      Submission latest = latestSubmissionByAssessmentId.get(assessment.getId());
      if (isCompletedSubmission(latest)) {
        continue;
      }

      tasks.add(
          StudentDashboardUpcomingTaskResponse.builder()
              .id(assessment.getId().toString())
              .title(assessment.getTitle())
              .subject(subjectByAssessmentId.getOrDefault(assessment.getId(), "General"))
              .dueDate(assessment.getEndDate())
              .type(mapTaskType(assessment.getAssessmentType()))
              .progressPercent(readSubmissionProgress(latest))
              .build());
    }

    tasks.sort(
        Comparator.comparing(
            StudentDashboardUpcomingTaskResponse::getDueDate,
            Comparator.nullsLast(Comparator.naturalOrder())));

    if (limit > 0 && tasks.size() > limit) {
      return tasks.subList(0, limit);
    }

    return tasks;
  }

  private Map<LocalDate, Long> collectActivitySecondsByDay(UUID studentId, Instant from, Instant to) {
    Map<LocalDate, Long> secondsByDay = new HashMap<>();

    List<Object[]> watchRows = lessonProgressRepository.findWatchActivityForWindow(studentId, from, to);
    for (Object[] row : watchRows) {
      Instant timestamp = (Instant) row[0];
      long seconds = ((Number) row[1]).longValue();
      accumulateByDay(secondsByDay, timestamp, seconds);
    }

    List<Object[]> submissionRows = submissionRepository.findSubmissionActivityForWindow(studentId, from, to);
    for (Object[] row : submissionRows) {
      Instant timestamp = (Instant) row[0];
      long seconds = ((Number) row[1]).longValue();
      accumulateByDay(secondsByDay, timestamp, seconds);
    }

    return secondsByDay;
  }

  private Set<LocalDate> collectActiveDays(UUID studentId, Instant from) {
    Set<LocalDate> activeDays = new HashSet<>();

    List<Instant> watchTimes = lessonProgressRepository.findLastWatchedAtAfter(studentId, from);
    for (Instant watchedAt : watchTimes) {
      activeDays.add(toLocalDate(watchedAt));
    }

    List<Instant> submittedTimes = submissionRepository.findSubmittedAtAfter(studentId, from);
    for (Instant submittedAt : submittedTimes) {
      activeDays.add(toLocalDate(submittedAt));
    }

    return activeDays;
  }

  private boolean isCompletedSubmission(Submission submission) {
    if (submission == null || submission.getStatus() == null) {
      return false;
    }

    return submission.getStatus() == SubmissionStatus.SUBMITTED
        || submission.getStatus() == SubmissionStatus.GRADED;
  }

  private double readSubmissionProgress(Submission submission) {
    if (submission == null) {
      return 0.0;
    }

    if (isCompletedSubmission(submission)) {
      return 100.0;
    }

    if (submission.getPercentage() != null) {
      return round2(submission.getPercentage().doubleValue());
    }

    return 0.0;
  }

  private double readSubmissionScore(Submission submission) {
    if (submission.getFinalScore() != null) {
      return round2(submission.getFinalScore().doubleValue());
    }
    if (submission.getScore() != null) {
      return round2(submission.getScore().doubleValue());
    }
    if (submission.getPercentage() != null) {
      return round2(submission.getPercentage().doubleValue());
    }
    return 0.0;
  }

  private String mapTaskType(AssessmentType type) {
    if (type == null) {
      return "quiz";
    }

    return switch (type) {
      case HOMEWORK -> "homework";
      case QUIZ -> "quiz";
      case EXAM, TEST -> "exam";
    };
  }

  private String toDayLabel(DayOfWeek dayOfWeek) {
    return switch (dayOfWeek) {
      case MONDAY -> "T2";
      case TUESDAY -> "T3";
      case WEDNESDAY -> "T4";
      case THURSDAY -> "T5";
      case FRIDAY -> "T6";
      case SATURDAY -> "T7";
      case SUNDAY -> "CN";
    };
  }

  private void accumulateByDay(Map<LocalDate, Long> secondsByDay, Instant timestamp, long seconds) {
    if (timestamp == null || seconds <= 0) {
      return;
    }

    LocalDate day = toLocalDate(timestamp);
    secondsByDay.merge(day, seconds, Long::sum);
  }

  private LocalDate toLocalDate(Instant timestamp) {
    return timestamp.atZone(DASHBOARD_ZONE).toLocalDate();
  }

  private boolean isSameLocalDate(Instant timestamp, LocalDate target) {
    if (timestamp == null) {
      return false;
    }
    return toLocalDate(timestamp).isEqual(target);
  }

  private int safeToInt(long value) {
    if (value > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (value < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) value;
  }

  @SafeVarargs
  private static <T> T firstNonNull(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private double round2(double value) {
    return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
  }
}
