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
public class StartAssessmentRequest {

    @NotNull(message = "Assessment ID is required")
    private UUID assessmentId;

    private String ipAddress;
}

