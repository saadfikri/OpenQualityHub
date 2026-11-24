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
public class CapacityOverviewResponse {

    private List<TeamCapacity> teams;
    private BigDecimal totalAvailableHours;
    private BigDecimal totalAllocatedHours;
    private long totalActiveRequests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamCapacity {
        private String teamId;
        private String teamName;
        private BigDecimal totalHours;
        private BigDecimal allocatedHours;
        private BigDecimal availableHours;
        private long activeRequests;
        private BigDecimal estimatedWorkload;
        private String utilizationLevel;
    }
}
