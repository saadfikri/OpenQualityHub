package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.DefectResponse;
import com.openqualityhub.entity.Defect;
import com.openqualityhub.entity.Project;
import com.openqualityhub.entity.Release;
import com.openqualityhub.entity.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DefectMapperTest {

    private final DefectMapper mapper = Mappers.getMapper(DefectMapper.class);

    @Test
    void toResponse_shouldMapAllRelations() {
        Project project = Project.builder().name("MyProject").build();
        project.setId(UUID.randomUUID());

        Release release = Release.builder().name("v1.0").build();
        release.setId(UUID.randomUUID());

        User assignee = User.builder().displayName("John Doe").build();
        assignee.setId(UUID.randomUUID());

        User reporter = User.builder().displayName("Jane Smith").build();
        reporter.setId(UUID.randomUUID());

        Defect defect = Defect.builder()
            .title("Login bug")
            .description("Cannot login")
            .severity(Defect.Severity.CRITICAL)
            .status(Defect.DefectStatus.OPEN)
            .priority(1)
            .component("auth")
            .project(project)
            .release(release)
            .assignedTo(assignee)
            .reportedBy(reporter)
            .resolvedAt(Instant.now())
            .build();
        defect.setId(UUID.randomUUID());

        DefectResponse response = mapper.toResponse(defect);

        assertEquals(defect.getId(), response.getId());
        assertEquals("Login bug", response.getTitle());
        assertEquals("CRITICAL", response.getSeverity());
        assertEquals("OPEN", response.getStatus());
        assertEquals(1, response.getPriority());
        assertEquals(project.getId(), response.getProjectId());
        assertEquals("MyProject", response.getProjectName());
        assertEquals(release.getId(), response.getReleaseId());
        assertEquals("v1.0", response.getReleaseName());
        assertEquals(assignee.getId(), response.getAssignedToId());
        assertEquals("John Doe", response.getAssignedToName());
        assertEquals(reporter.getId(), response.getReportedById());
        assertEquals("Jane Smith", response.getReportedByName());
        assertNotNull(response.getResolvedAt());
    }

    @Test
    void toResponse_shouldHandleNullRelations() {
        Defect defect = Defect.builder()
            .title("Simple bug")
            .severity(Defect.Severity.LOW)
            .status(Defect.DefectStatus.OPEN)
            .build();
        defect.setId(UUID.randomUUID());

        DefectResponse response = mapper.toResponse(defect);

        assertEquals("Simple bug", response.getTitle());
        assertNull(response.getProjectId());
        assertNull(response.getReleaseId());
        assertNull(response.getAssignedToId());
        assertNull(response.getReportedById());
    }
}
