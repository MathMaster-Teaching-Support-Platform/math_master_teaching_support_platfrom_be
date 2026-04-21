package com.fptu.math_master.dto.request;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderLessonsRequest {
    private List<LessonOrder> orders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonOrder {
        private UUID lessonId;
        private int orderIndex;
    }
}
