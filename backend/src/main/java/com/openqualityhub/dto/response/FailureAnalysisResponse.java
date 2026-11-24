package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureAnalysisResponse {

    private UUID id;
    private UUID testRunId;
    private String testRunName;
    private String status;
    private String failureCategory;
    private String summary;
    private String rootCause;
    private String suggestedFix;
    private BigDecimal confidenceScore;
    private String aiModel;
    private Integer promptTokens;
    private Integer completionTokens;
    private String humanFeedback;
    private String feedbackComment;
    private String feedbackBy;
    private Instant feedbackAt;
    private Instant createdAt;
}
