package com.openqualityhub.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openqualityhub.dto.request.CreateTestDataProfileRequest;
import com.openqualityhub.dto.request.CreateTestDataTemplateRequest;
import com.openqualityhub.dto.request.DataFieldRequest;
import com.openqualityhub.dto.request.GenerateTestDataRequest;
import com.openqualityhub.dto.request.UpdateTestDataTemplateRequest;
import com.openqualityhub.dto.response.TestDataDashboardResponse;
import com.openqualityhub.dto.response.TestDataGenerationResponse;
import com.openqualityhub.dto.response.TestDataProfileResponse;
import com.openqualityhub.dto.response.TestDataTemplateDetailResponse;
import com.openqualityhub.dto.response.TestDataTemplateResponse;
import com.openqualityhub.entity.DataField;
import com.openqualityhub.entity.Organization;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDataServiceImplTest {

    @Mock private TestDataTemplateRepository templateRepository;
    @Mock private DataFieldRepository fieldRepository;
    @Mock private TestDataProfileRepository profileRepository;
    @Mock private TestDataGenerationRepository generationRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TestDataMapper mapper;
    @Mock private DataGenerationEngine dataGenerationEngine;

    private ObjectMapper objectMapper;
    private TestDataServiceImpl service;

    private static final UUID DEFAULT_ORG_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private Organization org;
    private UUID templateId;
    private UUID profileId;
    private UUID generationId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new TestDataServiceImpl(templateRepository, fieldRepository, profileRepository,
                generationRepository, organizationRepository, projectRepository, mapper, dataGenerationEngine, objectMapper);

        org = Organization.builder().name("Demo").slug("demo").build();
        org.setId(DEFAULT_ORG_ID);
        templateId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        generationId = UUID.randomUUID();
    }

    // ---- Template CRUD ----

    @Test
    void createTemplate_success() {
        DataFieldRequest fieldReq = DataFieldRequest.builder()
                .name("username")
                .fieldType("STRING")
                .build();

        CreateTestDataTemplateRequest request = CreateTestDataTemplateRequest.builder()
                .name("User Template")
                .description("Template for users")
                .fields(List.of(fieldReq))
                .build();

        TestDataTemplate template = TestDataTemplate.builder()
                .organization(org)
                .name("User Template")
                .description("Template for users")
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        TestDataTemplateResponse expectedResponse = TestDataTemplateResponse.builder()
                .id(templateId)
                .name("User Template")
                .build();

        when(templateRepository.existsByNameAndOrganizationIdAndDeletedFalse("User Template", DEFAULT_ORG_ID))
                .thenReturn(false);
        when(organizationRepository.findById(DEFAULT_ORG_ID)).thenReturn(Optional.of(org));
        when(templateRepository.save(any(TestDataTemplate.class))).thenReturn(template);
        when(fieldRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(mapper.toTemplateResponse(any(TestDataTemplate.class))).thenReturn(expectedResponse);

        TestDataTemplateResponse result = service.createTemplate(request);

        assertNotNull(result);
        assertEquals("User Template", result.getName());
        verify(templateRepository).save(any(TestDataTemplate.class));
        verify(fieldRepository).saveAll(anyList());
    }

    @Test
    void createTemplate_duplicateName_throwsException() {
        CreateTestDataTemplateRequest request = CreateTestDataTemplateRequest.builder()
                .name("Existing Template")
                .fields(List.of(DataFieldRequest.builder().name("f").fieldType("STRING").build()))
                .build();

        when(templateRepository.existsByNameAndOrganizationIdAndDeletedFalse("Existing Template", DEFAULT_ORG_ID))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createTemplate(request));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_success() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("Old Name")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        UpdateTestDataTemplateRequest request = UpdateTestDataTemplateRequest.builder()
                .name("New Name")
                .description("Updated description")
                .build();

        TestDataTemplateResponse expectedResponse = TestDataTemplateResponse.builder()
                .id(templateId)
                .name("New Name")
                .build();

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(TestDataTemplate.class))).thenReturn(template);
        when(mapper.toTemplateResponse(any(TestDataTemplate.class))).thenReturn(expectedResponse);

        TestDataTemplateResponse result = service.updateTemplate(templateId, request);

        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("New Name", template.getName());
        assertEquals("Updated description", template.getDescription());
        verify(templateRepository).save(template);
    }

    @Test
    void updateTemplate_notFound_throwsException() {
        UpdateTestDataTemplateRequest request = UpdateTestDataTemplateRequest.builder()
                .name("Whatever")
                .build();

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateTemplate(templateId, request));
    }

    @Test
    void getTemplateById_success() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("My Template")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        TestDataTemplateDetailResponse expectedResponse = TestDataTemplateDetailResponse.builder()
                .id(templateId)
                .name("My Template")
                .build();

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(mapper.toTemplateDetailResponse(template)).thenReturn(expectedResponse);

        TestDataTemplateDetailResponse result = service.getTemplateById(templateId);

        assertNotNull(result);
        assertEquals("My Template", result.getName());
        verify(templateRepository).findByIdAndDeletedFalse(templateId);
    }

    @Test
    void getTemplateById_notFound_throwsException() {
        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getTemplateById(templateId));
    }

    @Test
    void getAllTemplates_noFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        TestDataTemplate template = TestDataTemplate.builder()
                .name("T1")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        Page<TestDataTemplate> page = new PageImpl<>(List.of(template));
        TestDataTemplateResponse resp = TestDataTemplateResponse.builder().id(templateId).name("T1").build();

        when(templateRepository.findByDeletedFalse(pageable)).thenReturn(page);
        when(mapper.toTemplateResponse(template)).thenReturn(resp);

        Page<TestDataTemplateResponse> result = service.getAllTemplates(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("T1", result.getContent().get(0).getName());
        verify(templateRepository).findByDeletedFalse(pageable);
        verify(templateRepository, never()).findByProjectIdAndDeletedFalse(any(), any());
    }

    @Test
    void getAllTemplates_withProjectFilter() {
        UUID projectId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        TestDataTemplate template = TestDataTemplate.builder()
                .name("Filtered")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        Page<TestDataTemplate> page = new PageImpl<>(List.of(template));
        TestDataTemplateResponse resp = TestDataTemplateResponse.builder().id(templateId).name("Filtered").build();

        when(templateRepository.findByProjectIdAndDeletedFalse(projectId, pageable)).thenReturn(page);
        when(mapper.toTemplateResponse(template)).thenReturn(resp);

        Page<TestDataTemplateResponse> result = service.getAllTemplates(projectId, pageable);

        assertEquals(1, result.getTotalElements());
        verify(templateRepository).findByProjectIdAndDeletedFalse(projectId, pageable);
        verify(templateRepository, never()).findByDeletedFalse(any());
    }

    @Test
    void deleteTemplate_success() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("ToDelete")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(TestDataTemplate.class))).thenReturn(template);

        service.deleteTemplate(templateId);

        assertTrue(template.isDeleted());
        verify(templateRepository).save(template);
    }

    // ---- Profile CRUD ----

    @Test
    void createProfile_success() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("Template")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        CreateTestDataProfileRequest request = CreateTestDataProfileRequest.builder()
                .name("Positive Profile")
                .templateId(templateId)
                .profileType("POSITIVE")
                .description("Positive test data")
                .build();

        TestDataProfile profile = TestDataProfile.builder()
                .template(template)
                .name("Positive Profile")
                .profileType(TestDataProfile.DataProfileType.POSITIVE)
                .description("Positive test data")
                .build();
        profile.setId(profileId);

        TestDataProfileResponse expectedResponse = TestDataProfileResponse.builder()
                .id(profileId)
                .name("Positive Profile")
                .profileType("POSITIVE")
                .build();

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(profileRepository.existsByNameAndTemplateIdAndDeletedFalse("Positive Profile", templateId))
                .thenReturn(false);
        when(profileRepository.save(any(TestDataProfile.class))).thenReturn(profile);
        when(mapper.toProfileResponse(any(TestDataProfile.class))).thenReturn(expectedResponse);

        TestDataProfileResponse result = service.createProfile(request);

        assertNotNull(result);
        assertEquals("Positive Profile", result.getName());
        assertEquals("POSITIVE", result.getProfileType());
        verify(profileRepository).save(any(TestDataProfile.class));
    }

    @Test
    void createProfile_duplicateName_throwsException() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("Template")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        CreateTestDataProfileRequest request = CreateTestDataProfileRequest.builder()
                .name("Duplicate")
                .templateId(templateId)
                .profileType("POSITIVE")
                .build();

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(profileRepository.existsByNameAndTemplateIdAndDeletedFalse("Duplicate", templateId))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createProfile(request));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void deleteProfile_success() {
        TestDataProfile profile = TestDataProfile.builder()
                .name("ToDelete")
                .profileType(TestDataProfile.DataProfileType.POSITIVE)
                .build();
        profile.setId(profileId);

        when(profileRepository.findByIdAndDeletedFalse(profileId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(TestDataProfile.class))).thenReturn(profile);

        service.deleteProfile(profileId);

        assertTrue(profile.isDeleted());
        verify(profileRepository).save(profile);
    }

    // ---- Generation ----

    @Test
    void generate_success() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("User Template")
                .organization(org)
                .fields(new ArrayList<>())
                .profiles(new ArrayList<>())
                .generations(new ArrayList<>())
                .build();
        template.setId(templateId);

        TestDataProfile profile = TestDataProfile.builder()
                .template(template)
                .name("Positive")
                .profileType(TestDataProfile.DataProfileType.POSITIVE)
                .build();
        profile.setId(profileId);

        List<DataField> fields = List.of(
                buildField("name", DataField.FieldType.STRING),
                buildField("age", DataField.FieldType.INTEGER)
        );

        GenerateTestDataRequest request = GenerateTestDataRequest.builder()
                .templateId(templateId)
                .profileId(profileId)
                .rowCount(5)
                .exportFormat("JSON")
                .build();

        TestDataGeneration savedGeneration = TestDataGeneration.builder()
                .template(template)
                .profile(profile)
                .rowCount(5)
                .status(TestDataGeneration.GenerationStatus.PENDING)
                .exportFormat(TestDataGeneration.ExportFormat.JSON)
                .build();
        savedGeneration.setId(generationId);

        List<Map<String, Object>> generatedRows = List.of(
                Map.of("name", "John", "age", 25),
                Map.of("name", "Jane", "age", 30),
                Map.of("name", "Bob", "age", 22),
                Map.of("name", "Alice", "age", 28),
                Map.of("name", "Eve", "age", 35)
        );

        TestDataGenerationResponse expectedResponse = TestDataGenerationResponse.builder()
                .id(generationId)
                .templateId(templateId)
                .templateName("User Template")
                .rowCount(5)
                .status("COMPLETED")
                .exportFormat("JSON")
                .build();

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.of(template));
        when(profileRepository.findByIdAndDeletedFalse(profileId)).thenReturn(Optional.of(profile));
        when(generationRepository.save(any(TestDataGeneration.class))).thenReturn(savedGeneration);
        when(fieldRepository.findByTemplateIdAndDeletedFalseOrderByDisplayOrder(templateId)).thenReturn(fields);
        when(dataGenerationEngine.generate(fields, profile, 5)).thenReturn(generatedRows);
        when(mapper.toGenerationResponse(any(TestDataGeneration.class))).thenReturn(expectedResponse);

        TestDataGenerationResponse result = service.generate(request);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(5, result.getRowCount());
        // The objectMapper is real, so serialization actually happened
        verify(dataGenerationEngine).generate(fields, profile, 5);
        verify(generationRepository, times(3)).save(any(TestDataGeneration.class));
    }

    @Test
    void generate_templateNotFound_throwsException() {
        GenerateTestDataRequest request = GenerateTestDataRequest.builder()
                .templateId(templateId)
                .rowCount(10)
                .exportFormat("JSON")
                .build();

        when(templateRepository.findByIdAndDeletedFalse(templateId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.generate(request));
    }

    @Test
    void getGeneration_success() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("Template")
                .organization(org)
                .build();
        template.setId(templateId);

        TestDataGeneration generation = TestDataGeneration.builder()
                .template(template)
                .rowCount(10)
                .status(TestDataGeneration.GenerationStatus.COMPLETED)
                .exportFormat(TestDataGeneration.ExportFormat.JSON)
                .build();
        generation.setId(generationId);

        TestDataGenerationResponse expectedResponse = TestDataGenerationResponse.builder()
                .id(generationId)
                .rowCount(10)
                .status("COMPLETED")
                .build();

        when(generationRepository.findByIdAndDeletedFalse(generationId)).thenReturn(Optional.of(generation));
        when(mapper.toGenerationResponse(generation)).thenReturn(expectedResponse);

        TestDataGenerationResponse result = service.getGeneration(generationId);

        assertNotNull(result);
        assertEquals(10, result.getRowCount());
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    void downloadGeneration_json() {
        TestDataGeneration generation = TestDataGeneration.builder()
                .rowCount(2)
                .status(TestDataGeneration.GenerationStatus.COMPLETED)
                .exportFormat(TestDataGeneration.ExportFormat.JSON)
                .generatedData("[{\"name\":\"John\"},{\"name\":\"Jane\"}]")
                .build();
        generation.setId(generationId);

        when(generationRepository.findByIdAndDeletedFalse(generationId)).thenReturn(Optional.of(generation));

        byte[] result = service.downloadGeneration(generationId);

        assertNotNull(result);
        String content = new String(result, StandardCharsets.UTF_8);
        assertEquals("[{\"name\":\"John\"},{\"name\":\"Jane\"}]", content);
    }

    @Test
    void downloadGeneration_notCompleted_throwsException() {
        TestDataGeneration generation = TestDataGeneration.builder()
                .rowCount(10)
                .status(TestDataGeneration.GenerationStatus.IN_PROGRESS)
                .exportFormat(TestDataGeneration.ExportFormat.JSON)
                .build();
        generation.setId(generationId);

        when(generationRepository.findByIdAndDeletedFalse(generationId)).thenReturn(Optional.of(generation));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.downloadGeneration(generationId));

        assertTrue(ex.getMessage().contains("not completed"));
    }

    @Test
    void getDashboard_success() {
        TestDataTemplate template = TestDataTemplate.builder()
                .name("T")
                .organization(org)
                .build();
        template.setId(templateId);

        TestDataGeneration generation = TestDataGeneration.builder()
                .template(template)
                .rowCount(5)
                .status(TestDataGeneration.GenerationStatus.COMPLETED)
                .exportFormat(TestDataGeneration.ExportFormat.JSON)
                .build();
        generation.setId(generationId);

        TestDataGenerationResponse genResponse = TestDataGenerationResponse.builder()
                .id(generationId)
                .rowCount(5)
                .status("COMPLETED")
                .build();

        Page<TestDataGeneration> recentPage = new PageImpl<>(List.of(generation));

        when(templateRepository.countByDeletedFalse()).thenReturn(5L);
        when(profileRepository.countByDeletedFalse()).thenReturn(12L);
        when(generationRepository.countByDeletedFalse()).thenReturn(42L);
        when(generationRepository.findByDeletedFalseOrderByCreatedAtDesc(PageRequest.of(0, 10)))
                .thenReturn(recentPage);
        when(mapper.toGenerationResponse(generation)).thenReturn(genResponse);

        TestDataDashboardResponse result = service.getDashboard();

        assertNotNull(result);
        assertEquals(5L, result.getTotalTemplates());
        assertEquals(12L, result.getTotalProfiles());
        assertEquals(42L, result.getTotalGenerations());
        assertEquals(1, result.getRecentGenerations().size());
        assertEquals("COMPLETED", result.getRecentGenerations().get(0).getStatus());
    }

    // ---- Helper ----

    private DataField buildField(String name, DataField.FieldType fieldType) {
        DataField field = DataField.builder()
                .name(name)
                .fieldType(fieldType)
                .nullable(false)
                .constraints("{}")
                .build();
        field.setId(UUID.randomUUID());
        return field;
    }
}
