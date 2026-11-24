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
public class IntegrationDashboardResponse {

    private long totalIntegrations;
    private long activeIntegrations;
    private long totalEventsProcessed;
    private long totalEventsFailed;
    private List<ProviderCount> byProvider;
    private List<EventStatusCount> eventsByStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderCount {
        private String provider;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventStatusCount {
        private String status;
        private long count;
    }
}
