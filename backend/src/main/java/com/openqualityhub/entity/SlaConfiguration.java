package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaConfiguration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority_level", nullable = false)
    private int priorityLevel;

    @Column(name = "response_time_hours", nullable = false)
    private int responseTimeHours;

    @Column(name = "resolution_time_hours", nullable = false)
    private int resolutionTimeHours;

    @Column(name = "business_hours_only", nullable = false)
    @Builder.Default
    private boolean businessHoursOnly = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
