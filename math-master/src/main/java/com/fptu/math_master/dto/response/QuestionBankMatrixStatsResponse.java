package com.fptu.math_master.dto.response;

import lombok.*;
import java.util.*;

/**
 * Response DTO for hierarchical question bank statistics
 * Provides matrix-friendly view of questions grouped by:
 * Grade Level > Chapter > Question Type > Cognitive Level
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankMatrixStatsResponse {
    private String gradeLevel;
    private int totalQuestions;
    private List<ChapterStats> chapters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterStats {
        private UUID chapterId;
        private String chapterName;
        private int totalQuestions;
        private List<TypeStats> types;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeStats {
        private String questionType; // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER
        private int totalQuestions;
        private Map<String, Integer> cognitiveCounts; // NB:2, TH:3, VD:1, VDC:0
    }
}
