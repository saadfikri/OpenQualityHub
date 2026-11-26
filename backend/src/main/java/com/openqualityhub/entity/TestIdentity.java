package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "test_identities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 255)
    private String fingerprint;

    @Column(name = "class_name", length = 500)
    private String className;

    @Column(name = "test_name", nullable = false, length = 500)
    private String testName;

    @Column(name = "suite_name", length = 500)
    private String suiteName;

    @Column(name = "first_seen_at", nullable = false)
    @Builder.Default
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private Instant lastSeenAt = Instant.now();

    @Column(name = "total_executions")
    @Builder.Default
    private int totalExecutions = 0;

    @Column(name = "total_passed")
    @Builder.Default
    private int totalPassed = 0;

    @Column(name = "total_failed")
    @Builder.Default
    private int totalFailed = 0;

    @Column(name = "total_skipped")
    @Builder.Default
    private int totalSkipped = 0;

    @Column(name = "flaky_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal flakyScore = BigDecimal.ZERO;

    @Column(name = "is_flaky")
    @Builder.Default
    private boolean flaky = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_classification", length = 50)
    @Builder.Default
    private FailureClassification failureClassification = FailureClassification.UNKNOWN;

    @Column(name = "last_status", length = 20)
    private String lastStatus;

    @Column(name = "avg_duration_ms")
    private Long avgDurationMs;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public enum FailureClassification {
        PRODUCT_DEFECT,
        INFRASTRUCTURE,
        TEST_INSTABILITY,
        UNKNOWN
    }

    public void recordExecution(TestCase.TestStatus status, Long durationMs) {
        totalExecutions++;
        lastStatus = status.name();
        lastSeenAt = Instant.now();
        updatedAt = Instant.now();

        switch (status) {
            case PASSED -> totalPassed++;
            case FAILED, ERROR -> totalFailed++;
            case SKIPPED -> totalSkipped++;
        }

        // Compute average duration with proper rounding (avoid Long truncation)
        if (durationMs != null && durationMs > 0) {
            avgDurationMs = avgDurationMs == null
                ? durationMs
                : Math.round((avgDurationMs * (totalExecutions - 1) + durationMs) / (double) totalExecutions);
        }

        // Compute flaky score using effective executions (exclude skipped)
        int effectiveExecutions = totalPassed + totalFailed;
        if (effectiveExecutions >= 3 && totalFailed > 0 && totalPassed > 0) {
            double minCount = Math.min(totalFailed, totalPassed);
            double score = Math.min(100.0, (minCount * 2.0 * 100.0) / effectiveExecutions);
            flakyScore = BigDecimal.valueOf(score).setScale(2, java.math.RoundingMode.HALF_UP);
            flaky = flakyScore.compareTo(BigDecimal.TEN) >= 0 && effectiveExecutions >= 5;
        } else {
            flakyScore = BigDecimal.ZERO;
            flaky = false;
        }
    }
}
