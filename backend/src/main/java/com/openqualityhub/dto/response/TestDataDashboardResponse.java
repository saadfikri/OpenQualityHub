package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDataDashboardResponse {

    private long totalTemplates;
    private long totalProfiles;
    private long totalGenerations;
    private List<TestDataGenerationResponse> recentGenerations;
}
