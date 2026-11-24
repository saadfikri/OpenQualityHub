package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "integration_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConfig extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Provider provider;

    @Column(name = "webhook_secret", length = 500)
    private String webhookSecret;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String config = "{}";

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Column(name = "event_count", nullable = false)
    @Builder.Default
    private long eventCount = 0;

    @OneToMany(mappedBy = "integration", cascade = CascadeType.ALL)
    @OrderBy("receivedAt DESC")
    @Builder.Default
    private List<WebhookEvent> events = new ArrayList<>();

    public enum Provider {
        GITHUB, GITLAB, JENKINS, JIRA, GENERIC
    }
}
