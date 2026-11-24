package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "releases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AttributeOverride(name = "version", column = @Column(name = "db_version"))
public class Release extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "version", nullable = false, length = 100)
    private String releaseVersion;

    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private ReleaseStatus status = ReleaseStatus.PLANNING;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @OneToMany(mappedBy = "release", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Build> builds = new ArrayList<>();

    public enum ReleaseStatus {
        PLANNING, IN_PROGRESS, READY_FOR_QA, QA_IN_PROGRESS, APPROVED, RELEASED, CANCELLED
    }
}
