package com.fptu.math_master.service.impl;

import com.fptu.math_master.entity.*;
import com.fptu.math_master.repository.*;
import com.fptu.math_master.service.ProgressRecalculationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProgressRecalculationServiceImpl implements ProgressRecalculationService {

  LearningRoadmapRepository roadmapRepository;
  RoadmapTopicRepository topicRepository;
  TopicCourseRepository topicCourseRepository;
  CourseRepository courseRepository;
  CourseLessonRepository courseLessonRepository;
  EnrollmentRepository enrollmentRepository;
  LessonProgressRepository lessonProgressRepository;
  AssessmentRepository assessmentRepository;

  @Override
  @Transactional
  public void recalculateRoadmapProgress(UUID roadmapId) {
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId).orElse(null);
    if (roadmap == null || roadmap.getDeletedAt() != null) {
      log.warn("Roadmap {} not found or deleted", roadmapId);
      return;
    }

    UUID studentId = roadmap.getStudentId();
    if (studentId == null) {
      log.warn("Roadmap {} has no student ID", roadmapId);
      return;
    }

    // Get all active topics for this roadmap
    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)
        .stream()
        .filter(t -> t.getDeletedAt() == null)
        .toList();

    int totalRoadmapLessons = 0;
    int completedRoadmapLessons = 0;
    int completedTopics = 0;

    for (RoadmapTopic topic : topics) {
      // Get courses for this topic
      List<TopicCourse> topicCourses = topicCourseRepository.findByTopicId(topic.getId());
      
      int topicTotalLessons = 0;
      int topicCompletedLessons = 0;

      for (TopicCourse topicCourse : topicCourses) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(topicCourse.getCourseId()).orElse(null);
        if (course == null) continue;

        // Count total lessons in course
        long courseTotalLessons = courseLessonRepository.countByCourseIdAndNotDeleted(course.getId());
        topicTotalLessons += courseTotalLessons;

        // Count completed lessons for this student
        Enrollment enrollment = enrollmentRepository
            .findByStudentIdAndCourseIdAndDeletedAtIsNull(studentId, course.getId())
            .orElse(null);
        
        if (enrollment != null) {
          long courseCompletedLessons = lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId());
          topicCompletedLessons += courseCompletedLessons;
        }
      }

      totalRoadmapLessons += topicTotalLessons;
      completedRoadmapLessons += topicCompletedLessons;

      // Consider topic completed if 100% of lessons are done
      if (topicTotalLessons > 0 && topicCompletedLessons >= topicTotalLessons) {
        completedTopics++;
      }
    }

    // Calculate overall progress
    BigDecimal progressPercentage = BigDecimal.ZERO;
    if (totalRoadmapLessons > 0) {
      progressPercentage = BigDecimal.valueOf((completedRoadmapLessons * 100.0) / totalRoadmapLessons)
          .setScale(2, RoundingMode.HALF_UP);
    }

    // Update roadmap
    roadmap.setProgressPercentage(progressPercentage);
    roadmap.setCompletedTopicsCount(completedTopics);
    roadmap.setTotalTopicsCount(topics.size());
    roadmap.setUpdatedAt(Instant.now());

    roadmapRepository.save(roadmap);

    log.info("Recalculated progress for roadmap {}: {}% ({}/{} lessons, {}/{} topics)", 
        roadmapId, progressPercentage, completedRoadmapLessons, totalRoadmapLessons, 
        completedTopics, topics.size());
  }

  @Override
  @Transactional
  public void recalculateAllRoadmapProgress() {
    List<LearningRoadmap> roadmaps = roadmapRepository.findAll()
        .stream()
        .filter(r -> r.getDeletedAt() == null && r.getStudentId() != null)
        .toList();

    log.info("Recalculating progress for {} roadmaps", roadmaps.size());

    for (LearningRoadmap roadmap : roadmaps) {
      try {
        recalculateRoadmapProgress(roadmap.getId());
      } catch (Exception e) {
        log.error("Error recalculating progress for roadmap {}: {}", roadmap.getId(), e.getMessage());
      }
    }

    log.info("Completed progress recalculation for all roadmaps");
  }

  @Override
  @Transactional
  public void cleanupInvalidEntryTests() {
    List<LearningRoadmap> roadmapsWithEntryTests = roadmapRepository.findAll()
        .stream()
        .filter(r -> r.getDeletedAt() == null && r.getEntryTestId() != null)
        .toList();

    int cleanedCount = 0;

    for (LearningRoadmap roadmap : roadmapsWithEntryTests) {
      boolean assessmentExists = assessmentRepository
          .findByIdAndNotDeleted(roadmap.getEntryTestId())
          .isPresent();

      if (!assessmentExists) {
        log.warn("Cleaning invalid entry test {} from roadmap {}", 
            roadmap.getEntryTestId(), roadmap.getId());
        roadmap.setEntryTestId(null);
        roadmap.setUpdatedAt(Instant.now());
        roadmapRepository.save(roadmap);
        cleanedCount++;
      }
    }

    log.info("Cleaned {} invalid entry test references", cleanedCount);
  }

  @Override
  @Transactional(readOnly = true)
  public String diagnoseProgressIssues(UUID roadmapId) {
    StringBuilder diagnosis = new StringBuilder();
    
    LearningRoadmap roadmap = roadmapRepository.findById(roadmapId).orElse(null);
    if (roadmap == null) {
      return "Roadmap not found";
    }

    diagnosis.append("=== ROADMAP PROGRESS DIAGNOSIS ===\n");
    diagnosis.append(String.format("Roadmap ID: %s\n", roadmapId));
    diagnosis.append(String.format("Roadmap Name: %s\n", roadmap.getName()));
    diagnosis.append(String.format("Student ID: %s\n", roadmap.getStudentId()));
    diagnosis.append(String.format("Stored Progress: %s%%\n", roadmap.getProgressPercentage()));
    diagnosis.append(String.format("Completed Topics: %d/%d\n", 
        roadmap.getCompletedTopicsCount(), roadmap.getTotalTopicsCount()));

    if (roadmap.getStudentId() == null) {
      diagnosis.append("ERROR: No student ID - this is a template roadmap\n");
      return diagnosis.toString();
    }

    // Check topics
    List<RoadmapTopic> topics = topicRepository.findByRoadmapIdOrderBySequenceOrder(roadmapId)
        .stream()
        .filter(t -> t.getDeletedAt() == null)
        .toList();

    diagnosis.append(String.format("\n=== TOPICS (%d) ===\n", topics.size()));

    int totalLessons = 0;
    int completedLessons = 0;

    for (RoadmapTopic topic : topics) {
      List<TopicCourse> topicCourses = topicCourseRepository.findByTopicId(topic.getId());
      
      int topicTotal = 0;
      int topicCompleted = 0;

      for (TopicCourse tc : topicCourses) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(tc.getCourseId()).orElse(null);
        if (course == null) continue;

        long courseTotal = courseLessonRepository.countByCourseIdAndNotDeleted(course.getId());
        topicTotal += courseTotal;

        Enrollment enrollment = enrollmentRepository
            .findByStudentIdAndCourseIdAndDeletedAtIsNull(roadmap.getStudentId(), course.getId())
            .orElse(null);

        if (enrollment != null) {
          long courseCompleted = lessonProgressRepository.countCompletedByEnrollmentId(enrollment.getId());
          topicCompleted += courseCompleted;
        }
      }

      totalLessons += topicTotal;
      completedLessons += topicCompleted;

      double topicProgress = topicTotal > 0 ? (topicCompleted * 100.0) / topicTotal : 0.0;
      
      diagnosis.append(String.format("Topic: %s - %d/%d lessons (%.1f%%) - %d courses\n", 
          topic.getTitle(), topicCompleted, topicTotal, topicProgress, topicCourses.size()));
    }

    double calculatedProgress = totalLessons > 0 ? (completedLessons * 100.0) / totalLessons : 0.0;
    
    diagnosis.append(String.format("\n=== CALCULATED PROGRESS ===\n"));
    diagnosis.append(String.format("Total Lessons: %d\n", totalLessons));
    diagnosis.append(String.format("Completed Lessons: %d\n", completedLessons));
    diagnosis.append(String.format("Calculated Progress: %.2f%%\n", calculatedProgress));
    diagnosis.append(String.format("Stored Progress: %s%%\n", roadmap.getProgressPercentage()));
    
    if (Math.abs(calculatedProgress - roadmap.getProgressPercentage().doubleValue()) > 1.0) {
      diagnosis.append("WARNING: Progress mismatch detected!\n");
    } else {
      diagnosis.append("Progress calculation looks correct.\n");
    }

    return diagnosis.toString();
  }
}