package com.fptu.math_master.dto.request;

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
public class SubmitAssessmentRequest {

    @NotNull(message = "Attempt ID is required")
    private UUID attemptId;

    private Boolean confirmed;
}

