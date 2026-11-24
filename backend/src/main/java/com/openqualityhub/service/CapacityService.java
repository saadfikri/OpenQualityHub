package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateCapacityAllocationRequest;
import com.openqualityhub.dto.response.CapacityOverviewResponse;

import java.time.LocalDate;

public interface CapacityService {

    void createOrUpdate(CreateCapacityAllocationRequest request);

    CapacityOverviewResponse getOverview(LocalDate weekStart);
}
