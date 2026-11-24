package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "defects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Defect extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private DefectStatus status = DefectStatus.OPEN;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 3;

    private String component;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_url", length = 500)
    private String externalUrl;

    public enum Severity {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    public enum DefectStatus {
        OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED
    }
}
