package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.ContributorActivityResponse;
import com.openqualityhub.dto.response.ContributorResponse;
import com.openqualityhub.entity.Contributor;
import com.openqualityhub.entity.ContributorActivity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContributorMapper {

    @Mapping(target = "provider", expression = "java(contributor.getProvider().name())")
    @Mapping(target = "linkedUserId", expression = "java(contributor.getLinkedUser() != null ? contributor.getLinkedUser().getId() : null)")
    @Mapping(target = "linkedUserName", expression = "java(contributor.getLinkedUser() != null ? (contributor.getLinkedUser().getDisplayName() != null ? contributor.getLinkedUser().getDisplayName() : contributor.getLinkedUser().getFullName()) : null)")
    ContributorResponse toResponse(Contributor contributor);

    @Mapping(target = "contributorId", source = "contributor.id")
    @Mapping(target = "activityType", expression = "java(activity.getActivityType().name())")
    @Mapping(target = "projectId", expression = "java(activity.getProject() != null ? activity.getProject().getId() : null)")
    @Mapping(target = "projectName", expression = "java(activity.getProject() != null ? activity.getProject().getName() : null)")
    ContributorActivityResponse toActivityResponse(ContributorActivity activity);
}
