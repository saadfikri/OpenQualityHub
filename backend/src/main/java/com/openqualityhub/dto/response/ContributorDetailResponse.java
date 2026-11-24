package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContributorDetailResponse {

    private ContributorResponse contributor;
    private List<ActivityBreakdown> activityBreakdown;
    private List<ProjectBreakdown> projects;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityBreakdown {
        private String activityType;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectBreakdown {
        private UUID projectId;
        private String projectName;
    }
}
