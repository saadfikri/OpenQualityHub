package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "quality_gate_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityGateEvaluation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_gate_id", nullable = false)
    private QualityGate qualityGate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EvaluationStatus status;

    @Column(name = "evaluated_at", nullable = false)
    @Builder.Default
    private Instant evaluatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluated_by")
    private User evaluatedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String evidence;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "override_by")
    private User overrideBy;

    @Column(name = "override_at")
    private Instant overrideAt;

    @Column(name = "override_expires_at")
    private Instant overrideExpiresAt;

    public enum EvaluationStatus {
        PASSED, FAILED, WARNING, INSUFFICIENT_DATA
    }
}
