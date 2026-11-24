package com.openqualityhub.dto.response;

import com.openqualityhub.entity.TestRun.TestSource;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class TestRunResponse {
    private UUID id;
    private String name;
    private TestSource source;
    private String environment;
    private int totalTests;
    private int passed;
    private int failed;
    private int skipped;
    private Long durationMs;
    private BigDecimal passRate;
    private String commitSha;
    private String branch;
    private Instant executedAt;
    private UUID projectId;
    private String projectName;
    private Instant createdAt;
}
