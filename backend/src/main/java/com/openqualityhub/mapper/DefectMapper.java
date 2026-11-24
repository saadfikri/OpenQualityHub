package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.DefectResponse;
import com.openqualityhub.entity.Defect;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DefectMapper {

    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "release.id", target = "releaseId")
    @Mapping(source = "release.name", target = "releaseName")
    @Mapping(source = "assignedTo.id", target = "assignedToId")
    @Mapping(source = "assignedTo.displayName", target = "assignedToName")
    @Mapping(source = "reportedBy.id", target = "reportedById")
    @Mapping(source = "reportedBy.displayName", target = "reportedByName")
    DefectResponse toResponse(Defect defect);
}
