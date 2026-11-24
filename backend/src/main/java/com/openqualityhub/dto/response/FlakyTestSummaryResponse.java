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
public class FlakyTestSummaryResponse {
    private long totalTestIdentities;
    private long flakyTestCount;
    private BigDecimal flakyPercentage;
    private List<ClassificationCount> byClassification;
    private List<ProjectFlakyCount> byProject;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationCount {
        private String classification;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectFlakyCount {
        private String projectId;
        private String projectName;
        private long flakyCount;
        private long totalIdentities;
    }
}
