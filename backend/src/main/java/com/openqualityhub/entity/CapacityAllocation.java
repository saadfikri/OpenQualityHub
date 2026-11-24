package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "capacity_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapacityAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "total_hours", nullable = false)
    @Builder.Default
    private BigDecimal totalHours = new BigDecimal("40.0");

    @Column(name = "allocated_hours", nullable = false)
    @Builder.Default
    private BigDecimal allocatedHours = BigDecimal.ZERO;

    @Column(name = "available_hours", insertable = false, updatable = false)
    private BigDecimal availableHours;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
