package com.fptu.math_master.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class ManualGradeRequest {

    @NotNull(message = "Answer ID is required")
    private UUID answerId;

    @NotNull(message = "Points earned is required")
    @DecimalMin(value = "0.0", message = "Points earned must be at least 0")
    private BigDecimal pointsEarned;

    private String feedback;
}

