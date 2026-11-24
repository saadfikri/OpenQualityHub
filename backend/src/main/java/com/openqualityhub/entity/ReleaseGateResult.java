package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "release_gate_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseGateResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private ReleaseReadinessReport report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id", nullable = false)
    private QualityGateCondition condition;

    @Column(name = "metric_key", nullable = false, length = 100)
    private String metricKey;

    @Column(name = "actual_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "expected_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QualityGateCondition.Operator operator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ResultStatus status;

    @Column(name = "is_critical", nullable = false)
    private boolean critical;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum ResultStatus {
        PASS, FAIL
    }
}
