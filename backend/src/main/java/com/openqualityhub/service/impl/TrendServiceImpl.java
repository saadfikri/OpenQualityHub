package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.DefectTrendResponse;
import com.openqualityhub.dto.response.PassRateTrendResponse;
import com.openqualityhub.dto.response.TrendPointResponse;
import com.openqualityhub.repository.DefectRepository;
import com.openqualityhub.repository.TestIdentityRepository;
import com.openqualityhub.repository.TestRunRepository;
import com.openqualityhub.service.TrendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrendServiceImpl implements TrendService {

    private final TestRunRepository testRunRepository;
    private final DefectRepository defectRepository;
    private final TestIdentityRepository testIdentityRepository;

    @Override
    public PassRateTrendResponse getPassRateTrend(int days, UUID projectId) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Object[]> rows = projectId != null
                ? testRunRepository.findDailyPassRateTrendByProject(since, projectId)
                : testRunRepository.findDailyPassRateTrend(since);

        List<PassRateTrendResponse.TrendPoint> points = rows.stream()
                .map(row -> PassRateTrendResponse.TrendPoint.builder()
                        .date(toLocalDate(row[0]))
                        .passRate(toBigDecimal(row[1]))
                        .totalTests(toInt(row[2]))
                        .build())
                .collect(Collectors.toList());

        return PassRateTrendResponse.builder().dataPoints(points).build();
    }

    @Override
    public DefectTrendResponse getDefectTrend(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Object[]> openedRows = defectRepository.findDailyOpenedSince(since);
        List<Object[]> resolvedRows = defectRepository.findDailyResolvedSince(since);

        Map<LocalDate, Integer> openedMap = new LinkedHashMap<>();
        for (Object[] row : openedRows) {
            openedMap.put(toLocalDate(row[0]), toInt(row[1]));
        }

        Map<LocalDate, Integer> resolvedMap = new LinkedHashMap<>();
        for (Object[] row : resolvedRows) {
            resolvedMap.put(toLocalDate(row[0]), toInt(row[1]));
        }

        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(openedMap.keySet());
        allDates.addAll(resolvedMap.keySet());

        List<DefectTrendResponse.DefectTrendPoint> points = allDates.stream()
                .map(date -> DefectTrendResponse.DefectTrendPoint.builder()
                        .date(date)
                        .opened(openedMap.getOrDefault(date, 0))
                        .resolved(resolvedMap.getOrDefault(date, 0))
                        .build())
                .collect(Collectors.toList());

        return DefectTrendResponse.builder().dataPoints(points).build();
    }

    @Override
    public TrendPointResponse getTestVolumeTrend(int days, UUID projectId) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Object[]> rows = projectId != null
                ? testRunRepository.findDailyTestVolumeByProject(since, projectId)
                : testRunRepository.findDailyTestVolume(since);

        List<TrendPointResponse.TrendPoint> points = rows.stream()
                .map(row -> TrendPointResponse.TrendPoint.builder()
                        .date(toLocalDate(row[0]))
                        .count(toInt(row[2]))
                        .build())
                .collect(Collectors.toList());

        return TrendPointResponse.builder().dataPoints(points).build();
    }

    @Override
    public TrendPointResponse getFlakyTrend(int days, UUID projectId) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        List<Object[]> rows = projectId != null
                ? testIdentityRepository.findDailyFlakyCountByProject(since, projectId)
                : testIdentityRepository.findDailyFlakyCount(since);

        List<TrendPointResponse.TrendPoint> points = rows.stream()
                .map(row -> TrendPointResponse.TrendPoint.builder()
                        .date(toLocalDate(row[0]))
                        .count(toInt(row[1]))
                        .build())
                .collect(Collectors.toList());

        return TrendPointResponse.builder().dataPoints(points).build();
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        if (value instanceof java.time.LocalDate) {
            return (java.time.LocalDate) value;
        }
        return LocalDate.parse(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return ((BigDecimal) value).setScale(2, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(((Number) value).doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        return ((Number) value).intValue();
    }
}
