package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "builds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Build extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private Release release;

    @Column(name = "build_number", nullable = false, length = 100)
    private String buildNumber;

    @Column(name = "commit_sha", length = 40)
    private String commitSha;

    @Column(length = 255)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private BuildStatus status = BuildStatus.IN_PROGRESS;

    @Column(name = "build_url", length = 500)
    private String buildUrl;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public enum BuildStatus {
        IN_PROGRESS, SUCCESS, FAILED, CANCELLED
    }
}
