package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaestroDashboardResponse {
    private long totalFlows;
    private long passedFlows;
    private long failedFlows;
    private double passRate;
    private long uniqueDevices;
    private long uniqueFlowNames;
    private MaestroDeviceMatrixResponse deviceMatrix;
    private MaestroFlowAnalyticsResponse flowAnalytics;
}
