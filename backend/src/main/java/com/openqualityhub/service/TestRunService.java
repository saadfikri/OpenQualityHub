package com.openqualityhub.service;

import com.openqualityhub.dto.response.TestRunDetailResponse;
import com.openqualityhub.dto.response.TestRunResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface TestRunService {

    TestRunResponse importJUnitResults(UUID projectId, String name, String environment, MultipartFile file);

    TestRunResponse importPlaywrightResults(UUID projectId, String name, String environment, MultipartFile file);

    TestRunResponse importWebdriverIOResults(UUID projectId, String name, String environment, MultipartFile file);

    TestRunResponse importCypressResults(UUID projectId, String name, String environment, MultipartFile file);

    TestRunResponse importTestNGResults(UUID projectId, String name, String environment, MultipartFile file);

    TestRunResponse importMaestroResults(UUID projectId, String name, String environment, MultipartFile file);

    TestRunDetailResponse getById(UUID id);

    Page<TestRunResponse> getByProjectId(UUID projectId, Pageable pageable);

    Page<TestRunResponse> getAll(Pageable pageable);

    void delete(UUID id);
}
