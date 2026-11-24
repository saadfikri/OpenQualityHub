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
public class QaServiceRequestResponse {
    private String id;
    private String requestNumber;
    private String title;
    private String description;
    private String status;
    private String impact;
    private String urgency;
    private int priorityScore;
    private Integer priorityOverride;
    private int effectivePriority;
    private String overrideReason;

    private String qaServiceId;
    private String qaServiceName;
    private String qaServiceCategory;

    private String requestingTeamId;
    private String requestingTeamName;
    private String requestingUserId;
    private String requestingUserName;

    private String projectId;
    private String projectName;

    private String assignedTeamId;
    private String assignedTeamName;
    private String assignedUserId;
    private String assignedUserName;

    private BigDecimal estimatedHours;
    private BigDecimal actualHours;

    private String responseDeadline;
    private String resolutionDeadline;
    private String respondedAt;
    private String resolvedAt;
    private String createdAt;
    private String updatedAt;

    private List<ActivityResponse> activities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityResponse {
        private String id;
        private String userId;
        private String userName;
        private String activityType;
        private String oldValue;
        private String newValue;
        private String comment;
        private String createdAt;
    }
}
