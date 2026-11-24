package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documentation_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentationAsset extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 50)
    private DocType docType;

    @Column(length = 2000)
    private String url;

    @Column(name = "repo_path", length = 1000)
    private String repoPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 20)
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "review_cycle_days", nullable = false)
    @Builder.Default
    private int reviewCycleDays = 90;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("executedAt DESC")
    @Builder.Default
    private List<DocumentationCheck> checks = new ArrayList<>();

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<DocumentationReview> reviews = new ArrayList<>();

    public enum DocType {
        API_SPEC, USER_GUIDE, RUNBOOK, ARCHITECTURE, TEST_PLAN,
        RELEASE_NOTES, ONBOARDING, POLICY, WIKI, OTHER
    }

    public enum Category {
        TECHNICAL, PROCESS, USER_FACING, COMPLIANCE, INTERNAL
    }

    public enum HealthStatus {
        HEALTHY, WARNING, CRITICAL, UNKNOWN
    }
}
