package com.fptu.math_master.dto.response;

import java.util.List;

public record LessonPageHistoryEntryResponse(
    String id,
    String action,
    String changedBy,
    String changedAt,
    String summary,
    LessonPageResponse before,
    LessonPageResponse after,
    List<String> summaryItems) {}
