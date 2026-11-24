package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "failure_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureAnalysis extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 50)
    private FailureCategory failureCategory;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "suggested_fix", columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_data", columnDefinition = "jsonb")
    @Builder.Default
    private String analysisData = "{}";

    @Enumerated(EnumType.STRING)
    @Column(name = "human_feedback", nullable = false, length = 20)
    @Builder.Default
    private HumanFeedback humanFeedback = HumanFeedback.PENDING;

    @Column(name = "feedback_comment", columnDefinition = "TEXT")
    private String feedbackComment;

    @Column(name = "feedback_by")
    private String feedbackBy;

    @Column(name = "feedback_at")
    private Instant feedbackAt;

    public enum AnalysisStatus {
        PENDING, ANALYZING, COMPLETED, FAILED, SKIPPED
    }

    public enum FailureCategory {
        CODE_REGRESSION, FLAKY_TEST, ENVIRONMENT_ISSUE, DATA_ISSUE,
        CONFIGURATION_ERROR, DEPENDENCY_FAILURE, TIMEOUT, ASSERTION_MISMATCH, UNKNOWN
    }

    public enum HumanFeedback {
        PENDING, ACCEPTED, REJECTED, PARTIALLY_ACCEPTED
    }
}
