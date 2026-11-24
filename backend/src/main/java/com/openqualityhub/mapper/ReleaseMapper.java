package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.ReleaseResponse;
import com.openqualityhub.entity.Release;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReleaseMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    ReleaseResponse toResponse(Release release);
}
