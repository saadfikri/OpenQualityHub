package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "qa_service_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QaServiceRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "request_number", nullable = false, length = 20)
    private String requestNumber;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qa_service_id", nullable = false)
    private QaService qaService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_team_id", nullable = false)
    private Team requestingTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_user_id", nullable = false)
    private User requestingUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private Team assignedTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RequestStatus status = RequestStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Impact impact = Impact.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Urgency urgency = Urgency.MEDIUM;

    @Column(name = "priority_score", nullable = false)
    @Builder.Default
    private int priorityScore = 3;

    @Column(name = "priority_override")
    private Integer priorityOverride;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_configuration_id")
    private SlaConfiguration slaConfiguration;

    @Column(name = "response_deadline")
    private Instant responseDeadline;

    @Column(name = "resolution_deadline")
    private Instant resolutionDeadline;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "estimated_hours")
    private BigDecimal estimatedHours;

    @Column(name = "actual_hours")
    private BigDecimal actualHours;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<QaRequestActivity> activities = new ArrayList<>();

    public int getEffectivePriority() {
        return priorityOverride != null ? priorityOverride : priorityScore;
    }

    public enum RequestStatus {
        SUBMITTED, TRIAGED, ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED, CLOSED, CANCELLED;

        private static final Map<RequestStatus, Set<RequestStatus>> VALID_TRANSITIONS = Map.of(
            SUBMITTED, Set.of(TRIAGED, CANCELLED),
            TRIAGED, Set.of(ASSIGNED, CANCELLED),
            ASSIGNED, Set.of(IN_PROGRESS, ON_HOLD, CANCELLED),
            IN_PROGRESS, Set.of(ON_HOLD, COMPLETED, CANCELLED),
            ON_HOLD, Set.of(IN_PROGRESS, CANCELLED),
            COMPLETED, Set.of(CLOSED, IN_PROGRESS),
            CLOSED, Set.of(),
            CANCELLED, Set.of()
        );

        public boolean canTransitionTo(RequestStatus target) {
            return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
        }
    }

    public enum Impact {
        CRITICAL, HIGH, MEDIUM, LOW
    }

    public enum Urgency {
        IMMEDIATE, HIGH, MEDIUM, LOW
    }
}
