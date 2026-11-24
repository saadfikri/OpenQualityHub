package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "test_data_generations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDataGeneration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private TestDataTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private TestDataProfile profile;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 10)
    @Builder.Default
    private ExportFormat exportFormat = ExportFormat.JSON;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_data", columnDefinition = "jsonb")
    private String generatedData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public enum GenerationStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    public enum ExportFormat {
        JSON, CSV
    }
}
