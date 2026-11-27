package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaestroFlowAnalyticsResponse {

    private long totalFlows;
    private long totalPassed;
    private long totalFailed;
    private double overallPassRate;
    private List<FlowSummary> flows;
    private List<TopFailingFlow> topFailing;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlowSummary {
        private String flowName;
        private String status;
        private long count;
        private double avgDurationMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopFailingFlow {
        private String flowName;
        private long failureCount;
    }
}
