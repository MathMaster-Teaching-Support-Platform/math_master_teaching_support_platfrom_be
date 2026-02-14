package com.fptu.math_master.dto.request;

import com.fptu.math_master.enums.RegradeRequestStatus;
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
public class RegradeResponseRequest {

    @NotNull(message = "Request ID is required")
    private UUID requestId;

    @NotNull(message = "Status is required")
    private RegradeRequestStatus status;

    private String teacherResponse;
}

