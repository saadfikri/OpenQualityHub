package com.openqualityhub.mapper;

import com.openqualityhub.dto.request.CreateProjectRequest;
import com.openqualityhub.dto.response.ProjectResponse;
import com.openqualityhub.entity.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectMapper {

    Project toEntity(CreateProjectRequest request);

    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "teamName", source = "team.name")
    @Mapping(target = "testRunCount", expression = "java(project.getTestRuns() != null ? project.getTestRuns().size() : 0)")
    ProjectResponse toResponse(Project project);
}
