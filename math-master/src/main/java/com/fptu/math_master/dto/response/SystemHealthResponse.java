package com.fptu.math_master.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {
    private String status; // "healthy", "warning", "critical"
    private List<Alert> alerts;
    private Metrics metrics;
    private GatewayStatus gatewayStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Alert {
        private String severity; // "info", "warning", "critical"
        private String message;
        private String timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metrics {
        private long totalTransactions24h;
        private double successRate;
        private double avgProcessingTimeMs;
        private long failedTransactions24h;
        private long pendingTransactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewayStatus {
        private String payosStatus; // "operational", "degraded", "down"
        private String lastWebhook;
        private double webhookSuccessRate;
    }
}
