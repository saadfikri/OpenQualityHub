package com.openqualityhub.mapper;

import com.openqualityhub.dto.response.TestCaseResponse;
import com.openqualityhub.dto.response.TestRunDetailResponse;
import com.openqualityhub.dto.response.TestRunResponse;
import com.openqualityhub.entity.TestCase;
import com.openqualityhub.entity.TestRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TestRunMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    TestRunResponse toResponse(TestRun testRun);

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "testCases", source = "testCases")
    TestRunDetailResponse toDetailResponse(TestRun testRun);

    TestCaseResponse toTestCaseResponse(TestCase testCase);

    List<TestCaseResponse> toTestCaseResponses(List<TestCase> testCases);
}
