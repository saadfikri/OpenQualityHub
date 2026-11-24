package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "quality_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "test_reliability_score", precision = 5, scale = 2)
    private BigDecimal testReliabilityScore;

    @Column(name = "defect_score", precision = 5, scale = 2)
    private BigDecimal defectScore;

    @Column(name = "risk_coverage_score", precision = 5, scale = 2)
    private BigDecimal riskCoverageScore;

    @Column(name = "automation_score", precision = 5, scale = 2)
    private BigDecimal automationScore;

    @Column(name = "gate_compliance_score", precision = 5, scale = 2)
    private BigDecimal gateComplianceScore;

    @Column(name = "calculated_at", nullable = false)
    @Builder.Default
    private Instant calculatedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String weights = "{\"testReliability\":0.30,\"defect\":0.25,\"riskCoverage\":0.20,\"automation\":0.15,\"gateCompliance\":0.10}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String details = "{}";
}
