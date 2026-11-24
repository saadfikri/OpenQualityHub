package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateTestDataProfileRequest;
import com.openqualityhub.dto.request.CreateTestDataTemplateRequest;
import com.openqualityhub.dto.request.GenerateTestDataRequest;
import com.openqualityhub.dto.request.UpdateTestDataProfileRequest;
import com.openqualityhub.dto.request.UpdateTestDataTemplateRequest;
import com.openqualityhub.dto.response.TestDataDashboardResponse;
import com.openqualityhub.dto.response.TestDataGenerationResponse;
import com.openqualityhub.dto.response.TestDataProfileResponse;
import com.openqualityhub.dto.response.TestDataTemplateDetailResponse;
import com.openqualityhub.dto.response.TestDataTemplateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TestDataService {

    TestDataTemplateResponse createTemplate(CreateTestDataTemplateRequest request);

    TestDataTemplateResponse updateTemplate(UUID id, UpdateTestDataTemplateRequest request);

    TestDataTemplateDetailResponse getTemplateById(UUID id);

    Page<TestDataTemplateResponse> getAllTemplates(UUID projectId, Pageable pageable);

    void deleteTemplate(UUID id);

    TestDataProfileResponse createProfile(CreateTestDataProfileRequest request);

    TestDataProfileResponse updateProfile(UUID id, UpdateTestDataProfileRequest request);

    void deleteProfile(UUID id);

    TestDataGenerationResponse generate(GenerateTestDataRequest request);

    TestDataGenerationResponse getGeneration(UUID id);

    Page<TestDataGenerationResponse> getGenerationsByTemplate(UUID templateId, Pageable pageable);

    byte[] downloadGeneration(UUID generationId);

    TestDataDashboardResponse getDashboard();
}
