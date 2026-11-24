package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.ReleaseResponse;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.Release;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseMapperTest {

    private final ReleaseMapper mapper = Mappers.getMapper(ReleaseMapper.class);

    @Test
    void toResponse_shouldMapProjectRelation() {
        Project project = Project.builder().name("Backend").build();
        project.setId(UUID.randomUUID());

        Release release = Release.builder()
            .releaseVersion("2.1.0")
            .name("Sprint 21")
            .description("Q4 release")
            .project(project)
            .targetDate(LocalDate.of(2026, 12, 1))
            .build();
        release.setId(UUID.randomUUID());

        ReleaseResponse response = mapper.toResponse(release);

        assertEquals(release.getId(), response.getId());
        assertEquals("2.1.0", response.getReleaseVersion());
        assertEquals("Sprint 21", response.getName());
        assertEquals(project.getId(), response.getProjectId());
        assertEquals("Backend", response.getProjectName());
        assertEquals(LocalDate.of(2026, 12, 1), response.getTargetDate());
    }

    @Test
    void toResponse_shouldHandleNullProject() {
        Release release = Release.builder()
            .releaseVersion("1.0.0")
            .name("Initial")
            .build();
        release.setId(UUID.randomUUID());

        ReleaseResponse response = mapper.toResponse(release);

        assertNull(response.getProjectId());
        assertNull(response.getProjectName());
    }
}
