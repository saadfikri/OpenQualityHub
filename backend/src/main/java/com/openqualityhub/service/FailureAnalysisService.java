package com.openqualityhub.service;

import com.openqualityhub.dto.request.SubmitFeedbackRequest;
import com.openqualityhub.dto.response.FailureAnalysisResponse;

import java.util.UUID;

public interface FailureAnalysisService {

    FailureAnalysisResponse triggerAnalysis(UUID testRunId);

    FailureAnalysisResponse getByTestRunId(UUID testRunId);

    FailureAnalysisResponse getById(UUID id);

    FailureAnalysisResponse submitFeedback(UUID analysisId, SubmitFeedbackRequest request);

    boolean isAvailable();
}
