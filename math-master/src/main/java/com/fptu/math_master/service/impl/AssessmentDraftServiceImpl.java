package com.fptu.math_master.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.entity.Answer;
import com.fptu.math_master.entity.QuizAttempt;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AnswerRepository;
import com.fptu.math_master.repository.QuizAttemptRepository;
import com.fptu.math_master.service.AssessmentDraftService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssessmentDraftServiceImpl implements AssessmentDraftService {

    final RedisTemplate<String, Object> redisTemplate;
    final QuizAttemptRepository quizAttemptRepository;
    final AnswerRepository answerRepository;
    final ObjectMapper objectMapper;

    @Value("${assessment.draft.ttl:86400}")
    Integer draftTtl;

    private static final String ANSWERS_KEY_PREFIX = "draft:attempt:";
    private static final String ANSWERS_SUFFIX = ":answers";
    private static final String FLAGS_SUFFIX = ":flags";
    private static final String META_SUFFIX = ":meta";

    @Override
    public void initDraft(UUID attemptId, UUID assessmentId, Integer timeLimitMinutes) {
        log.info("Initializing draft for attempt: {}", attemptId);

        String metaKey = ANSWERS_KEY_PREFIX + attemptId + META_SUFFIX;

        Map<String, Object> meta = new HashMap<>();
        meta.put("attemptId", attemptId.toString());
        meta.put("assessmentId", assessmentId.toString());
        meta.put("startedAt", Instant.now().toString());

        if (timeLimitMinutes != null && timeLimitMinutes > 0) {
            Instant expiresAt = Instant.now().plusSeconds(timeLimitMinutes * 60L);
            meta.put("expiresAt", expiresAt.toString());
        }

        redisTemplate.opsForHash().putAll(metaKey, meta);
        redisTemplate.expire(metaKey, draftTtl, TimeUnit.SECONDS);

        String answersKey = ANSWERS_KEY_PREFIX + attemptId + ANSWERS_SUFFIX;
        String flagsKey = ANSWERS_KEY_PREFIX + attemptId + FLAGS_SUFFIX;

        redisTemplate.opsForHash().put(answersKey, "initialized", "true");
        redisTemplate.expire(answersKey, draftTtl, TimeUnit.SECONDS);

        redisTemplate.opsForHash().put(flagsKey, "initialized", "true");
        redisTemplate.expire(flagsKey, draftTtl, TimeUnit.SECONDS);

        log.info("Draft initialized successfully for attempt: {}", attemptId);
    }

    @Override
    public void saveAnswer(UUID attemptId, UUID questionId, Object answerValue) {
        String answersKey = ANSWERS_KEY_PREFIX + attemptId + ANSWERS_SUFFIX;

        try {
            String answerJson = objectMapper.writeValueAsString(answerValue);
            redisTemplate.opsForHash().put(answersKey, questionId.toString(), answerJson);
            redisTemplate.expire(answersKey, draftTtl, TimeUnit.SECONDS);

            log.debug("Saved answer for attempt {} question {}", attemptId, questionId);
        } catch (Exception e) {
            log.error("Error saving answer to Redis", e);
            throw new RuntimeException("Failed to save answer", e);
        }
    }

    @Override
    public void saveFlag(UUID attemptId, UUID questionId, Boolean flagged) {
        String flagsKey = ANSWERS_KEY_PREFIX + attemptId + FLAGS_SUFFIX;

        redisTemplate.opsForHash().put(flagsKey, questionId.toString(), flagged);
        redisTemplate.expire(flagsKey, draftTtl, TimeUnit.SECONDS);

        log.debug("Saved flag for attempt {} question {}: {}", attemptId, questionId, flagged);
    }

    @Override
    public Map<String, Object> getDraftSnapshot(UUID attemptId) {
        String answersKey = ANSWERS_KEY_PREFIX + attemptId + ANSWERS_SUFFIX;
        String flagsKey = ANSWERS_KEY_PREFIX + attemptId + FLAGS_SUFFIX;
        String metaKey = ANSWERS_KEY_PREFIX + attemptId + META_SUFFIX;

        Map<Object, Object> answersRaw = redisTemplate.opsForHash().entries(answersKey);
        Map<Object, Object> flagsRaw = redisTemplate.opsForHash().entries(flagsKey);
        Map<Object, Object> metaRaw = redisTemplate.opsForHash().entries(metaKey);

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("attemptId", attemptId.toString());

        Map<String, Object> answers = new HashMap<>();
        answersRaw.forEach((key, value) -> {
            if (!"initialized".equals(key)) {
                try {
                    answers.put(key.toString(), objectMapper.readValue(value.toString(), Object.class));
                } catch (Exception e) {
                    log.error("Error parsing answer for key: {}", key, e);
                }
            }
        });
        snapshot.put("answers", answers);

        Map<String, Boolean> flags = new HashMap<>();
        flagsRaw.forEach((key, value) -> {
            if (!"initialized".equals(key)) {
                flags.put(key.toString(), Boolean.valueOf(value.toString()));
            }
        });
        snapshot.put("flags", flags);

        // Add metadata
        metaRaw.forEach((key, value) -> {
            if (key != null && value != null) {
                snapshot.put(key.toString(), value);
            }
        });

        return snapshot;
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public void flushDraftToDatabase(UUID attemptId) {
        log.info("Flushing draft to database for attempt: {}", attemptId);

        Map<String, Object> snapshot = getDraftSnapshot(attemptId);
        Map<String, Object> answers = (Map<String, Object>) snapshot.get("answers");

        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND));

        if (answers != null && !answers.isEmpty()) {
            answers.forEach((questionIdStr, answerValue) -> {
                try {
                    UUID questionId = UUID.fromString(questionIdStr);

                    Answer answer = answerRepository
                            .findBySubmissionIdAndQuestionId(attempt.getSubmissionId(), questionId)
                            .orElse(Answer.builder()
                                    .submissionId(attempt.getSubmissionId())
                                    .questionId(questionId)
                                    .build());

                    if (answerValue instanceof Map) {
                        answer.setAnswerData((Map<String, Object>) answerValue);
                    } else if (answerValue instanceof String) {
                        answer.setAnswerText(answerValue.toString());
                    }

                    answerRepository.save(answer);

                } catch (Exception e) {
                    log.error("Error flushing answer for question {}", questionIdStr, e);
                }
            });
        }

        log.info("Draft flushed successfully for attempt: {}", attemptId);
    }

    @Override
    public void deleteDraft(UUID attemptId) {
        String answersKey = ANSWERS_KEY_PREFIX + attemptId + ANSWERS_SUFFIX;
        String flagsKey = ANSWERS_KEY_PREFIX + attemptId + FLAGS_SUFFIX;
        String metaKey = ANSWERS_KEY_PREFIX + attemptId + META_SUFFIX;

        redisTemplate.delete(answersKey);
        redisTemplate.delete(flagsKey);
        redisTemplate.delete(metaKey);

        log.info("Draft deleted for attempt: {}", attemptId);
    }

    @Override
    public Integer getAnsweredCount(UUID attemptId) {
        String answersKey = ANSWERS_KEY_PREFIX + attemptId + ANSWERS_SUFFIX;
        Map<Object, Object> answers = redisTemplate.opsForHash().entries(answersKey);

        return (int) answers.keySet().stream()
                .filter(key -> !"initialized".equals(key))
                .count();
    }
}



