package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.IntegrationConfigResponse;
import com.openqualityhub.dto.response.WebhookEventResponse;
import com.openqualityhub.entity.IntegrationConfig;
import com.openqualityhub.entity.WebhookEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IntegrationMapper {

    @Mapping(target = "provider", expression = "java(config.getProvider().name())")
    @Mapping(target = "hasSecret", expression = "java(config.getWebhookSecret() != null && !config.getWebhookSecret().isBlank())")
    @Mapping(target = "projectId", expression = "java(config.getProject() != null ? config.getProject().getId() : null)")
    @Mapping(target = "projectName", expression = "java(config.getProject() != null ? config.getProject().getName() : null)")
    IntegrationConfigResponse toResponse(IntegrationConfig config);

    @Mapping(target = "integrationId", source = "integration.id")
    @Mapping(target = "provider", expression = "java(event.getProvider().name())")
    @Mapping(target = "status", expression = "java(event.getStatus().name())")
    WebhookEventResponse toEventResponse(WebhookEvent event);
}
