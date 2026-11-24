package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlakyTestResponse {
    private UUID id;
    private UUID projectId;
    private String projectName;
    private String fingerprint;
    private String className;
    private String testName;
    private String suiteName;
    private int totalExecutions;
    private int totalPassed;
    private int totalFailed;
    private int totalSkipped;
    private BigDecimal flakyScore;
    private boolean flaky;
    private String failureClassification;
    private String lastStatus;
    private Long avgDurationMs;
    private Instant firstSeenAt;
    private Instant lastSeenAt;
}
