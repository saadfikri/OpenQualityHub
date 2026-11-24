package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "qa_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QaService extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceCategory category;

    @Column(name = "estimated_hours")
    private BigDecimal estimatedHours;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    public enum ServiceCategory {
        TEST_PLANNING,
        TEST_EXECUTION,
        TEST_AUTOMATION,
        PERFORMANCE_TESTING,
        SECURITY_TESTING,
        ACCESSIBILITY_TESTING,
        REGRESSION_TESTING,
        EXPLORATORY_TESTING,
        TEST_ENVIRONMENT_SETUP,
        CONSULTATION
    }
}
