package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.DefectTrendResponse;
import com.openqualityhub.dto.response.PassRateTrendResponse;
import com.openqualityhub.dto.response.TrendPointResponse;
import com.openqualityhub.repository.DefectRepository;
import com.openqualityhub.repository.TestIdentityRepository;
import com.openqualityhub.repository.TestRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrendServiceImplTest {

    @Mock private TestRunRepository testRunRepository;
    @Mock private DefectRepository defectRepository;
    @Mock private TestIdentityRepository testIdentityRepository;

    @InjectMocks
    private TrendServiceImpl trendService;

    /** Helper to create a typed List<Object[]> avoiding varargs ambiguity. */
    private static List<Object[]> rows(Object[]... items) {
        List<Object[]> list = new ArrayList<>();
        Collections.addAll(list, items);
        return list;
    }

    // --- getPassRateTrend ---

    @Test
    void getPassRateTrend_withoutProjectId_callsGlobalQuery() {
        LocalDate date = LocalDate.of(2026, 11, 1);
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class)))
                .thenReturn(rows(new Object[]{date, new BigDecimal("95.50"), 100}));

        PassRateTrendResponse response = trendService.getPassRateTrend(30, null);

        assertNotNull(response);
        assertEquals(1, response.getDataPoints().size());
        assertEquals(date, response.getDataPoints().get(0).getDate());
        assertEquals(new BigDecimal("95.50"), response.getDataPoints().get(0).getPassRate());
        assertEquals(100, response.getDataPoints().get(0).getTotalTests());

        verify(testRunRepository).findDailyPassRateTrend(any(Instant.class));
        verify(testRunRepository, never()).findDailyPassRateTrendByProject(any(), any());
    }

    @Test
    void getPassRateTrend_withProjectId_callsProjectQuery() {
        UUID projectId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 10, 15);
        when(testRunRepository.findDailyPassRateTrendByProject(any(Instant.class), eq(projectId)))
                .thenReturn(rows(new Object[]{date, 87.3, 50}));

        PassRateTrendResponse response = trendService.getPassRateTrend(7, projectId);

        assertEquals(1, response.getDataPoints().size());
        assertEquals(new BigDecimal("87.30"), response.getDataPoints().get(0).getPassRate());
        verify(testRunRepository).findDailyPassRateTrendByProject(any(Instant.class), eq(projectId));
    }

    @Test
    void getPassRateTrend_emptyResult_returnsEmptyList() {
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        PassRateTrendResponse response = trendService.getPassRateTrend(30, null);

        assertNotNull(response);
        assertTrue(response.getDataPoints().isEmpty());
    }

    @Test
    void getPassRateTrend_sqlDateValue_convertsCorrectly() {
        Date sqlDate = Date.valueOf(LocalDate.of(2026, 8, 20));
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class)))
                .thenReturn(rows(new Object[]{sqlDate, null, null}));

        PassRateTrendResponse response = trendService.getPassRateTrend(30, null);

        assertEquals(LocalDate.of(2026, 8, 20), response.getDataPoints().get(0).getDate());
        assertEquals(BigDecimal.ZERO, response.getDataPoints().get(0).getPassRate());
        assertEquals(0, response.getDataPoints().get(0).getTotalTests());
    }

    @Test
    void getPassRateTrend_stringDateValue_parsesCorrectly() {
        when(testRunRepository.findDailyPassRateTrend(any(Instant.class)))
                .thenReturn(rows(new Object[]{"2026-06-15", new BigDecimal("100.00"), 10}));

        PassRateTrendResponse response = trendService.getPassRateTrend(30, null);

        assertEquals(LocalDate.of(2026, 6, 15), response.getDataPoints().get(0).getDate());
    }

    // --- getDefectTrend ---

    @Test
    void getDefectTrend_mergesOpenedAndResolvedDates() {
        LocalDate date1 = LocalDate.of(2026, 11, 1);
        LocalDate date2 = LocalDate.of(2026, 11, 2);
        LocalDate date3 = LocalDate.of(2026, 11, 3);

        when(defectRepository.findDailyOpenedSince(any(Instant.class)))
                .thenReturn(rows(
                        new Object[]{date1, 3},
                        new Object[]{date2, 1}
                ));
        when(defectRepository.findDailyResolvedSince(any(Instant.class)))
                .thenReturn(rows(
                        new Object[]{date2, 2},
                        new Object[]{date3, 4}
                ));

        DefectTrendResponse response = trendService.getDefectTrend(30);

        assertEquals(3, response.getDataPoints().size());

        assertEquals(date1, response.getDataPoints().get(0).getDate());
        assertEquals(3, response.getDataPoints().get(0).getOpened());
        assertEquals(0, response.getDataPoints().get(0).getResolved());

        assertEquals(date2, response.getDataPoints().get(1).getDate());
        assertEquals(1, response.getDataPoints().get(1).getOpened());
        assertEquals(2, response.getDataPoints().get(1).getResolved());

        assertEquals(date3, response.getDataPoints().get(2).getDate());
        assertEquals(0, response.getDataPoints().get(2).getOpened());
        assertEquals(4, response.getDataPoints().get(2).getResolved());
    }

    @Test
    void getDefectTrend_emptyResults_returnsEmptyList() {
        when(defectRepository.findDailyOpenedSince(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(defectRepository.findDailyResolvedSince(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        DefectTrendResponse response = trendService.getDefectTrend(7);

        assertNotNull(response);
        assertTrue(response.getDataPoints().isEmpty());
    }

    @Test
    void getDefectTrend_datesSorted() {
        LocalDate later = LocalDate.of(2026, 12, 1);
        LocalDate earlier = LocalDate.of(2026, 1, 1);

        when(defectRepository.findDailyOpenedSince(any(Instant.class)))
                .thenReturn(rows(new Object[]{later, 1}));
        when(defectRepository.findDailyResolvedSince(any(Instant.class)))
                .thenReturn(rows(new Object[]{earlier, 2}));

        DefectTrendResponse response = trendService.getDefectTrend(365);

        assertEquals(earlier, response.getDataPoints().get(0).getDate());
        assertEquals(later, response.getDataPoints().get(1).getDate());
    }

    // --- getTestVolumeTrend ---

    @Test
    void getTestVolumeTrend_withoutProjectId_callsGlobalQuery() {
        LocalDate date = LocalDate.of(2026, 11, 5);
        when(testRunRepository.findDailyTestVolume(any(Instant.class)))
                .thenReturn(rows(new Object[]{date, "ignored", 42}));

        TrendPointResponse response = trendService.getTestVolumeTrend(30, null);

        assertEquals(1, response.getDataPoints().size());
        assertEquals(date, response.getDataPoints().get(0).getDate());
        assertEquals(42, response.getDataPoints().get(0).getCount());
        verify(testRunRepository).findDailyTestVolume(any(Instant.class));
        verify(testRunRepository, never()).findDailyTestVolumeByProject(any(), any());
    }

    @Test
    void getTestVolumeTrend_withProjectId_callsProjectQuery() {
        UUID projectId = UUID.randomUUID();
        when(testRunRepository.findDailyTestVolumeByProject(any(Instant.class), eq(projectId)))
                .thenReturn(rows(new Object[]{LocalDate.of(2026, 11, 5), "x", 10}));

        TrendPointResponse response = trendService.getTestVolumeTrend(14, projectId);

        assertEquals(1, response.getDataPoints().size());
        assertEquals(10, response.getDataPoints().get(0).getCount());
        verify(testRunRepository).findDailyTestVolumeByProject(any(Instant.class), eq(projectId));
    }

    @Test
    void getTestVolumeTrend_emptyResult() {
        when(testRunRepository.findDailyTestVolume(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        TrendPointResponse response = trendService.getTestVolumeTrend(30, null);

        assertTrue(response.getDataPoints().isEmpty());
    }

    // --- getFlakyTrend ---

    @Test
    void getFlakyTrend_withoutProjectId_callsGlobalQuery() {
        LocalDate date = LocalDate.of(2026, 11, 10);
        when(testIdentityRepository.findDailyFlakyCount(any(Instant.class)))
                .thenReturn(rows(new Object[]{date, 5}));

        TrendPointResponse response = trendService.getFlakyTrend(30, null);

        assertEquals(1, response.getDataPoints().size());
        assertEquals(date, response.getDataPoints().get(0).getDate());
        assertEquals(5, response.getDataPoints().get(0).getCount());
        verify(testIdentityRepository).findDailyFlakyCount(any(Instant.class));
        verify(testIdentityRepository, never()).findDailyFlakyCountByProject(any(), any());
    }

    @Test
    void getFlakyTrend_withProjectId_callsProjectQuery() {
        UUID projectId = UUID.randomUUID();
        when(testIdentityRepository.findDailyFlakyCountByProject(any(Instant.class), eq(projectId)))
                .thenReturn(rows(new Object[]{LocalDate.of(2026, 11, 10), 3}));

        TrendPointResponse response = trendService.getFlakyTrend(7, projectId);

        assertEquals(1, response.getDataPoints().size());
        assertEquals(3, response.getDataPoints().get(0).getCount());
        verify(testIdentityRepository).findDailyFlakyCountByProject(any(Instant.class), eq(projectId));
    }

    @Test
    void getFlakyTrend_emptyResult() {
        when(testIdentityRepository.findDailyFlakyCount(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        TrendPointResponse response = trendService.getFlakyTrend(30, null);

        assertTrue(response.getDataPoints().isEmpty());
    }

    @Test
    void getFlakyTrend_multipleDataPoints() {
        LocalDate d1 = LocalDate.of(2026, 11, 1);
        LocalDate d2 = LocalDate.of(2026, 11, 2);
        when(testIdentityRepository.findDailyFlakyCount(any(Instant.class)))
                .thenReturn(rows(
                        new Object[]{d1, 2},
                        new Object[]{d2, 7}
                ));

        TrendPointResponse response = trendService.getFlakyTrend(30, null);

        assertEquals(2, response.getDataPoints().size());
        assertEquals(2, response.getDataPoints().get(0).getCount());
        assertEquals(7, response.getDataPoints().get(1).getCount());
    }
}
