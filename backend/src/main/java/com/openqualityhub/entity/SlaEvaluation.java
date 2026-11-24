package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sla_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private QaServiceRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_configuration_id", nullable = false)
    private SlaConfiguration slaConfiguration;

    @Column(name = "response_met")
    private Boolean responseMet;

    @Column(name = "resolution_met")
    private Boolean resolutionMet;

    @Column(name = "response_breach_minutes")
    private Long responseBreachMinutes;

    @Column(name = "resolution_breach_minutes")
    private Long resolutionBreachMinutes;

    @Column(name = "evaluated_at", nullable = false)
    @Builder.Default
    private Instant evaluatedAt = Instant.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
