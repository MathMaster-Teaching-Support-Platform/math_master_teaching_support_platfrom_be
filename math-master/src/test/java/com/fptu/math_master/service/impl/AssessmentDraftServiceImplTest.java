package com.fptu.math_master.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.math_master.BaseUnitTest;
import com.fptu.math_master.entity.Answer;
import com.fptu.math_master.entity.QuizAttempt;
import com.fptu.math_master.exception.AppException;
import com.fptu.math_master.exception.ErrorCode;
import com.fptu.math_master.repository.AnswerRepository;
import com.fptu.math_master.repository.QuizAttemptRepository;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

@DisplayName("AssessmentDraftServiceImpl - Tests")
class AssessmentDraftServiceImplTest extends BaseUnitTest {

  @InjectMocks private AssessmentDraftServiceImpl service;

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private QuizAttemptRepository quizAttemptRepository;
  @Mock private AnswerRepository answerRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private HashOperations<String, Object, Object> hashOperations;

  private UUID attemptId;
  private UUID assessmentId;
  private UUID questionId;

  @BeforeEach
  void setUp() throws Exception {
    attemptId = UUID.randomUUID();
    assessmentId = UUID.randomUUID();
    questionId = UUID.randomUUID();
    when(redisTemplate.opsForHash()).thenReturn(hashOperations);

    Field ttlField = AssessmentDraftServiceImpl.class.getDeclaredField("draftTtl");
    ttlField.setAccessible(true);
    ttlField.set(service, 3600);
  }

  /** Normal case: Initializes draft keys and applies TTL for metadata, answers, and flags. */
  @Test
  void it_should_initialize_draft_and_set_ttl_for_all_keys() {
    // ===== ARRANGE =====

    // ===== ACT =====
    service.initDraft(attemptId, assessmentId, 30);

    // ===== ASSERT & VERIFY =====
    verify(hashOperations, times(1)).putAll(eq("draft:attempt:" + attemptId + ":meta"), any(Map.class));
    verify(hashOperations, times(1))
        .put("draft:attempt:" + attemptId + ":answers", "initialized", "true");
    verify(hashOperations, times(1)).put("draft:attempt:" + attemptId + ":flags", "initialized", "true");
    verify(redisTemplate, times(3)).expire(any(String.class), eq(3600L), eq(TimeUnit.SECONDS));
  }

  /** Normal case: Initializes draft without expiresAt metadata when time limit is null. */
  @Test
  void it_should_initialize_draft_without_expires_at_when_time_limit_is_null() {
    // ===== ARRANGE =====

    // ===== ACT =====
    service.initDraft(attemptId, assessmentId, null);

    // ===== ASSERT & VERIFY =====
    verify(hashOperations, times(1)).putAll(eq("draft:attempt:" + attemptId + ":meta"), any(Map.class));
    verify(redisTemplate, times(3)).expire(any(String.class), eq(3600L), eq(TimeUnit.SECONDS));
  }

  /** Normal case: Initializes draft without expiresAt when time limit is non-positive. */
  @Test
  void it_should_initialize_draft_without_expires_at_when_time_limit_is_non_positive() {
    // ===== ARRANGE =====

    // ===== ACT =====
    service.initDraft(attemptId, assessmentId, 0);

    // ===== ASSERT & VERIFY =====
    verify(hashOperations, times(1)).putAll(eq("draft:attempt:" + attemptId + ":meta"), any(Map.class));
    verify(redisTemplate, times(3)).expire(any(String.class), eq(3600L), eq(TimeUnit.SECONDS));
  }

