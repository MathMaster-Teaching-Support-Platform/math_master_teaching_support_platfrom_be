package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAdjustmentRequest {

    @NotNull(message = "Submission ID is required")
    private UUID submissionId;

    @NotNull(message = "Adjustment amount is required")
    private BigDecimal adjustmentAmount;

    @NotNull(message = "Reason is required")
    private String reason;
}

