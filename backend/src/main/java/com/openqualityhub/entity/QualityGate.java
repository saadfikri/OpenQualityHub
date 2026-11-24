package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quality_gates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityGate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultGate = false;

    @OneToMany(mappedBy = "qualityGate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<QualityGateCondition> conditions = new ArrayList<>();
}
