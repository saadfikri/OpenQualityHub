package com.openqualityhub.service.impl;

import com.openqualityhub.dto.response.ReleaseResponse;
import com.openqualityhub.entity.Release;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.ReleaseMapper;
import com.openqualityhub.repository.ReleaseRepository;
import com.openqualityhub.service.ReleaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseServiceImpl implements ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final ReleaseMapper releaseMapper;

    @Override
    public ReleaseResponse getById(UUID id) {
        Release release = releaseRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Release", id));
        return releaseMapper.toResponse(release);
    }

    @Override
    public Page<ReleaseResponse> getByProjectId(UUID projectId, Pageable pageable) {
        return releaseRepository.findByProjectIdAndDeletedFalse(projectId, pageable)
            .map(releaseMapper::toResponse);
    }

    @Override
    public Page<ReleaseResponse> getAll(Pageable pageable) {
        return releaseRepository.findByDeletedFalse(pageable).map(releaseMapper::toResponse);
    }
}
