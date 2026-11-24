package com.openqualityhub.mapper;

import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.response.TeamResponse;
import com.openqualityhub.entity.Team;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeamMapper {

    Team toEntity(CreateTeamRequest request);

    @Mapping(target = "projectCount", expression = "java(team.getProjects() != null ? team.getProjects().size() : 0)")
    TeamResponse toResponse(Team team);
}
