package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.*;
import com.openqualityhub.entity.MaestroFlowResult;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.TestRun;
import com.openqualityhub.mapper.TestRunMapper;
import com.openqualityhub.repository.MaestroFlowResultRepository;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.impl.parser.MaestroJsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaestroServiceImplTest {

    @Mock private MaestroJsonParser maestroParser;
    @Mock private TestRunRepository testRunRepository;
    @Mock private MaestroFlowResultRepository flowResultRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TestRunMapper testRunMapper;
    @Mock private TestIdentityService testIdentityService;

    @InjectMocks
    private MaestroServiceImpl maestroService;

    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = Project.builder().name("Mobile App").build();
        project.setId(projectId);
    }

    @Test
    void getFlowResultsByTestRun_returnsFlowResponses() {
        UUID testRunId = UUID.randomUUID();
        TestRun testRun = TestRun.builder().name("Run").source(TestRun.TestSource.MAESTRO).build();
        testRun.setId(testRunId);

        MaestroFlowResult fr = MaestroFlowResult.builder()
            .flowName("Login Flow")
            .status(MaestroFlowResult.FlowStatus.PASSED)
            .devicePlatform(MaestroFlowResult.DevicePlatform.IOS)
            .deviceName("iPhone 15")
            .osVersion("17.2")
            .durationMs(3000L)
            .testRun(testRun)
            .build();
        fr.setId(UUID.randomUUID());

        when(flowResultRepository.findByTestRunIdAndDeletedFalse(testRunId)).thenReturn(List.of(fr));

        List<MaestroFlowResultResponse> results = maestroService.getFlowResultsByTestRun(testRunId);

        assertEquals(1, results.size());
        assertEquals("Login Flow", results.get(0).getFlowName());
        assertEquals("IOS", results.get(0).getDevicePlatform());
        assertEquals("iPhone 15", results.get(0).getDeviceName());
        assertEquals("PASSED", results.get(0).getStatus());
    }

    @Test
    void getDeviceMatrix_returnsPlatformsAndDevices() {
        Object[] deviceRow = {"iPhone 15", "17.2", "IOS", "PASSED", 10L};
        when(flowResultRepository.getDeviceMatrix()).thenReturn(List.<Object[]>of(deviceRow));

        Object[] platformRow = {"IOS", 20L, 18L, 2L};
        when(flowResultRepository.getPlatformSummary()).thenReturn(List.<Object[]>of(platformRow));

        Object[] appRow = {"com.example.app", "2.1.0", 15L, 14L};
        when(flowResultRepository.getAppVersionSummary()).thenReturn(List.<Object[]>of(appRow));

        MaestroDeviceMatrixResponse matrix = maestroService.getDeviceMatrix();

        assertEquals(1, matrix.getDevices().size());
        assertEquals("iPhone 15", matrix.getDevices().get(0).getDeviceName());
        assertEquals(10, matrix.getDevices().get(0).getCount());

        assertEquals(1, matrix.getPlatforms().size());
        assertEquals(90.0, matrix.getPlatforms().get(0).getPassRate());

        assertEquals(1, matrix.getAppVersions().size());
        assertEquals("2.1.0", matrix.getAppVersions().get(0).getAppVersion());
    }

    @Test
    void getFlowAnalytics_returnsFlowSummaryAndTopFailing() {
        when(flowResultRepository.countByDeletedFalse()).thenReturn(100L);
        when(flowResultRepository.countByStatusAndDeletedFalse(MaestroFlowResult.FlowStatus.PASSED)).thenReturn(85L);
        when(flowResultRepository.countByStatusAndDeletedFalse(MaestroFlowResult.FlowStatus.FAILED)).thenReturn(15L);

        Object[] flowRow = {"Login Flow", "PASSED", 50L, 2500.0};
        when(flowResultRepository.getFlowAnalytics()).thenReturn(List.<Object[]>of(flowRow));

        Object[] failRow = {"Checkout Flow", 8L};
        when(flowResultRepository.getTopFailingFlows()).thenReturn(List.<Object[]>of(failRow));

        MaestroFlowAnalyticsResponse analytics = maestroService.getFlowAnalytics();

        assertEquals(100, analytics.getTotalFlows());
        assertEquals(85, analytics.getTotalPassed());
        assertEquals(15, analytics.getTotalFailed());
        assertEquals(85.0, analytics.getOverallPassRate());
        assertEquals(1, analytics.getFlows().size());
        assertEquals("Login Flow", analytics.getFlows().get(0).getFlowName());
        assertEquals(1, analytics.getTopFailing().size());
        assertEquals("Checkout Flow", analytics.getTopFailing().get(0).getFlowName());
    }

    @Test
    void getDashboard_combinesAllMetrics() {
        when(flowResultRepository.countByDeletedFalse()).thenReturn(50L);
        when(flowResultRepository.countByStatusAndDeletedFalse(MaestroFlowResult.FlowStatus.PASSED)).thenReturn(40L);
        when(flowResultRepository.countByStatusAndDeletedFalse(MaestroFlowResult.FlowStatus.FAILED)).thenReturn(10L);
        when(flowResultRepository.getDeviceMatrix()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getPlatformSummary()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getAppVersionSummary()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getFlowAnalytics()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getTopFailingFlows()).thenReturn(Collections.emptyList());

        MaestroDashboardResponse dashboard = maestroService.getDashboard();

        assertEquals(50, dashboard.getTotalFlows());
        assertEquals(40, dashboard.getPassedFlows());
        assertEquals(10, dashboard.getFailedFlows());
        assertEquals(80.0, dashboard.getPassRate());
        assertNotNull(dashboard.getDeviceMatrix());
        assertNotNull(dashboard.getFlowAnalytics());
    }

    @Test
    void getFlowResults_withPlatformFilter() {
        Page<MaestroFlowResult> empty = new PageImpl<>(Collections.emptyList());
        when(flowResultRepository.findByDevicePlatformAndDeletedFalse(eq(MaestroFlowResult.DevicePlatform.IOS), any(Pageable.class)))
            .thenReturn(empty);

        Page<MaestroFlowResultResponse> results = maestroService.getFlowResults("ios", PageRequest.of(0, 20));

        assertNotNull(results);
        verify(flowResultRepository).findByDevicePlatformAndDeletedFalse(eq(MaestroFlowResult.DevicePlatform.IOS), any(Pageable.class));
    }

    @Test
    void getFlowResults_withoutPlatform_getsAll() {
        Page<MaestroFlowResult> empty = new PageImpl<>(Collections.emptyList());
        when(flowResultRepository.findByDeletedFalse(any(Pageable.class))).thenReturn(empty);

        Page<MaestroFlowResultResponse> results = maestroService.getFlowResults(null, PageRequest.of(0, 20));

        assertNotNull(results);
        verify(flowResultRepository).findByDeletedFalse(any(Pageable.class));
    }

    @Test
    void getDashboard_emptyData_handledGracefully() {
        when(flowResultRepository.countByDeletedFalse()).thenReturn(0L);
        when(flowResultRepository.countByStatusAndDeletedFalse(any())).thenReturn(0L);
        when(flowResultRepository.getDeviceMatrix()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getPlatformSummary()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getAppVersionSummary()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getFlowAnalytics()).thenReturn(Collections.emptyList());
        when(flowResultRepository.getTopFailingFlows()).thenReturn(Collections.emptyList());

        MaestroDashboardResponse dashboard = maestroService.getDashboard();

        assertEquals(0, dashboard.getTotalFlows());
        assertEquals(0.0, dashboard.getPassRate());
    }
}
