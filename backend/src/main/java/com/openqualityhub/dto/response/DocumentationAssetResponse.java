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
public class DocumentationAssetResponse {
    private String id;
    private String title;
    private String description;
    private String docType;
    private String url;
    private String repoPath;
    private String category;
    private String ownerId;
    private String ownerName;
    private String projectId;
    private String projectName;
    private String healthStatus;
    private String lastCheckedAt;
    private String lastReviewedAt;
    private int reviewCycleDays;
    private String createdAt;
    private String updatedAt;
    private List<CheckResponse> recentChecks;
    private List<ReviewResponse> recentReviews;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckResponse {
        private String id;
        private String checkType;
        private String status;
        private String message;
        private String executedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewResponse {
        private String id;
        private String reviewerId;
        private String reviewerName;
        private String status;
        private String dueDate;
        private String completedAt;
        private String comment;
        private String createdAt;
    }
}
