package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaestroFlowResultResponse {
    private String id;
    private String testRunId;
    private String flowName;
    private String flowFile;
    private String deviceName;
    private String devicePlatform;
    private String osVersion;
    private String deviceModel;
    private String deviceLocale;
    private String appId;
    private String appVersion;
    private String screenshotUrl;
    private String videoUrl;
    private Long durationMs;
    private String status;
    private String errorMessage;
    private int retryCount;
    private String createdAt;
}
