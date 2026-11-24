package com.openqualityhub.service;

import com.openqualityhub.dto.response.FlakyTestResponse;
import com.openqualityhub.dto.response.FlakyTestSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FlakyTestService {
    FlakyTestSummaryResponse getSummary();
    Page<FlakyTestResponse> getFlakyTests(UUID projectId, Pageable pageable);
    Page<FlakyTestResponse> getAllTestIdentities(UUID projectId, Pageable pageable);
    FlakyTestResponse getById(UUID id);
}
