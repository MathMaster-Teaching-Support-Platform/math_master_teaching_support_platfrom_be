package com.fptu.math_master.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteGradingRequest {

    @NotNull(message = "Submission ID is required")
    private UUID submissionId;

    @NotEmpty(message = "At least one answer grade is required")
    @Valid
    private List<ManualGradeRequest> grades;
}

