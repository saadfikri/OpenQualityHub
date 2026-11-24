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
public class DocumentationDashboardResponse {
    private long totalAssets;
    private long healthyCount;
    private long warningCount;
    private long criticalCount;
    private long unknownCount;
    private long missingOwnerCount;
    private long pendingReviews;
    private List<TypeCount> assetsByType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeCount {
        private String type;
        private long count;
    }
}
