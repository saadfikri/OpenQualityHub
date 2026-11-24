package com.openqualityhub.mapper;

import com.openqualityhub.dto.request.CreateTeamRequest;
import com.openqualityhub.dto.response.TeamResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamMapperTest {

    private final TeamMapper mapper = Mappers.getMapper(TeamMapper.class);

    @Test
    void toEntity_shouldMapFields() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("Platform QA");
        req.setDescription("Platform quality team");

        Team team = mapper.toEntity(req);

        assertEquals("Platform QA", team.getName());
        assertEquals("Platform quality team", team.getDescription());
    }

    @Test
    void toResponse_shouldMapFieldsAndProjectCount() {
        Team team = Team.builder().name("Backend QA").description("Backend team").build();
        team.setId(UUID.randomUUID());
        team.setProjects(List.of(
            Project.builder().name("P1").build(),
            Project.builder().name("P2").build()
        ));

        TeamResponse response = mapper.toResponse(team);

        assertEquals(team.getId(), response.getId());
        assertEquals("Backend QA", response.getName());
        assertEquals("Backend team", response.getDescription());
        assertEquals(2, response.getProjectCount());
    }

    @Test
    void toResponse_shouldHandleNullProjects() {
        Team team = Team.builder().name("Empty").build();
        team.setId(UUID.randomUUID());
        team.setProjects(null);

        TeamResponse response = mapper.toResponse(team);

        assertEquals(0, response.getProjectCount());
    }
}
