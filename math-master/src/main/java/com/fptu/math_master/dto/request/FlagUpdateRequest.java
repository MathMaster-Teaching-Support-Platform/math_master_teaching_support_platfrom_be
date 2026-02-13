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
public class FlagUpdateRequest {

    @NotNull(message = "Attempt ID is required")
    private UUID attemptId;

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @NotNull(message = "Flag status is required")
    private Boolean flagged;
}

