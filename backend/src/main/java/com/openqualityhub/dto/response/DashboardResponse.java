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
public class DashboardResponse {

    private SummaryStats summary;
    private List<PassRateTrend> passRateTrend;
    private List<ProjectHealth> projectHealth;
    private List<DefectSummary> defectsByStatus;
    private List<DefectSummary> defectsBySeverity;
    private List<TestRunSummary> recentTestRuns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryStats {
        private long totalTeams;
        private long totalProjects;
        private long totalTestRuns;
        private long totalTestCases;
        private long totalDefects;
        private long openDefects;
        private BigDecimal averagePassRate;
        private BigDecimal averageQualityScore;
        private long flakyTestCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassRateTrend {
        private String date;
        private BigDecimal passRate;
        private long totalTests;
        private long passed;
        private long failed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectHealth {
        private String projectId;
        private String projectName;
        private String teamName;
        private BigDecimal passRate;
        private BigDecimal qualityScore;
        private long openDefects;
        private long totalTestRuns;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectSummary {
        private String label;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestRunSummary {
        private String id;
        private String name;
        private String projectName;
        private String source;
        private int totalTests;
        private int passed;
        private int failed;
        private int skipped;
        private BigDecimal passRate;
        private String executedAt;
    }
}