  /** Normal case: Saves answer to Redis as JSON and refreshes TTL. */
  @Test
  void it_should_save_answer_to_redis_when_serialization_succeeds() throws Exception {
    // ===== ARRANGE =====
    Map<String, Object> answer = Map.of("value", "x^2 + 1");
    when(objectMapper.writeValueAsString(answer)).thenReturn("{\"value\":\"x^2 + 1\"}");

    // ===== ACT =====
    service.saveAnswer(attemptId, questionId, answer);

    // ===== ASSERT & VERIFY =====
    verify(hashOperations, times(1))
        .put("draft:attempt:" + attemptId + ":answers", questionId.toString(), "{\"value\":\"x^2 + 1\"}");
    verify(redisTemplate, times(1))
        .expire("draft:attempt:" + attemptId + ":answers", 3600L, TimeUnit.SECONDS);
  }

  /** Abnormal case: Throws runtime exception when answer serialization fails. */
  @Test
  void it_should_throw_runtime_exception_when_save_answer_serialization_fails() throws Exception {
    // ===== ARRANGE =====
    when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("json error"));

    // ===== ACT & ASSERT =====
    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> service.saveAnswer(attemptId, questionId, Map.of("a", 1)));
    assertEquals("Failed to save answer", exception.getMessage());
  }

  /** Normal case: Returns snapshot with parsed answers and flags, excluding initialized markers. */
  @Test
  void it_should_return_draft_snapshot_with_answers_flags_and_metadata() throws Exception {
    // ===== ARRANGE =====
    Map<Object, Object> answersRaw = new HashMap<>();
    answersRaw.put("initialized", "true");
    answersRaw.put(questionId.toString(), "{\"text\":\"Correct option\"}");
    Map<Object, Object> flagsRaw = new HashMap<>();
    flagsRaw.put(questionId.toString(), "true");
    Map<Object, Object> metaRaw = new HashMap<>();
    metaRaw.put("assessmentId", assessmentId.toString());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answersRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(flagsRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(metaRaw);
    when(objectMapper.readValue("{\"text\":\"Correct option\"}", Object.class))
        .thenReturn(Map.of("text", "Correct option"));

    // ===== ACT =====
    Map<String, Object> snapshot = service.getDraftSnapshot(attemptId);

    // ===== ASSERT =====
    assertEquals(attemptId.toString(), snapshot.get("attemptId"));
    assertEquals(assessmentId.toString(), snapshot.get("assessmentId"));
    assertNotNull(snapshot.get("answers"));
    assertNotNull(snapshot.get("flags"));
  }

  /** Normal case: Ignores malformed answer JSON in snapshot and continues processing other fields. */
  @Test
  void it_should_ignore_malformed_answer_json_when_building_snapshot() throws Exception {
    // ===== ARRANGE =====
    Map<Object, Object> answersRaw = new HashMap<>();
    answersRaw.put(questionId.toString(), "{malformed-json}");
    Map<Object, Object> flagsRaw = new HashMap<>();
    flagsRaw.put(questionId.toString(), "false");
    Map<Object, Object> metaRaw = new HashMap<>();
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answersRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(flagsRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(metaRaw);
    when(objectMapper.readValue("{malformed-json}", Object.class))
        .thenThrow(new RuntimeException("parse error"));

    // ===== ACT =====
    Map<String, Object> snapshot = service.getDraftSnapshot(attemptId);

    // ===== ASSERT =====
    Map<?, ?> answers = (Map<?, ?>) snapshot.get("answers");
    Map<?, ?> flags = (Map<?, ?>) snapshot.get("flags");
    assertTrue(answers.isEmpty());
    assertEquals(Boolean.FALSE, flags.get(questionId.toString()));
  }

  /** Abnormal case: Throws when flushing draft for unknown attempt. */
  @Test
  void it_should_throw_exception_when_flushing_draft_for_unknown_attempt() {
    // ===== ARRANGE =====
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(new HashMap<>());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(new HashMap<>());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(new HashMap<>());
    when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.empty());

    // ===== ACT & ASSERT =====
    AppException exception =
        assertThrows(AppException.class, () -> service.flushDraftToDatabase(attemptId));
    assertEquals(ErrorCode.QUIZ_ATTEMPT_NOT_FOUND, exception.getErrorCode());

    // ===== VERIFY =====
    verify(answerRepository, never()).save(any(Answer.class));
  }

  /** Normal case: Flushes string and map answers to database entities. */
  @Test
  void it_should_flush_answer_values_to_database_when_snapshot_contains_answers() throws Exception {
    // ===== ARRANGE =====
    UUID submissionId = UUID.randomUUID();
    QuizAttempt attempt = QuizAttempt.builder().submissionId(submissionId).build();
    attempt.setId(attemptId);

    Map<Object, Object> answersRaw = new HashMap<>();
    answersRaw.put(questionId.toString(), "\"choice-A\"");
    Map<Object, Object> flagsRaw = new HashMap<>();
    Map<Object, Object> metaRaw = new HashMap<>();

    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answersRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(flagsRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(metaRaw);
    when(objectMapper.readValue("\"choice-A\"", Object.class)).thenReturn("choice-A");
    when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(answerRepository.findBySubmissionIdAndQuestionId(submissionId, questionId)).thenReturn(Optional.empty());

    // ===== ACT =====
    service.flushDraftToDatabase(attemptId);

    // ===== ASSERT & VERIFY =====
    verify(answerRepository, times(1)).save(any(Answer.class));
  }

  /** Normal case: Flushes map answer payload into answerData field. */
  @Test
  void it_should_flush_map_answer_into_answer_data_when_answer_value_is_map() throws Exception {
    // ===== ARRANGE =====
    UUID submissionId = UUID.randomUUID();
    QuizAttempt attempt = QuizAttempt.builder().submissionId(submissionId).build();
    attempt.setId(attemptId);

    Map<Object, Object> answersRaw = new HashMap<>();
    answersRaw.put(questionId.toString(), "{\"value\":\"A\"}");
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answersRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(new HashMap<>());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(new HashMap<>());
    when(objectMapper.readValue("{\"value\":\"A\"}", Object.class)).thenReturn(Map.of("value", "A"));
    when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(answerRepository.findBySubmissionIdAndQuestionId(submissionId, questionId)).thenReturn(Optional.empty());

    // ===== ACT =====
    service.flushDraftToDatabase(attemptId);

    // ===== ASSERT & VERIFY =====
    verify(answerRepository, times(1)).save(any(Answer.class));
  }

  /** Normal case: Keeps answer unchanged when parsed answer value is neither map nor string. */
  @Test
  void it_should_skip_answer_data_update_when_answer_value_type_is_unsupported() throws Exception {
    // ===== ARRANGE =====
    UUID submissionId = UUID.randomUUID();
    QuizAttempt attempt = QuizAttempt.builder().submissionId(submissionId).build();
    attempt.setId(attemptId);
    Map<Object, Object> answersRaw = new HashMap<>();
    answersRaw.put(questionId.toString(), "123");
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answersRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(new HashMap<>());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(new HashMap<>());
    when(objectMapper.readValue("123", Object.class)).thenReturn(123);
    when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
    when(answerRepository.findBySubmissionIdAndQuestionId(submissionId, questionId)).thenReturn(Optional.empty());

    // ===== ACT =====
    service.flushDraftToDatabase(attemptId);

    // ===== ASSERT & VERIFY =====
    verify(answerRepository, times(1)).save(any(Answer.class));
  }

  /** Normal case: Skips flushing when snapshot contains invalid question id key. */
  @Test
  void it_should_skip_invalid_question_id_entry_when_flushing_draft() {
    // ===== ARRANGE =====
    UUID submissionId = UUID.randomUUID();
    QuizAttempt attempt = QuizAttempt.builder().submissionId(submissionId).build();
    attempt.setId(attemptId);
    Map<Object, Object> answersRaw = new HashMap<>();
    answersRaw.put("not-a-uuid", "\"choice-A\"");
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answersRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(new HashMap<>());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(new HashMap<>());
    when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

    // ===== ACT =====
    service.flushDraftToDatabase(attemptId);

    // ===== ASSERT & VERIFY =====
    verify(answerRepository, never()).save(any(Answer.class));
  }

  /** Normal case: Flush does nothing when snapshot has no answers. */
  @Test
  void it_should_not_persist_answers_when_snapshot_answers_are_empty() {
    // ===== ARRANGE =====
    UUID submissionId = UUID.randomUUID();
    QuizAttempt attempt = QuizAttempt.builder().submissionId(submissionId).build();
    attempt.setId(attemptId);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(new HashMap<>());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(new HashMap<>());
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(new HashMap<>());
    when(quizAttemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));

    // ===== ACT =====
    service.flushDraftToDatabase(attemptId);

    // ===== ASSERT & VERIFY =====
    verify(answerRepository, never()).save(any(Answer.class));
  }

  /** Normal case: Saves question flag and refreshes TTL. */
  @Test
  void it_should_save_flag_to_redis_and_refresh_ttl() {
    // ===== ARRANGE =====

    // ===== ACT =====
    service.saveFlag(attemptId, questionId, true);

    // ===== ASSERT & VERIFY =====
    verify(hashOperations, times(1)).put("draft:attempt:" + attemptId + ":flags", questionId.toString(), true);
    verify(redisTemplate, times(1))
        .expire("draft:attempt:" + attemptId + ":flags", 3600L, TimeUnit.SECONDS);
  }

  /** Normal case: Snapshot ignores initialized flag key and null metadata entries. */
  @Test
  void it_should_ignore_initialized_flag_and_null_metadata_entries_when_building_snapshot() throws Exception {
    // ===== ARRANGE =====
    Map<Object, Object> answersRaw = new HashMap<>();
    answersRaw.put(questionId.toString(), "\"A\"");
    Map<Object, Object> flagsRaw = new HashMap<>();
    flagsRaw.put("initialized", "true");
    flagsRaw.put(questionId.toString(), "true");
    Map<Object, Object> metaRaw = new HashMap<>();
    metaRaw.put(null, "ignored");
    metaRaw.put("startedAt", null);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answersRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":flags")).thenReturn(flagsRaw);
    when(hashOperations.entries("draft:attempt:" + attemptId + ":meta")).thenReturn(metaRaw);
    when(objectMapper.readValue("\"A\"", Object.class)).thenReturn("A");

    // ===== ACT =====
    Map<String, Object> snapshot = service.getDraftSnapshot(attemptId);

    // ===== ASSERT =====
    Map<?, ?> flags = (Map<?, ?>) snapshot.get("flags");
    assertEquals(1, flags.size());
    assertEquals(Boolean.TRUE, flags.get(questionId.toString()));
    assertTrue(!snapshot.containsKey("startedAt"));
  }

  /** Normal case: Deletes all draft keys for attempt and counts answered entries excluding marker. */
  @Test
  void it_should_delete_draft_keys_and_count_answered_entries() {
    // ===== ARRANGE =====
    Map<Object, Object> answers = new HashMap<>();
    answers.put("initialized", "true");
    answers.put(UUID.randomUUID().toString(), "{\"value\":\"A\"}");
    answers.put(UUID.randomUUID().toString(), "{\"value\":\"B\"}");
    when(hashOperations.entries("draft:attempt:" + attemptId + ":answers")).thenReturn(answers);

    // ===== ACT =====
    Integer count = service.getAnsweredCount(attemptId);
    service.deleteDraft(attemptId);

    // ===== ASSERT =====
    assertEquals(2, count);

    // ===== VERIFY =====
    verify(redisTemplate, times(1)).delete("draft:attempt:" + attemptId + ":answers");
    verify(redisTemplate, times(1)).delete("draft:attempt:" + attemptId + ":flags");
    verify(redisTemplate, times(1)).delete("draft:attempt:" + attemptId + ":meta");
  }
}
