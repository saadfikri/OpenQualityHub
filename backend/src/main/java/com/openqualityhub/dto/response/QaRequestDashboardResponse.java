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
public class QaRequestDashboardResponse {

    private long totalRequests;
    private long openRequests;
    private long inProgressRequests;
    private long completedRequests;
    private long cancelledRequests;
    private long slaBreachCount;
    private List<StatusCount> requestsByStatus;
    private List<PriorityCount> requestsByPriority;
    private List<ServiceCount> requestsByService;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private String status;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriorityCount {
        private int priority;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceCount {
        private String serviceName;
        private String category;
        private long count;
    }
}
