package com.openqualityhub.service;

import com.openqualityhub.dto.request.CreateQaServiceRequest;
import com.openqualityhub.dto.response.QaServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface QaServiceCatalogService {

    QaServiceResponse create(CreateQaServiceRequest request);

    QaServiceResponse update(UUID id, CreateQaServiceRequest request);

    QaServiceResponse getById(UUID id);

    Page<QaServiceResponse> getAll(Pageable pageable);

    Page<QaServiceResponse> getByCategory(String category, Pageable pageable);

    void delete(UUID id);
}
