package com.openqualityhub.service;

import com.openqualityhub.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MaestroService {

    TestRunResponse importMaestroResults(UUID projectId, String name, String environment, MultipartFile file);

    List<MaestroFlowResultResponse> getFlowResultsByTestRun(UUID testRunId);

    MaestroDeviceMatrixResponse getDeviceMatrix();

    MaestroFlowAnalyticsResponse getFlowAnalytics();

    MaestroDashboardResponse getDashboard();

    Page<MaestroFlowResultResponse> getFlowResults(String platform, Pageable pageable);
}
