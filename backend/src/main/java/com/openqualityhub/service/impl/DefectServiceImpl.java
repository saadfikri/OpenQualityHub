package com.openqualityhub.service.impl;

import com.openqualityhub.dto.request.CreateDefectRequest;
import com.openqualityhub.dto.request.UpdateDefectRequest;
import com.openqualityhub.dto.response.DefectResponse;
import com.openqualityhub.entity.Defect;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.Release;
import com.openqualityhub.entity.User;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.DefectMapper;
import com.openqualityhub.repository.*;
import com.openqualityhub.service.DefectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DefectServiceImpl implements DefectService {

    private final DefectRepository defectRepository;
    private final ProjectRepository projectRepository;
    private final ReleaseRepository releaseRepository;
    private final UserRepository userRepository;
    private final DefectMapper defectMapper;

    @Override
    @Transactional
    public DefectResponse create(CreateDefectRequest request, UUID reportedById) {
        Project project = projectRepository.findByIdAndDeletedFalse(request.getProjectId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        Defect defect = Defect.builder()
            .project(project)
            .title(request.getTitle())
            .description(request.getDescription())
            .severity(Defect.Severity.valueOf(request.getSeverity()))
            .status(Defect.DefectStatus.OPEN)
            .priority(request.getPriority())
            .component(request.getComponent())
            .externalId(request.getExternalId())
            .externalUrl(request.getExternalUrl())
            .build();

        if (request.getReleaseId() != null) {
            defect.setRelease(releaseRepository.findByIdAndDeletedFalse(request.getReleaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Release", request.getReleaseId())));
        }
        if (request.getAssignedToId() != null) {
            defect.setAssignedTo(userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getAssignedToId())));
        }
        if (reportedById != null) {
            defect.setReportedBy(userRepository.findById(reportedById).orElse(null));
        }

        defect = defectRepository.save(defect);
        log.info("Created defect: {} ({})", defect.getTitle(), defect.getId());
        return defectMapper.toResponse(defect);
    }

    @Override
    @Transactional
    public DefectResponse update(UUID id, UpdateDefectRequest request) {
        Defect defect = defectRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Defect", id));

        if (request.getTitle() != null) defect.setTitle(request.getTitle());
        if (request.getDescription() != null) defect.setDescription(request.getDescription());
        if (request.getSeverity() != null) defect.setSeverity(Defect.Severity.valueOf(request.getSeverity()));
        if (request.getStatus() != null) {
            Defect.DefectStatus newStatus = Defect.DefectStatus.valueOf(request.getStatus());
            defect.setStatus(newStatus);
            if (newStatus == Defect.DefectStatus.RESOLVED || newStatus == Defect.DefectStatus.CLOSED) {
                defect.setResolvedAt(Instant.now());
            }
        }
        if (request.getPriority() != null) defect.setPriority(request.getPriority());
        if (request.getComponent() != null) defect.setComponent(request.getComponent());
        if (request.getResolution() != null) defect.setResolution(request.getResolution());
        if (request.getAssignedToId() != null) {
            defect.setAssignedTo(userRepository.findById(request.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getAssignedToId())));
        }
        if (request.getExternalId() != null) defect.setExternalId(request.getExternalId());
        if (request.getExternalUrl() != null) defect.setExternalUrl(request.getExternalUrl());

        defect = defectRepository.save(defect);
        return defectMapper.toResponse(defect);
    }

    @Override
    public DefectResponse getById(UUID id) {
        return defectMapper.toResponse(defectRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Defect", id)));
    }

    @Override
    public Page<DefectResponse> getByProjectId(UUID projectId, Pageable pageable) {
        return defectRepository.findByProjectIdAndDeletedFalse(projectId, pageable)
            .map(defectMapper::toResponse);
    }

    @Override
    public Page<DefectResponse> getBySeverity(String severity, Pageable pageable) {
        Defect.Severity sev = Defect.Severity.valueOf(severity);
        return defectRepository.findBySeverityAndDeletedFalse(sev, pageable)
            .map(defectMapper::toResponse);
    }

    @Override
    public Page<DefectResponse> getByStatus(String status, Pageable pageable) {
        Defect.DefectStatus defectStatus = Defect.DefectStatus.valueOf(status);
        return defectRepository.findByStatusAndDeletedFalse(defectStatus, pageable)
            .map(defectMapper::toResponse);
    }

    @Override
    public Page<DefectResponse> getAll(Pageable pageable) {
        return defectRepository.findByDeletedFalse(pageable).map(defectMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Defect defect = defectRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Defect", id));
        defect.setDeleted(true);
        defectRepository.save(defect);
        log.info("Soft-deleted defect: {}", id);
    }
}
