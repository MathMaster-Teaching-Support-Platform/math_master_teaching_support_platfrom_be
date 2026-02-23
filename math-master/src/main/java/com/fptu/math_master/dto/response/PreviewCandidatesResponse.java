package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.CognitiveLevel;
import com.fptu.math_master.enums.QuestionDifficulty;
import com.fptu.math_master.enums.QuestionType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /exam-matrices/{matrixId}/cells/{cellId}/generate-preview.
 * Contains in-memory generated question candidates — NOTHING is written to the DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewCandidatesResponse {

  // ── Meta ────────────────────────────────────────────────────────────────

  private UUID templateId;
  private String templateName;
  private UUID cellId;
  private UUID matrixId;

  private int requestedCount;
  private int generatedCount;

  // ── Cell requirements (for UI display) ──────────────────────────────────

  private CellInfo cellRequirements;

  // ── Generated candidates ─────────────────────────────────────────────────

  private List<CandidateQuestion> candidates;

  // ── Warnings / diagnostics ───────────────────────────────────────────────

  /** Non-fatal messages: partial results, difficulty override, mismatch warnings, etc. */
  private List<String> warnings;

  // ─────────────────────────────────────────────────────────────────────────
  // Nested DTOs
  // ─────────────────────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CellInfo {
    private UUID cellId;
    private UUID chapterId;
    private String chapterTitle;
    private String topic;
    private CognitiveLevel cognitiveLevel;
    private QuestionDifficulty difficulty;
    private QuestionType questionType;
    private Integer numQuestions;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CandidateQuestion {

    /** 1-based index within this preview batch. */
    private int index;

    /** Fully rendered question text (placeholders substituted). */
    private String questionText;

    /**
     * Option map for MCQ: keys A/B/C/D → option text.
     * Null for non-MCQ types.
     */
    private Map<String, String> options;

    /**
     * For MCQ: the key of the correct option (A/B/C/D).
     * For non-MCQ: the correct answer value as a string.
     */
    private String correctAnswerKey;

    /** The concrete parameter values used to generate this question. */
    private Map<String, Object> usedParameters;

    /** Human-readable explanation of how the answer was computed, e.g. "3 + 5 = 8". */
    private String answerCalculation;

    /** Difficulty level determined by the template's difficultyRules for these parameters. */
    private QuestionDifficulty calculatedDifficulty;

    /** Optional step-by-step explanation / solution. */
    private String explanation;
  }
}

