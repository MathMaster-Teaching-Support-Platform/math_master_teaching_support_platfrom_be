package com.fptu.math_master.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowChartPointResponse {
    /** Day (YYYY-MM-DD), week (YYYY-MM-DD of Monday), or month (YYYY-MM). */
    private String label;
    private BigDecimal inflow;
    private BigDecimal outflow;
    private BigDecimal net;
}
