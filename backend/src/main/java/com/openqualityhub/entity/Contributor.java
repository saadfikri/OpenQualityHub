package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contributors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contributor extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Provider provider;

    @Column(nullable = false, length = 255)
    private String username;

    @Column(name = "display_name", length = 500)
    private String displayName;

    @Column(length = 500)
    private String email;

    @Column(name = "avatar_url", length = 2000)
    private String avatarUrl;

    @Column(name = "total_commits", nullable = false)
    @Builder.Default
    private int totalCommits = 0;

    @Column(name = "total_pipelines", nullable = false)
    @Builder.Default
    private int totalPipelines = 0;

    @Column(name = "total_merges", nullable = false)
    @Builder.Default
    private int totalMerges = 0;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id")
    private User linkedUser;

    @OneToMany(mappedBy = "contributor", cascade = CascadeType.ALL)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<ContributorActivity> activities = new ArrayList<>();

    public enum Provider {
        GITLAB, GITHUB, JENKINS, GENERIC
    }
}
