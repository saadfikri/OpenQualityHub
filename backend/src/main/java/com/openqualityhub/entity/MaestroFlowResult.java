package com.openqualityhub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "maestro_flow_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaestroFlowResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id")
    private TestCase testCase;

    @Column(name = "flow_name", nullable = false, length = 500)
    private String flowName;

    @Column(name = "flow_file", length = 500)
    private String flowFile;

    @Column(name = "device_name", length = 200)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_platform", length = 20)
    private DevicePlatform devicePlatform;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "device_model", length = 200)
    private String deviceModel;

    @Column(name = "device_locale", length = 20)
    private String deviceLocale;

    @Column(name = "app_id", length = 300)
    private String appId;

    @Column(name = "app_version", length = 100)
    private String appVersion;

    @Column(name = "screenshot_url", length = 1000)
    private String screenshotUrl;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlowStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    public enum DevicePlatform {
        IOS, ANDROID, WEB
    }

    public enum FlowStatus {
        PASSED, FAILED, ERROR, SKIPPED, CANCELLED
    }
}
