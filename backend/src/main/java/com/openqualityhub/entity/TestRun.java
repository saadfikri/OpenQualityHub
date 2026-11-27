package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRun extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestSource source;

    private String environment;

    @Column(name = "total_tests", nullable = false)
    @Builder.Default
    private int totalTests = 0;

    @Column(nullable = false)
    @Builder.Default
    private int passed = 0;

    @Column(nullable = false)
    @Builder.Default
    private int failed = 0;

    @Column(nullable = false)
    @Builder.Default
    private int skipped = 0;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "pass_rate", insertable = false, updatable = false)
    private BigDecimal passRate;

    @Column(name = "executed_at", nullable = false)
    @Builder.Default
    private Instant executedAt = Instant.now();

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Column(length = 255)
    private String branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_id")
    private Build build;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    public enum TestSource {
        PLAYWRIGHT, JUNIT, WEBDRIVERIO, CYPRESS, TESTNG, MAESTRO
    }
}
