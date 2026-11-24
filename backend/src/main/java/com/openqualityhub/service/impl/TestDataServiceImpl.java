package com.openqualityhub.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.dto.request.CreateTestDataProfileRequest;
import com.openqualityhub.dto.request.CreateTestDataTemplateRequest;
import com.openqualityhub.dto.request.DataFieldRequest;
import com.openqualityhub.dto.request.GenerateTestDataRequest;
import com.openqualityhub.dto.request.UpdateTestDataProfileRequest;
import com.openqualityhub.dto.request.UpdateTestDataTemplateRequest;
import com.openqualityhub.dto.response.TestDataDashboardResponse;
import com.openqualityhub.dto.response.TestDataGenerationResponse;
import com.openqualityhub.dto.response.TestDataProfileResponse;
import com.openqualityhub.dto.response.TestDataTemplateDetailResponse;
import com.openqualityhub.dto.response.TestDataTemplateResponse;
import com.openqualityhub.entity.DataField;
import com.openqualityhub.entity.TestDataGeneration;
import com.openqualityhub.entity.TestDataProfile;
import com.openqualityhub.entity.TestDataTemplate;
import com.openqualityhub.exception.ResourceNotFoundException;
import com.openqualityhub.mapper.TestDataMapper;
import com.openqualityhub.repository.DataFieldRepository;
import com.openqualityhub.repository.OrganizationRepository;
import com.openqualityhub.repository.ProjectRepository;
import com.openqualityhub.repository.TestDataGenerationRepository;
import com.openqualityhub.repository.TestDataProfileRepository;
import com.openqualityhub.repository.TestDataTemplateRepository;
import com.openqualityhub.service.TestDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestDataServiceImpl implements TestDataService {

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final TestDataTemplateRepository templateRepository;
    private final DataFieldRepository fieldRepository;
    private final TestDataProfileRepository profileRepository;
    private final TestDataGenerationRepository generationRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final TestDataMapper mapper;
    private final DataGenerationEngine dataGenerationEngine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TestDataTemplateResponse createTemplate(CreateTestDataTemplateRequest request) {
        if (templateRepository.existsByNameAndOrganizationIdAndDeletedFalse(request.getName(), DEFAULT_ORG_ID)) {
            throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists");
        }

        TestDataTemplate template = TestDataTemplate.builder()
                .organization(organizationRepository.findById(DEFAULT_ORG_ID).orElseThrow())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        if (request.getProjectId() != null) {
            projectRepository.findByIdAndDeletedFalse(request.getProjectId())
                    .ifPresent(template::setProject);
        }

        template = templateRepository.save(template);

        List<DataField> fields = buildFields(request.getFields(), template);
        fieldRepository.saveAll(fields);
        template.setFields(fields);

        log.info("Created test data template: {} ({})", template.getName(), template.getId());
        return mapper.toTemplateResponse(template);
    }

    @Override
    @Transactional
    public TestDataTemplateResponse updateTemplate(UUID id, UpdateTestDataTemplateRequest request) {
        TestDataTemplate template = templateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataTemplate", id));

        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getProjectId() != null) {
            projectRepository.findByIdAndDeletedFalse(request.getProjectId())
                    .ifPresent(template::setProject);
        }

        if (request.getFields() != null) {
            // Soft-delete existing fields
            template.getFields().stream()
                    .filter(f -> !f.isDeleted())
                    .forEach(f -> f.setDeleted(true));
            fieldRepository.saveAll(template.getFields());

            // Create new fields
            List<DataField> newFields = buildFields(request.getFields(), template);
            fieldRepository.saveAll(newFields);
            template.getFields().addAll(newFields);
        }

        template = templateRepository.save(template);
        log.info("Updated test data template: {} ({})", template.getName(), template.getId());
        return mapper.toTemplateResponse(template);
    }

    @Override
    public TestDataTemplateDetailResponse getTemplateById(UUID id) {
        TestDataTemplate template = templateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataTemplate", id));
        return mapper.toTemplateDetailResponse(template);
    }

    @Override
    public Page<TestDataTemplateResponse> getAllTemplates(UUID projectId, Pageable pageable) {
        if (projectId != null) {
            return templateRepository.findByProjectIdAndDeletedFalse(projectId, pageable)
                    .map(mapper::toTemplateResponse);
        }
        return templateRepository.findByDeletedFalse(pageable).map(mapper::toTemplateResponse);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        TestDataTemplate template = templateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataTemplate", id));
        template.setDeleted(true);
        templateRepository.save(template);
        log.info("Soft-deleted test data template: {}", id);
    }

    @Override
    @Transactional
    public TestDataProfileResponse createProfile(CreateTestDataProfileRequest request) {
        TestDataTemplate template = templateRepository.findByIdAndDeletedFalse(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("TestDataTemplate", request.getTemplateId()));

        if (profileRepository.existsByNameAndTemplateIdAndDeletedFalse(request.getName(), template.getId())) {
            throw new IllegalArgumentException("Profile with name '" + request.getName() + "' already exists for this template");
        }

        TestDataProfile.DataProfileType profileType = TestDataProfile.DataProfileType.valueOf(request.getProfileType());

        TestDataProfile profile = TestDataProfile.builder()
                .template(template)
                .name(request.getName())
                .profileType(profileType)
                .description(request.getDescription())
                .overrides(request.getOverrides() != null ? request.getOverrides() : "{}")
                .build();

        profile = profileRepository.save(profile);
        log.info("Created test data profile: {} ({}) for template {}", profile.getName(), profile.getId(), template.getId());
        return mapper.toProfileResponse(profile);
    }

    @Override
    @Transactional
    public TestDataProfileResponse updateProfile(UUID id, UpdateTestDataProfileRequest request) {
        TestDataProfile profile = profileRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataProfile", id));

        if (request.getName() != null) profile.setName(request.getName());
        if (request.getDescription() != null) profile.setDescription(request.getDescription());
        if (request.getOverrides() != null) profile.setOverrides(request.getOverrides());

        profile = profileRepository.save(profile);
        log.info("Updated test data profile: {} ({})", profile.getName(), profile.getId());
        return mapper.toProfileResponse(profile);
    }

    @Override
    @Transactional
    public void deleteProfile(UUID id) {
        TestDataProfile profile = profileRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataProfile", id));
        profile.setDeleted(true);
        profileRepository.save(profile);
        log.info("Soft-deleted test data profile: {}", id);
    }

    @Override
    @Transactional
    public TestDataGenerationResponse generate(GenerateTestDataRequest request) {
        TestDataTemplate template = templateRepository.findByIdAndDeletedFalse(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("TestDataTemplate", request.getTemplateId()));

        TestDataProfile profile = null;
        if (request.getProfileId() != null) {
            profile = profileRepository.findByIdAndDeletedFalse(request.getProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("TestDataProfile", request.getProfileId()));
        }

        TestDataGeneration.ExportFormat exportFormat = TestDataGeneration.ExportFormat.valueOf(request.getExportFormat());

        TestDataGeneration generation = TestDataGeneration.builder()
                .template(template)
                .profile(profile)
                .rowCount(request.getRowCount())
                .status(TestDataGeneration.GenerationStatus.PENDING)
                .exportFormat(exportFormat)
                .build();

        generation = generationRepository.save(generation);

        try {
            generation.setStatus(TestDataGeneration.GenerationStatus.IN_PROGRESS);
            generation.setStartedAt(Instant.now());
            generation = generationRepository.save(generation);

            List<DataField> fields = fieldRepository.findByTemplateIdAndDeletedFalseOrderByDisplayOrder(template.getId());
            List<Map<String, Object>> rows = dataGenerationEngine.generate(fields, profile, request.getRowCount());

            String jsonData = objectMapper.writeValueAsString(rows);
            generation.setGeneratedData(jsonData);
            generation.setStatus(TestDataGeneration.GenerationStatus.COMPLETED);
            generation.setCompletedAt(Instant.now());

            log.info("Generated {} rows for template {} (generation {})", request.getRowCount(), template.getId(), generation.getId());
        } catch (JsonProcessingException e) {
            generation.setStatus(TestDataGeneration.GenerationStatus.FAILED);
            generation.setErrorMessage("Failed to serialize generated data: " + e.getMessage());
            generation.setCompletedAt(Instant.now());
            log.error("Failed to serialize generated data for template {}: {}", template.getId(), e.getMessage());
        } catch (Exception e) {
            generation.setStatus(TestDataGeneration.GenerationStatus.FAILED);
            generation.setErrorMessage("Generation failed: " + e.getMessage());
            generation.setCompletedAt(Instant.now());
            log.error("Failed to generate data for template {}: {}", template.getId(), e.getMessage());
        }

        generation = generationRepository.save(generation);
        return mapper.toGenerationResponse(generation);
    }

    @Override
    public TestDataGenerationResponse getGeneration(UUID id) {
        TestDataGeneration generation = generationRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataGeneration", id));
        return mapper.toGenerationResponse(generation);
    }

    @Override
    public Page<TestDataGenerationResponse> getGenerationsByTemplate(UUID templateId, Pageable pageable) {
        templateRepository.findByIdAndDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataTemplate", templateId));
        return generationRepository.findByTemplateIdAndDeletedFalseOrderByCreatedAtDesc(templateId, pageable)
                .map(mapper::toGenerationResponse);
    }

    @Override
    public byte[] downloadGeneration(UUID generationId) {
        TestDataGeneration generation = generationRepository.findByIdAndDeletedFalse(generationId)
                .orElseThrow(() -> new ResourceNotFoundException("TestDataGeneration", generationId));

        if (generation.getStatus() != TestDataGeneration.GenerationStatus.COMPLETED) {
            throw new IllegalStateException("Generation is not completed. Current status: " + generation.getStatus());
        }

        if (generation.getGeneratedData() == null) {
            throw new IllegalStateException("No generated data available for download");
        }

        if (generation.getExportFormat() == TestDataGeneration.ExportFormat.CSV) {
            return convertJsonToCsv(generation.getGeneratedData()).getBytes(StandardCharsets.UTF_8);
        }

        return generation.getGeneratedData().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public TestDataDashboardResponse getDashboard() {
        long totalTemplates = templateRepository.countByDeletedFalse();
        long totalProfiles = profileRepository.countByDeletedFalse();
        long totalGenerations = generationRepository.countByDeletedFalse();

        List<TestDataGenerationResponse> recentGenerations = generationRepository
                .findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .map(mapper::toGenerationResponse)
                .getContent();

        return TestDataDashboardResponse.builder()
                .totalTemplates(totalTemplates)
                .totalProfiles(totalProfiles)
                .totalGenerations(totalGenerations)
                .recentGenerations(recentGenerations)
                .build();
    }

    // ---- Private helpers ----

    private List<DataField> buildFields(List<DataFieldRequest> fieldRequests, TestDataTemplate template) {
        List<DataField> fields = new ArrayList<>();
        for (int i = 0; i < fieldRequests.size(); i++) {
            DataFieldRequest req = fieldRequests.get(i);
            DataField.FieldType fieldType = DataField.FieldType.valueOf(req.getFieldType());

            DataField field = DataField.builder()
                    .template(template)
                    .name(req.getName())
                    .fieldType(fieldType)
                    .displayOrder(req.getDisplayOrder() > 0 ? req.getDisplayOrder() : i)
                    .nullable(req.isNullable())
                    .fakerGenerator(req.getFakerGenerator())
                    .constraints(req.getConstraints() != null ? req.getConstraints() : "{}")
                    .build();
            fields.add(field);
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private String convertJsonToCsv(String jsonData) {
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(jsonData,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            if (rows.isEmpty()) {
                return "";
            }

            StringBuilder csv = new StringBuilder();

            // Header
            List<String> headers = new ArrayList<>(rows.get(0).keySet());
            csv.append(headers.stream()
                    .map(this::escapeCsvField)
                    .collect(Collectors.joining(",")));
            csv.append("\n");

            // Rows
            for (Map<String, Object> row : rows) {
                csv.append(headers.stream()
                        .map(h -> escapeCsvField(String.valueOf(row.getOrDefault(h, ""))))
                        .collect(Collectors.joining(",")));
                csv.append("\n");
            }

            return csv.toString();
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to CSV: {}", e.getMessage());
            throw new IllegalStateException("Failed to convert generated data to CSV format", e);
        }
    }

    private String escapeCsvField(String value) {
        if (value == null || "null".equals(value)) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
