package com.openqualityhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaestroDeviceMatrixResponse {

    private List<DeviceEntry> devices;
    private List<PlatformSummary> platforms;
    private List<AppVersionEntry> appVersions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceEntry {
        private String deviceName;
        private String osVersion;
        private String platform;
        private String status;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformSummary {
        private String platform;
        private long totalFlows;
        private long passed;
        private long failed;
        private double passRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppVersionEntry {
        private String appId;
        private String appVersion;
        private long totalFlows;
        private long passed;
        private double passRate;
    }
}
