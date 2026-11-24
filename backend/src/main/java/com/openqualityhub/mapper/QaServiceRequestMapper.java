package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.QaServiceRequestResponse;
import com.openqualityhub.entity.QaRequestActivity;
import com.openqualityhub.entity.QaServiceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QaServiceRequestMapper {

    @Mapping(target = "id", expression = "java(r.getId() != null ? r.getId().toString() : null)")
    @Mapping(target = "status", expression = "java(r.getStatus() != null ? r.getStatus().name() : null)")
    @Mapping(target = "impact", expression = "java(r.getImpact() != null ? r.getImpact().name() : null)")
    @Mapping(target = "urgency", expression = "java(r.getUrgency() != null ? r.getUrgency().name() : null)")
    @Mapping(target = "effectivePriority", expression = "java(r.getEffectivePriority())")
    @Mapping(source = "qaService.id", target = "qaServiceId")
    @Mapping(source = "qaService.name", target = "qaServiceName")
    @Mapping(target = "qaServiceCategory", expression = "java(r.getQaService() != null && r.getQaService().getCategory() != null ? r.getQaService().getCategory().name() : null)")
    @Mapping(source = "requestingTeam.id", target = "requestingTeamId")
    @Mapping(source = "requestingTeam.name", target = "requestingTeamName")
    @Mapping(source = "requestingUser.id", target = "requestingUserId")
    @Mapping(source = "requestingUser.displayName", target = "requestingUserName")
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "assignedTeam.id", target = "assignedTeamId")
    @Mapping(source = "assignedTeam.name", target = "assignedTeamName")
    @Mapping(source = "assignedUser.id", target = "assignedUserId")
    @Mapping(source = "assignedUser.displayName", target = "assignedUserName")
    @Mapping(target = "responseDeadline", expression = "java(r.getResponseDeadline() != null ? r.getResponseDeadline().toString() : null)")
    @Mapping(target = "resolutionDeadline", expression = "java(r.getResolutionDeadline() != null ? r.getResolutionDeadline().toString() : null)")
    @Mapping(target = "respondedAt", expression = "java(r.getRespondedAt() != null ? r.getRespondedAt().toString() : null)")
    @Mapping(target = "resolvedAt", expression = "java(r.getResolvedAt() != null ? r.getResolvedAt().toString() : null)")
    @Mapping(target = "createdAt", expression = "java(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)")
    @Mapping(target = "updatedAt", expression = "java(r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null)")
    @Mapping(target = "activities", source = "activities")
    QaServiceRequestResponse toResponse(QaServiceRequest r);

    @Mapping(target = "id", expression = "java(a.getId() != null ? a.getId().toString() : null)")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.displayName", target = "userName")
    @Mapping(target = "activityType", expression = "java(a.getActivityType() != null ? a.getActivityType().name() : null)")
    @Mapping(target = "createdAt", expression = "java(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)")
    QaServiceRequestResponse.ActivityResponse toActivityResponse(QaRequestActivity a);

    List<QaServiceRequestResponse.ActivityResponse> toActivityResponses(List<QaRequestActivity> activities);
}
