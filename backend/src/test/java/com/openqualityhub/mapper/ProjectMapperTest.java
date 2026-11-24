package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.ProjectResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.Team;
import com.openqualityhub.entity.TestRun;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProjectMapperTest {

    private final ProjectMapper mapper = Mappers.getMapper(ProjectMapper.class);

    @Test
    void toResponse_shouldMapTeamFields() {
        Team team = Team.builder().name("QA Team").build();
        team.setId(UUID.randomUUID());

        Project project = Project.builder().name("API Service").description("REST API").team(team).build();
        project.setId(UUID.randomUUID());
        project.setTestRuns(List.of(TestRun.builder().name("Run1").build()));

        ProjectResponse response = mapper.toResponse(project);

        assertEquals(project.getId(), response.getId());
        assertEquals("API Service", response.getName());
        assertEquals(team.getId(), response.getTeamId());
        assertEquals("QA Team", response.getTeamName());
        assertEquals(1, response.getTestRunCount());
    }

    @Test
    void toResponse_shouldHandleNullTestRuns() {
        Team team = Team.builder().name("Team").build();
        team.setId(UUID.randomUUID());

        Project project = Project.builder().name("P").team(team).build();
        project.setId(UUID.randomUUID());
        project.setTestRuns(null);

        ProjectResponse response = mapper.toResponse(project);

        assertEquals(0, response.getTestRunCount());
    }
}
