package com.openqualityhub.service;

import com.openqualityhub.dto.response.DefectTrendResponse;
import com.openqualityhub.dto.response.PassRateTrendResponse;
import com.openqualityhub.dto.response.TrendPointResponse;

import java.util.UUID;

public interface TrendService {

    PassRateTrendResponse getPassRateTrend(int days, UUID projectId);

    DefectTrendResponse getDefectTrend(int days);

    TrendPointResponse getTestVolumeTrend(int days, UUID projectId);

    TrendPointResponse getFlakyTrend(int days, UUID projectId);
}
