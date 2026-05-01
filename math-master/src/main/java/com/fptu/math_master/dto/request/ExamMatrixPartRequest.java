package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixPartRequest {

    @NotNull(message = "Part number is required")
    @Min(value = 1, message = "Part number must be between 1 and 3")
    @Max(value = 3, message = "Part number must be between 1 and 3")
    private Integer partNumber;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    private String name; // Optional display name
}
