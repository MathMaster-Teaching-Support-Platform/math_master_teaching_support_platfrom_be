package com.fptu.math_master.dto.response;

import com.fptu.math_master.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixPartResponse {

    private UUID id;
    private Integer partNumber;
    private QuestionType questionType;
    private String name;
}
