package com.openqualityhub.dto.response;

import com.openqualityhub.entity.TestCase.TestStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class TestCaseResponse {
    private UUID id;
    private String name;
    private String className;
    private String suiteName;
    private TestStatus status;
    private Long durationMs;
    private String errorMessage;
    private String stackTrace;
}
