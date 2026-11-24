package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.QaServiceResponse;
import com.openqualityhub.entity.QaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QaServiceMapper {

    @Mapping(target = "id", expression = "java(service.getId() != null ? service.getId().toString() : null)")
    @Mapping(target = "category", expression = "java(service.getCategory() != null ? service.getCategory().name() : null)")
    @Mapping(target = "createdAt", expression = "java(service.getCreatedAt() != null ? service.getCreatedAt().toString() : null)")
    @Mapping(target = "updatedAt", expression = "java(service.getUpdatedAt() != null ? service.getUpdatedAt().toString() : null)")
    QaServiceResponse toResponse(QaService service);
}
