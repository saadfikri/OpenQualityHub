package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateSlaConfigurationRequest;
import com.openqualityhub.dto.response.SlaConfigurationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SlaService {

    SlaConfigurationResponse create(CreateSlaConfigurationRequest request);

    SlaConfigurationResponse update(UUID id, CreateSlaConfigurationRequest request);

    SlaConfigurationResponse getById(UUID id);

    Page<SlaConfigurationResponse> getAll(Pageable pageable);

    List<SlaConfigurationResponse> getActive();

    void delete(UUID id);
}
