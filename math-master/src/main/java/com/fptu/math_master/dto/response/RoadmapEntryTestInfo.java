package com.fptu.math_master.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapEntryTestInfo {
    private UUID assessmentId;
    private String name;
    private String description;
    private Integer totalQuestions;
    private String studentStatus;
    private Boolean canStart;
    private String cannotStartReason;
    private UUID activeAttemptId;
}
