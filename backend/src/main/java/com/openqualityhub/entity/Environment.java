package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "environments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Environment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "env_type", nullable = false, length = 50)
    private EnvironmentType envType;

    @Column(length = 500)
    private String url;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    public enum EnvironmentType {
        DEVELOPMENT, INTEGRATION, STAGING, PRE_PRODUCTION, PRODUCTION
    }
}
