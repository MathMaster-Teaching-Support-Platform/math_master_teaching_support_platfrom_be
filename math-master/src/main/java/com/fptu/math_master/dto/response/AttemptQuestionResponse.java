package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptQuestionResponse {

    private UUID questionId;
    private Integer orderIndex;
    private QuestionType questionType;
    private String questionText;
    private Map<String, Object> options;
    private BigDecimal points;
}
