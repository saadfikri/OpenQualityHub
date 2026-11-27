package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.*;
import com.openqualityhub.entity.MaestroFlowResult;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.repository.MaestroFlowResultRepository;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.MaestroService;
import com.openqualityhub.service.impl.parser.MaestroJsonParser;
import com.openqualityhub.mapper.TestRunMapper;
import com.openqualityhub.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaestroServiceImpl implements MaestroService {

    private final MaestroJsonParser maestroParser;
    private final TestRunRepository testRunRepository;
    private final MaestroFlowResultRepository flowResultRepository;
    private final ProjectRepository projectRepository;
    private final TestRunMapper testRunMapper;
    private final TestIdentityService testIdentityService;

    @Override
    @Transactional
    public TestRunResponse importMaestroResults(UUID projectId, String name, String environment, MultipartFile file) {
        Project project = projectRepository.findByIdAndDeletedFalse(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        try {
            MaestroJsonParser.ParseResult result = maestroParser.parse(file.getInputStream());

            TestRun testRun = result.testRun();
            testRun.setName(name);
            testRun.setProject(project);
            testRun.setSource(TestRun.TestSource.MAESTRO);
            testRun.setEnvironment(environment);

            testRun = testRunRepository.save(testRun);

            if (!result.flowResults().isEmpty()) {
                for (MaestroFlowResult fr : result.flowResults()) {
                    fr.setTestRun(testRun);
                }
                flowResultRepository.saveAll(result.flowResults());
            }

            if (!testRun.getTestCases().isEmpty()) {
                testIdentityService.recordExecutions(testRun);
            }

            log.info("Imported Maestro results: {} ({} flows) for project {}",
                testRun.getId(), testRun.getTotalTests(), project.getName());
            return testRunMapper.toResponse(testRun);
        } catch (Exception e) {
            log.error("Failed to import Maestro results: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse Maestro JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaestroFlowResultResponse> getFlowResultsByTestRun(UUID testRunId) {
        return flowResultRepository.findByTestRunIdAndDeletedFalse(testRunId).stream()
            .map(this::toFlowResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MaestroDeviceMatrixResponse getDeviceMatrix() {
        // Device entries grouped by device+os+platform+status
        List<MaestroDeviceMatrixResponse.DeviceEntry> devices = new ArrayList<>();
        for (Object[] row : flowResultRepository.getDeviceMatrix()) {
            devices.add(MaestroDeviceMatrixResponse.DeviceEntry.builder()
                .deviceName(row[0] != null ? row[0].toString() : "Unknown")
                .osVersion(row[1] != null ? row[1].toString() : "Unknown")
                .platform(row[2] != null ? row[2].toString() : "Unknown")
                .status(row[3] != null ? row[3].toString() : "Unknown")
                .count(((Number) row[4]).longValue())
                .build());
        }

        // Platform summary
        List<MaestroDeviceMatrixResponse.PlatformSummary> platforms = new ArrayList<>();
        for (Object[] row : flowResultRepository.getPlatformSummary()) {
            long total = ((Number) row[1]).longValue();
            long passedCount = ((Number) row[2]).longValue();
            long failedCount = ((Number) row[3]).longValue();
            platforms.add(MaestroDeviceMatrixResponse.PlatformSummary.builder()
                .platform(row[0] != null ? row[0].toString() : "Unknown")
                .totalFlows(total)
                .passed(passedCount)
                .failed(failedCount)
                .passRate(total > 0 ? (passedCount * 100.0 / total) : 0)
                .build());
        }

        // App version summary
        List<MaestroDeviceMatrixResponse.AppVersionEntry> appVersions = new ArrayList<>();
        for (Object[] row : flowResultRepository.getAppVersionSummary()) {
            long total = ((Number) row[2]).longValue();
            long passedCount = ((Number) row[3]).longValue();
            appVersions.add(MaestroDeviceMatrixResponse.AppVersionEntry.builder()
                .appId(row[0] != null ? row[0].toString() : null)
                .appVersion(row[1] != null ? row[1].toString() : null)
                .totalFlows(total)
                .passed(passedCount)
                .passRate(total > 0 ? (passedCount * 100.0 / total) : 0)
                .build());
        }

        return MaestroDeviceMatrixResponse.builder()
            .devices(devices)
            .platforms(platforms)
            .appVersions(appVersions)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MaestroFlowAnalyticsResponse getFlowAnalytics() {
        long totalFlows = flowResultRepository.countByDeletedFalse();
        long totalPassed = flowResultRepository.countByStatusAndDeletedFalse(MaestroFlowResult.FlowStatus.PASSED);
        long totalFailed = flowResultRepository.countByStatusAndDeletedFalse(MaestroFlowResult.FlowStatus.FAILED);

        List<MaestroFlowAnalyticsResponse.FlowSummary> flows = new ArrayList<>();
        for (Object[] row : flowResultRepository.getFlowAnalytics()) {
            flows.add(MaestroFlowAnalyticsResponse.FlowSummary.builder()
                .flowName(row[0] != null ? row[0].toString() : "Unknown")
                .status(row[1] != null ? row[1].toString() : "Unknown")
                .count(((Number) row[2]).longValue())
                .avgDurationMs(row[3] != null ? ((Number) row[3]).doubleValue() : 0)
                .build());
        }

        List<MaestroFlowAnalyticsResponse.TopFailingFlow> topFailing = new ArrayList<>();
        for (Object[] row : flowResultRepository.getTopFailingFlows()) {
            topFailing.add(MaestroFlowAnalyticsResponse.TopFailingFlow.builder()
                .flowName(row[0] != null ? row[0].toString() : "Unknown")
                .failureCount(((Number) row[1]).longValue())
                .build());
        }

        return MaestroFlowAnalyticsResponse.builder()
            .totalFlows(totalFlows)
            .totalPassed(totalPassed)
            .totalFailed(totalFailed)
            .overallPassRate(totalFlows > 0 ? (totalPassed * 100.0 / totalFlows) : 0)
            .flows(flows)
            .topFailing(topFailing)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MaestroDashboardResponse getDashboard() {
        MaestroDeviceMatrixResponse deviceMatrix = getDeviceMatrix();
        MaestroFlowAnalyticsResponse flowAnalytics = getFlowAnalytics();

        // Reuse counts from flowAnalytics to avoid redundant DB queries
        long total = flowAnalytics.getTotalFlows();
        long passed = flowAnalytics.getTotalPassed();
        long failed = flowAnalytics.getTotalFailed();

        long uniqueDevices = deviceMatrix.getDevices().stream()
            .map(MaestroDeviceMatrixResponse.DeviceEntry::getDeviceName)
            .distinct().count();
        long uniqueFlows = flowAnalytics.getFlows().stream()
            .map(MaestroFlowAnalyticsResponse.FlowSummary::getFlowName)
            .distinct().count();

        return MaestroDashboardResponse.builder()
            .totalFlows(total)
            .passedFlows(passed)
            .failedFlows(failed)
            .passRate(total > 0 ? (passed * 100.0 / total) : 0)
            .uniqueDevices(uniqueDevices)
            .uniqueFlowNames(uniqueFlows)
            .deviceMatrix(deviceMatrix)
            .flowAnalytics(flowAnalytics)
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaestroFlowResultResponse> getFlowResults(String platform, Pageable pageable) {
        Page<MaestroFlowResult> page;
        if (platform != null) {
            try {
                MaestroFlowResult.DevicePlatform dp = MaestroFlowResult.DevicePlatform.valueOf(platform.toUpperCase());
                page = flowResultRepository.findByDevicePlatformAndDeletedFalse(dp, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid platform: " + platform + ". Valid values: IOS, ANDROID, WEB");
            }
        } else {
            page = flowResultRepository.findByDeletedFalse(pageable);
        }
        return page.map(this::toFlowResponse);
    }

    private MaestroFlowResultResponse toFlowResponse(MaestroFlowResult fr) {
        return MaestroFlowResultResponse.builder()
            .id(fr.getId().toString())
            .testRunId(fr.getTestRun().getId().toString())
            .flowName(fr.getFlowName())
            .flowFile(fr.getFlowFile())
            .deviceName(fr.getDeviceName())
            .devicePlatform(fr.getDevicePlatform() != null ? fr.getDevicePlatform().name() : null)
            .osVersion(fr.getOsVersion())
            .deviceModel(fr.getDeviceModel())
            .deviceLocale(fr.getDeviceLocale())
            .appId(fr.getAppId())
            .appVersion(fr.getAppVersion())
            .screenshotUrl(fr.getScreenshotUrl())
            .videoUrl(fr.getVideoUrl())
            .durationMs(fr.getDurationMs())
            .status(fr.getStatus().name())
            .errorMessage(fr.getErrorMessage())
            .retryCount(fr.getRetryCount())
            .createdAt(fr.getCreatedAt() != null ? fr.getCreatedAt().toString() : null)
            .build();
    }
}
