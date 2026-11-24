package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.DataFieldResponse;
import com.openqualityhub.dto.response.TestDataGenerationResponse;
import com.openqualityhub.dto.response.TestDataProfileResponse;
import com.openqualityhub.dto.response.TestDataTemplateDetailResponse;
import com.openqualityhub.dto.response.TestDataTemplateResponse;
import com.openqualityhub.entity.DataField;
import com.openqualityhub.entity.TestDataGeneration;
import com.openqualityhub.entity.TestDataProfile;
import com.openqualityhub.entity.TestDataTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TestDataMapper {

    @Mapping(target = "projectId", expression = "java(template.getProject() != null ? template.getProject().getId() : null)")
    @Mapping(target = "projectName", expression = "java(template.getProject() != null ? template.getProject().getName() : null)")
    @Mapping(target = "fieldCount", expression = "java(template.getFields() != null ? (int) template.getFields().stream().filter(f -> !f.isDeleted()).count() : 0)")
    @Mapping(target = "profileCount", expression = "java(template.getProfiles() != null ? (int) template.getProfiles().stream().filter(p -> !p.isDeleted()).count() : 0)")
    @Mapping(target = "generationCount", expression = "java(template.getGenerations() != null ? (int) template.getGenerations().stream().filter(g -> !g.isDeleted()).count() : 0)")
    TestDataTemplateResponse toTemplateResponse(TestDataTemplate template);

    @Mapping(target = "projectId", expression = "java(template.getProject() != null ? template.getProject().getId() : null)")
    @Mapping(target = "projectName", expression = "java(template.getProject() != null ? template.getProject().getName() : null)")
    @Mapping(target = "fieldCount", expression = "java(template.getFields() != null ? (int) template.getFields().stream().filter(f -> !f.isDeleted()).count() : 0)")
    @Mapping(target = "profileCount", expression = "java(template.getProfiles() != null ? (int) template.getProfiles().stream().filter(p -> !p.isDeleted()).count() : 0)")
    @Mapping(target = "generationCount", expression = "java(template.getGenerations() != null ? (int) template.getGenerations().stream().filter(g -> !g.isDeleted()).count() : 0)")
    @Mapping(target = "fields", expression = "java(template.getFields() != null ? template.getFields().stream().filter(f -> !f.isDeleted()).map(this::toFieldResponse).toList() : java.util.List.of())")
    @Mapping(target = "profiles", expression = "java(template.getProfiles() != null ? template.getProfiles().stream().filter(p -> !p.isDeleted()).map(this::toProfileResponse).toList() : java.util.List.of())")
    TestDataTemplateDetailResponse toTemplateDetailResponse(TestDataTemplate template);

    @Mapping(target = "fieldType", expression = "java(field.getFieldType().name())")
    DataFieldResponse toFieldResponse(DataField field);

    @Mapping(target = "profileType", expression = "java(profile.getProfileType().name())")
    TestDataProfileResponse toProfileResponse(TestDataProfile profile);

    @Mapping(target = "templateId", source = "template.id")
    @Mapping(target = "templateName", source = "template.name")
    @Mapping(target = "profileId", source = "profile.id")
    @Mapping(target = "profileName", source = "profile.name")
    @Mapping(target = "status", expression = "java(gen.getStatus().name())")
    @Mapping(target = "exportFormat", expression = "java(gen.getExportFormat().name())")
    TestDataGenerationResponse toGenerationResponse(TestDataGeneration gen);

    List<DataFieldResponse> toFieldResponses(List<DataField> fields);

    List<TestDataProfileResponse> toProfileResponses(List<TestDataProfile> profiles);
}
