package com.fptu.math_master.configuration;

import com.fptu.math_master.repository.AiReviewRepository;
import com.fptu.math_master.repository.AnswerRepository;
import com.fptu.math_master.repository.AssessmentQuestionRepository;
import com.fptu.math_master.repository.AssessmentRepository;
import com.fptu.math_master.repository.GradeAuditLogRepository;
import com.fptu.math_master.repository.QuestionRepository;
import com.fptu.math_master.repository.QuizAttemptRepository;
import com.fptu.math_master.repository.RegradeRequestRepository;
import com.fptu.math_master.repository.SubmissionRepository;
import com.fptu.math_master.repository.UserRepository;
import com.fptu.math_master.service.GradingService;
import com.fptu.math_master.service.impl.GradingServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GradingServiceConfig {

  @Bean
  @Primary
  @ConditionalOnMissingBean(GradingService.class)
  public GradingService gradingService(
      SubmissionRepository submissionRepository,
      AnswerRepository answerRepository,
      QuestionRepository questionRepository,
      AssessmentQuestionRepository assessmentQuestionRepository,
      AssessmentRepository assessmentRepository,
      UserRepository userRepository,
      GradeAuditLogRepository gradeAuditLogRepository,
      RegradeRequestRepository regradeRequestRepository,
      QuizAttemptRepository quizAttemptRepository,
      AiReviewRepository aiReviewRepository) {
    return new GradingServiceImpl(
        submissionRepository,
        answerRepository,
        questionRepository,
        assessmentQuestionRepository,
        assessmentRepository,
        userRepository,
        gradeAuditLogRepository,
        regradeRequestRepository,
        quizAttemptRepository,
        aiReviewRepository);
  }
}
