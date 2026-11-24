package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadinessReportResponse {
    private String id;
    private String releaseId;
    private String releaseName;
    private String releaseVersion;
    private BigDecimal overallScore;
    private String gateStatus;
    private String summary;
    private String generatedAt;
    private String generatedByName;
    private List<GateResultItem> gateResults;
    private MetricsSnapshot metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateResultItem {
        private String id;
        private String conditionId;
        private String metricKey;
        private BigDecimal actualValue;
        private BigDecimal expectedValue;
        private String operator;
        private String status;
        private boolean critical;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsSnapshot {
        private BigDecimal passRate;
        private long totalTestRuns;
        private long openDefects;
        private long criticalDefects;
        private long highDefects;
        private long totalBuilds;
        private long successfulBuilds;
        private BigDecimal buildSuccessRate;
    }
}
