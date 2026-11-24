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
public class ReleaseDashboardResponse {
    private List<ReleaseReadinessSummary> activeReleases;
    private long totalActiveReleases;
    private long passingReleases;
    private long warningReleases;
    private long failingReleases;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseReadinessSummary {
        private String releaseId;
        private String releaseName;
        private String releaseVersion;
        private String projectName;
        private String releaseStatus;
        private String targetDate;
        private BigDecimal overallScore;
        private String gateStatus;
        private long approvedCount;
        private long rejectedCount;
        private String lastEvaluatedAt;
    }
}
