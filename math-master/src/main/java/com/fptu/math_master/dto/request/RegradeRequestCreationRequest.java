package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegradeRequestCreationRequest {

    @NotNull(message = "Submission ID is required")
    private UUID submissionId;

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @NotBlank(message = "Reason is required")
    private String reason;
}

