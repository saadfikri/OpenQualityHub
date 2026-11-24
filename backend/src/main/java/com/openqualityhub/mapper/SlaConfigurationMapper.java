package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.SlaConfigurationResponse;
import com.openqualityhub.entity.SlaConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SlaConfigurationMapper {

    @Mapping(target = "id", expression = "java(sla.getId() != null ? sla.getId().toString() : null)")
    @Mapping(target = "createdAt", expression = "java(sla.getCreatedAt() != null ? sla.getCreatedAt().toString() : null)")
    @Mapping(target = "updatedAt", expression = "java(sla.getUpdatedAt() != null ? sla.getUpdatedAt().toString() : null)")
    SlaConfigurationResponse toResponse(SlaConfiguration sla);
}
