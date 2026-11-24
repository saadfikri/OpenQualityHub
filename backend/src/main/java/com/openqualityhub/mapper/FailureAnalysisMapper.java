package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.FailureAnalysisResponse;
import com.openqualityhub.entity.FailureAnalysis;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FailureAnalysisMapper {

    @Mapping(target = "testRunId", source = "testRun.id")
    @Mapping(target = "testRunName", source = "testRun.name")
    @Mapping(target = "status", expression = "java(analysis.getStatus().name())")
    @Mapping(target = "failureCategory", expression = "java(analysis.getFailureCategory() != null ? analysis.getFailureCategory().name() : null)")
    @Mapping(target = "humanFeedback", expression = "java(analysis.getHumanFeedback().name())")
    FailureAnalysisResponse toResponse(FailureAnalysis analysis);
}
