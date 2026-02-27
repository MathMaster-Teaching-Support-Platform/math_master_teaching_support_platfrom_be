package com.fptu.math_master.service.impl;

import com.fptu.math_master.entity.Assessment;
import com.fptu.math_master.entity.QuizAttempt;
import com.fptu.math_master.enums.AssessmentStatus;
import com.fptu.math_master.enums.SubmissionStatus;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.QuizAttemptRepository;
import com.fptu.math_master.service.AssessmentAutoSubmitService;
import com.fptu.math_master.service.AssessmentDraftService;
import com.fptu.math_master.service.CentrifugoService;
import com.fptu.math_master.service.GradingService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssessmentAutoSubmitServiceImpl implements AssessmentAutoSubmitService {

  QuizAttemptRepository quizAttemptRepository;
  AssessmentRepository assessmentRepository;
  AssessmentDraftService draftService;
  CentrifugoService centrifugoService;
  GradingService gradingService;

  @Override
  @Scheduled(fixedRate = 60000)
  @Transactional
  public void autoSubmitExpiredAttempts() {
    log.debug("Checking for expired assessment attempts...");

    Instant now = Instant.now();
    List<QuizAttempt> expiredAttempts = quizAttemptRepository.findExpiredAttempts(now);

    for (QuizAttempt attempt : expiredAttempts) {
      try {
        Assessment assessment =
            assessmentRepository
                .findById(attempt.getAssessmentId())
                .orElseThrow(() -> new AppException(ErrorCode.ASSESSMENT_NOT_FOUND));

        if (assessment.getTimeLimitMinutes() != null) {
          Instant expiresAt =
              attempt.getStartedAt().plusSeconds(assessment.getTimeLimitMinutes() * 60L);

          if (now.isAfter(expiresAt)) {
            log.info(
                "Auto-submitting expired attempt: {} for student: {}",
                attempt.getId(),
                attempt.getStudentId());

            try {
              draftService.flushDraftToDatabase(attempt.getId());
            } catch (Exception e) {
              log.error("Error flushing draft for attempt: {}", attempt.getId(), e);
            }

            attempt.setSubmittedAt(now);
            attempt.setStatus(SubmissionStatus.SUBMITTED);
            attempt.setTimeSpentSeconds(
                (int) Duration.between(attempt.getStartedAt(), now).getSeconds());

            quizAttemptRepository.save(attempt);
            draftService.deleteDraft(attempt.getId());
            centrifugoService.publishSubmitted(attempt.getId());

            // Trigger auto-grading
            try {
              gradingService.autoGradeSubmission(attempt.getSubmissionId());
            } catch (Exception ex) {
              log.error(
                  "Error during auto-grading for submission: {}", attempt.getSubmissionId(), ex);
            }

            log.info("Successfully auto-submitted attempt: {}", attempt.getId());
          }
        }

      } catch (Exception e) {
        log.error("Error auto-submitting attempt: {}", attempt.getId(), e);
      }
    }

    if (!expiredAttempts.isEmpty()) {
      log.info("Auto-submitted {} expired attempts", expiredAttempts.size());
    }
  }

  @Override
  @Scheduled(fixedRate = 300_000) // every 5 minutes
  @Transactional
  public void autoCloseExpiredAssessments() {
    log.debug("Checking for assessments to auto-close...");
    Instant now = Instant.now();

    List<Assessment> expired =
        assessmentRepository.findPublishedAssessmentsWithExpiredEndDate(now);

    for (Assessment assessment : expired) {
      try {
        assessment.setStatus(AssessmentStatus.CLOSED);
        assessmentRepository.save(assessment);
        log.info("Auto-closed assessment {} (endDate={})", assessment.getId(), assessment.getEndDate());
      } catch (Exception e) {
        log.error("Failed to auto-close assessment {}", assessment.getId(), e);
      }
    }

    if (!expired.isEmpty()) {
      log.info("Auto-closed {} expired assessment(s)", expired.size());
    }
  }
}
